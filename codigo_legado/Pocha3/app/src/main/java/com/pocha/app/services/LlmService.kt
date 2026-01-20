package com.pocha.app.services

import android.util.Log
import com.pocha.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class LlmService {
    
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val systemInstructions = "Eres Pocha, una asistente virtual educada, clara y sencilla. " +
            "Responde SIEMPRE de forma BREVE, DIRECTA y sin tecnicismos. " +
            "Si te preguntan la hora, responde con naturalidad. No des explicaciones de cómo funciona el tiempo. " +
            "Usa un lenguaje natural y respetuoso, pero evita expresiones excesivamente cariñosas como 'mi amor' o 'mi cielo'. Responde siempre en español."

    suspend fun getAiResponse(userInput: String): String {
        return withContext(Dispatchers.IO) {
            if (apiKey.isNullOrBlank()) {
                return@withContext "Lo siento, me falta mi llave de memoria. Por favor, configura mi clave de API."
            }
            
            val sdf = java.text.SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy, HH:mm", java.util.Locale("es", "ES"))
            val currentTime = sdf.format(java.util.Date())
            val cleanInput = "Contexto: Hoy es $currentTime. Usuario dice: ${if (userInput.isBlank()) "Hola" else userInput}"
            
            // Modelos actualizados a Enero 2026
            val models = listOf("gemini-3-flash", "gemini-2.5-flash", "gemini-2.5-pro")
            
            for (modelName in models) {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                try {
                    val jsonRequest = JSONObject().apply {
                        put("contents", JSONArray().put(JSONObject().apply {
                            put("parts", JSONArray().put(JSONObject().put("text", cleanInput)))
                        }))
                        put("system_instruction", JSONObject().apply {
                            put("parts", JSONArray().put(JSONObject().put("text", systemInstructions)))
                        })
                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.7)
                            put("maxOutputTokens", 800)
                        })
                    }

                    val body = jsonRequest.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder().url(url).post(body).build()

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""
                        
                        if (response.isSuccessful) {
                            val jsonResponse = JSONObject(responseBody)
                            val candidates = jsonResponse.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val candidate = candidates.getJSONObject(0)
                                val text = candidate.optJSONObject("content")
                                    ?.optJSONArray("parts")
                                    ?.optJSONObject(0)
                                    ?.optString("text")
                                
                                if (!text.isNullOrBlank()) {
                                    return@withContext text
                                }
                            }
                        } else {
                            Log.e("LlmService", "Fallo con $modelName: ${response.code} - $responseBody")
                        }
                        Unit
                    }
                } catch (e: Exception) {
                    Log.e("LlmService", "Excepción con $modelName", e)
                }
            }
            
            "Lo siento, mi conexión está un poco inestable. ¿Podrías repetirme eso?"
        }
    }

    private fun parseError(body: String): String {
        return try {
            val json = JSONObject(body)
            val error = json.optJSONObject("error")
            error?.optString("message") ?: "Error desconocido"
        } catch (e: Exception) {
            "Error de red"
        }
    }
}
