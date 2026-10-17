package com.stillfeed.app.protection.detection

import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.protection.model.AccessibilityWindowContext
import com.stillfeed.app.protection.model.SurfaceClassification

/**
 * Surface classifier for Instagram ([PACKAGE_INSTAGRAM]).
 *
 * SPECIFICATION (STEP 06 Phase 4):
 * - Feed -> ALLOWED
 * - Stories -> ALLOWED
 * - DMs -> ALLOWED
 * - Profile / Settings -> ALLOWED
 * - Reels (standalone or tab) -> BLOCKED
 * - Reel opened from DM or shared link -> BLOCKED
 *
 * Detection Heuristics:
 * Targets short-form algorithmic video feeds (Clips / Reels) while leaving all communication
 * (Direct Messages, Stories, Profile, Settings) and ordinary browsing accessible.
 */
class InstagramSurfaceClassifier : SurfaceClassifier {

    override fun canHandle(packageName: String): Boolean =
        packageName == PACKAGE_INSTAGRAM

    override fun classify(context: AccessibilityWindowContext): SurfaceClassification {
        if (!canHandle(context.packageName)) {
            return SurfaceClassification.allowed(SurfaceCategory.UNKNOWN, "Not Instagram")
        }

        // 1. Check for Reels indicators (Clips viewer, reels view pager, clips container)
        val matchedReelsSignal = REELS_IDENTIFIERS.firstOrNull { signal ->
            context.hasViewIdContaining(signal) ||
                    context.hasTagContaining(signal) ||
                    context.rootClassName?.contains(signal, ignoreCase = true) == true
        }

        if (matchedReelsSignal != null) {
            // A Reel opened from DM will display the clips viewer on top of DM thread.
            // The presence of a clips/reels container establishes that a Reel is visible.
            return SurfaceClassification.blocked(
                surfaceCategory = SurfaceCategory.REELS,
                signal = "Instagram Reels matched: $matchedReelsSignal"
            )
        }

        // 2. Identify allowed surfaces (Stories, DMs, Feed, Profile, Settings)
        val matchedStorySignal = STORY_IDENTIFIERS.firstOrNull { signal ->
            context.hasViewIdContaining(signal) || context.hasTagContaining(signal)
        }
        if (matchedStorySignal != null) {
            return SurfaceClassification.allowed(
                surfaceCategory = SurfaceCategory.FEED,
                signal = "Instagram Story allowed: $matchedStorySignal"
            )
        }

        val matchedDmSignal = DM_IDENTIFIERS.firstOrNull { signal ->
            context.hasViewIdContaining(signal) ||
                    context.hasTagContaining(signal) ||
                    context.rootClassName?.contains(signal, ignoreCase = true) == true
        }
        if (matchedDmSignal != null) {
            return SurfaceClassification.allowed(
                surfaceCategory = SurfaceCategory.UNKNOWN,
                signal = "Instagram DM allowed: $matchedDmSignal"
            )
        }

        val matchedFeedSignal = FEED_PROFILE_IDENTIFIERS.firstOrNull { signal ->
            context.hasViewIdContaining(signal) || context.hasTagContaining(signal)
        }
        if (matchedFeedSignal != null) {
            return SurfaceClassification.allowed(
                surfaceCategory = SurfaceCategory.FEED,
                signal = "Instagram Feed/Profile allowed: $matchedFeedSignal"
            )
        }

        // Default: If no short-form algorithmic video container is detected, allow normal browsing
        return SurfaceClassification.allowed(
            surfaceCategory = SurfaceCategory.UNKNOWN,
            signal = "Default Instagram browsing allowed"
        )
    }

    companion object {
        const val PACKAGE_INSTAGRAM = "com.instagram.android"

        /**
         * Structural identifiers uniquely indicating the Instagram Reels / Clips short-form viewer.
         */
        val REELS_IDENTIFIERS = listOf(
            "clips_viewer_view_pager",
            "clips_video_container",
            "clips_swipe_refresh_container",
            "clips_media_item",
            "reel_viewer_clips_item",
            "clips_viewer",
            "ClipsViewerActivity",
            "TransparentModalActivity",
            "reels_tab",
            "clips_tab",
            "clips_item_container",
            "clips_root",
            "clips_author",
            "clips_audio",
            "clips_like",
            "clips_comment",
            "clips",
            "reels",
            "feed_video",
            "video_container"
        )

        /**
         * Identifiers associated with Instagram Stories (ephemeral content; NOT algorithmic Reels).
         */
        val STORY_IDENTIFIERS = listOf(
            "reel_viewer_image_view",
            "reel_viewer_shadow_top",
            "reel_feed_timeline",
            "story_viewer",
            "reel_viewer_root",
            "direct_story"
        )

        /**
         * Identifiers associated with Direct Messages (conversations, chat threads, inbox).
         */
        val DM_IDENTIFIERS = listOf(
            "direct_thread",
            "direct_inbox",
            "direct_chat",
            "DirectThreadActivity",
            "DirectInboxFragment",
            "message_composer",
            "row_thread_title",
            "thread_title"
        )

        /**
         * Identifiers associated with regular feed, profile, and settings.
         */
        val FEED_PROFILE_IDENTIFIERS = listOf(
            "feed_recycler_view",
            "main_feed",
            "profile_tab",
            "profile_header",
            "settings_list",
            "settings_item"
        )
    }
}
