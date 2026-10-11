package com.stillfeed.app.protection

import android.accessibilityservice.AccessibilityService
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.protection.accessibility.StillfeedAccessibilityService
import com.stillfeed.app.protection.blocking.DefaultInterventionCoordinator
import com.stillfeed.app.protection.blocking.InterventionActivity
import com.stillfeed.app.protection.model.ProtectionDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InterventionCoordinatorTest {

    private lateinit var service: StillfeedAccessibilityService
    private lateinit var coordinator: DefaultInterventionCoordinator
    private val globalActionsPerformed = mutableListOf<Int>()

    @Before
    fun setUp() {
        service = Robolectric.buildService(StillfeedAccessibilityService::class.java).create().get()
        globalActionsPerformed.clear()
        coordinator = DefaultInterventionCoordinator(
            cooldownMs = 500L,
            launchActivity = true,
            globalActionPerformer = { _, action ->
                globalActionsPerformed.add(action)
                true
            }
        )
    }

    @Test
    fun executeIntervention_performsGlobalActionBack() {
        val decision = ProtectionDecision.Block(
            packageName = "com.instagram.android",
            surfaceCategory = SurfaceCategory.REELS,
            reason = "Reels detected"
        )

        coordinator.executeIntervention(service, decision)

        assertTrue(
            "Global action list should contain GLOBAL_ACTION_BACK",
            globalActionsPerformed.contains(AccessibilityService.GLOBAL_ACTION_BACK)
        )

        val startedIntent = shadowOf(service).nextStartedActivity
        assertNotNull("Should start InterventionActivity", startedIntent)
        assertEquals(InterventionActivity::class.java.name, startedIntent.component?.className)
        assertEquals("com.instagram.android", startedIntent.getStringExtra(InterventionActivity.EXTRA_BLOCKED_PACKAGE))
        assertEquals(SurfaceCategory.REELS.name, startedIntent.getStringExtra(InterventionActivity.EXTRA_SURFACE_CATEGORY))
    }

    @Test
    fun executeIntervention_respectsCooldown() {
        val decision = ProtectionDecision.Block(
            packageName = "com.instagram.android",
            surfaceCategory = SurfaceCategory.REELS,
            reason = "Reels detected"
        )

        // First call triggers
        coordinator.executeIntervention(service, decision)
        assertEquals(1, globalActionsPerformed.size)

        // Rapid second call within cooldown (500ms) is ignored
        coordinator.executeIntervention(service, decision)
        assertEquals("Second call within cooldown must be throttled", 1, globalActionsPerformed.size)
    }

    @Test
    fun executeIntervention_differentPackage_bypassesCooldown() {
        val igDecision = ProtectionDecision.Block(
            packageName = "com.instagram.android",
            surfaceCategory = SurfaceCategory.REELS,
            reason = "Reels detected"
        )
        val ytDecision = ProtectionDecision.Block(
            packageName = "com.google.android.youtube",
            surfaceCategory = SurfaceCategory.SHORTS,
            reason = "Shorts detected"
        )

        coordinator.executeIntervention(service, igDecision)
        assertEquals(1, globalActionsPerformed.size)

        coordinator.executeIntervention(service, ytDecision)
        assertEquals("Different package should not be throttled by previous package", 2, globalActionsPerformed.size)
    }
}
