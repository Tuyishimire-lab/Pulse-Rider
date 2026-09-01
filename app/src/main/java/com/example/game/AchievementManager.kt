package com.example.game

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AchievementManager(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("pulse_rider_achievements", Context.MODE_PRIVATE)

  var unlockedAchievements: Set<Achievement> by mutableStateOf(loadUnlocked())
    private set

  var pendingNotification: Achievement? by mutableStateOf(null)
    private set

  fun dismissNotification() {
    pendingNotification = null
  }

  /**
   * Evaluate all achievement conditions after a game-over.
   * @param engine the game engine with current run stats
   * @param totalRuns total number of completed runs (all-time)
   * @param totalNearMisses total number of near-misses (all-time)
   */
  fun checkAchievements(
    engine: GameEngine,
    totalRuns: Int,
    totalNearMisses: Int
  ) {
    val scoreInt = engine.score.toInt()
    val maxStreak = engine.maxStreak
    val phaseNumber = engine.currentPhase.phaseNumber
    val shieldsCollected = engine.shieldsCollectedThisRun
    val surgesCollected = engine.surgesCollectedThisRun
    val usedShield = engine.usedShieldThisRun

    val newlyUnlocked = mutableListOf<Achievement>()

    fun tryUnlock(achievement: Achievement, condition: Boolean) {
      if (condition && achievement !in unlockedAchievements) {
        newlyUnlocked.add(achievement)
      }
    }

    // Run-count achievements
    tryUnlock(Achievement.FIRST_RIDE, totalRuns >= 1)
    tryUnlock(Achievement.RUNS_10, totalRuns >= 10)
    tryUnlock(Achievement.RUNS_50, totalRuns >= 50)

    // Score achievements
    tryUnlock(Achievement.SCORE_1000, scoreInt >= 1000)
    tryUnlock(Achievement.SCORE_5000, scoreInt >= 5000)
    tryUnlock(Achievement.SCORE_10000, scoreInt >= 10000)

    // Streak achievements
    tryUnlock(Achievement.STREAK_5, maxStreak >= 5)
    tryUnlock(Achievement.STREAK_10, maxStreak >= 10)

    // Near-miss achievements (all-time cumulative)
    tryUnlock(Achievement.NEAR_MISS_50, totalNearMisses >= 50)
    tryUnlock(Achievement.NEAR_MISS_200, totalNearMisses >= 200)

    // Phase achievements
    tryUnlock(Achievement.PHASE_3, phaseNumber >= 3)
    tryUnlock(Achievement.PHASE_4, phaseNumber >= 4)

    // Per-run achievements
    tryUnlock(Achievement.SHIELD_3, shieldsCollected >= 3)
    tryUnlock(Achievement.SURGE_MASTER, surgesCollected >= 3)
    tryUnlock(Achievement.NO_SHIELD, scoreInt >= 2000 && !usedShield)

    if (newlyUnlocked.isNotEmpty()) {
      val updated = unlockedAchievements + newlyUnlocked
      unlockedAchievements = updated
      saveUnlocked(updated)
      // Show the most impressive (last in enum order) as the notification
      pendingNotification = newlyUnlocked.last()
    }
  }

  private fun loadUnlocked(): Set<Achievement> {
    val saved = prefs.getStringSet(KEY_UNLOCKED, emptySet()) ?: emptySet()
    return saved.mapNotNull { name ->
      try { Achievement.valueOf(name) } catch (_: Exception) { null }
    }.toSet()
  }

  private fun saveUnlocked(achievements: Set<Achievement>) {
    prefs.edit()
      .putStringSet(KEY_UNLOCKED, achievements.map { it.name }.toSet())
      .apply()
  }

  companion object {
    private const val KEY_UNLOCKED = "unlocked_achievements"
  }
}
