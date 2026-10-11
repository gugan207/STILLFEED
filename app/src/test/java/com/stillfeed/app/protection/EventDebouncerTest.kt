package com.stillfeed.app.protection

import android.view.accessibility.AccessibilityEvent
import com.stillfeed.app.protection.accessibility.EventDebouncer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EventDebouncerTest {

    private lateinit var debouncer: EventDebouncer

    @Before
    fun setUp() {
        debouncer = EventDebouncer(debounceWindowMs = 150L)
    }

    @Test
    fun nullEvent_returnsFalse() {
        assertFalse(debouncer.shouldProcess(null))
    }

    @Test
    fun windowStateChanged_alwaysPassesThroughImmediately() {
        val event1 = AccessibilityEvent.obtain().apply {
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            packageName = "com.instagram.android"
        }
        val event2 = AccessibilityEvent.obtain().apply {
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            packageName = "com.instagram.android"
        }

        assertTrue("First window state change must pass", debouncer.shouldProcess(event1))
        assertTrue("Subsequent window state change must pass immediately without debounce", debouncer.shouldProcess(event2))
    }

    @Test
    fun contentChanged_withinDebounceWindow_isDropped() {
        val event1 = AccessibilityEvent.obtain().apply {
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageName = "com.instagram.android"
        }
        val event2 = AccessibilityEvent.obtain().apply {
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageName = "com.instagram.android"
        }

        assertTrue("First content change should pass", debouncer.shouldProcess(event1))
        assertFalse("Rapid subsequent content change within 150ms should be dropped", debouncer.shouldProcess(event2))
    }

    @Test
    fun differentPackages_doNotThrottleEachOther() {
        val instagramEvent = AccessibilityEvent.obtain().apply {
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageName = "com.instagram.android"
        }
        val youtubeEvent = AccessibilityEvent.obtain().apply {
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageName = "com.google.android.youtube"
        }

        assertTrue(debouncer.shouldProcess(instagramEvent))
        assertTrue("Event from a different package should not be throttled by previous package", debouncer.shouldProcess(youtubeEvent))
    }

    @Test
    fun reset_clearsState() {
        val event1 = AccessibilityEvent.obtain().apply {
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageName = "com.instagram.android"
        }
        assertTrue(debouncer.shouldProcess(event1))

        debouncer.reset()

        val event2 = AccessibilityEvent.obtain().apply {
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageName = "com.instagram.android"
        }
        assertTrue("After reset, next event must be processed immediately", debouncer.shouldProcess(event2))
    }
}
