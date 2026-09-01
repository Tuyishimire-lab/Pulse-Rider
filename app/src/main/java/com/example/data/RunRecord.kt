package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_records")
data class RunRecord(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val score: Int,
  val nearMissCount: Int,
  val maxStreak: Int,
  val distance: Int,
  val phaseReached: Int,
  val timestamp: Long = System.currentTimeMillis()
)
