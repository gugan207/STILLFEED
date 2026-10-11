package com.stillfeed.app.feature.insights

import com.stillfeed.app.awareness.AwarenessManager
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.domain.repository.AwarenessRepository
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.usage.analysis.UsageSessionAnalyzer
import com.stillfeed.app.usage.stats.RawUsageTimelineEvent
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for [InsightsViewModel] and historical usage experience (STEP 10 Section 2, 3, 5, 6, 11).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeUsageRepository: FakeUsageRepository
    private lateinit var fakeAwarenessRepository: FakeAwarenessRepository
    private lateinit var fakeUsageStatsSource: FakeUsageStatsDataSource
    private lateinit var usageAggregator: UsageAggregator
    private lateinit var awarenessManager: AwarenessManager
    private lateinit var viewModel: InsightsViewModel

    // Fixed test timestamp: 2026-10-15 12:00:00 UTC
    private val fixedNowMillis = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeUsageRepository = FakeUsageRepository()
        fakeAwarenessRepository = FakeAwarenessRepository()
        fakeUsageStatsSource = FakeUsageStatsDataSource(hasPermission = true)

        usageAggregator = UsageAggregator(fakeUsageRepository)
        awarenessManager = AwarenessManager(
            awarenessRepository = fakeAwarenessRepository,
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource
        )

        viewModel = InsightsViewModel(
            usageAggregator = usageAggregator,
            awarenessManager = awarenessManager,
            usageStatsDataSource = fakeUsageStatsSource
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_withNoData_rendersFirstDayEmptyState() = runTest(testDispatcher) {
        viewModel.loadInsights(fixedNowMillis)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isFirstDayOrEmpty)
        assertEquals(0, state.weeklyMonitoredMinutes)
        assertEquals(0, state.todayMinutes)
        assertNull(state.errorMessage)
    }

    @Test
    fun loadInsights_aggregatesWeeklyDataAndBreakdownAccurately() = runTest(testDispatcher) {
        // Seed usage events in the current week (Oct 9 to Oct 15)
        val day1Millis = ZonedDateTime.of(2026, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        val day2Millis = ZonedDateTime.of(2026, 10, 12, 14, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

        // Day 1: Instagram session (30 mins = 1,800,000 ms) + 3 blocked interventions
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "e1",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = day1Millis,
                durationMillis = 30 * 60 * 1000L
            )
        )
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "e2",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.BLOCKED,
                timestampMillis = day1Millis + 1000L,
                isBlocked = true
            )
        )

        // Day 2: YouTube session (20 mins = 1,200,000 ms) + 2 blocked interventions
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "e3",
                packageName = UsageSessionAnalyzer.PACKAGE_YOUTUBE,
                surfaceCategory = SurfaceCategory.SHORTS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = day2Millis,
                durationMillis = 20 * 60 * 1000L
            )
        )
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "e4",
                packageName = UsageSessionAnalyzer.PACKAGE_YOUTUBE,
                surfaceCategory = SurfaceCategory.SHORTS,
                eventType = UsageEventType.BLOCKED,
                timestampMillis = day2Millis + 1000L,
                isBlocked = true
            )
        )

        viewModel.loadInsights(fixedNowMillis)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isFirstDayOrEmpty)
        assertEquals(50, state.weeklyMonitoredMinutes)
        assertEquals(30, state.instagramWeeklyMinutes)
        assertEquals(20, state.youtubeWeeklyMinutes)
        assertEquals(2, state.blockedInterventionCount)
    }

    @Test
    fun loadInsights_computesWeekOverWeekDifference() = runTest(testDispatcher) {
        // Prior week (Oct 2 to Oct 8): 60 mins of usage
        val priorWeekMillis = ZonedDateTime.of(2026, 10, 5, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "prior1",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = priorWeekMillis,
                durationMillis = 60 * 60 * 1000L
            )
        )

        // Current week (Oct 9 to Oct 15): 45 mins of usage (reduction of 15 mins)
        val currentWeekMillis = ZonedDateTime.of(2026, 10, 11, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "curr1",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = currentWeekMillis,
                durationMillis = 45 * 60 * 1000L
            )
        )

        val currentMonth = awarenessManager.getCurrentMonthIdentifier(fixedNowMillis)
        fakeAwarenessRepository.saveMonthlyState(
            AwarenessMonthlyState(
                monthIdentifier = currentMonth,
                targetReductionPercentage = 10,
                baselineMinutes = 600,
                currentProgressMinutes = 200,
                targetUsageMinutes = 540,
                reductionAmountMinutes = 60,
                status = MonthlyAwarenessStatus.REDUCING
            )
        )

        viewModel.loadInsights(fixedNowMillis)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(45, state.weeklyMonitoredMinutes)
        assertEquals(60, state.previousWeeklyMinutes)
        assertEquals(-15, state.weeklyDifferenceMinutes)
        assertEquals(-25, state.weeklyDifferencePercentage) // (45 - 60) / 60 = -25%
        assertTrue(state.reflectionHeadline.contains("lowest-use days"))
    }

    @Test
    fun dailyRhythm_generatesSevenDayItems_withNormalizedFractions() = runTest(testDispatcher) {
        // Seed 1 event on one day
        val dayMillis = ZonedDateTime.of(2026, 10, 12, 10, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "r1",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = dayMillis,
                durationMillis = 40 * 60 * 1000L
            )
        )

        viewModel.loadInsights(fixedNowMillis)
        advanceUntilIdle()

        val rhythm = viewModel.uiState.value.dailyRhythm
        assertEquals(7, rhythm.size)
        // Today is Oct 15
        assertTrue(rhythm.any { it.isToday })
        // Check labels are non-empty day characters
        rhythm.forEach { item ->
            assertTrue(item.dayLabel in listOf("M", "T", "W", "T", "F", "S", "S"))
            assertTrue(item.normalizedHeightFraction in 0.05f..1.0f)
        }
    }

    @Test
    fun missingUsageStatsPermission_rendersUnavailableEmptyState() = runTest(testDispatcher) {
        fakeUsageStatsSource.hasPermission = false

        viewModel.loadInsights(fixedNowMillis)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.hasUsageStatsPermission)
        assertTrue(state.isFirstDayOrEmpty)
    }

    @Test
    fun awarenessIntegration_reflectsTargetAndBaselineContext() = runTest(testDispatcher) {
        // Pre-seed monthly awareness state in repository
        val currentMonth = awarenessManager.getCurrentMonthIdentifier(fixedNowMillis)
        val state = AwarenessMonthlyState(
            monthIdentifier = currentMonth,
            targetReductionPercentage = 10,
            baselineMinutes = 600,
            currentProgressMinutes = 200,
            targetUsageMinutes = 540,
            reductionAmountMinutes = 60,
            status = MonthlyAwarenessStatus.REDUCING
        )
        fakeAwarenessRepository.saveMonthlyState(state)

        viewModel.loadInsights(fixedNowMillis)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.monthlyAwarenessState)
        assertEquals(10, uiState.monthlyAwarenessState?.targetReductionPercentage)
        assertEquals(600, uiState.monthlyAwarenessState?.baselineMinutes)
        assertEquals(540, uiState.monthlyAwarenessState?.targetUsageMinutes)
    }

    @Test
    fun processRecreation_restoresInsightsState() = runTest(testDispatcher) {
        // Seed 1 event
        val dayMillis = ZonedDateTime.of(2026, 10, 11, 10, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "pr1",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = dayMillis,
                durationMillis = 25 * 60 * 1000L
            )
        )

        viewModel.loadInsights(fixedNowMillis)
        advanceUntilIdle()
        assertEquals(25, viewModel.uiState.value.weeklyMonitoredMinutes)

        // Simulate process recreation: fresh ViewModel reads same repositories
        val recreatedViewModel = InsightsViewModel(
            usageAggregator = usageAggregator,
            awarenessManager = awarenessManager,
            usageStatsDataSource = fakeUsageStatsSource
        )
        recreatedViewModel.loadInsights(fixedNowMillis)
        advanceUntilIdle()

        val recreatedState = recreatedViewModel.uiState.value
        assertEquals(25, recreatedState.weeklyMonitoredMinutes)
        assertFalse(recreatedState.isFirstDayOrEmpty)
    }
}

// -----------------------------------------------------------------------------
// Test Doubles
// -----------------------------------------------------------------------------

private class FakeAwarenessRepository : AwarenessRepository {
    private val store = mutableMapOf<String, AwarenessMonthlyState>()
    private val stateFlows = mutableMapOf<String, MutableStateFlow<AwarenessMonthlyState?>>()

    override fun observeMonthlyState(monthIdentifier: String): Flow<AwarenessMonthlyState?> {
        return stateFlows.getOrPut(monthIdentifier) { MutableStateFlow(store[monthIdentifier]) }
    }

    override suspend fun getMonthlyState(monthIdentifier: String): AwarenessMonthlyState? {
        return store[monthIdentifier]
    }

    override suspend fun saveMonthlyState(state: AwarenessMonthlyState) {
        store[state.monthIdentifier] = state
        stateFlows.getOrPut(state.monthIdentifier) { MutableStateFlow(state) }.value = state
    }

    override suspend fun getAllMonthlyStates(): List<AwarenessMonthlyState> {
        return store.values.toList()
    }

    override suspend fun deleteMonthlyState(monthIdentifier: String) {
        store.remove(monthIdentifier)
        stateFlows[monthIdentifier]?.value = null
    }

    override suspend fun clearAwarenessData() {
        store.clear()
        stateFlows.values.forEach { it.value = null }
    }
}

private class FakeUsageStatsDataSource(var hasPermission: Boolean = true) : UsageStatsDataSource {
    override fun hasUsageStatsPermission(): Boolean = hasPermission
    override fun queryTimelineEvents(startTimeMillis: Long, endTimeMillis: Long): List<RawUsageTimelineEvent> = emptyList()
}

private class FakeUsageRepository : UsageRepository {
    private val events = mutableListOf<UsageEvent>()

    override suspend fun recordEvent(event: UsageEvent) {
        events.add(event)
    }

    override suspend fun recordEvents(events: List<UsageEvent>) {
        this.events.addAll(events)
    }

    override suspend fun getEventById(id: String): UsageEvent? {
        return events.find { it.id == id }
    }

    override fun observeEventsInRange(startMillis: Long, endMillis: Long): Flow<List<UsageEvent>> {
        return kotlinx.coroutines.flow.flowOf(getEventsInRangeSync(startMillis, endMillis))
    }

    override suspend fun getEventsInRange(startMillis: Long, endMillis: Long): List<UsageEvent> {
        return getEventsInRangeSync(startMillis, endMillis)
    }

    private fun getEventsInRangeSync(startMillis: Long, endMillis: Long): List<UsageEvent> {
        return events.filter { it.timestampMillis in startMillis..endMillis }
    }

    override suspend fun getEventCount(): Int = events.size

    override suspend fun deleteEventsOlderThan(cutoffMillis: Long): Int {
        val before = events.size
        events.removeAll { it.timestampMillis < cutoffMillis }
        return before - events.size
    }

    override suspend fun clearUsageHistory() {
        events.clear()
    }
}
