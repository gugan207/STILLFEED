package com.stillfeed.app.feature.settings.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Exact Figma Settings Toggle Row (Node 70:155-70:159).
 *
 * SPECIFICATIONS FROM FIGMA:
 * - Card container: fill #FBF8F2, r=22dp, width 334dp, height 76dp
 * - Title: 14sp SemiBold #1F2421
 * - Subtitle: 11sp Regular #6D746E
 * - Switch Track: 44x26dp, r=13dp, fill #7D927C (on) / #D9D4CA (off)
 * - Switch Knob: 18x18dp, r=9dp, fill #FBF8F2
 */
@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) com.stillfeed.app.core.ui.theme.StillfeedSageGreen else StillfeedTheme.colors.divider,
        label = "trackColor"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 4.dp,
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StillfeedTheme.colors.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .semantics {
                role = Role.Switch
                contentDescription = "$title, $subtitle"
                stateDescription = if (checked) "On" else "Off"
            }
            .then(
                if (onCheckedChange != null && enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onCheckedChange(!checked) }
                    )
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
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

            // Custom Figma Switch (44x26dp track with 18x18dp knob)
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(trackColor),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = thumbOffset)
                        .size(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(com.stillfeed.app.core.ui.theme.StillfeedCardCream)
                )
            }
        }
    }
}
