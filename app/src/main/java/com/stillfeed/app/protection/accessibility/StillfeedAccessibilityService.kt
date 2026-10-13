package com.stillfeed.app.protection.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.stillfeed.app.StillfeedApplication
import com.stillfeed.app.protection.blocking.DefaultInterventionCoordinator
import com.stillfeed.app.protection.blocking.InterventionCoordinator
import com.stillfeed.app.protection.detection.CompositeSurfaceClassifier
import com.stillfeed.app.protection.engine.DefaultProtectionEngine
import com.stillfeed.app.protection.engine.ProtectionEngine
import com.stillfeed.app.protection.model.ProtectionDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Production Android Accessibility Service for STILLFEED (STEP 06 Phase 2).
 *
 * ARCHITECTURAL SEPARATION:
 * [StillfeedAccessibilityService]
 *   -> [AccessibilityEventAdapter] (Privacy-safe window context extraction)
 *   -> [ProtectionEngine] (Surface evaluation & domain decision)
 *   -> [InterventionCoordinator] (Immediate non-invasive interception)
 *
 * SAFETY & PRIVACY GUARANTEES:
 * 1. Debounces high-frequency scroll and content change events via [EventDebouncer].
 * 2. Bounds node traversal and recycles node resources immediately to prevent memory leaks.
 * 3. Extracts structural view IDs only; NEVER inspects text, messages, passwords, or frames.
 * 4. Operates local-first with zero internet requirement.
 * 5. Fails safely on missing nodes, background transitions, or unexpected OS events.
 */
class StillfeedAccessibilityService : AccessibilityService() {

    var serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var protectionEngine: ProtectionEngine? = null
    var eventAdapter: AccessibilityEventAdapter = AccessibilityEventAdapter()
    var eventDebouncer: EventDebouncer = EventDebouncer()
    var interventionCoordinator: InterventionCoordinator = DefaultInterventionCoordinator()

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        initializeDependencies()
    }

    fun initializeDependencies() {
        val app = applicationContext as? StillfeedApplication
        if (app != null && protectionEngine == null) {
            protectionEngine = app.protectionEngine
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName.isBlank()) return

        // Fast rejection: Only process monitored apps (Instagram & YouTube)
        if (!MONITORED_PACKAGES.contains(packageName)) return

        // Debounce noisy events
        if (!eventDebouncer.shouldProcess(event)) return

        // Safe extraction of structural window context (privacy-minimal)
        val rootNode = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        }

        val windowContext = eventAdapter.extractWindowContext(event, rootNode)

        // Evaluate with domain protection engine
        val engine = protectionEngine ?: return
        serviceScope.launch {
            try {
                val decision = engine.evaluateContext(windowContext)
                if (decision is ProtectionDecision.Block) {
                    interventionCoordinator.executeIntervention(
                        this@StillfeedAccessibilityService,
                        decision
                    )
                }
            } catch (_: Exception) {
                // Fail-safe: evaluation error must never crash the accessibility service
            }
        }
    }

    override fun onInterrupt() {
        eventDebouncer.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
    }

    companion object {
        var isRunning: Boolean = false
            internal set

        val MONITORED_PACKAGES = setOf(
            "com.instagram.android",
            "com.google.android.youtube"
        )
    }
}
