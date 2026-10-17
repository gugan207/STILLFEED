package com.stillfeed.app.usage.aggregation

/**
 * Domain model representing aggregated daily attention usage (STEP 07 Section 5).
 *
 * Privacy Guarantees:
 * - NO user messages, queries, media, or private details.
 * - Stores solely daily aggregate durations and intervention counts.
 */
data class DailyUsageSummary(
    val dateString: String,
    val instagramUsageMillis: Long,
    val youtubeUsageMillis: Long,
    val totalMonitoredMillis: Long,
    val blockedInterventionCount: Int,
    val sessionCount: Int,
    val verifiedReelsCount: Int? = null
)
