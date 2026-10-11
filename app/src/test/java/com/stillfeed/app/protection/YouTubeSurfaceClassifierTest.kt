package com.stillfeed.app.protection

import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.protection.detection.YouTubeSurfaceClassifier
import com.stillfeed.app.protection.model.AccessibilityWindowContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class YouTubeSurfaceClassifierTest {

    private lateinit var classifier: YouTubeSurfaceClassifier

    @Before
    fun setUp() {
        classifier = YouTubeSurfaceClassifier()
    }

    @Test
    fun packageHandling_identifiesYouTubePackageOnly() {
        assertTrue(classifier.canHandle("com.google.android.youtube"))
        assertFalse(classifier.canHandle("com.instagram.android"))
        assertFalse(classifier.canHandle("com.android.chrome"))
    }

    @Test
    fun normalVideoPlayback_isAllowed() {
        val context = AccessibilityWindowContext(
            packageName = "com.google.android.youtube",
            activeViewIds = setOf("com.google.android.youtube:id/watch_panel", "com.google.android.youtube:id/video_player")
        )
        val result = classifier.classify(context)
        assertFalse("Normal YouTube video must be allowed", result.isProtectedShortForm)
        assertEquals(SurfaceCategory.FEED, result.surfaceCategory)
    }

    @Test
    fun searchScreen_isAllowed() {
        val context = AccessibilityWindowContext(
            packageName = "com.google.android.youtube",
            activeViewIds = setOf("com.google.android.youtube:id/search_results")
        )
        val result = classifier.classify(context)
        assertFalse("YouTube search must be allowed", result.isProtectedShortForm)
    }

    @Test
    fun channelPage_isAllowed() {
        val context = AccessibilityWindowContext(
            packageName = "com.google.android.youtube",
            activeViewIds = setOf("com.google.android.youtube:id/channel_header", "com.google.android.youtube:id/feed_tabs")
        )
        val result = classifier.classify(context)
        assertFalse("YouTube channel must be allowed", result.isProtectedShortForm)
    }

    @Test
    fun commentsSection_isAllowed() {
        val context = AccessibilityWindowContext(
            packageName = "com.google.android.youtube",
            activeViewIds = setOf("com.google.android.youtube:id/comments_sheet", "com.google.android.youtube:id/comments_list")
        )
        val result = classifier.classify(context)
        assertFalse("YouTube comments must be allowed", result.isProtectedShortForm)
    }

    @Test
    fun shortsPlayer_isBlocked() {
        val context = AccessibilityWindowContext(
            packageName = "com.google.android.youtube",
            activeViewIds = setOf("com.google.android.youtube:id/reel_watch_pager", "com.google.android.youtube:id/shorts_player")
        )
        val result = classifier.classify(context)
        assertTrue("YouTube Shorts must be blocked", result.isProtectedShortForm)
        assertEquals(SurfaceCategory.SHORTS, result.surfaceCategory)
    }

    @Test
    fun shortsActivityOrFragment_isBlocked() {
        val context = AccessibilityWindowContext(
            packageName = "com.google.android.youtube",
            rootClassName = "com.google.android.apps.youtube.app.extensions.reel.ShortsActivity",
            activeViewIds = setOf("com.google.android.youtube:id/reel_recycler")
        )
        val result = classifier.classify(context)
        assertTrue("YouTube Shorts Fragment/Activity must be blocked", result.isProtectedShortForm)
        assertEquals(SurfaceCategory.SHORTS, result.surfaceCategory)
    }

    @Test
    fun defaultYouTubeBrowsing_isAllowed() {
        val context = AccessibilityWindowContext(
            packageName = "com.google.android.youtube",
            activeViewIds = emptySet()
        )
        val result = classifier.classify(context)
        assertFalse("Default YouTube browsing must be allowed", result.isProtectedShortForm)
    }
}
