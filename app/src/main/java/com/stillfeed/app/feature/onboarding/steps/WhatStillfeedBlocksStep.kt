package com.stillfeed.app.feature.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedTagBadge
import com.stillfeed.app.core.ui.components.StillfeedTagType
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 02 — What STILLFEED Blocks (Figma Node 39:23).
 * Explains which short-form content is protected (Reels & Shorts)
 * and which core features remain accessible (DMs, Stories, normal videos).
 */
@Composable
fun WhatStillfeedBlocksStep(
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
                sectionLabel = "WHY IT EXISTS",
                stepCounter = "02 / 05"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.section))

            Text(
                text = "Short video is the thing we’re changing.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // BLOCKED Section
            StillfeedTagBadge(
                text = "BLOCKED",
                type = StillfeedTagType.Blocked
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            StillfeedCard {
                Text(
                    text = "Reels",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Shorts · endless feeds · autoplay",
                    style = StillfeedTheme.typography.bodyMedium,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // KEPT Section
            StillfeedTagBadge(
                text = "KEPT",
                type = StillfeedTagType.Kept
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            StillfeedCard {
                Text(
                    text = "DMs + Stories",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Keep the social parts you came for.",
                    style = StillfeedTheme.typography.bodyMedium,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            Text(
                text = "Even when a Reel opens from a DM, STILLFEED keeps the boundary.",
                style = StillfeedTheme.typography.bodyLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            Text(
                text = "Normal YouTube videos, searches, and direct conversations stay available so you never lose connection.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

        StillfeedPrimaryButton(
            text = "CONTINUE",
            onClick = onContinue
        )
    }
}
