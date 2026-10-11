package com.stillfeed.app.protection.detection

import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.protection.model.AccessibilityWindowContext
import com.stillfeed.app.protection.model.SurfaceClassification

/**
 * Surface classifier for YouTube ([PACKAGE_YOUTUBE]).
 *
 * SPECIFICATION (STEP 06 Phase 5):
 * - Normal video -> ALLOWED
 * - Search -> ALLOWED
 * - Channel -> ALLOWED
 * - Comments -> ALLOWED
 * - Shorts -> BLOCKED
 *
 * Targets algorithmic short-form YouTube Shorts while preserving normal long-form video playback,
 * channels, comments, and search.
 */
class YouTubeSurfaceClassifier : SurfaceClassifier {

    override fun canHandle(packageName: String): Boolean =
        packageName == PACKAGE_YOUTUBE

    override fun classify(context: AccessibilityWindowContext): SurfaceClassification {
        if (!canHandle(context.packageName)) {
            return SurfaceClassification.allowed(SurfaceCategory.UNKNOWN, "Not YouTube")
        }

        // 1. Check for YouTube Shorts indicators
        val matchedShortsSignal = SHORTS_IDENTIFIERS.firstOrNull { signal ->
            context.hasViewIdContaining(signal) ||
                    context.hasTagContaining(signal) ||
                    context.rootClassName?.contains(signal, ignoreCase = true) == true
        }

        if (matchedShortsSignal != null) {
            return SurfaceClassification.blocked(
                surfaceCategory = SurfaceCategory.SHORTS,
                signal = "YouTube Shorts matched: $matchedShortsSignal"
            )
        }

        // 2. Check for standard YouTube features (regular video, search, channel, comments)
        val matchedNormalVideoSignal = NORMAL_VIDEO_IDENTIFIERS.firstOrNull { signal ->
            context.hasViewIdContaining(signal) || context.hasTagContaining(signal)
        }
        if (matchedNormalVideoSignal != null) {
            return SurfaceClassification.allowed(
                surfaceCategory = SurfaceCategory.FEED,
                signal = "YouTube standard video allowed: $matchedNormalVideoSignal"
            )
        }

        val matchedBrowseSignal = BROWSE_SEARCH_IDENTIFIERS.firstOrNull { signal ->
            context.hasViewIdContaining(signal) || context.hasTagContaining(signal)
        }
        if (matchedBrowseSignal != null) {
            return SurfaceClassification.allowed(
                surfaceCategory = SurfaceCategory.UNKNOWN,
                signal = "YouTube search/channel/comments allowed: $matchedBrowseSignal"
            )
        }

        // Default: Normal YouTube browsing allowed
        return SurfaceClassification.allowed(
            surfaceCategory = SurfaceCategory.UNKNOWN,
            signal = "Default YouTube browsing allowed"
        )
    }

    companion object {
        const val PACKAGE_YOUTUBE = "com.google.android.youtube"

        /**
         * Structural identifiers uniquely identifying YouTube Shorts.
         */
        val SHORTS_IDENTIFIERS = listOf(
            "reel_watch_pager",
            "reel_watch_fragment",
            "reel_player_fragment",
            "shorts_container",
            "shorts_player",
            "reel_recycler",
            "pivot_shorts",
            "ShortsFragment",
            "ShortsActivity",
            "shorts_shelf_item",
            "shorts_overlay",
            "shorts_sound_header"
        )

        /**
         * Identifiers associated with standard long-form YouTube video playback.
         */
        val NORMAL_VIDEO_IDENTIFIERS = listOf(
            "watch_panel",
            "player_fragment",
            "watch_while_layout",
            "video_player",
            "player_view",
            "full_screen_player"
        )

        /**
         * Identifiers associated with channels, search, comments, and home feed.
         */
        val BROWSE_SEARCH_IDENTIFIERS = listOf(
            "search_results",
            "channel_header",
            "comments_sheet",
            "comments_list",
            "browse_fragment",
            "feed_tabs",
            "account_header"
        )
    }
}
