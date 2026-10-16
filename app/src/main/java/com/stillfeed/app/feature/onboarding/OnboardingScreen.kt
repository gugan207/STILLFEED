package com.stillfeed.app.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.onboarding.steps.DailyAllowanceStep
import com.stillfeed.app.feature.onboarding.steps.PermissionsStep
import com.stillfeed.app.feature.onboarding.steps.SetupCompleteStep
import com.stillfeed.app.feature.onboarding.steps.WelcomeStep
import com.stillfeed.app.feature.onboarding.steps.WhatStillfeedBlocksStep

/**
 * Top-level Onboarding container coordinating the 5-stage setup sequence (Flow 01, Node 99:220).
 *
 * Sequence:
 * 0. Welcome (Node 39:2)
 * 1. What STILLFEED Blocks (Node 39:23)
 * 2. Permissions (Node 39:41)
 * 3. Daily Allowance (Node 39:64)
 * 4. Setup Complete (Node 39:84)
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-check system permissions whenever the screen resumes (e.g. returning from Android Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Intercept back presses: step back through onboarding stages before exiting
    BackHandler(enabled = uiState.currentStep > 0) {
        viewModel.previousStep()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StillfeedTheme.colors.backgroundPrimary)
            .systemBarsPadding()
    ) {
        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            label = "OnboardingStepTransition"
        ) { step ->
            when (step) {
                0 -> {
                    WelcomeStep(
                        onContinue = { viewModel.nextStep() }
                    )
                }
                1 -> {
                    WhatStillfeedBlocksStep(
                        onContinue = { viewModel.nextStep() }
                    )
                }
                2 -> {
                    PermissionsStep(
                        isAccessibilityGranted = uiState.isAccessibilityPermissionGranted,
                        isUsageAccessGranted = uiState.isUsageAccessPermissionGranted,
                        onContinue = { viewModel.nextStep() }
                    )
                }
                3 -> {
                    DailyAllowanceStep(
                        selectedAllowance = uiState.selectedDailyReelsAllowance,
                        onSelectAllowance = { allowance -> viewModel.selectAllowance(allowance) },
                        onContinue = { viewModel.nextStep() }
                    )
                }
                4 -> {
                    SetupCompleteStep(
                        selectedAllowance = uiState.selectedDailyReelsAllowance,
                        isProtectionActive = uiState.isProtectionActive,
                        onFinish = {
                            viewModel.completeOnboarding(onSuccess = onNavigateToHome)
                        }
                    )
                }
            }
        }
    }
}
