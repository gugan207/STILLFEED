package com.stillfeed.app.protection.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * High-performance sound and haptic synthesizer for STILLFEED reset activities.
 *
 * Provides:
 * 1. Resonant time bomb / timer ticking pulse per second during reset countdown.
 * 2. Rewarding 3-tone completion chime when reset countdown reaches 0s.
 * 3. Uses USAGE_MEDIA / STREAM_MUSIC so sound is loud and clearly audible even when face-down.
 * 4. Dual-layer: Zero-latency PCM AudioTrack with ToneGenerator fallback.
 * 5. Synchronized sensory haptic vibration.
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
    private var fallbackToneGen: ToneGenerator? = null

    init {
        initAudioTracks()
        try {
            fallbackToneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Exception) {}
    }

    private fun initAudioTracks() {
        try {
            val sampleRate = 44100

            // 1. Synthesize "time bomb / timer tick" pulse (~60ms)
            // Crisp 880Hz (A5) fundamental with 1760Hz octave overtone, 4ms attack, smooth exponential decay
            val tickDuration = 0.060f
            val tickSamples = (sampleRate * tickDuration).toInt()
            val tickPcm = ShortArray(tickSamples)
            val attackSamples = (sampleRate * 0.004f).toInt()

            for (i in 0 until tickSamples) {
                val t = i.toFloat() / sampleRate
                // Attack ramp to avoid DC pop
                val attack = if (i < attackSamples) i.toFloat() / attackSamples else 1.0f
                val decay = exp(-t * 60f) * attack
                val sample = (sin(2.0 * PI * 880.0 * t) * 0.75 + sin(2.0 * PI * 1760.0 * t) * 0.25) * decay
                tickPcm[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            tickTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
            tickTrack?.setVolume(1.0f)

            // 2. Synthesize triumphant 3-tone completion chime (~650ms: C5 -> E5 -> G5)
            val compDuration = 0.65f
            val compSamples = (sampleRate * compDuration).toInt()
            val compPcm = ShortArray(compSamples)

            for (i in 0 until compSamples) {
                val t = i.toFloat() / sampleRate
                val freq: Double
                val noteTime: Float
                val noteDecay: Float

                if (t < 0.18f) {
                    freq = 523.25 // C5
                    noteTime = t
                    noteDecay = exp(-noteTime * 6f)
                } else if (t < 0.36f) {
                    freq = 659.25 // E5
                    noteTime = t - 0.18f
                    noteDecay = exp(-noteTime * 6f)
                } else {
                    freq = 783.99 // G5
                    noteTime = t - 0.36f
                    noteDecay = exp(-noteTime * 4.5f)
                }

                val attack = min(1.0f, noteTime / 0.005f)
                val sample = (sin(2.0 * PI * freq * t) * 0.7 + sin(2.0 * PI * freq * 2.0 * t) * 0.3) * noteDecay * attack
                compPcm[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            completionTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
            completionTrack?.setVolume(1.0f)
        } catch (_: Exception) {
            // AudioTrack initialization fallback
        }
    }

    /**
     * Plays the loud countdown time bomb / timer tick sound and synchronized haptic feedback.
     */
    fun playTick() {
        var played = false
        try {
            tickTrack?.let { track ->
                track.stop()
                track.reloadStaticData()
                track.play()
                played = true
            }
        } catch (_: Exception) {}

        if (!played) {
            try {
                fallbackToneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
            } catch (_: Exception) {}
        }

        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(24L, 140))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(24L)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Plays the rewarding completion chime and celebratory haptic feedback.
     */
    fun playCompletion() {
        var played = false
        try {
            completionTrack?.let { track ->
                track.stop()
                track.reloadStaticData()
                track.play()
                played = true
            }
        } catch (_: Exception) {}

        if (!played) {
            try {
                fallbackToneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            } catch (_: Exception) {}
        }

        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 120), intArrayOf(0, 180, 0, 255), -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 80, 60, 120), -1)
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

            fallbackToneGen?.release()
            fallbackToneGen = null
        } catch (_: Exception) {}
    }
}
