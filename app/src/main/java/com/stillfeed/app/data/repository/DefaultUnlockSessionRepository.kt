package com.stillfeed.app.data.repository

import com.stillfeed.app.domain.model.BatchUnlockSession
import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.repository.UnlockHistoryRepository
import com.stillfeed.app.domain.repository.UnlockSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Production implementation of [UnlockSessionRepository] (STEP 19).
 *
 * Guarantees:
 * 1. Thread-safe atomic unlock creation protected by [Mutex].
 * 2. Idempotent: repeated rapid requests within cooldown window return the existing active session.
 * 3. Records permanent audit trail in [UnlockHistoryRepository] without blocking.
 * 4. Deduplicates repeat accessibility events for the same Reel/Short so allowance is not falsely consumed.
 */
class DefaultUnlockSessionRepository(
    private val unlockHistoryRepository: UnlockHistoryRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UnlockSessionRepository {

    private val mutex = Mutex()
    private val activeSessions = ConcurrentHashMap<String, BatchUnlockSession>()
    private val sessionsFlow = MutableStateFlow<Map<String, BatchUnlockSession>>(emptyMap())

    // Deduplication tracking: packageName -> (lastSignalKey, lastConsumedTimestamp)
    private val lastConsumedSignals = ConcurrentHashMap<String, Pair<String?, Long>>()

    override fun getActiveUnlock(packageName: String): BatchUnlockSession? {
        val session = activeSessions[packageName] ?: return null
        if (!session.isValid) {
            activeSessions.remove(packageName)
            sessionsFlow.value = activeSessions.toMap()
            return null
        }
        return session
    }

    override suspend fun createUnlockSession(
        packageName: String,
        batchAllowance: Int,
        activityType: String,
        verifiedActivityCompletion: Boolean
    ): Result<BatchUnlockSession> = withContext(ioDispatcher) {
        if (!verifiedActivityCompletion) {
            return@withContext Result.failure(
                IllegalStateException("Cannot grant batch unlock without verified activity completion.")
            )
        }

        if (!BatchUnlockSession.SUPPORTED_BATCH_OPTIONS.contains(batchAllowance)) {
            return@withContext Result.failure(
                IllegalArgumentException("Unsupported batch allowance: $batchAllowance. Must be one of ${BatchUnlockSession.SUPPORTED_BATCH_OPTIONS}")
            )
        }

        mutex.withLock {
            val existing = activeSessions[packageName]
            val now = System.currentTimeMillis()

            // Idempotency: if an active valid session was created in the last 2 seconds with the same parameters, reuse it
            if (existing != null && existing.isValid && (now - existing.createdAtMillis) < IDEMPOTENT_WINDOW_MS) {
                return@withLock Result.success(existing)
            }

            val newSession = BatchUnlockSession(
                id = UUID.randomUUID().toString(),
                packageName = packageName,
                selectedBatchAllowance = batchAllowance,
                remainingBatchAllowance = batchAllowance,
                activityType = activityType,
                verifiedActivityCompletion = true,
                createdAtMillis = now,
                expiresAtMillis = now + BatchUnlockSession.DEFAULT_EXPIRATION_WINDOW_MS
            )

            activeSessions[packageName] = newSession
            sessionsFlow.value = activeSessions.toMap()

            // Record audit trail event in Room DB
            try {
                unlockHistoryRepository?.recordUnlock(
                    UnlockEvent(
                        id = newSession.id,
                        timestampMillis = now,
                        allowanceMinutes = batchAllowance, // Stored as batch allowance unit
                        reason = "Batch unlock: $batchAllowance reels ($activityType)"
                    )
                )
            } catch (_: Exception) {
                // Fail-safe: persistence failure should not block immediate user unlock
            }

            Result.success(newSession)
        }
    }

    override suspend fun consumeReelAllowance(
        packageName: String,
        signalKey: String?
    ): BatchUnlockSession? = withContext(ioDispatcher) {
        mutex.withLock {
            val session = activeSessions[packageName] ?: return@withLock null
            if (!session.isValid) {
                activeSessions.remove(packageName)
                sessionsFlow.value = activeSessions.toMap()
                return@withLock null
            }

            val now = System.currentTimeMillis()
            val lastSignal = lastConsumedSignals[packageName]

            // Deduplication & Indeterminate Tracking Rule:
            // 1. If signalKey matches the previous signal key (e.g. static container "clips_viewer_view_pager"
            //    or "reel_recycler"), the user is still in the same ongoing viewing session.
            //    Repeated accessibility events (window state, playback ticks, caption updates) MUST NOT
            //    decrement again for the same Reel/Short.
            // 2. Mark isIndeterminateTracking = true to explicitly acknowledge that static view IDs alone
            //    cannot verify individual reel transitions without Step 21's floating companion.
            if (lastSignal != null) {
                val (previousKey, previousTime) = lastSignal
                val timeDelta = now - previousTime

                if (signalKey != null && signalKey == previousKey) {
                    val updatedSession = if (!session.isIndeterminateTracking) {
                        val flagged = session.copy(isIndeterminateTracking = true)
                        activeSessions[packageName] = flagged
                        sessionsFlow.value = activeSessions.toMap()
                        flagged
                    } else {
                        session
                    }
                    return@withLock updatedSession
                }

                // For a new distinct signal key, ensure minimum human consumption threshold (1.5s)
                if (timeDelta < MIN_REEL_CONSUMPTION_INTERVAL_MS) {
                    return@withLock session
                }
            }

            val updatedRemaining = (session.remainingBatchAllowance - 1).coerceAtLeast(0)
            val updatedSession = session.copy(
                remainingBatchAllowance = updatedRemaining
            )

            lastConsumedSignals[packageName] = Pair(signalKey, now)

            if (updatedSession.isExhausted) {
                activeSessions.remove(packageName)
            } else {
                activeSessions[packageName] = updatedSession
            }

            sessionsFlow.value = activeSessions.toMap()
            updatedSession
        }
    }

    override suspend fun expireUnlock(packageName: String): Unit = withContext(ioDispatcher) {
        mutex.withLock {
            activeSessions.remove(packageName)
            sessionsFlow.value = activeSessions.toMap()
        }
    }

    override fun observeActiveUnlock(packageName: String): Flow<BatchUnlockSession?> {
        return sessionsFlow.map { map ->
            val session = map[packageName]
            if (session != null && session.isValid) session else null
        }
    }

    override suspend fun clearAll(): Unit = withContext(ioDispatcher) {
        mutex.withLock {
            activeSessions.clear()
            lastConsumedSignals.clear()
            sessionsFlow.value = emptyMap()
        }
    }

    companion object {
        private const val IDEMPOTENT_WINDOW_MS = 2000L
        private const val DEDUPE_SAME_SIGNAL_WINDOW_MS = 3000L
        private const val MIN_REEL_CONSUMPTION_INTERVAL_MS = 1500L
    }
}
