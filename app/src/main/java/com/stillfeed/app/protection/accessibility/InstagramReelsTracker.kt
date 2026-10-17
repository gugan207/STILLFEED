package com.stillfeed.app.protection.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.stillfeed.app.floating.FloatingIconController
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tracks Instagram Reels scrolling sessions, calculates organic Reels watched,
 * filters out sponsored ads, and coordinates visibility and counter updates
 * with the Floating Brain Companion.
 */
class InstagramReelsTracker(
    private val floatingIconController: FloatingIconController
) {
    private val reelCounter = AtomicInteger(0)
    private var lastScrollTimestamp = 0L
    private var lastReelSignature: String? = null
    private var isCurrentlyInReels = false

    /**
     * Evaluates accessibility events from Instagram.
     */
    fun onAccessibilityEvent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        isReelsSurface: Boolean
    ) {
        if (!isReelsSurface) {
            if (isCurrentlyInReels) {
                isCurrentlyInReels = false
                floatingIconController.setReelsActive(false)
            }
            return
        }

        // Entering or currently in Reels: show floating companion in top-center of Reels
        if (!isCurrentlyInReels) {
            isCurrentlyInReels = true
            floatingIconController.setReelsActive(true)
            floatingIconController.updateReelCount(reelCounter.get())
        }

        // Check for ad / sponsored reel
        val isAd = isSponsoredAd(rootNode)
        if (isAd) {
            // "avoid the ads counting" - do not increment counter for sponsored reels!
            return
        }

        val now = System.currentTimeMillis()
        val isScroll = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        val isWindowChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

        // When a user scrolls to a new reel:
        if ((isScroll || isWindowChange) && (now - lastScrollTimestamp > MIN_REEL_INTERVAL_MS)) {
            val signature = extractReelSignature(event)
            if (signature != null && signature != lastReelSignature) {
                lastReelSignature = signature
                lastScrollTimestamp = now
                val newCount = reelCounter.incrementAndGet()
                floatingIconController.updateReelCount(newCount)
            } else if (isScroll && (now - lastScrollTimestamp > MIN_SCROLL_INTERVAL_MS)) {
                // Fallback for scroll events without distinct node indices
                lastScrollTimestamp = now
                val newCount = reelCounter.incrementAndGet()
                floatingIconController.updateReelCount(newCount)
            }
        }
    }

    /**
     * Resets the active calculated reels count to zero.
     */
    fun resetCount() {
        reelCounter.set(0)
        lastReelSignature = null
        floatingIconController.updateReelCount(0)
    }

    /**
     * Gets the current calculated reel count.
     */
    fun getCurrentCount(): Int = reelCounter.get()

    /**
     * Inspects nodes to determine if the currently displayed Reel is an advertisement / sponsored post.
     */
    private fun isSponsoredAd(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false

        var foundSponsored = false
        val nodesToRecycle = mutableListOf<AccessibilityNodeInfo>()

        try {
            fun inspect(node: AccessibilityNodeInfo?, depth: Int) {
                if (node == null || depth > 8 || foundSponsored) return

                val text = node.text?.toString()
                val desc = node.contentDescription?.toString()
                val viewId = node.viewIdResourceName

                // 1. Text checks for "Sponsored" or promotional CTA
                if (text != null) {
                    if (text.equals("Sponsored", ignoreCase = true) ||
                        text.equals("sponsored", ignoreCase = true) ||
                        AD_CTA_TEXTS.any { text.equals(it, ignoreCase = true) }) {
                        foundSponsored = true
                        return
                    }
                }

                // 2. Content description checks
                if (desc != null) {
                    if (desc.contains("Sponsored", ignoreCase = true) ||
                        desc.contains("sponsored", ignoreCase = true)) {
                        foundSponsored = true
                        return
                    }
                }

                // 3. View ID checks
                if (viewId != null) {
                    if (AD_VIEW_ID_SIGNALS.any { viewId.contains(it, ignoreCase = true) }) {
                        foundSponsored = true
                        return
                    }
                }

                for (i in 0 until node.childCount) {
                    if (foundSponsored) break
                    val child = try { node.getChild(i) } catch (_: Exception) { null }
                    if (child != null) {
                        nodesToRecycle.add(child)
                        inspect(child, depth + 1)
                    }
                }
            }

            inspect(rootNode, 0)
        } catch (_: Exception) {
            // Fail-safe
        } finally {
            nodesToRecycle.forEach {
                try { it.recycle() } catch (_: Exception) {}
            }
        }

        return foundSponsored
    }

    private fun extractReelSignature(event: AccessibilityEvent): String? {
        return if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            "SCROLL_${event.windowId}_${event.fromIndex}_${event.itemCount}"
        } else {
            null
        }
    }

    companion object {
        private const val MIN_REEL_INTERVAL_MS = 1400L
        private const val MIN_SCROLL_INTERVAL_MS = 2000L

        val AD_CTA_TEXTS = setOf(
            "Sponsored",
            "Install Now",
            "Shop Now",
            "Learn More",
            "Sign Up",
            "Download",
            "Watch More",
            "Apply Now",
            "Book Now",
            "Contact Us",
            "Order Now",
            "Get Quote"
        )

        val AD_VIEW_ID_SIGNALS = listOf(
            "ad_action_button",
            "sponsored_reel",
            "clips_ad_",
            "lead_gen_cta",
            "sponsored_context_widget"
        )
    }
}
