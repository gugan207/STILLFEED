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

/**
 * Configuration holder for Google OAuth client settings.
 *
 * SECURITY INVARIANT:
 * - Only the Web Client ID (public identifier) is stored here.
 * - OAuth Client Secrets are NEVER included in Android or version control.
 */
object GoogleAuthConfig {
    /**
     * Web Client ID created in Google Cloud Console.
     * When configured, CredentialManager retrieves ID tokens matching this audience.
     */
    var serverClientId: String = ""
}

/**
 * Helper class orchestrating Android Credential Manager for Google ID Token acquisition.
 */
class GoogleSignInHelper(
    private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun getGoogleIdToken(): Result<String> {
        val clientId = GoogleAuthConfig.serverClientId.trim()
        if (clientId.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Google Sign-In is awaiting configuration: Enter your Google Cloud Web Client ID in GoogleAuthConfig.serverClientId, and configure the Google Provider in Supabase Dashboard."
                )
            )
        }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
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
            Result.failure(IllegalStateException("Google Sign-In error: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
