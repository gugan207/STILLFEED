package com.stillfeed.app.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedBottomNavigation
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedHeader
import com.stillfeed.app.core.ui.components.StillfeedNavDestination
import com.stillfeed.app.core.ui.theme.StillfeedSageGreen
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 21 — Yearly Report (Figma Node 130:85).
 * Flow 05 — Reports (03 / 03)
 */
@Composable
fun YearlyReportScreen(
    viewModel: ReportsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StillfeedTheme.colors.backgroundPrimary,
        bottomBar = {
            StillfeedBottomNavigation(
                selectedDestination = StillfeedNavDestination.Insights,
                onDestinationSelected = { dest ->
                    when (dest) {
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
            // Header (Node 130:85)
            StillfeedHeader(
                sectionLabel = "REPORTS",
                stepCounter = "03 / 03"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Your year",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "The long-term pattern matters.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Hero Card
            StillfeedCard {
                Text(
                    text = uiState.yearlyReelsWatched?.toString() ?: "―",
                    style = StillfeedTheme.typography.display,
                    color = StillfeedTheme.colors.textPrimary
                )
                Text(
                    text = if (uiState.yearlyReelsWatched != null) "reels watched this year" else "${uiState.yearlyMonitoredHours}h monitored short video",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

                Text(
                    text = "12-month trend",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // 12-Month Bars (Jan - Dec)
                val months = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    months.forEachIndexed { idx, m ->
                        // Sample trend curve reflecting steady reduction
                        val fraction = (0.9f - (idx * 0.04f)).coerceIn(0.2f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (idx == 9) StillfeedSageGreen // October best month
                                        else StillfeedTheme.colors.surfaceVariant
                                    )
                            )
                            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                            Text(
                                text = m,
                                style = StillfeedTheme.typography.caption,
                                color = StillfeedTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // Best Month Card
            StillfeedCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Best month · ${uiState.bestMonthName}",
                        style = StillfeedTheme.typography.titleSmall,
                        color = StillfeedTheme.colors.textPrimary
                    )
                    Text(
                        text = "↓ ${uiState.bestMonthReductionPercentage}% below Jan",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedSageGreen
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                Text(
                    text = "Second-half usage stayed consistently lower than your first half.",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            Text(
                text = "← Back to Insights",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateBack() }
                    .padding(8.dp)
            )
        }
    }
}
