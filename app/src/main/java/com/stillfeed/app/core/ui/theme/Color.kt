package com.stillfeed.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =============================================================================
// STILLFEED Design Tokens — Extracted from Figma (SW3qMYI0E3pnYALuExsdd8)
// Visual source of truth: Page 02 (App Design), Page 01 (Widgets)
// =============================================================================

// Base Palette Tokens — Light Mode ("Warm paper")
val StillfeedWarmPaper = Color(0xFFF5F0E7)        // rgb(245, 240, 231) — Primary app background
val StillfeedCardCream = Color(0xFFFBF8F2)        // rgb(251, 248, 242) — Card surface & CTA text
val StillfeedCharcoal = Color(0xFF1F2421)         // rgb(31, 36, 33)   — Primary text & dark CTA fill
val StillfeedMutedOlive = Color(0xFF6D746E)       // rgb(109, 116, 110) — Secondary & metadata text
val StillfeedOliveSubtle = Color(0xFF8C928D)      // rgb(140, 146, 141) — Muted captions
val StillfeedSandBorder = Color(0xFFD9D4CA)       // rgb(217, 212, 202) — Card borders & dividers
val StillfeedSageGreen = Color(0xFF7D927C)        // rgb(125, 146, 124) — Accent, KEPT tag, Protected status
val StillfeedHighlightGold = Color(0xFFE8C86B)     // rgb(232, 200, 107) — Current-day highlight (Figma Node 70:137)
val StillfeedBlushCoral = Color(0xFFD8A5A1)       // rgb(216, 165, 161) — Destructive, BLOCKED tag
val StillfeedStatusMuted = Color(0xFFD9D4CA)      // rgb(217, 212, 202) — "Not connected" badge fill

// Base Palette Tokens — Dark Mode ("Low-light", Screen 32 Appearance)
val StillfeedDarkSlate = Color(0xFF252A27)        // rgb(37, 42, 39)   — Dark app background
val StillfeedDarkSurface = Color(0xFF303531)      // rgb(48, 53, 49)   — Dark card surface
val StillfeedDarkSurfaceVariant = Color(0xFF3A403B) // rgb(58, 64, 59) — Dark surface highlight
val StillfeedDarkTextPrimary = Color(0xFFFBF8F2)  // rgb(251, 248, 242) — Dark mode primary text
val StillfeedDarkTextSecondary = Color(0xFFD7D9D4)// rgb(215, 217, 212) — Dark mode secondary text
val StillfeedDarkTextMuted = Color(0xFF969C97)    // rgb(150, 156, 151) — Dark mode muted text
val StillfeedDarkBorder = Color(0xFF4A504B)       // rgb(74, 80, 75)   — Dark mode borders & dividers
val StillfeedDarkSageGreen = Color(0xFF8EA58D)    // rgb(142, 165, 141) — Dark mode accent / sage
val StillfeedDarkCoral = Color(0xFFDEAFAB)        // rgb(222, 175, 171) — Dark mode destructive

/**
 * Semantic color contract for STILLFEED components.
 */
@Immutable
data class StillfeedColors(
    val backgroundPrimary: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textInverse: Color,
    val accent: Color,
    val destructive: Color,
    val divider: Color,
    val buttonPrimaryBackground: Color,
    val buttonPrimaryText: Color,
    val buttonSecondaryBackground: Color,
    val buttonSecondaryText: Color,
    val buttonSecondaryBorder: Color,
    val statusProtectedBackground: Color,
    val statusProtectedText: Color,
    val statusInactiveBackground: Color,
    val statusInactiveText: Color,
    val tagBlockedBackground: Color,
    val tagBlockedText: Color,
    val tagKeptBackground: Color,
    val tagKeptText: Color,
    val isDark: Boolean
)

fun stillfeedLightColors(): StillfeedColors = StillfeedColors(
    backgroundPrimary = StillfeedWarmPaper,
    backgroundSecondary = StillfeedCardCream,
    surface = StillfeedCardCream,
    surfaceVariant = StillfeedWarmPaper,
    textPrimary = StillfeedCharcoal,
    textSecondary = StillfeedMutedOlive,
    textMuted = StillfeedOliveSubtle,
    textInverse = StillfeedCardCream,
    accent = StillfeedSageGreen,
    destructive = StillfeedBlushCoral,
    divider = StillfeedSandBorder,
    buttonPrimaryBackground = StillfeedCharcoal,
    buttonPrimaryText = StillfeedCardCream,
    buttonSecondaryBackground = StillfeedCardCream,
    buttonSecondaryText = StillfeedCharcoal,
    buttonSecondaryBorder = StillfeedSandBorder,
    statusProtectedBackground = StillfeedSageGreen,
    statusProtectedText = StillfeedCharcoal,
    statusInactiveBackground = StillfeedStatusMuted,
    statusInactiveText = StillfeedMutedOlive,
    tagBlockedBackground = StillfeedBlushCoral,
    tagBlockedText = StillfeedCharcoal,
    tagKeptBackground = StillfeedSageGreen,
    tagKeptText = StillfeedCharcoal,
    isDark = false
)

fun stillfeedDarkColors(): StillfeedColors = StillfeedColors(
    backgroundPrimary = StillfeedDarkSlate,
    backgroundSecondary = StillfeedDarkSurface,
    surface = StillfeedDarkSurface,
    surfaceVariant = StillfeedDarkSurfaceVariant,
    textPrimary = StillfeedDarkTextPrimary,
    textSecondary = StillfeedDarkTextSecondary,
    textMuted = StillfeedDarkTextMuted,
    textInverse = StillfeedCharcoal,
    accent = StillfeedDarkSageGreen,
    destructive = StillfeedDarkCoral,
    divider = StillfeedDarkBorder,
    buttonPrimaryBackground = StillfeedDarkTextPrimary,
    buttonPrimaryText = StillfeedDarkSlate,
    buttonSecondaryBackground = StillfeedDarkSurface,
    buttonSecondaryText = StillfeedDarkTextPrimary,
    buttonSecondaryBorder = StillfeedDarkBorder,
    statusProtectedBackground = StillfeedDarkSageGreen,
    statusProtectedText = StillfeedDarkSlate,
    statusInactiveBackground = StillfeedDarkSurfaceVariant,
    statusInactiveText = StillfeedDarkTextSecondary,
    tagBlockedBackground = StillfeedDarkCoral,
    tagBlockedText = StillfeedDarkSlate,
    tagKeptBackground = StillfeedDarkSageGreen,
    tagKeptText = StillfeedDarkSlate,
    isDark = true
)

val LocalStillfeedColors = staticCompositionLocalOf { stillfeedLightColors() }
