package com.stillfeed.app.protection.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * High-performance sound and haptic synthesizer for STILLFEED reset activities.
 *
 * Provides:
 * 1. Ticking countdown pulse ("tick... tick...") synthesized per second during reset countdown.
 * 2. Rewarding completion chime when reset finishes.
 * 3. Zero-dependency PCM synthesis via AudioTrack: zero latency, zero asset load failures.
 * 4. Synchronized haptic feedback for physical sensory awareness.
 */
class ResetSoundPlayer(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var tickTrack: AudioTrack? = null
    private var completionTrack: AudioTrack? = null

    init {
        initAudioTracks()
    }

    private fun initAudioTracks() {
        try {
            val sampleRate = 44100

            // 1. Synthesize "time bomb / timer tick" pulse (~22ms, high mechanical click 2400Hz with exponential damping)
            val tickDuration = 0.022f
            val tickSamples = (sampleRate * tickDuration).toInt()
            val tickPcm = ShortArray(tickSamples)
            for (i in 0 until tickSamples) {
                val t = i.toFloat() / sampleRate
                val decay = exp(-t * 220f)
                val sample = sin(2.0 * PI * 2400.0 * t) * decay * 0.75 +
                        sin(2.0 * PI * 1200.0 * t) * decay * 0.25
                tickPcm[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            tickTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(tickSamples * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            tickTrack?.write(tickPcm, 0, tickSamples)

            // 2. Synthesize completion chime (~450ms, dual-tone chime: 659Hz E5 -> 880Hz A5)
            val compDuration = 0.45f
            val compSamples = (sampleRate * compDuration).toInt()
            val compPcm = ShortArray(compSamples)
            for (i in 0 until compSamples) {
                val t = i.toFloat() / sampleRate
                val freq = if (t < 0.12f) 659.25 else 880.0
                val decay = if (t < 0.12f) exp(-t * 8f) else exp(-(t - 0.12f) * 6f)
                val sample = sin(2.0 * PI * freq * t) * decay * 0.8
                compPcm[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            completionTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(compSamples * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            completionTrack?.write(compPcm, 0, compSamples)
        } catch (_: Exception) {
            // AudioTrack initialization fallback
        }
    }

    /**
     * Plays the crisp countdown tick sound and subtle haptic feedback.
     */
    fun playTick() {
        try {
            tickTrack?.let { track ->
                track.stop()
                track.reloadStaticData()
                track.play()
            }
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(18L, 90))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(18L)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Plays the rewarding completion chime and celebratory haptic feedback.
     */
    fun playCompletion() {
        try {
            completionTrack?.let { track ->
                track.stop()
                track.reloadStaticData()
                track.play()
            }
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 50, 100), intArrayOf(0, 180, 0, 255), -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 70, 50, 100), -1)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Releases audio tracks and stops playback.
     */
    fun release() {
        try {
            tickTrack?.stop()
            tickTrack?.release()
            tickTrack = null

            completionTrack?.stop()
            completionTrack?.release()
            completionTrack = null
        } catch (_: Exception) {}
    }
}
