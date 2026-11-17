package com.example.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.HeatingStrength

class HapticFeedbackHelper(context: Context) {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    fun vibrateIntensityToggle(strength: HeatingStrength) {
        val vib = vibrator ?: return
        if (!runCatching { vib.hasVibrator() }.getOrDefault(false)) return

        val durationMs = when (strength) {
            HeatingStrength.LOW -> 20L
            HeatingStrength.MEDIUM -> 34L
            HeatingStrength.HIGH -> 52L
        }
        val amplitude = when (strength) {
            HeatingStrength.LOW -> 90
            HeatingStrength.MEDIUM -> 165
            HeatingStrength.HIGH -> 255
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effectiveAmp = if (vib.hasAmplitudeControl()) {
                    amplitude
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
                vib.vibrate(VibrationEffect.createOneShot(durationMs, effectiveAmp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        }
    }

    fun vibrateThermalSafetyCutoff() {
        val vib = vibrator ?: return
        if (!runCatching { vib.hasVibrator() }.getOrDefault(false)) return

        val timings = longArrayOf(0L, 90L, 70L, 140L)
        val amplitudes = intArrayOf(0, 255, 0, 255)

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vib.hasAmplitudeControl()) {
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vib.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(timings, -1)
            }
        }
    }
}
