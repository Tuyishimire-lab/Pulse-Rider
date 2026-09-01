package com.example.data

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("pulse_rider_prefs", Context.MODE_PRIVATE)

  var soundFxEnabled: Boolean
    get() = prefs.getBoolean(KEY_SOUND_FX, true)
    set(value) = prefs.edit().putBoolean(KEY_SOUND_FX, value).apply()

  var musicSynthEnabled: Boolean
    get() = prefs.getBoolean(KEY_MUSIC_SYNTH, true)
    set(value) = prefs.edit().putBoolean(KEY_MUSIC_SYNTH, value).apply()

  var hapticsEnabled: Boolean
    get() = prefs.getBoolean(KEY_HAPTICS, true)
    set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

  var localHighScore: Int
    get() = prefs.getInt(KEY_HIGH_SCORE, 0)
    set(value) = prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()

  companion object {
    private const val KEY_SOUND_FX = "key_sound_fx"
    private const val KEY_MUSIC_SYNTH = "key_music_synth"
    private const val KEY_HAPTICS = "key_haptics"
    private const val KEY_HIGH_SCORE = "key_high_score"
  }
}
