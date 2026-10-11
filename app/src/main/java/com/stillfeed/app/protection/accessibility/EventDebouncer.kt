package com.stillfeed.app.protection.accessibility

import android.view.accessibility.AccessibilityEvent

/**
 * Debouncer for filtering noisy high-frequency accessibility events (e.g. rapid scroll
 * and window content updates) while ensuring immediate reaction to window state transitions.
 */
class EventDebouncer(
    private val debounceWindowMs: Long = DEFAULT_DEBOUNCE_WINDOW_MS
) {
    private var lastEventTimestamp: Long = 0L
    private var lastPackageName: String? = null
    private var lastEventType: Int = 0

    /**
     * Determines whether the given [event] should be processed or dropped due to debounce.
     *
     * Window state transitions ([AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED]) are always
     * permitted through immediately to ensure instant protection.
     */
    @Synchronized
    fun shouldProcess(event: AccessibilityEvent?): Boolean {
        if (event == null) return false

        val now = System.currentTimeMillis()
        val eventType = event.eventType
        val packageName = event.packageName?.toString() ?: ""

        // Window state changes represent immediate app/screen navigations — process immediately
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastEventTimestamp = now
            lastPackageName = packageName
            lastEventType = eventType
            return true
        }

        // For window content changes, throttle if arriving too rapidly from the same package
        val elapsed = now - lastEventTimestamp
        if (packageName == lastPackageName && elapsed < debounceWindowMs) {
            return false
        }

        lastEventTimestamp = now
        lastPackageName = packageName
        lastEventType = eventType
        return true
    }

    @Synchronized
    fun reset() {
        lastEventTimestamp = 0L
        lastPackageName = null
        lastEventType = 0
    }

    companion object {
        const val DEFAULT_DEBOUNCE_WINDOW_MS = 150L
    }
}
