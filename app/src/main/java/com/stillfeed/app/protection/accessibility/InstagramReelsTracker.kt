package com.stillfeed.app.protection.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.domain.repository.UnlockSessionRepository
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.floating.FloatingIconController
import com.stillfeed.app.protection.intervention.ui.InterventionViewModel
import com.stillfeed.app.widget.update.WidgetUpdateCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tracks Instagram Reels scrolling sessions, calculates organic Reels watched,
 * filters out sponsored ads, coordinates real-time counter updates with the Floating
 * Brain Companion, persists verified events into Room, notifies home-screen widgets,
 * and enforces blocking when allowance limits are reached.
 */
class InstagramReelsTracker(
    private val floatingIconController: FloatingIconController,
    private val usageRepository: UsageRepository? = null,
    private val widgetUpdateCoordinator: WidgetUpdateCoordinator? = null,
    private val appPreferencesRepository: AppPreferencesRepository? = null,
    private val unlockSessionRepository: UnlockSessionRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val reelCounter = AtomicInteger(0)
    private var lastScrollTimestamp = 0L
    private var lastReelSignature: String? = null
    private var isCurrentlyInReels = false
    private var lastReelsDetectedTimestamp = 0L

    var onLimitReached: ((count: Int, isBatch: Boolean, batchAllowance: Int?) -> Unit)? = null

    init {
        // Initialize counter with today's verified Reels consumption from Room if available
        scope.launch {
            try {
                if (usageRepository != null) {
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val startOfDay = calendar.timeInMillis
                    calendar.apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    val endOfDay = calendar.timeInMillis

                    val todayEvents = usageRepository.getEventsInRange(startOfDay, endOfDay)
                        .filter { it.packageName == "com.instagram.android" }
                    val todayReels = todayEvents.count { it.metadata == InterventionViewModel.VERIFIED_REEL_CONSUMPTION_METADATA }
                    if (todayReels > 0) {
                        reelCounter.set(todayReels)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Evaluates accessibility events from Instagram.
     */
    fun onAccessibilityEvent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        isReelsSurface: Boolean
    ) {
        val now = System.currentTimeMillis()

        if (!isReelsSurface) {
            // Exit debouncing: Do not drop Reels state on single transient subview events
            // Only deactivate if no Reels signals have been seen for REELS_EXIT_TIMEOUT_MS
            if (isCurrentlyInReels && (now - lastReelsDetectedTimestamp > REELS_EXIT_TIMEOUT_MS)) {
                isCurrentlyInReels = false
                floatingIconController.setReelsActive(false)
            }
            return
        }

        lastReelsDetectedTimestamp = now

        // Entering or currently in Reels: show floating companion below front camera in Reels
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

        val isScroll = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        val isWindowChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

        // When a user scrolls to a new reel:
        if ((isScroll || isWindowChange) && (now - lastScrollTimestamp > MIN_REEL_INTERVAL_MS)) {
            val signature = extractReelSignature(event, rootNode)
            if (signature != null && signature != lastReelSignature) {
                lastReelSignature = signature
                lastScrollTimestamp = now
                onReelConsumed(now)
            } else if (isScroll && (now - lastScrollTimestamp > MIN_SCROLL_INTERVAL_MS)) {
                // Fallback for scroll events without distinct node signatures
                lastScrollTimestamp = now
                onReelConsumed(now)
            }
        }
    }

    /**
     * Handles an organic Reel being consumed. Updates counter, floating companion,
     * persists event in Room database, refreshes home widgets, and blocks when allowance limit is reached.
     */
    private fun onReelConsumed(now: Long) {
        val newCount = reelCounter.incrementAndGet()
        floatingIconController.updateReelCount(newCount)

        scope.launch {
            try {
                // 1. Log verified individual reel consumption in Room
                usageRepository?.recordEvent(
                    UsageEvent(
                        id = UUID.randomUUID().toString(),
                        timestampMillis = now,
                        packageName = "com.instagram.android",
                        surfaceCategory = SurfaceCategory.REELS,
                        eventType = UsageEventType.UNLOCKED,
                        isBlocked = false,
                        metadata = InterventionViewModel.VERIFIED_REEL_CONSUMPTION_METADATA
                    )
                )

                // 2. Consume batch allowance if an unlock session is active
                val activeSession = unlockSessionRepository?.getActiveUnlock("com.instagram.android")
                val isBatchActive = activeSession != null && activeSession.isValid
                val updatedSession = if (activeSession != null) {
                    unlockSessionRepository.consumeReelAllowance(
                        packageName = "com.instagram.android",
                        signalKey = "REEL_CONSUMED",
                        isExplicitTransition = true
                    )
                } else null

                // 3. Immediately request widgets to refresh data
                widgetUpdateCoordinator?.requestUpdate()

                // 4. Check whether allowance limits are reached
                val prefs = appPreferencesRepository?.getPreferences()
                val dailyAllowance = prefs?.dailyReelsAllowance ?: 30
                val remainingBatch = updatedSession?.remainingBatchAllowance ?: activeSession?.remainingBatchAllowance ?: 0

                val isLimitExceeded = (isBatchActive && remainingBatch <= 0) ||
                        (!isBatchActive && newCount >= dailyAllowance)

                if (isLimitExceeded) {
                    withContext(Dispatchers.Main) {
                        onLimitReached?.invoke(newCount, isBatchActive, activeSession?.selectedBatchAllowance)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Resets the active calculated reels count to zero.
     */
    fun resetCount() {
        reelCounter.set(0)
        lastReelSignature = null
        floatingIconController.updateReelCount(0)
        widgetUpdateCoordinator?.requestUpdate()
    }

    /**
     * Immediately deactivates reels state and hides the floating companion.
     */
    fun setInactiveImmediate() {
        isCurrentlyInReels = false
        floatingIconController.setReelsActive(false)
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
                        AD_CTA_TEXTS.any { text.equals(it, ignoreCase = true) }) {
                        foundSponsored = true
                        return
                    }
                }

                // 2. Content description checks
                if (desc != null) {
                    if (desc.contains("Sponsored", ignoreCase = true) ||
                        AD_CTA_TEXTS.any { desc.contains(it, ignoreCase = true) }) {
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

    /**
     * Extracts a unique signature for the active Reel in view using author, audio, or window indices.
     */
    private fun extractReelSignature(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?): String? {
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && event.fromIndex >= 0) {
            return "SCROLL_${event.windowId}_${event.fromIndex}_${event.itemCount}"
        }

        if (rootNode != null) {
            var signature: String? = null
            val nodesToRecycle = mutableListOf<AccessibilityNodeInfo>()
            try {
                fun inspect(node: AccessibilityNodeInfo?, depth: Int) {
                    if (node == null || depth > 8 || signature != null) return

                    val viewId = node.viewIdResourceName
                    if (viewId != null) {
                        if (viewId.contains("clips_author") || viewId.contains("row_feed_photo_profile_name") || viewId.contains("user_name")) {
                            val text = node.text?.toString() ?: node.contentDescription?.toString()
                            if (!text.isNullOrBlank()) {
                                signature = "AUTHOR_$text"
                                return
                            }
                        }
                        if (viewId.contains("clips_audio") || viewId.contains("music_attribution")) {
                            val text = node.text?.toString() ?: node.contentDescription?.toString()
                            if (!text.isNullOrBlank()) {
                                signature = "AUDIO_$text"
                                return
                            }
                        }
                    }

                    for (i in 0 until node.childCount) {
                        if (signature != null) break
                        val child = try { node.getChild(i) } catch (_: Exception) { null }
                        if (child != null) {
                            nodesToRecycle.add(child)
                            inspect(child, depth + 1)
                        }
                    }
                }

                inspect(rootNode, 0)
            } catch (_: Exception) {
            } finally {
                nodesToRecycle.forEach {
                    try { it.recycle() } catch (_: Exception) {}
                }
            }
            if (signature != null) return signature
        }

        return if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            "SCROLL_TIME_${System.currentTimeMillis() / 1200L}"
        } else null
    }

    companion object {
        private const val MIN_REEL_INTERVAL_MS = 1000L
        private const val MIN_SCROLL_INTERVAL_MS = 1400L
        private const val REELS_EXIT_TIMEOUT_MS = 2500L

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
