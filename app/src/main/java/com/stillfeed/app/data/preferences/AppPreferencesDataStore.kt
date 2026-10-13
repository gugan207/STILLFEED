package com.stillfeed.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "stillfeed_preferences")

object AppPreferencesKeys {
    val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    val ONBOARDING_STEP = intPreferencesKey("onboarding_step")
    val DAILY_REELS_ALLOWANCE = intPreferencesKey("daily_reels_allowance")
    val QUICK_PROTECTION_TOGGLE = booleanPreferencesKey("quick_protection_toggle")
    val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
    val PREFERRED_THEME_MODE = stringPreferencesKey("preferred_theme_mode")
    val LAST_USAGE_SYNC_TIMESTAMP = longPreferencesKey("last_usage_sync_timestamp")
}
