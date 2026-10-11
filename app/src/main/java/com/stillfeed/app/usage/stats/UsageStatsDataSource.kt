package com.stillfeed.app.usage.stats

/**
 * Lightweight, privacy-safe structural representation of an Android UsageStats timeline event.
 *
 * Privacy Guarantees:
 * - NO user content, text, queries, or media.
 * - Retains strictly package names, lifecycle event types, and timestamps.
 */
data class RawUsageTimelineEvent(
    val packageName: String,
    val timestampMillis: Long,
    val eventType: Int
)

/**
 * Domain-level abstraction for interacting with system usage statistics (STEP 07 Section 3).
 *
 * Keeps Android-specific [android.app.usage.UsageStatsManager] and [android.app.AppOpsManager]
 * decoupled from domain analysis logic.
 */
interface UsageStatsDataSource {
    /**
     * Checks whether the user has granted the [android.Manifest.permission.PACKAGE_USAGE_STATS] permission.
     */
    fun hasUsageStatsPermission(): Boolean

    /**
     * Queries raw usage timeline events within the bounded window [[startTimeMillis], [endTimeMillis]].
     * Returns an empty list safely if permission is absent or on OS error.
     */
    fun queryTimelineEvents(startTimeMillis: Long, endTimeMillis: Long): List<RawUsageTimelineEvent>
}
