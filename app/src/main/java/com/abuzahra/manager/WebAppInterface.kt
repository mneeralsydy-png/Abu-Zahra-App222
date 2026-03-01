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
            Log.d("ParentApp", "User status check: ${user?.uid}")
            if (user != null) {
                sendResultToUI("window.onAuthSuccess('${user.uid}')")
            } else {
                sendResultToUI("window.showLoginScreen()")
            }
        } catch (e: Exception) {
            Log.e("ParentApp", "CheckUser Error", e)
            sendResultToUI("window.showLoginScreen()") // Fallback
        }
    }

    @JavascriptInterface
    fun loginUser(jsonData: String) {
        val data = JSONObject(jsonData)
        val email = data.getString("email")
        val pass = data.getString("pass")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                sendResultToUI("window.onAuthSuccess('${auth.currentUser?.uid}')")
            } catch (e: Exception) {
                sendResultToUI("window.onAuthError('${e.message}')")
            }
        }
    }

    @JavascriptInterface
    fun registerUser(jsonData: String) {
        val data = JSONObject(jsonData)
        val email = data.getString("email")
        val pass = data.getString("pass")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
                sendResultToUI("window.onAuthSuccess('${auth.currentUser?.uid}')")
            } catch (e: Exception) {
                sendResultToUI("window.onAuthError('${e.message}')")
            }
        }
    }
    
    @JavascriptInterface
    fun loginWithGoogle() {
        // استدعاء الدالة في MainActivity
        if (mContext is MainActivity) {
            mContext.startGoogleSignIn()
        }
    }

    @JavascriptInterface
    fun startListeningForChild(code: String) {
        val uid = auth.currentUser?.uid ?: return
        val codeRef = db.collection("linking_codes").document(code)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                codeRef.set(mapOf("parent_uid" to uid)).await()
            } catch (e: Exception) {
                Log.e("ParentApp", "Failed to set code", e)
            }
        }

        listenerRegistration = db.collection("parents").document(uid).collection("children")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                if (!snapshot.isEmpty) {
                    listenerRegistration?.remove()
                    sendResultToUI("window.onChildLinked()")
                }
            }
    }

    private fun sendResultToUI(jsCode: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (mContext is MainActivity) {
                    mContext.webView.evaluateJavascript(jsCode, null)
                }
            } catch (e: Exception) {
                Log.e("ParentApp", "UI Update Failed", e)
            }
        }
    }
}
