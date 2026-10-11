package com.stillfeed.app.protection

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.stillfeed.app.protection.accessibility.AccessibilityEventAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccessibilityEventAdapterTest {

    private lateinit var adapter: AccessibilityEventAdapter

    @Before
    fun setUp() {
        adapter = AccessibilityEventAdapter(maxDepth = 5, maxNodesToInspect = 10)
    }

    @Test
    fun nullEvent_returnsEmptyContext() {
        val context = adapter.extractWindowContext(null, null)
        assertEquals("", context.packageName)
        assertTrue(context.activeViewIds.isEmpty())
        assertTrue(context.structuralTags.isEmpty())
    }

    @Test
    fun eventWithNullRootNode_extractsPackageAndClassSafely() {
        val event = AccessibilityEvent.obtain().apply {
            packageName = "com.instagram.android"
            className = "com.instagram.mainactivity.MainActivity"
        }

        val context = adapter.extractWindowContext(event, null)
        assertEquals("com.instagram.android", context.packageName)
        assertEquals("com.instagram.mainactivity.MainActivity", context.rootClassName)
        assertTrue(context.structuralTags.contains("com.instagram.mainactivity.MainActivity"))
        assertTrue(context.activeViewIds.isEmpty())
    }

    @Test
    fun privacyEnforcement_neverCollectsNodeTextOrPrivateContent() {
        val event = AccessibilityEvent.obtain().apply {
            packageName = "com.instagram.android"
            className = "com.instagram.direct.fragment.DirectThreadFragment"
        }

        val rootNode = AccessibilityNodeInfo.obtain().apply {
            packageName = "com.instagram.android"
            className = "android.widget.FrameLayout"
            viewIdResourceName = "com.instagram.android:id/direct_thread"
            text = "Sensitive Private Chat Message That Must Never Be Collected"
            contentDescription = "Private User Caption"
        }

        val context = adapter.extractWindowContext(event, rootNode)

        // Structural ID must be present
        assertTrue(context.activeViewIds.contains("com.instagram.android:id/direct_thread"))
        // Class names must be present
        assertTrue(context.structuralTags.contains("android.widget.FrameLayout"))

        // Context model MUST NOT contain any private text fields
        // Verify via structural properties
        assertFalse(context.activeViewIds.any { it.contains("Sensitive") })
        assertFalse(context.structuralTags.any { it.contains("Sensitive") })
    }

    @Test
    fun boundedTraversal_respectsMaxNodesLimit() {
        val smallAdapter = AccessibilityEventAdapter(maxDepth = 2, maxNodesToInspect = 2)

        val event = AccessibilityEvent.obtain().apply {
            packageName = "com.instagram.android"
        }

        val root = AccessibilityNodeInfo.obtain().apply {
            viewIdResourceName = "id_root"
        }

        val context = smallAdapter.extractWindowContext(event, root)
        assertTrue(context.activeViewIds.contains("id_root"))
    }
}
