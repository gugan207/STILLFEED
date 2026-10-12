package com.stillfeed.app.auth.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.stillfeed.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * STILLFEED Brand Header (Figma Component: Auth / Brand Header, Node 296:283).
 * Contains brand symbol and wordmark with exact Figma proportions and typography.
 */
@Composable
fun AuthBrandHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Focus Symbol (40x40dp)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(32.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                // Concentric focus rings matching STILLFEED design system
                drawCircle(
                    color = Color(0xFF1F2421),
                    radius = 14.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF6D746E),
                    radius = 8.dp.toPx(),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF1F2421),
                    radius = 3.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "SKILLFEED",
            fontFamily = StillfeedTheme.typography.headlineLarge.fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 1.2.sp,
            color = Color(0xFF1F2421)
        )
    }
}

/**
 * Auth Divider (Figma Component: Auth / Divider, Node 287:93).
 * Thin sand lines with centered 'or' text.
 */
@Composable
fun AuthDivider(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFFD9D4CA))
        )
        Text(
            text = "or",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF6D746E),
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFFD9D4CA))
        )
    }
}

/**
 * Google Sign-In Button (Figma Component: Auth / Button Secondary, Node 288:58).
 * Exact dimensions: 56dp height, 26dp pill corner radius, #FBF8F2 background, #D9D4CA border.
 */
@Composable
fun GoogleSignInButton(
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
            containerColor = Color(0xFFFBF8F2),
            contentColor = Color(0xFF1F2421),
            disabledContainerColor = Color(0xFFFBF8F2).copy(alpha = 0.6f),
            disabledContentColor = Color(0xFF1F2421).copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9D4CA)),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics { this.role = Role.Button }
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF1F2421),
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Official Google 4-Color 'G' Icon from Figma Design System (Node 286:61)
                Icon(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Continue with Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2421)
                )
            }
        }
    }
}

/**
 * Authentication Alert Card (Figma Component: Auth / Alert, Node 288:77).
 * Displays respectful error message with coral background (#FBEAE7) and red border (#B3261E).
 */
@Composable
fun AuthAlertCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFBEAE7))
            .border(1.dp, Color(0xFFB3261E), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            // Warning Alert Icon (16x16dp)
            Canvas(
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            ) {
                val cx = size.width / 2f
                val h = size.height
                val path = Path().apply {
                    moveTo(cx, 1f)
                    lineTo(size.width - 1f, h - 1f)
                    lineTo(1f, h - 1f)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color(0xFFB3261E),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Exclamation dot
                drawCircle(
                    color = Color(0xFFB3261E),
                    radius = 1.dp.toPx(),
                    center = Offset(cx, h - 4.dp.toPx())
                )
                // Exclamation line
                drawLine(
                    color = Color(0xFFB3261E),
                    start = Offset(cx, 5.dp.toPx()),
                    end = Offset(cx, h - 7.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFB3261E),
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Six-digit OTP Input Row (Figma Component: Auth / OTP Digit, Node 288:91).
 * Six discrete boxes (49 x 60 dp) with auto-advancing focus, backspace handling, and paste support.
 */
@Composable
fun OtpInputRow(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hasError: Boolean = false,
    enabled: Boolean = true
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }

    LaunchedEffect(Unit) {
        if (enabled && otpValue.isEmpty()) {
            focusRequesters.firstOrNull()?.requestFocus()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 6) {
            val char = otpValue.getOrNull(i)?.toString() ?: ""
            val isFocused = otpValue.length == i

            val borderColor = when {
                hasError -> Color(0xFFB3261E)
                isFocused -> Color(0xFF1F2421)
                else -> Color(0xFFD9D4CA)
            }
            val borderWidth = if (isFocused) 2.dp else 1.dp

            Box(
                modifier = Modifier
                    .width(49.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFBF8F2))
                    .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
                    .clickable(enabled = enabled) {
                        val targetIndex = otpValue.length.coerceAtMost(5)
                        focusRequesters[targetIndex].requestFocus()
                    },
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = char,
                    onValueChange = { newText ->
                        if (newText.length > 1) {
                            // User pasted a multi-character string
                            val digitsOnly = newText.filter { it.isDigit() }.take(6)
                            onOtpChange(digitsOnly)
                            val nextIndex = digitsOnly.length.coerceAtMost(5)
                            focusRequesters[nextIndex].requestFocus()
                        } else if (newText.isNotEmpty() && newText.first().isDigit()) {
                            val newOtp = StringBuilder(otpValue)
                            if (i < newOtp.length) {
                                newOtp.setCharAt(i, newText.first())
                            } else {
                                newOtp.append(newText.first())
                            }
                            val updated = newOtp.toString().take(6)
                            onOtpChange(updated)
                            if (i < 5) {
                                focusRequesters[i + 1].requestFocus()
                            }
                        }
                    },
                    modifier = Modifier
                        .focusRequester(focusRequesters[i])
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Backspace && char.isEmpty() && i > 0) {
                                focusRequesters[i - 1].requestFocus()
                                val newOtp = otpValue.dropLast(1)
                                onOtpChange(newOtp)
                                true
                            } else {
                                false
                            }
                        }
                        .semantics { contentDescription = "Digit ${i + 1}" },
                    textStyle = StillfeedTheme.typography.headlineMedium.copy(
                        color = Color(0xFF1F2421),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = if (i == 5) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { if (i < 5) focusRequesters[i + 1].requestFocus() }
                    ),
                    singleLine = true,
                    enabled = enabled,
                    cursorBrush = SolidColor(Color.Transparent)
                )
            }
        }
    }
}

/**
 * Mail Icon in Dark Circle used on confirmation / sent screens (Nodes 289:678, 289:812).
 */
@Composable
fun DarkCircleMailIcon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(Color(0xFF1F2421), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            // Envelope outline
            drawRoundRect(
                color = Color(0xFFFBF8F2),
                size = androidx.compose.ui.geometry.Size(w, h * 0.75f),
                topLeft = Offset(0f, h * 0.125f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                style = Stroke(width = 1.8.dp.toPx())
            )
            // Envelope flap lines
            val flap = Path().apply {
                moveTo(2f, h * 0.2f)
                lineTo(w / 2f, h * 0.55f)
                lineTo(w - 2f, h * 0.2f)
            }
            drawPath(
                path = flap,
                color = Color(0xFFFBF8F2),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
