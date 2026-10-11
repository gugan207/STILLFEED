package com.stillfeed.app.data.remote

/**
 * Payload returned by a remote authentication provider upon successful authentication or token refresh.
 */
data class RemoteAuthResponse(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long,
    val registeredDeviceBindingId: String? = null
)

/**
 * Remote data source contract for communicating with the production identity provider.
 *
 * Keeps provider SDK details (Supabase, Firebase, or Custom HTTP client) encapsulated in the data layer.
 */
interface RemoteAuthDataSource {
    suspend fun signUp(email: String, password: CharSequence, displayName: String?): Result<RemoteAuthResponse>
    suspend fun signIn(email: String, password: CharSequence): Result<RemoteAuthResponse>
    suspend fun signOut(accessToken: String): Result<Unit>
    suspend fun refreshSession(refreshToken: String): Result<RemoteAuthResponse>
    suspend fun deleteAccount(accessToken: String): Result<Unit>
}
