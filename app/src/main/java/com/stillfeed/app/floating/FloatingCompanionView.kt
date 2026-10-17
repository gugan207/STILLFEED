package com.stillfeed.app.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.stillfeed.app.R

/**
 * Isolated, high-fidelity floating companion view.
 *
 * Hosts the local offline Three.js 3D WebGL model, and automatically degrades to a lightweight
 * native Canvas fallback if WebGL is unavailable or encounters context loss.
 */
@SuppressLint("SetJavaScriptEnabled")
class FloatingCompanionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var webView: WebView? = null
    private val fallbackView: NativeFallbackView = NativeFallbackView(context)
    private var currentState: FloatingIconUiState = FloatingIconUiState()
    private var isRendererReady: Boolean = false
    private var isUsingFallback: Boolean = false

    var onMoodChangedListener: ((BrainMood) -> Unit)? = null
    var onIconClickListener: (() -> Unit)? = null

    init {
        // Fallback view is always added first at the base
        addView(fallbackView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        fallbackView.visibility = View.VISIBLE

        setupWebView()
    }

    private fun setupWebView() {
        try {
            val wv = WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true // restricted to android_asset
                    allowContentAccess = false
                    allowFileAccessFromFileURLs = false
                    allowUniversalAccessFromFileURLs = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }

                val bridge = FloatingCompanionJsBridge(
                    onMoodChanged = { moodId, _ ->
                        val resolved = BrainMood.fromId(moodId)
                        onMoodChangedListener?.invoke(resolved)
                    },
                    onIconClicked = {
                        onIconClickListener?.invoke()
                    },
                    onRendererReady = { isWebGL ->
                        isRendererReady = true
                        if (isWebGL) {
                            fallbackView.visibility = View.GONE
                            this.visibility = View.VISIBLE
                        } else {
                            switchToFallback()
                        }
                    }
                )

                addJavascriptInterface(bridge, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        switchToFallback()
                    }
                }

                loadUrl("file:///android_asset/floating_icon/floating_icon.html")
            }

            webView = wv
            addView(wv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        } catch (e: Exception) {
            switchToFallback()
        }
    }

    private fun switchToFallback() {
        isUsingFallback = true
        webView?.visibility = View.GONE
        fallbackView.visibility = View.VISIBLE
        fallbackView.updateState(currentState)
    }

    /**
     * Updates the companion view with new reactive state.
     */
    fun bindState(state: FloatingIconUiState) {
        currentState = state
        fallbackView.updateState(state)

        if (isRendererReady && !isUsingFallback) {
            val script = "window.SkillfeedIcon && window.SkillfeedIcon.setCompanionData(" +
                    "'${escapeJs(state.displayValue)}', " +
                    "${state.progressRatio}, " +
                    "${state.mood.ordinal});"
            webView?.evaluateJavascript(script, null)

            val animScript = "window.SkillfeedIcon && window.SkillfeedIcon.setAnimationsEnabled(${state.isAnimationsEnabled});"
            webView?.evaluateJavascript(animScript, null)
        }
    }

    /**
     * Triggers a spring pop bounce on tap.
     */
    fun triggerTapBounce() {
        if (isRendererReady && !isUsingFallback) {
            webView?.evaluateJavascript("window.SkillfeedIcon && window.SkillfeedIcon.triggerTap();", null)
        } else {
            fallbackView.triggerTap()
        }
    }

    /**
     * Pauses the animation loop to conserve battery when screen is off or app is in background.
     */
    fun pause() {
        webView?.evaluateJavascript("window.SkillfeedIcon && window.SkillfeedIcon.pause();", null)
        webView?.onPause()
    }

    /**
     * Resumes the animation loop when visible.
     */
    fun resume() {
        webView?.onResume()
        webView?.evaluateJavascript("window.SkillfeedIcon && window.SkillfeedIcon.resume();", null)
    }

    /**
     * Properly releases all WebGL and WebView resources.
     */
    fun destroyView() {
        webView?.let { wv ->
            wv.stopLoading()
            wv.removeJavascriptInterface("AndroidBridge")
            removeView(wv)
            wv.destroy()
        }
        webView = null
    }

    private fun escapeJs(s: String): String {
        return s.replace("\\", "\\\\").replace("'", "\\'")
    }

    /**
     * Lightweight native Canvas fallback reproducing the reference SVG fallback.
     */
    private class NativeFallbackView(context: Context) : View(context) {
        private var state = FloatingIconUiState()
        private val pillOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#2B1400")
        }
        private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#713A00")
        }
        private val brainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FF9DB5")
        }
        private val brainOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.parseColor("#5A1F3A")
        }
        private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#08080C")
        }
        private val eyeHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        private val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#4A1D2C")
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFB15E")
            textAlign = Paint.Align.CENTER
            try {
                typeface = ResourcesCompat.getFont(context, R.font.inter_bold) ?: Typeface.DEFAULT_BOLD
            } catch (e: Exception) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }

        private val pillRect = RectF()
        private val pillOutlineRect = RectF()
        private val mouthPath = Path()

        fun updateState(newState: FloatingIconUiState) {
            state = newState
            pillPaint.color = Color.parseColor(state.mood.pillColorHex)
            brainPaint.color = Color.parseColor(state.mood.brainColorHex)
            textPaint.color = Color.parseColor(state.mood.numColorHex)
            invalidate()
        }

        fun triggerTap() {
            animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(120)
                .withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }
                .start()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            val scale = minOf(w / 300f, h / 140f)
            canvas.save()
            canvas.translate((w - 300f * scale) / 2f, (h - 140f * scale) / 2f)
            canvas.scale(scale, scale)

            // 1. Dark pill outline
            pillOutlineRect.set(6f, 14f, 294f, 126f)
            canvas.drawRoundRect(pillOutlineRect, 56f, 56f, pillOutlinePaint)

            // 2. Brown pill body
            pillRect.set(10f, 10f, 290f, 122f)
            canvas.drawRoundRect(pillRect, 56f, 56f, pillPaint)

            // 3. Brain ellipse and outline
            canvas.drawOval(34f, 26f, 138f, 110f, brainPaint)
            canvas.drawOval(34f, 26f, 138f, 110f, brainOutlinePaint)

            // 4. Eyes
            canvas.drawCircle(68f, 68f, 11f, eyePaint)
            canvas.drawCircle(104f, 68f, 11f, eyePaint)
            canvas.drawCircle(65f, 64f, 4f, eyeHighlightPaint)
            canvas.drawCircle(101f, 64f, 4f, eyeHighlightPaint)

            // 5. Mouth (smile or frown based on mood)
            mouthPath.reset()
            if (state.mood == BrainMood.SAD || state.mood == BrainMood.WORN_OUT) {
                mouthPath.moveTo(78f, 94f)
                mouthPath.quadTo(86f, 86f, 94f, 94f)
            } else {
                mouthPath.moveTo(78f, 88f)
                mouthPath.quadTo(86f, 96f, 94f, 88f)
            }
            canvas.drawPath(mouthPath, mouthPaint)

            // 6. Number / em-dash display
            textPaint.textSize = if (state.displayValue.length > 2) 46f else 56f
            val fontMetrics = textPaint.fontMetrics
            val baseline = 75f - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(state.displayValue, 214f, baseline, textPaint)

            canvas.restore()
        }
    }
}
