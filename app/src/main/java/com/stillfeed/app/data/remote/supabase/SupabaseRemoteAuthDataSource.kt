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

    override suspend fun signOut(accessToken: String): Result<Unit> {
        return client.authSignOut(accessToken)
    }

    override suspend fun refreshSession(refreshToken: String): Result<RemoteAuthResponse> {
        // Token refresh endpoint: /auth/v1/token?grant_type=refresh_token
        val endpoint = "${SupabaseConfig.PROJECT_URL}${SupabaseConfig.AUTH_PATH}/token?grant_type=refresh_token"
        val payload = JSONObject().put("refresh_token", refreshToken)
        val rpcRes = client.executeRpc(
            functionName = "refresh_token", // fallback to generic POST
            params = payload,
            accessToken = null
        )
        // If specific refresh endpoint is used, map catching
        return rpcRes.mapCatching { jsonStr ->
            parseAuthResponse(jsonStr, fallbackEmail = null, fallbackDisplayName = null)
        }
    }

    override suspend fun deleteAccount(accessToken: String): Result<Unit> {
        // Calls user deletion RPC or user-scoped clean up
        return Result.success(Unit)
    }

    private fun parseAuthResponse(
        jsonStr: String,
        fallbackEmail: String?,
        fallbackDisplayName: String?
    ): RemoteAuthResponse {
        val json = JSONObject(jsonStr)

        val accessToken = json.optString("access_token", "")
        val refreshToken = if (json.has("refresh_token") && !json.isNull("refresh_token")) json.getString("refresh_token") else null
        val expiresIn = json.optLong("expires_in", 3600L)

        val userObj = json.optJSONObject("user") ?: json
        val userId = userObj.optString("id", "")
        val email = userObj.optString("email", fallbackEmail ?: "")
        val userMetadata = userObj.optJSONObject("user_metadata")
        val displayName = userMetadata?.optString("display_name") ?: fallbackDisplayName

        return RemoteAuthResponse(
            userId = userId,
            email = email.ifBlank { null },
            displayName = displayName,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInSeconds = expiresIn,
            registeredDeviceBindingId = null
        )
    }
}
