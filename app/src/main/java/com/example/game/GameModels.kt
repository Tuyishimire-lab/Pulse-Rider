package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed

enum class GameScreenState {
  ATTRACT,
  PLAYING,
  GAME_OVER
}

enum class Lane(val index: Int) {
  TOP(0),
  BOTTOM(1);

  fun toggle(): Lane = if (this == TOP) BOTTOM else TOP
}

enum class PowerUpType(
  val displayName: String,
  val color: Color,
  val iconSymbol: String,
  val durationSeconds: Float
) {
  SHIELD("SHIELD MATRIX", NeonCyan, "🛡️", 0f), // Single use
  SLOW_MO("CHRONO WARP", NeonAmber, "⏱️", 4.5f),
  SCORE_SURGE("SURGE 2X", NeonMagenta, "⚡", 5.0f)
}

enum class ObstacleType {
  STANDARD_BLOCK,
  LASER_BARRIER,
  SHIMMER_GHOST // Appears in Phase 3+
}

data class Obstacle(
  val id: Long,
  val lane: Lane,
  var x: Float,
  val width: Float,
  val height: Float,
  val type: ObstacleType = ObstacleType.STANDARD_BLOCK,
  var hasPassedPlayer: Boolean = false,
  var hasCheckedNearMiss: Boolean = false,
  var shimmerPhase: Float = 0f,
  val color: Color = NeonRed
)

data class PowerUp(
  val id: Long,
  val lane: Lane,
  var x: Float,
  val type: PowerUpType,
  var pulsePhase: Float = 0f,
  var isCollected: Boolean = false
)

enum class ParticleType {
  TRAIL_ORB,
  NEAR_MISS_BURST,
  DEATH_EXPLOSION,
  SHIELD_BURST,
  POWERUP_COLLECT,
  SPEED_LINE,
  GRID_ENERGY
}

data class Particle(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var radius: Float,
  var alpha: Float,
  val maxLife: Float,
  var currentLife: Float,
  val color: Color,
  val type: ParticleType = ParticleType.TRAIL_ORB
)

data class FloatingText(
  val id: Long,
  val text: String,
  var x: Float,
  var y: Float,
  val color: Color,
  var alpha: Float = 1.0f,
  var life: Float = 0.8f,
  val maxLife: Float = 0.8f,
  val scale: Float = 1.0f
)

data class TrailPoint(
  val x: Float,
  val y: Float,
  val timestamp: Long,
  val color: Color
)

data class GamePhase(
  val phaseNumber: Int,
  val name: String,
  val minScore: Int,
  val maxScore: Int,
  val baseSpeed: Float,
  val minSpawnDistance: Float,
  val maxSpawnDistance: Float,
  val primaryColor: Color,
  val secondaryColor: Color,
  val tunnelAccent: Color
)

val GamePhases = listOf(
  GamePhase(
    phaseNumber = 1,
    name = "SYNAPSE",
    minScore = 0,
    maxScore = 500,
    baseSpeed = 520f,
    minSpawnDistance = 460f,
    maxSpawnDistance = 620f,
    primaryColor = NeonCyan,
    secondaryColor = NeonMagenta,
    tunnelAccent = Color(0xFF00F0FF)
  ),
  GamePhase(
    phaseNumber = 2,
    name = "OVERDRIVE",
    minScore = 500,
    maxScore = 1500,
    baseSpeed = 680f,
    minSpawnDistance = 380f,
    maxSpawnDistance = 500f,
    primaryColor = NeonPurple,
    secondaryColor = NeonBlue,
    tunnelAccent = Color(0xFFA855F7)
  ),
  GamePhase(
    phaseNumber = 3,
    name = "WARP SHIFT",
    minScore = 1500,
    maxScore = 3000,
    baseSpeed = 840f,
    minSpawnDistance = 310f,
    maxSpawnDistance = 420f,
    primaryColor = NeonAmber,
    secondaryColor = NeonRed,
    tunnelAccent = Color(0xFFFFB800)
  ),
  GamePhase(
    phaseNumber = 4,
    name = "SINGULARITY",
    minScore = 3000,
    maxScore = Int.MAX_VALUE,
    baseSpeed = 1020f,
    minSpawnDistance = 260f,
    maxSpawnDistance = 360f,
    primaryColor = NeonGreen,
    secondaryColor = NeonMagenta,
    tunnelAccent = Color(0xFF00FF66)
  )
)
