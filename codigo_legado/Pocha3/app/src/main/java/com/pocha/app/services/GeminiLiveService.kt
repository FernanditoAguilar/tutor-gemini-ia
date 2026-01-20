package com.pocha.app.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.pocha.app.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GeminiLiveService(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var audioTrack: AudioTrack? = null
    
    // Configuración de la voz (Puck, Charon, Kore, Fenrir, Zephyr)
    private var selectedVoice = "Zephyr" 

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash-8b", // O gemini-2.0-flash-exp cuando esté disponible estable
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.7f
            // En el SDK v1beta/Multimodal Live se configura via 'speechConfig' 
            // pero para este ejemplo inicial usamos la interfaz de streaming disponible
        }
    )

    private val systemInstructions = "Eres Pocha, una asistente virtual educada y clara. " +
            "Responde de forma natural, breve y directa. " +
            "Tu voz debe ser cálida y pausada."

    init {
        setupAudioTrack()
    }

    private fun setupAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            24000, 
            AudioFormat.CHANNEL_OUT_MONO, 
            AudioFormat.CHANNEL_OUT_MONO
        )
        
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(24000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        
        audioTrack?.play()
    }

    fun startRealtimeConversation(userInput: String, onTextUpdate: (String) -> Unit) {
        scope.launch {
            try {
                // Para una versión "Live" real usuaríamos el socket API
                // Por ahora implementamos el streaming de respuesta para mejorar la percepción de velocidad
                val chat = model.startChat(
                    history = listOf(
                        content(role = "user") { text(systemInstructions) },
                        content(role = "model") { text("Entendido, soy Pocha. ¿En qué puedo ayudarte?") }
                    )
                )

                chat.sendMessageStream(userInput).collectLatest { response ->
                    response.text?.let { onTextUpdate(it) }
                }
            } catch (e: Exception) {
                Log.e("GeminiLiveService", "Error en conversación", e)
            }
        }
    }

    fun stop() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
