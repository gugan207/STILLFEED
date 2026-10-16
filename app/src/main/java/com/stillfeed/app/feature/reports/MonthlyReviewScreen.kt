package com.stillfeed.app.feature.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 20 — Monthly Review (Figma Node 130:55).
 * Flow 05 — Reports (02 / 03)
 */
@Composable
fun MonthlyReviewScreen(
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
            // Header (Node 130:55)
            StillfeedHeader(
                sectionLabel = "REPORTS",
                stepCounter = "02 / 03"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Next month",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "Choose a pace that feels realistic.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Monthly Change Card
            StillfeedCard {
                Text(
                    text = "MONTHLY CHANGE",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "${uiState.monthlyReductionSummaryPercentage}% lower",
                    style = StillfeedTheme.typography.display,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "Reel use vs recent pattern",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            Text(
                text = "Pick the pace that feels realistic.",
                style = StillfeedTheme.typography.titleSmall,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            // 4 Pace Options (Figma 130:55)
            val paceOptions = listOf(
                PaceOption("Gentle", 5, 29),
                PaceOption("Balanced", 10, 27),
                PaceOption("Focused", 15, 26),
                PaceOption("Challenge", 20, 24)
            )

            paceOptions.forEach { opt ->
                val isSelected = uiState.selectedMonthlyPacePercentage == opt.pct
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = StillfeedTheme.spacing.xs)
                        .clip(StillfeedTheme.shapes.card)
                        .clickable { viewModel.selectMonthlyPace(opt.pct) },
                    shape = StillfeedTheme.shapes.card,
                    color = if (isSelected) StillfeedTheme.colors.surfaceVariant else StillfeedTheme.colors.surface,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) StillfeedTheme.colors.textPrimary else StillfeedTheme.colors.divider
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = StillfeedTheme.spacing.cardPadding, vertical = StillfeedTheme.spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = opt.name,
                                style = StillfeedTheme.typography.titleSmall,
                                color = StillfeedTheme.colors.textPrimary
                            )
                            Text(
                                text = "${opt.pct}% lower",
                                style = StillfeedTheme.typography.bodySmall,
                                color = StillfeedTheme.colors.textSecondary
                            )
                        }

                        Text(
                            text = "${opt.reels} reels",
                            style = StillfeedTheme.typography.bodyLarge,
                            color = StillfeedTheme.colors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Your limit changes once per month, after review.",
                style = StillfeedTheme.typography.caption,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            StillfeedPrimaryButton(
                text = if (uiState.isMonthlyAllowanceSaved) "Limit updated ✓" else "Use this limit",
                onClick = { viewModel.applyMonthlyLimit() }
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

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

private data class PaceOption(val name: String, val pct: Int, val reels: Int)
