package com.stillfeed.app.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.awareness.AwarenessViewModel
import com.stillfeed.app.feature.awareness.components.AwarenessStatusBadge
import com.stillfeed.app.feature.insights.components.DailyRhythmChart

/**
 * STILLFEED Insights Screen (Figma Screen 12 / Node 70:113 and Screen 18 / Node 70:261).
 *
 * PRODUCT CONSTRAINTS:
 * - Strictly zero scores out of 100.
 * - Strictly zero gamification or shame/guilt messaging.
 * - Calm, objective historical usage visibility only.
 */
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToWeeklyReport: () -> Unit = {},
    onNavigateToMonthlyReview: () -> Unit = {},
    onNavigateToYearlyReport: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StillfeedTheme.colors.backgroundPrimary,
        bottomBar = {
            StillfeedBottomNavigation(
                selectedDestination = StillfeedNavDestination.Insights,
                onDestinationSelected = { destination ->
                    when (destination) {
                        StillfeedNavDestination.Home -> onNavigateToHome()
                        StillfeedNavDestination.Insights -> { /* Already on Insights */ }
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
            if (uiState.isFirstDayOrEmpty) {
                // SCREEN 18 — FIRST DAY / EMPTY STATE (Node 70:261)
                StillfeedHeader(
                    sectionLabel = "FIRST DAY",
                    stepCounter = "NEW"
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

                Text(
                    text = "Start with noticing.",
                    style = StillfeedTheme.typography.titleLarge,
                    color = StillfeedTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

                Text(
                    text = if (!uiState.hasUsageStatsPermission) {
                        "Usage access permission is required to analyze your rhythm without judging it."
                    } else {
                        "There’s no baseline yet. SKILLFEED will learn your rhythm without judging it."
                    },
                    style = StillfeedTheme.typography.bodyMedium,
                    color = StillfeedTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

                // Welcome Card (Node 70:267)
                StillfeedCard {
                    Text(
                        text = "TODAY",
                        style = StillfeedTheme.typography.label,
                        color = StillfeedTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

                    Text(
                        text = AwarenessViewModel.formatMinutes(uiState.todayMinutes),
                        style = StillfeedTheme.typography.display,
                        color = StillfeedTheme.colors.textPrimary
                    )

                    Text(
                        text = "reels watched so far",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                    Text(
                        text = if (uiState.todayMinutes == 0) "Your mind looks fresh." else "First day observation active.",
                        style = StillfeedTheme.typography.bodyMedium,
                        color = StillfeedTheme.colors.accent
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

                // Primary CTA (Figma Node 70:272 - 70:273: "Open SKILLFEED")
                StillfeedPrimaryButton(
                    text = "Open SKILLFEED",
                    onClick = onNavigateToHome
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // First Day Note (Figma Node 70:274)
                Text(
                    text = "Your first week becomes your reference point.",
                    style = StillfeedTheme.typography.caption,
                    color = StillfeedTheme.colors.textSecondary
                )
            } else {
                // SCREEN 12 — ACTIVE INSIGHTS EXPERIENCE (Node 70:113)
                StillfeedHeader(
                    sectionLabel = "INSIGHTS",
                    stepCounter = "02 / 03"
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

                Text(
                    text = "Your week in short video",
                    style = StillfeedTheme.typography.titleLarge,
                    color = StillfeedTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

                Text(
                    text = "A quieter feed gives your attention more room.",
                    style = StillfeedTheme.typography.bodyMedium,
                    color = StillfeedTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

                // 1. WEEKLY SUMMARY CARD WITH 7-DAY RHYTHM CHART (Node 70:119)
                StillfeedCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "THIS WEEK",
                                style = StillfeedTheme.typography.label,
                                color = StillfeedTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                            Text(
                                text = AwarenessViewModel.formatMinutes(uiState.weeklyMonitoredMinutes),
                                style = StillfeedTheme.typography.display,
                                color = StillfeedTheme.colors.textPrimary
                            )
                            Text(
                                text = "monitored short video",
                                style = StillfeedTheme.typography.bodySmall,
                                color = StillfeedTheme.colors.textSecondary
                            )
                        }

                        // Week-over-week difference indicator
                        if (uiState.previousWeeklyMinutes > 0) {
                            val isReduced = uiState.weeklyDifferenceMinutes < 0
                            val absDiff = if (uiState.weeklyDifferenceMinutes < 0) -uiState.weeklyDifferenceMinutes else uiState.weeklyDifferenceMinutes
                            val arrow = if (isReduced) "↓" else "↑"
                            val percentage = uiState.weeklyDifferencePercentage
                            val diffText = if (percentage != null) {
                                val absPct = if (percentage < 0) -percentage else percentage
                                "$arrow $absPct% vs last week"
                            } else {
                                "$arrow ${AwarenessViewModel.formatMinutes(absDiff)} from last week"
                            }

                            Text(
                                text = diffText,
                                style = StillfeedTheme.typography.bodySmall,
                                color = if (isReduced) StillfeedTheme.colors.accent else StillfeedTheme.colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))
                    StillfeedDivider()
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                    // Embedded 7-Day Daily Rhythm Chart
                    DailyRhythmChart(items = uiState.dailyRhythm)
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

                // 2. MONITORED APP BREAKDOWN CARD
                Text(
                    text = "BREAKDOWN & INTERVENTIONS",
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
                            Text(
                                text = "Reels monitored",
                                style = StillfeedTheme.typography.bodySmall,
                                color = StillfeedTheme.colors.textSecondary
                            )
                        }
                        Text(
                            text = AwarenessViewModel.formatMinutes(uiState.instagramWeeklyMinutes),
                            style = StillfeedTheme.typography.titleMedium,
                            color = StillfeedTheme.colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))
                    StillfeedDivider()
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

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
                            Text(
                                text = "Shorts monitored",
                                style = StillfeedTheme.typography.bodySmall,
                                color = StillfeedTheme.colors.textSecondary
                            )
                        }
                        Text(
                            text = AwarenessViewModel.formatMinutes(uiState.youtubeWeeklyMinutes),
                            style = StillfeedTheme.typography.titleMedium,
                            color = StillfeedTheme.colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))
                    StillfeedDivider()
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                    // Blocked Interventions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Interventions",
                                style = StillfeedTheme.typography.titleSmall,
                                color = StillfeedTheme.colors.textPrimary
                            )
                            Text(
                                text = "Reels & Shorts blocked",
                                style = StillfeedTheme.typography.bodySmall,
                                color = StillfeedTheme.colors.textSecondary
                            )
                        }
                        Text(
                            text = "${uiState.blockedInterventionCount} blocked",
                            style = StillfeedTheme.typography.titleMedium,
                            color = StillfeedTheme.colors.accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

                // 3. CALM REFLECTION / AWARENESS CARD (Node 70:139)
                StillfeedCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AWARENESS CONTEXT",
                            style = StillfeedTheme.typography.label,
                            color = StillfeedTheme.colors.textSecondary
                        )

                        uiState.monthlyAwarenessState?.let { awareness ->
                            AwarenessStatusBadge(status = awareness.status)
                        }
                    }

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                    Text(
                        text = uiState.reflectionHeadline,
                        style = StillfeedTheme.typography.bodyLarge,
                        color = StillfeedTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xxs))

                    Text(
                        text = uiState.reflectionDetail,
                        style = StillfeedTheme.typography.bodyMedium,
                        color = StillfeedTheme.colors.textSecondary
                    )

                    uiState.monthlyAwarenessState?.let { awareness ->
                        if (awareness.targetReductionPercentage > 0 && awareness.baselineMinutes > 0) {
                            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))
                            Text(
                                text = "Target: ${AwarenessViewModel.formatMinutes(awareness.targetUsageMinutes)} · Baseline: ${AwarenessViewModel.formatMinutes(awareness.baselineMinutes)} (-${awareness.targetReductionPercentage}%)",
                                style = StillfeedTheme.typography.caption,
                                color = StillfeedTheme.colors.textMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

                // 4. DETAILED REPORTS (Screens 19, 20, 21)
                Text(
                    text = "REPORTS & REVIEWS",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                StillfeedCard(
                    modifier = Modifier.clickable { onNavigateToWeeklyReport() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Weekly Report",
                                style = StillfeedTheme.typography.titleSmall,
                                color = StillfeedTheme.colors.textPrimary
                            )
                            Text(
                                text = "7-day pattern, best day, and changes",
                                style = StillfeedTheme.typography.bodySmall,
                                color = StillfeedTheme.colors.textSecondary
                            )
                        }
                        Text(
                            text = "›",
                            style = StillfeedTheme.typography.titleMedium,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                StillfeedCard(
                    modifier = Modifier.clickable { onNavigateToMonthlyReview() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly Review",
                                style = StillfeedTheme.typography.titleSmall,
                                color = StillfeedTheme.colors.textPrimary
                            )
                            Text(
                                text = "Evaluate rhythm and adjust allowance pace",
                                style = StillfeedTheme.typography.bodySmall,
                                color = StillfeedTheme.colors.textSecondary
                            )
                        }
                        Text(
                            text = "›",
                            style = StillfeedTheme.typography.titleMedium,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                StillfeedCard(
                    modifier = Modifier.clickable { onNavigateToYearlyReport() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Yearly Report",
                                style = StillfeedTheme.typography.titleSmall,
                                color = StillfeedTheme.colors.textPrimary
                            )
                            Text(
                                text = "12-month long-term pattern and trend",
                                style = StillfeedTheme.typography.bodySmall,
                                color = StillfeedTheme.colors.textSecondary
                            )
                        }
                        Text(
                            text = "›",
                            style = StillfeedTheme.typography.titleMedium,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
