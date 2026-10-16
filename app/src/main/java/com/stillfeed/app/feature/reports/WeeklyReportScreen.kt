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
 * Screen 19 — Weekly Report (Figma Node 130:19).
 * Flow 05 — Reports (01 / 03)
 */
@Composable
fun WeeklyReportScreen(
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
            // Header (Node 130:19)
            StillfeedHeader(
                sectionLabel = "REPORTS",
                stepCounter = "01 / 03"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Your week",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "What changed this week.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Main Hero Card
            StillfeedCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = uiState.weeklyReelsWatched?.toString() ?: "―",
                            style = StillfeedTheme.typography.display,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Text(
                            text = if (uiState.weeklyReelsWatched != null) "reels watched" else "${uiState.weeklyMonitoredMinutes}m monitored",
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }

                    uiState.weeklyDifferencePercentage?.let { diff ->
                        val isLower = diff < 0
                        val text = "${if (isLower) "↓" else "↑"} ${kotlin.math.abs(diff)}% vs last week"
                        Text(
                            text = text,
                            style = StillfeedTheme.typography.bodyMedium,
                            color = if (isLower) StillfeedSageGreen else StillfeedTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

                Text(
                    text = "7-day pattern",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // 7-day pattern chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxUsage = (uiState.weeklyPattern.maxOfOrNull { it.usageMinutes } ?: 1).coerceAtLeast(1)
                    uiState.weeklyPattern.forEach { day ->
                        val heightFraction = (day.usageMinutes.toFloat() / maxUsage).coerceIn(0.12f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (day.isToday) StillfeedTheme.colors.accent
                                        else StillfeedTheme.colors.surfaceVariant
                                    )
                            )
                            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                            Text(
                                text = day.dayLabel,
                                style = StillfeedTheme.typography.caption,
                                color = StillfeedTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // Highlight Rows
            StillfeedCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Best day · ${uiState.bestDayName}",
                        style = StillfeedTheme.typography.titleSmall,
                        color = StillfeedTheme.colors.textPrimary
                    )
                    Text(
                        text = "Lowest usage",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedSageGreen
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Most improved · ${uiState.mostImprovedDayName}",
                        style = StillfeedTheme.typography.titleSmall,
                        color = StillfeedTheme.colors.textPrimary
                    )
                    Text(
                        text = "Calmer pace",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedSageGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Navigation back link
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
