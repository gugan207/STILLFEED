package com.stillfeed.app.auth.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * STILLFEED Authentication Input Field.
 *
 * Implements the exact finalized Figma specifications (Nodes 265:43, 265:47, 265:62, 265:66, 265:71):
 * - Dimensions: 334dp width, 62dp height
 * - Corner radius: 18dp
 * - Surface fill: StillfeedCardCream (#FBF8F2) — NO separate white inner rectangle
 * - Border: 1dp StillfeedSandBorder (#D9D4CA)
 * - Padding: 16dp start/end, 9dp top/bottom
 * - Label: Inter SemiBold 10sp, StillfeedMutedOlive (#6D746E)
 * - Editable text: Inter Regular 14sp, StillfeedCharcoal (#1F2421)
 */
@Composable
fun AuthInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    hasError: Boolean = false,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(
                color = StillfeedTheme.colors.surface,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = if (hasError) StillfeedTheme.colors.destructive else StillfeedTheme.colors.divider,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(start = 16.dp, top = 9.dp, end = 12.dp, bottom = 9.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = if (trailingContent != null) 42.dp else 0.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(3.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = StillfeedTheme.typography.bodyMedium,
                        color = StillfeedTheme.colors.textMuted
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = label },
                    textStyle = StillfeedTheme.typography.bodyMedium.copy(
                        color = StillfeedTheme.colors.textPrimary
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(StillfeedTheme.colors.textPrimary),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    enabled = enabled
                )
            }
        }

        if (trailingContent != null) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd),
                contentAlignment = Alignment.Center
            ) {
                trailingContent()
            }
        }
    }
}

/**
 * STILLFEED Password Visibility Eye Toggle.
 *
 * Implements the exact Figma eye geometry (Node 278:37):
 * - Visual icon size: 24 x 24 dp
 * - Minimum touch target: 44 x 44 dp
 * - Stroke: 1.45dp, color #1F2421 at 62% alpha
 * - Pupil: 6 x 6 dp, color #1F2421 at 70% alpha
 * - Core: 2 x 2 dp, color #FBF8F2 at 90% alpha
 * - Revealed state: Diagonal slash across eye
 * - Dynamic accessibility label: "Show password" / "Hide password"
 */
@Composable
fun PasswordVisibilityToggle(
    isPasswordVisible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val description = if (isPasswordVisible) "Hide password" else "Show password"
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = description,
                onClick = onToggle
            )
            .semantics {
                this.contentDescription = description
                this.role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            val scaleX = w / 48f
            val scaleY = h / 48f

            // Eye outline matching Figma path
            val path = Path().apply {
                moveTo(2f * scaleX, 24f * scaleY)
                cubicTo(
                    9.7f * scaleX, 12.2222f * scaleY,
                    17.84f * scaleX, 8f * scaleY,
                    24f * scaleX, 8f * scaleY
                )
                cubicTo(
                    30.16f * scaleX, 8f * scaleY,
                    38.3f * scaleX, 12.2222f * scaleY,
                    46f * scaleX, 24f * scaleY
                )
                cubicTo(
                    38.3f * scaleX, 35.7778f * scaleY,
                    30.16f * scaleX, 40f * scaleY,
                    24f * scaleX, 40f * scaleY
                )
                cubicTo(
                    17.84f * scaleX, 40f * scaleY,
                    9.7f * scaleX, 35.7778f * scaleY,
                    2f * scaleX, 24f * scaleY
                )
                close()
            }

            drawPath(
                path = path,
                color = Color(0xFF1F2421).copy(alpha = 0.62f),
                style = Stroke(
                    width = 1.45.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Eye pupil (circle r = 6 in 48x48 box -> 3dp)
            drawCircle(
                color = Color(0xFF1F2421).copy(alpha = 0.70f),
                radius = 3.dp.toPx(),
                center = Offset(24f * scaleX, 24f * scaleY)
            )

            // Eye core (circle r = 2 in 48x48 box -> 1dp)
            drawCircle(
                color = Color(0xFFFBF8F2).copy(alpha = 0.90f),
                radius = 1.dp.toPx(),
                center = Offset(24f * scaleX, 24f * scaleY)
            )

            // Diagonal slash when password is revealed
            if (isPasswordVisible) {
                drawLine(
                    color = Color(0xFF1F2421).copy(alpha = 0.75f),
                    start = Offset(8f * scaleX, 40f * scaleY),
                    end = Offset(40f * scaleX, 8f * scaleY),
                    strokeWidth = 1.45.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * STILLFEED Authentication Primary CTA Button.
 *
 * Exact Figma dimensions (Nodes 265:52, 265:76):
 * - Width: 294dp, Height: 52dp
 * - Corner radius: 26dp
 * - Container fill: StillfeedCharcoal (#1F2421)
 * - Text: Inter SemiBold 14sp, StillfeedCardCream (#FBF8F2)
 * - Loading state: Subtle progress indicator, prevents duplicate submission
 */
@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = StillfeedTheme.colors.buttonPrimaryBackground,
            contentColor = StillfeedTheme.colors.buttonPrimaryText,
            disabledContainerColor = StillfeedTheme.colors.buttonPrimaryBackground.copy(alpha = 0.6f),
            disabledContentColor = StillfeedTheme.colors.buttonPrimaryText.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .width(294.dp)
            .height(52.dp)
            .semantics {
                this.role = Role.Button
            }
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = StillfeedTheme.colors.buttonPrimaryText,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = StillfeedTheme.typography.button,
                color = StillfeedTheme.colors.buttonPrimaryText
            )
        }
    }
}

/**
 * STILLFEED Authentication Secondary Prompt / Navigation Action.
 *
 * Exact Figma specifications (Nodes 275:37, 275:38):
 * - Centered, Inter Regular 12sp, StillfeedMutedOlive (#6D746E)
 * - Sign In: "New here?  CREATE ACCOUNT"
 * - Create Account: "Already have an account?  SIGN IN"
 */
@Composable
fun AuthSecondaryAction(
    prefixText: String,
    actionText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .width(294.dp)
            .height(44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = actionText,
                onClick = onClick
            )
            .semantics {
                this.contentDescription = "$prefixText $actionText"
                this.role = Role.Button
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$prefixText  ",
            style = StillfeedTheme.typography.bodySmall,
            color = StillfeedTheme.colors.textSecondary
        )
        Text(
            text = actionText,
            style = StillfeedTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = StillfeedTheme.colors.textPrimary
        )
    }
}
