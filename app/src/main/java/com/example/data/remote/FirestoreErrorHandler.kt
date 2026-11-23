package com.example.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    LIST,
    GET,
    WRITE,
}

fun handleFirestoreError(
    error: Exception,
    operationType: OperationType,
    path: String?,
    auth: FirebaseAuth,
): Nothing {
    if (error is CancellationException) throw error

    val user = auth.currentUser
    val payload = JSONObject().apply {
        put("error", error.message ?: "Unknown Firestore error")
        put("operationType", operationType.name.lowercase())
        put("path", path ?: "")
        put(
            "authInfo",
            JSONObject().apply {
                put("userId", user?.uid ?: "")
                put("email", user?.email ?: "")
                put("emailVerified", user?.isEmailVerified ?: false)
                put("isAnonymous", user?.isAnonymous ?: false)
            },
        )
    }.toString()

    Log.e("FirestoreError", "Firestore Error: $payload", error)
    throw IllegalStateException(
        error.localizedMessage ?: "Gagal menyinkronkan data dengan Cloud Firestore ($operationType).",
        error,
    )
}
