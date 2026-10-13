package com.stillfeed.app.feature.onboarding

import com.stillfeed.app.domain.model.AppPreferences
import com.stillfeed.app.domain.model.ProtectionState
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.protection.permission.AccessibilityPermissionChecker
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePrefsRepo: FakeAppPreferencesRepository
    private lateinit var fakeProtectionRepo: FakeProtectionStateRepository
    private lateinit var fakeAccessibilityChecker: FakeAccessibilityPermissionChecker
    private lateinit var fakeUsageStatsDataSource: FakeUsageStatsDataSource

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrefsRepo = FakeAppPreferencesRepository()
        fakeProtectionRepo = FakeProtectionStateRepository()
        fakeAccessibilityChecker = FakeAccessibilityPermissionChecker(granted = false)
        fakeUsageStatsDataSource = FakeUsageStatsDataSource(granted = false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): OnboardingViewModel {
        return OnboardingViewModel(
            appPreferencesRepository = fakePrefsRepo,
            accessibilityPermissionChecker = fakeAccessibilityChecker,
            usageStatsDataSource = fakeUsageStatsDataSource,
            protectionStateRepository = fakeProtectionRepo
        )
    }

    @Test
    fun initialState_hasDefault30ReelsAllowanceAndStep0() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.currentStep)
        assertEquals(30, state.selectedDailyReelsAllowance)
        assertFalse(state.isAccessibilityPermissionGranted)
        assertFalse(state.isUsageAccessPermissionGranted)
        assertFalse(state.isProtectionActive)
        assertFalse(state.isOnboardingCompleted)
    }

    @Test
    fun stepTransitions_advanceAndRegressWithinBounds() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 0 -> 1 -> 2 -> 3 -> 4
        viewModel.nextStep()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.currentStep)
        assertEquals(1, fakePrefsRepo.getPreferences().onboardingStep)

        viewModel.nextStep()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.currentStep)

        // Clamps at 4
        viewModel.nextStep()
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.currentStep)

        // 4 -> 3 -> 2 -> 1 -> 0
        viewModel.previousStep()
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.currentStep)

        // Clamps at 0
        viewModel.previousStep()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.currentStep)
    }

    @Test
    fun selectAllowance_updatesStateAndPersistsToDataStore() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectAllowance(20)
        advanceUntilIdle()

        assertEquals(20, viewModel.uiState.value.selectedDailyReelsAllowance)
        assertEquals(20, fakePrefsRepo.getPreferences().dailyReelsAllowance)

        viewModel.selectAllowance(45)
        advanceUntilIdle()

        assertEquals(45, viewModel.uiState.value.selectedDailyReelsAllowance)
        assertEquals(45, fakePrefsRepo.getPreferences().dailyReelsAllowance)
    }

    @Test
    fun refreshPermissions_detectsSystemGrantsAccurately() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isProtectionActive)

        // Grant only accessibility
        fakeAccessibilityChecker.granted = true
        viewModel.refreshPermissions()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAccessibilityPermissionGranted)
        assertFalse(viewModel.uiState.value.isUsageAccessPermissionGranted)
        assertFalse("Both permissions required for protection active", viewModel.uiState.value.isProtectionActive)

        // Grant both
        fakeUsageStatsDataSource.granted = true
        viewModel.refreshPermissions()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAccessibilityPermissionGranted)
        assertTrue(viewModel.uiState.value.isUsageAccessPermissionGranted)
        assertTrue("Both permissions granted -> protection active", viewModel.uiState.value.isProtectionActive)
    }

    @Test
    fun completeOnboarding_withPermissions_enablesProtectionAndMarksCompleted() = runTest {
        fakeAccessibilityChecker.granted = true
        fakeUsageStatsDataSource.granted = true

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectAllowance(30)
        advanceUntilIdle()

        var callbackInvoked = false
        viewModel.completeOnboarding {
            callbackInvoked = true
        }
        advanceUntilIdle()

        assertTrue("Callback must be invoked", callbackInvoked)
        assertTrue("UI state must reflect completion", viewModel.uiState.value.isOnboardingCompleted)
        assertTrue("Preferences must record completion", fakePrefsRepo.getPreferences().hasCompletedOnboarding)
        assertEquals(30, fakePrefsRepo.getPreferences().dailyReelsAllowance)
        assertTrue("Protection state must be enabled when permissions present", fakeProtectionRepo.getProtectionState().isEnabled)
    }

    @Test
    fun completeOnboarding_withoutPermissions_doesNotEnableProtectionPrematurely() = runTest {
        fakeAccessibilityChecker.granted = false
        fakeUsageStatsDataSource.granted = false

        val viewModel = createViewModel()
        advanceUntilIdle()

        var callbackInvoked = false
        viewModel.completeOnboarding {
            callbackInvoked = true
        }
        advanceUntilIdle()

        assertTrue("Callback must be invoked", callbackInvoked)
        assertTrue("Preferences must record completion", fakePrefsRepo.getPreferences().hasCompletedOnboarding)
        assertFalse("Must NOT claim protection is active when permissions are missing", fakeProtectionRepo.getProtectionState().isEnabled)
    }

    @Test
    fun restoration_resumesSavedOnboardingStepAndAllowance() = runTest {
        fakePrefsRepo.updatePreferences {
            it.copy(onboardingStep = 3, dailyReelsAllowance = 45)
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.currentStep)
        assertEquals(45, viewModel.uiState.value.selectedDailyReelsAllowance)
    }
}

private class FakeAccessibilityPermissionChecker(var granted: Boolean = false) : AccessibilityPermissionChecker {
    override fun isAccessibilityPermissionGranted(): Boolean = granted
}

private class FakeUsageStatsDataSource(var granted: Boolean = false) : UsageStatsDataSource {
    override fun hasUsageStatsPermission(): Boolean = granted
    override fun queryTimelineEvents(startTimeMillis: Long, endTimeMillis: Long): List<RawUsageTimelineEvent> = emptyList()
}

private class FakeProtectionStateRepository : ProtectionStateRepository {
    private val stateFlow = MutableStateFlow(ProtectionState())
    override fun observeProtectionState(): Flow<ProtectionState> = stateFlow.asStateFlow()
    override suspend fun getProtectionState(): ProtectionState = stateFlow.value
    override suspend fun setProtectionEnabled(enabled: Boolean) {
        stateFlow.value = stateFlow.value.copy(isEnabled = enabled)
    }
    override suspend fun clearProtectionState() {
        stateFlow.value = ProtectionState(isEnabled = false)
    }
}

private class FakeAppPreferencesRepository(initialPrefs: AppPreferences = AppPreferences()) : AppPreferencesRepository {
    private var prefs = initialPrefs
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
