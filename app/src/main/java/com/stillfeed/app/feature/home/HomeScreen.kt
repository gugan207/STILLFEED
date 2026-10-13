package com.stillfeed.app.feature.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillfeed.app.core.ui.components.StillfeedBottomNavigation
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedNavDestination
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedStatus
import com.stillfeed.app.core.ui.components.StillfeedStatusBadge
import com.stillfeed.app.core.ui.theme.StillfeedHighlightGold
import com.stillfeed.app.core.ui.theme.StillfeedSageGreen
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.awareness.AwarenessViewModel
import com.stillfeed.app.feature.home.components.DaySunCompanion
import com.stillfeed.app.feature.home.components.EveningMoonCompanion
import com.stillfeed.app.feature.home.components.HomeWeeklyRhythmChart
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * STILLFEED Home Dashboard Screen aligning with:
 * - Screen 06 — Home (Day) (Figma Node 39:103)
 * - Screen 07 — Home (Evening) (Figma Node 39:137)
 *
 * Requirements enforced:
 * 1. Automatic time-of-day awareness: Day (06:00 - 17:59) vs Evening (18:00 - 05:59).
 * 2. Visual fidelity: Exact Figma typography, colors, pill badges, and companion graphics.
 * 3. USAGE DATA HONESTY: Monitored time is in minutes. Does not fabricate Reel counts.
 *    Shows em-dash for reel count while showing true monitored minutes and persisted allowance.
 * 4. Context cards: Day shows "A LITTLE CONTEXT" with yesterday comparison and weekly mini-rhythm.
 *    Evening shows "TONIGHT" with calm reflection copy.
 * 5. Navigation: Reset activity CTA establishes contract for Step 19.
 * 6. Protection preservation: Alerts when protection is paused or permissions missing.
 */
@Composable
fun HomeScreen(
    viewModel: AwarenessViewModel,
    onNavigateToAwarenessSelection: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onResetActivity: () -> Unit = {},
    onNavigateToPermissionDisabled: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StillfeedTheme.colors.backgroundPrimary,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
            // 1. BRAND HEADER WITH SECTION LABEL AND STEP INDICATOR
            val isEvening = uiState.isEvening
            StillfeedScreenHeader(
                sectionLabel = if (isEvening) "EVENING" else "TODAY",
                stepCounter = if (isEvening) "02 / 03" else "01 / 03"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 2. HERO USAGE DISPLAY
            // Data contract honesty: displays '—' for watched reels pending dedicated tracking,
            // accompanied by verified monitored app duration.
            val todayFormatted = AwarenessViewModel.formatMinutes(uiState.todayUsageMinutes)
            val heroCountText = if (uiState.isReelsCountAvailable && uiState.watchedReelsToday != null) {
                "${uiState.watchedReelsToday}"
            } else {
                "—"
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = heroCountText,
                    style = StillfeedTheme.typography.superDisplay,
                    color = StillfeedTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

                Text(
                    text = "reels watched today · $todayFormatted monitored app time",
                    style = StillfeedTheme.typography.bodyMedium,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // 3. COMPANION MOOD ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mood capsule pill
                val badgeColor = if (isEvening) StillfeedHighlightGold else StillfeedSageGreen
                val badgeText = if (isEvening) "MAKE A LITTLE SPACE" else "YOUR MIND LOOKS FRESH"

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(badgeColor)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = StillfeedTheme.typography.labelSmall,
                        color = StillfeedTheme.colors.textPrimary
                    )
                }

                // Companion graphic (Sun for Day, Crescent Moon for Evening)
                if (isEvening) {
                    EveningMoonCompanion()
                } else {
                    DaySunCompanion()
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 4. CONTEXT CARD (DAY VS EVENING)
            if (!isEvening) {
                // Day: Screen 06 "A LITTLE CONTEXT" Card
                StillfeedCard {
                    Text(
                        text = "A LITTLE CONTEXT",
                        style = StillfeedTheme.typography.label,
                        color = StillfeedTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                    val diffMinutes = uiState.yesterdayUsageDifferenceMinutes
                    val contextHeadline = when {
                        diffMinutes < 0 -> "You’re using short video\nless than yesterday."
                        diffMinutes > 0 -> "A gentle check-in\non today's rhythm."
                        else -> "Steady attention rhythm\ntoday."
                    }

                    Text(
                        text = contextHeadline,
                        style = StillfeedTheme.typography.titleMedium,
                        color = StillfeedTheme.colors.textPrimary,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                    val yesterdayFormatted = AwarenessViewModel.formatMinutes(uiState.yesterdayUsageMinutes)
                    Text(
                        text = "$todayFormatted today  ·  $yesterdayFormatted yesterday",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

                    // Delta Badge
                    val deltaText = when {
                        diffMinutes < 0 -> "↓ ${AwarenessViewModel.formatMinutes(abs(diffMinutes))} less"
                        diffMinutes > 0 -> "↑ ${AwarenessViewModel.formatMinutes(diffMinutes)} more"
                        else -> "— steady rhythm"
                    }
                    val deltaColor = if (diffMinutes < 0) StillfeedSageGreen else StillfeedTheme.colors.textSecondary

                    Text(
                        text = deltaText,
                        style = StillfeedTheme.typography.bodyLarge,
                        color = deltaColor
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                    Text(
                        text = "Allowance: ${uiState.dailyReelsAllowance} reels · Monitored in app time",
                        style = StillfeedTheme.typography.caption,
                        color = StillfeedTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

                    // Mini-rhythm 7-day visualization
                    HomeWeeklyRhythmChart(items = uiState.weeklyRhythm)
                }
            } else {
                // Evening: Screen 07 "TONIGHT" Card
                StillfeedCard {
                    Text(
                        text = "TONIGHT",
                        style = StillfeedTheme.typography.label,
                        color = StillfeedTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                    Text(
                        text = "You’ve been here\na while.",
                        style = StillfeedTheme.typography.titleMedium,
                        color = StillfeedTheme.colors.textPrimary,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

                    Text(
                        text = "A reset is available whenever you want it.",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                    Text(
                        text = "$todayFormatted monitored app time  ·  Allowance: ${uiState.dailyReelsAllowance} reels",
                        style = StillfeedTheme.typography.caption,
                        color = StillfeedTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

                // Evening Reflection Headline (Figma Node 39:137)
                Text(
                    text = "Keep the rest of your evening yours.",
                    style = StillfeedTheme.typography.bodyLarge,
                    color = StillfeedTheme.colors.textPrimary,
                    modifier = Modifier.padding(horizontal = StillfeedTheme.spacing.xs)
                )
            }

            // 5. PROTECTION STATUS WARNING (Preserve protection visibility)
            if (!uiState.isProtectionEnabled || !uiState.hasUsageStatsPermission) {
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))
                StillfeedCard(
                    modifier = Modifier.clickable { onNavigateToPermissionDisabled() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Protection Paused",
                                style = StillfeedTheme.typography.titleSmall,
                                color = StillfeedTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap to review missing permissions and resume monitoring.",
                                style = StillfeedTheme.typography.bodySmall,
                                color = StillfeedTheme.colors.textSecondary
                            )
                        }
                        StillfeedStatusBadge(status = StillfeedStatus.NotConnected)
                    }
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 6. RESET ACTIVITY CTA (Figma Node 39:103 & 39:137)
            StillfeedPrimaryButton(
                text = "Reset activity",
                onClick = onResetActivity
            )
        }
    }
}
