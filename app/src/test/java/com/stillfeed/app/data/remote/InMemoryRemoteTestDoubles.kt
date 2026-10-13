package com.stillfeed.app.data.remote

import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.User
import com.stillfeed.app.domain.repository.RemoteHistoryRepository
import com.stillfeed.app.domain.repository.RemoteUserRepository
import java.io.IOException
import java.util.UUID

/**
 * In-memory test double for [RemoteAuthDataSource] simulating remote identity provider behaviors.
 */
class InMemoryRemoteAuthDataSource : RemoteAuthDataSource {

    private val accounts = mutableMapOf<String, StoredAccount>()

    var simulateNetworkError: Boolean = false
    var simulateMalformedResponse: Boolean = false
    var simulateAccountAlreadyExists: Boolean = false
    var simulateInvalidCredentials: Boolean = false
    var simulateExpiredRefreshToken: Boolean = false
    var simulateExistingUserResponse: Boolean = false
    var simulateSmtp550Error: Boolean = false
    var simulateConfirmationMailError: Boolean = false

    var signedOutTokens = mutableListOf<String>()
    var deletedTokens = mutableListOf<String>()

    data class StoredAccount(
        val userId: String,
        val email: String,
        val password: String,
        val displayName: String?,
        var registeredDeviceBindingId: String? = null
    )

    fun registerExistingAccount(
        email: String,
        password: String,
        userId: String = "user_${UUID.randomUUID().toString().take(8)}",
        displayName: String? = null,
        deviceBindingId: String? = null
    ): StoredAccount {
        val account = StoredAccount(userId, email.trim().lowercase(), password, displayName, deviceBindingId)
        accounts[email.trim().lowercase()] = account
        return account
    }

    override suspend fun signUp(
        email: String,
        password: CharSequence,
        displayName: String?
    ): Result<RemoteAuthResponse> {
        if (simulateNetworkError) {
            return Result.failure(IOException("Network error: connection timed out"))
        }
        if (simulateSmtp550Error) {
            return Result.failure(IllegalArgumentException("gomail: could not send email 1: 550 You can only send testing emails to your own email address"))
        }
        if (simulateConfirmationMailError) {
            return Result.failure(IllegalArgumentException("Error sending confirmation mail"))
        }
        if (simulateMalformedResponse) {
            return Result.failure(IllegalArgumentException("Malformed JSON received from authentication service"))
        }
        val normalized = email.trim().lowercase()
        if (simulateExistingUserResponse) {
            return Result.success(
                RemoteAuthResponse(
                    userId = "user_existing_123",
                    email = normalized,
                    displayName = displayName,
                    accessToken = "",
                    refreshToken = null,
                    expiresInSeconds = 3600L,
                    requiresEmailVerification = true,
                    isExistingUser = true
                )
            )
        }
        if (simulateAccountAlreadyExists || accounts.containsKey(normalized)) {
            return Result.failure(IllegalArgumentException("User already exists with this email address"))
        }

        val userId = "user_${UUID.randomUUID().toString().take(8)}"
        val account = StoredAccount(userId, normalized, password.toString(), displayName)
        accounts[normalized] = account

        return Result.success(
            RemoteAuthResponse(
                userId = userId,
                email = normalized,
                displayName = displayName,
                accessToken = "jwt_access_${UUID.randomUUID()}",
                refreshToken = "jwt_refresh_${UUID.randomUUID()}",
                expiresInSeconds = 3600L,
                registeredDeviceBindingId = null
            )
        )
    }

    override suspend fun signIn(
        email: String,
        password: CharSequence
    ): Result<RemoteAuthResponse> {
        if (simulateNetworkError) {
            return Result.failure(IOException("Network error: connection timed out"))
        }
        if (simulateMalformedResponse) {
            return Result.failure(IllegalArgumentException("Malformed JSON received from authentication service"))
        }
        if (simulateInvalidCredentials) {
            return Result.failure(IllegalArgumentException("Invalid login credentials provided"))
        }

        val normalized = email.trim().lowercase()
        val account = accounts[normalized]
            ?: return Result.failure(IllegalArgumentException("Invalid login credentials: user not found"))

        if (account.password != password.toString()) {
            return Result.failure(IllegalArgumentException("Invalid login credentials: password does not match"))
        }

        return Result.success(
            RemoteAuthResponse(
                userId = account.userId,
                email = account.email,
                displayName = account.displayName,
                accessToken = "jwt_access_${UUID.randomUUID()}",
                refreshToken = "jwt_refresh_${UUID.randomUUID()}",
                expiresInSeconds = 3600L,
                registeredDeviceBindingId = account.registeredDeviceBindingId
            )
        )
    }

    override suspend fun signOut(accessToken: String): Result<Unit> {
        if (simulateNetworkError) {
            return Result.failure(IOException("Network timeout during sign out"))
        }
        signedOutTokens.add(accessToken)
        return Result.success(Unit)
    }

    override suspend fun refreshSession(refreshToken: String): Result<RemoteAuthResponse> {
        if (simulateNetworkError) {
            return Result.failure(IOException("Network connection unavailable"))
        }
        if (simulateExpiredRefreshToken) {
            return Result.failure(IllegalStateException("Refresh token has expired or has been revoked"))
        }

        return Result.success(
            RemoteAuthResponse(
                userId = "refreshed_user_id",
                email = "refreshed@example.com",
                displayName = "Refreshed User",
                accessToken = "jwt_access_refreshed_${UUID.randomUUID()}",
                refreshToken = "jwt_refresh_refreshed_${UUID.randomUUID()}",
                expiresInSeconds = 3600L
            )
        )
    }

    override suspend fun deleteAccount(accessToken: String): Result<Unit> {
        if (simulateNetworkError) {
            return Result.failure(IOException("Network timeout during account deletion"))
        }
        deletedTokens.add(accessToken)
        return Result.success(Unit)
    }

    var validOtpCode: String = "123456"
    var resendCallCount: Int = 0
    var recoverEmailSentTo: String? = null

    override suspend fun verifyEmailOtp(email: String, token: String, type: String): Result<RemoteAuthResponse> {
        if (simulateNetworkError) return Result.failure(IOException("Network timeout during OTP verification"))
        if (token != validOtpCode) {
            return Result.failure(IllegalArgumentException("Token has expired or is invalid"))
        }
        val normalized = email.trim().lowercase()
        val account = accounts[normalized] ?: StoredAccount(
            userId = "user_otp_${UUID.randomUUID().toString().take(8)}",
            email = normalized,
            password = "hashed_placeholder",
            displayName = null
        ).also { accounts[normalized] = it }

        return Result.success(
            RemoteAuthResponse(
                userId = account.userId,
                email = account.email,
                displayName = account.displayName,
                accessToken = "jwt_access_otp_${UUID.randomUUID()}",
                refreshToken = "jwt_refresh_otp_${UUID.randomUUID()}",
                expiresInSeconds = 3600L,
                registeredDeviceBindingId = account.registeredDeviceBindingId,
                requiresEmailVerification = false
            )
        )
    }

    override suspend fun resendOtp(email: String, type: String): Result<Unit> {
        if (simulateNetworkError) return Result.failure(IOException("Network timeout during OTP resend"))
        resendCallCount++
        return Result.success(Unit)
    }

    override suspend fun recoverPassword(email: String): Result<Unit> {
        if (simulateNetworkError) return Result.failure(IOException("Network timeout during password recovery"))
        recoverEmailSentTo = email.trim().lowercase()
        return Result.success(Unit)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<RemoteAuthResponse> {
        if (simulateNetworkError) return Result.failure(IOException("Network timeout during Google sign-in"))
        if (idToken.isBlank()) return Result.failure(IllegalArgumentException("Invalid Google ID token"))
        val email = "google_user@example.com"
        val userId = "google_user_123"
        return Result.success(
            RemoteAuthResponse(
                userId = userId,
                email = email,
                displayName = "Google User",
                accessToken = "jwt_google_access_${UUID.randomUUID()}",
                refreshToken = "jwt_google_refresh_${UUID.randomUUID()}",
                expiresInSeconds = 3600L,
                registeredDeviceBindingId = null,
                requiresEmailVerification = false
            )
        )
    }
}

/**
 * In-memory test double for [RemoteUserRepository].
 */
class InMemoryRemoteUserRepository : RemoteUserRepository {

    private val profiles = mutableMapOf<String, User>()
    private val deviceBindings = mutableMapOf<String, String>()

    var simulateFailure: Boolean = false

    override suspend fun fetchRemoteUserProfile(userId: String): Result<User?> {
        if (simulateFailure) return Result.failure(IOException("Failed to connect to user service"))
        return Result.success(profiles[userId])
    }

    override suspend fun updateRemoteUserProfile(user: User): Result<Unit> {
        if (simulateFailure) return Result.failure(IOException("Failed to connect to user service"))
        profiles[user.id] = user
        return Result.success(Unit)
    }

    override suspend fun registerDeviceBinding(userId: String, deviceBindingId: String): Result<Unit> {
        if (simulateFailure) return Result.failure(IOException("Failed to connect to user service"))
        deviceBindings[userId] = deviceBindingId
        val existing = profiles[userId]
        if (existing != null) {
            profiles[userId] = existing.copy(deviceBindingId = deviceBindingId)
        }
        return Result.success(Unit)
    }

    override suspend fun getRegisteredDeviceBindingId(userId: String): Result<String?> {
        if (simulateFailure) return Result.failure(IOException("Failed to connect to user service"))
        return Result.success(deviceBindings[userId])
    }

    override suspend fun deleteRemoteUserData(userId: String): Result<Unit> {
        if (simulateFailure) return Result.failure(IOException("Failed to connect to user service"))
        profiles.remove(userId)
        deviceBindings.remove(userId)
        return Result.success(Unit)
    }
}

/**
 * In-memory test double for [RemoteHistoryRepository].
 *
 * STEP 05 Remediation 2: Stores records with strict (userId, deviceBindingId) isolation.
 * History retrieval is strictly scoped to the exact server-authorized device binding.
 */
class InMemoryRemoteHistoryRepository : RemoteHistoryRepository {

    // Store history partitioned by Pair(userId, deviceBindingId)
    val usageStore = mutableMapOf<Pair<String, String>, MutableList<UsageEvent>>()
    val unlockStore = mutableMapOf<Pair<String, String>, MutableList<UnlockEvent>>()

    var simulateNetworkError: Boolean = false

    fun seedUsageHistory(userId: String, events: List<UsageEvent>, deviceBindingId: String = "default_binding") {
        usageStore.getOrPut(Pair(userId, deviceBindingId)) { mutableListOf() }.addAll(events)
    }

    fun seedUnlockHistory(userId: String, events: List<UnlockEvent>, deviceBindingId: String = "default_binding") {
        unlockStore.getOrPut(Pair(userId, deviceBindingId)) { mutableListOf() }.addAll(events)
    }

    fun getUsageEventsForBinding(userId: String, deviceBindingId: String): List<UsageEvent> {
        return usageStore[Pair(userId, deviceBindingId)]?.toList() ?: emptyList()
    }

    fun getUnlockEventsForBinding(userId: String, deviceBindingId: String): List<UnlockEvent> {
        return unlockStore[Pair(userId, deviceBindingId)]?.toList() ?: emptyList()
    }

    override suspend fun backupUsageEvents(userId: String, events: List<UsageEvent>): Result<Unit> {
        if (simulateNetworkError) return Result.failure(IOException("Network error"))
        // Tag with default binding for test backup
        usageStore.getOrPut(Pair(userId, "default_binding")) { mutableListOf() }.addAll(events)
        return Result.success(Unit)
    }

    override suspend fun backupUnlockEvents(userId: String, events: List<UnlockEvent>): Result<Unit> {
        if (simulateNetworkError) return Result.failure(IOException("Network error"))
        unlockStore.getOrPut(Pair(userId, "default_binding")) { mutableListOf() }.addAll(events)
        return Result.success(Unit)
    }

    override suspend fun deleteRemoteHistory(userId: String): Result<Unit> {
        if (simulateNetworkError) return Result.failure(IOException("Network error"))
        usageStore.keys.filter { it.first == userId }.forEach { usageStore.remove(it) }
        unlockStore.keys.filter { it.first == userId }.forEach { unlockStore.remove(it) }
        return Result.success(Unit)
    }
}

/**
 * In-memory test double for [RestoreAuthorizationService].
 *
 * MODELS THE TRUSTED SERVER-SIDE SECURITY BOUNDARY (STEP 05 Remediation 2):
 *
 * 1. Enforces single active binding per account via [activeEnrollments] map keyed by userId.
 * 2. Issues and validates high-entropy server-issued restore authorization credentials (SHA-256).
 * 3. Rejects client-supplied device IDs or claims: only the valid credential proves enrollment.
 * 4. Strictly scopes history queries to the authorized binding:
 *    WHERE user_id = auth.uid() AND device_binding_id = v_authorized_binding.
 */
class InMemoryRestoreAuthorizationService(
    private val remoteUserRepository: InMemoryRemoteUserRepository,
    private val remoteHistoryRepository: InMemoryRemoteHistoryRepository
) : RestoreAuthorizationService {

    data class ServerEnrolledBinding(
        val userId: String,
        val deviceBindingId: String,
        val restoreCredentialHash: String,
        var isActive: Boolean = true
    )

    // Keyed by userId: ENFORCES EXACTLY ONE ACTIVE BINDING PER ACCOUNT!
    private val activeEnrollments = mutableMapOf<String, ServerEnrolledBinding>()
    private val validTokens = mutableMapOf<String, String>()
    private val revokedTokens = mutableSetOf<String>()

    var simulateServerError: Boolean = false
    var simulateExpiredAuthorization: Boolean = false

    fun registerValidToken(accessToken: String, userId: String) {
        validTokens[accessToken] = userId
    }

    fun revokeToken(accessToken: String) {
        revokedTokens.add(accessToken)
    }

    /**
     * Compute SHA-256 hash string for credential verification.
     */
    fun hashCredential(credential: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(credential.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * Directly seed an enrolled binding on the server (for test setup).
     */
    fun seedEnrollment(
        userId: String,
        deviceBindingId: String,
        rawRestoreCredential: String,
        isActive: Boolean = true
    ) {
        activeEnrollments[userId] = ServerEnrolledBinding(
            userId = userId,
            deviceBindingId = deviceBindingId,
            restoreCredentialHash = hashCredential(rawRestoreCredential),
            isActive = isActive
        )
    }

    /**
     * Get the active enrolled binding for a user (for test inspection).
     */
    fun getActiveEnrollment(userId: String): ServerEnrolledBinding? {
        return activeEnrollments[userId]
    }

    override suspend fun enrollDeviceBinding(
        accessToken: String,
        deviceBindingId: String
    ): Result<DeviceEnrollmentResult> {
        if (simulateServerError) {
            return Result.failure(IOException("Server error during device enrollment"))
        }
        val userId = validTokens[accessToken]
            ?: return Result.failure(SecurityException("Unauthenticated caller"))

        // Check if an active binding already exists for this account (STEP 05 Remediation 3)
        val existing = activeEnrollments[userId]
        if (existing != null && existing.isActive) {
            return Result.success(
                DeviceEnrollmentResult.DeniedAlreadyBound(
                    "Account already has an active device binding. Re-enrollment or binding replacement is denied."
                )
            )
        }

        // Generate high-entropy server-issued restore credential
        val rawCredential = "sfrc_" + UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "")
        val credentialHash = hashCredential(rawCredential)

        // Store first active binding
        activeEnrollments[userId] = ServerEnrolledBinding(
            userId = userId,
            deviceBindingId = deviceBindingId,
            restoreCredentialHash = credentialHash,
            isActive = true
        )

        remoteUserRepository.registerDeviceBinding(userId, deviceBindingId)

        return Result.success(
            DeviceEnrollmentResult.Enrolled(
                deviceBindingId = deviceBindingId,
                restoreCredential = rawCredential
            )
        )
    }

    override suspend fun requestRestoreAuthorization(
        accessToken: String,
        restoreCredential: String
    ): RestoreAuthorizationResult {
        if (simulateServerError) {
            return RestoreAuthorizationResult.ServerError(
                IOException("Server internal error"),
                "Server encountered an internal error during restore authorization."
            )
        }

        // Step 1: Server verifies JWT and extracts auth.uid()
        val authenticatedUserId = validTokens[accessToken]
            ?: return RestoreAuthorizationResult.ServerError(
                SecurityException("Invalid or expired access token"),
                "Authentication failed. Access token is not valid."
            )

        if (revokedTokens.contains(accessToken) || simulateExpiredAuthorization) {
            return RestoreAuthorizationResult.Expired(
                "Restore authorization has expired or been revoked. Please re-authenticate."
            )
        }

        // Step 2: Format validation on restore credential
        if (restoreCredential.isBlank() || restoreCredential.length < 16) {
            return RestoreAuthorizationResult.DeniedUnauthorized(
                "Restore denied: Missing or malformed device restore credential."
            )
        }

        // Step 3: Look up active enrollment for this account (single active binding constraint)
        val enrollment = activeEnrollments[authenticatedUserId]
            ?: return RestoreAuthorizationResult.NoRegisteredBinding

        if (!enrollment.isActive) {
            return RestoreAuthorizationResult.DeniedUnauthorized(
                "Restore denied: Device binding enrollment is inactive or revoked."
            )
        }

        // Step 4: Server-side cryptographic hash comparison
        // Note: A client submitting a plain device_binding_id will fail because its hash
        // does not match the secret server-issued restore credential hash!
        val providedHash = hashCredential(restoreCredential)
        if (providedHash != enrollment.restoreCredentialHash) {
            return RestoreAuthorizationResult.DeniedUnauthorized(
                "Restore denied: Invalid device restore credential. Merely knowing a device binding ID does not authorize restore."
            )
        }

        // Step 5: Strict history query isolation
        // Query history constrained to auth.uid() AND the authorized binding
        val authorizedBinding = enrollment.deviceBindingId
        val usageEvents = remoteHistoryRepository.getUsageEventsForBinding(authenticatedUserId, authorizedBinding)
        val unlockEvents = remoteHistoryRepository.getUnlockEventsForBinding(authenticatedUserId, authorizedBinding)

        return RestoreAuthorizationResult.Authorized(
            authorizedBindingId = authorizedBinding,
            usageEvents = usageEvents,
            unlockEvents = unlockEvents
        )
    }
}
