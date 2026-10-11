package com.stillfeed.app.awareness

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.repository.LocalAwarenessRepository
import com.stillfeed.app.data.repository.LocalUsageRepository
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for [AwarenessManager] (STEP 08 Section 2, 3, 6 & 10).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AwarenessManagerTest {

    private lateinit var database: StillfeedDatabase
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var awarenessRepository: LocalAwarenessRepository
    private lateinit var usageRepository: LocalUsageRepository
    private lateinit var usageAggregator: UsageAggregator
    private lateinit var awarenessManager: AwarenessManager

    private var mockPermissionGranted = true

    private val fakeDataSource = object : UsageStatsDataSource {
        override fun hasUsageStatsPermission(): Boolean = mockPermissionGranted
        override fun queryTimelineEvents(startTimeMillis: Long, endTimeMillis: Long): List<com.stillfeed.app.usage.stats.RawUsageTimelineEvent> = emptyList()
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        awarenessRepository = LocalAwarenessRepository(database.awarenessMonthlyDao(), testDispatcher)
        usageRepository = LocalUsageRepository(database.usageEventDao(), testDispatcher)
        usageAggregator = UsageAggregator(usageRepository)

        mockPermissionGranted = true

        awarenessManager = AwarenessManager(
            awarenessRepository = awarenessRepository,
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeDataSource
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getOrCreateMonthlyState_createsNewIsolatedState() = runTest(testDispatcher) {
        // Oct 15, 2026 UTC
        val nowMillis = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

        val state = awarenessManager.getOrCreateMonthlyState("2026-10", nowMillis)
        assertNotNull(state)
        assertEquals("2026-10", state.monthIdentifier)
        assertEquals(0, state.targetReductionPercentage)
        assertEquals(MonthlyAwarenessStatus.BASELINE_FORMING, state.status)

        val retrieved = awarenessRepository.getMonthlyState("2026-10")
        assertNotNull(retrieved)
        assertEquals("2026-10", retrieved?.monthIdentifier)
    }

    @Test
    fun selectMonthlyAwarenessPercentage_setsValidPercentageAndCalculatesTargets() = runTest(testDispatcher) {
        // Oct 15, 2026 UTC
        val nowMillis = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

        // Seed 7 days in September to form a 600-min baseline
        seedHistoricalSeptemberDays()

        val initial = awarenessManager.getOrCreateMonthlyState("2026-10", nowMillis)
        assertEquals(600, initial.baselineMinutes)

        // Select 15% reduction
        val result = awarenessManager.selectMonthlyAwarenessPercentage("2026-10", 15, nowMillis)
        assertTrue(result.isSuccess)

        val updated = result.getOrThrow()
        assertEquals(15, updated.targetReductionPercentage)
        assertEquals(90, updated.reductionAmountMinutes)
        assertEquals(510, updated.targetUsageMinutes)
        assertEquals(MonthlyAwarenessStatus.REDUCING, updated.status)
    }

    @Test
    fun selectMonthlyAwarenessPercentage_rejectsInvalidPercentage() = runTest(testDispatcher) {
        val nowMillis = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

        val result = awarenessManager.selectMonthlyAwarenessPercentage("2026-10", 7, nowMillis)
        assertTrue(result.isFailure)
    }

    @Test
    fun monthlyIsolation_previousMonthIsNeverMutatedByCurrentMonth() = runTest(testDispatcher) {
        val septNowMillis = ZonedDateTime.of(2026, 9, 15, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        val octNowMillis = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

        // 1. Setup September with 10%
        seedHistoricalSeptemberDays()
        awarenessManager.getOrCreateMonthlyState("2026-09", septNowMillis)
        awarenessManager.selectMonthlyAwarenessPercentage("2026-09", 10, septNowMillis)

        val septStateBefore = awarenessRepository.getMonthlyState("2026-09")
        assertEquals(10, septStateBefore?.targetReductionPercentage)

        // 2. Setup October with 20%
        awarenessManager.getOrCreateMonthlyState("2026-10", octNowMillis)
        awarenessManager.selectMonthlyAwarenessPercentage("2026-10", 20, octNowMillis)

        // 3. Verify September was NOT mutated
        val septStateAfter = awarenessRepository.getMonthlyState("2026-09")
        val octState = awarenessRepository.getMonthlyState("2026-10")

        assertEquals(10, septStateAfter?.targetReductionPercentage)
        assertEquals(20, octState?.targetReductionPercentage)
    }

    @Test
    fun permissionMissing_setsDataUnavailableStatus() = runTest(testDispatcher) {
        mockPermissionGranted = false
        val nowMillis = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

        val state = awarenessManager.getOrCreateMonthlyState("2026-10", nowMillis)
        assertEquals(MonthlyAwarenessStatus.DATA_UNAVAILABLE, state.status)
    }

    private suspend fun seedHistoricalSeptemberDays() {
        // 7 days in late September: 23 to 29 September 2026
        // Each day: 600 minutes (10 hours) = 36,000,000 ms
        for (day in 23..29) {
            val startOfDay = ZonedDateTime.of(2026, 9, day, 10, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
            val event = UsageEvent(
                id = "evt_sept_$day",
                timestampMillis = startOfDay,
                packageName = "com.instagram.android",
                surfaceCategory = SurfaceCategory.FEED,
                eventType = UsageEventType.SESSION_END,
                durationMillis = 600 * 60000L,
                isBlocked = false
            )
            usageRepository.recordEvent(event)
        }
    }
}
