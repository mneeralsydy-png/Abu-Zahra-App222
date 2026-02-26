package com.abualzahra.parent.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class LocalSyncService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "SEND_COMMAND") {
            val cmd = intent.getStringExtra("command")
            Log.d("SyncService", "Command received: $cmd")
            // هنا يتم معالجة الأمر وإرساله للطفل لاحقاً
        }
        return START_STICKY
    }
}
