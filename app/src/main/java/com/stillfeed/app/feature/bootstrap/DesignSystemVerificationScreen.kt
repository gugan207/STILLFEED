package com.stillfeed.app.feature.bootstrap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedBottomNavigation
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedDivider
import com.stillfeed.app.core.ui.components.StillfeedHeader
import com.stillfeed.app.core.ui.components.StillfeedNavDestination
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.components.StillfeedStatus
import com.stillfeed.app.core.ui.components.StillfeedStatusBadge
import com.stillfeed.app.core.ui.components.StillfeedTagBadge
import com.stillfeed.app.core.ui.components.StillfeedTagType
import com.stillfeed.app.core.ui.components.StillfeedTextButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Temporary Internal Development Screen for STEP 02 Visual Verification.
 * Not for production use.
 * Displays all design tokens, typography, colors, components, and bottom navigation.
 */
@Composable
fun DesignSystemVerificationScreen(
    modifier: Modifier = Modifier
) {
    var isDarkTheme by remember { mutableStateOf(false) }
    var selectedNav by remember { mutableStateOf(StillfeedNavDestination.Home) }

    StillfeedTheme(darkTheme = isDarkTheme) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = StillfeedTheme.colors.backgroundPrimary
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = StillfeedTheme.spacing.screenMargin)
                    .padding(top = 16.dp, bottom = 20.dp)
            ) {
                // 1. HEADER HIERARCHY (STILLFEED -> Section Label -> Step Counter)
                StillfeedHeader(
                    sectionLabel = "Design System Verification",
                    stepCounter = "02 / 02"
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDarkTheme) "Theme: Dark (Low-light)" else "Theme: Light (Warm paper)",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textSecondary
                    )
                    StillfeedTextButton(
                        text = if (isDarkTheme) "Switch to Light" else "Switch to Dark",
                        onClick = { isDarkTheme = !isDarkTheme }
                    )
                }

                StillfeedDivider()
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

                // 2. PRODUCT MOTTO
                Text(
                    text = "YOUR ATTENTION IS STILL YOURS",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "A calmer way to use your phone.",
                    style = StillfeedTheme.typography.headlineLarge,
                    color = StillfeedTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

                // 3. TYPOGRAPHY SCALE SHOWCASE
                Text(
                    text = "TYPOGRAPHY SCALE",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                StillfeedCard {
                    Text(
                        text = "Super Display (86sp)",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textSecondary
                    )
                    Text(
                        text = "12",
                        style = StillfeedTheme.typography.superDisplay,
                        color = StillfeedTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                    Text(
                        text = "Display (52sp): 74 reels",
                        style = StillfeedTheme.typography.display,
                        color = StillfeedTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                    Text(
                        text = "Title Large (30sp): Make it work for you.",
                        style = StillfeedTheme.typography.titleLarge,
                        color = StillfeedTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                    Text(
                        text = "Title Medium (24sp): Reels & Short video",
                        style = StillfeedTheme.typography.titleMedium,
                        color = StillfeedTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                    Text(
                        text = "Body Medium (14sp): STILLFEED blocks endless short-form video while keeping the social parts you came for.",
                        style = StillfeedTheme.typography.bodyMedium,
                        color = StillfeedTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

                    Text(
                        text = "Body Small (12sp): Fixed for this month; reviewed monthly.",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

                // 4. COLOR TOKENS
                Text(
                    text = "COLOR PALETTE TOKENS",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                StillfeedCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ColorSwatch("Bg", StillfeedTheme.colors.backgroundPrimary)
                        ColorSwatch("Surface", StillfeedTheme.colors.surface)
                        ColorSwatch("Primary", StillfeedTheme.colors.textPrimary)
                        ColorSwatch("Secondary", StillfeedTheme.colors.textSecondary)
                        ColorSwatch("Sage", StillfeedTheme.colors.accent)
                        ColorSwatch("Coral", StillfeedTheme.colors.destructive)
                        ColorSwatch("Border", StillfeedTheme.colors.divider)
                    }
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

                // 5. STATUS INDICATORS & TAGS
                Text(
                    text = "STATUS BADGES & TAGS",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(StillfeedTheme.spacing.sm)
                ) {
                    StillfeedStatusBadge(status = StillfeedStatus.Protected)
                    StillfeedStatusBadge(status = StillfeedStatus.NotConnected)
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(StillfeedTheme.spacing.sm)
                ) {
                    StillfeedTagBadge(text = "BLOCKED", type = StillfeedTagType.Blocked)
                    StillfeedTagBadge(text = "KEPT", type = StillfeedTagType.Kept)
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

                // 6. BUTTONS
                Text(
                    text = "BUTTON COMPONENTS",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                StillfeedPrimaryButton(
                    text = "START WITH THESE",
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                StillfeedSecondaryButton(
                    text = "Review permissions",
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                StillfeedTextButton(
                    text = "Reset activity",
                    onClick = {},
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

                // 7. BOTTOM NAVIGATION PREVIEW
                Text(
                    text = "BOTTOM NAVIGATION (3 EQUAL ZONES)",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                StillfeedBottomNavigation(
                    selectedDestination = selectedNav,
                    onDestinationSelected = { selectedNav = it }
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color, CircleShape)
                .border(1.dp, StillfeedTheme.colors.divider, CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = StillfeedTheme.typography.caption,
            color = StillfeedTheme.colors.textSecondary
        )
    }
}
