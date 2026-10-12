package com.stillfeed.app.auth.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * STILLFEED Reset Link Sent Screen (Figma Frame: 00E — Reset Link Sent, Node 289:808).
 *
 * Exact specifications:
 * - Brand Header: Focus mark + SKILLFEED wordmark
 * - Mail icon in 48dp charcoal circle (#1F2421)
 * - Title: "Check your email." (Inter SemiBold 42sp, #1F2421)
 * - Subtitle: "If an account exists for {email}, a link to reset your password is on its way. It can take a minute to arrive." (Inter Regular 15sp, #6D746E)
 * - Secondary CTA: "Back to sign in" (56dp pill, #FBF8F2 background, 1dp #D9D4CA border)
 */
@Composable
fun ResetLinkSentScreen(
    viewModel: AuthViewModel,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val emailTarget = uiState.resetEmailSentTo ?: uiState.emailInput.ifBlank { "your email" }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF5F0E7),
        topBar = {
            AuthBrandHeader(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 8.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Mail Icon in dark circle
            DarkCircleMailIcon()

            Spacer(modifier = Modifier.height(16.dp))

            // Screen Title & Subtitle
            Text(
                text = "Check your email.",
                fontSize = 42.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2421),
                lineHeight = 46.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "If an account exists for $emailTarget, a link to reset your password is on its way. It can take a minute to arrive.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF6D746E),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Back to sign in button (Pill 56dp, #FBF8F2 with #D9D4CA border)
            Button(
                onClick = onNavigateToSignIn,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFBF8F2),
                    contentColor = Color(0xFF1F2421)
                ),
                border = BorderStroke(1.dp, Color(0xFFD9D4CA)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { this.role = Role.Button }
            ) {
                Text(
                    text = "Back to sign in",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2421)
                )
            }
        }
    }
}
