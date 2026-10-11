package com.stillfeed.app.protection.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import com.stillfeed.app.protection.model.ProtectionDecision

/**
 * Coordinates and executes immediate blocking intervention when a protected short-form
 * surface is detected (STEP 06 Phase 6).
 */
interface InterventionCoordinator {
    /**
     * Executes the blocking intervention on the active [service].
     */
    fun executeIntervention(service: AccessibilityService, decision: ProtectionDecision.Block)
}

/**
 * Production implementation of [InterventionCoordinator].
 *
 * Deterministic Intervention Rules:
 * 1. Stops further user interaction with the algorithmic video stream immediately by invoking
 *    [AccessibilityService.GLOBAL_ACTION_BACK]. If the user tapped a Reel link in DMs, this
 *    immediately returns them to their DM conversation.
 * 2. Launches the calm, non-judgmental [InterventionActivity] providing explicit exit options.
 * 3. Enforces an intervention cooldown to avoid tight loop trigger storms.
 * 4. Zero shame, guilt, failure, or negative messaging.
 */
class DefaultInterventionCoordinator(
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    private val launchActivity: Boolean = true,
    private val globalActionPerformer: (AccessibilityService, Int) -> Boolean = { service, action ->
        try {
            service.performGlobalAction(action)
        } catch (_: Exception) {
            false
        }
    }
) : InterventionCoordinator {

    private var lastInterventionTimestamp: Long = 0L
    private var lastIntervenedPackage: String? = null

    @Synchronized
    override fun executeIntervention(service: AccessibilityService, decision: ProtectionDecision.Block) {
        val now = System.currentTimeMillis()
        if (decision.packageName == lastIntervenedPackage && (now - lastInterventionTimestamp) < cooldownMs) {
            return
        }

        lastInterventionTimestamp = now
        lastIntervenedPackage = decision.packageName

        // 1. Immediate Action: Pop out of the short-form viewer
        try {
            globalActionPerformer(service, AccessibilityService.GLOBAL_ACTION_BACK)
        } catch (_: Exception) {
            // Fail-safe
        }

        // 2. Launch Stillfeed Intervention UI if enabled
        if (launchActivity) {
            try {
                val intent = Intent(service, InterventionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(InterventionActivity.EXTRA_BLOCKED_PACKAGE, decision.packageName)
                    putExtra(InterventionActivity.EXTRA_SURFACE_CATEGORY, decision.surfaceCategory.name)
                }
                service.startActivity(intent)
            } catch (_: Exception) {
                // Fail-safe in background
            }
        }
    }

    companion object {
        const val DEFAULT_COOLDOWN_MS = 800L
    }
}
