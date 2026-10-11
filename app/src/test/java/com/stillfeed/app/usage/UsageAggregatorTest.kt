package com.stillfeed.app.usage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.repository.LocalUsageRepository
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.usage.analysis.UsageSessionAnalyzer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsageAggregatorTest {

    private lateinit var database: StillfeedDatabase
    private lateinit var usageRepository: LocalUsageRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var aggregator: UsageAggregator

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        usageRepository = LocalUsageRepository(database.usageEventDao(), testDispatcher)
        aggregator = UsageAggregator(usageRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getTodayUsage_whenEmpty_returnsZeroTotals() = testScope.runTest {
        val summary = aggregator.getTodayUsage()
        assertEquals(0L, summary.instagramUsageMillis)
        assertEquals(0L, summary.youtubeUsageMillis)
        assertEquals(0L, summary.totalMonitoredMillis)
        assertEquals(0, summary.blockedInterventionCount)
        assertEquals(0, summary.sessionCount)
    }

    @Test
    fun getTodayUsage_calculatesInstagramAndYouTubeTotalsCorrectly() = testScope.runTest {
        val baseDate = LocalDate.of(2026, 10, 8)
        val todayStart = baseDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val nowMillis = todayStart + (12 * 60 * 60 * 1000L) // Noon

        // Instagram session: 15 minutes (900,000 ms)
        val igSession = UsageEvent(
            id = UUID.randomUUID().toString(),
            timestampMillis = todayStart + (2 * 60 * 60 * 1000L),
            packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
            surfaceCategory = SurfaceCategory.FEED,
            eventType = UsageEventType.SESSION_END,
            durationMillis = 900_000L
        )

        // YouTube session: 25 minutes (1,500,000 ms)
        val ytSession = UsageEvent(
            id = UUID.randomUUID().toString(),
            timestampMillis = todayStart + (4 * 60 * 60 * 1000L),
            packageName = UsageSessionAnalyzer.PACKAGE_YOUTUBE,
            surfaceCategory = SurfaceCategory.FEED,
            eventType = UsageEventType.SESSION_END,
            durationMillis = 1_500_000L
        )

        // Blocked intervention event
        val blockedEvent = UsageEvent(
            id = UUID.randomUUID().toString(),
            timestampMillis = todayStart + (5 * 60 * 60 * 1000L),
            packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
            surfaceCategory = SurfaceCategory.REELS,
            eventType = UsageEventType.BLOCKED,
            isBlocked = true
        )

        usageRepository.recordEvents(listOf(igSession, ytSession, blockedEvent))

        val summary = aggregator.getTodayUsage(nowMillis)

        assertEquals("2026-10-08", summary.dateString)
        assertEquals(900_000L, summary.instagramUsageMillis)
        assertEquals(1_500_000L, summary.youtubeUsageMillis)
        assertEquals(2_400_000L, summary.totalMonitoredMillis)
        assertEquals(1, summary.blockedInterventionCount)
        assertEquals(2, summary.sessionCount)
    }

    @Test
    fun getHistoricalDailySummaries_aggregatesAcrossMultipleDates() = testScope.runTest {
        val day1Date = LocalDate.of(2026, 10, 1)
        val day1Start = day1Date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val day2Date = LocalDate.of(2026, 10, 2)
        val day2Start = day2Date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val day3Date = LocalDate.of(2026, 10, 3)
        val day3Start = day3Date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        // Day 1: 10 mins IG
        val eventDay1 = UsageEvent(
            id = UUID.randomUUID().toString(),
            timestampMillis = day1Start + 1000L,
            packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
            surfaceCategory = SurfaceCategory.FEED,
            eventType = UsageEventType.SESSION_END,
            durationMillis = 600_000L
        )

        // Day 2: 20 mins YT
        val eventDay2 = UsageEvent(
            id = UUID.randomUUID().toString(),
            timestampMillis = day2Start + 1000L,
            packageName = UsageSessionAnalyzer.PACKAGE_YOUTUBE,
            surfaceCategory = SurfaceCategory.FEED,
            eventType = UsageEventType.SESSION_END,
            durationMillis = 1_200_000L
        )

        usageRepository.recordEvents(listOf(eventDay1, eventDay2))

        val historical = aggregator.getHistoricalDailySummaries(day1Start, day3Start)

        assertEquals(2, historical.size)
        assertEquals("2026-10-01", historical[0].dateString)
        assertEquals(600_000L, historical[0].instagramUsageMillis)
        assertEquals(0L, historical[0].youtubeUsageMillis)

        assertEquals("2026-10-02", historical[1].dateString)
        assertEquals(0L, historical[1].instagramUsageMillis)
        assertEquals(1_200_000L, historical[1].youtubeUsageMillis)
    }
}
