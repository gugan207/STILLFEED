package com.stillfeed.app.feature.awareness.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus

/**
 * Calm status badge for STILLFEED's Monthly Awareness state (STEP 09 Section 4).
 *
 * Mapped from Figma Node 145:20 (14dp corner radius pill, 10sp SemiBold, warm calm palette).
 *
 * PRODUCT CONSTRAINTS:
 * - NEVER score out of 100.
 * - NEVER use shame, guilt, failure, or "lazy" messaging.
 * - Calm, objective information only.
 */
@Composable
fun AwarenessStatusBadge(
    status: MonthlyAwarenessStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label, accessibilityLabel) = when (status) {
        MonthlyAwarenessStatus.BASELINE_FORMING -> Quadruple(
            StillfeedTheme.colors.statusInactiveBackground,
            StillfeedTheme.colors.statusInactiveText,
            "Forming baseline",
            "Monthly awareness status: Forming baseline"
        )
        MonthlyAwarenessStatus.ON_TRACK -> Quadruple(
            StillfeedTheme.colors.statusProtectedBackground,
            StillfeedTheme.colors.statusProtectedText,
            "On track",
            "Monthly awareness status: On track"
        )
        MonthlyAwarenessStatus.REDUCING -> Quadruple(
            StillfeedTheme.colors.statusProtectedBackground,
            StillfeedTheme.colors.statusProtectedText,
            "Reducing",
            "Monthly awareness status: Reducing"
        )
        MonthlyAwarenessStatus.TARGET_REACHED -> Quadruple(
            StillfeedTheme.colors.statusProtectedBackground,
            StillfeedTheme.colors.statusProtectedText,
            "Target reached",
            "Monthly awareness status: Target reached"
        )
        MonthlyAwarenessStatus.DATA_UNAVAILABLE -> Quadruple(
            StillfeedTheme.colors.statusInactiveBackground,
            StillfeedTheme.colors.statusInactiveText,
            "Unavailable",
            "Monthly awareness status: Data unavailable"
        )
    }

    Box(
        modifier = modifier
            .semantics { contentDescription = accessibilityLabel }
            .background(color = bgColor, shape = StillfeedTheme.shapes.tag)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            style = StillfeedTheme.typography.label,
            color = textColor
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
