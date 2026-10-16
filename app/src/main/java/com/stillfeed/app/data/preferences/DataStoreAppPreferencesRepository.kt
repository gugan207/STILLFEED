package com.stillfeed.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.stillfeed.app.domain.model.AppPreferences
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreAppPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : AppPreferencesRepository {

    override fun observePreferences(): Flow<AppPreferences> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                mapToAppPreferences(preferences)
            }

    override suspend fun getPreferences(): AppPreferences =
        observePreferences().first()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[AppPreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    override suspend fun setOnboardingStep(step: Int) {
        dataStore.edit { preferences ->
            preferences[AppPreferencesKeys.ONBOARDING_STEP] = step
        }
    }

    override suspend fun setDailyReelsAllowance(allowance: Int) {
        dataStore.edit { preferences ->
            preferences[AppPreferencesKeys.DAILY_REELS_ALLOWANCE] = allowance
        }
    }

    override suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        dataStore.edit { preferences ->
            preferences[AppPreferencesKeys.IS_FIRST_LAUNCH] = isFirstLaunch
        }
    }

    override suspend fun setQuickProtectionToggle(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AppPreferencesKeys.QUICK_PROTECTION_TOGGLE] = enabled
        }
    }

    override suspend fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
        dataStore.edit { preferences ->
            val current = mapToAppPreferences(preferences)
            val updated = transform(current)
            preferences[AppPreferencesKeys.IS_FIRST_LAUNCH] = updated.isFirstLaunch
            preferences[AppPreferencesKeys.HAS_COMPLETED_ONBOARDING] = updated.hasCompletedOnboarding
            preferences[AppPreferencesKeys.ONBOARDING_STEP] = updated.onboardingStep
            preferences[AppPreferencesKeys.DAILY_REELS_ALLOWANCE] = updated.dailyReelsAllowance
            preferences[AppPreferencesKeys.QUICK_PROTECTION_TOGGLE] = updated.quickProtectionToggle
            preferences[AppPreferencesKeys.DAILY_REMINDER_ENABLED] = updated.dailyReminderEnabled
            preferences[AppPreferencesKeys.PREFERRED_THEME_MODE] = updated.preferredThemeMode
            preferences[AppPreferencesKeys.LAST_USAGE_SYNC_TIMESTAMP] = updated.lastUsageSyncTimestampMillis
            preferences[AppPreferencesKeys.APP_LOCK_ENABLED] = updated.appLockEnabled
            preferences[AppPreferencesKeys.BIOMETRIC_LOCK_ENABLED] = updated.biometricLockEnabled
            preferences[AppPreferencesKeys.APP_LOCK_PIN] = updated.appLockPin
            preferences[AppPreferencesKeys.AUTO_LOCK_TIMEOUT_MINUTES] = updated.autoLockTimeoutMinutes
            preferences[AppPreferencesKeys.NOTIF_DAILY_SUMMARY] = updated.notifDailySummary
            preferences[AppPreferencesKeys.NOTIF_WEEKLY_SUMMARY] = updated.notifWeeklySummary
            preferences[AppPreferencesKeys.NOTIF_ACTIVITY_REMINDERS] = updated.notifActivityReminders
            preferences[AppPreferencesKeys.QUIET_HOURS_ENABLED] = updated.quietHoursEnabled
        }
    }

    override suspend fun clearPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private fun mapToAppPreferences(preferences: Preferences): AppPreferences {
        return AppPreferences(
            isFirstLaunch = preferences[AppPreferencesKeys.IS_FIRST_LAUNCH] ?: true,
            hasCompletedOnboarding = preferences[AppPreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false,
            onboardingStep = preferences[AppPreferencesKeys.ONBOARDING_STEP] ?: 0,
            dailyReelsAllowance = preferences[AppPreferencesKeys.DAILY_REELS_ALLOWANCE] ?: 30,
            quickProtectionToggle = preferences[AppPreferencesKeys.QUICK_PROTECTION_TOGGLE] ?: false,
            dailyReminderEnabled = preferences[AppPreferencesKeys.DAILY_REMINDER_ENABLED] ?: true,
            preferredThemeMode = preferences[AppPreferencesKeys.PREFERRED_THEME_MODE] ?: "SYSTEM",
            lastUsageSyncTimestampMillis = preferences[AppPreferencesKeys.LAST_USAGE_SYNC_TIMESTAMP] ?: 0L,
            appLockEnabled = preferences[AppPreferencesKeys.APP_LOCK_ENABLED] ?: false,
            biometricLockEnabled = preferences[AppPreferencesKeys.BIOMETRIC_LOCK_ENABLED] ?: true,
            appLockPin = preferences[AppPreferencesKeys.APP_LOCK_PIN] ?: "",
            autoLockTimeoutMinutes = preferences[AppPreferencesKeys.AUTO_LOCK_TIMEOUT_MINUTES] ?: 1,
            notifDailySummary = preferences[AppPreferencesKeys.NOTIF_DAILY_SUMMARY] ?: true,
            notifWeeklySummary = preferences[AppPreferencesKeys.NOTIF_WEEKLY_SUMMARY] ?: true,
            notifActivityReminders = preferences[AppPreferencesKeys.NOTIF_ACTIVITY_REMINDERS] ?: true,
            quietHoursEnabled = preferences[AppPreferencesKeys.QUIET_HOURS_ENABLED] ?: true
        )
    }
}
