package com.ai.evaassistant

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this).apply {
            text = "Ева Ассистент успешно запущен!"
            textSize = 20f
            setPadding(50, 100, 50, 50)
        }
        setContentView(text)
    }
}
