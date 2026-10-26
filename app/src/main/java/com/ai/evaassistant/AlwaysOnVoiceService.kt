package com.ai.evaassistant

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AlwaysOnVoiceService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
