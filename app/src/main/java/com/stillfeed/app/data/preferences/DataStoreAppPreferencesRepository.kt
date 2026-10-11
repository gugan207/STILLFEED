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
            preferences[AppPreferencesKeys.QUICK_PROTECTION_TOGGLE] = updated.quickProtectionToggle
            preferences[AppPreferencesKeys.DAILY_REMINDER_ENABLED] = updated.dailyReminderEnabled
            preferences[AppPreferencesKeys.PREFERRED_THEME_MODE] = updated.preferredThemeMode
            preferences[AppPreferencesKeys.LAST_USAGE_SYNC_TIMESTAMP] = updated.lastUsageSyncTimestampMillis
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
            quickProtectionToggle = preferences[AppPreferencesKeys.QUICK_PROTECTION_TOGGLE] ?: false,
            dailyReminderEnabled = preferences[AppPreferencesKeys.DAILY_REMINDER_ENABLED] ?: true,
            preferredThemeMode = preferences[AppPreferencesKeys.PREFERRED_THEME_MODE] ?: "SYSTEM",
            lastUsageSyncTimestampMillis = preferences[AppPreferencesKeys.LAST_USAGE_SYNC_TIMESTAMP] ?: 0L
        )
    }
}
