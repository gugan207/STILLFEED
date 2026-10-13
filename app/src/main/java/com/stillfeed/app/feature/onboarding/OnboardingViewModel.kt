package com.stillfeed.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.protection.permission.AccessibilityPermissionChecker
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the 5-screen Onboarding flow (Figma Flow 01, Node 99:220).
 *
 * Screen 01 (0): Welcome
 * Screen 02 (1): What STILLFEED Blocks
 * Screen 03 (2): Permissions
 * Screen 04 (3): Daily Allowance
 * Screen 05 (4): Setup Complete
 */
data class OnboardingUiState(
    val currentStep: Int = 0,
    val isAccessibilityPermissionGranted: Boolean = false,
    val isUsageAccessPermissionGranted: Boolean = false,
    val selectedDailyReelsAllowance: Int = 30,
    val isOnboardingCompleted: Boolean = false,
    val isLoading: Boolean = false
) {
    /**
     * Protection is active if and only if both required Android system permissions are granted.
     * Per Step 17 requirements: Never pretend a permission was granted or claim protection
     * is active when required permissions are missing.
     */
    val isProtectionActive: Boolean
        get() = isAccessibilityPermissionGranted && isUsageAccessPermissionGranted
}

/**
 * ViewModel orchestrating the 5 onboarding stages, system permission detection,
 * daily allowance persistence (default: 30 reels), and transition to Home.
 */
class OnboardingViewModel(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val accessibilityPermissionChecker: AccessibilityPermissionChecker?,
    private val usageStatsDataSource: UsageStatsDataSource?,
    private val protectionStateRepository: ProtectionStateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        refreshPermissions()
        viewModelScope.launch {
            val prefs = appPreferencesRepository.getPreferences()
            _uiState.update { current ->
                current.copy(
                    currentStep = prefs.onboardingStep.coerceIn(0, 4),
                    selectedDailyReelsAllowance = if (prefs.dailyReelsAllowance > 0) prefs.dailyReelsAllowance else 30,
                    isOnboardingCompleted = prefs.hasCompletedOnboarding
                )
            }
        }
    }

    /**
     * Re-checks actual Android system permission states via secure settings and AppOpsManager.
     * Must be called whenever user returns to the app from system settings.
     */
    fun refreshPermissions() {
        val accessibilityGranted = accessibilityPermissionChecker?.isAccessibilityPermissionGranted() ?: false
        val usageAccessGranted = usageStatsDataSource?.hasUsageStatsPermission() ?: false

        _uiState.update { current ->
            current.copy(
                isAccessibilityPermissionGranted = accessibilityGranted,
                isUsageAccessPermissionGranted = usageAccessGranted
            )
        }
    }

    /**
     * Updates the daily Reels allowance (audited default: 30).
     */
    fun selectAllowance(reels: Int) {
        val sanitized = reels.coerceAtLeast(1)
        _uiState.update { it.copy(selectedDailyReelsAllowance = sanitized) }
        viewModelScope.launch {
            appPreferencesRepository.setDailyReelsAllowance(sanitized)
        }
    }

    /**
     * Advances to the next onboarding step.
     */
    fun nextStep() {
        val next = (_uiState.value.currentStep + 1).coerceAtMost(4)
        setStep(next)
    }

    /**
     * Navigates back to the preceding onboarding step.
     */
    fun previousStep() {
        val prev = (_uiState.value.currentStep - 1).coerceAtLeast(0)
        setStep(prev)
    }

    /**
     * Explicitly navigates to a specific onboarding step (0..4).
     */
    fun setStep(step: Int) {
        val targetStep = step.coerceIn(0, 4)
        _uiState.update { it.copy(currentStep = targetStep) }
        viewModelScope.launch {
            appPreferencesRepository.setOnboardingStep(targetStep)
        }
    }

    /**
     * Finalizes the onboarding flow:
     * 1. Persists selected daily allowance to DataStore.
     * 2. Sets hasCompletedOnboarding = true in DataStore.
     * 3. Enables protection state in Room if required permissions are granted.
     * 4. Triggers completion callback to enter Home.
     */
    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val allowance = _uiState.value.selectedDailyReelsAllowance
            appPreferencesRepository.setDailyReelsAllowance(allowance)
            appPreferencesRepository.setOnboardingCompleted(true)

            if (_uiState.value.isProtectionActive) {
                protectionStateRepository.setProtectionEnabled(true)
            }

            _uiState.update {
                it.copy(
                    isOnboardingCompleted = true,
                    isLoading = false
                )
            }
            onSuccess()
        }
    }

    companion object {
        fun provideFactory(
            appPreferencesRepository: AppPreferencesRepository,
            accessibilityPermissionChecker: AccessibilityPermissionChecker?,
            usageStatsDataSource: UsageStatsDataSource?,
            protectionStateRepository: ProtectionStateRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OnboardingViewModel(
                    appPreferencesRepository = appPreferencesRepository,
                    accessibilityPermissionChecker = accessibilityPermissionChecker,
                    usageStatsDataSource = usageStatsDataSource,
                    protectionStateRepository = protectionStateRepository
                ) as T
            }
        }
    }
}
