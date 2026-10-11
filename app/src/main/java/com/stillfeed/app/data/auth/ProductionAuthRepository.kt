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

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        coroutineScope?.launch(ioDispatcher) {
            refreshSession()
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

        // Persist account profile in local Room cache
        val localUser = User(
            id = response.userId,
            email = response.email ?: normalizedEmail,
            displayName = response.displayName ?: normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
            createdAtMillis = System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis(),
            deviceBindingId = registeredBindingId
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

        val localUser = User(
            id = response.userId,
            email = response.email ?: normalizedEmail,
            displayName = response.displayName ?: displayName ?: normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
            createdAtMillis = System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis(),
            deviceBindingId = deviceBindingId.ifBlank { null }
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
        _authState.value = AuthState.Loading
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
        val authUser = if (localUser != null) {
            AuthUser(
                id = localUser.id,
                email = localUser.email,
                displayName = localUser.displayName,
                createdAtMillis = localUser.createdAtMillis,
                deviceBindingId = session.deviceBindingId ?: localUser.deviceBindingId
            )
        } else {
            AuthUser(
                id = session.userId,
                email = session.userEmail,
                displayName = session.userEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() },
                deviceBindingId = session.deviceBindingId
            )
        }
        _authState.value = AuthState.SignedIn(authUser)
        Result.success(authUser)
    }

    override suspend fun getCurrentUser(): AuthUser? = withContext(ioDispatcher) {
        val session = sessionRepository.getSession() ?: return@withContext null
        val localUser = userRepository.getUser(session.userId) ?: return@withContext null
        AuthUser(
            id = localUser.id,
            email = localUser.email,
            displayName = localUser.displayName,
            createdAtMillis = localUser.createdAtMillis,
            deviceBindingId = session.deviceBindingId ?: localUser.deviceBindingId
        )
    }

    override suspend fun bindDevice(deviceId: String): Result<AuthUser> = withContext(ioDispatcher) {
        val currentSession = sessionRepository.getSession()
            ?: return@withContext Result.failure(IllegalStateException("No authenticated user to bind device."))

        // Register binding remotely
        val remoteBindResult = remoteUserRepository.registerDeviceBinding(currentSession.userId, deviceId)
        if (remoteBindResult.isFailure) {
            val ex = remoteBindResult.exceptionOrNull() ?: RuntimeException("Remote device binding registration failed")
            return@withContext Result.failure(ex)
        }

        // Bind locally
        deviceBindingRepository.bindCurrentDevice(deviceId)

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
            message.contains("network") || message.contains("timeout") || message.contains("connect") || message.contains("unable to connect") -> {
                AuthError.NetworkFailure()
            }
            message.contains("invalid") || message.contains("credentials") || message.contains("password") || message.contains("not found") -> {
                if (isSignUp) AuthError.AccountCreationFailed() else AuthError.InvalidCredentials()
            }
            message.contains("malformed") || message.contains("parse") || message.contains("json") -> {
                AuthError.Unknown("Malformed response received from authentication service.")
            }
            else -> {
                if (isSignUp) AuthError.AccountCreationFailed() else AuthError.Unknown()
            }
        }
    }
}
