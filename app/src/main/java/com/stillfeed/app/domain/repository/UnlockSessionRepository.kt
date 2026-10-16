package com.stillfeed.app.domain.repository

import com.stillfeed.app.domain.model.BatchUnlockSession
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing active batch unlock sessions (STEP 19).
 */
interface UnlockSessionRepository {

    /**
     * Retrieves the currently active and valid unlock session for [packageName], or null if none exists/expired.
     */
    fun getActiveUnlock(packageName: String): BatchUnlockSession?

    /**
     * Atomically creates and persists an active unlock session for [packageName].
     * Idempotent: repeated rapid requests return the active session without duplicate grants.
     */
    suspend fun createUnlockSession(
        packageName: String,
        batchAllowance: Int,
        activityType: String,
        verifiedActivityCompletion: Boolean
    ): Result<BatchUnlockSession>

    /**
     * Decrements the remaining allowance by 1 for [packageName] upon a verified new Reel transition.
     * If the session becomes exhausted, it is marked as exhausted.
     * Returns the updated session, or null if no valid session existed.
     */
    suspend fun consumeReelAllowance(
        packageName: String,
        signalKey: String? = null,
        isExplicitTransition: Boolean = false
    ): BatchUnlockSession?

    /**
     * Explicitly expires or clears the active unlock session for [packageName].
     */
    suspend fun expireUnlock(packageName: String)

    /**
     * Observes active unlock session changes for [packageName].
     */
    fun observeActiveUnlock(packageName: String): Flow<BatchUnlockSession?>

    /**
     * Clears all active unlock sessions (e.g., on sign out or full reset).
     */
    suspend fun clearAll()

    /**
     * Retrieves the most recently expired or exhausted batch unlock session for [packageName],
     * or any package if [packageName] is null.
     */
    fun getLatestExpiredSession(packageName: String? = null): BatchUnlockSession?

    /**
     * Clears the recorded expired session for [packageName], or all if [packageName] is null.
     */
    fun clearExpiredSession(packageName: String? = null)
}
