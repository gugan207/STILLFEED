package com.stillfeed.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stillfeed.app.auth.model.AuthUser
import com.stillfeed.app.auth.repository.AuthRepository
import com.stillfeed.app.awareness.AwarenessManager
import com.stillfeed.app.domain.model.AppPreferences
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import com.stillfeed.app.domain.model.ProtectionState
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.domain.repository.AwarenessRepository
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.domain.repository.UnlockHistoryRepository
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.domain.repository.UserRepository
import com.stillfeed.app.feature.settings.export.DataExportService
import com.stillfeed.app.protection.permission.AccessibilityPermissionChecker
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the STILLFEED Settings screen (Figma Node 70:149 & 70:221).
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val protectionEnabled: Boolean = false,
    val storiesAndDmsAvailable: Boolean = true, // Always true per STILLFEED product rules
    val isAccessibilityPermissionGranted: Boolean = false,
    val isUsageStatsPermissionGranted: Boolean = false,
    val monthlyAwarenessState: AwarenessMonthlyState? = null,
    val preferredThemeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val currentUser: AuthUser? = null,
    val isExporting: Boolean = false,
    val exportJsonResult: String? = null,
    val isDeletingAccount: Boolean = false,
    val accountDeleted: Boolean = false,
    val signedOut: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Determines whether protection is currently paused due to missing Android system permissions.
     * Corresponds to Figma Screen 16 (Node 70:221 "Your protection is paused.").
     */
    val isProtectionPausedDueToPermissions: Boolean
        get() = protectionEnabled && (!isAccessibilityPermissionGranted || !isUsageStatsPermissionGranted)
}

/**
 * Settings ViewModel orchestrating protection toggling, permission refresh,
 * theme selection, awareness configuration, data export, and account lifecycle.
 */
class SettingsViewModel(
    private val protectionStateRepository: ProtectionStateRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val awarenessManager: AwarenessManager,
    private val awarenessRepository: AwarenessRepository,
    private val authRepository: AuthRepository,
    private val usageRepository: UsageRepository,
    private val unlockHistoryRepository: UnlockHistoryRepository,
    private val userRepository: UserRepository,
    private val accessibilityPermissionChecker: AccessibilityPermissionChecker?,
    private val usageStatsDataSource: UsageStatsDataSource?,
    private val dataExportService: DataExportService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeStreams()
    }

    private fun observeStreams() {
        viewModelScope.launch {
            protectionStateRepository.observeProtectionState().collect { state ->
                _uiState.update { it.copy(protectionEnabled = state.isEnabled) }
            }
        }

        viewModelScope.launch {
            appPreferencesRepository.observePreferences().collect { prefs ->
                _uiState.update { it.copy(preferredThemeMode = prefs.preferredThemeMode) }
            }
        }
    }

    /**
     * Refreshes all settings data and permission states.
     */
    fun loadSettings(nowMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            try {
                val protectionState = protectionStateRepository.getProtectionState()
                val prefs = appPreferencesRepository.getPreferences()
                val user = authRepository.getCurrentUser()

                val hasAccessibility = accessibilityPermissionChecker?.isAccessibilityPermissionGranted() ?: false
                val hasUsageStats = usageStatsDataSource?.hasUsageStatsPermission() ?: false

                val monthId = awarenessManager.getCurrentMonthIdentifier(nowMillis)
                val awarenessState = awarenessManager.getOrCreateMonthlyState(monthId, nowMillis)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        protectionEnabled = protectionState.isEnabled,
                        isAccessibilityPermissionGranted = hasAccessibility,
                        isUsageStatsPermissionGranted = hasUsageStats,
                        monthlyAwarenessState = awarenessState,
                        preferredThemeMode = prefs.preferredThemeMode,
                        currentUser = user,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    /**
     * Re-checks actual Android permission states when the user returns from system settings.
     */
    fun refreshPermissions() {
        val hasAccessibility = accessibilityPermissionChecker?.isAccessibilityPermissionGranted() ?: false
        val hasUsageStats = usageStatsDataSource?.hasUsageStatsPermission() ?: false
        _uiState.update {
            it.copy(
                isAccessibilityPermissionGranted = hasAccessibility,
                isUsageStatsPermissionGranted = hasUsageStats
            )
        }
    }

    /**
     * Enables or disables protection immediately.
     */
    fun setProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            protectionStateRepository.setProtectionEnabled(enabled)
            _uiState.update { it.copy(protectionEnabled = enabled) }
        }
    }

    /**
     * Updates and persists the theme preference ("SYSTEM", "LIGHT", "DARK").
     */
    fun setThemeMode(themeMode: String) {
        viewModelScope.launch {
            val sanitized = when (themeMode.uppercase()) {
                "LIGHT" -> "LIGHT"
                "DARK" -> "DARK"
                else -> "SYSTEM"
            }
            appPreferencesRepository.updatePreferences { it.copy(preferredThemeMode = sanitized) }
            _uiState.update { it.copy(preferredThemeMode = sanitized) }
        }
    }

    /**
     * Executes deterministic, sanitized data export.
     */
    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportJsonResult = null, errorMessage = null) }
            try {
                val json = dataExportService.generateExportJson()
                _uiState.update { it.copy(isExporting = false, exportJsonResult = json) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, errorMessage = "Export failed: ${e.message}") }
            }
        }
    }

    /**
     * Clears export result from state.
     */
    fun clearExportResult() {
        _uiState.update { it.copy(exportJsonResult = null) }
    }

    /**
     * Signs out the user, clearing session credentials and navigating back to AuthGate.
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _uiState.update { it.copy(signedOut = true, currentUser = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Sign out failed: ${e.message}") }
            }
        }
    }

    /**
     * Permanently deletes user account, local usage events, awareness states, and preferences.
     * Prevents accidental credential recreation.
     */
    fun deleteAccountAndData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true, errorMessage = null) }
            try {
                // 1. Delete remote/cloud account if available
                val deleteResult = authRepository.deleteAccount()
                if (deleteResult.isFailure) {
                    val ex = deleteResult.exceptionOrNull()
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            errorMessage = "Account deletion failed: ${ex?.message}"
                        )
                    }
                    return@launch
                }

                // 2. Clear all local repositories
                usageRepository.clearUsageHistory()
                unlockHistoryRepository.clearUnlockHistory()
                awarenessRepository.clearAwarenessData()
                protectionStateRepository.clearProtectionState()
                appPreferencesRepository.clearPreferences()
                userRepository.clearAllUsers()

                // 3. Terminate local session
                authRepository.signOut()

                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        accountDeleted = true,
                        signedOut = true,
                        currentUser = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        errorMessage = "Deletion encountered error: ${e.message}"
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            protectionStateRepository: ProtectionStateRepository,
            appPreferencesRepository: AppPreferencesRepository,
            awarenessManager: AwarenessManager,
            awarenessRepository: AwarenessRepository,
            authRepository: AuthRepository,
            usageRepository: UsageRepository,
            unlockHistoryRepository: UnlockHistoryRepository,
            userRepository: UserRepository,
            accessibilityPermissionChecker: AccessibilityPermissionChecker?,
            usageStatsDataSource: UsageStatsDataSource?,
            dataExportService: DataExportService
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    protectionStateRepository = protectionStateRepository,
                    appPreferencesRepository = appPreferencesRepository,
                    awarenessManager = awarenessManager,
                    awarenessRepository = awarenessRepository,
                    authRepository = authRepository,
                    usageRepository = usageRepository,
                    unlockHistoryRepository = unlockHistoryRepository,
                    userRepository = userRepository,
                    accessibilityPermissionChecker = accessibilityPermissionChecker,
                    usageStatsDataSource = usageStatsDataSource,
                    dataExportService = dataExportService
                ) as T
            }
        }
    }
}
