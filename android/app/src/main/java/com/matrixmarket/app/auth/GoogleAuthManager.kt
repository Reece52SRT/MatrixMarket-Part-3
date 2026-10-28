package com.matrixmarket.app.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.matrixmarket.app.BuildConfig
import kotlinx.coroutines.tasks.await

private const val TAG = "GoogleAuthManager"

// Single sign-on with Google through Firebase Authentication.
// Returns a Firebase ID token which our API verifies SERVER-SIDE before it issues
// its own app token - the client never just "tells" the API who it is.
class GoogleAuthManager(private val context: Context) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }

    // Clears the cached Google account so the account picker always appears.
    suspend fun getSignInIntent(): Intent {
        try {
            googleSignInClient.signOut().await()
        } catch (e: Exception) {
            Log.w(TAG, "Sign-out before sign-in failed (safe to ignore)", e)
        }
        return googleSignInClient.signInIntent
    }

    class SsoCancelledException : Exception("Sign-in cancelled")

    suspend fun handleSignInResult(data: Intent?): Result<String> {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
                ?: return Result.failure(Exception("Google Sign-In returned no account"))
            val googleIdToken = account.idToken
                ?: return Result.failure(Exception("Google did not return an ID token"))

            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val firebaseUser = FirebaseAuth.getInstance().signInWithCredential(credential).await().user
                ?: return Result.failure(Exception("Firebase sign-in returned no user"))

            val firebaseIdToken = firebaseUser.getIdToken(true).await().token
                ?: return Result.failure(Exception("Firebase did not return an ID token"))

            Log.i(TAG, "Google SSO succeeded for Firebase uid ${firebaseUser.uid}")
            Result.success(firebaseIdToken)
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in failed with status ${e.statusCode}", e)
            if (e.statusCode == CommonStatusCodes.CANCELED || e.statusCode == 12501) {
                return Result.failure(SsoCancelledException())
            }
            val message = when (e.statusCode) {
                CommonStatusCodes.DEVELOPER_ERROR ->
                    "Google sign-in is not set up for this app build (code 10). Add this build's SHA-1 fingerprint in Firebase."
                CommonStatusCodes.NETWORK_ERROR -> "No internet connection. Please try again."
                else -> "Google sign-in failed (code ${e.statusCode})."
            }
            Result.failure(Exception(message, e))
        } catch (e: Exception) {
            Log.e(TAG, "Firebase credential sign-in failed", e)
            Result.failure(Exception("Google sign-in failed. Please try again.", e))
        }
    }

    fun signOut() {
        try {
            googleSignInClient.signOut()
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Sign-out failed", e)
        }
    }
}
