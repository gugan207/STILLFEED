package com.stillfeed.app.feature.onboarding.steps

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.onboarding.components.OnboardingCalmIllustration

/**
 * Screen 01 — Welcome (Figma Node 39:2).
 * Introduces STILLFEED and its core purpose with a calm, editorial aesthetic.
 */
@Composable
fun WelcomeStep(
    onContinue: () -> Unit,
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
                sectionLabel = "GET STARTED",
                stepCounter = "01 / 05"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.section))

            Text(
                text = "YOUR ATTENTION IS STILL YOURS",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            Text(
                text = "A calmer way to use your phone.",
                style = StillfeedTheme.typography.headlineLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "STILLFEED blocks endless short-form video while keeping the parts you actually came for.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.section))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                OnboardingCalmIllustration(size = 140.dp)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StillfeedPrimaryButton(
                text = "START WITH THESE",
                onClick = onContinue
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = StillfeedTheme.colors.accent,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(StillfeedTheme.spacing.sm))
                Text(
                    text = "You can change everything later.",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
            }
        }
    }
}
