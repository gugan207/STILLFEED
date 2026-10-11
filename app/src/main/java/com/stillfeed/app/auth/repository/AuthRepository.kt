package com.stillfeed.app.auth.repository

import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.auth.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain boundary interface for STILLFEED authentication.
 *
 * Keeps the application UI and domain layers decoupled from any specific
 * authentication provider (e.g. Firebase Auth, Supabase, OAuth backend, or local session).
 */
interface AuthRepository {
    /**
     * Hot observable stream of the current authentication state.
     */
    val authState: StateFlow<AuthState>

    /**
     * Authenticate an existing account.
     * Note: password uses [CharSequence] to avoid unnecessary string pool retention.
     */
    suspend fun signIn(email: String, password: CharSequence): Result<AuthUser>

    /**
     * Create a new STILLFEED account.
     */
    suspend fun signUp(email: String, password: CharSequence, displayName: String? = null): Result<AuthUser>

    /**
     * Terminate the active authentication session securely.
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Check and refresh the existing session on application launch.
     */
    suspend fun refreshSession(): Result<AuthUser?>

    /**
     * Retrieve the current cached authenticated user, or null if signed out.
     */
    suspend fun getCurrentUser(): AuthUser?

    /**
     * Extension point for device identity binding (STEP 03 requirement 12).
     * Prepares for the later same-device restore requirement without transferring
     * history automatically to any new device.
     */
    suspend fun bindDevice(deviceId: String): Result<AuthUser>

    /**
     * Permanently deletes user account, local session, and associated data.
     */
    suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
}
