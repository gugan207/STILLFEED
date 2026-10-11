package com.stillfeed.app.domain.repository

import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.model.UsageEvent

/**
 * Domain repository contract for remote storage of usage and unlock history.
 *
 * STEP 05 Remediation Security Model:
 * - Backup operations are authenticated via the user's JWT (auth.uid() enforced by RLS).
 * - History FETCHING no longer accepts a client-supplied device-binding proof parameter.
 * - All history retrieval for restore goes through [RestoreAuthorizationService],
 *   which performs server-side device-binding verification.
 * - Direct fetch methods exist only for server-internal or backup verification use.
 */
interface RemoteHistoryRepository {
    suspend fun backupUsageEvents(userId: String, events: List<UsageEvent>): Result<Unit>
    suspend fun backupUnlockEvents(userId: String, events: List<UnlockEvent>): Result<Unit>
    suspend fun deleteRemoteHistory(userId: String): Result<Unit>
}
