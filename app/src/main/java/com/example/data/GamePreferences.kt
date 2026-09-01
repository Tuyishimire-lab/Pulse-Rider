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

  var ghostEnabled: Boolean
    get() = prefs.getBoolean(KEY_GHOST_ENABLED, true)
    set(value) = prefs.edit().putBoolean(KEY_GHOST_ENABLED, value).apply()

  fun saveGhostRecording(recording: com.example.game.GhostRecording) {
    // Serialize frames as pipe-delimited: y,lane,time|y,lane,time|...
    val framesStr = recording.frames.joinToString("|") { "${it.y},${it.lane},${it.elapsedTime}" }
    prefs.edit()
      .putString(KEY_GHOST_FRAMES, framesStr)
      .putInt(KEY_GHOST_SCORE, recording.finalScore)
      .putFloat(KEY_GHOST_DURATION, recording.totalDuration)
      .apply()
  }

  fun loadGhostRecording(): com.example.game.GhostRecording? {
    val framesStr = prefs.getString(KEY_GHOST_FRAMES, null) ?: return null
    if (framesStr.isBlank()) return null

    val frames = framesStr.split("|").mapNotNull { segment ->
      val parts = segment.split(",")
      if (parts.size == 3) {
        try {
          com.example.game.GhostFrame(
            y = parts[0].toFloat(),
            lane = parts[1].toInt(),
            elapsedTime = parts[2].toFloat()
          )
        } catch (_: Exception) { null }
      } else null
    }

    if (frames.isEmpty()) return null

    return com.example.game.GhostRecording(
      frames = frames,
      finalScore = prefs.getInt(KEY_GHOST_SCORE, 0),
      totalDuration = prefs.getFloat(KEY_GHOST_DURATION, 0f)
    )
  }

  var dailyChallengeDate: String
    get() = prefs.getString(KEY_DAILY_DATE, "") ?: ""
    set(value) = prefs.edit().putString(KEY_DAILY_DATE, value).apply()

  var dailyHighScore: Int
    get() = prefs.getInt(KEY_DAILY_HIGH_SCORE, 0)
    set(value) = prefs.edit().putInt(KEY_DAILY_HIGH_SCORE, value).apply()

  companion object {
    private const val KEY_SOUND_FX = "key_sound_fx"
    private const val KEY_MUSIC_SYNTH = "key_music_synth"
    private const val KEY_HAPTICS = "key_haptics"
    private const val KEY_HIGH_SCORE = "key_high_score"
    private const val KEY_GHOST_ENABLED = "key_ghost_enabled"
    private const val KEY_GHOST_FRAMES = "key_ghost_frames"
    private const val KEY_GHOST_SCORE = "key_ghost_score"
    private const val KEY_GHOST_DURATION = "key_ghost_duration"
    private const val KEY_DAILY_DATE = "key_daily_date"
    private const val KEY_DAILY_HIGH_SCORE = "key_daily_high_score"
  }
}
