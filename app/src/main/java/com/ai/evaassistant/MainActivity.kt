package com.ai.evaassistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("eva_config", Context.MODE_PRIVATE)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 70, 50, 50)
            setBackgroundColor(Color.parseColor("#121218"))
        }

        fun label(txt: String) = TextView(this).apply {
            text = txt
            setTextColor(Color.parseColor("#E0E0FF"))
            textSize = 14f
            setPadding(0, 24, 0, 6)
        }

        fun input(hintTxt: String, savedKey: String, defVal: String) = EditText(this).apply {
            hint = hintTxt
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setText(prefs.getString(savedKey, defVal))
        }

        val header = TextView(this).apply {
            text = "Ева AI — Настройки Голоса"
            textSize = 22f
            setTextColor(Color.parseColor("#FF6EC7"))
            setPadding(0, 0, 0, 20)
        }

        val geminiBox = input("Gemini API Key", "gemini_key", "")
        val elevenBox = input("ElevenLabs Key", "eleven_key", "")
        val voiceIdBox = input("Voice ID голоса", "voice_id", "EXAVITQu4vr4xnSDxMaL")
        
        // Новые поля для настройки эмоций
        val stabilityBox = input("Стабильность (0.1 - 1.0, по умолч: 0.45)", "stability", "0.45")
        val styleBox = input("Эмоциональность/Стиль (0.0 - 1.0, по умолч: 0.20)", "style_ex", "0.20")

        val startBtn = Button(this).apply {
            text = "ЗАПУСТИТЬ ЕВУ В ФОНОВОМ РЕЖИМЕ"
            setBackgroundColor(Color.parseColor("#D81B60"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                prefs.edit()
                    .putString("gemini_key", geminiBox.text.toString())
                    .putString("eleven_key", elevenBox.text.toString())
                    .putString("voice_id", voiceIdBox.text.toString())
                    .putString("stability", stabilityBox.text.toString())
                    .putString("style_ex", styleBox.text.toString())
                    .apply()
                checkAndStartService()
            }
        }

        val stopBtn = Button(this).apply {
            text = "Остановить фоновую работу"
            setOnClickListener {
                stopService(Intent(this@MainActivity, AlwaysOnVoiceService::class.java))
                Toast.makeText(this@MainActivity, "Ева отдыхает", Toast.LENGTH_SHORT).show()
            }
        }

        root.addView(header)
        root.addView(label("Ключ Google Gemini:"))
        root.addView(geminiBox)
        root.addView(label("Ключ ElevenLabs:"))
        root.addView(elevenBox)
        root.addView(label("ID женского голоса ElevenLabs:"))
        root.addView(voiceIdBox)
        root.addView(label("Стабильность голоса (Stability):"))
        root.addView(stabilityBox)
        root.addView(label("Эмоциональность (Style Exaggeration):"))
        root.addView(styleBox)
        root.addView(startBtn)
        root.addView(stopBtn)

        scroll.addView(root)
        setContentView(scroll)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }
    }

    private fun checkAndStartService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            Toast.makeText(this, "Дайте разрешение на микрофон!", Toast.LENGTH_LONG).show()
            return
        }

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            } catch (_: Exception) {}
        }

        val svc = Intent(this, AlwaysOnVoiceService::class.java)
        stopService(svc)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }
        Toast.makeText(this, "Ева запущена с новыми настройками голоса!", Toast.LENGTH_LONG).show()
    }
}
