package com.stillfeed.app.auth.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * STILLFEED Verify Code Screen (Figma Frames: 00C — Verify Code, 00C-E — Wrong code, Nodes 289:674, 289:719).
 *
 * Exact specifications:
 * - Brand Header: Focus mark + SKILLFEED wordmark
 * - Mail icon in 48dp charcoal circle (#1F2421)
 * - Title: "Check your email." (Inter SemiBold 42sp, #1F2421)
 * - Subtitle: "We sent a six-digit code to {email}." (Inter Regular 15sp, #6D746E)
 * - Error alert on invalid/expired code: "That code is incorrect or has expired. Request a new code and try again."
 * - Discrete 6-digit OTP row (49x60dp each, #FBF8F2 surface, auto-advancing focus)
 * - Help text: "The code expires after a short time. Keep it private."
 * - Primary CTA: "Verify code" (pill 56dp, #1F2421)
 * - Resend prompt: "Didn’t get it?  Resend code" (with 30s countdown state: "Resend code in 0:30")
 * - Navigation link: "Use a different email"
 */
@Composable
fun VerifyCodeScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF5F0E7)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Brand Header (Figma Left-Aligned)
                AuthBrandHeader()

                Spacer(modifier = Modifier.height(18.dp))

                // Mail Icon in dark circle (Left-Aligned)
                DarkCircleMailIcon()

                Spacer(modifier = Modifier.height(14.dp))

                // Screen Title & Subtitle (Figma Left-Aligned)
                Text(
                    text = "Check your email.",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2421),
                    lineHeight = 42.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                val displayEmail = uiState.emailInput.ifBlank { "your email" }
                Text(
                    text = "We sent a six-digit code to $displayEmail.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6D746E),
                    lineHeight = 22.sp
                )

                // Respectful error alert
                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthAlertCard(message = uiState.errorMessage ?: "")
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Verification Code Label (Figma Left-Aligned)
                Text(
                    text = "Verification code",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6D746E),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 6-digit OTP Input Row (Discrete boxes centered in the row)
                OtpInputRow(
                    otpValue = uiState.otpInput,
                    onOtpChange = { viewModel.onOtpChanged(it) },
                    hasError = uiState.errorMessage != null,
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Help text (Figma Left-Aligned)
                Text(
                    text = "The code expires after a short time. Keep it private.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6D746E)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Primary CTA: Verify code
                AuthPrimaryButton(
                    text = if (uiState.isLoading) "Verifying code…" else "Verify code",
                    onClick = { viewModel.submitVerifyOtp() },
                    isLoading = uiState.isLoading,
                    enabled = uiState.otpInput.length == 6 && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Didn't get it? Resend code
                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Didn’t get it?  ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6D746E)
                )

                if (uiState.resendCountdownSeconds > 0) {
                    val secFormatted = if (uiState.resendCountdownSeconds < 10) "0${uiState.resendCountdownSeconds}" else "${uiState.resendCountdownSeconds}"
                    Text(
                        text = "Resend code in 0:$secFormatted",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6D746E)
                    )
                } else {
                    Text(
                        text = "Resend code",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2421),
                        modifier = Modifier
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                enabled = !uiState.isLoading,
                                role = Role.Button,
                                onClick = { viewModel.resendOtp() }
                            )
                            .semantics { this.role = Role.Button }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Use a different email link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Use a different email",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2421),
                    modifier = Modifier
                        .clickable(
                            enabled = !uiState.isLoading,
                            role = Role.Button,
                            onClick = onNavigateBack
                        )
                        .padding(vertical = 4.dp)
                        .semantics { this.role = Role.Button }
                )
            }
        }
    }
}
}
