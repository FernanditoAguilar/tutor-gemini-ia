package com.pocha.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.pocha.app.databinding.ActivityMainBinding
import com.pocha.app.services.AudioHandler
import com.pocha.app.services.GeminiLiveService
import com.pocha.app.services.LlmService
import com.pocha.app.services.SttService
import com.pocha.app.services.TtsService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val RECORD_AUDIO_REQUEST_CODE = 101

    private lateinit var sttService: SttService
    private val llmService = LlmService()
    private lateinit var ttsService: TtsService
    private lateinit var geminiLiveService: GeminiLiveService
    private lateinit var audioHandler: AudioHandler

    private var isBotActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioHandler = AudioHandler(this)
        ttsService = TtsService(this)
        sttService = SttService(this)
        geminiLiveService = GeminiLiveService(this)
        
        setupUI()
        binding.tvConversationStatus.append("\n[Versión STT Real 1.3]")
        checkPermissions()
    }

    private fun setupUI() {
        binding.btnMainAction.setOnClickListener {
            handlePochaClick()
        }
    }

    private fun handlePochaClick() {
        if (isBotActive) return

        // 1. Detener cualquier voz previa
        ttsService.stop()
        
        // 2. Iniciar escucha
        startSttFlow()
    }

    private fun startSttFlow() {
        setUiState(true, "Escuchando...")
        
        sttService.startListening(
            onResult = { text ->
                if (text.isNotBlank()) {
                    processConversation(text)
                } else {
                    setUiState(false, "No escuché nada. Toca para intentar de nuevo.")
                }
            },
            onError = { error ->
                setUiState(false, "Ups: $error")
            }
        )
    }

    private fun setUiState(active: Boolean, status: String) {
        isBotActive = active
        binding.tvConversationStatus.text = status
        binding.btnMainAction.isEnabled = !active
        binding.btnMainAction.alpha = if (active) 0.5f else 1.0f
    }

    private fun processConversation(userInput: String) {
        lifecycleScope.launch {
            try {
                setUiState(true, "Pocha está pensando...")
                binding.tvConversationStatus.text = "Tú: $userInput\n\nPocha: ..."

                // Usamos el nuevo servicio Live para una respuesta fluida
                geminiLiveService.startRealtimeConversation(userInput) { streamingText ->
                    runOnUiThread {
                        binding.tvConversationStatus.text = "Tú: $userInput\n\nPocha: $streamingText"
                        
                        // Scroll al final mientras escribe
                        binding.scrollConversation.post {
                            binding.scrollConversation.fullScroll(View.FOCUS_DOWN)
                        }
                    }
                }
                
                // Nota: En esta etapa el servicio Live maneja el texto. 
                // Para una versión final, el audio vendría directamente del stream.
                // Por ahora simulamos la fluidez visual.
                
                // Mantenemos una pausa para el feedback visual
                kotlinx.coroutines.delay(2000)
                setUiState(false, "Listo")
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Error en el flujo", e)
                setUiState(false, "Error: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioHandler.release()
        ttsService.shutdown()
        sttService.destroy()
        geminiLiveService.stop()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, 
                arrayOf(Manifest.permission.RECORD_AUDIO), 
                RECORD_AUDIO_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de audio concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso de audio denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
