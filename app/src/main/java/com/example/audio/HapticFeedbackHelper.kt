package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackHelper(context: Context) {
  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    vibratorManager?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  var enabled: Boolean = true

  fun pulseShift() {
    if (!enabled || vibrator == null || !vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    } else {
      @Suppress("DEPRECATION")
      vibrator.vibrate(15)
    }
  }

  fun nearMiss() {
    if (!enabled || vibrator == null || !vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
    } else {
      @Suppress("DEPRECATION")
      vibrator.vibrate(35)
    }
  }

  fun powerUp() {
    if (!enabled || vibrator == null || !vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(
        VibrationEffect.createWaveform(
          longArrayOf(0, 20, 40, 30),
          intArrayOf(0, 150, 0, 220),
          -1
        )
      )
    } else {
      @Suppress("DEPRECATION")
      vibrator.vibrate(50)
    }
  }

  fun crash() {
    if (!enabled || vibrator == null || !vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(
        VibrationEffect.createWaveform(
          longArrayOf(0, 60, 40, 80),
          intArrayOf(0, 255, 0, 200),
          -1
        )
      )
    } else {
      @Suppress("DEPRECATION")
      vibrator.vibrate(180)
    }
  }
}
