package com.stillfeed.app.protection.intervention.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedHeader
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 14 — Reel from DM Blocked (Figma Node 70:184).
 *
 * Dedicated intervention shown when short-form video is opened from an Instagram Direct Message.
 * Reassures the user that DMs and Stories remain accessible while protecting against accidental
 * detours into endless short-form feeds.
 */
@Composable
fun Screen14DmReelBlocked(
    onBackToMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StillfeedTheme.colors.backgroundPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StillfeedTheme.spacing.screenMargin)
            .padding(top = StillfeedTheme.spacing.lg, bottom = StillfeedTheme.spacing.xl),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header (Node 70:186 - 70:187)
            StillfeedHeader(
                sectionLabel = "FROM A MESSAGE",
                stepCounter = "EDGE"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // Hero Headline (Node 70:188)
            Text(
                text = "This reel is still paused.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            // Body Explainer (Node 70:189)
            Text(
                text = "You can keep chatting and viewing Stories. Short video stays blocked, even when it arrives through a DM.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Block Card (Node 70:190 - 70:194)
            StillfeedCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause Badge (Node 70:191 - 70:192)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StillfeedTheme.colors.tagBlockedBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "❚❚",
                            style = StillfeedTheme.typography.caption,
                            color = StillfeedTheme.colors.tagBlockedText
                        )
                    }

                    Spacer(modifier = Modifier.size(StillfeedTheme.spacing.md))

                    Column {
                        Text(
                            text = "Reel blocked",
                            style = StillfeedTheme.typography.titleSmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your message is still here.",
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Primary CTA (Node 70:195 - 70:196)
            StillfeedPrimaryButton(
                text = "Back to messages",
                onClick = onBackToMessages
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            // Small Note (Node 70:197)
            Text(
                text = "No accidental detours into short video.",
                style = StillfeedTheme.typography.caption,
                color = StillfeedTheme.colors.textSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
