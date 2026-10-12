package com.stillfeed.app.auth.google

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

import android.util.Log

/**
 * Configuration holder for Google OAuth client settings.
 *
 * SECURITY INVARIANT:
 * - Only the Web Client ID (public identifier) is stored here.
 * - OAuth Client Secrets are NEVER included in Android or version control.
 */
object GoogleAuthConfig {
    const val DEFAULT_SERVER_CLIENT_ID: String =
        "92258811699-dj7fh1fc8lh460h1rtm1f8k0f20pdk4m.apps.googleusercontent.com"

    /**
     * Web Client ID created in Google Cloud Console.
     * When configured, CredentialManager retrieves ID tokens matching this audience.
     */
    var serverClientId: String = DEFAULT_SERVER_CLIENT_ID
}

/**
 * Helper class orchestrating Android Credential Manager for Google ID Token acquisition.
 */
class GoogleSignInHelper(
    private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun getGoogleIdToken(nonce: String? = null): Result<String> {
        val clientId = GoogleAuthConfig.serverClientId.trim()
        if (clientId.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Google Sign-In is not configured: Enter your Google Cloud Web Client ID in GoogleAuthConfig.serverClientId."
                )
            )
        }

        return try {
            val optionBuilder = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)

            if (!nonce.isNullOrBlank()) {
                optionBuilder.setNonce(nonce)
            }

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(optionBuilder.build())
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(googleIdTokenCredential.idToken)
            } else {
                Result.failure(IllegalArgumentException("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: NoCredentialException) {
            Result.failure(IllegalStateException("No Google accounts available on this device.", e))
        } catch (e: GetCredentialException) {
            Log.e("GoogleSignInHelper", "Credential Manager failed: ${e.message}", e)
            Result.failure(IllegalStateException("Google Sign-In error: ${e.message}", e))
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "Unexpected exception during Google Sign-In", e)
            Result.failure(e)
        }
    }
}

