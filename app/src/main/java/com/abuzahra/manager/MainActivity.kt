package com.abuzahra.manager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    lateinit var webView: WebView
    private val PERMISSIONS_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. طلب الأذونات المطلوبة فوراً
        requestRuntimePermissions()

        webView = findViewById(R.id.webview)
        setupWebView()
    }

    private fun requestRuntimePermissions() {
        val permissionsNeeded = mutableListOf<String>()

        // قائمة الأذونات المطلوبة
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        // إضافة إذن الإشعارات للأندرويد 13 فما فوق
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // للإصدارات الحديثة من أندرويد 13+ لا نحتاج READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             permissions.remove(Manifest.permission.READ_EXTERNAL_STORAGE)
             permissions.remove(Manifest.permission.WRITE_EXTERNAL_STORAGE)
             permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun setupWebView() {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true
        
        // إضافة الواجهة قبل التحميل
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidNative")
        webView.webViewClient = WebViewClient()
        
        // تحميل الصفحة
        webView.loadUrl("file:///android_asset/index.html")
    }
}
