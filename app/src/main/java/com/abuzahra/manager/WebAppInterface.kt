package com.abuzahra.manager

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class WebAppInterface(private val mContext: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    @JavascriptInterface
    fun checkUserStatus() {
        try {
            val user = auth.currentUser
            if (user != null) sendResultToUI("window.onAuthSuccess('${user.uid}')")
            else sendResultToUI("window.showLoginScreen()")
        } catch (e: Exception) { sendResultToUI("window.showLoginScreen()") }
    }

    @JavascriptInterface
    fun loginUser(jsonData: String) {
        val data = JSONObject(jsonData)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.signInWithEmailAndPassword(data.getString("email"), data.getString("pass")).await()
                sendResultToUI("window.onAuthSuccess('${auth.currentUser?.uid}')")
            } catch (e: Exception) { sendResultToUI("window.onAuthError('${e.message}')") }
        }
    }

    @JavascriptInterface
    fun registerUser(jsonData: String) {
        val data = JSONObject(jsonData)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.createUserWithEmailAndPassword(data.getString("email"), data.getString("pass")).await()
                sendResultToUI("window.onAuthSuccess('${auth.currentUser?.uid}')")
            } catch (e: Exception) { sendResultToUI("window.onAuthError('${e.message}')") }
        }
    }
    
    @JavascriptInterface
    fun loginWithGoogle() {
        if (mContext is MainActivity) mContext.startGoogleSignIn()
    }

    @JavascriptInterface
    fun startListeningForChild(code: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            sendResultToUI("window.onAuthError('المستخدم غير مسجل الدخول')")
            return
        }

        val codeRef = db.collection("linking_codes").document(code)
        
        // 1. إنشاء/تحديث الكود
        CoroutineScope(Dispatchers.IO).launch {
            try { 
                codeRef.set(mapOf("parent_uid" to uid)).await()
                Log.d("ParentApp", "Code written successfully")
            } catch (e: Exception) { 
                Log.e("ParentApp", "Failed to write code", e)
                sendResultToUI("window.onAuthError('فشل كتابة الكود: ${e.message}')")
            }
        }

        // 2. التأكد من وجود مستند الوالد (مهم جداً)
        val parentRef = db.collection("parents").document(uid)
        CoroutineScope(Dispatchers.IO).launch {
             try {
                 parentRef.set(mapOf("exists" to true), com.google.firebase.firestore.SetOptions.merge()).await()
             } catch (e: Exception) { Log.e("ParentApp", "Parent doc creation failed", e) }
        }

        // 3. بدء الاستماع
        listenerRegistration = db.collection("parents").document(uid).collection("children")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ParentApp", "Listener FAILED", e)
                    // إرسال رسالة الخطأ للواجهة لنعرف السبب
                    sendResultToUI("window.onAuthError('خطأ في المستمع: ${e.message}')")
                    return@addSnapshotListener
                }
                
                if (snapshot == null) return@addSnapshotListener
                
                Log.d("ParentApp", "Snapshot size: ${snapshot.size()}")
                
                if (!snapshot.isEmpty) {
                    listenerRegistration?.remove()
                    sendResultToUI("window.onChildLinked()")
                }
            }
    }

    private fun sendResultToUI(jsCode: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try { (mContext as MainActivity).webView.evaluateJavascript(jsCode, null) } catch (e: Exception) {}
        }
    }
}
