package com.pocha.app.services

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AudioHandler(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var outputFilePath: String? = null

    fun startRecording(): String? {
        val outputFile = File(context.cacheDir, "user_input.m4a")
        outputFilePath = outputFile.absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFilePath)
            
            try {
                prepare()
                start()
                Log.d("AudioHandler", "Recording started: $outputFilePath")
            } catch (e: IOException) {
                Log.e("AudioHandler", "Recording failed", e)
                return null
            }
        }
        return outputFilePath
    }

    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Log.d("AudioHandler", "Recording stopped")
            outputFilePath?.let { File(it) }
        } catch (e: Exception) {
            Log.e("AudioHandler", "Error stopping recording", e)
            null
        }
    }

    fun playAudio(audioData: ByteArray, onComplete: () -> Unit = {}) {
        if (audioData.isEmpty()) {
            Log.w("AudioHandler", "Received empty audio data, skipping playback")
            onComplete()
            return
        }
        try {
            // Create a temp file to play from byte array
            val tempFile = File.createTempFile("pocha_response", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioData) }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener { 
                    onComplete()
                    it.release() 
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("AudioHandler", "MediaPlayer error: $what, $extra")
                    onComplete()
                    true
                }
                prepare()
                start()
            }
            Log.d("AudioHandler", "Playback started")
        } catch (e: Exception) {
            Log.e("AudioHandler", "Playback failed", e)
            onComplete()
        }
    }
    
    fun release() {
        mediaRecorder?.release()
        mediaPlayer?.release()
    }
}
