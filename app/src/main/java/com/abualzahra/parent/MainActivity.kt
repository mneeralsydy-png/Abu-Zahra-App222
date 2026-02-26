package com.abuzahra.manager

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        setupWebView()
        
        // التحقق من حالة المستخدم عند البدء
        checkUserStatus()
    }

    private fun setupWebView() {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidNative")
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun checkUserStatus() {
        // سيتم التعامل مع الحالة عبر استدعاءات JS بعد تحميل الصفحة
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
