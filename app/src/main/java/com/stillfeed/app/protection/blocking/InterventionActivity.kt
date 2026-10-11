package com.stillfeed.app.protection.blocking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stillfeed.app.MainActivity
import com.stillfeed.app.R
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.components.StillfeedStatus
import com.stillfeed.app.core.ui.components.StillfeedStatusBadge
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Calm, respectful intervention activity displayed when a short-form algorithmic video feed
 * (Reels or Shorts) is intercepted (STEP 06 Phase 6).
 *
 * SPECIFICATION:
 * - Uses existing STILLFEED design system components and tokens.
 * - Presents clear, non-judgmental information.
 * - Zero shame, guilt, failure, or negative messaging.
 * - Provides a safe, easy return path.
 */
class InterventionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        val surfaceCategory = intent.getStringExtra(EXTRA_SURFACE_CATEGORY) ?: "Short-form"

        setContent {
            StillfeedTheme {
                InterventionContent(
                    surfaceCategory = surfaceCategory,
                    onReturn = { finish() },
                    onOpenStillfeed = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_SURFACE_CATEGORY = "extra_surface_category"
    }
}

@Composable
fun InterventionContent(
    surfaceCategory: String,
    onReturn: () -> Unit,
    onOpenStillfeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StillfeedTheme.colors.backgroundPrimary)
            .systemBarsPadding()
            .padding(StillfeedTheme.spacing.screenMargin),
        contentAlignment = Alignment.Center
    ) {
        StillfeedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(StillfeedTheme.spacing.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                StillfeedStatusBadge(status = StillfeedStatus.Protected)

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                Text(
                    text = stringResource(R.string.intervention_title),
                    style = StillfeedTheme.typography.titleLarge,
                    color = StillfeedTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                Text(
                    text = stringResource(R.string.intervention_subtitle),
                    style = StillfeedTheme.typography.bodyMedium,
                    color = StillfeedTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

                StillfeedPrimaryButton(
                    text = stringResource(R.string.intervention_button_return),
                    onClick = onReturn
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                StillfeedSecondaryButton(
                    text = stringResource(R.string.intervention_button_open_app),
                    onClick = onOpenStillfeed
                )
            }
        }
    }
}
