package com.abualzahra.parent

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.widget.Toast
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Build

class WebAppInterface(private val mContext: Context) {

    // دالة لعرض رسائل من الويب
    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()
    }

    // دالة لفتح إعدادات الوصول (Accessibility)
    @JavascriptInterface
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mContext.startActivity(intent)
    }

    // دالة لفتح إعدادات الموقع
    @JavascriptInterface
    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mContext.startActivity(intent)
    }
    
    // دالة للتحقق من حالة الجهاز (للتطوير المستقبلي)
    @JavascriptInterface
    fun getDeviceInfo(): String {
        return android.os.Build.MODEL
    }
}
