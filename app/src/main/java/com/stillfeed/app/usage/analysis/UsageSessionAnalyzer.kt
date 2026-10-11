package com.stillfeed.app.usage.analysis

import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.usage.stats.RawUsageTimelineEvent
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Deterministic analyzer that converts raw Android usage timeline events into normalized,
 * consolidated [ForegroundSession]s (STEP 07 Section 4).
 *
 * Guarantees:
 * 1. Strictly tracks monitored packages: Instagram and YouTube.
 * 2. Filters out zero-duration, negative-duration, or invalid-timestamp sessions.
 * 3. Coalesces overlapping duplicate sessions for the same package.
 * 4. Splits sessions that cross midnight boundaries so daily aggregations are exact.
 * 5. Caches open sessions at query boundaries without leaking into the future.
 */
class UsageSessionAnalyzer(
    private val monitoredPackages: Set<String> = MONITORED_PACKAGES
) {

    /**
     * Converts raw timeline events into consolidated [ForegroundSession] objects.
     */
    fun analyze(
        rawEvents: List<RawUsageTimelineEvent>,
        queryEndTimeMillis: Long
    ): List<ForegroundSession> {
        if (rawEvents.isEmpty()) return emptyList()

        val consolidatedSessions = mutableListOf<ForegroundSession>()

        // Analyze each monitored package independently
        monitoredPackages.forEach { packageName ->
            val packageEvents = rawEvents
                .filter { it.packageName == packageName && it.timestampMillis > 0 }
                .sortedBy { it.timestampMillis }

            if (packageEvents.isEmpty()) return@forEach

            val rawSessions = extractRawSessions(packageName, packageEvents, queryEndTimeMillis)
            val mergedSessions = mergeOverlappingSessions(rawSessions)
            val splitSessions = mergedSessions.flatMap { splitAtMidnightBoundaries(it) }

            consolidatedSessions.addAll(splitSessions)
        }

        return consolidatedSessions.sortedBy { it.startTimeMillis }
    }

    private fun extractRawSessions(
        packageName: String,
        events: List<RawUsageTimelineEvent>,
        queryEndTimeMillis: Long
    ): List<ForegroundSession> {
        val sessions = mutableListOf<ForegroundSession>()
        var sessionStart: Long? = null

        events.forEach { event ->
            when (event.eventType) {
                EVENT_TYPE_RESUMED, EVENT_TYPE_MOVE_TO_FOREGROUND -> {
                    // If not already in a session, record start
                    if (sessionStart == null) {
                        sessionStart = event.timestampMillis
                    }
                }
                EVENT_TYPE_PAUSED, EVENT_TYPE_STOPPED, EVENT_TYPE_MOVE_TO_BACKGROUND, EVENT_TYPE_SCREEN_OFF -> {
                    if (sessionStart != null) {
                        val start = sessionStart!!
                        val end = event.timestampMillis
                        if (end > start) {
                            sessions.add(
                                ForegroundSession(
                                    id = UUID.randomUUID().toString(),
                                    packageName = packageName,
                                    startTimeMillis = start,
                                    endTimeMillis = end,
                                    durationMillis = end - start,
                                    surfaceCategory = defaultSurfaceCategory(packageName)
                                )
                            )
                        }
                        sessionStart = null
                    }
                }
            }
        }

        // Handle open session extending up to query end cutoff
        if (sessionStart != null) {
            val start = sessionStart!!
            val end = queryEndTimeMillis
            if (end > start) {
                sessions.add(
                    ForegroundSession(
                        id = UUID.randomUUID().toString(),
                        packageName = packageName,
                        startTimeMillis = start,
                        endTimeMillis = end,
                        durationMillis = end - start,
                        surfaceCategory = defaultSurfaceCategory(packageName)
                    )
                )
            }
        }

        return sessions
    }

    /**
     * Merges adjacent or overlapping sessions for the same package.
     */
    fun mergeOverlappingSessions(sessions: List<ForegroundSession>): List<ForegroundSession> {
        if (sessions.isEmpty()) return emptyList()

        val sorted = sessions.sortedBy { it.startTimeMillis }
        val merged = mutableListOf<ForegroundSession>()

        for (session in sorted) {
            if (session.durationMillis <= 0 || session.endTimeMillis <= session.startTimeMillis) {
                continue // Discard invalid/zero-duration sessions
            }

            if (merged.isEmpty()) {
                merged.add(session)
            } else {
                val last = merged.last()
                if (session.startTimeMillis <= last.endTimeMillis) {
                    // Overlapping or contiguous: extend the previous session
                    val newEnd = maxOf(last.endTimeMillis, session.endTimeMillis)
                    val newDuration = newEnd - last.startTimeMillis
                    merged[merged.size - 1] = last.copy(
                        endTimeMillis = newEnd,
                        durationMillis = newDuration,
                        isIntervened = last.isIntervened || session.isIntervened
                    )
                } else {
                    merged.add(session)
                }
            }
        }

        return merged
    }

    /**
     * Splits a session if it spans across midnight UTC boundaries.
     */
    fun splitAtMidnightBoundaries(session: ForegroundSession): List<ForegroundSession> {
        val startZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(session.startTimeMillis), ZoneOffset.UTC)
        val endZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(session.endTimeMillis), ZoneOffset.UTC)

        // Same day -> no split required
        if (startZdt.toLocalDate() == endZdt.toLocalDate()) {
            return listOf(session)
        }

        val result = mutableListOf<ForegroundSession>()
        var currentStartMillis = session.startTimeMillis

        while (currentStartMillis < session.endTimeMillis) {
            val currentZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentStartMillis), ZoneOffset.UTC)
            val nextMidnightZdt = currentZdt.toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC)
            val nextMidnightMillis = nextMidnightZdt.toInstant().toEpochMilli()

            val currentEndMillis = minOf(session.endTimeMillis, nextMidnightMillis)
            val duration = currentEndMillis - currentStartMillis

            if (duration > 0) {
                result.add(
                    ForegroundSession(
                        id = UUID.randomUUID().toString(),
                        packageName = session.packageName,
                        startTimeMillis = currentStartMillis,
                        endTimeMillis = currentEndMillis,
                        durationMillis = duration,
                        surfaceCategory = session.surfaceCategory,
                        isIntervened = session.isIntervened
                    )
                )
            }

            currentStartMillis = currentEndMillis
        }

        return result
    }

    private fun defaultSurfaceCategory(packageName: String): SurfaceCategory =
        when (packageName) {
            PACKAGE_INSTAGRAM -> SurfaceCategory.FEED
            PACKAGE_YOUTUBE -> SurfaceCategory.FEED
            else -> SurfaceCategory.UNKNOWN
        }

    companion object {
        const val PACKAGE_INSTAGRAM = "com.instagram.android"
        const val PACKAGE_YOUTUBE = "com.google.android.youtube"

        val MONITORED_PACKAGES = setOf(PACKAGE_INSTAGRAM, PACKAGE_YOUTUBE)

        // Android UsageEvents.Event constants
        const val EVENT_TYPE_RESUMED = 1
        const val EVENT_TYPE_PAUSED = 2
        const val EVENT_TYPE_STOPPED = 23
        const val EVENT_TYPE_SCREEN_OFF = 16
        const val EVENT_TYPE_MOVE_TO_FOREGROUND = 1
        const val EVENT_TYPE_MOVE_TO_BACKGROUND = 2
    }
}
