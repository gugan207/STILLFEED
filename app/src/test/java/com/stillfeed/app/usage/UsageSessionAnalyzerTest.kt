package com.stillfeed.app.usage

import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.usage.analysis.ForegroundSession
import com.stillfeed.app.usage.analysis.UsageSessionAnalyzer
import com.stillfeed.app.usage.stats.RawUsageTimelineEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class UsageSessionAnalyzerTest {

    private lateinit var analyzer: UsageSessionAnalyzer

    @Before
    fun setUp() {
        analyzer = UsageSessionAnalyzer()
    }

    @Test
    fun singleInstagramSession_calculatesCorrectDuration() {
        val startTime = 1_000_000L
        val endTime = 1_060_000L // 60 seconds
        val rawEvents = listOf(
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, startTime, UsageSessionAnalyzer.EVENT_TYPE_RESUMED),
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, endTime, UsageSessionAnalyzer.EVENT_TYPE_PAUSED)
        )

        val sessions = analyzer.analyze(rawEvents, endTime + 10_000L)

        assertEquals(1, sessions.size)
        val session = sessions[0]
        assertEquals(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, session.packageName)
        assertEquals(startTime, session.startTimeMillis)
        assertEquals(endTime, session.endTimeMillis)
        assertEquals(60_000L, session.durationMillis)
    }

    @Test
    fun singleYouTubeSession_calculatesCorrectDuration() {
        val startTime = 2_000_000L
        val endTime = 2_120_000L // 120 seconds
        val rawEvents = listOf(
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_YOUTUBE, startTime, UsageSessionAnalyzer.EVENT_TYPE_RESUMED),
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_YOUTUBE, endTime, UsageSessionAnalyzer.EVENT_TYPE_STOPPED)
        )

        val sessions = analyzer.analyze(rawEvents, endTime + 10_000L)

        assertEquals(1, sessions.size)
        val session = sessions[0]
        assertEquals(UsageSessionAnalyzer.PACKAGE_YOUTUBE, session.packageName)
        assertEquals(120_000L, session.durationMillis)
    }

    @Test
    fun multipleSessions_acrossBothMonitoredPackages() {
        val rawEvents = listOf(
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, 1000L, UsageSessionAnalyzer.EVENT_TYPE_RESUMED),
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, 5000L, UsageSessionAnalyzer.EVENT_TYPE_PAUSED),
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_YOUTUBE, 6000L, UsageSessionAnalyzer.EVENT_TYPE_RESUMED),
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_YOUTUBE, 12000L, UsageSessionAnalyzer.EVENT_TYPE_PAUSED)
        )

        val sessions = analyzer.analyze(rawEvents, 20000L)

        assertEquals(2, sessions.size)
        assertEquals(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, sessions[0].packageName)
        assertEquals(4000L, sessions[0].durationMillis)

        assertEquals(UsageSessionAnalyzer.PACKAGE_YOUTUBE, sessions[1].packageName)
        assertEquals(6000L, sessions[1].durationMillis)
    }

    @Test
    fun overlappingSessions_areMergedToPreventDoubleCounting() {
        // Two overlapping raw chunks from the same package (e.g. multi-window or rapid activity recreation)
        val session1 = ForegroundSession(
            packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            durationMillis = 4000L
        )
        val session2 = ForegroundSession(
            packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
            startTimeMillis = 4000L, // overlaps with session1
            endTimeMillis = 8000L,
            durationMillis = 4000L
        )

        val merged = analyzer.mergeOverlappingSessions(listOf(session1, session2))

        assertEquals(1, merged.size)
        assertEquals(1000L, merged[0].startTimeMillis)
        assertEquals(8000L, merged[0].endTimeMillis)
        assertEquals(7000L, merged[0].durationMillis)
    }

    @Test
    fun zeroDurationSession_isFilteredOut() {
        val rawEvents = listOf(
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, 5000L, UsageSessionAnalyzer.EVENT_TYPE_RESUMED),
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, 5000L, UsageSessionAnalyzer.EVENT_TYPE_PAUSED) // same ms
        )

        val sessions = analyzer.analyze(rawEvents, 10000L)
        assertTrue("Zero-duration sessions must be discarded", sessions.isEmpty())
    }

    @Test
    fun invalidTimestamps_areFilteredOut() {
        val rawEvents = listOf(
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, -100L, UsageSessionAnalyzer.EVENT_TYPE_RESUMED),
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, 0L, UsageSessionAnalyzer.EVENT_TYPE_PAUSED)
        )

        val sessions = analyzer.analyze(rawEvents, 10000L)
        assertTrue("Negative and zero timestamps must be discarded", sessions.isEmpty())
    }

    @Test
    fun midnightDayBoundarySession_isSplitDeterministically() {
        // Construct a session spanning across midnight UTC
        // Day 1: 23:55 (5 minutes before midnight) to Day 2: 00:10 (10 minutes after midnight)
        val day1Midnight = LocalDate.of(2026, 10, 8).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val startTime = day1Midnight - (5 * 60 * 1000L) // 23:55
        val endTime = day1Midnight + (10 * 60 * 1000L)  // 00:10

        val session = ForegroundSession(
            packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
            startTimeMillis = startTime,
            endTimeMillis = endTime,
            durationMillis = 15 * 60 * 1000L
        )

        val split = analyzer.splitAtMidnightBoundaries(session)

        assertEquals(2, split.size)
        // Day 1 portion: 5 minutes up to midnight
        assertEquals(startTime, split[0].startTimeMillis)
        assertEquals(day1Midnight, split[0].endTimeMillis)
        assertEquals(5 * 60 * 1000L, split[0].durationMillis)

        // Day 2 portion: 10 minutes starting from midnight
        assertEquals(day1Midnight, split[1].startTimeMillis)
        assertEquals(endTime, split[1].endTimeMillis)
        assertEquals(10 * 60 * 1000L, split[1].durationMillis)
    }

    @Test
    fun unmonitoredPackages_areIgnored() {
        val rawEvents = listOf(
            RawUsageTimelineEvent("com.whatsapp", 1000L, UsageSessionAnalyzer.EVENT_TYPE_RESUMED),
            RawUsageTimelineEvent("com.whatsapp", 5000L, UsageSessionAnalyzer.EVENT_TYPE_PAUSED),
            RawUsageTimelineEvent("com.android.chrome", 6000L, UsageSessionAnalyzer.EVENT_TYPE_RESUMED),
            RawUsageTimelineEvent("com.android.chrome", 9000L, UsageSessionAnalyzer.EVENT_TYPE_PAUSED)
        )

        val sessions = analyzer.analyze(rawEvents, 10000L)
        assertTrue("Unmonitored packages must not generate analyzed sessions", sessions.isEmpty())
    }
}
