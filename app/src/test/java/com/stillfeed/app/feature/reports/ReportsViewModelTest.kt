package com.stillfeed.app.feature.reports

import com.stillfeed.app.domain.model.AppPreferences
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.usage.aggregation.UsageAggregator
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

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePreferencesRepository: FakeAppPreferencesRepository
    private lateinit var fakeUsageRepository: FakeUsageRepository
    private lateinit var usageAggregator: UsageAggregator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePreferencesRepository = FakeAppPreferencesRepository(
            AppPreferences(dailyReelsAllowance = 30)
        )
        fakeUsageRepository = FakeUsageRepository()
        usageAggregator = UsageAggregator(fakeUsageRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun criticalMetricsRule_honestNullForReelsCount_neverFabricated() = runTest(testDispatcher) {
        val viewModel = ReportsViewModel(
            usageAggregator = usageAggregator,
            appPreferencesRepository = fakePreferencesRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // CRITICAL METRICS RULE: Verified reel counting must be null rather than fabricated
        assertNull("Weekly reels watched must be null if verified count unavailable", state.weeklyReelsWatched)
        assertNull("Yearly reels watched must be null if verified count unavailable", state.yearlyReelsWatched)
    }

    @Test
    fun selectMonthlyPace_updatesAllowanceAccurately() = runTest(testDispatcher) {
        val viewModel = ReportsViewModel(
            usageAggregator = usageAggregator,
            appPreferencesRepository = fakePreferencesRepository
        )
        advanceUntilIdle()

        // Base allowance is 30
        // Gentle: 5% reduction -> 30 * 0.95 = 28.5 -> 28
        viewModel.selectMonthlyPace(5)
        assertEquals(5, viewModel.uiState.value.selectedMonthlyPacePercentage)
        assertEquals(28, viewModel.uiState.value.selectedDailyAllowanceReels)
        assertFalse(viewModel.uiState.value.isMonthlyAllowanceSaved)

        // Balanced: 10% reduction -> 30 * 0.9 = 27
        viewModel.selectMonthlyPace(10)
        assertEquals(10, viewModel.uiState.value.selectedMonthlyPacePercentage)
        assertEquals(27, viewModel.uiState.value.selectedDailyAllowanceReels)

        // Focused: 15% reduction -> 30 * 0.85 = 25.5 -> 25
        viewModel.selectMonthlyPace(15)
        assertEquals(15, viewModel.uiState.value.selectedMonthlyPacePercentage)
        assertEquals(25, viewModel.uiState.value.selectedDailyAllowanceReels)

        // Challenge: 20% reduction -> 30 * 0.80 = 24
        viewModel.selectMonthlyPace(20)
        assertEquals(20, viewModel.uiState.value.selectedMonthlyPacePercentage)
        assertEquals(24, viewModel.uiState.value.selectedDailyAllowanceReels)
    }

    @Test
    fun applyMonthlyLimit_persistsToPreferences_andMarksSaved() = runTest(testDispatcher) {
        val viewModel = ReportsViewModel(
            usageAggregator = usageAggregator,
            appPreferencesRepository = fakePreferencesRepository
        )
        advanceUntilIdle()

        viewModel.selectMonthlyPace(10) // 27 reels
        var callbackInvoked = false
        viewModel.applyMonthlyLimit { callbackInvoked = true }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("isMonthlyAllowanceSaved should be true after applying", state.isMonthlyAllowanceSaved)
        assertEquals(27, state.currentDailyAllowanceReels)
        assertTrue(callbackInvoked)

        // Verify persisted to preferences
        assertEquals(27, fakePreferencesRepository.getPreferences().dailyReelsAllowance)
    }

    @Test
    fun weeklyReport_buildsSevenDaysPattern() = runTest(testDispatcher) {
        val viewModel = ReportsViewModel(
            usageAggregator = usageAggregator,
            appPreferencesRepository = fakePreferencesRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Weekly pattern must have exactly 7 days", 7, state.weeklyPattern.size)
        assertTrue("One day must be marked as today", state.weeklyPattern.any { it.isToday })
    }

    @Test
    fun yearlyReport_maintainsExpectedTrendData() = runTest(testDispatcher) {
        val viewModel = ReportsViewModel(
            usageAggregator = usageAggregator,
            appPreferencesRepository = fakePreferencesRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Yearly trend must contain 12 months", 12, state.yearlyTrendMonthlyHours.size)
        assertEquals("October", state.bestMonthName)
        assertEquals(22, state.bestMonthReductionPercentage)
        assertTrue(state.isSecondHalfLower)
    }

    private class FakeAppPreferencesRepository(initialPrefs: AppPreferences = AppPreferences()) : AppPreferencesRepository {
        private var prefs = initialPrefs
        private val flow = MutableStateFlow(prefs)

        override fun observePreferences(): Flow<AppPreferences> = flow.asStateFlow()
        override suspend fun getPreferences(): AppPreferences = prefs
        override suspend fun setOnboardingCompleted(completed: Boolean) { updatePreferences { it.copy(hasCompletedOnboarding = completed) } }
        override suspend fun setFirstLaunch(isFirstLaunch: Boolean) { updatePreferences { it.copy(isFirstLaunch = isFirstLaunch) } }
        override suspend fun setQuickProtectionToggle(enabled: Boolean) { updatePreferences { it.copy(quickProtectionToggle = enabled) } }
        override suspend fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
            prefs = transform(prefs)
            flow.value = prefs
        }
        override suspend fun clearPreferences() {
            prefs = AppPreferences()
            flow.value = prefs
        }
    }

    private class FakeUsageRepository : UsageRepository {
        private val events = mutableListOf<UsageEvent>()
        override suspend fun recordEvent(event: UsageEvent) { events.add(event) }
        override suspend fun recordEvents(events: List<UsageEvent>) { this.events.addAll(events) }
        override suspend fun getEventById(id: String): UsageEvent? = events.find { it.id == id }
        override fun observeEventsInRange(startMillis: Long, endMillis: Long): Flow<List<UsageEvent>> = flowOf(events.filter { it.timestampMillis in startMillis..endMillis })
        override suspend fun getEventsInRange(startMillis: Long, endMillis: Long): List<UsageEvent> = events.filter { it.timestampMillis in startMillis..endMillis }
        override suspend fun getEventCount(): Int = events.size
        override suspend fun deleteEventsOlderThan(cutoffMillis: Long): Int = 0
        override suspend fun clearUsageHistory() { events.clear() }
    }
}
