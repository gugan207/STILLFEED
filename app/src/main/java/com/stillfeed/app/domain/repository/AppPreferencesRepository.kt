package com.stillfeed.app.domain.repository

import com.stillfeed.app.domain.model.AppPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing lightweight application preferences.
 */
interface AppPreferencesRepository {
    fun observePreferences(): Flow<AppPreferences>
    suspend fun getPreferences(): AppPreferences
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setFirstLaunch(isFirstLaunch: Boolean)
    suspend fun setQuickProtectionToggle(enabled: Boolean)
    suspend fun updatePreferences(transform: (AppPreferences) -> AppPreferences)
    suspend fun clearPreferences()
}
