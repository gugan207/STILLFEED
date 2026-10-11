package com.stillfeed.app.feature.settings.export

import com.stillfeed.app.auth.model.AuthUser
import com.stillfeed.app.auth.repository.AuthRepository
import com.stillfeed.app.domain.model.AppPreferences
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.domain.repository.AwarenessRepository
import com.stillfeed.app.domain.repository.UsageRepository
import org.json.JSONArray
import org.json.JSONObject

/**
 * Deterministic Data Export Service for STILLFEED (STEP 11 requirement 6).
 *
 * PRIVACY GUARANTEES:
 * 1. Strictly exports approved metadata, usage logs, awareness states, and preferences.
 * 2. EXCLUDES passwords, hashes, salts, and session tokens.
 * 3. EXCLUDES raw accessibility node trees, window contents, messages, and captions.
 * 4. EXCLUDES screenshots, video frames, search queries, or audio.
 * 5. Uses deterministic, structured JSON serialization.
 */
class DataExportService(
    private val authRepository: AuthRepository,
    private val usageRepository: UsageRepository,
    private val awarenessRepository: AwarenessRepository,
    private val preferencesRepository: AppPreferencesRepository
) {

    /**
     * Generates a sanitized, deterministic JSON export payload.
     */
    suspend fun generateExportJson(currentTimeMillis: Long = System.currentTimeMillis()): String {
        val root = JSONObject()

        // 1. Export Metadata
        root.put("appName", "STILLFEED")
        root.put("exportSchemaVersion", 1)
        root.put("exportTimestampMillis", currentTimeMillis)

        // 2. Sanitized User Profile (No passwords, no tokens)
        val currentUser = authRepository.getCurrentUser()
        val userObj = JSONObject()
        if (currentUser != null) {
            userObj.put("id", currentUser.id)
            userObj.put("email", currentUser.email ?: "")
            userObj.put("displayName", currentUser.displayName ?: "")
            userObj.put("createdAtMillis", currentUser.createdAtMillis)
            userObj.put("isDeviceBound", currentUser.isDeviceBound)
        }
        root.put("userProfile", userObj)

        // 3. App Preferences (Excluding internal session markers)
        val prefs = preferencesRepository.getPreferences()
        val prefsObj = JSONObject()
        prefsObj.put("preferredThemeMode", prefs.preferredThemeMode)
        prefsObj.put("dailyReminderEnabled", prefs.dailyReminderEnabled)
        prefsObj.put("quickProtectionToggle", prefs.quickProtectionToggle)
        prefsObj.put("hasCompletedOnboarding", prefs.hasCompletedOnboarding)
        root.put("appPreferences", prefsObj)

        // 4. Awareness Monthly States
        val awarenessStates = awarenessRepository.getAllMonthlyStates()
        val awarenessArray = JSONArray()
        for (state in awarenessStates) {
            val sObj = JSONObject()
            sObj.put("monthIdentifier", state.monthIdentifier)
            sObj.put("targetReductionPercentage", state.targetReductionPercentage)
            sObj.put("baselineMinutes", state.baselineMinutes)
            sObj.put("currentProgressMinutes", state.currentProgressMinutes)
            sObj.put("targetUsageMinutes", state.targetUsageMinutes)
            sObj.put("reductionAmountMinutes", state.reductionAmountMinutes)
            sObj.put("status", state.status.name)
            awarenessArray.put(sObj)
        }
        root.put("awarenessMonthlyHistory", awarenessArray)

        // 5. Monitored Usage Events
        val usageEvents = usageRepository.getEventsInRange(0L, Long.MAX_VALUE)
        val usageArray = JSONArray()
        for (event in usageEvents) {
            val eObj = JSONObject()
            eObj.put("id", event.id)
            eObj.put("timestampMillis", event.timestampMillis)
            eObj.put("packageName", event.packageName)
            eObj.put("surfaceCategory", event.surfaceCategory.name)
            eObj.put("eventType", event.eventType.name)
            eObj.put("durationMillis", event.durationMillis ?: 0L)
            eObj.put("isBlocked", event.isBlocked)
            usageArray.put(eObj)
        }
        root.put("usageEvents", usageArray)

        return root.toString(2)
    }
}
