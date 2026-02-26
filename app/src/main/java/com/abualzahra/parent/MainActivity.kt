package com.abualzahra.parent

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.webkit.JavascriptInterface
import android.widget.Toast
import android.content.Intent
import android.provider.Settings
import android.net.Uri

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        
        // إعدادات الويب
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true // مهم لعمل Firebase
        webSettings.databaseEnabled = true
        webSettings.setSupportZoom(false)
        
        // السماح بالوصول من ملفات الأصول (Assets)
        webSettings.allowFileAccess = true
        
        // ربط الواجهة الأصلية بالويب (Native Bridge)
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidNative")

        // تحميل الصفحة من مجلد assets
        webView.setWebViewClient(WebViewClient())
        webView.loadUrl("file:///android_asset/index.html")
    }

    // التعامل مع زر الرجوع
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
