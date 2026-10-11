package com.stillfeed.app.usage.sync

import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.usage.analysis.UsageSessionAnalyzer
import com.stillfeed.app.usage.reconciliation.UsageReconciler
import com.stillfeed.app.usage.stats.UsageStatsDataSource

/**
 * Result of a usage analysis synchronization cycle.
 */
sealed interface UsageSyncResult {
    data class Success(
        val newSessionsCount: Int,
        val syncedDurationMillis: Long
    ) : UsageSyncResult

    data object PermissionMissing : UsageSyncResult

    data class Error(val message: String) : UsageSyncResult
}

/**
 * High-level coordinator for background usage statistics synchronization (STEP 07 Section 6 & 7).
 *
 * Responsibilities:
 * 1. Checks PACKAGE_USAGE_STATS permission safely without crashing.
 * 2. Executes bounded, incremental queries using persistent [AppPreferencesRepository] watermark.
 * 3. Orchestrates [UsageSessionAnalyzer] and [UsageReconciler].
 * 4. Advances the sync watermark upon completion to prevent duplicate processing.
 * 5. Runs local-first with zero internet requirement.
 */
class UsageSyncManager(
    private val usageStatsDataSource: UsageStatsDataSource,
    private val sessionAnalyzer: UsageSessionAnalyzer = UsageSessionAnalyzer(),
    private val reconciler: UsageReconciler,
    private val preferencesRepository: AppPreferencesRepository
) {

    /**
     * Executes an incremental usage sync cycle up to [nowMillis].
     */
    suspend fun syncUsage(nowMillis: Long = System.currentTimeMillis()): UsageSyncResult {
        // 1. Permission Check
        if (!usageStatsDataSource.hasUsageStatsPermission()) {
            return UsageSyncResult.PermissionMissing
        }

        return try {
            // 2. Determine incremental query window
            val preferences = preferencesRepository.getPreferences()
            val lastSync = preferences.lastUsageSyncTimestampMillis

            val startTimeMillis = if (lastSync <= 0L || lastSync > nowMillis) {
                maxOf(0L, nowMillis - DEFAULT_LOOKBACK_MS)
            } else {
                maxOf(lastSync, nowMillis - MAX_LOOKBACK_MS)
            }

            if (startTimeMillis >= nowMillis) {
                return UsageSyncResult.Success(0, 0L)
            }

            // 3. Query system UsageStats
            val rawEvents = usageStatsDataSource.queryTimelineEvents(startTimeMillis, nowMillis)

            if (rawEvents.isEmpty()) {
                preferencesRepository.updatePreferences { it.copy(lastUsageSyncTimestampMillis = nowMillis) }
                return UsageSyncResult.Success(0, 0L)
            }

            // 4. Analyze contiguous foreground sessions
            val analyzedSessions = sessionAnalyzer.analyze(rawEvents, nowMillis)

            // 5. Reconcile with Room and Accessibility interventions
            val reconciledEvents = reconciler.reconcile(analyzedSessions)

            // 6. Update sync watermark
            preferencesRepository.updatePreferences { it.copy(lastUsageSyncTimestampMillis = nowMillis) }

            val totalDuration = reconciledEvents.sumOf { it.durationMillis ?: 0L }
            UsageSyncResult.Success(reconciledEvents.size, totalDuration)
        } catch (e: Exception) {
            UsageSyncResult.Error(e.message ?: "Unknown sync error")
        }
    }

    companion object {
        const val DEFAULT_LOOKBACK_MS = 24 * 60 * 60 * 1000L // 24 hours back for first run
        const val MAX_LOOKBACK_MS = 48 * 60 * 60 * 1000L     // Bounded 48h limit for battery safety
    }
}
