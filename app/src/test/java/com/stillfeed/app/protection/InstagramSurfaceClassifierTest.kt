package com.stillfeed.app.protection

import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.protection.detection.InstagramSurfaceClassifier
import com.stillfeed.app.protection.model.AccessibilityWindowContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InstagramSurfaceClassifierTest {

    private lateinit var classifier: InstagramSurfaceClassifier

    @Before
    fun setUp() {
        classifier = InstagramSurfaceClassifier()
    }

    @Test
    fun packageHandling_identifiesInstagramPackageOnly() {
        assertTrue(classifier.canHandle("com.instagram.android"))
        assertFalse(classifier.canHandle("com.google.android.youtube"))
        assertFalse(classifier.canHandle("com.whatsapp"))
    }

    @Test
    fun feedSurface_isAllowed() {
        val context = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/feed_recycler_view", "com.instagram.android:id/action_bar")
        )
        val result = classifier.classify(context)
        assertFalse("Instagram Feed must be allowed", result.isProtectedShortForm)
        assertEquals(SurfaceCategory.FEED, result.surfaceCategory)
    }

    @Test
    fun storySurface_isAllowed() {
        val context = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/reel_viewer_image_view", "com.instagram.android:id/reel_viewer_root")
        )
        val result = classifier.classify(context)
        assertFalse("Instagram Stories must be allowed", result.isProtectedShortForm)
    }

    @Test
    fun dmSurface_isAllowed() {
        val context = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            rootClassName = "com.instagram.direct.fragment.DirectThreadFragment",
            activeViewIds = setOf("com.instagram.android:id/direct_thread", "com.instagram.android:id/message_composer")
        )
        val result = classifier.classify(context)
        assertFalse("Instagram DMs must be allowed", result.isProtectedShortForm)
    }

    @Test
    fun profileAndSettings_isAllowed() {
        val profileContext = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/profile_tab", "com.instagram.android:id/profile_header")
        )
        val profileResult = classifier.classify(profileContext)
        assertFalse("Instagram Profile must be allowed", profileResult.isProtectedShortForm)

        val settingsContext = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/settings_list", "com.instagram.android:id/settings_item")
        )
        val settingsResult = classifier.classify(settingsContext)
        assertFalse("Instagram Settings must be allowed", settingsResult.isProtectedShortForm)
    }

    @Test
    fun standaloneReels_isBlocked() {
        val context = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/clips_viewer_view_pager", "com.instagram.android:id/clips_video_container")
        )
        val result = classifier.classify(context)
        assertTrue("Instagram Reels must be blocked", result.isProtectedShortForm)
        assertEquals(SurfaceCategory.REELS, result.surfaceCategory)
    }

    @Test
    fun reelLaunchedFromDm_isBlocked() {
        // When a user taps a reel link within DMs, Instagram opens the clips viewer over or instead of the thread
        val context = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            rootClassName = "com.instagram.modal.ModalActivity",
            activeViewIds = setOf(
                "com.instagram.android:id/direct_thread",
                "com.instagram.android:id/clips_viewer_view_pager",
                "com.instagram.android:id/clips_video_container"
            )
        )
        val result = classifier.classify(context)
        assertTrue("Reel opened from DM must still be blocked", result.isProtectedShortForm)
        assertEquals(SurfaceCategory.REELS, result.surfaceCategory)
    }

    @Test
    fun unknownNonInstagramPackage_isAllowed() {
        val context = AccessibilityWindowContext(
            packageName = "com.example.calculator"
        )
        val result = classifier.classify(context)
        assertFalse("Unknown packages must be allowed", result.isProtectedShortForm)
    }
}
