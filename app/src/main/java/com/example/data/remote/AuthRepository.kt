package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
    context: Context,
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    ),
) {
    private val appContext = context.applicationContext

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun attemptAutoSignIn(activityContext: Context): FirebaseUser? {
        if (auth.currentUser != null) return auth.currentUser
        val webClientId = appContext.getString(R.string.default_web_client_id)
        if (webClientId.isBlank()) return null

        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCred).await()
                authResult.user?.also { user ->
                    syncUserProfileSafe(user)
                }
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            null
        } catch (e: Exception) {
            Log.d(TAG, "Silent Google auto sign-in skipped: ${e.message}")
            null
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        val webClientId = appContext.getString(R.string.default_web_client_id)
        if (webClientId.isBlank()) {
            return Result.failure(IllegalStateException("Konfigurasi Web Client ID belum tersedia."))
        }

        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCred).await()
                val user = authResult.user
                    ?: return Result.failure(IllegalStateException("Gagal memuat profil akun Google."))
                syncUserProfileSafe(user)
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Jenis kredensial Google tidak dikenali."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google sign-in cancelled or credential config rejected", e)
            Result.failure(IllegalStateException("Login Google dibatalkan. Silakan pilih akun Google untuk melanjutkan."))
        } catch (e: NoCredentialException) {
            Result.failure(
                IllegalStateException(
                    "Tidak ada akun Gmail/Google yang aktif di perangkat. Tambahkan akun Google di Pengaturan HP lalu coba lagi."
                )
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "CredentialManager error during sign-in", e)
            Result.failure(
                IllegalStateException(
                    e.errorMessage?.toString() ?: "Gagal masuk dengan akun Google."
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google sign-in", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Terjadi kesalahan saat masuk dengan Google."
                )
            )
        }
    }

    suspend fun signOut(activityContext: Context) {
        withContext(NonCancellable) {
            auth.signOut()
            runCatching {
                val credentialManager = CredentialManager.create(activityContext)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }.onFailure { err ->
                Log.w(TAG, "Failed to clear credential state on sign-out", err)
            }
        }
    }

    private suspend fun syncUserProfileSafe(user: FirebaseUser) {
        val docPath = "users/${user.uid}"
        val docRef = db.collection("users").document(user.uid)
        val profile = FirestoreUserProfile(
            id = user.uid,
            uid = user.uid,
            email = user.email.orEmpty(),
            displayName = user.displayName?.ifBlank { null } ?: (user.email?.substringBefore('@') ?: "Pengguna Kangbas"),
            photoUrl = user.photoUrl?.toString(),
        )
        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(profile.toCreateMap()).await()
            } else {
                docRef.update(profile.toUpdateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not sync user profile at $docPath: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
