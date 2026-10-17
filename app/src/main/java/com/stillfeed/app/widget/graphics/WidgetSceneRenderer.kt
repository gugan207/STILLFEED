package com.stillfeed.app.widget.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.stillfeed.app.R
import com.stillfeed.app.widget.model.WidgetSizeCategory
import com.stillfeed.app.widget.model.WidgetTimeTheme
import com.stillfeed.app.widget.model.WidgetUiState

/**
 * Precision graphic renderer that renders the exact Figma vector scene
 * (Canvas `01 — Widgets`, Node IDs `216:2`, `216:33`, `216:58`, `216:81`, `216:107`)
 * onto an Android Bitmap.
 *
 * Supports:
 * - Exact vector geometry for Sun rays, Moon, Stars, Cloud formations, Rolling hills
 * - Blooming companion flower with petals, stem, leaves, center, and responsive facial expressions
 * - Scaling to any launcher widget target size with full subpixel anti-aliasing
 * - Standalone full-card rendering with Inter typography for previews and widgets
 */
object WidgetSceneRenderer {

    // Reference Dimensions from Figma
    private const val COMPACT_REF_W = 250f
    private const val COMPACT_REF_H = 190f

    private const val MEDIUM_REF_W = 430f
    private const val MEDIUM_REF_H = 190f

    private const val LARGE_REF_W = 360f
    private const val LARGE_REF_H = 360f

    /**
     * Renders the background illustration (sky elements, rolling hills, companion character)
     * without text onto a transparent/colored Bitmap.
     */
    fun renderBackgroundIllustration(
        theme: WidgetTimeTheme,
        sizeCategory: WidgetSizeCategory,
        width: Int,
        height: Int,
        cornerRadiusPx: Float = 0f
    ): Bitmap {
        val safeW = width.coerceAtLeast(100)
        val safeH = height.coerceAtLeast(80)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val refW = when (sizeCategory) {
            WidgetSizeCategory.COMPACT -> COMPACT_REF_W
            WidgetSizeCategory.MEDIUM -> MEDIUM_REF_W
            WidgetSizeCategory.LARGE -> LARGE_REF_W
        }
        val refH = when (sizeCategory) {
            WidgetSizeCategory.COMPACT -> COMPACT_REF_H
            WidgetSizeCategory.MEDIUM -> MEDIUM_REF_H
            WidgetSizeCategory.LARGE -> LARGE_REF_H
        }

        val sx = safeW.toFloat() / refW
        val sy = safeH.toFloat() / refH

        canvas.save()

        // Optional corner clip if rendered with card background
        if (cornerRadiusPx > 0f) {
            val clipPath = Path().apply {
                addRoundRect(
                    RectF(0f, 0f, safeW.toFloat(), safeH.toFloat()),
                    cornerRadiusPx,
                    cornerRadiusPx,
                    Path.Direction.CW
                )
            }
            canvas.clipPath(clipPath)
        }

        // Draw solid background fill
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = getBackgroundColor(theme)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, safeW.toFloat(), safeH.toFloat(), bgPaint)

        // Scale canvas to reference coordinate system
        canvas.scale(sx, sy)

        // Render celestial sky elements
        drawSky(canvas, theme, sizeCategory)

        // Render rolling landscape hills
        drawLandscape(canvas, theme, sizeCategory, refW, refH)

        // Render companion flower character
        drawCompanion(canvas, theme, sizeCategory)

        canvas.restore()
        return bitmap
    }

    /**
     * Renders the complete Figma card (background, illustration, brand mark, count, labels, message)
     * using Inter typography onto a high-DPI Bitmap.
     */
    fun renderFullCard(
        context: Context,
        state: WidgetUiState,
        sizeCategory: WidgetSizeCategory,
        width: Int,
        height: Int
    ): Bitmap {
        val safeW = width.coerceAtLeast(100)
        val safeH = height.coerceAtLeast(80)
        val cornerRadius = 20f * (safeW.toFloat() / 250f).coerceIn(1f, 2.5f)

        // 1. Draw background and vector illustration
        val bitmap = renderBackgroundIllustration(state.theme, sizeCategory, safeW, safeH, cornerRadius)
        val canvas = Canvas(bitmap)

        val refW = when (sizeCategory) {
            WidgetSizeCategory.COMPACT -> COMPACT_REF_W
            WidgetSizeCategory.MEDIUM -> MEDIUM_REF_W
            WidgetSizeCategory.LARGE -> LARGE_REF_W
        }
        val refH = when (sizeCategory) {
            WidgetSizeCategory.COMPACT -> COMPACT_REF_H
            WidgetSizeCategory.MEDIUM -> MEDIUM_REF_H
            WidgetSizeCategory.LARGE -> LARGE_REF_H
        }

        val sx = safeW.toFloat() / refW
        val sy = safeH.toFloat() / refH

        canvas.save()
        canvas.scale(sx, sy)

        // Load Inter Fonts with fallback to system Sans-Serif
        val interBold = runCatching { ResourcesCompat.getFont(context, R.font.inter_bold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        val interSemiBold = runCatching { ResourcesCompat.getFont(context, R.font.inter_semibold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        val interRegular = runCatching { ResourcesCompat.getFont(context, R.font.inter_regular) }.getOrNull() ?: Typeface.DEFAULT

        val textColor = getTextColor(state.theme)

        // 2. Draw Brand Mark: Logo Leaf + "Stillfeed"
        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            style = Paint.Style.FILL
        }
        canvas.save()
        canvas.rotate(-20f, 15f + 8f, 18f + 6.5f)
        canvas.drawOval(RectF(14.7f, 18f, 14.7f + 16f, 18f + 13f), leafPaint)
        canvas.restore()

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = interSemiBold
            textSize = 13f
        }
        canvas.drawText("Stillfeed", 39f, 29f, brandPaint)

        // 3. Size-specific Typography Layout
        when (sizeCategory) {
            WidgetSizeCategory.COMPACT -> {
                // Count
                val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    typeface = interBold
                    textSize = 42f
                }
                canvas.drawText(state.displayCount, 20f, 88f, countPaint)

                // Label: "reels today"
                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    typeface = interRegular
                    textSize = 12f
                }
                canvas.drawText("reels today", 22f, 108f, labelPaint)

                // Mindful Message
                val messagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    typeface = interRegular
                    textSize = 13f
                }
                canvas.drawText(state.editorialMessage, 20f, 138f, messagePaint)
            }

            WidgetSizeCategory.MEDIUM -> {
                // Count
                val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    typeface = interBold
                    textSize = 42f
                }
                canvas.drawText(state.displayCount, 20f, 88f, countPaint)

                // Label: "reels today"
                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    typeface = interRegular
                    textSize = 12f
                }
                canvas.drawText("reels today", 22f, 108f, labelPaint)

                // Mindful Message
                val messagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    typeface = interRegular
                    textSize = 14f
                }
                canvas.drawText(state.editorialMessage, 20f, 138f, messagePaint)
            }

            WidgetSizeCategory.LARGE -> {
                // Large Headline
                val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    typeface = interSemiBold
                    textSize = 23f
                }
                canvas.drawText(state.editorialHeadline, 22f, 85f, headlinePaint)

                // Count
                val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    typeface = interBold
                    textSize = 56f
                }
                canvas.drawText(state.displayCount, 22f, 218f, countPaint)

                // Label: "reels today"
                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    typeface = interRegular
                    textSize = 13f
                }
                canvas.drawText("reels today", 26f, 240f, labelPaint)

                // Mindful Message
                val messagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    typeface = interRegular
                    textSize = 15f
                }
                canvas.drawText(state.editorialMessage, 22f, 276f, messagePaint)
            }
        }

        canvas.restore()
        return bitmap
    }

    private fun drawSky(canvas: Canvas, theme: WidgetTimeTheme, sizeCategory: WidgetSizeCategory) {
        when (theme) {
            WidgetTimeTheme.MORNING -> {
                // Radiant Sun with 8 rays (Figma 216:5 - 216:13)
                val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FAAB2E")
                    style = Paint.Style.FILL
                }
                val sunCenterX = 209f
                val sunCenterY = 33f
                canvas.drawCircle(sunCenterX, sunCenterY, 15f, sunPaint)

                // 8 Rays around Sun
                val rayAngles = floatArrayOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
                val rayDist = 20f
                for (angle in rayAngles) {
                    canvas.save()
                    canvas.rotate(angle, sunCenterX, sunCenterY)
                    canvas.drawRoundRect(
                        RectF(sunCenterX + rayDist, sunCenterY - 2f, sunCenterX + rayDist + 8f, sunCenterY + 2f),
                        2f, 2f, sunPaint
                    )
                    canvas.restore()
                }

                // Morning Cloud Cluster (Figma 216:14 - 216:16)
                val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FFFAE8")
                    style = Paint.Style.FILL
                }
                canvas.drawOval(RectF(48f, 55.9f, 48f + 34.3f, 55.9f + 19.8f), cloudPaint)
                canvas.drawOval(RectF(63.2f, 44f, 63.2f + 29.7f, 44f + 25.1f), cloudPaint)
                canvas.drawOval(RectF(83f, 54.6f, 83f + 23.8f, 54.6f + 17.2f), cloudPaint)
            }

            WidgetTimeTheme.DAY -> {
                val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FFFDF4")
                    style = Paint.Style.FILL
                }
                // Left Cloud Formation (216:39 - 216:41)
                canvas.drawOval(RectF(250f, 53.4f, 250f + 27.0f, 53.4f + 15.6f), cloudPaint)
                canvas.drawOval(RectF(262f, 44f, 262f + 23.4f, 44f + 19.8f), cloudPaint)
                canvas.drawOval(RectF(277.6f, 52.3f, 277.6f + 18.7f, 52.3f + 13.5f), cloudPaint)

                // Right Cloud Formation (216:36 - 216:38)
                canvas.drawOval(RectF(348f, 42.6f, 348f + 32.2f, 42.6f + 18.6f), cloudPaint)
                canvas.drawOval(RectF(359.8f, 32f, 359.8f + 27.9f, 32f + 23.6f), cloudPaint)
                canvas.drawOval(RectF(370.9f, 37.9f, 370.9f + 22.3f, 37.9f + 16.1f), cloudPaint)
            }

            WidgetTimeTheme.AFTERNOON -> {
                // Amber Sun (216:61)
                val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F9AF66")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(372f + 17f, 22f + 17f, 17f, sunPaint)

                // Warm Clouds (216:62 - 216:64)
                val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FFEAD6")
                    style = Paint.Style.FILL
                }
                canvas.drawOval(RectF(254f, 50.4f, 254f + 30.2f, 50.4f + 17.4f), cloudPaint)
                canvas.drawOval(RectF(267.3f, 40f, 267.3f + 26.1f, 40f + 22.0f), cloudPaint)
                canvas.drawOval(RectF(284.7f, 49.3f, 284.7f + 20.9f, 49.3f + 15.1f), cloudPaint)
            }

            WidgetTimeTheme.EVENING -> {
                // Setting Sun (216:84)
                val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F7A072")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(296f + 19f, 22f + 19f, 19f, sunPaint)

                // Lilac Dusk Clouds (216:85 - 216:87)
                val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#EDDDE6")
                    style = Paint.Style.FILL
                }
                canvas.drawOval(RectF(188f, 48.1f, 188f + 29.1f, 48.1f + 16.8f), cloudPaint)
                canvas.drawOval(RectF(200.9f, 38f, 200.9f + 25.2f, 38f + 21.3f), cloudPaint)
                canvas.drawOval(RectF(217.7f, 47.0f, 217.7f + 20.2f, 47.0f + 14.6f), cloudPaint)
            }

            WidgetTimeTheme.NIGHT -> {
                // Golden Full Moon (216:110)
                val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F4E0A0")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(302f + 17f, 20f + 17f, 17f, moonPaint)

                // 5 Twinkling Stars (216:111 - 216:115)
                val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#D1D8E0")
                    style = Paint.Style.FILL
                }
                val starXs = floatArrayOf(180f, 216f, 237.6f, 280.8f, 309.6f)
                val starY = 31.6f
                for (x in starXs) {
                    canvas.drawCircle(x + 2f, starY + 2f, 2f, starPaint)
                }
            }
        }
    }

    private fun drawLandscape(
        canvas: Canvas,
        theme: WidgetTimeTheme,
        sizeCategory: WidgetSizeCategory,
        refW: Float,
        refH: Float
    ) {
        val backColor = when (theme) {
            WidgetTimeTheme.MORNING -> Color.parseColor("#D4E0AD")
            WidgetTimeTheme.DAY -> Color.parseColor("#ADCCB8")
            WidgetTimeTheme.AFTERNOON -> Color.parseColor("#E2A891")
            WidgetTimeTheme.EVENING -> Color.parseColor("#825999")
            WidgetTimeTheme.NIGHT -> Color.parseColor("#143A4F")
        }

        val frontColor = when (theme) {
            WidgetTimeTheme.MORNING -> Color.parseColor("#9EB887")
            WidgetTimeTheme.DAY -> Color.parseColor("#7AA88E")
            WidgetTimeTheme.AFTERNOON -> Color.parseColor("#AD7A72")
            WidgetTimeTheme.EVENING -> Color.parseColor("#4F3F68")
            WidgetTimeTheme.NIGHT -> Color.parseColor("#1C495B")
        }

        val backHillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backColor
            style = Paint.Style.FILL
        }

        val frontHillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = frontColor
            style = Paint.Style.FILL
        }

        when (sizeCategory) {
            WidgetSizeCategory.COMPACT -> {
                // Back Hill (216:17)
                canvas.drawOval(RectF(-46f, 115.9f, -46f + 240f, 115.9f + 150f), backHillPaint)
                // Front Hill (216:18)
                canvas.drawOval(RectF(12.5f, 135.9f, 12.5f + 265f, 135.9f + 140f), frontHillPaint)
            }
            WidgetSizeCategory.MEDIUM -> {
                // Back Hill (216:42, 216:65)
                canvas.drawOval(RectF(-46f, 115.9f, -46f + 412.8f, 115.9f + 150f), backHillPaint)
                // Front Hill (216:43, 216:66)
                canvas.drawOval(RectF(21.5f, 135.9f, 21.5f + 455.8f, 135.9f + 140f), frontHillPaint)
            }
            WidgetSizeCategory.LARGE -> {
                // Back Hill (216:88, 216:116)
                canvas.drawOval(RectF(-46f, 219.6f, -46f + 345.6f, 219.6f + 240f), backHillPaint)
                // Front Hill (216:89, 216:117)
                canvas.drawOval(RectF(18f, 239.6f, 18f + 381.6f, 239.6f + 220f), frontHillPaint)
            }
        }
    }

    private fun drawCompanion(canvas: Canvas, theme: WidgetTimeTheme, sizeCategory: WidgetSizeCategory) {
        val stemColor = when (theme) {
            WidgetTimeTheme.MORNING -> Color.parseColor("#47734D")
            WidgetTimeTheme.DAY -> Color.parseColor("#44724F")
            WidgetTimeTheme.AFTERNOON -> Color.parseColor("#546D47")
            WidgetTimeTheme.EVENING -> Color.parseColor("#385647")
            WidgetTimeTheme.NIGHT -> Color.parseColor("#38686B")
        }

        val petalTopColor = when (theme) {
            WidgetTimeTheme.MORNING -> Color.parseColor("#FAADA3")
            WidgetTimeTheme.DAY -> Color.parseColor("#F9B5A8")
            WidgetTimeTheme.AFTERNOON -> Color.parseColor("#F9999B")
            WidgetTimeTheme.EVENING -> Color.parseColor("#CC7087")
            WidgetTimeTheme.NIGHT -> Color.parseColor("#9B77C6")
        }

        val petalSideColor = when (theme) {
            WidgetTimeTheme.MORNING -> Color.parseColor("#F78C91")
            WidgetTimeTheme.DAY -> Color.parseColor("#EF9699")
            WidgetTimeTheme.AFTERNOON -> Color.parseColor("#E87591")
            WidgetTimeTheme.EVENING -> Color.parseColor("#AA567F")
            WidgetTimeTheme.NIGHT -> Color.parseColor("#7A59AA")
        }

        val faceDetailColor = Color.parseColor("#1A2121")

        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stemColor
            style = Paint.Style.FILL
        }
        val topPetalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = petalTopColor
            style = Paint.Style.FILL
        }
        val sidePetalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = petalSideColor
            style = Paint.Style.FILL
        }
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = petalTopColor
            style = Paint.Style.FILL
        }
        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = faceDetailColor
            style = Paint.Style.FILL
        }

        when (sizeCategory) {
            WidgetSizeCategory.COMPACT -> {
                // Stem
                canvas.drawRect(RectF(192.3f, 170.8f, 192.3f + 5.4f, 170.8f + 16.8f), stemPaint)

                // Leaves
                canvas.save()
                canvas.rotate(-30f, 162.3f + 14.4f, 171.8f + 10f)
                canvas.drawOval(RectF(162.3f, 171.8f, 162.3f + 28.8f, 171.8f + 20.1f), stemPaint)
                canvas.restore()

                canvas.save()
                canvas.rotate(30f, 200.9f + 14.4f, 161.2f + 9.5f)
                canvas.drawOval(RectF(200.9f, 161.2f, 200.9f + 28.8f, 161.2f + 19.0f), stemPaint)
                canvas.restore()

                // Petals
                canvas.drawOval(RectF(184.1f, 123.9f, 184.1f + 22.2f, 123.9f + 24.7f), topPetalPaint)
                canvas.drawOval(RectF(169.3f, 140.2f, 169.3f + 24.7f, 140.2f + 22.7f), sidePetalPaint)
                canvas.drawOval(RectF(195.0f, 140.2f, 195.0f + 24.7f, 140.2f + 22.7f), sidePetalPaint)
                canvas.drawOval(RectF(184.1f, 150.1f, 184.1f + 21.7f, 150.1f + 22.2f), sidePetalPaint)

                // Center Disc
                canvas.drawOval(RectF(186.6f, 142.7f, 186.6f + 16.8f, 142.7f + 16.8f), centerPaint)

                // Face: Eyes & Mouth
                canvas.drawRect(RectF(189.1f, 148.1f, 189.1f + 4.7f, 148.1f + 0.9f), facePaint)
                canvas.drawRect(RectF(195.7f, 148.1f, 195.7f + 4.7f, 148.1f + 0.9f), facePaint)
                canvas.drawOval(RectF(193.6f, 152.8f, 193.6f + 2.8f, 152.8f + 1.6f), facePaint)
            }

            WidgetSizeCategory.MEDIUM -> {
                // Stem
                canvas.drawRect(RectF(332.7f, 170.8f, 332.7f + 5.4f, 170.8f + 16.8f), stemPaint)

                // Leaves
                canvas.save()
                canvas.rotate(-30f, 302.7f + 14.4f, 171.8f + 10f)
                canvas.drawOval(RectF(302.7f, 171.8f, 302.7f + 28.8f, 171.8f + 20.1f), stemPaint)
                canvas.restore()

                canvas.save()
                canvas.rotate(30f, 341.3f + 14.4f, 161.2f + 9.5f)
                canvas.drawOval(RectF(341.3f, 161.2f, 341.3f + 28.8f, 161.2f + 19.0f), stemPaint)
                canvas.restore()

                // Petals
                canvas.drawOval(RectF(324.5f, 123.9f, 324.5f + 22.2f, 123.9f + 24.7f), topPetalPaint)
                canvas.drawOval(RectF(309.7f, 140.2f, 309.7f + 24.7f, 140.2f + 22.7f), sidePetalPaint)
                canvas.drawOval(RectF(335.4f, 140.2f, 335.4f + 24.7f, 140.2f + 22.7f), sidePetalPaint)
                canvas.drawOval(RectF(324.5f, 150.1f, 324.5f + 21.7f, 150.1f + 22.2f), sidePetalPaint)

                // Center Disc
                canvas.drawOval(RectF(327.0f, 142.7f, 327.0f + 16.8f, 142.7f + 16.8f), centerPaint)

                // Face
                canvas.drawRect(RectF(329.5f, 148.1f, 329.5f + 4.7f, 148.1f + 0.9f), facePaint)
                canvas.drawRect(RectF(336.1f, 148.1f, 336.1f + 4.7f, 148.1f + 0.9f), facePaint)
                canvas.drawOval(RectF(334.0f, 152.8f, 334.0f + 2.8f, 152.8f + 1.6f), facePaint)
            }

            WidgetSizeCategory.LARGE -> {
                // Large Blooming Companion (360x360 reference)
                // Stem (216:92, 216:120)
                canvas.drawRect(RectF(267.7f, 326.2f, 267.7f + 11.9f, 326.2f + 36.7f), stemPaint)

                // Left & Right Leaves (216:90, 216:91)
                canvas.save()
                canvas.rotate(-30f, 202.1f + 31.5f, 328.3f + 22f)
                canvas.drawOval(RectF(202.1f, 328.3f, 202.1f + 63.0f, 328.3f + 43.9f), stemPaint)
                canvas.restore()

                canvas.save()
                canvas.rotate(30f, 286.6f + 31.5f, 305.0f + 20.7f)
                canvas.drawOval(RectF(286.6f, 305.0f, 286.6f + 62.9f, 305.0f + 41.5f), stemPaint)
                canvas.restore()

                // Large Petals (216:93 - 216:96)
                canvas.drawOval(RectF(249.8f, 223.6f, 249.8f + 48.6f, 223.6f + 54f), topPetalPaint)
                canvas.drawOval(RectF(217.4f, 259.2f, 217.4f + 54f, 259.2f + 49.7f), sidePetalPaint)
                canvas.drawOval(RectF(273.6f, 259.2f, 273.6f + 54f, 259.2f + 49.7f), sidePetalPaint)
                canvas.drawOval(RectF(249.8f, 280.8f, 249.8f + 47.5f, 280.8f + 48.6f), sidePetalPaint)

                // Large Center Disc (216:97, 216:125)
                canvas.drawOval(RectF(255.2f, 264.6f, 255.2f + 36.7f, 264.6f + 36.7f), centerPaint)

                // Mindful Brows (Evening only)
                if (theme == WidgetTimeTheme.EVENING) {
                    canvas.save()
                    canvas.rotate(15f, 259.6f + 5.6f, 271.1f + 1f)
                    canvas.drawRect(RectF(259.6f, 271.1f, 259.6f + 11.3f, 271.1f + 1.9f), facePaint)
                    canvas.restore()

                    canvas.save()
                    canvas.rotate(-15f, 275.2f + 5.6f, 271.1f + 1f)
                    canvas.drawRect(RectF(275.2f, 271.1f, 275.2f + 11.3f, 271.1f + 1.9f), facePaint)
                    canvas.restore()
                }

                // Eyes & Mouth (216:98, 216:99, 216:102)
                canvas.drawRect(RectF(260.6f, 276.5f, 260.6f + 10.3f, 276.5f + 1.9f), facePaint)
                canvas.drawRect(RectF(275.2f, 276.5f, 275.2f + 10.3f, 276.5f + 1.9f), facePaint)
                canvas.drawOval(RectF(270.6f, 286.7f, 270.6f + 6.0f, 286.7f + 3.5f), facePaint)
            }
        }
    }

    fun getBackgroundColor(theme: WidgetTimeTheme): Int {
        return when (theme) {
            WidgetTimeTheme.MORNING -> Color.parseColor("#FAF2D1")
            WidgetTimeTheme.DAY -> Color.parseColor("#D3EFE8")
            WidgetTimeTheme.AFTERNOON -> Color.parseColor("#F9D6BF")
            WidgetTimeTheme.EVENING -> Color.parseColor("#BAA3D8")
            WidgetTimeTheme.NIGHT -> Color.parseColor("#112335")
        }
    }

    fun getTextColor(theme: WidgetTimeTheme): Int {
        return when (theme) {
            WidgetTimeTheme.NIGHT -> Color.parseColor("#F9F7EF")
            else -> Color.parseColor("#1A2121")
        }
    }
}
