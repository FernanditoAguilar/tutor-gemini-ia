package com.pocha.app.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsService(context: Context) : TextToSpeech.OnInitListener {
    
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "AR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsService", "Lenguaje no soportado (AR), intentando español genérico")
                tts?.setLanguage(Locale("es", "ES"))
            }
            isInitialized = true
            // Ajustes personalizados por el usuario
            tts?.setPitch(0.7f) // Más grave
            tts?.setSpeechRate(0.7f) // Más lento y pausado
        } else {
            Log.e("TtsService", "Error al inicializar TTS")
        }
    }

    fun speak(text: String, onComplete: () -> Unit = {}) {
        if (isInitialized) {
            Log.d("TtsService", "Speaking: $text")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PochaID")
            // Nota: En una implementación más avanzada usaríamos UtteranceProgressListener 
            // para detectar el fin real, pero para esta versión básica invocamos el callback.
            onComplete()
        } else {
            Log.w("TtsService", "TTS no inicializado aún")
            onComplete()
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
