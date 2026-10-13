package com.stillfeed.app.feature.home

import com.stillfeed.app.awareness.AwarenessManager
import com.stillfeed.app.domain.model.AppPreferences
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.ProtectionState
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.domain.repository.AwarenessRepository
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.feature.awareness.AwarenessViewModel
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.usage.analysis.UsageSessionAnalyzer
import com.stillfeed.app.usage.stats.RawUsageTimelineEvent
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Unit tests verifying Home (Day) and Home (Evening) presentation states (STEP 18).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomePresentationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAwarenessRepository: FakeAwarenessRepository
    private lateinit var fakeUsageRepository: FakeUsageRepository
    private lateinit var fakeProtectionStateRepository: FakeProtectionStateRepository
    private lateinit var fakePreferencesRepository: FakeAppPreferencesRepository
    private lateinit var fakeUsageStatsSource: FakeUsageStatsDataSource
    private lateinit var usageAggregator: UsageAggregator
    private lateinit var awarenessManager: AwarenessManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAwarenessRepository = FakeAwarenessRepository()
        fakeUsageRepository = FakeUsageRepository()
        fakeProtectionStateRepository = FakeProtectionStateRepository()
        fakePreferencesRepository = FakeAppPreferencesRepository()
        fakeUsageStatsSource = FakeUsageStatsDataSource(hasPermission = true)

        usageAggregator = UsageAggregator(fakeUsageRepository)
        awarenessManager = AwarenessManager(
            awarenessRepository = fakeAwarenessRepository,
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun dayPresentation_whenClockIsDaytime_isEveningIsFalse() = runTest(testDispatcher) {
        // 2026-10-13T10:30:00Z (10:30 AM UTC -> Day)
        val fixedClock = Clock.fixed(Instant.parse("2026-10-13T10:30:00Z"), ZoneOffset.UTC)
        val viewModel = createViewModel(clock = fixedClock)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("At 10:30 AM, isEvening must be false", state.isEvening)
    }

    @Test
    fun eveningPresentation_whenClockIsEvening_isEveningIsTrue() = runTest(testDispatcher) {
        // 2026-10-13T19:45:00Z (7:45 PM UTC -> Evening)
        val fixedClock = Clock.fixed(Instant.parse("2026-10-13T19:45:00Z"), ZoneOffset.UTC)
        val viewModel = createViewModel(clock = fixedClock)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("At 19:45 PM, isEvening must be true", state.isEvening)
    }

    @Test
    fun lateNightPresentation_whenClockIsLateNight_isEveningIsTrue() = runTest(testDispatcher) {
        // 2026-10-13T02:15:00Z (2:15 AM UTC -> Night/Evening)
        val fixedClock = Clock.fixed(Instant.parse("2026-10-13T02:15:00Z"), ZoneOffset.UTC)
        val viewModel = createViewModel(clock = fixedClock)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("At 02:15 AM, isEvening must be true", state.isEvening)
    }

    @Test
    fun boundaryTransitions_dayAndEveningExactBoundaries() = runTest(testDispatcher) {
        // 05:59:59 -> Evening
        val clock0559 = Clock.fixed(Instant.parse("2026-10-13T05:59:59Z"), ZoneOffset.UTC)
        val vm0559 = createViewModel(clock = clock0559)
        advanceUntilIdle()
        assertTrue("05:59 must be evening", vm0559.uiState.value.isEvening)

        // 06:00:00 -> Day
        val clock0600 = Clock.fixed(Instant.parse("2026-10-13T06:00:00Z"), ZoneOffset.UTC)
        val vm0600 = createViewModel(clock = clock0600)
        advanceUntilIdle()
        assertFalse("06:00 must be day", vm0600.uiState.value.isEvening)

        // 17:59:59 -> Day
        val clock1759 = Clock.fixed(Instant.parse("2026-10-13T17:59:59Z"), ZoneOffset.UTC)
        val vm1759 = createViewModel(clock = clock1759)
        advanceUntilIdle()
        assertFalse("17:59 must be day", vm1759.uiState.value.isEvening)

        // 18:00:00 -> Evening
        val clock1800 = Clock.fixed(Instant.parse("2026-10-13T18:00:00Z"), ZoneOffset.UTC)
        val vm1800 = createViewModel(clock = clock1800)
        advanceUntilIdle()
        assertTrue("18:00 must be evening", vm1800.uiState.value.isEvening)
    }

    @Test
    fun dailyAllowance_reflectsPersistedPreferencesAndDoesNotResetOnTimeShift() = runTest(testDispatcher) {
        fakePreferencesRepository.setDailyReelsAllowance(45)

        val clockDay = Clock.fixed(Instant.parse("2026-10-13T14:00:00Z"), ZoneOffset.UTC)
        val viewModel = createViewModel(clock = clockDay)
        advanceUntilIdle()

        assertEquals(45, viewModel.uiState.value.dailyReelsAllowance)

        // Transition time to evening
        val clockEvening = Clock.fixed(Instant.parse("2026-10-13T20:00:00Z"), ZoneOffset.UTC)
        viewModel.loadMonthlyState(clockEvening.millis())
        advanceUntilIdle()

        // Allowance remains intact at 45
        assertEquals(45, viewModel.uiState.value.dailyReelsAllowance)
        assertTrue(viewModel.uiState.value.isEvening)
    }

    @Test
    fun usageData_unavailableReelCountContractIsHonored() = runTest(testDispatcher) {
        val clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC)
        val viewModel = createViewModel(clock = clock)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Must never invent or fabricate reel counts
        assertFalse("Reel counts must be flagged as unavailable pending tracking", state.isReelsCountAvailable)
        assertNull("Watched reels count must be null", state.watchedReelsToday)
    }

    @Test
    fun yesterdayComparison_calculatesDifferencesAccurately() = runTest(testDispatcher) {
        // Tuesday 2026-10-13 12:00 UTC
        val todayTime = Instant.parse("2026-10-13T12:00:00Z").toEpochMilli()
        val yesterdayTime = Instant.parse("2026-10-12T15:00:00Z").toEpochMilli()

        // Record 50 minutes yesterday on Instagram
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "ev_yesterday",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = com.stillfeed.app.domain.model.SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = yesterdayTime,
                durationMillis = 50 * 60000L
            )
        )

        // Record 30 minutes today on YouTube
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "ev_today",
                packageName = UsageSessionAnalyzer.PACKAGE_YOUTUBE,
                surfaceCategory = com.stillfeed.app.domain.model.SurfaceCategory.SHORTS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = todayTime,
                durationMillis = 30 * 60000L
            )
        )

        val clock = Clock.fixed(Instant.ofEpochMilli(todayTime), ZoneOffset.UTC)
        val viewModel = createViewModel(clock = clock)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(30, state.todayUsageMinutes)
        assertEquals(50, state.yesterdayUsageMinutes)
        // Today - Yesterday: 30 - 50 = -20 (less than yesterday)
        assertEquals(-20, state.yesterdayUsageDifferenceMinutes)
    }

    @Test
    fun weeklyRhythm_builds7DaysWithCorrectTodayHighlight() = runTest(testDispatcher) {
        // Tuesday 2026-10-13 12:00 UTC
        val fixedClock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC)
        val viewModel = createViewModel(clock = fixedClock)
        advanceUntilIdle()

        val weekly = viewModel.uiState.value.weeklyRhythm
        assertEquals(7, weekly.size)

        // Expected day labels Monday to Sunday: M, T, W, T, F, S, S
        assertEquals("M", weekly[0].dayLabel)
        assertEquals("T", weekly[1].dayLabel)
        assertEquals("W", weekly[2].dayLabel)
        assertEquals("T", weekly[3].dayLabel)
        assertEquals("F", weekly[4].dayLabel)
        assertEquals("S", weekly[5].dayLabel)
        assertEquals("S", weekly[6].dayLabel)

        // Tuesday (index 1) must be marked as today
        assertFalse(weekly[0].isToday)
        assertTrue(weekly[1].isToday)
        assertFalse(weekly[2].isToday)
    }

    private fun createViewModel(clock: Clock): AwarenessViewModel {
        return AwarenessViewModel(
            awarenessManager = awarenessManager,
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource,
            protectionStateRepository = fakeProtectionStateRepository,
            appPreferencesRepository = fakePreferencesRepository,
            clock = clock
        )
    }
}

// -----------------------------------------------------------------------------
// Test Doubles
// -----------------------------------------------------------------------------

private class FakeAppPreferencesRepository : AppPreferencesRepository {
    private var prefs = AppPreferences()
    private val flow = MutableStateFlow(prefs)

    override fun observePreferences(): Flow<AppPreferences> = flow.asStateFlow()
    override suspend fun getPreferences(): AppPreferences = prefs
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        prefs = prefs.copy(hasCompletedOnboarding = completed)
        flow.value = prefs
    }
    override suspend fun setOnboardingStep(step: Int) {
        prefs = prefs.copy(onboardingStep = step)
        flow.value = prefs
    }
    override suspend fun setDailyReelsAllowance(allowance: Int) {
        prefs = prefs.copy(dailyReelsAllowance = allowance)
        flow.value = prefs
    }
    override suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        prefs = prefs.copy(isFirstLaunch = isFirstLaunch)
        flow.value = prefs
    }
    override suspend fun setQuickProtectionToggle(enabled: Boolean) {
        prefs = prefs.copy(quickProtectionToggle = enabled)
        flow.value = prefs
    }
    override suspend fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
        prefs = transform(prefs)
        flow.value = prefs
    }
    override suspend fun clearPreferences() {
        prefs = AppPreferences()
        flow.value = prefs
    }
}

private class FakeAwarenessRepository : AwarenessRepository {
    private val store = mutableMapOf<String, AwarenessMonthlyState>()
    private val stateFlows = mutableMapOf<String, MutableStateFlow<AwarenessMonthlyState?>>()

    override fun observeMonthlyState(monthIdentifier: String): Flow<AwarenessMonthlyState?> {
        return stateFlows.getOrPut(monthIdentifier) { MutableStateFlow(store[monthIdentifier]) }
    }
    override suspend fun getMonthlyState(monthIdentifier: String): AwarenessMonthlyState? = store[monthIdentifier]
    override suspend fun saveMonthlyState(state: AwarenessMonthlyState) {
        store[state.monthIdentifier] = state
        stateFlows.getOrPut(state.monthIdentifier) { MutableStateFlow(state) }.value = state
    }
    override suspend fun getAllMonthlyStates(): List<AwarenessMonthlyState> = store.values.toList()
    override suspend fun deleteMonthlyState(monthIdentifier: String) {
        store.remove(monthIdentifier)
        stateFlows[monthIdentifier]?.value = null
    }
    override suspend fun clearAwarenessData() {
        store.clear()
        stateFlows.values.forEach { it.value = null }
    }
}

private class FakeProtectionStateRepository : ProtectionStateRepository {
    private val stateFlow = MutableStateFlow(ProtectionState(isEnabled = true))
    override fun observeProtectionState(): Flow<ProtectionState> = stateFlow.asStateFlow()
    override suspend fun getProtectionState(): ProtectionState = stateFlow.value
    override suspend fun setProtectionEnabled(enabled: Boolean) {
        stateFlow.value = ProtectionState(isEnabled = enabled, updatedAtMillis = System.currentTimeMillis())
    }
    override suspend fun clearProtectionState() {
        stateFlow.value = ProtectionState(isEnabled = false, updatedAtMillis = 0L)
    }
}

private class FakeUsageStatsDataSource(var hasPermission: Boolean = true) : UsageStatsDataSource {
    override fun hasUsageStatsPermission(): Boolean = hasPermission
    override fun queryTimelineEvents(startTimeMillis: Long, endTimeMillis: Long): List<RawUsageTimelineEvent> = emptyList()
}

private class FakeUsageRepository : UsageRepository {
    private val events = mutableListOf<UsageEvent>()
    override suspend fun recordEvent(event: UsageEvent) { events.add(event) }
    override suspend fun recordEvents(events: List<UsageEvent>) { this.events.addAll(events) }
    override suspend fun getEventById(id: String): UsageEvent? = events.find { it.id == id }
    override fun observeEventsInRange(startMillis: Long, endMillis: Long): Flow<List<UsageEvent>> = flowOf(events.filter { it.timestampMillis in startMillis..endMillis })
    override suspend fun getEventsInRange(startMillis: Long, endMillis: Long): List<UsageEvent> = events.filter { it.timestampMillis in startMillis..endMillis }
    override suspend fun getEventCount(): Int = events.size
    override suspend fun deleteEventsOlderThan(cutoffMillis: Long): Int {
        val before = events.size
        events.removeAll { it.timestampMillis < cutoffMillis }
        return before - events.size
    }
    override suspend fun clearUsageHistory() { events.clear() }
}
