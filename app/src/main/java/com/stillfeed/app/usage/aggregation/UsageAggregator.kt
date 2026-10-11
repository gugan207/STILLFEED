package com.stillfeed.app.usage.aggregation

import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.usage.analysis.UsageSessionAnalyzer
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Domain-level aggregator for calculating daily attention usage totals (STEP 07 Section 5).
 *
 * Capabilities:
 * - Calculates today's Instagram duration.
 * - Calculates today's YouTube duration.
 * - Calculates combined monitored usage.
 * - Calculates historical daily usage summaries over specified date ranges.
 *
 * PRODUCT CONSTRAINTS:
 * - Do NOT calculate monthly awareness reduction percentages (5%, 10%, 15%, 20%).
 * - Do NOT calculate scores out of 100.
 * - Do NOT introduce shame, guilt, failure, or "lazy" messaging.
 */
class UsageAggregator(
    private val usageRepository: UsageRepository
) {

    /**
     * Calculates the aggregated usage for today (UTC day boundary).
     */
    suspend fun getTodayUsage(nowMillis: Long = System.currentTimeMillis()): DailyUsageSummary {
        val nowZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneOffset.UTC)
        val startOfDayMillis = nowZdt.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val events = usageRepository.getEventsInRange(startOfDayMillis, nowMillis)
        return aggregateEventsForDate(nowZdt.toLocalDate().toString(), events)
    }

    /**
     * Calculates historical daily summaries for the window [[startMillis], [endMillis]].
     */
    suspend fun getHistoricalDailySummaries(
        startMillis: Long,
        endMillis: Long
    ): List<DailyUsageSummary> {
        if (startMillis >= endMillis) return emptyList()

        val events = usageRepository.getEventsInRange(startMillis, endMillis)
        if (events.isEmpty()) return emptyList()

        // Group by UTC date string YYYY-MM-DD
        val eventsByDate = events.groupBy { event ->
            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.timestampMillis), ZoneOffset.UTC)
            zdt.toLocalDate().toString()
        }

        return eventsByDate.map { (dateString, dateEvents) ->
            aggregateEventsForDate(dateString, dateEvents)
        }.sortedBy { it.dateString }
    }

    private fun aggregateEventsForDate(
        dateString: String,
        events: List<UsageEvent>
    ): DailyUsageSummary {
        val sessions = events.filter { it.eventType == UsageEventType.SESSION_END }

        val instagramMillis = sessions
            .filter { it.packageName == UsageSessionAnalyzer.PACKAGE_INSTAGRAM }
            .sumOf { it.durationMillis ?: 0L }

        val youtubeMillis = sessions
            .filter { it.packageName == UsageSessionAnalyzer.PACKAGE_YOUTUBE }
            .sumOf { it.durationMillis ?: 0L }

        val blockedCount = events.count { it.isBlocked || it.eventType == UsageEventType.BLOCKED }

        return DailyUsageSummary(
            dateString = dateString,
            instagramUsageMillis = instagramMillis,
            youtubeUsageMillis = youtubeMillis,
            totalMonitoredMillis = instagramMillis + youtubeMillis,
            blockedInterventionCount = blockedCount,
            sessionCount = sessions.size
        )
    }
}
