package com.ai.evaassistant

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat

class AlwaysOnVoiceService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private lateinit var speechIntent: Intent

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        setupRecognizer()
        startListening()
    }

    private fun setupRecognizer() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
        }

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val phrase = list?.firstOrNull() ?: ""
                if (phrase.isNotEmpty()) {
                    // Здесь в дальнейшем обрабатывается распознанная речь
                }
                restartListening()
            }

            override fun onError(error: Int) {
                restartListening()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        try {
            recognizer?.startListening(speechIntent)
        } catch (_: Exception) {}
    }

    private fun restartListening() {
        android.os.Handler(mainLooper).postDelayed({
            startListening()
        }, 1000)
    }

    private fun startForegroundNotification() {
        val channelId = "eva_service_channel"
        val channel = NotificationChannel(channelId, "Ева ФОН", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ева активна")
            .setContentText("Фоновый режим включен...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        recognizer?.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
