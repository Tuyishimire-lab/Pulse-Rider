package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
  @Insert
  suspend fun insertRun(record: RunRecord): Long

  @Query("SELECT * FROM run_records ORDER BY score DESC LIMIT 10")
  fun getTopRuns(): Flow<List<RunRecord>>

  @Query("SELECT * FROM run_records ORDER BY timestamp DESC LIMIT 20")
  fun getRecentRuns(): Flow<List<RunRecord>>

  @Query("SELECT MAX(score) FROM run_records")
  fun getHighScore(): Flow<Int?>

  @Query("SELECT MAX(maxStreak) FROM run_records")
  fun getBestStreak(): Flow<Int?>

  @Query("SELECT SUM(nearMissCount) FROM run_records")
  fun getTotalNearMisses(): Flow<Int?>

  @Query("SELECT COUNT(*) FROM run_records")
  fun getTotalRunsCount(): Flow<Int>
}
