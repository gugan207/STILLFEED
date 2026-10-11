package com.stillfeed.app.domain.model

/**
 * Domain model representing lightweight application preferences stored in Jetpack DataStore.
 *
 * NOTE: Relational datasets, usage logs, and analytics events are strictly excluded from DataStore
 * and stored in Room.
 */
data class AppPreferences(
    val isFirstLaunch: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
    val onboardingStep: Int = 0,
    val quickProtectionToggle: Boolean = false,
    val dailyReminderEnabled: Boolean = true,
    val preferredThemeMode: String = "SYSTEM",
    val lastUsageSyncTimestampMillis: Long = 0L
)
