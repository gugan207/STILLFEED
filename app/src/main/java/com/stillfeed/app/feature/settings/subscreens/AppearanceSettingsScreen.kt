package com.stillfeed.app.feature.settings.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 32: Appearance Settings (Figma Node 153:46 / FLOW 09: Supporting Settings States).
 */
@Composable
fun AppearanceSettingsScreen(
    currentThemeMode: String,
    onSelectTheme: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            StillfeedScreenHeader(
                sectionLabel = "SETTINGS",
                stepCounter = null
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Appearance.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "Choose how STILLFEED should look.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Option 1: System
            ThemeOptionCard(
                title = "Follow device",
                subtitle = "Match your Android system appearance",
                isSelected = currentThemeMode.equals("SYSTEM", ignoreCase = true),
                onClick = { onSelectTheme("SYSTEM") }
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            // Option 2: Light
            ThemeOptionCard(
                title = "Light · Warm paper",
                subtitle = "Subtle tactile parchment (#F7F4EE)",
                isSelected = currentThemeMode.equals("LIGHT", ignoreCase = true),
                onClick = { onSelectTheme("LIGHT") }
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            // Option 3: Dark
            ThemeOptionCard(
                title = "Dark · Low-light",
                subtitle = "Calm contrast for evening hours",
                isSelected = currentThemeMode.equals("DARK", ignoreCase = true),
                onClick = { onSelectTheme("DARK") }
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Changes apply instantly.",
                style = StillfeedTheme.typography.caption,
                color = StillfeedTheme.colors.textMuted
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            StillfeedSecondaryButton(
                text = "Back to Settings",
                onClick = onNavigateBack
            )
        }
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) StillfeedTheme.colors.accent else StillfeedTheme.colors.divider

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StillfeedTheme.colors.surface)
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = StillfeedTheme.typography.titleSmall,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = StillfeedTheme.typography.caption,
                    color = StillfeedTheme.colors.textSecondary
                )
            }
            if (isSelected) {
                Text(
                    text = "✓",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.accent
                )
            }
        }
    }
}
