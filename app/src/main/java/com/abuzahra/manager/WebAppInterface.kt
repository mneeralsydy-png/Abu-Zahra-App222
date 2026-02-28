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

    @JavascriptInterface
    fun checkUserStatus() {
        val user = auth.currentUser
        if (user != null) {
            // إذا المستخدم مسجل دخوله، نبحث عن كوده القديم أو ننشئ جديداً
            getOrGenerateCode(user.uid)
        } else {
            sendResultToUI("window.showLoginScreen()")
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
                getOrGenerateCode(auth.currentUser?.uid!!)
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
                getOrGenerateCode(auth.currentUser?.uid!!)
            } catch (e: Exception) {
                sendResultToUI("window.onAuthError('${e.message}')")
            }
        }
    }

    @JavascriptInterface
    fun loginGoogle() {
        if (mContext is MainActivity) {
            mContext.startGoogleSignIn()
        }
    }

    // المنطق الجديد: كود واحد لكل مستخدم
    private fun getOrGenerateCode(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. البحث عن كود موجود مسبقاً لهذا المستخدم
                val query = db.collection("linking_codes")
                    .whereEqualTo("parent_uid", uid)
                    .limit(1)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    // وجد كود قديم -> نعرضه
                    val existingCode = query.documents[0].id
                    sendResultToUI("window.onAuthSuccess('$uid')")
                    sendResultToUI("window.showBindingCode('$existingCode')")
                } else {
                    // لم يجد كود -> نولد كوداً جديداً ونحفظه للأبد
                    val newCode = generateRandomCode()
                    val codeData = mapOf("parent_uid" to uid, "created_at" to System.currentTimeMillis())
                    db.collection("linking_codes").document(newCode).set(codeData).await()
                    
                    sendResultToUI("window.onAuthSuccess('$uid')")
                    sendResultToUI("window.showBindingCode('$newCode')")
                }
            } catch (e: Exception) {
                Log.e("ParentApp", "Error getting code", e)
                sendResultToUI("window.onAuthError('خطأ في جلب الكود: ${e.message}')")
            }
        }
    }

    private fun generateRandomCode(): String {
        return (100000000..999999999).random().toString()
    }

    private fun sendResultToUI(jsCode: String) {
        CoroutineScope(Dispatchers.Main).launch {
            (mContext as MainActivity).webView.evaluateJavascript(jsCode, null)
        }
    }
}
