package com.example.game

enum class Achievement(
  val title: String,
  val description: String,
  val icon: String
) {
  FIRST_RIDE("First Ride", "Complete your first run", "I"),
  STREAK_5("Hot Streak", "Reach a x5 multiplier streak", "V"),
  STREAK_10("Untouchable", "Reach the maximum x10 streak", "X"),
  SCORE_1000("Kilovolt", "Score 1,000 points", "1K"),
  SCORE_5000("High Voltage", "Score 5,000 points", "5K"),
  SCORE_10000("Megawatt", "Score 10,000 points", "10K"),
  NEAR_MISS_50("Daredevil", "Accumulate 50 total near-misses", "50"),
  NEAR_MISS_200("Death Wish", "Accumulate 200 total near-misses", "200"),
  PHASE_3("Warp Speed", "Reach Phase 3 - WARP SHIFT", "P3"),
  PHASE_4("Singularity", "Reach Phase 4 - SINGULARITY", "P4"),
  SHIELD_3("Shield Wall", "Collect 3 shields in a single run", "S3"),
  NO_SHIELD("Naked Run", "Score 2,000+ without using any shield", "NR"),
  RUNS_10("Regular Rider", "Complete 10 total runs", "10"),
  RUNS_50("Veteran Pulse", "Complete 50 total runs", "50"),
  SURGE_MASTER("Surge Master", "Collect 3 score surges in a single run", "x6")
}
