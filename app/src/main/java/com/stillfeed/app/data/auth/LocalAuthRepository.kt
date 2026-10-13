package com.stillfeed.app.data.auth

import com.stillfeed.app.auth.model.AuthError
import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.auth.model.AuthUser
import com.stillfeed.app.auth.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Concrete implementation of [AuthRepository] for development and local session handling.
 *
 * Security architecture (STEP 03 Security Remediation):
 * - Fast general-purpose hashing (e.g. SHA-256) is NOT used for passwords.
 * - Password derivation delegates to [PasswordHasher] (modern PBKDF2-HMAC-SHA256 with 600,000 iterations).
 * - Password verification uses timing-safe byte equality ([java.security.MessageDigest.isEqual]).
 * - Plaintext passwords are NEVER stored in memory or persistent storage.
 * - Sensitive tokens and passwords are NEVER logged.
 * - Decoupled repository boundary enables seamless future integration of production cloud providers.
 */
import com.stillfeed.app.domain.model.User
import com.stillfeed.app.domain.repository.UserRepository

class LocalAuthRepository(
    private val sessionStorage: AuthSessionStorage,
    private val passwordHasher: PasswordHasher = Pbkdf2PasswordHasher(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val userRepository: UserRepository? = null
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // In-memory credential registry storing only cryptographically derived password hashes
    private val credentialsStore = mutableMapOf<String, StoredCredential>()

    init {
        // Bootstrap session state from secure storage
        val existingToken = sessionStorage.getSessionToken()
        val existingUser = sessionStorage.getUser()

        if (existingToken != null && existingUser != null) {
            _authState.value = AuthState.SignedIn(existingUser)
        } else {
            _authState.value = AuthState.SignedOut
        }
    }

    override suspend fun signIn(email: String, password: CharSequence): Result<AuthUser> = withContext(ioDispatcher) {
        _authState.value = AuthState.SigningIn

        val normalizedEmail = email.trim().lowercase()

        // Check development credential store
        val stored = credentialsStore[normalizedEmail]
        if (stored == null) {
            // For development smooth onboarding: if first time, register account with derived hash
            val storedHash = passwordHasher.hashPassword(password)
            val userId = "user_${UUID.randomUUID().toString().take(8)}"
            val newUser = AuthUser(
                id = userId,
                email = normalizedEmail,
                displayName = normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                deviceBindingId = sessionStorage.getDeviceBinding()
            )
            credentialsStore[normalizedEmail] = StoredCredential(userId, storedHash)

            val sessionToken = "session_${UUID.randomUUID()}"
            sessionStorage.saveSession(newUser, sessionToken)
            persistUserLocally(newUser)
            _authState.value = AuthState.SignedIn(newUser)
            return@withContext Result.success(newUser)
        }

        // Timing-safe password verification via PasswordHasher
        val isValid = passwordHasher.verifyPassword(password, stored.storedHash)
        if (!isValid) {
            val error = AuthError.InvalidCredentials()
            _authState.value = AuthState.Error(error)
            return@withContext Result.failure(IllegalArgumentException(error.userMessage))
        }

        val user = AuthUser(
            id = stored.userId,
            email = normalizedEmail,
            displayName = normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
            deviceBindingId = sessionStorage.getDeviceBinding()
        )
        val sessionToken = "session_${UUID.randomUUID()}"
        sessionStorage.saveSession(user, sessionToken)
        persistUserLocally(user)
        _authState.value = AuthState.SignedIn(user)
        Result.success(user)
    }

    override suspend fun signUp(
        email: String,
        password: CharSequence,
        displayName: String?
    ): Result<AuthUser> = withContext(ioDispatcher) {
        _authState.value = AuthState.SigningUp

        val normalizedEmail = email.trim().lowercase()
        if (credentialsStore.containsKey(normalizedEmail)) {
            val error = AuthError.AccountAlreadyExists()
            _authState.value = AuthState.Error(error)
            return@withContext Result.failure(IllegalArgumentException(error.userMessage))
        }

        val userId = "user_${UUID.randomUUID().toString().take(8)}"
        val storedHash = passwordHasher.hashPassword(password)
        credentialsStore[normalizedEmail] = StoredCredential(userId, storedHash)

        val newUser = AuthUser(
            id = userId,
            email = normalizedEmail,
            displayName = displayName?.trim().takeIf { !it.isNullOrBlank() }
                ?: normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
            deviceBindingId = sessionStorage.getDeviceBinding()
        )

        val sessionToken = "session_${UUID.randomUUID()}"
        sessionStorage.saveSession(newUser, sessionToken)
        persistUserLocally(newUser)
        _authState.value = AuthState.SignedIn(newUser)
        Result.success(newUser)
    }

    override suspend fun signOut(): Result<Unit> = withContext(ioDispatcher) {
        sessionStorage.clearSession()
        _authState.value = AuthState.SignedOut
        Result.success(Unit)
    }

    override suspend fun verifyEmailOtp(email: String, token: String): Result<AuthUser> = withContext(ioDispatcher) {
        if (token != "123456") {
            val err = AuthError.InvalidCredentials("That code is incorrect or has expired.")
            _authState.value = AuthState.Error(err)
            return@withContext Result.failure(IllegalArgumentException(err.userMessage))
        }
        val normalizedEmail = email.trim().lowercase()
        val user = AuthUser(
            id = "user_otp_${UUID.randomUUID().toString().take(8)}",
            email = normalizedEmail,
            displayName = normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
            deviceBindingId = sessionStorage.getDeviceBinding()
        )
        val sessionToken = "session_otp_${UUID.randomUUID()}"
        sessionStorage.saveSession(user, sessionToken)
        persistUserLocally(user)
        _authState.value = AuthState.SignedIn(user)
        Result.success(user)
    }

    override suspend fun resendOtp(email: String): Result<Unit> = withContext(ioDispatcher) {
        Result.success(Unit)
    }

    var simulateRecoveryError: Boolean = false
    var simulateUpdatePasswordError: Boolean = false

    override suspend fun sendPasswordReset(email: String, redirectTo: String?): Result<Unit> = withContext(ioDispatcher) {
        if (simulateRecoveryError) {
            return@withContext Result.failure(IllegalArgumentException("Password recovery email could not be delivered. In Resend development mode, delivery is permitted only to guganthangaraj7@gmail.com."))
        }
        Result.success(Unit)
    }

    override suspend fun handleRecoveryLink(deepLinkUri: String): Result<Unit> = withContext(ioDispatcher) {
        if (deepLinkUri.contains("error")) {
            Result.failure(IllegalArgumentException("This password reset link has expired or is invalid. Please request a new link."))
        } else if (!deepLinkUri.contains("type=recovery")) {
            Result.failure(IllegalArgumentException("Invalid recovery link: unexpected flow type. Expected 'recovery'."))
        } else {
            Result.success(Unit)
        }
    }

    override suspend fun resetPassword(newPassword: CharSequence): Result<Unit> = withContext(ioDispatcher) {
        if (simulateUpdatePasswordError) {
            return@withContext Result.failure(IllegalArgumentException("Failed to update password. Session expired."))
        }
        Result.success(Unit)
    }

    override fun clearRecoverySession() {}

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> = withContext(ioDispatcher) {
        if (idToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Google ID token"))
        }
        val user = AuthUser(
            id = "google_user_123",
            email = "google_user@stillfeed.app",
            displayName = "Google User",
            deviceBindingId = sessionStorage.getDeviceBinding()
        )
        val sessionToken = "session_google_${UUID.randomUUID()}"
        sessionStorage.saveSession(user, sessionToken)
        persistUserLocally(user)
        _authState.value = AuthState.SignedIn(user)
        Result.success(user)
    }

    override suspend fun refreshSession(): Result<AuthUser?> = withContext(ioDispatcher) {
        _authState.value = AuthState.Loading
        val token = sessionStorage.getSessionToken()
        val user = sessionStorage.getUser()

        if (token != null && user != null) {
            _authState.value = AuthState.SignedIn(user)
            Result.success(user)
        } else {
            sessionStorage.clearSession()
            _authState.value = AuthState.SignedOut
            Result.success(null)
        }
    }

    override suspend fun getCurrentUser(): AuthUser? = withContext(ioDispatcher) {
        sessionStorage.getUser()
    }

    override suspend fun bindDevice(deviceId: String): Result<AuthUser> = withContext(ioDispatcher) {
        val currentUser = sessionStorage.getUser()
            ?: return@withContext Result.failure(IllegalStateException("No authenticated user to bind device."))

        val updatedUser = currentUser.withDeviceBinding(deviceId)
        val token = sessionStorage.getSessionToken() ?: "session_${UUID.randomUUID()}"
        sessionStorage.saveDeviceBinding(deviceId)
        sessionStorage.saveSession(updatedUser, token)
        persistUserLocally(updatedUser)
        _authState.value = AuthState.SignedIn(updatedUser)
        Result.success(updatedUser)
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(ioDispatcher) {
        val currentUser = sessionStorage.getUser()
        if (currentUser != null) {
            currentUser.email?.trim()?.lowercase()?.let { credentialsStore.remove(it) }
            userRepository?.deleteUser(currentUser.id)
        }
        sessionStorage.clearSession()
        _authState.value = AuthState.SignedOut
        Result.success(Unit)
    }

    private suspend fun persistUserLocally(authUser: AuthUser) {
        userRepository?.saveUser(
            User(
                id = authUser.id,
                email = authUser.email,
                displayName = authUser.displayName,
                createdAtMillis = authUser.createdAtMillis,
                updatedAtMillis = System.currentTimeMillis(),
                deviceBindingId = authUser.deviceBindingId
            )
        )
    }

    /**
     * Stored credential representation containing only the cryptographic hash and salt metadata.
     * Plaintext passwords are NEVER stored.
     */
    internal data class StoredCredential(
        val userId: String,
        val storedHash: StoredPasswordHash
    )
}
