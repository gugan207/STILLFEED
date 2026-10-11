package com.stillfeed.app.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = StillfeedCharcoal,
    onPrimary = StillfeedCardCream,
    secondary = StillfeedSageGreen,
    onSecondary = StillfeedCharcoal,
    tertiary = StillfeedBlushCoral,
    background = StillfeedWarmPaper,
    onBackground = StillfeedCharcoal,
    surface = StillfeedCardCream,
    onSurface = StillfeedCharcoal,
    surfaceVariant = StillfeedWarmPaper,
    onSurfaceVariant = StillfeedMutedOlive,
    outline = StillfeedSandBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = StillfeedDarkTextPrimary,
    onPrimary = StillfeedDarkSlate,
    secondary = StillfeedDarkSageGreen,
    onSecondary = StillfeedDarkSlate,
    tertiary = StillfeedDarkCoral,
    background = StillfeedDarkSlate,
    onBackground = StillfeedDarkTextPrimary,
    surface = StillfeedDarkSurface,
    onSurface = StillfeedDarkTextPrimary,
    surfaceVariant = StillfeedDarkSurfaceVariant,
    onSurfaceVariant = StillfeedDarkTextSecondary,
    outline = StillfeedDarkBorder
)

/**
 * Access STILLFEED design system tokens throughout the Compose hierarchy.
 */
object StillfeedTheme {
    val colors: StillfeedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStillfeedColors.current

    val typography: StillfeedTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalStillfeedTypography.current

    val spacing: StillfeedSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalStillfeedSpacing.current

    val shapes: StillfeedShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalStillfeedShapes.current
}

@Composable
fun StillfeedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val stillfeedColors = if (darkTheme) stillfeedDarkColors() else stillfeedLightColors()
    val stillfeedTypography = StillfeedTypography()
    val stillfeedSpacing = StillfeedSpacing()
    val stillfeedShapes = StillfeedShapes()
    val materialColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = stillfeedColors.backgroundPrimary.toArgb()
            window.navigationBarColor = stillfeedColors.backgroundPrimary.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalStillfeedColors provides stillfeedColors,
        LocalStillfeedTypography provides stillfeedTypography,
        LocalStillfeedSpacing provides stillfeedSpacing,
        LocalStillfeedShapes provides stillfeedShapes
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = MaterialTypography,
            content = content
        )
    }
}
