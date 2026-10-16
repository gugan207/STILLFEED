package com.stillfeed.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.theme.StillfeedTheme

enum class StillfeedStatus {
    Protected,
    NotConnected,
    Offline
}

enum class StillfeedTagType {
    Blocked,
    Kept
}

/**
 * STILLFEED Status Badge (e.g. for Protected / Not connected / Offline status).
 * Pill with 16dp corner radius, 32dp height, 10sp uppercase/bold text.
 */
@Composable
fun StillfeedStatusBadge(
    status: StillfeedStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        StillfeedStatus.Protected -> Triple(
            StillfeedTheme.colors.statusProtectedBackground,
            StillfeedTheme.colors.statusProtectedText,
            "Protected"
        )
        StillfeedStatus.NotConnected -> Triple(
            StillfeedTheme.colors.statusInactiveBackground,
            StillfeedTheme.colors.statusInactiveText,
            "Not connected"
        )
        StillfeedStatus.Offline -> Triple(
            StillfeedTheme.colors.surfaceVariant,
            StillfeedTheme.colors.textSecondary,
            "Offline"
        )
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = StillfeedTheme.shapes.badge)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = StillfeedTheme.typography.label,
            color = textColor
        )
    }
}

/**
 * STILLFEED Tag Badge (e.g. "BLOCKED" and "KEPT" tags).
 * Pill with 14dp corner radius, 28dp height, 10sp uppercase/bold text.
 */
@Composable
fun StillfeedTagBadge(
    text: String,
    type: StillfeedTagType,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (type) {
        StillfeedTagType.Blocked -> Pair(
            StillfeedTheme.colors.tagBlockedBackground,
            StillfeedTheme.colors.tagBlockedText
        )
        StillfeedTagType.Kept -> Pair(
            StillfeedTheme.colors.tagKeptBackground,
            StillfeedTheme.colors.tagKeptText
        )
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = StillfeedTheme.shapes.tag)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = StillfeedTheme.typography.label,
            color = textColor
        )
    }
}
