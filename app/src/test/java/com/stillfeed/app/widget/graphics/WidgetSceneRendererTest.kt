package com.stillfeed.app.widget.graphics

import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.widget.model.WidgetSizeCategory
import com.stillfeed.app.widget.model.WidgetTimeTheme
import com.stillfeed.app.widget.model.WidgetUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetSceneRendererTest {

    @Test
    fun testBackgroundColorsMatchFigmaSpecifications() {
        assertEquals(Color.parseColor("#FAF2D1"), WidgetSceneRenderer.getBackgroundColor(WidgetTimeTheme.MORNING))
        assertEquals(Color.parseColor("#D3EFE8"), WidgetSceneRenderer.getBackgroundColor(WidgetTimeTheme.DAY))
        assertEquals(Color.parseColor("#F9D6BF"), WidgetSceneRenderer.getBackgroundColor(WidgetTimeTheme.AFTERNOON))
        assertEquals(Color.parseColor("#BAA3D8"), WidgetSceneRenderer.getBackgroundColor(WidgetTimeTheme.EVENING))
        assertEquals(Color.parseColor("#112335"), WidgetSceneRenderer.getBackgroundColor(WidgetTimeTheme.NIGHT))
    }

    @Test
    fun testTextColorsMatchFigmaSpecifications() {
        // Night uses light paper text (#F9F7EF)
        assertEquals(Color.parseColor("#F9F7EF"), WidgetSceneRenderer.getTextColor(WidgetTimeTheme.NIGHT))

        // Daytime themes use charcoal (#1A2121)
        assertEquals(Color.parseColor("#1A2121"), WidgetSceneRenderer.getTextColor(WidgetTimeTheme.MORNING))
        assertEquals(Color.parseColor("#1A2121"), WidgetSceneRenderer.getTextColor(WidgetTimeTheme.DAY))
        assertEquals(Color.parseColor("#1A2121"), WidgetSceneRenderer.getTextColor(WidgetTimeTheme.AFTERNOON))
        assertEquals(Color.parseColor("#1A2121"), WidgetSceneRenderer.getTextColor(WidgetTimeTheme.EVENING))
    }

    @Test
    fun testRenderBackgroundIllustrationForAllThemesAndSizes() {
        val themes = WidgetTimeTheme.values()
        val sizes = WidgetSizeCategory.values()

        for (theme in themes) {
            for (size in sizes) {
                val bitmap = WidgetSceneRenderer.renderBackgroundIllustration(
                    theme = theme,
                    sizeCategory = size,
                    width = 250,
                    height = 190,
                    cornerRadiusPx = 20f
                )
                assertNotNull("Bitmap should render for $theme in $size", bitmap)
                assertEquals(250, bitmap.width)
                assertEquals(190, bitmap.height)
            }
        }
    }

    @Test
    fun testRenderFullCardWithRobolectric() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val state = WidgetUiState(
            theme = WidgetTimeTheme.MORNING,
            reelCount = 6,
            monitoredMinutes = 12L,
            editorialMessage = "Your mind looks fresh."
        )

        val fullCardBitmap = WidgetSceneRenderer.renderFullCard(
            context = context,
            state = state,
            sizeCategory = WidgetSizeCategory.COMPACT,
            width = 500,
            height = 380
        )

        assertNotNull(fullCardBitmap)
        assertEquals(500, fullCardBitmap.width)
        assertEquals(380, fullCardBitmap.height)
    }
}
