package com.stillfeed.app.floating

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * Thread-safe JavaScript interface exposing two-way communication between the WebView and Android.
 */
class FloatingCompanionJsBridge(
    private val onMoodChanged: (moodId: String, moodName: String) -> Unit,
    private val onIconClicked: () -> Unit,
    private val onRendererReady: (isWebGL: Boolean) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun notifyMoodChanged(moodId: String, moodName: String) {
        mainHandler.post {
            onMoodChanged(moodId, moodName)
        }
    }

    @JavascriptInterface
    fun notifyIconClicked() {
        mainHandler.post {
            onIconClicked()
        }
    }

    @JavascriptInterface
    fun notifyRendererReady(isWebGL: Boolean) {
        mainHandler.post {
            onRendererReady(isWebGL)
        }
    }
}
