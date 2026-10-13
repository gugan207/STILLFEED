package com.stillfeed.app.feature.onboarding.steps

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 04 — Daily Allowance (Figma Node 39:64).
 *
 * Configures the initial daily Reels allowance (default: 30 reels).
 * Clearly distinguishes integer Reels count from monitored minutes.
 */
@Composable
fun DailyAllowanceStep(
    selectedAllowance: Int,
    onSelectAllowance: (Int) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tierLabel = when (selectedAllowance) {
        in 1..15 -> "Mindful"
        in 16..25 -> "Focused"
        in 26..35 -> "Balanced"
        else -> "Gentle"
    }

    val presetAllowances = listOf(15, 20, 30, 45)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StillfeedTheme.spacing.screenMargin)
            .padding(top = StillfeedTheme.spacing.md, bottom = StillfeedTheme.spacing.xl),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            StillfeedScreenHeader(
                sectionLabel = "SET YOUR DEFAULT",
                stepCounter = "04 / 05"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.section))

            Text(
                text = "Choose your starting limit.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            Text(
                text = "Your awareness limit stays fixed for this month. At the monthly review, you can choose a lower limit.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Hero Allowance Display (Figma 39:64)
            Text(
                text = "TODAY",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedAllowance reels",
                    style = StillfeedTheme.typography.display,
                    color = StillfeedTheme.colors.textPrimary
                )

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = StillfeedTheme.colors.surface,
                    border = BorderStroke(
                        width = StillfeedTheme.spacing.dividerThickness,
                        color = StillfeedTheme.colors.divider
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tierLabel,
                            style = StillfeedTheme.typography.bodyLarge,
                            color = StillfeedTheme.colors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            // Presets selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetAllowances.forEach { allowance ->
                    val isSelected = allowance == selectedAllowance
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectAllowance(allowance) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) StillfeedTheme.colors.buttonPrimaryBackground else StillfeedTheme.colors.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) StillfeedTheme.colors.buttonPrimaryBackground else StillfeedTheme.colors.divider
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$allowance",
                                style = StillfeedTheme.typography.bodyLarge,
                                color = if (isSelected) StillfeedTheme.colors.buttonPrimaryText else StillfeedTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // THIS MONTH Card
            StillfeedCard {
                Text(
                    text = "THIS MONTH",
                    style = StillfeedTheme.typography.bodyLarge,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$selectedAllowance reels",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fixed for this month.",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            // MONTHLY REVIEW Card
            StillfeedCard {
                Text(
                    text = "MONTHLY REVIEW",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose again",
                    style = StillfeedTheme.typography.titleLarge,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Review your progress before changing the limit.",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

        StillfeedPrimaryButton(
            text = "Use this limit",
            onClick = onContinue
        )
    }
}
