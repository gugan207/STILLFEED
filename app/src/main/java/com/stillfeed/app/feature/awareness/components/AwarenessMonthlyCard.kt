package com.stillfeed.app.feature.awareness.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import com.stillfeed.app.feature.awareness.AwarenessUiState
import com.stillfeed.app.feature.awareness.AwarenessViewModel

/**
 * Reusable Monthly Awareness Card embedded within the Home dashboard (Figma Node 160:35).
 *
 * PRODUCT CONSTRAINTS:
 * - NEVER score out of 100.
 * - NEVER use shame, guilt, failure, or "lazy" messaging.
 * - Integer-safe minute formatting without Composable-level recalculations.
 * - Calm, objective information hierarchy.
 */
@Composable
fun AwarenessMonthlyCard(
    uiState: AwarenessUiState,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibilityCardDescription = when (uiState.status) {
        MonthlyAwarenessStatus.BASELINE_FORMING ->
            "Monthly awareness card. Baseline is forming. Tap to view awareness settings."
        MonthlyAwarenessStatus.DATA_UNAVAILABLE ->
            "Monthly awareness card. Usage data is unavailable. Tap to view awareness settings."
        else ->
            "Monthly awareness card. Target reduction ${uiState.targetReductionPercentage} percent. " +
            "Current progress ${AwarenessViewModel.formatMinutes(uiState.currentProgressMinutes)} of ${AwarenessViewModel.formatMinutes(uiState.targetUsageMinutes)}. " +
            "Status: ${uiState.status.name.replace('_', ' ').lowercase()}. Tap to view details."
    }

    StillfeedCard(
        modifier = modifier
            .semantics {
                contentDescription = accessibilityCardDescription
            }
            .clickable(
                role = Role.Button,
                onClick = onCardClick
            )
    ) {
        // Top Row: Section Label + Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MONTHLY AWARENESS",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textSecondary
            )

            AwarenessStatusBadge(status = uiState.status)
        }

        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

        when (uiState.status) {
            MonthlyAwarenessStatus.BASELINE_FORMING -> {
                Text(
                    text = "Establishing your baseline",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "A minimum of 7 full days of usage data is needed to build a calm, accurate monthly baseline.",
                    style = StillfeedTheme.typography.bodyMedium,
                    color = StillfeedTheme.colors.textSecondary
                )
            }
            MonthlyAwarenessStatus.DATA_UNAVAILABLE -> {
                Text(
                    text = "Usage data unavailable",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = if (!uiState.hasUsageStatsPermission) {
                        "Usage access permission is required to analyze background app time."
                    } else {
                        "No usage events recorded for this calendar month yet."
                    },
                    style = StillfeedTheme.typography.bodyMedium,
                    color = StillfeedTheme.colors.textSecondary
                )
            }
            else -> {
                // Active Awareness State (ON_TRACK, REDUCING, TARGET_REACHED)
                val targetStr = AwarenessViewModel.formatMinutes(uiState.targetUsageMinutes)
                val progressStr = AwarenessViewModel.formatMinutes(uiState.currentProgressMinutes)
                val baselineStr = AwarenessViewModel.formatMinutes(uiState.baselineMinutes)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = progressStr,
                            style = StillfeedTheme.typography.headlineMedium,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Text(
                            text = "of $targetStr monthly target",
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }

                    if (uiState.targetReductionPercentage > 0) {
                        Text(
                            text = "-${uiState.targetReductionPercentage}% TARGET",
                            style = StillfeedTheme.typography.label,
                            color = StillfeedTheme.colors.accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // Calm Linear Progress Track (no score rings, no gamification)
                val progressRatio = if (uiState.targetUsageMinutes > 0) {
                    (uiState.currentProgressMinutes.toFloat() / uiState.targetUsageMinutes.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(StillfeedTheme.colors.divider)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressRatio)
                            .height(6.dp)
                            .background(StillfeedTheme.colors.accent)
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                // Explanatory note
                val detailText = if (uiState.targetReductionPercentage > 0 && uiState.baselineMinutes > 0) {
                    "Baseline: $baselineStr · Planned reduction: ${AwarenessViewModel.formatMinutes(uiState.reductionAmountMinutes)}"
                } else {
                    "Baseline: $baselineStr"
                }

                Text(
                    text = detailText,
                    style = StillfeedTheme.typography.caption,
                    color = StillfeedTheme.colors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

        // Tap hint footer
        Text(
            text = if (uiState.isLockedForMonth) "Target locked for this month · Tap to view" else "Tap to adjust monthly awareness target",
            style = StillfeedTheme.typography.caption,
            color = StillfeedTheme.colors.textSecondary
        )
    }
}
