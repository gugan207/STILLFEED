package com.stillfeed.app.feature.settings

import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.auth.model.AuthUser
import com.stillfeed.app.auth.repository.AuthRepository
import com.stillfeed.app.awareness.AwarenessManager
import com.stillfeed.app.domain.model.AppPreferences
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import com.stillfeed.app.domain.model.ProtectionState
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.domain.model.User
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.domain.repository.AwarenessRepository
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.domain.repository.UnlockHistoryRepository
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.domain.repository.UserRepository
import com.stillfeed.app.feature.settings.export.DataExportService
import com.stillfeed.app.protection.permission.AccessibilityPermissionChecker
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.usage.analysis.UsageSessionAnalyzer
import com.stillfeed.app.usage.stats.RawUsageTimelineEvent
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeProtectionRepository: FakeProtectionStateRepository
    private lateinit var fakePreferencesRepository: FakeAppPreferencesRepository
    private lateinit var fakeAwarenessRepository: FakeAwarenessRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeUsageRepository: FakeUsageRepository
    private lateinit var fakeUnlockRepository: FakeUnlockHistoryRepository
    private lateinit var fakeUserRepository: FakeUserRepository

    private lateinit var fakeAccessibilityChecker: FakeAccessibilityPermissionChecker
    private lateinit var fakeUsageStatsSource: FakeUsageStatsDataSource

    private lateinit var usageAggregator: UsageAggregator
    private lateinit var awarenessManager: AwarenessManager
    private lateinit var dataExportService: DataExportService

    private lateinit var viewModel: SettingsViewModel

    private val fixedNowMillis = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeProtectionRepository = FakeProtectionStateRepository(initialEnabled = true)
        fakePreferencesRepository = FakeAppPreferencesRepository()
        fakeAwarenessRepository = FakeAwarenessRepository()
        fakeAuthRepository = FakeAuthRepository(
            currentUser = AuthUser(
                id = "usr_123",
                email = "calm@stillfeed.app",
                displayName = "Calm User",
                createdAtMillis = 1000L,
                deviceBindingId = "dev_123"
            )
        )
        fakeUsageRepository = FakeUsageRepository()
        fakeUnlockRepository = FakeUnlockHistoryRepository()
        fakeUserRepository = FakeUserRepository()

        fakeAccessibilityChecker = FakeAccessibilityPermissionChecker(granted = true)
        fakeUsageStatsSource = FakeUsageStatsDataSource(hasPermission = true)

        usageAggregator = UsageAggregator(fakeUsageRepository)
        awarenessManager = AwarenessManager(
            awarenessRepository = fakeAwarenessRepository,
            usageAggregator = usageAggregator,
            usageStatsDataSource = fakeUsageStatsSource
        )

        dataExportService = DataExportService(
            authRepository = fakeAuthRepository,
            usageRepository = fakeUsageRepository,
            awarenessRepository = fakeAwarenessRepository,
            preferencesRepository = fakePreferencesRepository
        )

        viewModel = createViewModel()
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            protectionStateRepository = fakeProtectionRepository,
            appPreferencesRepository = fakePreferencesRepository,
            awarenessManager = awarenessManager,
            awarenessRepository = fakeAwarenessRepository,
            authRepository = fakeAuthRepository,
            usageRepository = fakeUsageRepository,
            unlockHistoryRepository = fakeUnlockRepository,
            userRepository = fakeUserRepository,
            accessibilityPermissionChecker = fakeAccessibilityChecker,
            usageStatsDataSource = fakeUsageStatsSource,
            dataExportService = dataExportService
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadSettings_initializesStateFromRepositories() = runTest(testDispatcher) {
        viewModel.loadSettings(fixedNowMillis)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.protectionEnabled)
        assertTrue(state.isAccessibilityPermissionGranted)
        assertTrue(state.isUsageStatsPermissionGranted)
        assertEquals("calm@stillfeed.app", state.currentUser?.email)
        assertEquals("SYSTEM", state.preferredThemeMode)
        assertFalse(state.isProtectionPausedDueToPermissions)
    }

    @Test
    fun protectionToggle_enablesAndDisablesImmediately_andPersists() = runTest(testDispatcher) {
        viewModel.loadSettings(fixedNowMillis)
        advanceUntilIdle()

        // Disable protection
        viewModel.setProtectionEnabled(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.protectionEnabled)
        assertFalse(fakeProtectionRepository.getProtectionState().isEnabled)

        // Enable protection
        viewModel.setProtectionEnabled(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.protectionEnabled)
        assertTrue(fakeProtectionRepository.getProtectionState().isEnabled)
    }

    @Test
    fun permissionStatus_detectsDisabledPermission_andRefreshesOnReturn() = runTest(testDispatcher) {
        fakeAccessibilityChecker.granted = false
        fakeUsageStatsSource.hasPermission = false

        viewModel.loadSettings(fixedNowMillis)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertFalse(state.isAccessibilityPermissionGranted)
        assertFalse(state.isUsageStatsPermissionGranted)
        assertTrue("Edge state must be flagged when protection is enabled but permissions missing", state.isProtectionPausedDueToPermissions)

        // User returns after enabling in system settings
        fakeAccessibilityChecker.granted = true
        fakeUsageStatsSource.hasPermission = true

        viewModel.refreshPermissions()
        state = viewModel.uiState.value
        assertTrue(state.isAccessibilityPermissionGranted)
        assertTrue(state.isUsageStatsPermissionGranted)
        assertFalse(state.isProtectionPausedDueToPermissions)
    }

    @Test
    fun awarenessSettings_reflectsSelectedMonthlyTargetAndLockState() = runTest(testDispatcher) {
        val currentMonth = awarenessManager.getCurrentMonthIdentifier(fixedNowMillis)
        fakeAwarenessRepository.saveMonthlyState(
            AwarenessMonthlyState(
                monthIdentifier = currentMonth,
                targetReductionPercentage = 15,
                baselineMinutes = 600,
                currentProgressMinutes = 200,
                targetUsageMinutes = 510,
                reductionAmountMinutes = 90,
                status = MonthlyAwarenessStatus.REDUCING
            )
        )

        viewModel.loadSettings(fixedNowMillis)
        advanceUntilIdle()

        val awareness = viewModel.uiState.value.monthlyAwarenessState
        assertNotNull(awareness)
        assertEquals(15, awareness?.targetReductionPercentage)
        assertEquals(MonthlyAwarenessStatus.REDUCING, awareness?.status)
    }

    @Test
    fun themeSelection_persistsThemeModeInPreferences() = runTest(testDispatcher) {
        viewModel.setThemeMode("LIGHT")
        advanceUntilIdle()

        assertEquals("LIGHT", viewModel.uiState.value.preferredThemeMode)
        assertEquals("LIGHT", fakePreferencesRepository.getPreferences().preferredThemeMode)

        viewModel.setThemeMode("DARK")
        advanceUntilIdle()

        assertEquals("DARK", viewModel.uiState.value.preferredThemeMode)
        assertEquals("DARK", fakePreferencesRepository.getPreferences().preferredThemeMode)
    }

    @Test
    fun dataExport_generatesSanitizedJson_excludingPasswordsAndSensitiveKeys() = runTest(testDispatcher) {
        // Seed usage event
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "ev_export",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = fixedNowMillis,
                durationMillis = 30 * 60 * 1000L
            )
        )

        viewModel.exportData()
        advanceUntilIdle()

        val jsonString = viewModel.uiState.value.exportJsonResult
        assertNotNull("Export JSON should be produced", jsonString)

        val root = JSONObject(jsonString!!)
        assertEquals("STILLFEED", root.getString("appName"))
        assertEquals(1, root.getInt("exportSchemaVersion"))

        val userObj = root.getJSONObject("userProfile")
        assertEquals("usr_123", userObj.getString("id"))
        assertEquals("calm@stillfeed.app", userObj.getString("email"))

        // Strictly verify sensitive exclusions
        assertFalse("Passwords must never be exported", root.has("password"))
        assertFalse("Password hashes must never be exported", root.has("passwordHash"))
        assertFalse("Tokens must never be exported", root.has("sessionToken"))
        assertFalse("Bearer tokens must never be exported", root.has("token"))
        assertFalse("Raw node text must never be exported", root.has("accessibilityText"))

        val usageArray = root.getJSONArray("usageEvents")
        assertEquals(1, usageArray.length())
        assertEquals("ev_export", usageArray.getJSONObject(0).getString("id"))
    }

    @Test
    fun signOut_terminatesSession_andSetsSignedOutFlag() = runTest(testDispatcher) {
        viewModel.loadSettings(fixedNowMillis)
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.signedOut)
        assertNull(viewModel.uiState.value.currentUser)
        assertEquals(AuthState.SignedOut, fakeAuthRepository.authState.value)
    }

    @Test
    fun deleteAccountAndData_purgesAllLocalRepositories_andSignsOut() = runTest(testDispatcher) {
        // Seed data in usage, unlock, awareness, preferences
        fakeUsageRepository.recordEvent(
            UsageEvent(
                id = "u1",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = fixedNowMillis
            )
        )
        fakeUnlockRepository.recordUnlock(UnlockEvent("un1", fixedNowMillis, 15, "break"))
        fakeAwarenessRepository.saveMonthlyState(
            AwarenessMonthlyState(monthIdentifier = "2026-10", targetReductionPercentage = 10)
        )

        viewModel.deleteAccountAndData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.accountDeleted)
        assertTrue(state.signedOut)
        assertNull(state.currentUser)

        // Verify repository cleanups
        assertEquals(0, fakeUsageRepository.getEventCount())
        assertEquals(0, fakeUnlockRepository.getUnlockCount())
        assertEquals(0, fakeAwarenessRepository.getAllMonthlyStates().size)
        assertFalse(fakeProtectionRepository.getProtectionState().isEnabled)
    }

    @Test
    fun processRecreation_restoresSettingsState() = runTest(testDispatcher) {
        viewModel.setProtectionEnabled(false)
        viewModel.setThemeMode("DARK")
        advanceUntilIdle()

        val recreatedViewModel = createViewModel()
        recreatedViewModel.loadSettings(fixedNowMillis)
        advanceUntilIdle()

        val state = recreatedViewModel.uiState.value
        assertFalse(state.protectionEnabled)
        assertEquals("DARK", state.preferredThemeMode)
    }
}

// -----------------------------------------------------------------------------
// Test Doubles
// -----------------------------------------------------------------------------

private class FakeProtectionStateRepository(initialEnabled: Boolean = true) : ProtectionStateRepository {
    private val stateFlow = MutableStateFlow(ProtectionState(isEnabled = initialEnabled))

    override fun observeProtectionState(): Flow<ProtectionState> = stateFlow.asStateFlow()

    override suspend fun getProtectionState(): ProtectionState = stateFlow.value

    override suspend fun setProtectionEnabled(enabled: Boolean) {
        stateFlow.value = ProtectionState(isEnabled = enabled)
    }

    override suspend fun clearProtectionState() {
        stateFlow.value = ProtectionState(isEnabled = false)
    }
}

private class FakeAppPreferencesRepository : AppPreferencesRepository {
    private var prefs = AppPreferences()
    private val flow = MutableStateFlow(prefs)

    override fun observePreferences(): Flow<AppPreferences> = flow.asStateFlow()

    override suspend fun getPreferences(): AppPreferences = prefs

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        updatePreferences { it.copy(hasCompletedOnboarding = completed) }
    }

    override suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        updatePreferences { it.copy(isFirstLaunch = isFirstLaunch) }
    }

    override suspend fun setQuickProtectionToggle(enabled: Boolean) {
        updatePreferences { it.copy(quickProtectionToggle = enabled) }
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

private class FakeAccessibilityPermissionChecker(var granted: Boolean = true) : AccessibilityPermissionChecker {
    override fun isAccessibilityPermissionGranted(): Boolean = granted
}

private class FakeUsageStatsDataSource(var hasPermission: Boolean = true) : UsageStatsDataSource {
    override fun hasUsageStatsPermission(): Boolean = hasPermission
    override fun queryTimelineEvents(startTimeMillis: Long, endTimeMillis: Long): List<RawUsageTimelineEvent> = emptyList()
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

private class FakeUsageRepository : UsageRepository {
    private val events = mutableListOf<UsageEvent>()

    override suspend fun recordEvent(event: UsageEvent) { events.add(event) }
    override suspend fun recordEvents(events: List<UsageEvent>) { this.events.addAll(events) }
    override suspend fun getEventById(id: String): UsageEvent? = events.find { it.id == id }
    override fun observeEventsInRange(startMillis: Long, endMillis: Long): Flow<List<UsageEvent>> = flowOf(events.filter { it.timestampMillis in startMillis..endMillis })
    override suspend fun getEventsInRange(startMillis: Long, endMillis: Long): List<UsageEvent> {
        return events.filter { it.timestampMillis in startMillis..endMillis }
    }
    override suspend fun getEventCount(): Int = events.size
    override suspend fun deleteEventsOlderThan(cutoffMillis: Long): Int {
        val countBefore = events.size
        events.removeAll { it.timestampMillis < cutoffMillis }
        return countBefore - events.size
    }
    override suspend fun clearUsageHistory() { events.clear() }
}

private class FakeUnlockHistoryRepository : UnlockHistoryRepository {
    private val unlocks = mutableListOf<UnlockEvent>()
    override suspend fun recordUnlock(event: UnlockEvent) { unlocks.add(event) }
    override suspend fun getUnlockById(id: String): UnlockEvent? = unlocks.find { it.id == id }
    override fun observeUnlocksInRange(startMillis: Long, endMillis: Long): Flow<List<UnlockEvent>> = flowOf(unlocks.filter { it.timestampMillis in startMillis..endMillis })
    override suspend fun getUnlocksInRange(startMillis: Long, endMillis: Long): List<UnlockEvent> = unlocks.filter { it.timestampMillis in startMillis..endMillis }
    override suspend fun clearUnlockHistory() { unlocks.clear() }
    fun getUnlockCount(): Int = unlocks.size
}

private class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<String, User>()
    override fun observeUser(userId: String): Flow<User?> = flowOf(users[userId])
    override fun observeCurrentUser(): Flow<User?> = flowOf(users.values.firstOrNull())
    override suspend fun getUser(userId: String): User? = users[userId]
    override suspend fun getCurrentUser(): User? = users.values.firstOrNull()
    override suspend fun saveUser(user: User) { users[user.id] = user }
    override suspend fun updateUser(user: User) { users[user.id] = user }
    override suspend fun deleteUser(userId: String) { users.remove(userId) }
    override suspend fun clearAllUsers() { users.clear() }
}

private class FakeAuthRepository(private var currentUser: AuthUser? = null) : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(
        if (currentUser != null) AuthState.SignedIn(currentUser!!) else AuthState.SignedOut
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun signIn(email: String, password: CharSequence): Result<AuthUser> {
        val user = currentUser ?: AuthUser(id = "usr_test", email = email, displayName = "Test")
        currentUser = user
        _authState.value = AuthState.SignedIn(user)
        return Result.success(user)
    }
    override suspend fun signUp(email: String, password: CharSequence, displayName: String?): Result<AuthUser> {
        val user = AuthUser(id = "usr_test", email = email, displayName = displayName)
        currentUser = user
        _authState.value = AuthState.SignedIn(user)
        return Result.success(user)
    }
    override suspend fun signOut(): Result<Unit> {
        currentUser = null
        _authState.value = AuthState.SignedOut
        return Result.success(Unit)
    }
    override suspend fun refreshSession(): Result<AuthUser?> = Result.success(currentUser)
    override suspend fun getCurrentUser(): AuthUser? = currentUser
    override suspend fun bindDevice(deviceId: String): Result<AuthUser> = Result.success(currentUser!!)
    override suspend fun deleteAccount(): Result<Unit> {
        currentUser = null
        _authState.value = AuthState.SignedOut
        return Result.success(Unit)
    }
}
