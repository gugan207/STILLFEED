package com.stillfeed.app.feature.edgestates

import com.stillfeed.app.domain.model.ProtectionState
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.feature.edgestates.connectivity.NetworkConnectivityObserver
import com.stillfeed.app.feature.edgestates.model.ProtectionRuntimeStatus
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
    private lateinit var fakeProtectionStateRepository: FakeProtectionStateRepository
    private lateinit var usageAggregator: UsageAggregator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAccessibilityChecker = FakeAccessibilityPermissionChecker(granted = true)
        fakeUsageStatsSource = FakeUsageStatsDataSource(hasPermission = true)
        fakeNetworkObserver = FakeNetworkConnectivityObserver(isOnline = true)
        fakeUsageRepository = FakeUsageRepository()
        fakeProtectionStateRepository = FakeProtectionStateRepository(enabled = true)
        usageAggregator = UsageAggregator(fakeUsageRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // Screen 15: Batch Finished Tests (Gated State & Metric Truth)
    // =========================================================================

    @Test
    fun batchFinished_defaultsToNotExpiredBeforeVerifiedEvent() = runTest(testDispatcher) {
        val viewModel = BatchFinishedViewModel(
            usageAggregator = usageAggregator,
            initialDailyAllowanceReels = 30
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Batch must not be marked expired before verified event", state.isBatchExpired)
        assertEquals("No batch has ended.", state.headline)
        assertEquals("A batch completion state appears here once a verified unlock session expires.", state.body)
        assertNull("Watched reels number must be null when tracking unintercepted", state.todayReelsWatched)
    }

    @Test
    fun batchFinished_marksExpiredOnlyAfterVerifiedExpirationEventSupplied() = runTest(testDispatcher) {
        val viewModel = BatchFinishedViewModel(
            usageAggregator = usageAggregator,
            initialDailyAllowanceReels = 30
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isBatchExpired)

        // Verified expiration event from Step 19
        viewModel.onBatchExpired(unlockedReels = 20, durationMinutes = 15)

        val state = viewModel.uiState.value
        assertTrue("Batch must be marked expired after verified event", state.isBatchExpired)
        assertEquals("That batch is done.", state.headline)
        assertEquals("Your unlocked batch is done. Take a reset if you want another small batch.", state.body)
        assertEquals(20, state.batchUnlockedReelsCount)
        assertEquals(15, state.batchDurationMinutes)
    }

    @Test
    fun batchFinished_whenReelCountUnavailable_displaysHonestFallback() = runTest(testDispatcher) {
        val viewModel = BatchFinishedViewModel(
            usageAggregator = usageAggregator,
            initialDailyAllowanceReels = 30,
            initialBatchReels = null,
            initialBatchDurationMinutes = 15,
            initialIsExpired = true
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isBatchExpired)
        assertFalse("Reel counts must not be claimed as verified", state.isReelsCountAvailable)
        assertNull("Watched reels number must be null when tracking unintercepted", state.todayReelsWatched)
        assertEquals("That batch is done.", state.headline)
        assertEquals("Your unlocked batch is done. Take a reset if you want another small batch.", state.body)
        assertEquals(30, state.dailyAllowanceReels)
    }

    @Test
    fun batchFinished_distinguishesBatchExpirationFromDailyAllowance() = runTest(testDispatcher) {
        val viewModel = BatchFinishedViewModel(
            usageAggregator = usageAggregator,
            initialDailyAllowanceReels = 40,
            initialBatchReels = 20,
            initialBatchDurationMinutes = 20,
            initialIsExpired = true
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Daily allowance remains distinct at 40
        assertEquals(40, state.dailyAllowanceReels)
        assertEquals(20, state.batchUnlockedReelsCount)
        assertEquals(20, state.batchDurationMinutes)
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
    // Screen 17: No Internet / Offline Tests (Truthful Runtime Protection)
    // =========================================================================

    @Test
    fun offline_whenPermissionsGrantedAndServiceActive_reportsProtected() = runTest(testDispatcher) {
        fakeNetworkObserver.setOnline(false)
        fakeAccessibilityChecker.granted = true
        fakeUsageStatsSource.hasPermission = true
        fakeProtectionStateRepository.enabled = true

        val viewModel = OfflineViewModel(
            networkConnectivityObserver = fakeNetworkObserver,
            accessibilityPermissionChecker = fakeAccessibilityChecker,
            usageStatsDataSource = fakeUsageStatsSource,
            protectionStateRepository = fakeProtectionStateRepository,
            isServiceRunningProvider = { true },
            usageAggregator = usageAggregator
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isOnline)
        assertEquals(ProtectionRuntimeStatus.PROTECTED, state.protectionStatus)
        assertTrue("Claims protected only when permissions, toggle, and active service verified", state.isLocallyProtected)
        assertEquals("STILL PROTECTED · On-device engine active", state.statusLabel)
    }

    @Test
    fun offline_whenPermissionsGrantedAndServiceUnavailable_reportsStatusUnverified() = runTest(testDispatcher) {
        fakeNetworkObserver.setOnline(false)
        fakeAccessibilityChecker.granted = true
        fakeUsageStatsSource.hasPermission = true
        fakeProtectionStateRepository.enabled = true

        // Runtime service status is unknown/unverified (provider is null)
        val viewModel = OfflineViewModel(
            networkConnectivityObserver = fakeNetworkObserver,
            accessibilityPermissionChecker = fakeAccessibilityChecker,
            usageStatsDataSource = fakeUsageStatsSource,
            protectionStateRepository = fakeProtectionStateRepository,
            isServiceRunningProvider = null,
            usageAggregator = usageAggregator
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isOnline)
        assertEquals(ProtectionRuntimeStatus.PERMISSIONS_ENABLED_STATUS_UNVERIFIED, state.protectionStatus)
        assertFalse("Must never claim STILL PROTECTED when runtime status is unverified", state.isLocallyProtected)
        assertEquals("Required permissions enabled; protection status unverified.", state.statusLabel)
    }

    @Test
    fun offline_whenServiceStopped_reportsProtectionPaused() = runTest(testDispatcher) {
        fakeNetworkObserver.setOnline(false)
        fakeAccessibilityChecker.granted = true
        fakeUsageStatsSource.hasPermission = true
        fakeProtectionStateRepository.enabled = true

        // Service explicitly stopped
        val viewModel = OfflineViewModel(
            networkConnectivityObserver = fakeNetworkObserver,
            accessibilityPermissionChecker = fakeAccessibilityChecker,
            usageStatsDataSource = fakeUsageStatsSource,
            protectionStateRepository = fakeProtectionStateRepository,
            isServiceRunningProvider = { false },
            usageAggregator = usageAggregator
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLocallyProtected)
        assertEquals(ProtectionRuntimeStatus.PROTECTION_DISABLED, state.protectionStatus)
        assertEquals("PROTECTION PAUSED · Accessibility service is stopped", state.statusLabel)
    }

    @Test
    fun offline_whenProtectionDisabled_reportsProtectionPaused() = runTest(testDispatcher) {
        fakeNetworkObserver.setOnline(false)
        fakeAccessibilityChecker.granted = true
        fakeUsageStatsSource.hasPermission = true
        fakeProtectionStateRepository.enabled = false // Protection toggled off by user

        val viewModel = OfflineViewModel(
            networkConnectivityObserver = fakeNetworkObserver,
            accessibilityPermissionChecker = fakeAccessibilityChecker,
            usageStatsDataSource = fakeUsageStatsSource,
            protectionStateRepository = fakeProtectionStateRepository,
            isServiceRunningProvider = { true },
            usageAggregator = usageAggregator
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLocallyProtected)
        assertEquals(ProtectionRuntimeStatus.PROTECTION_DISABLED, state.protectionStatus)
        assertEquals("PROTECTION PAUSED · Protection is turned off", state.statusLabel)
    }

    @Test
    fun offline_whenPermissionsMissing_reportsProtectionPaused() = runTest(testDispatcher) {
        fakeNetworkObserver.setOnline(false)
        fakeAccessibilityChecker.granted = false // Permission missing

        val viewModel = OfflineViewModel(
            networkConnectivityObserver = fakeNetworkObserver,
            accessibilityPermissionChecker = fakeAccessibilityChecker,
            usageStatsDataSource = fakeUsageStatsSource,
            protectionStateRepository = fakeProtectionStateRepository,
            usageAggregator = usageAggregator
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLocallyProtected)
        assertEquals(ProtectionRuntimeStatus.PERMISSIONS_MISSING, state.protectionStatus)
        assertEquals("PROTECTION PAUSED · Required permissions missing", state.statusLabel)
    }

    @Test
    fun offline_retryConnectionEvaluatesActualConnectivity() = runTest(testDispatcher) {
        fakeNetworkObserver.setOnline(false)
        val viewModel = OfflineViewModel(
            networkConnectivityObserver = fakeNetworkObserver,
            accessibilityPermissionChecker = fakeAccessibilityChecker,
            usageStatsDataSource = fakeUsageStatsSource,
            protectionStateRepository = fakeProtectionStateRepository,
            isServiceRunningProvider = { true },
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
    // Screen 18: First Day / Empty State Tests (Metric Truth & Data Gating)
    // =========================================================================

    @Test
    fun firstDay_whenUsagePermissionMissing_marksUsageUnavailableAndNullReelCount() = runTest(testDispatcher) {
        fakeUsageStatsSource.hasPermission = false

        val viewModel = FirstDayViewModel(
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFirstDay)
        assertFalse(state.isUsageAccessGranted)
        assertFalse(state.isUsageDataAvailable)
        assertNull("Monitored minutes must be null when permission is missing", state.todayMonitoredMinutes)
        assertNull("Reel count must not be assumed zero when data is unavailable", state.todayReelsWatched)
    }

    @Test
    fun firstDay_whenUsageAggregatorUnavailable_marksUsageUnavailableAndNullReelCount() = runTest(testDispatcher) {
        fakeUsageStatsSource.hasPermission = true

        val viewModel = FirstDayViewModel(
            usageAggregator = null,
            usageStatsDataSource = fakeUsageStatsSource
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFirstDay)
        assertTrue(state.isUsageAccessGranted)
        assertFalse(state.isUsageDataAvailable)
        assertNull("Monitored minutes must be null when aggregation unavailable", state.todayMonitoredMinutes)
        assertNull("Reel count must remain null", state.todayReelsWatched)
    }

    @Test
    fun firstDay_whenUsageIsVerifiedZero_showsZeroMinutesAndNullReelCount() = runTest(testDispatcher) {
        fakeUsageStatsSource.hasPermission = true

        val viewModel = FirstDayViewModel(
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFirstDay)
        assertTrue(state.isUsageAccessGranted)
        assertTrue(state.isUsageDataAvailable)
        assertEquals(0, state.todayMonitoredMinutes)
        assertNull("Reels count must NOT be converted from zero monitored minutes", state.todayReelsWatched)
    }

    @Test
    fun firstDay_whenUsageAccumulates_transitionsGracefullyWithMonitoredMinutesAndNullReelCount() = runTest(testDispatcher) {
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

    @Test
    fun firstDay_neverConvertsMonitoredMinutesIntoReelsWatched() = runTest(testDispatcher) {
        val viewModel = FirstDayViewModel(
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Even when todayMonitoredMinutes is 0, todayReelsWatched remains strictly null
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

private class FakeProtectionStateRepository(var enabled: Boolean = true) : ProtectionStateRepository {
    override fun observeProtectionState(): Flow<ProtectionState> = flowOf(ProtectionState(enabled))
    override suspend fun getProtectionState(): ProtectionState = ProtectionState(enabled)
    override suspend fun setProtectionEnabled(enabled: Boolean) { this.enabled = enabled }
    override suspend fun clearProtectionState() { enabled = false }
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
