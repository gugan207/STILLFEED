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
    val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
    val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
    val AUTO_LOCK_TIMEOUT_MINUTES = intPreferencesKey("auto_lock_timeout_minutes")
    val NOTIF_DAILY_SUMMARY = booleanPreferencesKey("notif_daily_summary")
    val NOTIF_WEEKLY_SUMMARY = booleanPreferencesKey("notif_weekly_summary")
    val NOTIF_ACTIVITY_REMINDERS = booleanPreferencesKey("notif_activity_reminders")
    val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    val FLOATING_COMPANION_ENABLED = booleanPreferencesKey("floating_companion_enabled")
    val FLOATING_COMPANION_ANIMATIONS_ENABLED = booleanPreferencesKey("floating_companion_animations_enabled")
    val FLOATING_COMPANION_POS_X = intPreferencesKey("floating_companion_pos_x")
    val FLOATING_COMPANION_POS_Y = intPreferencesKey("floating_companion_pos_y")
}
