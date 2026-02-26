package com.abualzahra.parent

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.json.JSONObject
import com.abualzahra.parent.services.LocalSyncService

class WebAppInterface(private val mContext: Context) {

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()
    }

    // التحقق مما إذا كانت صلاحية الموقع مفعلة حقاً
    @JavascriptInterface
    fun isLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // فتح إعدادات الموقع
    @JavascriptInterface
    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mContext.startActivity(intent)
    }
    
    // فتح إعدادات التطبيق (لإعطاء صلاحيات يدوياً إذا رفضها المستخدم)
    @JavascriptInterface
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = android.net.Uri.fromParts("package", mContext.packageName, null)
        intent.data = uri
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mContext.startActivity(intent)
    }

    @JavascriptInterface
    fun startBindingSession(code: String) {
        Toast.makeText(mContext, "جاري تشغيل خدمة الاكتشاف... الكود: $code", Toast.LENGTH_LONG).show()
        // هنا يمكنك بدء الـ Service التي تستمع للاتصال المحلي
        val serviceIntent = Intent(mContext, LocalSyncService::class.java).apply {
            action = "START_LISTENING"
            putExtra("code", code)
        }
        mContext.startService(serviceIntent)
    }

    @JavascriptInterface
    fun sendCommand(cmd: String) {
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
        info.put("os_version", android.os.Build.VERSION.RELEASE)
        return info.toString()
    }
}
