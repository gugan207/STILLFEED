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
import com.stillfeed.app.core.ui.theme.StillfeedSageGreen
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.awareness.AwarenessViewModel
import com.stillfeed.app.feature.edgestates.model.FirstDayUiState

/**
 * Screen 18 — First Day / Empty State (Figma Node 70:261).
 *
 * Requirements:
 * - Supports a genuine new-user or no-history state.
 * - Explains what will appear as reliable usage data becomes available.
 * - Does not fabricate daily or historical Reel counts.
 * - Does not interpret missing data as a verified zero.
 * - Distinguishes unavailable Reel-count metrics from valid monitored-time metrics.
 * - Embeddable via [FirstDayEmptyState] into InsightsScreen without modifying it in Step 20A.
 */
@Composable
fun FirstDayScreen(
    viewModel: FirstDayViewModel,
    onOpenStillfeed: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    FirstDayContent(
        uiState = uiState,
        onOpenStillfeed = onOpenStillfeed,
        modifier = modifier,
        onNavigateToHome = onNavigateToHome,
        onNavigateToInsights = onNavigateToInsights,
        onNavigateToSettings = onNavigateToSettings
    )
}

@Composable
fun FirstDayContent(
    uiState: FirstDayUiState,
    onOpenStillfeed: () -> Unit,
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
                selectedDestination = StillfeedNavDestination.Insights,
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
                sectionLabel = "FIRST DAY",
                stepCounter = "NEW"
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

            // 3. FIRST DAY WELCOME CARD COMPONENT
            FirstDayEmptyStateCard(uiState = uiState)

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 4. PRIMARY CTA
            StillfeedPrimaryButton(
                text = "Open STILLFEED",
                onClick = onOpenStillfeed
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // 5. FIRST DAY REFERENCE NOTE
            Text(
                text = uiState.referenceNote,
                style = StillfeedTheme.typography.caption,
                color = StillfeedTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Embeddable Welcome/First-Day card component (Figma Node 70:261).
 *
 * Designed to be embedded directly into InsightsScreen or HomeScreen when
 * no historical usage data exists yet.
 */
@Composable
fun FirstDayEmptyStateCard(
    uiState: FirstDayUiState,
    modifier: Modifier = Modifier
) {
    StillfeedCard(modifier = modifier) {
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
                // Honest numeric representation:
                // Show "0" only when a reliable source confirms zero Reels.
                // When Reel-count tracking is unavailable, display "—".
                val heroNumber = if (uiState.isReelsCountAvailable && uiState.todayReelsWatched != null) {
                    "${uiState.todayReelsWatched}"
                } else {
                    "—"
                }

                Text(
                    text = heroNumber,
                    style = StillfeedTheme.typography.superDisplay,
                    color = StillfeedTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                val subtitleText = when {
                    uiState.isReelsCountAvailable -> "reels watched so far"
                    !uiState.isUsageAccessGranted -> "reels watched so far · Usage access needed to monitor time"
                    !uiState.isUsageDataAvailable || uiState.todayMonitoredMinutes == null -> "reels watched so far · Monitored time unavailable"
                    else -> {
                        val formatted = AwarenessViewModel.formatMinutes(uiState.todayMonitoredMinutes)
                        "reels watched so far · $formatted monitored app time"
                    }
                }

                Text(
                    text = subtitleText,
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
    }
}
