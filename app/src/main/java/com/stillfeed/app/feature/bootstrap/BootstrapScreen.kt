package com.stillfeed.app.feature.bootstrap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Bootstrap entry point for STEP 02.
 * Directly renders the temporary internal Design System Verification screen.
 */
@Composable
fun BootstrapScreen(
    modifier: Modifier = Modifier
) {
    DesignSystemVerificationScreen(modifier = modifier)
}
