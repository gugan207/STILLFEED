package com.stillfeed.app.protection.accessibility

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.stillfeed.app.protection.model.AccessibilityWindowContext

/**
 * Adapter that converts raw Android [AccessibilityEvent] and [AccessibilityNodeInfo] hierarchies
 * into lightweight, privacy-safe [AccessibilityWindowContext] structural snapshots.
 *
 * PRIVACY GUARANTEES (STEP 06 Phase 9):
 * 1. ZERO textual content extraction: Does NOT read node.text, user chat messages, or captions.
 * 2. ZERO credential inspection: Ignores password fields and sensitive input fields.
 * 3. ZERO media access: Does not capture frames, screenshots, or canvas data.
 * 4. Structural ONLY: Collects only resource IDs (e.g. "clips_viewer_view_pager") and class names.
 *
 * RESOURCE SAFETY (STEP 06 Phase 2):
 * - Bounds tree traversal (max depth 10, max visited 50 nodes).
 * - Immediate recycling/releasing of node instances to prevent memory leaks and IPC buffer saturation.
 * - Failsafe null checking for every level of the view hierarchy.
 */
class AccessibilityEventAdapter(
    private val maxDepth: Int = DEFAULT_MAX_DEPTH,
    private val maxNodesToInspect: Int = DEFAULT_MAX_NODES
) {

    /**
     * Extracts a privacy-safe structural snapshot from the event and active window root.
     */
    fun extractWindowContext(
        event: AccessibilityEvent?,
        rootNode: AccessibilityNodeInfo?
    ): AccessibilityWindowContext {
        if (event == null) {
            return AccessibilityWindowContext(packageName = "")
        }

        val packageName = event.packageName?.toString() ?: ""
        val rootClassName = event.className?.toString()

        val activeViewIds = mutableSetOf<String>()
        val structuralTags = mutableSetOf<String>()

        if (rootClassName != null) {
            structuralTags.add(rootClassName)
        }

        // If a root node is available, extract structural resource IDs safely
        if (rootNode != null) {
            var visitedCount = 0
            val nodesToRecycle = mutableListOf<AccessibilityNodeInfo>()

            try {
                fun traverse(node: AccessibilityNodeInfo?, depth: Int) {
                    if (node == null || depth > maxDepth || visitedCount >= maxNodesToInspect) {
                        return
                    }
                    visitedCount++

                    // Extract ONLY viewIdResourceName (structural identifier)
                    node.viewIdResourceName?.let { viewId ->
                        activeViewIds.add(viewId)
                    }

                    // Extract class name (structural identifier)
                    node.className?.let { className ->
                        structuralTags.add(className.toString())
                    }

                    val childCount = node.childCount
                    for (i in 0 until childCount) {
                        if (visitedCount >= maxNodesToInspect) break
                        val child = try {
                            node.getChild(i)
                        } catch (_: Exception) {
                            null
                        }
                        if (child != null) {
                            nodesToRecycle.add(child)
                            traverse(child, depth + 1)
                        }
                    }
                }

                traverse(rootNode, depth = 0)
            } catch (_: Exception) {
                // Fail-safe: unexpected accessibility platform changes should never crash
            } finally {
                // Recycle all allocated child nodes to prevent IPC memory exhaustion
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    nodesToRecycle.forEach {
                        try {
                            @Suppress("DEPRECATION")
                            it.recycle()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }

        return AccessibilityWindowContext(
            packageName = packageName,
            rootClassName = rootClassName,
            activeViewIds = activeViewIds,
            structuralTags = structuralTags,
            windowId = event.windowId,
            timestampMillis = System.currentTimeMillis()
        )
    }

    companion object {
        const val DEFAULT_MAX_DEPTH = 10
        const val DEFAULT_MAX_NODES = 40
    }
}
