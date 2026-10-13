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
    fun firstDay_whenNewUserWithNoHistory_detectsGenuineFirstDay() = runTest(testDispatcher) {
        fakeUsageStatsSource.hasPermission = true

        val viewModel = FirstDayViewModel(
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource,
            usageRepository = fakeUsageRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Must detect genuine first day when no prior history exists", state.isFirstDay)
        assertFalse("Must not have historical usage", state.hasHistoricalUsage)
        assertTrue(state.isUsageAccessGranted)
        assertTrue(state.isUsageDataAvailable)
        assertEquals(0, state.todayMonitoredMinutes)
        assertNull("Reels count must remain null when single-reel tracking is unintercepted", state.todayReelsWatched)
        assertEquals("Start with noticing.", state.headline)
        assertEquals("There’s no baseline yet. STILLFEED will learn your rhythm without judging it.", state.body)
        assertEquals("Your mind looks fresh.", state.mindStateLabel)
        assertEquals("Your first week becomes your reference point.", state.referenceNote)
    }

    @Test
    fun firstDay_whenReturningUserWithHistoryAndZeroMinutesToday_displaysNoUsageRecordedToday() = runTest(testDispatcher) {
        fakeUsageStatsSource.hasPermission = true

        val nowMillis = 1700000000000L // 2023-11-14T22:13:20Z
        val yesterdayMillis = nowMillis - 86400000L // 2023-11-13T22:13:20Z

        // Record historical event before today
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "ev_yesterday",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = com.stillfeed.app.domain.model.SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = yesterdayMillis,
                durationMillis = 30 * 60000L
            )
        )

        val viewModel = FirstDayViewModel(
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource,
            usageRepository = fakeUsageRepository
        )
        viewModel.loadFirstDayState(nowMillis)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Must NOT label returning user as first-day merely because today has 0 monitored time", state.isFirstDay)
        assertTrue("Must detect historical usage from prior days", state.hasHistoricalUsage)
        assertTrue(state.isUsageAccessGranted)
        assertTrue(state.isUsageDataAvailable)
        assertEquals(0, state.todayMonitoredMinutes)
        assertNull("Reels count must remain null when tracking unintercepted", state.todayReelsWatched)
        assertEquals("No usage recorded today.", state.headline)
        assertEquals("You haven’t spent monitored time in Instagram or YouTube yet today.", state.body)
        assertEquals("Quiet", state.mindStateLabel)
        assertEquals("Your ongoing rhythm continues on-device.", state.referenceNote)
    }

    @Test
    fun firstDay_whenUsagePermissionMissing_marksUsageUnavailableAndNullReelCount() = runTest(testDispatcher) {
        fakeUsageStatsSource.hasPermission = false

        val viewModel = FirstDayViewModel(
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource,
            usageRepository = fakeUsageRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Cannot claim first day when usage permission is missing", state.isFirstDay)
        assertFalse(state.isUsageAccessGranted)
        assertFalse(state.isUsageDataAvailable)
        assertNull("Monitored minutes must be null when permission is missing", state.todayMonitoredMinutes)
        assertNull("Reel count must not be assumed zero when data is unavailable", state.todayReelsWatched)
        assertEquals("Permission needed to monitor.", state.headline)
        assertEquals("STILLFEED needs Usage Access permission to measure attention across monitored apps.", state.body)
    }

    @Test
    fun firstDay_whenUsageAggregatorUnavailable_marksUsageUnavailableAndNullReelCount() = runTest(testDispatcher) {
        fakeUsageStatsSource.hasPermission = true

        val viewModel = FirstDayViewModel(
            usageAggregator = null,
            usageStatsDataSource = fakeUsageStatsSource,
            usageRepository = null
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Cannot claim first day when usage data layer is unavailable", state.isFirstDay)
        assertTrue(state.isUsageAccessGranted)
        assertFalse(state.isUsageDataAvailable)
        assertNull("Monitored minutes must be null when aggregation unavailable", state.todayMonitoredMinutes)
        assertNull("Reel count must remain null", state.todayReelsWatched)
        assertEquals("Usage data unavailable.", state.headline)
        assertEquals("Unable to refresh usage metrics from device storage right now.", state.body)
    }

    @Test
    fun firstDay_whenReturningUserWithUsageAccumulatedToday_displaysRhythm() = runTest(testDispatcher) {
        val nowMillis = 1700000000000L

        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "ev_today",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = com.stillfeed.app.domain.model.SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = nowMillis,
                durationMillis = 25 * 60000L
            )
        )

        val viewModel = FirstDayViewModel(
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource,
            usageRepository = fakeUsageRepository
        )
        viewModel.loadFirstDayState(nowMillis)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isFirstDay)
        assertTrue(state.isUsageDataAvailable)
        assertEquals(25, state.todayMonitoredMinutes)
        assertFalse(state.isReelsCountAvailable)
        assertNull(state.todayReelsWatched)
        assertEquals("Today's rhythm.", state.headline)
        assertEquals("Your attention rhythm for today across monitored apps.", state.body)
        assertEquals("Calm", state.mindStateLabel)
    }

    @Test
    fun firstDay_neverConvertsMonitoredMinutesIntoReelsWatched() = runTest(testDispatcher) {
        val viewModel = FirstDayViewModel(
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource,
            usageRepository = fakeUsageRepository
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
