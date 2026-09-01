package com.example.game

/**
 * A single frame of ghost playback data — position snapshot.
 */
data class GhostFrame(
  val y: Float,
  val lane: Int,
  val elapsedTime: Float
)

/**
 * A complete ghost recording of a player's run.
 */
data class GhostRecording(
  val frames: List<GhostFrame>,
  val finalScore: Int,
  val totalDuration: Float
)
