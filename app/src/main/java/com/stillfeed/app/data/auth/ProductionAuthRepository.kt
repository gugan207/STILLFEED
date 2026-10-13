package com.stillfeed.app.data.auth

import com.stillfeed.app.auth.model.AuthError
import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.auth.model.AuthUser
import com.stillfeed.app.auth.repository.AuthRepository
import com.stillfeed.app.data.remote.RemoteAuthDataSource
import com.stillfeed.app.data.remote.RemoteAuthResponse
import com.stillfeed.app.domain.model.AuthSession
import com.stillfeed.app.domain.model.User
import com.stillfeed.app.domain.repository.DeviceBindingRepository
import com.stillfeed.app.domain.repository.RemoteUserRepository
import com.stillfeed.app.domain.repository.SessionRepository
import com.stillfeed.app.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Production implementation of [AuthRepository] integrating with remote authentication provider
 * while enforcing STILLFEED security, privacy, and device-bound restore principles.
 *
 * Security & Architecture rules:
 * - Passwords are NEVER stored locally or logged.
 * - Sensitive bearer tokens are stored in [SessionRepository] and NEVER logged.
 * - Decoupled from provider SDK details via [RemoteAuthDataSource] and [RemoteUserRepository].
 * - When an account signs up, the current device's binding token is registered remotely.
 * - When an account signs in, the remote registered device binding is fetched and associated with the session.
 * - Cross-device history restore is NEVER automatically permitted on sign-in.
 */
class ProductionAuthRepository(
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val sessionRepository: SessionRepository,
    private val remoteUserRepository: RemoteUserRepository,
    private val deviceBindingRepository: DeviceBindingRepository,
    private val userRepository: UserRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    coroutineScope: CoroutineScope? = null
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(
        if (coroutineScope != null) AuthState.Loading else AuthState.SignedOut
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    @Volatile
    private var activeRecoveryAccessToken: String? = null

    init {
        coroutineScope?.launch(ioDispatcher) {
            try {
                refreshSession()
            } catch (_: Throwable) {
                _authState.value = AuthState.SignedOut
            }
        }
    }

    override suspend fun signIn(email: String, password: CharSequence): Result<AuthUser> = withContext(ioDispatcher) {
        _authState.value = AuthState.SigningIn

        val normalizedEmail = email.trim().lowercase()
        val remoteResult = remoteAuthDataSource.signIn(normalizedEmail, password)

        if (remoteResult.isFailure) {
            val exception = remoteResult.exceptionOrNull() ?: RuntimeException("Sign-in failed")
            val authError = mapToAuthError(exception, isSignUp = false)
            _authState.value = AuthState.Error(authError)
            return@withContext Result.failure(IllegalArgumentException(authError.userMessage))
        }

        val response = remoteResult.getOrThrow()

        // Fetch or verify registered device binding ID for this user account
        val registeredBindingId = response.registeredDeviceBindingId
            ?: remoteUserRepository.getRegisteredDeviceBindingId(response.userId).getOrNull()

        val ttlMillis = response.expiresInSeconds * 1000L
        val session = AuthSession(
            userId = response.userId,
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            expiresAtMillis = System.currentTimeMillis() + ttlMillis,
            userEmail = response.email ?: normalizedEmail,
            deviceBindingId = registeredBindingId
        )
        sessionRepository.saveSession(session)

        // Persist account profile in local Room cache without overwriting existing data
        val existingUser = userRepository.getUser(response.userId)
        val localUser = User(
            id = response.userId,
            email = response.email ?: normalizedEmail,
            displayName = response.displayName ?: existingUser?.displayName ?: normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
            createdAtMillis = existingUser?.createdAtMillis ?: System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis(),
            deviceBindingId = registeredBindingId ?: existingUser?.deviceBindingId
        )
        userRepository.saveUser(localUser)

        val authUser = AuthUser(
            id = localUser.id,
            email = localUser.email,
            displayName = localUser.displayName,
            createdAtMillis = localUser.createdAtMillis,
            deviceBindingId = registeredBindingId
        )
        _authState.value = AuthState.SignedIn(authUser)
        Result.success(authUser)
    }

    override suspend fun signUp(
        email: String,
        password: CharSequence,
        displayName: String?
    ): Result<AuthUser> = withContext(ioDispatcher) {
        _authState.value = AuthState.SigningUp

        val normalizedEmail = email.trim().lowercase()
        val remoteResult = remoteAuthDataSource.signUp(normalizedEmail, password, displayName)

        if (remoteResult.isFailure) {
            val exception = remoteResult.exceptionOrNull() ?: RuntimeException("Account creation failed")
            val authError = mapToAuthError(exception, isSignUp = true)
            _authState.value = AuthState.Error(authError)
            return@withContext Result.failure(IllegalArgumentException(authError.userMessage))
        }

        val response = remoteResult.getOrThrow()

        if (response.isExistingUser) {
            val authError = AuthError.AccountAlreadyExists()
            _authState.value = AuthState.Error(authError)
            return@withContext Result.failure(IllegalArgumentException(authError.userMessage))
        }

        if (response.requiresEmailVerification) {
            _authState.value = AuthState.NeedsEmailVerification(normalizedEmail, response.userId)
            val pendingUser = AuthUser(
                id = response.userId,
                email = normalizedEmail,
                displayName = response.displayName ?: displayName ?: normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                createdAtMillis = System.currentTimeMillis(),
                deviceBindingId = null
            )
            return@withContext Result.success(pendingUser)
        }

        // Bind current device identity upon account creation
        val currentDeviceBinding = deviceBindingRepository.getCurrentBinding()
        val deviceBindingId = currentDeviceBinding.bindingId
        if (deviceBindingId.isNotBlank()) {
            remoteUserRepository.registerDeviceBinding(response.userId, deviceBindingId)
        }

        val ttlMillis = response.expiresInSeconds * 1000L
        val session = AuthSession(
            userId = response.userId,
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            expiresAtMillis = System.currentTimeMillis() + ttlMillis,
            userEmail = response.email ?: normalizedEmail,
            deviceBindingId = deviceBindingId.ifBlank { null }
        )
        sessionRepository.saveSession(session)

        val existingUser = userRepository.getUser(response.userId)
        val localUser = User(
            id = response.userId,
            email = response.email ?: normalizedEmail,
            displayName = response.displayName ?: displayName ?: existingUser?.displayName ?: normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
            createdAtMillis = existingUser?.createdAtMillis ?: System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis(),
            deviceBindingId = deviceBindingId.ifBlank { existingUser?.deviceBindingId }
        )
        userRepository.saveUser(localUser)

        val authUser = AuthUser(
            id = localUser.id,
            email = localUser.email,
            displayName = localUser.displayName,
            createdAtMillis = localUser.createdAtMillis,
            deviceBindingId = localUser.deviceBindingId
        )
        _authState.value = AuthState.SignedIn(authUser)
        Result.success(authUser)
    }

    override suspend fun verifyEmailOtp(email: String, token: String): Result<AuthUser> = withContext(ioDispatcher) {
        _authState.value = AuthState.Loading

        val normalizedEmail = email.trim().lowercase()
        val remoteResult = remoteAuthDataSource.verifyEmailOtp(normalizedEmail, token)

        if (remoteResult.isFailure) {
            val exception = remoteResult.exceptionOrNull() ?: RuntimeException("Verification failed")
            val authError = mapToAuthError(exception, isSignUp = false)
            _authState.value = AuthState.Error(authError)
            return@withContext Result.failure(IllegalArgumentException(authError.userMessage))
        }

        val response = remoteResult.getOrThrow()

        // Bind current device identity upon verified sign up
        val currentDeviceBinding = deviceBindingRepository.getCurrentBinding()
        val deviceBindingId = currentDeviceBinding.bindingId
        if (deviceBindingId.isNotBlank()) {
            remoteUserRepository.registerDeviceBinding(response.userId, deviceBindingId)
        }

        val ttlMillis = response.expiresInSeconds * 1000L
        val session = AuthSession(
            userId = response.userId,
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            expiresAtMillis = System.currentTimeMillis() + ttlMillis,
            userEmail = response.email ?: normalizedEmail,
            deviceBindingId = deviceBindingId.ifBlank { null }
        )
        sessionRepository.saveSession(session)

        val existingUser = userRepository.getUser(response.userId)
        val localUser = User(
            id = response.userId,
            email = response.email ?: normalizedEmail,
            displayName = response.displayName ?: existingUser?.displayName ?: normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
            createdAtMillis = existingUser?.createdAtMillis ?: System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis(),
            deviceBindingId = deviceBindingId.ifBlank { existingUser?.deviceBindingId }
        )
        userRepository.saveUser(localUser)

        val authUser = AuthUser(
            id = localUser.id,
            email = localUser.email,
            displayName = localUser.displayName,
            createdAtMillis = localUser.createdAtMillis,
            deviceBindingId = localUser.deviceBindingId
        )
        _authState.value = AuthState.SignedIn(authUser)
        Result.success(authUser)
    }

    override suspend fun resendOtp(email: String): Result<Unit> = withContext(ioDispatcher) {
        val normalizedEmail = email.trim().lowercase()
        remoteAuthDataSource.resendOtp(normalizedEmail)
    }

    override suspend fun sendPasswordReset(email: String, redirectTo: String?): Result<Unit> = withContext(ioDispatcher) {
        val normalizedEmail = email.trim().lowercase()
        val redirect = redirectTo ?: "stillfeed://reset-password"
        val remoteResult = remoteAuthDataSource.recoverPassword(normalizedEmail, redirect)
        if (remoteResult.isFailure) {
            val exception = remoteResult.exceptionOrNull() ?: RuntimeException("Password recovery request failed")
            val authError = mapToAuthError(exception, isSignUp = false)
            return@withContext Result.failure(IllegalArgumentException(authError.userMessage))
        }
        Result.success(Unit)
    }

    override suspend fun handleRecoveryLink(deepLinkUri: String): Result<Unit> = withContext(ioDispatcher) {
        val uri = deepLinkUri.trim()
        if (uri.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid recovery link."))
        }

        val params = parseUriParams(uri)

        // Check for error parameters (expired/invalid link)
        val error = params["error"] ?: params["error_code"]
        if (!error.isNullOrBlank()) {
            val errorDescription = params["error_description"]?.replace("+", " ")
                ?: "This password reset link has expired or is invalid. Please request a new link."
            activeRecoveryAccessToken = null
            return@withContext Result.failure(IllegalArgumentException(errorDescription))
        }

        // Check for direct access_token in fragment (#access_token=...) or query (?access_token=...)
        val accessToken = params["access_token"]
        if (!accessToken.isNullOrBlank()) {
            activeRecoveryAccessToken = accessToken
            return@withContext Result.success(Unit)
        }

        // Check for token or token_hash (e.g. from /verify?token=...&type=recovery)
        val token = params["token"] ?: params["token_hash"]
        if (!token.isNullOrBlank()) {
            val verifyResult = remoteAuthDataSource.verifyEmailOtp(email = "", token = token, type = "recovery")
            if (verifyResult.isSuccess) {
                val response = verifyResult.getOrThrow()
                activeRecoveryAccessToken = response.accessToken
                return@withContext Result.success(Unit)
            } else {
                activeRecoveryAccessToken = null
                val exception = verifyResult.exceptionOrNull() ?: RuntimeException("Invalid token")
                val authError = mapToAuthError(exception, isSignUp = false)
                return@withContext Result.failure(IllegalArgumentException(authError.userMessage))
            }
        }

        Result.failure(IllegalArgumentException("Malformed or unrecognized password reset link."))
    }

    override suspend fun resetPassword(newPassword: CharSequence): Result<Unit> = withContext(ioDispatcher) {
        val token = activeRecoveryAccessToken
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("No active recovery session found. Please open the reset link from your email."))
        }
        if (newPassword.length < 8) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 8 characters."))
        }
        if (!newPassword.any { it.isUpperCase() } || !newPassword.any { !it.isLetter() }) {
            return@withContext Result.failure(IllegalArgumentException("Password must include at least 1 uppercase letter and 1 number or symbol."))
        }

        val remoteResult = remoteAuthDataSource.updatePassword(token, newPassword)
        if (remoteResult.isFailure) {
            val exception = remoteResult.exceptionOrNull() ?: RuntimeException("Password update failed")
            val authError = mapToAuthError(exception, isSignUp = false)
            return@withContext Result.failure(IllegalArgumentException(authError.userMessage))
        }

        activeRecoveryAccessToken = null
        Result.success(Unit)
    }

    override fun clearRecoverySession() {
        activeRecoveryAccessToken = null
    }

    private fun parseUriParams(uriString: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val fragmentIndex = uriString.indexOf('#')
        if (fragmentIndex != -1 && fragmentIndex < uriString.length - 1) {
            val fragment = uriString.substring(fragmentIndex + 1)
            parseQueryString(fragment, params)
        }
        val queryIndex = uriString.indexOf('?')
        if (queryIndex != -1) {
            val queryEnd = if (fragmentIndex != -1 && fragmentIndex > queryIndex) fragmentIndex else uriString.length
            val query = uriString.substring(queryIndex + 1, queryEnd)
            parseQueryString(query, params)
        }
        return params
    }

    private fun parseQueryString(queryString: String, params: MutableMap<String, String>) {
        queryString.split('&').forEach { pair ->
            val eq = pair.indexOf('=')
            if (eq != -1) {
                val key = pair.substring(0, eq)
                val value = pair.substring(eq + 1)
                try {
                    params[key] = java.net.URLDecoder.decode(value, "UTF-8")
                } catch (_: Exception) {
                    params[key] = value
                }
            }
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> = withContext(ioDispatcher) {
        _authState.value = AuthState.SigningIn

        val remoteResult = remoteAuthDataSource.signInWithGoogle(idToken)
        if (remoteResult.isFailure) {
            val exception = remoteResult.exceptionOrNull() ?: RuntimeException("Google sign-in failed")
            val authError = mapToAuthError(exception, isSignUp = false)
            _authState.value = AuthState.Error(authError)
            return@withContext Result.failure(IllegalArgumentException(authError.userMessage))
        }

        val response = remoteResult.getOrThrow()

        val currentDeviceBinding = deviceBindingRepository.getCurrentBinding()
        val deviceBindingId = currentDeviceBinding.bindingId
        if (deviceBindingId.isNotBlank()) {
            remoteUserRepository.registerDeviceBinding(response.userId, deviceBindingId)
        }

        val ttlMillis = response.expiresInSeconds * 1000L
        val session = AuthSession(
            userId = response.userId,
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            expiresAtMillis = System.currentTimeMillis() + ttlMillis,
            userEmail = response.email ?: "",
            deviceBindingId = deviceBindingId.ifBlank { null }
        )
        sessionRepository.saveSession(session)

        val existingUser = userRepository.getUser(response.userId)
        val localUser = User(
            id = response.userId,
            email = response.email ?: existingUser?.email ?: "",
            displayName = response.displayName ?: existingUser?.displayName ?: "Google User",
            createdAtMillis = existingUser?.createdAtMillis ?: System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis(),
            deviceBindingId = deviceBindingId.ifBlank { existingUser?.deviceBindingId }
        )
        userRepository.saveUser(localUser)

        val authUser = AuthUser(
            id = localUser.id,
            email = localUser.email,
            displayName = localUser.displayName,
            createdAtMillis = localUser.createdAtMillis,
            deviceBindingId = localUser.deviceBindingId
        )
        _authState.value = AuthState.SignedIn(authUser)
        Result.success(authUser)
    }

    override suspend fun signOut(): Result<Unit> = withContext(ioDispatcher) {
        val currentSession = sessionRepository.getSession()
        if (currentSession != null && currentSession.accessToken.isNotBlank()) {
            try {
                remoteAuthDataSource.signOut(currentSession.accessToken)
            } catch (_: Exception) {
                // Ensure local sign out succeeds even during offline / network errors
            }
        }
        sessionRepository.clearSession()
        _authState.value = AuthState.SignedOut
        Result.success(Unit)
    }

    override suspend fun refreshSession(): Result<AuthUser?> = withContext(ioDispatcher) {
        if (_authState.value !is AuthState.SignedIn) {
            _authState.value = AuthState.Loading
        }
        val session = sessionRepository.getSession()

        if (session == null) {
            _authState.value = AuthState.SignedOut
            return@withContext Result.success(null)
        }

        if (session.isExpired) {
            val refreshToken = session.refreshToken
            if (!refreshToken.isNullOrBlank()) {
                val refreshResult = remoteAuthDataSource.refreshSession(refreshToken)
                if (refreshResult.isSuccess) {
                    val response = refreshResult.getOrThrow()
                    val ttlMillis = response.expiresInSeconds * 1000L
                    val updatedSession = session.copy(
                        accessToken = response.accessToken,
                        refreshToken = response.refreshToken ?: refreshToken,
                        expiresAtMillis = System.currentTimeMillis() + ttlMillis,
                        deviceBindingId = response.registeredDeviceBindingId ?: session.deviceBindingId
                    )
                    sessionRepository.saveSession(updatedSession)

                    val existingLocal = userRepository.getUser(response.userId)
                    val authUser = AuthUser(
                        id = response.userId,
                        email = response.email ?: session.userEmail,
                        displayName = response.displayName ?: existingLocal?.displayName,
                        createdAtMillis = existingLocal?.createdAtMillis ?: System.currentTimeMillis(),
                        deviceBindingId = updatedSession.deviceBindingId
                    )
                    _authState.value = AuthState.SignedIn(authUser)
                    return@withContext Result.success(authUser)
                }
            }

            // Session has expired and could not be refreshed
            sessionRepository.clearSession()
            val expiredError = AuthError.SessionExpired()
            _authState.value = AuthState.SessionExpired(expiredError.userMessage)
            return@withContext Result.failure(IllegalStateException(expiredError.userMessage))
        }

        // Active valid session
        val localUser = userRepository.getUser(session.userId)
        val authUser = AuthUser(
            id = session.userId,
            email = session.userEmail ?: localUser?.email ?: "",
            displayName = localUser?.displayName,
            createdAtMillis = localUser?.createdAtMillis ?: System.currentTimeMillis(),
            deviceBindingId = session.deviceBindingId
        )
        _authState.value = AuthState.SignedIn(authUser)
        Result.success(authUser)
    }

    override suspend fun getCurrentUser(): AuthUser? = withContext(ioDispatcher) {
        val session = sessionRepository.getSession() ?: return@withContext null
        val localUser = userRepository.getUser(session.userId)
        AuthUser(
            id = session.userId,
            email = session.userEmail ?: localUser?.email ?: "",
            displayName = localUser?.displayName,
            createdAtMillis = localUser?.createdAtMillis ?: System.currentTimeMillis(),
            deviceBindingId = session.deviceBindingId
        )
    }

    override suspend fun bindDevice(deviceId: String): Result<AuthUser> = withContext(ioDispatcher) {
        val currentSession = sessionRepository.getSession()
            ?: return@withContext Result.failure(IllegalStateException("No active session to bind"))

        // Register binding remotely
        val registerResult = remoteUserRepository.registerDeviceBinding(currentSession.userId, deviceId)
        if (registerResult.isFailure) {
            val ex = registerResult.exceptionOrNull() ?: RuntimeException("Remote device binding failed")
            return@withContext Result.failure(ex)
        }

        // Update active session
        val updatedSession = currentSession.copy(deviceBindingId = deviceId)
        sessionRepository.saveSession(updatedSession)

        // Update local user in Room
        val existing = userRepository.getUser(currentSession.userId)
        val updatedUser = existing?.copy(
            deviceBindingId = deviceId,
            updatedAtMillis = System.currentTimeMillis()
        ) ?: User(
            id = currentSession.userId,
            email = currentSession.userEmail ?: "",
            displayName = currentSession.userEmail?.substringBefore("@"),
            createdAtMillis = System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis(),
            deviceBindingId = deviceId
        )
        userRepository.saveUser(updatedUser)

        val authUser = AuthUser(
            id = updatedUser.id,
            email = updatedUser.email,
            displayName = updatedUser.displayName,
            createdAtMillis = updatedUser.createdAtMillis,
            deviceBindingId = deviceId
        )
        _authState.value = AuthState.SignedIn(authUser)
        Result.success(authUser)
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(ioDispatcher) {
        val session = sessionRepository.getSession()
        if (session != null) {
            try {
                if (session.accessToken.isNotBlank()) {
                    remoteAuthDataSource.deleteAccount(session.accessToken)
                }
            } catch (_: Exception) {
                // Continue local cleanup even if remote account deletion fails
            }
            try {
                remoteUserRepository.deleteRemoteUserData(session.userId)
            } catch (_: Exception) {
                // Ignore remote cleanup error
            }
            userRepository.deleteUser(session.userId)
        }
        sessionRepository.clearSession()
        _authState.value = AuthState.SignedOut
        Result.success(Unit)
    }

    private fun mapToAuthError(throwable: Throwable, isSignUp: Boolean): AuthError {
        val message = throwable.message?.lowercase() ?: ""
        return when {
            message.contains("already registered") || message.contains("already exists") || message.contains("duplicate") -> {
                AuthError.AccountAlreadyExists()
            }
            message.contains("550") || message.contains("could not send email") || message.contains("testing emails") ||
                message.contains("confirmation mail") || message.contains("confirmation email") ||
                message.contains("recovery email") || message.contains("recovery mail") -> {
                AuthError.Unknown("Unable to send email. In Resend test mode, emails can only be sent to the verified account owner (guganthangaraj7@gmail.com). To send to other emails, verify a domain in Resend.")
            }
            message.contains("network") || message.contains("timeout") || message.contains("connect") || message.contains("unable to connect") -> {
                AuthError.NetworkFailure()
            }
            message.contains("otp") || message.contains("expired") || message.contains("token") -> {
                AuthError.Unknown("That code is incorrect or has expired. Request a new code and try again.")
            }
            message.contains("provider") && (message.contains("disabled") || message.contains("not enabled")) -> {
                AuthError.Unknown("Google Sign-In is awaiting Google OAuth configuration in the Supabase Dashboard. Please sign in with Email or configure Google provider credentials.")
            }
            message.contains("invalid") || message.contains("credentials") || message.contains("password") || message.contains("not found") -> {
                if (isSignUp) AuthError.AccountCreationFailed() else AuthError.InvalidCredentials()
            }
            message.contains("malformed") || message.contains("parse") || message.contains("json") -> {
                AuthError.Unknown("Malformed response received from authentication service.")
            }
            else -> {
                if (isSignUp) AuthError.AccountCreationFailed() else AuthError.Unknown(throwable.message ?: "Authentication failed")
            }
        }
    }
}
