package com.stillfeed.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedBottomNavigation
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedDivider
import com.stillfeed.app.core.ui.components.StillfeedHeader
import com.stillfeed.app.core.ui.components.StillfeedNavDestination
import com.stillfeed.app.core.ui.components.StillfeedStatus
import com.stillfeed.app.core.ui.components.StillfeedStatusBadge
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.awareness.AwarenessViewModel
import com.stillfeed.app.feature.awareness.components.AwarenessMonthlyCard

/**
 * STILLFEED Home Dashboard Screen (Figma Screen 05 / Node 160:12).
 *
 * Visual source of truth:
 * - 390x844dp geometry with 28dp screen margins.
 * - Header: STILLFEED brand wordmark + "TODAY" section label.
 * - Hero display counter: Today's monitored usage minutes (SuperDisplay 86sp).
 * - Protected Apps card: Instagram (Reels blocked) and YouTube (Shorts blocked).
 * - Monthly Awareness card: Embedding [AwarenessMonthlyCard] (Node 160:35).
 * - Bottom Navigation: 3 equal zones (Home, Insights, Settings).
 *
 * PRODUCT CONSTRAINTS:
 * - Strictly zero scores out of 100.
 * - Strictly zero shame, guilt, failure, or "lazy" messaging.
 * - Calm, objective information hierarchy.
 */
@Composable
fun HomeScreen(
    viewModel: AwarenessViewModel,
    onNavigateToAwarenessSelection: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StillfeedTheme.colors.backgroundPrimary,
        bottomBar = {
            StillfeedBottomNavigation(
                selectedDestination = StillfeedNavDestination.Home,
                onDestinationSelected = { destination ->
                    when (destination) {
                        StillfeedNavDestination.Home -> { /* Already on Home */ }
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
            // 1. BRAND HEADER
            StillfeedHeader(
                sectionLabel = "TODAY"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // 2. HERO TODAY USAGE DISPLAY
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "MONITORED USAGE TODAY",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

                val todayFormatted = AwarenessViewModel.formatMinutes(uiState.todayUsageMinutes)
                Text(
                    text = todayFormatted,
                    style = StillfeedTheme.typography.superDisplay,
                    color = StillfeedTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 3. PROTECTED APPS CARD
            Text(
                text = "PROTECTED APPS",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            StillfeedCard {
                // Instagram Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Instagram",
                            style = StillfeedTheme.typography.titleSmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Reels blocked · DMs & Stories kept",
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }

                    StillfeedStatusBadge(
                        status = if (uiState.isProtectionEnabled) StillfeedStatus.Protected else StillfeedStatus.NotConnected
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))
                StillfeedDivider()
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // YouTube Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "YouTube",
                            style = StillfeedTheme.typography.titleSmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Shorts blocked · Videos kept",
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }

                    StillfeedStatusBadge(
                        status = if (uiState.isProtectionEnabled) StillfeedStatus.Protected else StillfeedStatus.NotConnected
                    )
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 4. MONTHLY AWARENESS EMBEDDED CARD (Node 160:35)
            AwarenessMonthlyCard(
                uiState = uiState,
                onCardClick = onNavigateToAwarenessSelection
            )
        }
    }
}
