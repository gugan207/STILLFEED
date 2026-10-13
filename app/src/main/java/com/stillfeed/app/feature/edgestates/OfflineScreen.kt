package com.stillfeed.app.feature.edgestates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillfeed.app.core.ui.components.StillfeedBottomNavigation
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedNavDestination
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.theme.StillfeedSageGreen
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.awareness.AwarenessViewModel
import com.stillfeed.app.feature.edgestates.model.OfflineUiState

/**
 * Screen 17 — No Internet / Offline State (Figma Node 70:241).
 *
 * Requirements:
 * - Distinguishes network connectivity from local on-device protection.
 * - Does not disable local protection merely because the device is offline.
 * - Claims "STILL PROTECTED" only when local permissions and protection state verify it.
 * - Retry action only triggers verifiable network evaluation. Zero fake APIs.
 */
@Composable
fun OfflineScreen(
    viewModel: OfflineViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage) {
        val msg = uiState.feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
        }
    }

    OfflineContent(
        uiState = uiState,
        onRetry = { viewModel.retryConnection() },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        onNavigateToHome = onNavigateToHome,
        onNavigateToInsights = onNavigateToInsights,
        onNavigateToSettings = onNavigateToSettings
    )
}

@Composable
fun OfflineContent(
    uiState: OfflineUiState,
    onRetry: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StillfeedTheme.colors.backgroundPrimary,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            StillfeedBottomNavigation(
                selectedDestination = StillfeedNavDestination.Home,
                onDestinationSelected = { destination ->
                    when (destination) {
                        StillfeedNavDestination.Home -> onNavigateToHome()
                        StillfeedNavDestination.Insights -> onNavigateToInsights()
                        StillfeedNavDestination.Settings -> onNavigateToSettings()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = StillfeedTheme.spacing.screenMargin)
                .padding(top = StillfeedTheme.spacing.lg, bottom = StillfeedTheme.spacing.xl)
        ) {
            // 1. BRAND HEADER & SECTION
            StillfeedScreenHeader(
                sectionLabel = "OFFLINE",
                stepCounter = "EDGE"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 2. HERO STATEMENT & BODY
            Text(
                text = uiState.headline,
                style = StillfeedTheme.typography.headlineMedium,
                color = StillfeedTheme.colors.textPrimary,
                lineHeight = 38.sp
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            Text(
                text = uiState.body,
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 3. OFFLINE STATUS CARD (Figma Node 70:241)
            StillfeedCard {
                // Honest label: STILL PROTECTED only if local permissions are active
                val protectedLabel = if (uiState.isLocallyProtected) "STILL PROTECTED" else "PROTECTION PAUSED"
                val labelColor = if (uiState.isLocallyProtected) StillfeedSageGreen else StillfeedTheme.colors.statusInactiveText

                Text(
                    text = protectedLabel,
                    style = StillfeedTheme.typography.label,
                    color = labelColor
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                val usageTitle = if (uiState.isReelsCountAvailable && uiState.todayReelsWatched != null) {
                    "${uiState.todayReelsWatched} reels watched today."
                } else {
                    val formatted = AwarenessViewModel.formatMinutes(uiState.todayMonitoredMinutes)
                    "$formatted monitored app time today."
                }

                Text(
                    text = usageTitle,
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

                Text(
                    text = uiState.detailText,
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 4. RETRY ACTION
            StillfeedPrimaryButton(
                text = if (uiState.isRetrying) "Checking connection..." else "Try again",
                onClick = onRetry,
                enabled = !uiState.isRetrying
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // 5. OFFLINE SAFE NOTE
            Text(
                text = uiState.offlineNote,
                style = StillfeedTheme.typography.caption,
                color = StillfeedTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
