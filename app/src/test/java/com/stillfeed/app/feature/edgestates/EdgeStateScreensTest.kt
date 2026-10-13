package com.stillfeed.app.feature.edgestates

import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.feature.edgestates.connectivity.NetworkConnectivityObserver
import com.stillfeed.app.protection.permission.AccessibilityPermissionChecker
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

/**
 * Isolated unit tests for Step 20A Edge-State state models, ViewModels, and UI contracts.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EdgeStateScreensTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAccessibilityChecker: FakeAccessibilityPermissionChecker
    private lateinit var fakeUsageStatsSource: FakeUsageStatsDataSource
    private lateinit var fakeNetworkObserver: FakeNetworkConnectivityObserver
    private lateinit var fakeUsageRepository: FakeUsageRepository
    private lateinit var usageAggregator: UsageAggregator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAccessibilityChecker = FakeAccessibilityPermissionChecker(granted = true)
        fakeUsageStatsSource = FakeUsageStatsDataSource(hasPermission = true)
        fakeNetworkObserver = FakeNetworkConnectivityObserver(isOnline = true)
        fakeUsageRepository = FakeUsageRepository()
        usageAggregator = UsageAggregator(fakeUsageRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // Screen 15: Batch Finished Tests
    // =========================================================================

    @Test
    fun batchFinished_whenReelCountUnavailable_displaysHonestFallback() = runTest(testDispatcher) {
        val viewModel = BatchFinishedViewModel(
            usageAggregator = usageAggregator,
            initialDailyAllowanceReels = 30,
            initialBatchReels = null,
            initialBatchDurationMinutes = 15
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Reel counts must not be claimed as verified", state.isReelsCountAvailable)
        assertNull("Watched reels number must be null when tracking unintercepted", state.todayReelsWatched)
        assertEquals("Your unlocked batch is done. Take a reset if you want another small batch.", state.body)
        assertEquals(30, state.dailyAllowanceReels)
    }

    @Test
    fun batchFinished_distinguishesBatchExpirationFromDailyAllowance() = runTest(testDispatcher) {
        val viewModel = BatchFinishedViewModel(
            usageAggregator = usageAggregator,
            initialDailyAllowanceReels = 40,
            initialBatchReels = 20,
            initialBatchDurationMinutes = 20
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Daily allowance remains distinct at 40
        assertEquals(40, state.dailyAllowanceReels)
        assertEquals(20, state.batchUnlockedReelsCount)
        assertEquals(20, state.batchDurationMinutes)
    }

    @Test
    fun batchFinished_updateExpiredBatch_reflectsHookCorrectly() = runTest(testDispatcher) {
        val viewModel = BatchFinishedViewModel(
            usageAggregator = usageAggregator,
            initialDailyAllowanceReels = 30
        )
        advanceUntilIdle()

        viewModel.updateExpiredBatch(unlockedReels = 15, durationMinutes = 15)
        assertEquals(15, viewModel.uiState.value.batchUnlockedReelsCount)
        assertEquals(15, viewModel.uiState.value.batchDurationMinutes)
    }

    // =========================================================================
    // Screen 16: Permission Disabled Tests
    // =========================================================================

    @Test
    fun permissionDisabled_evaluatesPermissionsIndependently() = runTest(testDispatcher) {
        // Case 1: Both granted -> Protection NOT paused
        fakeAccessibilityChecker.granted = true
        fakeUsageStatsSource.hasPermission = true
        val viewModel = PermissionDisabledViewModel(fakeAccessibilityChecker, fakeUsageStatsSource)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertTrue(state.isAccessibilityGranted)
        assertTrue(state.isUsageAccessGranted)
        assertFalse("Protection is active when both granted", state.isProtectionPaused)

        // Case 2: Accessibility missing -> Protection paused
        fakeAccessibilityChecker.granted = false
        viewModel.refreshPermissionState()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertFalse(state.isAccessibilityGranted)
        assertTrue(state.isUsageAccessGranted)
        assertTrue("Protection paused when accessibility missing", state.isProtectionPaused)

        // Case 3: Usage Access missing -> Protection paused
        fakeAccessibilityChecker.granted = true
        fakeUsageStatsSource.hasPermission = false
        viewModel.refreshPermissionState()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue(state.isAccessibilityGranted)
        assertFalse(state.isUsageAccessGranted)
        assertTrue("Protection paused when usage access missing", state.isProtectionPaused)
    }

    // =========================================================================
    // Screen 17: No Internet / Offline Tests
    // =========================================================================

    @Test
    fun offline_localProtectionStaysActiveWhenDeviceIsOffline() = runTest(testDispatcher) {
        // Device is offline
        fakeNetworkObserver.setOnline(false)
        // But local permissions are intact
        fakeAccessibilityChecker.granted = true
        fakeUsageStatsSource.hasPermission = true

        val viewModel = OfflineViewModel(
            networkConnectivityObserver = fakeNetworkObserver,
            accessibilityPermissionChecker = fakeAccessibilityChecker,
            usageStatsDataSource = fakeUsageStatsSource,
            usageAggregator = usageAggregator
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Device is reported offline", state.isOnline)
        assertTrue("Local protection remains verified and intact on-device", state.isLocallyProtected)
    }

    @Test
    fun offline_doesNotClaimProtectionIfLocalPermissionsAreMissing() = runTest(testDispatcher) {
        fakeNetworkObserver.setOnline(false)
        fakeAccessibilityChecker.granted = false // Permission missing

        val viewModel = OfflineViewModel(
            networkConnectivityObserver = fakeNetworkObserver,
            accessibilityPermissionChecker = fakeAccessibilityChecker,
            usageStatsDataSource = fakeUsageStatsSource,
            usageAggregator = usageAggregator
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Device is offline", state.isOnline)
        assertFalse("Cannot claim protection is active when local accessibility is missing", state.isLocallyProtected)
    }

    @Test
    fun offline_retryConnectionEvaluatesActualConnectivity() = runTest(testDispatcher) {
        fakeNetworkObserver.setOnline(false)
        val viewModel = OfflineViewModel(
            networkConnectivityObserver = fakeNetworkObserver,
            accessibilityPermissionChecker = fakeAccessibilityChecker,
            usageStatsDataSource = fakeUsageStatsSource,
            usageAggregator = usageAggregator
        )
        advanceUntilIdle()

        viewModel.retryConnection()
        advanceUntilIdle()
        assertEquals("Still offline · Protection continues on-device", viewModel.uiState.value.feedbackMessage)

        // Network comes back online
        fakeNetworkObserver.setOnline(true)
        viewModel.retryConnection()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isOnline)
        assertEquals("Connected to network", viewModel.uiState.value.feedbackMessage)
    }

    // =========================================================================
    // Screen 18: First Day / Empty State Tests
    // =========================================================================

    @Test
    fun firstDay_whenNoUsageExists_flagsFirstDayCorrectly() = runTest(testDispatcher) {
        val viewModel = FirstDayViewModel(
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFirstDay)
        assertEquals(0, state.todayMonitoredMinutes)
        assertEquals(0, state.todayReelsWatched)
    }

    @Test
    fun firstDay_whenUsageAccumulates_transitionsGracefully() = runTest(testDispatcher) {
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "ev_today",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = com.stillfeed.app.domain.model.SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = System.currentTimeMillis(),
                durationMillis = 25 * 60000L
            )
        )

        val viewModel = FirstDayViewModel(
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isFirstDay)
        assertEquals(25, state.todayMonitoredMinutes)
        assertFalse(state.isReelsCountAvailable)
        assertNull(state.todayReelsWatched)
    }
}

// -----------------------------------------------------------------------------
// Test Doubles
// -----------------------------------------------------------------------------

private class FakeAccessibilityPermissionChecker(var granted: Boolean = true) : AccessibilityPermissionChecker {
    override fun isAccessibilityPermissionGranted(): Boolean = granted
}

private class FakeUsageStatsDataSource(var hasPermission: Boolean = true) : UsageStatsDataSource {
    override fun hasUsageStatsPermission(): Boolean = hasPermission
    override fun queryTimelineEvents(startTimeMillis: Long, endTimeMillis: Long): List<RawUsageTimelineEvent> = emptyList()
}

private class FakeNetworkConnectivityObserver(private var isOnline: Boolean = true) : NetworkConnectivityObserver {
    private val flow = MutableStateFlow(isOnline)
    override fun observeIsOnline(): Flow<Boolean> = flow.asStateFlow()
    override fun isOnline(): Boolean = isOnline
    fun setOnline(online: Boolean) {
        isOnline = online
        flow.value = online
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
    override suspend fun deleteEventsOlderThan(cutoffMillis: Long): Int {
        val before = events.size
        events.removeAll { it.timestampMillis < cutoffMillis }
        return before - events.size
    }
    override suspend fun clearUsageHistory() { events.clear() }
}
