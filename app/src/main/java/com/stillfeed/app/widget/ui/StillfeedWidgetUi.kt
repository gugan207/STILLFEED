package com.stillfeed.app.widget.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.stillfeed.app.MainActivity
import com.stillfeed.app.widget.graphics.WidgetSceneRenderer
import com.stillfeed.app.widget.model.WidgetSizeCategory
import com.stillfeed.app.widget.model.WidgetTimeTheme
import com.stillfeed.app.widget.model.WidgetUiState

/**
 * Figma-matched Jetpack Glance composables for native Android Home Screen Widgets.
 *
 * Implements the 3 canonical size form-factors and 5 diurnal themes:
 * - COMPACT (250 × 190 dp)
 * - MEDIUM (430 × 190 dp)
 * - LARGE (360 × 360 dp)
 *
 * Visual Rule: Environment first · Companion second · Count last.
 */
@Composable
fun StillfeedWidgetContent(
    context: Context,
    state: WidgetUiState,
    sizeCategory: WidgetSizeCategory,
    widthDp: Int = 250,
    heightDp: Int = 190
) {
    val textColor = if (state.theme == WidgetTimeTheme.NIGHT) {
        Color(0xFFFAF7F0)
    } else {
        Color(0xFF1A2121)
    }
    val textColProvider = ColorProvider(textColor)

    // Render background vector illustration at 2x density for crisp launcher display
    val renderW = (widthDp * 2).coerceAtLeast(300)
    val renderH = (heightDp * 2).coerceAtLeast(200)
    val illustrationBitmap = WidgetSceneRenderer.renderBackgroundIllustration(
        theme = state.theme,
        sizeCategory = sizeCategory,
        width = renderW,
        height = renderH,
        cornerRadiusPx = 0f
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(Color(WidgetSceneRenderer.getBackgroundColor(state.theme)))
            .clickable(actionStartActivity<MainActivity>())
    ) {
        // 1. Vector Illustration Layer (Sky + Hills + Companion Character)
        Image(
            provider = ImageProvider(illustrationBitmap),
            contentDescription = "Stillfeed companion scene",
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize()
        )

        // 2. Foreground Typography & Layout
        when (sizeCategory) {
            WidgetSizeCategory.COMPACT -> {
                CompactWidgetOverlay(state = state, textColor = textColProvider)
            }
            WidgetSizeCategory.MEDIUM -> {
                MediumWidgetOverlay(state = state, textColor = textColProvider)
            }
            WidgetSizeCategory.LARGE -> {
                LargeWidgetOverlay(state = state, textColor = textColProvider)
            }
        }
    }
}

@Composable
private fun CompactWidgetOverlay(
    state: WidgetUiState,
    textColor: ColorProvider
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 12.dp)
    ) {
        // Brand Row
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = GlanceModifier.width(20.dp))
            Text(
                text = "Stillfeed",
                style = TextStyle(
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Reel Count
        Text(
            text = state.displayCount,
            style = TextStyle(
                color = textColor,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )
        )

        // Label: "reels today"
        Text(
            text = "reels today",
            style = TextStyle(
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        // Mindful Message
        Text(
            text = state.editorialMessage,
            style = TextStyle(
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            ),
            maxLines = 2
        )
    }
}

@Composable
private fun MediumWidgetOverlay(
    state: WidgetUiState,
    textColor: ColorProvider
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 14.dp)
    ) {
        // Brand Row
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = GlanceModifier.width(20.dp))
            Text(
                text = "Stillfeed",
                style = TextStyle(
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        // Reel Count
        Text(
            text = state.displayCount,
            style = TextStyle(
                color = textColor,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        )

        // Label: "reels today"
        Text(
            text = "reels today",
            style = TextStyle(
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        // Mindful Message
        Text(
            text = state.editorialMessage,
            style = TextStyle(
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            ),
            maxLines = 2
        )
    }
}

@Composable
private fun LargeWidgetOverlay(
    state: WidgetUiState,
    textColor: ColorProvider
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 18.dp)
    ) {
        // Brand Row
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = GlanceModifier.width(20.dp))
            Text(
                text = "Stillfeed",
                style = TextStyle(
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(14.dp))

        // Editorial Headline
        Text(
            text = state.editorialHeadline,
            style = TextStyle(
                color = textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 2
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        // Reel Count
        Text(
            text = state.displayCount,
            style = TextStyle(
                color = textColor,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold
            )
        )

        // Label: "reels today"
        Text(
            text = "reels today",
            style = TextStyle(
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Mindful Message
        Text(
            text = state.editorialMessage,
            style = TextStyle(
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            ),
            maxLines = 2
        )
    }
}

/**
 * Creates a safe Intent to launch STILLFEED with authentication preserved.
 */
fun createWidgetLaunchIntent(context: Context, destination: String = "home"): Intent {
    return Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("source", "widget")
        putExtra("destination", destination)
    }
}
