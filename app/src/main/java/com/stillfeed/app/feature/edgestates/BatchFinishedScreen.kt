package com.stillfeed.app.feature.edgestates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillfeed.app.core.ui.components.StillfeedBottomNavigation
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedNavDestination
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.theme.StillfeedSageGreen
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.awareness.AwarenessViewModel
import com.stillfeed.app.feature.edgestates.model.BatchFinishedUiState

/**
 * Screen 15 — Daily Limit / Batch Finished (Figma Node 70:198).
 *
 * Requirements:
 * - Displayed only upon a verified batch-expiration event.
 * - Distinguishes batch expiration from daily allowance and monthly targets.
 * - Does not invent or fabricate watched Reel counts.
 * - Leaves connection to Step 19's unlock-expiration logic for integration stage.
 */
@Composable
fun BatchFinishedScreen(
    viewModel: BatchFinishedViewModel,
    onChooseReset: () -> Unit,
    onKeepChatting: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    BatchFinishedContent(
        uiState = uiState,
        onChooseReset = onChooseReset,
        onKeepChatting = onKeepChatting,
        modifier = modifier,
        onNavigateToHome = onNavigateToHome,
        onNavigateToInsights = onNavigateToInsights,
        onNavigateToSettings = onNavigateToSettings
    )
}

@Composable
fun BatchFinishedContent(
    uiState: BatchFinishedUiState,
    onChooseReset: () -> Unit,
    onKeepChatting: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StillfeedTheme.colors.backgroundPrimary,
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
                sectionLabel = "BATCH FINISHED",
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

            // 3. USAGE SUMMARY CARD (Figma Node 70:198)
            StillfeedCard {
                Text(
                    text = "TODAY",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val heroCountText = if (uiState.isReelsCountAvailable && uiState.todayReelsWatched != null) {
                            "${uiState.todayReelsWatched}"
                        } else {
                            "—"
                        }

                        Text(
                            text = heroCountText,
                            style = StillfeedTheme.typography.display,
                            color = StillfeedTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        val monitoredText = AwarenessViewModel.formatMinutes(uiState.todayMonitoredMinutes)
                        Text(
                            text = "reels watched · $monitoredText monitored app time",
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }

                    Text(
                        text = uiState.mindStateLabel,
                        style = StillfeedTheme.typography.caption,
                        color = StillfeedSageGreen,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // Distinction between batch expiration and daily allowance
                val allowanceText = "Daily allowance: ${uiState.dailyAllowanceReels} reels (monitored in app time)"
                Text(
                    text = allowanceText,
                    style = StillfeedTheme.typography.caption,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 4. CALLS TO ACTION
            StillfeedPrimaryButton(
                text = "Choose a reset",
                onClick = onChooseReset,
                enabled = uiState.isBatchExpired
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            StillfeedSecondaryButton(
                text = "Keep chatting",
                onClick = onKeepChatting
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // 5. CALM FOOTER NOTE
            Text(
                text = uiState.footerNote,
                style = StillfeedTheme.typography.caption,
                color = StillfeedTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
