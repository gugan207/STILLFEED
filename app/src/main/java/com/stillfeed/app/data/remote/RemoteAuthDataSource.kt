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
    val registeredDeviceBindingId: String? = null,
    val requiresEmailVerification: Boolean = false,
    val isExistingUser: Boolean = false
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

    suspend fun verifyEmailOtp(email: String, token: String, type: String = "signup"): Result<RemoteAuthResponse> =
        Result.failure(UnsupportedOperationException("Email OTP verification not implemented"))

    suspend fun resendOtp(email: String, type: String = "signup"): Result<Unit> =
        Result.failure(UnsupportedOperationException("Resend OTP not implemented"))

    suspend fun recoverPassword(email: String, redirectTo: String? = null): Result<Unit> =
        Result.failure(UnsupportedOperationException("Password recovery not implemented"))

    suspend fun updatePassword(accessToken: String, newPassword: CharSequence): Result<Unit> =
        Result.failure(UnsupportedOperationException("Update password not implemented"))

    suspend fun signInWithGoogle(idToken: String): Result<RemoteAuthResponse> =
        Result.failure(UnsupportedOperationException("Google Sign-In not implemented"))
}
