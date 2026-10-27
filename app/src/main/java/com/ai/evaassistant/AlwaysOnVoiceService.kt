package com.ai.evaassistant

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class AlwaysOnVoiceService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private lateinit var speechIntent: Intent
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient()

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
                    processUserPhrase(phrase)
                } else {
                    restartListening()
                }
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

    private fun processUserPhrase(phrase: String) {
        val prefs = getSharedPreferences("eva_config", Context.MODE_PRIVATE)
        val geminiKey = prefs.getString("gemini_key", "") ?: ""
        val elevenKey = prefs.getString("eleven_key", "") ?: ""
        val voiceId = prefs.getString("voice_id", "EXAVITQu4vr4xnSDxMaL") ?: "EXAVITQu4vr4xnSDxMaL"

        serviceScope.launch {
            try {
                val answer = if (geminiKey.isNotEmpty()) {
                    askGemini(phrase, geminiKey)
                } else {
                    "Пожалуйста, укажите API-ключ Gemini в настройках приложения."
                }

                if (elevenKey.isNotEmpty()) {
                    speakWithElevenLabs(answer, elevenKey, voiceId)
                }
            } catch (_: Exception) {
            } finally {
                restartListening()
            }
        }
    }

    private suspend fun askGemini(prompt: String, apiKey: String): String = withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val jsonBody = JSONObject().apply {
            put("contents", org.json.JSONArray().put(
                JSONObject().put("parts", org.json.JSONArray().put(
                    JSONObject().put("text", "Ответь кратко, чувственно и по-русски на фразу: $prompt")
                ))
            ))
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext "Ошибка соединения с ИИ"
            val body = response.body?.string() ?: return@withContext "Пустой ответ"
            val json = JSONObject(body)
            json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

    private suspend fun speakWithElevenLabs(text: String, apiKey: String, voiceId: String) = withContext(Dispatchers.IO) {
        val url = "https://api.elevenlabs.io/v1/text-to-speech/$voiceId"
        val jsonBody = JSONObject().apply {
            put("text", text)
            put("model_id", "eleven_multilingual_v2")
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("xi-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val bytes = response.body?.bytes() ?: return@withContext
                val tempFile = File(cacheDir, "eva_voice.mp3")
                FileOutputStream(tempFile).use { it.write(bytes) }

                withContext(Dispatchers.Main) {
                    MediaPlayer.create(applicationContext, Uri.fromFile(tempFile))?.apply {
                        setOnCompletionListener { 
                            it.release()
                            tempFile.delete()
                        }
                        start()
                    }
                }
            }
        }
    }

    private fun startListening() {
        try {
            recognizer?.startListening(speechIntent)
        } catch (_: Exception) {}
    }

    private fun restartListening() {
        android.os.Handler(mainLooper).postDelayed({
            startListening()
        }, 1500)
    }

    private fun startForegroundNotification() {
        val channelId = "eva_service_channel"
        val channel = NotificationChannel(channelId, "Ева ФОН", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ева активна")
            .setContentText("Слушаю вас и готова отвечать...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        recognizer?.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
