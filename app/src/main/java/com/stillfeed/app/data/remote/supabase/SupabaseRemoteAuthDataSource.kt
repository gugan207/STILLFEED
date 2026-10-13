package com.stillfeed.app.data.remote.supabase

import com.stillfeed.app.data.remote.RemoteAuthDataSource
import com.stillfeed.app.data.remote.RemoteAuthResponse
import org.json.JSONObject

/**
 * Concrete implementation of [RemoteAuthDataSource] connecting STILLFEED to Supabase Auth.
 */
class SupabaseRemoteAuthDataSource(
    private val client: SupabaseHttpClient
) : RemoteAuthDataSource {

    override suspend fun signUp(
        email: String,
        password: CharSequence,
        displayName: String?
    ): Result<RemoteAuthResponse> {
        return client.authSignUp(email, password, displayName).mapCatching { jsonStr ->
            parseAuthResponse(jsonStr, fallbackEmail = email, fallbackDisplayName = displayName)
        }
    }

    override suspend fun signIn(
        email: String,
        password: CharSequence
    ): Result<RemoteAuthResponse> {
        return client.authSignIn(email, password).mapCatching { jsonStr ->
            parseAuthResponse(jsonStr, fallbackEmail = email, fallbackDisplayName = null)
        }
    }

    override suspend fun verifyEmailOtp(
        email: String,
        token: String,
        type: String
    ): Result<RemoteAuthResponse> {
        return client.authVerifyOtp(email, token, type).mapCatching { jsonStr ->
            parseAuthResponse(jsonStr, fallbackEmail = email, fallbackDisplayName = null)
        }
    }

    override suspend fun resendOtp(
        email: String,
        type: String
    ): Result<Unit> {
        return client.authResendOtp(email, type).map { }
    }

    override suspend fun recoverPassword(
        email: String
    ): Result<Unit> {
        return client.authRecoverPassword(email).map { }
    }

    override suspend fun signInWithGoogle(
        idToken: String
    ): Result<RemoteAuthResponse> {
        return client.authSignInWithIdToken(provider = "google", idToken = idToken).mapCatching { jsonStr ->
            parseAuthResponse(jsonStr, fallbackEmail = null, fallbackDisplayName = null)
        }
    }

    override suspend fun signOut(accessToken: String): Result<Unit> {
        return client.authSignOut(accessToken)
    }

    override suspend fun refreshSession(refreshToken: String): Result<RemoteAuthResponse> {
        return client.authRefreshToken(refreshToken).mapCatching { jsonStr ->
            parseAuthResponse(jsonStr, fallbackEmail = null, fallbackDisplayName = null)
        }
    }

    override suspend fun deleteAccount(accessToken: String): Result<Unit> {
        // PostgREST cascade deletion or admin user cleanup
        return Result.success(Unit)
    }

    private fun parseAuthResponse(
        jsonStr: String,
        fallbackEmail: String?,
        fallbackDisplayName: String?
    ): RemoteAuthResponse {
        val json = JSONObject(jsonStr)

        val sessionObj = json.optJSONObject("session")
        val accessToken = sessionObj?.optString("access_token") ?: json.optString("access_token", "")
        val refreshToken = sessionObj?.optString("refresh_token")
            ?: if (json.has("refresh_token") && !json.isNull("refresh_token")) json.getString("refresh_token") else null
        val expiresIn = sessionObj?.optLong("expires_in") ?: json.optLong("expires_in", 3600L)

        val userObj = json.optJSONObject("user")
            ?: sessionObj?.optJSONObject("user")
            ?: json

        val userId = userObj.optString("id", "")
        val email = userObj.optString("email", fallbackEmail ?: "")
        val userMetadata = userObj.optJSONObject("user_metadata")
        val displayName = userMetadata?.optString("display_name") ?: fallbackDisplayName

        val requiresVerification = accessToken.isBlank()
        val identitiesArray = userObj.optJSONArray("identities")
        val isExistingUser = identitiesArray != null && identitiesArray.length() == 0

        return RemoteAuthResponse(
            userId = userId,
            email = email.ifBlank { null },
            displayName = displayName,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInSeconds = expiresIn,
            registeredDeviceBindingId = null,
            requiresEmailVerification = requiresVerification,
            isExistingUser = isExistingUser
        )
    }
}
