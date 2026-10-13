package com.stillfeed.app.feature.onboarding.steps

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedStatus
import com.stillfeed.app.core.ui.components.StillfeedStatusBadge
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.onboarding.components.OnboardingCalmIllustration

/**
 * Screen 05 — Setup Complete (Figma Node 39:84).
 *
 * Confirms the onboarding completion with the verified daily allowance and actual
 * permission status. Per Step 17 requirements: NEVER claims protection is active
 * when required permissions are missing.
 */
@Composable
fun SetupCompleteStep(
    selectedAllowance: Int,
    isProtectionActive: Boolean,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                sectionLabel = "READY",
                stepCounter = "05 / 05"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                OnboardingCalmIllustration(size = 130.dp)
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Your phone can feel quieter now.",
                style = StillfeedTheme.typography.headlineMedium,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            Text(
                text = "STILLFEED stays in the background. When a Reel is blocked, you get a simple choice instead of a dead end.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Summary Card (Figma 39:84)
            StillfeedCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$selectedAllowance reels",
                            style = StillfeedTheme.typography.titleMedium,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "today’s allowance",
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }

                    Text(
                        text = "06:00–22:00",
                        style = StillfeedTheme.typography.bodyLarge,
                        color = StillfeedTheme.colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // Verified Protection Status Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isProtectionActive) {
                            "Protection is active"
                        } else {
                            "Permissions pending (paused)"
                        },
                        style = StillfeedTheme.typography.bodySmall,
                        color = if (isProtectionActive) {
                            StillfeedTheme.colors.textSecondary
                        } else {
                            StillfeedTheme.colors.destructive
                        }
                    )

                    StillfeedStatusBadge(
                        status = if (isProtectionActive) StillfeedStatus.Protected else StillfeedStatus.NotConnected
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

        StillfeedPrimaryButton(
            text = "Open STILLFEED",
            onClick = onFinish
        )
    }
}
