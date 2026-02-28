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
            Log.d("ParentApp", "Checking user status: ${user?.uid}")
            if (user != null) {
                sendResultToUI("window.onAuthSuccess('${user.uid}')")
            } else {
                sendResultToUI("window.showLoginScreen()")
            }
        } catch (e: Exception) {
            Log.e("ParentApp", "CheckUser Error", e)
            sendResultToUI("window.showLoginScreen()") //fallback to login
        }
    }

    @JavascriptInterface
    fun loginUser(jsonData: String) {
        try {
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
        } catch (e: Exception) {
            Log.e("ParentApp", "Login Error", e)
        }
    }

    @JavascriptInterface
    fun registerUser(jsonData: String) {
        try {
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
        } catch (e: Exception) {
            Log.e("ParentApp", "Register Error", e)
        }
    }

    @JavascriptInterface
    fun startListeningForChild(code: String) {
        try {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                sendResultToUI("window.onAuthError('User not logged in')")
                return
            }

            val codeRef = db.collection("linking_codes").document(code)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    codeRef.set(mapOf("parent_uid" to uid)).await()
                } catch (e: Exception) {
                    Log.e("ParentApp", "Firestore Set Error", e)
                }
            }

            listenerRegistration = db.collection("parents").document(uid).collection("children")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("ParentApp", "Snapshot Error", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        listenerRegistration?.remove()
                        sendResultToUI("window.onChildLinked()")
                        val deviceId = snapshot.documents[0].id
                        startListeningForUpdates(uid, deviceId)
                    }
                }
        } catch (e: Exception) {
            Log.e("ParentApp", "Binding Error", e)
        }
    }
    
    private fun startListeningForUpdates(parentUid: String, deviceId: String) {
        val deviceRef = db.collection("parents").document(parentUid).collection("children").document(deviceId)
        deviceRef.addSnapshotListener { doc, e ->
            if (e != null || doc == null || !doc.exists()) return@addSnapshotListener
            val battery = doc.getLong("battery_level")?.toInt() ?: 0
            val json = JSONObject().put("battery", battery).toString()
            sendResultToUI("window.updateChildData('$json')")
        }
    }

    private fun sendResultToUI(jsCode: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (mContext is MainActivity) {
                    mContext.webView.evaluateJavascript(jsCode, null)
                }
            } catch (e: Exception) {
                Log.e("ParentApp", "UI Update Error", e)
            }
        }
    }
}
