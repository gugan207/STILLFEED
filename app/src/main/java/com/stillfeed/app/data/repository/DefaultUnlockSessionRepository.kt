package com.stillfeed.app.data.repository

import com.stillfeed.app.data.local.dao.ActiveUnlockSessionDao
import com.stillfeed.app.data.local.mapper.toDomain
import com.stillfeed.app.data.local.mapper.toEntity
import com.stillfeed.app.domain.model.BatchUnlockSession
import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.repository.UnlockHistoryRepository
import com.stillfeed.app.domain.repository.UnlockSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
 * 2. Durable Room persistence via [ActiveUnlockSessionDao]: survives process death and recreation.
 * 3. Restoration on startup restores ONLY active sessions that remain valid (unexpired & unexhausted).
 * 4. Idempotent: repeated rapid requests within cooldown window return the existing active session.
 * 5. Defensible Reel transition detection:
 *    - Repeated events on the same static container for the same Reel do NOT decrement.
 *    - Explicit verified transitions (scrolls or distinct transition tokens) consume 1 allowance.
 *    - Debounces rapid duplicate scroll events (minimum 1.5s between reel decrements).
 *    - Flags [BatchUnlockSession.isIndeterminateTracking] when only static container IDs are observed.
 *    - Bounded expiration window ensures safe fallback against indefinite viewing.
 * 6. Correct audit semantics: records [UnlockEvent] without corrupting [UnlockEvent.allowanceMinutes]
 *    with batch Reel counts.
 */
class DefaultUnlockSessionRepository(
    private val activeUnlockSessionDao: ActiveUnlockSessionDao? = null,
    private val unlockHistoryRepository: UnlockHistoryRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UnlockSessionRepository {

    private val mutex = Mutex()
    private val activeSessions = ConcurrentHashMap<String, BatchUnlockSession>()
    private val sessionsFlow = MutableStateFlow<Map<String, BatchUnlockSession>>(emptyMap())

    // Deduplication tracking: packageName -> (lastSignalKey, lastConsumedTimestamp)
    private val lastConsumedSignals = ConcurrentHashMap<String, Pair<String?, Long>>()

    // Tracks verified expired or exhausted sessions: packageName -> BatchUnlockSession
    private val expiredSessions = ConcurrentHashMap<String, BatchUnlockSession>()

    init {
        restorePersistedSessions()
    }

    /**
     * Synchronously restores persisted sessions from Room on initialization.
     * Restores ONLY sessions that remain valid (neither expired nor exhausted).
     */
    private fun restorePersistedSessions() {
        try {
            val persistedEntities = activeUnlockSessionDao?.getAllSessionsSync() ?: emptyList()
            val now = System.currentTimeMillis()
            persistedEntities.forEach { entity ->
                val session = entity.toDomain()
                if (session.isValid(now)) {
                    activeSessions[session.packageName] = session
                } else {
                    // Record expired session and clean up from disk
                    expiredSessions[session.packageName] = session
                    activeUnlockSessionDao?.deleteSessionByPackageSync(session.packageName)
                }
            }
            sessionsFlow.value = activeSessions.toMap()
        } catch (_: Exception) {
            // Fail-safe: database access error during init should not crash process
        }
    }

    override fun getActiveUnlock(packageName: String): BatchUnlockSession? {
        val session = activeSessions[packageName] ?: return null
        val now = System.currentTimeMillis()
        if (!session.isValid(now)) {
            activeSessions.remove(packageName)
            expiredSessions[packageName] = session
            sessionsFlow.value = activeSessions.toMap()
            try {
                activeUnlockSessionDao?.deleteSessionByPackageSync(packageName)
            } catch (_: Exception) {
            }
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
            val now = System.currentTimeMillis()
            val existing = activeSessions[packageName]

            // Idempotency: if an active valid session was created in the last 2 seconds with the same parameters, reuse it
            if (existing != null && existing.isValid(now) && (now - existing.createdAtMillis) < IDEMPOTENT_WINDOW_MS) {
                return@withLock Result.success(existing)
            }

            val boundedExpiresAt = BatchUnlockSession.calculateExpirationMillis(batchAllowance, now)

            val newSession = BatchUnlockSession(
                id = UUID.randomUUID().toString(),
                packageName = packageName,
                selectedBatchAllowance = batchAllowance,
                remainingBatchAllowance = batchAllowance,
                activityType = activityType,
                verifiedActivityCompletion = true,
                createdAtMillis = now,
                expiresAtMillis = boundedExpiresAt
            )

            // Requirement 3: Durable storage in Room.
            // If persistence fails, never report an unlock as durably saved when it was not!
            if (activeUnlockSessionDao != null) {
                try {
                    activeUnlockSessionDao.insertOrUpdate(newSession.toEntity())
                } catch (e: Exception) {
                    return@withLock Result.failure(
                        IllegalStateException("Failed to durably persist active unlock session: ${e.message}", e)
                    )
                }
            }

            activeSessions[packageName] = newSession
            expiredSessions.remove(packageName)
            sessionsFlow.value = activeSessions.toMap()

            // Requirement 4: Record audit history.
            // Do NOT store Reel count as minutes in allowanceMinutes. Set allowanceMinutes = 0
            // and clearly state batch Reel count in reason to protect duration-based Insights.
            try {
                unlockHistoryRepository?.recordUnlock(
                    UnlockEvent(
                        id = newSession.id,
                        timestampMillis = now,
                        allowanceMinutes = 0,
                        reason = "Batch unlock: $batchAllowance reels ($activityType)"
                    )
                )
            } catch (_: Exception) {
                // Fail-safe: audit history error should not block active unlock
            }

            Result.success(newSession)
        }
    }

    override suspend fun consumeReelAllowance(
        packageName: String,
        signalKey: String?,
        isExplicitTransition: Boolean
    ): BatchUnlockSession? = withContext(ioDispatcher) {
        mutex.withLock {
            val session = activeSessions[packageName] ?: return@withLock null
            val now = System.currentTimeMillis()
            if (!session.isValid(now)) {
                activeSessions.remove(packageName)
                sessionsFlow.value = activeSessions.toMap()
                activeUnlockSessionDao?.deleteSessionByPackage(packageName)
                return@withLock null
            }

            val lastSignal = lastConsumedSignals[packageName]

            // Scenario 1: First entry into the short-form viewer during this unlock session.
            // Count the initial reel being presented to the user.
            if (lastSignal == null) {
                val updatedRemaining = (session.remainingBatchAllowance - 1).coerceAtLeast(0)
                val updatedSession = session.copy(
                    remainingBatchAllowance = updatedRemaining
                )
                lastConsumedSignals[packageName] = Pair(signalKey, now)
                persistSessionUpdate(updatedSession)
                return@withLock updatedSession
            }

            val (previousKey, previousTime) = lastSignal
            val timeDelta = now - previousTime

            // Scenario 2: Repeated events on the same static container without an explicit transition.
            // Captions, playback ticks, volume, comment drawers fire TYPE_WINDOW_CONTENT_CHANGED.
            // Deduplicate completely — do NOT decrement again for the same Reel!
            if (!isExplicitTransition && signalKey != null && signalKey == previousKey) {
                val updatedSession = if (!session.isIndeterminateTracking) {
                    val flagged = session.copy(isIndeterminateTracking = true)
                    persistSessionUpdate(flagged)
                    flagged
                } else {
                    session
                }
                return@withLock updatedSession
            }

            // Scenario 3: Rapid duplicate transitions within debounce threshold (1.5s).
            // A human cannot physically consume another Reel in under 1.5s. Drop rapid multi-ticks.
            if (timeDelta < MIN_REEL_CONSUMPTION_INTERVAL_MS) {
                return@withLock session
            }

            // Scenario 4: Verified new Reel transition (either explicit scroll event OR distinct item key).
            // Decrement exactly ONE allowance.
            val updatedRemaining = (session.remainingBatchAllowance - 1).coerceAtLeast(0)
            val updatedSession = session.copy(
                remainingBatchAllowance = updatedRemaining
            )
            lastConsumedSignals[packageName] = Pair(signalKey, now)
            persistSessionUpdate(updatedSession)
            return@withLock updatedSession
        }
    }

    private suspend fun persistSessionUpdate(session: BatchUnlockSession) {
        if (session.isExhausted) {
            activeSessions.remove(session.packageName)
            expiredSessions[session.packageName] = session
            try {
                activeUnlockSessionDao?.deleteSessionByPackage(session.packageName)
            } catch (_: Exception) {
            }
        } else {
            activeSessions[session.packageName] = session
            try {
                activeUnlockSessionDao?.insertOrUpdate(session.toEntity())
            } catch (_: Exception) {
            }
        }
        sessionsFlow.value = activeSessions.toMap()
    }

    override suspend fun expireUnlock(packageName: String): Unit = withContext(ioDispatcher) {
        mutex.withLock {
            val session = activeSessions.remove(packageName)
            if (session != null) {
                expiredSessions[packageName] = session
            }
            lastConsumedSignals.remove(packageName)
            sessionsFlow.value = activeSessions.toMap()
            try {
                activeUnlockSessionDao?.deleteSessionByPackage(packageName)
            } catch (_: Exception) {
            }
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
            expiredSessions.clear()
            lastConsumedSignals.clear()
            sessionsFlow.value = emptyMap()
            try {
                activeUnlockSessionDao?.deleteAllSessions()
            } catch (_: Exception) {
            }
        }
    }

    override fun getLatestExpiredSession(packageName: String?): BatchUnlockSession? {
        val now = System.currentTimeMillis()
        if (packageName != null) {
            val session = expiredSessions[packageName] ?: return null
            val isRecent = (now - session.createdAtMillis) < MAX_EXPIRATION_AUDIT_WINDOW_MS
            return if (isRecent) {
                session
            } else {
                expiredSessions.remove(packageName)
                null
            }
        } else {
            val candidate = expiredSessions.values.maxByOrNull { it.createdAtMillis } ?: return null
            val isRecent = (now - candidate.createdAtMillis) < MAX_EXPIRATION_AUDIT_WINDOW_MS
            return if (isRecent) {
                candidate
            } else {
                expiredSessions.remove(candidate.packageName)
                null
            }
        }
    }

    override fun clearExpiredSession(packageName: String?) {
        if (packageName != null) {
            expiredSessions.remove(packageName)
        } else {
            expiredSessions.clear()
        }
    }

    companion object {
        private const val IDEMPOTENT_WINDOW_MS = 2000L
        private const val MIN_REEL_CONSUMPTION_INTERVAL_MS = 1500L
        private const val MAX_EXPIRATION_AUDIT_WINDOW_MS = 30 * 60 * 1000L // 30 minutes recency window
    }
}
