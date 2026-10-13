package com.stillfeed.app.protection.blocking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stillfeed.app.StillfeedApplication
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.protection.intervention.ui.InterventionStep
import com.stillfeed.app.protection.intervention.ui.InterventionViewModel
import com.stillfeed.app.protection.intervention.ui.Screen08BlockedReel
import com.stillfeed.app.protection.intervention.ui.Screen09ResetActivity
import com.stillfeed.app.protection.intervention.ui.Screen10AwarenessCards
import com.stillfeed.app.protection.intervention.ui.Screen11UnlockSelection

/**
 * Production intervention activity orchestrating the complete Figma intervention journey (STEP 19).
 *
 * Screen Progression:
 * 1. Screen 08 — Blocked Reel (Figma 39:162): Choice between "Exit reels" (Go back) and "Watch reels" (Choose a reset).
 * 2. Screen 09 — Reset Activity (Figma 64:7): Choose & execute a pause, walk, movement, or card reset with timer.
 * 3. Screen 10 — Awareness Cards (Figma 64:46): Read authored reflective cards with pause indicator.
 * 4. Screen 11 — Unlock (Figma 64:65): Select 5, 10, 20, or 30 Reels/Shorts batch unlock.
 *
 * Guarantees:
 * - Single activity prevents duplicate screen stacking.
 * - Back button safely steps back or exits without granting an unlock.
 * - Atomic unlock creation before returning to the protected app.
 */
class InterventionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        val surfaceCategory = intent.getStringExtra(EXTRA_SURFACE_CATEGORY) ?: "Reel"
        val startStepName = intent.getStringExtra(EXTRA_START_STEP)
        val initialStep = startStepName?.let {
            try {
                InterventionStep.valueOf(it)
            } catch (_: Exception) {
                InterventionStep.BLOCKED_REEL
            }
        } ?: InterventionStep.BLOCKED_REEL

        val app = application as? StillfeedApplication

        setContent {
            StillfeedTheme {
                val vm: InterventionViewModel = viewModel(
                    factory = app?.let {
                        InterventionViewModel.provideFactory(
                            unlockSessionRepository = it.unlockSessionRepository,
                            usageRepository = it.usageRepository,
                            blockedPackage = blockedPackage,
                            surfaceCategory = surfaceCategory,
                            initialStep = initialStep
                        )
                    } ?: throw IllegalStateException("StillfeedApplication must be initialized.")
                )

                val uiState by vm.uiState.collectAsState()

                // Intercept hardware/system back button
                BackHandler {
                    val handled = vm.handleBackNavigation()
                    if (!handled) {
                        finish()
                    }
                }

                // If unlock granted successfully, return to target app or finish
                LaunchedEffect(uiState.isUnlockSuccessful) {
                    if (uiState.isUnlockSuccessful) {
                        try {
                            if (blockedPackage.isNotBlank()) {
                                val launchIntent = packageManager.getLaunchIntentForPackage(blockedPackage)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                    startActivity(launchIntent)
                                }
                            }
                        } catch (_: Exception) {
                            // Fail-safe
                        }
                        finish()
                    }
                }

                Crossfade(targetState = uiState.currentStep, label = "interventionCrossfade") { step ->
                    when (step) {
                        InterventionStep.BLOCKED_REEL -> {
                            Screen08BlockedReel(
                                todayReelsCount = uiState.todayReelsCount,
                                onChooseReset = { vm.onChooseReset() },
                                onGoBack = { finish() }
                            )
                        }
                        InterventionStep.RESET_SELECTION,
                        InterventionStep.RESET_IN_PROGRESS -> {
                            Screen09ResetActivity(
                                todayReelsCount = uiState.todayReelsCount,
                                selectedActivity = uiState.selectedActivity,
                                isTimerRunning = uiState.isActivityTimerRunning,
                                remainingSeconds = uiState.activityRemainingSeconds,
                                onSelectActivity = { vm.onSelectActivity(it) },
                                onStartActivity = { vm.onStartSelectedActivity() },
                                onCancelActivity = { vm.onCancelActivityTimer() }
                            )
                        }
                        InterventionStep.AWARENESS_CARDS -> {
                            Screen10AwarenessCards(
                                card = uiState.currentCard,
                                cardIndex = uiState.currentCardIndex,
                                totalCards = uiState.cards.size,
                                isLastCard = uiState.isLastCard,
                                onNextCard = { vm.onNextCard() },
                                onFinishReflection = { vm.onFinishAwarenessCards() }
                            )
                        }
                        InterventionStep.UNLOCK_SELECTION -> {
                            Screen11UnlockSelection(
                                selectedBatch = uiState.selectedBatchAllowance,
                                isSubmitting = uiState.isSubmittingUnlock,
                                errorMessage = uiState.errorMessage,
                                onSelectBatch = { vm.onSelectBatchAllowance(it) },
                                onConfirmUnlock = { vm.onConfirmUnlock() }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_SURFACE_CATEGORY = "extra_surface_category"
        const val EXTRA_START_STEP = "extra_start_step"
    }
}
