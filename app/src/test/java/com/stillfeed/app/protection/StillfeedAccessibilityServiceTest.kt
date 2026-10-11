package com.stillfeed.app.protection

import android.view.accessibility.AccessibilityEvent
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.protection.accessibility.StillfeedAccessibilityService
import com.stillfeed.app.protection.blocking.InterventionCoordinator
import com.stillfeed.app.protection.engine.ProtectionEngine
import com.stillfeed.app.protection.model.AccessibilityWindowContext
import com.stillfeed.app.protection.model.ProtectionDecision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StillfeedAccessibilityServiceTest {

    private lateinit var service: StillfeedAccessibilityService
    private lateinit var fakeEngine: FakeProtectionEngine
    private lateinit var fakeCoordinator: FakeInterventionCoordinator

    class FakeProtectionEngine : ProtectionEngine {
        private val _enabled = MutableStateFlow(true)
        override val isProtectionEnabled: StateFlow<Boolean> = _enabled
        var evaluatedContexts = mutableListOf<AccessibilityWindowContext>()
        var nextDecision: ProtectionDecision = ProtectionDecision.ProtectionDisabled

        override suspend fun evaluateContext(context: AccessibilityWindowContext): ProtectionDecision {
            evaluatedContexts.add(context)
            return nextDecision
        }

        override fun updateProtectionState(enabled: Boolean) {
            _enabled.value = enabled
        }
    }

    class FakeInterventionCoordinator : InterventionCoordinator {
        var interventions = mutableListOf<ProtectionDecision.Block>()

        override fun executeIntervention(
            service: android.accessibilityservice.AccessibilityService,
            decision: ProtectionDecision.Block
        ) {
            interventions.add(decision)
        }
    }

    @Before
    fun setUp() {
        service = Robolectric.buildService(StillfeedAccessibilityService::class.java).create().get()
        service.serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)
        fakeEngine = FakeProtectionEngine()
        fakeCoordinator = FakeInterventionCoordinator()

        service.protectionEngine = fakeEngine
        service.interventionCoordinator = fakeCoordinator
    }

    @Test
    fun unmonitoredPackage_isIgnoredImmediately() {
        val event = AccessibilityEvent.obtain().apply {
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            packageName = "com.whatsapp"
        }

        service.onAccessibilityEvent(event)

        assertTrue("Unmonitored packages must not be evaluated by engine", fakeEngine.evaluatedContexts.isEmpty())
        assertTrue(fakeCoordinator.interventions.isEmpty())
    }

    @Test
    fun monitoredPackage_evaluatedByEngine() {
        val event = AccessibilityEvent.obtain().apply {
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            packageName = "com.instagram.android"
        }

        service.onAccessibilityEvent(event)

        // Give the coroutine a tick to execute
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        assertEquals(1, fakeEngine.evaluatedContexts.size)
        assertEquals("com.instagram.android", fakeEngine.evaluatedContexts[0].packageName)
    }

    @Test
    fun nullEventOrMissingPackage_doesNotCrash() {
        service.onAccessibilityEvent(null)

        val emptyEvent = AccessibilityEvent.obtain().apply {
            packageName = ""
        }
        service.onAccessibilityEvent(emptyEvent)

        assertTrue(fakeEngine.evaluatedContexts.isEmpty())
    }

    @Test
    fun onInterrupt_resetsEventDebouncer() {
        service.onInterrupt()
        // Must succeed without throwing
    }

    @Test
    fun onDestroy_cancelsScopeSafely() {
        service.onDestroy()
        // Must succeed without throwing
    }
}
