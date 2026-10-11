package com.stillfeed.app.protection.engine

import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.protection.detection.CompositeSurfaceClassifier
import com.stillfeed.app.protection.detection.SurfaceClassifier
import com.stillfeed.app.protection.model.AccessibilityWindowContext
import com.stillfeed.app.protection.model.ProtectionDecision
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Domain-level contract for the STILLFEED attention-protection engine (STEP 06 Phase 3).
 *
 * Responsibilities:
 * 1. Coordinates persistent protection toggle state.
 * 2. Rapidly updates active blocking policy in real time without app restart.
 * 3. Evaluates structural window contexts and classifies short-form surfaces.
 * 4. Issues deterministic [ProtectionDecision.Block] or [ProtectionDecision.Allow].
 * 5. Records privacy-minimal [UsageEvent] entries when short-form sessions are intercepted.
 */
interface ProtectionEngine {
    val isProtectionEnabled: StateFlow<Boolean>

    /**
     * Evaluates the given [context] and issues a protection decision.
     */
    suspend fun evaluateContext(context: AccessibilityWindowContext): ProtectionDecision

    /**
     * Updates protection state immediately in memory.
     */
    fun updateProtectionState(enabled: Boolean)

    /**
     * Cancels any ongoing background observation.
     */
    fun close() {}
}

/**
 * Production implementation of [ProtectionEngine].
 */
class DefaultProtectionEngine(
    private val protectionStateRepository: ProtectionStateRepository,
    private val usageRepository: UsageRepository? = null,
    private val classifier: SurfaceClassifier = CompositeSurfaceClassifier(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    coroutineScope: CoroutineScope? = null
) : ProtectionEngine {

    private val engineScope = coroutineScope ?: CoroutineScope(SupervisorJob() + ioDispatcher)
    private var observerJob: kotlinx.coroutines.Job? = null

    private val _isProtectionEnabled = MutableStateFlow(false)
    override val isProtectionEnabled: StateFlow<Boolean> = _isProtectionEnabled.asStateFlow()

    // Tracks last blocked surface to prevent redundant duplicate database writes
    private var lastBlockedPackage: String? = null
    private var lastBlockedTimestamp: Long = 0L

    init {
        // Observe persistent protection state dynamically in real-time
        observerJob = engineScope.launch {
            try {
                _isProtectionEnabled.value = protectionStateRepository.getProtectionState().isEnabled
                protectionStateRepository.observeProtectionState().collect { state ->
                    _isProtectionEnabled.value = state.isEnabled
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Expected when scope cancels
            } catch (_: Exception) {
                // Fail-safe
            }
        }
    }

    override fun close() {
        observerJob?.cancel()
    }

    override fun updateProtectionState(enabled: Boolean) {
        _isProtectionEnabled.value = enabled
    }

    override suspend fun evaluateContext(context: AccessibilityWindowContext): ProtectionDecision =
        withContext(ioDispatcher) {
            // Rule 1: When protection is disabled, content is allowed through without intervention
            val enabled = if (_isProtectionEnabled.value) {
                true
            } else {
                val persistent = protectionStateRepository.getProtectionState().isEnabled
                if (persistent) {
                    _isProtectionEnabled.value = true
                }
                persistent
            }

            if (!enabled) {
                return@withContext ProtectionDecision.ProtectionDisabled
            }

            // Rule 2: Classify surface
            val classification = classifier.classify(context)

            if (classification.isProtectedShortForm) {
                // Rate-limit database events (e.g. 1 per 2 seconds per package) to avoid flooding Room
                val now = System.currentTimeMillis()
                val isRecentDuplicate = context.packageName == lastBlockedPackage &&
                        (now - lastBlockedTimestamp) < DEBOUNCE_DATABASE_WRITE_MS

                if (!isRecentDuplicate && usageRepository != null) {
                    val event = UsageEvent(
                        id = UUID.randomUUID().toString(),
                        timestampMillis = now,
                        packageName = context.packageName,
                        surfaceCategory = classification.surfaceCategory,
                        eventType = UsageEventType.BLOCKED,
                        isBlocked = true,
                        metadata = classification.matchedSignal
                    )
                    try {
                        usageRepository.recordEvent(event)
                        lastBlockedPackage = context.packageName
                        lastBlockedTimestamp = now
                    } catch (_: Exception) {
                        // Fail-safe: recording failure should never crash the engine
                    }
                }

                return@withContext ProtectionDecision.Block(
                    packageName = context.packageName,
                    surfaceCategory = classification.surfaceCategory,
                    reason = classification.matchedSignal ?: "Protected short-form surface detected"
                )
            }

            // Allowed surface (Feed, DMs, Stories, Normal YouTube, non-monitored apps)
            return@withContext ProtectionDecision.Allow(
                packageName = context.packageName,
                surfaceCategory = classification.surfaceCategory,
                reason = classification.matchedSignal ?: "Allowed surface"
            )
        }

    companion object {
        private const val DEBOUNCE_DATABASE_WRITE_MS = 2000L
    }
}
