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

    // تسجيل الدخول
    @JavascriptInterface
    fun loginUser(jsonData: String) {
        val data = JSONObject(jsonData)
        val email = data.getString("email")
        val pass = data.getString("pass")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    (mContext as MainActivity).webView.evaluateJavascript("window.onAuthSuccess('$uid')", null)
                }
            } catch (e: Exception) {
                (mContext as MainActivity).webView.evaluateJavascript("window.onAuthError('${e.message}')", null)
            }
        }
    }

    // إنشاء حساب
    @JavascriptInterface
    fun registerUser(jsonData: String) {
        val data = JSONObject(jsonData)
        val email = data.getString("email")
        val pass = data.getString("pass")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    (mContext as MainActivity).webView.evaluateJavascript("window.onAuthSuccess('$uid')", null)
                }
            } catch (e: Exception) {
                (mContext as MainActivity).webView.evaluateJavascript("window.onAuthError('${e.message}')", null)
            }
        }
    }

    // بدء الاستماع لربط الطفل
    @JavascriptInterface
    fun startListeningForChild(code: String) {
        val uid = auth.currentUser?.uid ?: return

        // 1. حفظ الكود في Firestore
        val codeRef = db.collection("linking_codes").document(code)
        val codeData = hashMapOf(
            "parent_uid" to uid,
            "status" to "active",
            "created_at" to System.currentTimeMillis()
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            codeRef.set(codeData).await()
        }

        // 2. الاستماع للتغييرات في مسار الأطفال
        val childrenRef = db.collection("parents").document(uid).collection("children")
        
        listenerRegistration = childrenRef.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            
            if (!snapshot.isEmpty) {
                // يوجد طفل مربوط!
                listenerRegistration?.remove() // إيقاف الاستماع
                
                // إرسال أمر للواجهة للانتقال للوحة التحكم
                CoroutineScope(Dispatchers.Main).launch {
                    (mContext as MainActivity).webView.evaluateJavascript("window.onChildLinked()", null)
                }
                
                // بدء الاستماع لتحديثات الطفل (مثل البطارية)
                startListeningForUpdates(uid, snapshot.documents[0].id)
            }
        }
    }

    private fun startListeningForUpdates(parentUid: String, deviceId: String) {
        val deviceRef = db.collection("parents").document(parentUid).collection("children").document(deviceId)
        deviceRef.addSnapshotListener { doc, e ->
            if (e != null || doc == null || !doc.exists()) return@addSnapshotListener
            
            val battery = doc.getLong("battery_level")?.toInt() ?: 0
            val json = JSONObject().put("battery", battery).toString()
            
            CoroutineScope(Dispatchers.Main).launch {
                (mContext as MainActivity).webView.evaluateJavascript("window.updateChildData('$json')", null)
            }
        }
    }
}
