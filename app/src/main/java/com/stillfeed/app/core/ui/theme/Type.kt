package com.stillfeed.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stillfeed.app.R

// =============================================================================
// Inter Font Family — Local-first Android font resources from SIL OFL Inter font
// =============================================================================
val InterFontFamily = FontFamily(
    Font(resId = R.font.inter_regular, weight = FontWeight.Normal),
    Font(resId = R.font.inter_medium, weight = FontWeight.Medium),
    Font(resId = R.font.inter_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.inter_bold, weight = FontWeight.Bold)
)

/**
 * STILLFEED Typography Scale.
 * Extracted directly from Figma text styles and screen elements.
 */
@Immutable
data class StillfeedTypography(
    /**
     * Brand wordmark "STILLFEED", Inter Bold 15sp, tracking 0.5sp
     */
    val brandWordmark: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    ),

    /**
     * Hero number display (e.g. "12" on Home screen), Inter SemiBold 86sp
     */
    val superDisplay: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 86.sp,
        lineHeight = 104.sp
    ),

    /**
     * Large counter / numeric metrics (e.g. "74", "30 reels"), Inter SemiBold 52sp
     */
    val display: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 52.sp,
        lineHeight = 63.sp
    ),

    /**
     * Hero screen headings (e.g. "A calmer way to use your phone.", "Your week"), Inter SemiBold 42sp
     */
    val headlineLarge: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp,
        lineHeight = 51.sp
    ),

    /**
     * Prominent screen headers (e.g. "Your phone can feel quieter now."), Inter SemiBold 35sp
     */
    val headlineMedium: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 35.sp,
        lineHeight = 42.sp
    ),

    /**
     * Section headings (e.g. "Short video is the thing...", "Make it work for you."), Inter SemiBold 30sp - 32sp
     */
    val titleLarge: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp
    ),

    /**
     * Card titles (e.g. "Reels", "DMs + Stories", "30 reels"), Inter SemiBold 23sp - 25sp
     */
    val titleMedium: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 29.sp
    ),

    /**
     * Smaller card headings & dialog banners, Inter SemiBold 21sp - 22sp
     */
    val titleSmall: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 26.sp
    ),

    /**
     * Emphasized body / subtitles, Inter SemiBold 16sp - 17sp
     */
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp
    ),

    /**
     * Standard readable explanatory body text, Inter Regular 14sp
     */
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 17.sp
    ),

    /**
     * Secondary captions and footers, Inter Regular 12sp - 13sp
     */
    val bodySmall: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 15.sp
    ),

    /**
     * Button labels (e.g. "START WITH THESE", "Choose a reset"), Inter SemiBold 14sp
     */
    val button: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 17.sp
    ),

    /**
     * Uppercase section labels & tags (e.g. "GET STARTED", "TODAY", "SETTINGS", "BLOCKED"), Inter SemiBold 10sp
     */
    val label: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp
    ),

    /**
     * Step indicators & small metadata (e.g. "01 / 05", "01 / 03"), Inter SemiBold 10sp
     */
    val stepIndicator: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp
    ),

    /**
     * Active bottom nav labels & small bold chips, Inter SemiBold 10sp
     */
    val labelSmall: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp
    ),

    /**
     * Inactive bottom nav labels & footnote text, Inter Regular 10sp
     */
    val caption: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
)

val LocalStillfeedTypography = staticCompositionLocalOf { StillfeedTypography() }

/**
 * Material 3 Typography integration using STILLFEED tokens.
 */
val MaterialTypography = Typography(
    displayLarge = StillfeedTypography().headlineLarge,
    displayMedium = StillfeedTypography().headlineMedium,
    displaySmall = StillfeedTypography().titleLarge,
    headlineLarge = StillfeedTypography().titleLarge,
    headlineMedium = StillfeedTypography().titleMedium,
    headlineSmall = StillfeedTypography().titleSmall,
    titleLarge = StillfeedTypography().titleMedium,
    titleMedium = StillfeedTypography().titleSmall,
    titleSmall = StillfeedTypography().bodyLarge,
    bodyLarge = StillfeedTypography().bodyLarge,
    bodyMedium = StillfeedTypography().bodyMedium,
    bodySmall = StillfeedTypography().bodySmall,
    labelLarge = StillfeedTypography().button,
    labelMedium = StillfeedTypography().label,
    labelSmall = StillfeedTypography().caption
)
