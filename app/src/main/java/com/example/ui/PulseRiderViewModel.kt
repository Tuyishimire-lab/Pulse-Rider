package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.CyberSynthAudio
import com.example.audio.HapticFeedbackHelper
import com.example.data.AppDatabase
import com.example.data.GamePreferences
import com.example.data.RunRecord
import com.example.game.GameEngine
import com.example.game.GameScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameUiState(
  val isSoundFxEnabled: Boolean = true,
  val isMusicSynthEnabled: Boolean = true,
  val isHapticsEnabled: Boolean = true,
  val localHighScore: Int = 0,
  val showLeaderboard: Boolean = false,
  val lastRunRecord: RunRecord? = null,
  val isNewHighScore: Boolean = false
)

class PulseRiderViewModel(application: Application) : AndroidViewModel(application) {
  private val database = AppDatabase.getDatabase(application)
  private val runDao = database.runDao()
  val preferences = GamePreferences(application)

  val audio = CyberSynthAudio(viewModelScope)
  val haptics = HapticFeedbackHelper(application)
  val gameEngine = GameEngine(audio, haptics)

  private val _uiState = MutableStateFlow(
    GameUiState(
      isSoundFxEnabled = preferences.soundFxEnabled,
      isMusicSynthEnabled = preferences.musicSynthEnabled,
      isHapticsEnabled = preferences.hapticsEnabled,
      localHighScore = preferences.localHighScore
    )
  )
  val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

  val topRuns: StateFlow<List<RunRecord>> = runDao.getTopRuns()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val totalRunsCount: StateFlow<Int> = runDao.getTotalRunsCount()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val totalNearMisses: StateFlow<Int?> = runDao.getTotalNearMisses()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val bestStreak: StateFlow<Int?> = runDao.getBestStreak()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

  init {
    audio.sfxEnabled = preferences.soundFxEnabled
    audio.musicEnabled = preferences.musicSynthEnabled
    haptics.enabled = preferences.hapticsEnabled
  }

  fun toggleSoundFx() {
    val newValue = !preferences.soundFxEnabled
    preferences.soundFxEnabled = newValue
    audio.sfxEnabled = newValue
    _uiState.value = _uiState.value.copy(isSoundFxEnabled = newValue)
  }

  fun toggleMusicSynth() {
    val newValue = !preferences.musicSynthEnabled
    preferences.musicSynthEnabled = newValue
    audio.musicEnabled = newValue
    _uiState.value = _uiState.value.copy(isMusicSynthEnabled = newValue)
  }

  fun toggleHaptics() {
    val newValue = !preferences.hapticsEnabled
    preferences.hapticsEnabled = newValue
    haptics.enabled = newValue
    _uiState.value = _uiState.value.copy(isHapticsEnabled = newValue)
  }

  fun setShowLeaderboard(show: Boolean) {
    _uiState.value = _uiState.value.copy(showLeaderboard = show)
  }

  fun handleGameOver(finalScore: Int, nearMisses: Int, maxStreak: Int, distance: Int, phase: Int) {
    val isNewBest = finalScore > preferences.localHighScore
    if (isNewBest) {
      preferences.localHighScore = finalScore
      _uiState.value = _uiState.value.copy(localHighScore = finalScore)
    }

    val record = RunRecord(
      score = finalScore,
      nearMissCount = nearMisses,
      maxStreak = maxStreak,
      distance = distance,
      phaseReached = phase
    )

    _uiState.value = _uiState.value.copy(
      lastRunRecord = record,
      isNewHighScore = isNewBest
    )

    viewModelScope.launch {
      runDao.insertRun(record)
    }
  }

  fun shareScoreCard(context: Context, score: Int, streak: Int, nearMisses: Int, phaseName: String) {
    val shareText = """
      ⚡ PULSE RIDER RUN ⚡
      ━━━━━━━━━━━━━━━━━━━━
      🏆 Score: $score
      🔥 Max Streak: x$streak
      ⚡ Near-Misses: $nearMisses
      🌐 Reached: Phase $phaseName
      
      Can you ride the pulse further? #PulseRider #Cyberpunk
    """.trimIndent()

    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, shareText)
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Pulse Rider Run")
    context.startActivity(shareIntent)
  }

  override fun onCleared() {
    super.onCleared()
    audio.release()
  }
}
