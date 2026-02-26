package com.abualzahra.parent

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.widget.Toast
import org.json.JSONObject

class WebAppInterface(private val mContext: Context) {

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mContext.startActivity(intent)
    }

    @JavascriptInterface
    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mContext.startActivity(intent)
    }
    
    @JavascriptInterface
    fun openUsageSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mContext.startActivity(intent)
    }

    @JavascriptInterface
    fun startBindingSession(code: String) {
        // هنا يتم تجهيز الـ Local Server لاستقبال اتصال الطفل
        // سيتم تنفيذ هذا في مرحلة الـ Networking المتقدمة
        showToast("جاري البحث عن الأجهزة المحلية... الكود: $code")
    }

    @JavascriptInterface
    fun sendCommand(cmd: String) {
        // إرسال أمر للطفل عبر الـ Service
        val serviceIntent = Intent(mContext, LocalSyncService::class.java).apply {
            action = "SEND_COMMAND"
            putExtra("command", cmd)
        }
        mContext.startService(serviceIntent)
    }
    
    @JavascriptInterface
    fun getDeviceInfo(): String {
        val info = JSONObject()
        info.put("model", android.os.Build.MODEL)
        info.put("manufacturer", android.os.Build.MANUFACTURER)
        return info.toString()
    }
}
