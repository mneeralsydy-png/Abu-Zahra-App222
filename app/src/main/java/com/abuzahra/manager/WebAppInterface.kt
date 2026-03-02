package com.abuzahra.manager

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
        val user = auth.currentUser
        if (user == null) {
            sendResultToUI("window.showLoginScreen()")
            return
        }

        val uid = user.uid
        val parentRef = db.collection("parents").document(uid)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. تحقق إذا كان الطفل مربوطاً بالفعل
                val childrenSnapshot = parentRef.collection("children").limit(1).get().await()
                if (!childrenSnapshot.isEmpty) {
                    // مربوط -> أرسل الإعدادات للواجهة لتشغيل المستمعين (Listeners)
                    val deviceId = childrenSnapshot.documents[0].id
                    val email = user.email ?: ""
                    val json = JSONObject()
                    json.put("parentId", uid)
                    json.put("deviceId", deviceId)
                    json.put("email", email)
                    sendResultToUI("window.onChildLinked('$json')")
                    return@launch
                }

                // 2. غير مربوط -> تحقق من كود الربط
                val parentDoc = parentRef.get().await()
                val existingCode = parentDoc.getString("binding_code")
                
                if (!existingCode.isNullOrEmpty() && existingCode.length == 9) {
                    sendResultToUI("window.onAuthSuccess('$existingCode')")
                } else {
                    val newCode = (100000000..999999999).random().toString()
                    parentRef.set(mapOf("binding_code" to newCode), SetOptions.merge()).await()
                    sendResultToUI("window.onAuthSuccess('$newCode')")
                }

            } catch (e: Exception) {
                Log.e("ParentApp", "Error checking status", e)
                sendResultToUI("window.showLoginScreen()")
            }
        }
    }

    @JavascriptInterface
    fun loginUser(jsonData: String) {
        val data = JSONObject(jsonData)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.signInWithEmailAndPassword(data.getString("email"), data.getString("pass")).await()
                checkUserStatus()
            } catch (e: Exception) { sendResultToUI("window.onAuthError('${e.message}')") }
        }
    }

    @JavascriptInterface
    fun registerUser(jsonData: String) {
        val data = JSONObject(jsonData)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.createUserWithEmailAndPassword(data.getString("email"), data.getString("pass")).await()
                checkUserStatus()
            } catch (e: Exception) { sendResultToUI("window.onAuthError('${e.message}')") }
        }
    }
    
    @JavascriptInterface
    fun loginWithGoogle() {
        if (mContext is MainActivity) mContext.startGoogleSignIn()
    }

    @JavascriptInterface
    fun startListeningForChild(code: String) {
        val uid = auth.currentUser?.uid ?: return
        val codeRef = db.collection("linking_codes").document(code)
        
        CoroutineScope(Dispatchers.IO).launch {
            try { codeRef.set(mapOf("parent_uid" to uid)).await() } catch (e: Exception) { Log.e("ParentApp", "Code write failed", e) }
        }

        listenerRegistration = db.collection("parents").document(uid).collection("children")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                if (!snapshot.isEmpty) {
                    listenerRegistration?.remove()
                    val deviceId = snapshot.documents[0].id
                    val email = auth.currentUser?.email ?: ""
                    val json = JSONObject()
                    json.put("parentId", uid)
                    json.put("deviceId", deviceId)
                    json.put("email", email)
                    sendResultToUI("window.onChildLinked('$json')")
                }
            }
    }

    private fun sendResultToUI(jsCode: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try { (mContext as MainActivity).webView.evaluateJavascript(jsCode, null) } catch (e: Exception) {}
        }
    }
}
