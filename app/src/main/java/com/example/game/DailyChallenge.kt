package com.example.game

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * Daily modifiers that change gameplay rules.
 */
enum class DailyModifier(
  val displayName: String,
  val description: String,
  val icon: String
) {
  MIRROR_MODE("Mirror Mode", "Lanes are inverted - swipe up goes down", "[M]"),
  SPEED_DEMON("Speed Demon", "Game starts at Phase 2 speed", "[>>]"),
  NO_SHIELDS("No Shields", "Shield power-ups don't spawn", "[X]"),
  GLASS_CANNON("Glass Cannon", "Score x3 but no second chances", "[!]"),
  MARATHON("Marathon", "Wider gaps but speed ramps faster", "[~]"),
  SURGE_RUSH("Surge Rush", "Score surges last 2x longer", "[2x]"),
  TIGHT_SQUEEZE("Tight Squeeze", "Near-miss zone is even tighter", "[.]")
}

/**
 * Represents today's daily challenge.
 */
data class DailyChallenge(
  val date: String,
  val seed: Long,
  val modifier: DailyModifier,
  val highScore: Int = 0
) {
  companion object {
    /**
     * Generate today's challenge deterministically from the date.
     */
    fun today(): DailyChallenge {
      val date = LocalDate.now()
      val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
      val seed = dateStr.hashCode().toLong()
      val modifiers = DailyModifier.entries
      val modifier = modifiers[Random(seed).nextInt(modifiers.size)]
      return DailyChallenge(date = dateStr, seed = seed, modifier = modifier)
    }
  }
}
