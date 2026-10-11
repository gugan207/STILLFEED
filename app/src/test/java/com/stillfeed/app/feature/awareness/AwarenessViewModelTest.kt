package com.stillfeed.app.feature.awareness

import com.stillfeed.app.awareness.AwarenessManager
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import com.stillfeed.app.domain.model.ProtectionState
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.domain.repository.AwarenessRepository
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.usage.aggregation.DailyUsageSummary
import com.stillfeed.app.usage.aggregation.UsageAggregator
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

/**
 * Unit & Integration tests for [AwarenessViewModel] and product UI states (STEP 09 Section 3, 4, 5, 8, 10).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AwarenessViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAwarenessRepository: FakeAwarenessRepository
    private lateinit var fakeUsageRepository: FakeUsageRepository
    private lateinit var fakeProtectionStateRepository: FakeProtectionStateRepository
    private lateinit var fakeUsageStatsSource: FakeUsageStatsDataSource
    private lateinit var usageAggregator: UsageAggregator
    private lateinit var awarenessManager: AwarenessManager
    private lateinit var viewModel: AwarenessViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAwarenessRepository = FakeAwarenessRepository()
        fakeUsageRepository = FakeUsageRepository()
        fakeProtectionStateRepository = FakeProtectionStateRepository()
        fakeUsageStatsSource = FakeUsageStatsDataSource(hasPermission = true)

        usageAggregator = UsageAggregator(fakeUsageRepository)
        awarenessManager = AwarenessManager(
            awarenessRepository = fakeAwarenessRepository,
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource
        )

        viewModel = AwarenessViewModel(
            awarenessManager = awarenessManager,
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource,
            protectionStateRepository = fakeProtectionStateRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsCurrentMonthlyState_formingBaseline() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(MonthlyAwarenessStatus.BASELINE_FORMING, state.status)
        assertEquals(0, state.targetReductionPercentage)
        assertFalse(state.isLockedForMonth)
        assertTrue(state.hasUsageStatsPermission)
    }

    @Test
    fun selectPercentageOptions_allFourValidOptions_updateInFlightState() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onSelectPercentageOption(5)
        assertEquals(5, viewModel.uiState.value.userSelectedPercentage)

        viewModel.onSelectPercentageOption(10)
        assertEquals(10, viewModel.uiState.value.userSelectedPercentage)

        viewModel.onSelectPercentageOption(15)
        assertEquals(15, viewModel.uiState.value.userSelectedPercentage)

        viewModel.onSelectPercentageOption(20)
        assertEquals(20, viewModel.uiState.value.userSelectedPercentage)
    }

    @Test
    fun selectPercentageOption_rejectsInvalidValues() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onSelectPercentageOption(10)
        assertEquals(10, viewModel.uiState.value.userSelectedPercentage)

        // Invalid values must not alter the selection
        viewModel.onSelectPercentageOption(0)
        assertEquals(10, viewModel.uiState.value.userSelectedPercentage)

        viewModel.onSelectPercentageOption(7)
        assertEquals(10, viewModel.uiState.value.userSelectedPercentage)

        viewModel.onSelectPercentageOption(25)
        assertEquals(10, viewModel.uiState.value.userSelectedPercentage)

        viewModel.onSelectPercentageOption(-5)
        assertEquals(10, viewModel.uiState.value.userSelectedPercentage)
    }

    @Test
    fun confirmPercentageSelection_locksStateForMonth_andPersistsToRepository() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onSelectPercentageOption(15)
        var callbackInvoked = false
        viewModel.confirmPercentageSelection {
            callbackInvoked = true
        }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        val state = viewModel.uiState.value
        assertEquals(15, state.targetReductionPercentage)
        assertTrue(state.isLockedForMonth)

        // Verify state is persisted
        val currentMonth = awarenessManager.getCurrentMonthIdentifier()
        val persisted = fakeAwarenessRepository.getMonthlyState(currentMonth)
        assertNotNull(persisted)
        assertEquals(15, persisted?.targetReductionPercentage)
    }

    @Test
    fun processRecreation_restoresPersistedMonthlyState() = runTest(testDispatcher) {
        advanceUntilIdle()

        // 1. User selects and locks 10%
        viewModel.onSelectPercentageOption(10)
        viewModel.confirmPercentageSelection()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLockedForMonth)

        // 2. Simulate process recreation: Create fresh ViewModel instance reading from same repository
        val recreatedViewModel = AwarenessViewModel(
            awarenessManager = awarenessManager,
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource,
            protectionStateRepository = fakeProtectionStateRepository
        )
        advanceUntilIdle()

        val recreatedState = recreatedViewModel.uiState.value
        assertFalse(recreatedState.isLoading)
        assertTrue(recreatedState.isLockedForMonth)
        assertEquals(10, recreatedState.targetReductionPercentage)
        assertEquals(10, recreatedState.userSelectedPercentage)
    }

    @Test
    fun missingUsageStatsPermission_setsStatusToDataUnavailable_andDoesNotCrash() = runTest(testDispatcher) {
        fakeUsageStatsSource.hasPermission = false

        viewModel.loadMonthlyState()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.hasUsageStatsPermission)
        assertEquals(MonthlyAwarenessStatus.DATA_UNAVAILABLE, state.status)
        assertNull(state.errorMessage)
    }

    @Test
    fun awarenessStates_renderingContract() = runTest(testDispatcher) {
        advanceUntilIdle()
        val currentMonth = awarenessManager.getCurrentMonthIdentifier()

        // 1. Baseline forming
        val stateForming = AwarenessMonthlyState(
            monthIdentifier = currentMonth,
            targetReductionPercentage = 0,
            baselineMinutes = 0,
            currentProgressMinutes = 0,
            targetUsageMinutes = 0,
            reductionAmountMinutes = 0,
            status = MonthlyAwarenessStatus.BASELINE_FORMING
        )
        fakeAwarenessRepository.saveMonthlyState(stateForming)
        viewModel.loadMonthlyState()
        advanceUntilIdle()
        assertEquals(MonthlyAwarenessStatus.BASELINE_FORMING, viewModel.uiState.value.status)

        // 2. Permission missing -> Data unavailable
        fakeUsageStatsSource.hasPermission = false
        viewModel.loadMonthlyState()
        advanceUntilIdle()
        assertEquals(MonthlyAwarenessStatus.DATA_UNAVAILABLE, viewModel.uiState.value.status)
        fakeUsageStatsSource.hasPermission = true

        // 3. User locked 10% target with established baseline
        viewModel.onSelectPercentageOption(10)
        viewModel.confirmPercentageSelection()
        advanceUntilIdle()

        val currentState = viewModel.uiState.value
        assertTrue(currentState.isLockedForMonth)
        assertEquals(10, currentState.targetReductionPercentage)
    }

    @Test
    fun protectionStateRepository_updatesIsProtectionEnabledInUiState() = runTest(testDispatcher) {
        advanceUntilIdle()

        fakeProtectionStateRepository.setProtectionEnabled(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isProtectionEnabled)

        fakeProtectionStateRepository.setProtectionEnabled(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isProtectionEnabled)
    }

    @Test
    fun formatMinutes_formatsDurationsCalmlyAndAccurately() {
        assertEquals("0m", AwarenessViewModel.formatMinutes(0))
        assertEquals("0m", AwarenessViewModel.formatMinutes(-15))
        assertEquals("35m", AwarenessViewModel.formatMinutes(35))
        assertEquals("1h", AwarenessViewModel.formatMinutes(60))
        assertEquals("1h 15m", AwarenessViewModel.formatMinutes(75))
        assertEquals("2h", AwarenessViewModel.formatMinutes(120))
        assertEquals("2h 45m", AwarenessViewModel.formatMinutes(165))
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
