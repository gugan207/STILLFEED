package com.stillfeed.app.usage.reconciliation

import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.usage.analysis.ForegroundSession
import com.stillfeed.app.usage.analysis.UsageSessionAnalyzer
import kotlin.math.abs

/**
 * Reconciles analyzed [ForegroundSession]s with existing Room usage events and
 * STEP 06 Accessibility Service interventions (STEP 07 Section 9).
 *
 * Deterministic Reconciliation Rules:
 * 1. Duplicate Session Prevention: If a session matching the package and time window already
 *    exists in Room, it is skipped to avoid double-counting.
 * 2. Step 06 Intervention Correlation: If an Accessibility Service BLOCKED event occurred during
 *    the session window, the session is marked as intervened (isBlocked = true) and attributed
 *    to REELS / SHORTS without inflating its real UsageStats duration.
 * 3. Bounded Persistence: Persists only validated [UsageEvent] records with SESSION_END.
 */
class UsageReconciler(
    private val usageRepository: UsageRepository
) {

    /**
     * Reconciles candidate [sessions] against existing Room events and records new sessions.
     * Returns the list of newly persisted [UsageEvent] records.
     */
    suspend fun reconcile(sessions: List<ForegroundSession>): List<UsageEvent> {
        if (sessions.isEmpty()) return emptyList()

        val minTime = sessions.minOf { it.startTimeMillis } - PADDING_WINDOW_MS
        val maxTime = sessions.maxOf { it.endTimeMillis } + PADDING_WINDOW_MS

        val existingEvents = usageRepository.getEventsInRange(minTime, maxTime)
        val existingSessions = existingEvents.filter { it.eventType == UsageEventType.SESSION_END }
        val existingBlockedEvents = existingEvents.filter { it.isBlocked || it.eventType == UsageEventType.BLOCKED }

        val eventsToInsert = mutableListOf<UsageEvent>()

        for (session in sessions) {
            // Rule 1: Duplicate Prevention
            val isDuplicate = existingSessions.any { existing ->
                existing.packageName == session.packageName &&
                        (abs(existing.timestampMillis - session.endTimeMillis) < DUPLICATE_TOLERANCE_MS ||
                                (session.startTimeMillis <= existing.timestampMillis && existing.timestampMillis <= session.endTimeMillis))
            }

            if (isDuplicate) {
                continue // Skip duplicate session
            }

            // Rule 2: Correlate with Accessibility Service Intervention
            val overlappingBlock = existingBlockedEvents.firstOrNull { block ->
                block.packageName == session.packageName &&
                        block.timestampMillis in (session.startTimeMillis - INTERVENTION_TOLERANCE_MS)..(session.endTimeMillis + INTERVENTION_TOLERANCE_MS)
            }

            val isIntervened = overlappingBlock != null || session.isIntervened
            val surfaceCategory = if (isIntervened) {
                if (session.packageName == UsageSessionAnalyzer.PACKAGE_INSTAGRAM) {
                    SurfaceCategory.REELS
                } else {
                    SurfaceCategory.SHORTS
                }
            } else {
                session.surfaceCategory
            }

            val metadata = if (isIntervened) {
                "Intervened by STILLFEED"
            } else {
                "Foreground session"
            }

            val event = UsageEvent(
                id = session.id,
                timestampMillis = session.endTimeMillis,
                packageName = session.packageName,
                surfaceCategory = surfaceCategory,
                eventType = UsageEventType.SESSION_END,
                durationMillis = session.durationMillis,
                isBlocked = isIntervened,
                metadata = metadata
            )

            eventsToInsert.add(event)
        }

        if (eventsToInsert.isNotEmpty()) {
            usageRepository.recordEvents(eventsToInsert)
        }

        return eventsToInsert
    }

    companion object {
        private const val PADDING_WINDOW_MS = 5000L
        private const val DUPLICATE_TOLERANCE_MS = 2000L
        private const val INTERVENTION_TOLERANCE_MS = 1500L
    }
}
