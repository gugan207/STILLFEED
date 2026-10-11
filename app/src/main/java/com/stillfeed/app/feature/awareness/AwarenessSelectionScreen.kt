package com.stillfeed.app.feature.awareness

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedHeader
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.domain.model.AwarenessPercentage
import com.stillfeed.app.feature.awareness.components.AwarenessStatusBadge

/**
 * Monthly Awareness Target Selection Screen (Figma Screen 10 / Node 180:45).
 *
 * PRODUCT CONSTRAINTS:
 * - Supported choices are strictly 5%, 10%, 15%, 20%.
 * - Fixed for the entire calendar month once confirmed.
 * - Non-judgmental, calm copy without shame or pressure.
 * - Standard 390x844 geometry with 28dp screen margins.
 */
@Composable
fun AwarenessSelectionScreen(
    viewModel: AwarenessViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val options = listOf(
        AwarenessOption(
            percentage = 5,
            title = "5% Reduction",
            subtitle = "Gentle adjustment · Light, steady reduction"
        ),
        AwarenessOption(
            percentage = 10,
            title = "10% Reduction",
            subtitle = "Moderate adjustment · Balanced, sustainable shift"
        ),
        AwarenessOption(
            percentage = 15,
            title = "15% Reduction",
            subtitle = "Focused adjustment · Meaningful reclaim of attention"
        ),
        AwarenessOption(
            percentage = 20,
            title = "20% Reduction",
            subtitle = "Deep adjustment · Substantial reduction in monitored apps"
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StillfeedTheme.colors.backgroundPrimary
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = StillfeedTheme.spacing.screenMargin)
                .padding(top = StillfeedTheme.spacing.lg, bottom = StillfeedTheme.spacing.xl)
        ) {
            // Header Hierarchy: Brand wordmark -> Section Label -> Step Indicator
            StillfeedHeader(
                sectionLabel = "MONTHLY AWARENESS",
                stepCounter = if (uiState.isLockedForMonth) "LOCKED" else "CHOOSE TARGET"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Choose your awareness target.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "Pick a reduction from your baseline. To avoid constant tracking anxiety, your chosen target remains steady for the entire month.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // Current Status Banner
            StillfeedCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT STATUS",
                            style = StillfeedTheme.typography.label,
                            color = StillfeedTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.baselineMinutes > 0) {
                                "Baseline: ${AwarenessViewModel.formatMinutes(uiState.baselineMinutes)}"
                            } else {
                                "Baseline forming (>=7 days needed)"
                            },
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                    }
                    AwarenessStatusBadge(status = uiState.status)
                }

                if (uiState.isLockedForMonth) {
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))
                    Text(
                        text = "Your target is locked at ${uiState.targetReductionPercentage}% for the remainder of this month.",
                        style = StillfeedTheme.typography.caption,
                        color = StillfeedTheme.colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // 4 Selectable Percentage Cards (5%, 10%, 15%, 20%)
            options.forEach { option ->
                val isSelected = if (uiState.isLockedForMonth) {
                    uiState.targetReductionPercentage == option.percentage
                } else {
                    uiState.userSelectedPercentage == option.percentage
                }

                val calculatedSavingMinutes = if (uiState.baselineMinutes > 0) {
                    (uiState.baselineMinutes.toLong() * option.percentage / 100L).toInt()
                } else 0

                val cardSemanticDescription = "${option.title}, ${option.subtitle}. " +
                        (if (calculatedSavingMinutes > 0) "Estimated saving ${AwarenessViewModel.formatMinutes(calculatedSavingMinutes)}. " else "") +
                        (if (isSelected) "Selected." else "Not selected.")

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = StillfeedTheme.spacing.xs)
                        .semantics { contentDescription = cardSemanticDescription }
                        .selectable(
                            selected = isSelected,
                            enabled = !uiState.isLockedForMonth,
                            role = Role.RadioButton,
                            onClick = {
                                viewModel.onSelectPercentageOption(option.percentage)
                            }
                        ),
                    shape = StillfeedTheme.shapes.card,
                    color = if (isSelected) StillfeedTheme.colors.surface else StillfeedTheme.colors.backgroundSecondary,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else StillfeedTheme.spacing.dividerThickness,
                        color = if (isSelected) StillfeedTheme.colors.textPrimary else StillfeedTheme.colors.divider
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(StillfeedTheme.spacing.cardPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${option.percentage}%",
                                style = StillfeedTheme.typography.titleMedium,
                                color = StillfeedTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xxs))
                            Text(
                                text = option.subtitle,
                                style = StillfeedTheme.typography.bodySmall,
                                color = StillfeedTheme.colors.textSecondary
                            )
                            if (calculatedSavingMinutes > 0) {
                                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                                Text(
                                    text = "Target reduction: ~${AwarenessViewModel.formatMinutes(calculatedSavingMinutes)}/month",
                                    style = StillfeedTheme.typography.caption,
                                    color = StillfeedTheme.colors.accent
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Confirmation / Action Buttons
            if (!uiState.isLockedForMonth) {
                val isSelectionValid = AwarenessPercentage.isValid(uiState.userSelectedPercentage)
                StillfeedPrimaryButton(
                    text = if (isSelectionValid) "CONFIRM ${uiState.userSelectedPercentage}% TARGET" else "SELECT A TARGET",
                    enabled = isSelectionValid && !uiState.isLoading,
                    onClick = {
                        viewModel.confirmPercentageSelection(onSuccess = onNavigateBack)
                    }
                )
            } else {
                StillfeedPrimaryButton(
                    text = "TARGET LOCKED FOR THIS MONTH",
                    enabled = false,
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            StillfeedSecondaryButton(
                text = "RETURN TO HOME",
                onClick = onNavigateBack
            )
        }
    }
}

private data class AwarenessOption(
    val percentage: Int,
    val title: String,
    val subtitle: String
)
