package com.abuzahra.manager

import android.content.Context
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
    fun loginUser(jsonData: String) {
        val data = JSONObject(jsonData)
        val email = data.getString("email")
        val pass = data.getString("pass")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    sendResultToUI("window.onAuthSuccess('$uid')")
                }
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
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    sendResultToUI("window.onAuthSuccess('$uid')")
                }
            } catch (e: Exception) {
                sendResultToUI("window.onAuthError('${e.message}')")
            }
        }
    }

    @JavascriptInterface
    fun startListeningForChild(code: String) {
        val uid = auth.currentUser?.uid ?: return

        val codeRef = db.collection("linking_codes").document(code)
        val codeData = hashMapOf(
            "parent_uid" to uid,
            "status" to "active",
            "created_at" to System.currentTimeMillis()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                codeRef.set(codeData).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val childrenRef = db.collection("parents").document(uid).collection("children")
        
        listenerRegistration = childrenRef.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            
            if (!snapshot.isEmpty) {
                listenerRegistration?.remove()
                sendResultToUI("window.onChildLinked()")
                
                val deviceId = snapshot.documents[0].id
                startListeningForUpdates(uid, deviceId)
            }
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
            // الوصول للـ webView أصبح ممكناً لأننا أزلنا private
            (mContext as MainActivity).webView.evaluateJavascript(jsCode, null)
        }
    }
}
