package com.example.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.audio.CyberSynthAudio
import com.example.audio.HapticFeedbackHelper
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(
  private val audio: CyberSynthAudio?,
  private val haptics: HapticFeedbackHelper?
) {
  private val idGenerator = AtomicLong(1)

  // Screen dimensions
  var screenWidth: Float = 1080f
  var screenHeight: Float = 2160f

  // Rail vertical positions
  var topRailY: Float = 0f
  var bottomRailY: Float = 0f
  var railSpacing: Float = 0f

  // Player State
  var playerX: Float = 0f
  var playerY: Float = 0f
  var playerTargetLane: Lane = Lane.TOP
  var playerRadius: Float = 36f
  private var playerVelocityY: Float = 0f
  private var gestureBoost: Float = 1.0f

  // Active Power-ups
  var hasShield: Boolean by mutableStateOf(false)
  var slowMoTimer: Float by mutableFloatStateOf(0f)
  var scoreSurgeTimer: Float by mutableFloatStateOf(0f)

  // Trails and FX
  val playerTrail = mutableListOf<TrailPoint>()
  val particles = mutableListOf<Particle>()
  val floatingTexts = mutableListOf<FloatingText>()
  val obstacles = mutableListOf<Obstacle>()
  val powerUps = mutableListOf<PowerUp>()

  // Game Progress
  var state: GameScreenState by mutableStateOf(GameScreenState.ATTRACT)
  var score: Double by mutableDoubleStateOf(0.0)
  var distanceTraveled: Double by mutableDoubleStateOf(0.0)
  var currentMultiplier: Int by mutableIntStateOf(1)
  var maxStreak: Int by mutableIntStateOf(1)
  var nearMissCount: Int by mutableIntStateOf(0)
  var currentPhase: GamePhase by mutableStateOf(GamePhases.first())

  // Slow-Mo & Juice Timers
  var timeScale: Float = 1.0f
  private var slowMoEffectTimer: Float = 0f
  var screenShakeTrauma: Float = 0f
  var chromaticFlash: Float = 0f
  var speedLinesIntensity: Float = 0f

  // Speed-driven visual state
  var currentSpeedMultiplier: Float = 1.0f
  var gridScrollPhase: Float = 0f

  // Spawning controls
  private var nextObstacleSpawnX: Float = 0f
  private var nextPowerUpSpawnDistance: Float = 400f
  private var totalObstaclesSpawned: Int = 0

  // Milestone Celebration
  var currentMilestoneText: String? by mutableStateOf(null)
  var milestoneBannerAlpha: Float by mutableFloatStateOf(0f)
  private val reachedMilestones = mutableSetOf<Int>()

  // Combo Timer — streak decays over time
  var comboTimer: Float = 0f
  val comboWindow: Float = 3.0f
  var comboTimerFraction: Float by mutableFloatStateOf(0f)

  // Per-run achievement counters
  var shieldsCollectedThisRun: Int = 0
  var surgesCollectedThisRun: Int = 0
  var usedShieldThisRun: Boolean = false

  // Game-over restart cooldown (prevents accidental restarts)
  private var gameOverTimestamp: Long = 0L
  private val gameOverCooldownMs: Long = 600L

  // Ghost Run Recording & Playback
  private val ghostFrames = mutableListOf<GhostFrame>()
  private var ghostRecordTick: Int = 0
  private var runElapsedTime: Float = 0f
  var activeGhost: GhostRecording? = null
  var ghostPlaybackTime: Float = 0f
  var ghostY: Float = 0f
  var lastGhostRecording: GhostRecording? = null
    private set

  // Daily Challenge
  var isDailyChallenge: Boolean = false
    private set
  var dailyModifier: DailyModifier? = null
    private set
  var dailyScoreMultiplier: Int = 1
    private set

  fun updateDimensions(width: Float, height: Float) {
    screenWidth = width
    screenHeight = height

    // Rails are positioned nicely in the central third of the screen
    val centerY = height * 0.48f
    railSpacing = height * 0.16f
    topRailY = centerY - (railSpacing / 2f)
    bottomRailY = centerY + (railSpacing / 2f)

    playerX = width * 0.22f
    playerRadius = (width * 0.038f).coerceIn(24f, 44f)

    if (state == GameScreenState.ATTRACT && playerY == 0f) {
      playerY = topRailY
    }
  }

  fun startNewGame(dailyChallenge: DailyChallenge? = null) {
    state = GameScreenState.PLAYING
    score = 0.0
    distanceTraveled = 0.0
    currentMultiplier = 1
    maxStreak = 1
    nearMissCount = 0

    // Daily challenge setup
    isDailyChallenge = dailyChallenge != null
    dailyModifier = dailyChallenge?.modifier
    dailyScoreMultiplier = if (dailyModifier == DailyModifier.GLASS_CANNON) 3 else 1

    // SPEED_DEMON: start at Phase 2
    currentPhase = if (dailyModifier == DailyModifier.SPEED_DEMON) {
      GamePhases[1.coerceAtMost(GamePhases.lastIndex)]
    } else {
      GamePhases.first()
    }

    playerTargetLane = Lane.TOP
    playerY = topRailY
    playerVelocityY = 0f

    hasShield = false
    slowMoTimer = 0f
    scoreSurgeTimer = 0f
    slowMoEffectTimer = 0f
    timeScale = 1.0f
    screenShakeTrauma = 0f
    chromaticFlash = 0f
    gestureBoost = 1.0f
    gridScrollPhase = 0f
    currentSpeedMultiplier = 1.0f
    gameOverTimestamp = 0L
    comboTimer = 0f
    comboTimerFraction = 0f
    shieldsCollectedThisRun = 0
    surgesCollectedThisRun = 0
    usedShieldThisRun = false
    ghostFrames.clear()
    ghostRecordTick = 0
    runElapsedTime = 0f
    ghostPlaybackTime = 0f
    ghostY = topRailY
    lastGhostRecording = null

    playerTrail.clear()
    particles.clear()
    floatingTexts.clear()
    obstacles.clear()
    powerUps.clear()
    reachedMilestones.clear()
    currentMilestoneText = null
    milestoneBannerAlpha = 0f

    nextObstacleSpawnX = screenWidth + 300f
    nextPowerUpSpawnDistance = 600f
    totalObstaclesSpawned = 0

    audio?.startRhythm(1.0f)
  }

  fun pauseGame() {
    if (state == GameScreenState.PLAYING) {
      state = GameScreenState.PAUSED
      audio?.stopRhythm()
    }
  }

  fun resumeGame() {
    if (state == GameScreenState.PAUSED) {
      state = GameScreenState.PLAYING
      audio?.startRhythm(currentSpeedMultiplier)
    }
  }

  fun togglePause() {
    if (state == GameScreenState.PLAYING) {
      pauseGame()
    } else if (state == GameScreenState.PAUSED) {
      resumeGame()
    }
  }

  fun quitToMenu() {
    audio?.stopRhythm()
    state = GameScreenState.ATTRACT
    playerTrail.clear()
    particles.clear()
    floatingTexts.clear()
    obstacles.clear()
    powerUps.clear()
  }

  fun onScreenTapped() {
    when (state) {
      GameScreenState.ATTRACT -> {
        startNewGame()
      }
      GameScreenState.PLAYING -> {
        toggleLane(1.0f)
      }
      GameScreenState.PAUSED -> {
        // Paused state handled by pause overlay UI
      }
      GameScreenState.GAME_OVER -> {
        if (System.currentTimeMillis() - gameOverTimestamp >= gameOverCooldownMs) {
          startNewGame()
        }
      }
    }
  }

  /**
   * Directional swipe gesture: moves to the lane matching swipe direction.
   * @param velocityY vertical velocity of the swipe (positive = downward)
   * @param swipeDistanceY total vertical distance of the swipe
   */
  fun onSwipeGesture(velocityY: Float, swipeDistanceY: Float) {
    if (state != GameScreenState.PLAYING) {
      if (state == GameScreenState.GAME_OVER &&
          System.currentTimeMillis() - gameOverTimestamp < gameOverCooldownMs) {
        return
      }
      onScreenTapped()
      return
    }

    val targetLane = if (swipeDistanceY > 0) {
      if (dailyModifier == DailyModifier.MIRROR_MODE) Lane.TOP else Lane.BOTTOM
    } else {
      if (dailyModifier == DailyModifier.MIRROR_MODE) Lane.BOTTOM else Lane.TOP
    }

    // Only switch if we're not already on the target lane
    if (targetLane == playerTargetLane) return

    // Gesture speed influences transition snappiness (normalized to 0.5..2.5 range)
    val speed = kotlin.math.abs(velocityY).coerceIn(200f, 4000f)
    val normalizedBoost = 0.5f + (speed - 200f) / (4000f - 200f) * 2.0f

    // Give the player an initial velocity kick in the swipe direction
    val kickDirection = if (swipeDistanceY > 0) 1f else -1f
    playerVelocityY += kickDirection * speed * 0.4f

    toggleLane(normalizedBoost)
  }

  private fun toggleLane(boost: Float = 1.0f) {
    playerTargetLane = playerTargetLane.toggle()
    gestureBoost = boost
    audio?.playPhaseShift(playerTargetLane == Lane.TOP)
    haptics?.pulseShift()

    // Spawn lane shift spark particles
    spawnPhaseShiftParticles()
  }

  fun update(realDeltaTime: Float) {
    if (realDeltaTime <= 0f || state == GameScreenState.PAUSED) return

    // Cap delta time to prevent spiraling after pauses
    val dtClamped = realDeltaTime.coerceIn(0.001f, 0.05f)

    // Handle Slow-Mo time dilation (e.g. on near-miss or powerup)
    if (slowMoEffectTimer > 0f) {
      slowMoEffectTimer -= dtClamped
      timeScale = 0.35f
      if (slowMoEffectTimer <= 0f) {
        timeScale = if (slowMoTimer > 0f) 0.6f else 1.0f
      }
    } else {
      timeScale = if (slowMoTimer > 0f) 0.6f else 1.0f
    }

    val gameDt = dtClamped * timeScale

    // Update Juice / Shake
    if (screenShakeTrauma > 0f) {
      screenShakeTrauma = (screenShakeTrauma - dtClamped * 2.2f).coerceAtLeast(0f)
    }
    if (chromaticFlash > 0f) {
      chromaticFlash = (chromaticFlash - dtClamped * 2.8f).coerceAtLeast(0f)
    }
    if (milestoneBannerAlpha > 0f) {
      milestoneBannerAlpha = (milestoneBannerAlpha - dtClamped * 0.8f).coerceAtLeast(0f)
      if (milestoneBannerAlpha == 0f) {
        currentMilestoneText = null
      }
    }

    // Power-up active timers
    if (slowMoTimer > 0f) {
      slowMoTimer = (slowMoTimer - dtClamped).coerceAtLeast(0f)
    }
    if (scoreSurgeTimer > 0f) {
      scoreSurgeTimer = (scoreSurgeTimer - dtClamped).coerceAtLeast(0f)
    }

    // Combo timer decay — streak resets when timer expires
    if (comboTimer > 0f) {
      comboTimer = (comboTimer - dtClamped).coerceAtLeast(0f)
      comboTimerFraction = comboTimer / comboWindow
      if (comboTimer <= 0f) {
        currentMultiplier = 1
        comboTimerFraction = 0f
      }
    }

    if (state == GameScreenState.PLAYING) {
      updatePlayingState(gameDt, dtClamped)
    } else if (state == GameScreenState.ATTRACT) {
      updateAttractState(gameDt)
    }

    // Update particles & floating texts regardless of state
    updateParticles(gameDt)
    updateFloatingTexts(gameDt)
  }

  private fun updatePlayingState(gameDt: Float, realDt: Float) {
    // Current Phase and Speed
    val scoreInt = score.toInt()
    val phaseIndex = when {
      scoreInt >= 3000 -> 3
      scoreInt >= 1500 -> 2
      scoreInt >= 500 -> 1
      else -> 0
    }
    currentPhase = GamePhases[phaseIndex]

    val baseSpeed = currentPhase.baseSpeed
    val speedMultiplier = 1.0f + (scoreInt / 5000f).coerceAtMost(1.0f)
    currentSpeedMultiplier = speedMultiplier
    val effectiveSpeed = baseSpeed * speedMultiplier * (if (slowMoTimer > 0f) 0.65f else 1.0f)

    audio?.updateSpeed(speedMultiplier)

    // Increase Distance & Score
    val distanceStep = effectiveSpeed * gameDt
    distanceTraveled += distanceStep

    val surgeBonus = if (scoreSurgeTimer > 0f) 2 else 1
    val frameScore = (effectiveSpeed * 0.035f * currentMultiplier * surgeBonus * dailyScoreMultiplier * gameDt).toDouble()
    score += frameScore

    // Check milestones (500, 1000, 2500, 5000, 7500, 10000)
    checkMilestones(scoreInt)

    // Smoothly interpolate player Y to target rail with gesture-responsive spring
    val targetY = if (playerTargetLane == Lane.TOP) topRailY else bottomRailY
    val springStiffness = 18f * gestureBoost.coerceIn(0.8f, 2.5f)
    val springDamping = 8.5f * gestureBoost.coerceIn(0.9f, 1.8f)
    val displacement = targetY - playerY
    val springForce = displacement * springStiffness
    val dampingForce = playerVelocityY * springDamping
    playerVelocityY += (springForce - dampingForce) * gameDt
    playerY += playerVelocityY * gameDt

    // Decay gesture boost back to neutral
    gestureBoost = (gestureBoost + (1.0f - gestureBoost) * 4f * gameDt).coerceIn(0.5f, 2.5f)

    // Ghost: record frame every 3 ticks
    runElapsedTime += gameDt
    ghostRecordTick++
    if (ghostRecordTick % 3 == 0) {
      ghostFrames.add(GhostFrame(y = playerY, lane = playerTargetLane.index, elapsedTime = runElapsedTime))
    }

    // Ghost: advance playback
    val ghost = activeGhost
    if (ghost != null && ghost.frames.isNotEmpty()) {
      ghostPlaybackTime += gameDt
      val frame = ghost.frames.lastOrNull { it.elapsedTime <= ghostPlaybackTime }
      if (frame != null) {
        ghostY = frame.y
      }
    }

    // Update speed lines intensity
    speedLinesIntensity = ((effectiveSpeed - 400f) / 700f).coerceIn(0.2f, 1.0f)

    // Update grid scroll phase driven by game speed
    gridScrollPhase += effectiveSpeed * 0.002f * gameDt

    // Record trail history
    recordPlayerTrail()

    // Spawn and update obstacles
    spawnObstacles(effectiveSpeed, distanceStep)
    updateObstacles(effectiveSpeed, gameDt)

    // Spawn and update power-ups
    spawnPowerUps(distanceStep)
    updatePowerUps(effectiveSpeed, gameDt)

    // Ambient cyber particles
    spawnAmbientParticles()

    // Speed-based trail sparks at high speeds
    if (speedMultiplier > 1.2f && Random.nextFloat() < (speedMultiplier - 1.0f) * 0.5f) {
      particles.add(
        Particle(
          x = playerX - 15f - Random.nextFloat() * 20f,
          y = playerY + (Random.nextFloat() - 0.5f) * 16f,
          vx = -Random.nextFloat() * 250f - 100f,
          vy = (Random.nextFloat() - 0.5f) * 60f,
          radius = Random.nextFloat() * 4f + 1.5f,
          alpha = 0.9f,
          maxLife = 0.25f,
          currentLife = 0.25f,
          color = currentPhase.primaryColor,
          type = ParticleType.TRAIL_ORB
        )
      )
    }
  }

  private fun updateAttractState(gameDt: Float) {
    // Gentle sine wave bobbing in attract mode
    val timeSec = System.currentTimeMillis() / 1000.0
    val targetY = if (sin(timeSec * 1.5) > 0) topRailY else bottomRailY
    val displacement = targetY - playerY
    playerY += displacement * 8f * gameDt
    recordPlayerTrail()
    spawnAmbientParticles()
  }

  private fun recordPlayerTrail() {
    val trailColor = if (scoreSurgeTimer > 0f) NeonMagenta else currentPhase.primaryColor
    playerTrail.add(
      0,
      TrailPoint(
        x = playerX,
        y = playerY,
        timestamp = System.currentTimeMillis(),
        color = trailColor
      )
    )

    // Keep trail length constrained — scales with speed
    val maxTrailLength = (18 + (currentSpeedMultiplier - 1.0f) * 22f).toInt().coerceIn(18, 40)
    while (playerTrail.size > maxTrailLength) {
      playerTrail.removeAt(playerTrail.lastIndex)
    }
  }

  private fun spawnObstacles(effectiveSpeed: Float, distanceStep: Float) {
    nextObstacleSpawnX -= distanceStep

    if (nextObstacleSpawnX <= screenWidth + 200f) {
      val scoreInt = score.toInt()
      // MARATHON modifier: wider spawn distances
      val marathonFactor = if (dailyModifier == DailyModifier.MARATHON) 1.5f else 1.0f
      val spawnGap = (Random.nextFloat() * (currentPhase.maxSpawnDistance - currentPhase.minSpawnDistance) + currentPhase.minSpawnDistance) * marathonFactor
      nextObstacleSpawnX = (screenWidth + 200f).coerceAtLeast(nextObstacleSpawnX + spawnGap)

      val blockWidth = (screenWidth * 0.12f).coerceIn(70f, 130f)
      val blockHeight = (railSpacing * 0.72f).coerceIn(80f, 180f)

      // Obstacle type variation
      val isShimmer = currentPhase.phaseNumber >= 3 && Random.nextFloat() < 0.28f
      val obstacleType = if (isShimmer) ObstacleType.SHIMMER_GHOST else {
        if (Random.nextFloat() < 0.35f) ObstacleType.LASER_BARRIER else ObstacleType.STANDARD_BLOCK
      }

      // Pattern selection
      val patternRoll = Random.nextFloat()
      val laneChoice = if (Random.nextBoolean()) Lane.TOP else Lane.BOTTOM

      if (currentPhase.phaseNumber >= 2 && patternRoll < 0.30f) {
        // Double Obstacle sequence (top then bottom with tight gap)
        obstacles.add(
          Obstacle(
            id = idGenerator.getAndIncrement(),
            lane = laneChoice,
            x = nextObstacleSpawnX,
            width = blockWidth,
            height = blockHeight,
            type = obstacleType,
            color = currentPhase.secondaryColor
          )
        )
        // Opposite lane close behind
        val tightGap = (effectiveSpeed * 0.42f).coerceIn(240f, 400f)
        obstacles.add(
          Obstacle(
            id = idGenerator.getAndIncrement(),
            lane = laneChoice.toggle(),
            x = nextObstacleSpawnX + tightGap,
            width = blockWidth,
            height = blockHeight,
            type = ObstacleType.STANDARD_BLOCK,
            color = currentPhase.secondaryColor
          )
        )
        nextObstacleSpawnX += tightGap + spawnGap * 0.5f
      } else {
        // Single Obstacle
        obstacles.add(
          Obstacle(
            id = idGenerator.getAndIncrement(),
            lane = laneChoice,
            x = nextObstacleSpawnX,
            width = blockWidth,
            height = blockHeight,
            type = obstacleType,
            color = currentPhase.secondaryColor
          )
        )
      }

      totalObstaclesSpawned++
    }
  }

  private fun updateObstacles(speed: Float, dt: Float) {
    val iterator = obstacles.iterator()

    while (iterator.hasNext()) {
      val obs = iterator.next()
      obs.x -= speed * dt

      if (obs.type == ObstacleType.SHIMMER_GHOST) {
        obs.shimmerPhase += dt * 8f
      }

      val obstacleRailY = if (obs.lane == Lane.TOP) topRailY else bottomRailY
      val obsLeft = obs.x - obs.width / 2f
      val obsRight = obs.x + obs.width / 2f
      val obsTop = obstacleRailY - obs.height / 2f
      val obsBottom = obstacleRailY + obs.height / 2f

      // Collision Detection: Circle vs AABB (Player radius)
      val closestX = playerX.coerceIn(obsLeft, obsRight)
      val closestY = playerY.coerceIn(obsTop, obsBottom)
      val dx = playerX - closestX
      val dy = playerY - closestY
      val distanceSq = dx * dx + dy * dy

      if (distanceSq < (playerRadius * 0.85f) * (playerRadius * 0.85f)) {
        // Collision happened!
        if (obs.type == ObstacleType.SHIMMER_GHOST) {
          // Fake-out obstacle phases through!
          spawnShimmerPhaseThroughParticles(obs.x, obstacleRailY)
          iterator.remove()
          continue
        }

        if (hasShield) {
          // Shield absorbs the blow!
          hasShield = false
          screenShakeTrauma = 0.6f
          audio?.playShieldDeflect()
          haptics?.powerUp()
          spawnShieldBreakParticles(playerX, playerY)
          floatingTexts.add(
            FloatingText(
              id = idGenerator.getAndIncrement(),
              text = "SHIELD ABSORBED",
              x = playerX - 15f,
              y = topRailY - 35f,
              color = NeonCyan,
              scale = 0.85f
            )
          )
          usedShieldThisRun = true
          iterator.remove()
          continue
        } else {
          // Player dies!
          triggerGameOver()
          return
        }
      }

      // Near-Miss Detection:
      // When obstacle passes the player (obsRight < playerX) and was not colliding, check if player was close
      if (!obs.hasCheckedNearMiss && obs.x < playerX + 15f) {
        obs.hasCheckedNearMiss = true

        // Distance from player to the obstacle bounding box
        val distToCenter = sqrt((playerX - obs.x) * (playerX - obs.x) + (playerY - obstacleRailY) * (playerY - obstacleRailY))
        val nearMissMultiplier = if (dailyModifier == DailyModifier.TIGHT_SQUEEZE) 0.55f else 0.75f
        val isTightDodge = distToCenter < (railSpacing * nearMissMultiplier)

        if (isTightDodge && obs.type != ObstacleType.SHIMMER_GHOST) {
          triggerNearMiss(obs.x, obstacleRailY)
        }
      }

      // Remove off-screen obstacles
      if (obs.x < -200f) {
        iterator.remove()
      }
    }
  }

  private fun triggerNearMiss(obsX: Float, obsY: Float) {
    nearMissCount++
    currentMultiplier = (currentMultiplier + 1).coerceAtMost(10)
    comboTimer = comboWindow
    comboTimerFraction = 1.0f
    if (currentMultiplier > maxStreak) {
      maxStreak = currentMultiplier
    }

    val nearMissBonus = 50 * currentMultiplier
    score += nearMissBonus

    // Slow-Mo time dilation for 200ms
    slowMoEffectTimer = 0.22f
    screenShakeTrauma = (screenShakeTrauma + 0.35f).coerceAtMost(0.8f)

    audio?.playNearMiss(currentMultiplier)
    haptics?.nearMiss()

    // Near miss particle burst
    spawnNearMissParticles(playerX, (playerY + obsY) / 2f)

    // Floating text — positioned above the track trailing behind player
    floatingTexts.add(
      FloatingText(
        id = idGenerator.getAndIncrement(),
        text = "+$nearMissBonus x$currentMultiplier",
        x = playerX - 15f,
        y = topRailY - 35f,
        color = NeonAmber,
        scale = 0.85f
      )
    )
  }

  private fun spawnPowerUps(distanceStep: Float) {
    nextPowerUpSpawnDistance -= distanceStep

    if (nextPowerUpSpawnDistance <= 0f) {
      // Spawn a random powerup
      val typeRoll = Random.nextFloat()
      val type = when {
        // NO_SHIELDS / GLASS_CANNON: skip shield spawns
        dailyModifier == DailyModifier.NO_SHIELDS || dailyModifier == DailyModifier.GLASS_CANNON -> {
          if (typeRoll < 0.55f) PowerUpType.SLOW_MO else PowerUpType.SCORE_SURGE
        }
        typeRoll < 0.40f -> PowerUpType.SHIELD
        typeRoll < 0.70f -> PowerUpType.SLOW_MO
        else -> PowerUpType.SCORE_SURGE
      }

      val lane = if (Random.nextBoolean()) Lane.TOP else Lane.BOTTOM
      powerUps.add(
        PowerUp(
          id = idGenerator.getAndIncrement(),
          lane = lane,
          x = screenWidth + 200f,
          type = type
        )
      )

      nextPowerUpSpawnDistance = Random.nextFloat() * 800f + 700f
    }
  }

  private fun updatePowerUps(speed: Float, dt: Float) {
    val iterator = powerUps.iterator()

    while (iterator.hasNext()) {
      val pup = iterator.next()
      pup.x -= speed * dt
      pup.pulsePhase += dt * 6f

      val pupRailY = if (pup.lane == Lane.TOP) topRailY else bottomRailY
      val dist = sqrt((playerX - pup.x) * (playerX - pup.x) + (playerY - pupRailY) * (playerY - pupRailY))

      if (dist < playerRadius + 30f) {
        // Collect Power-up!
        pup.isCollected = true
        applyPowerUp(pup.type)
        spawnPowerUpCollectParticles(pup.x, pupRailY, pup.type.color)
        iterator.remove()
        continue
      }

      if (pup.x < -100f) {
        iterator.remove()
      }
    }
  }

  private fun applyPowerUp(type: PowerUpType) {
    audio?.playPowerUpCollect()
    haptics?.powerUp()

    when (type) {
      PowerUpType.SHIELD -> {
        hasShield = true
        shieldsCollectedThisRun++
        floatingTexts.add(
          FloatingText(
            id = idGenerator.getAndIncrement(),
            text = "SHIELD ACTIVE",
            x = playerX - 15f,
            y = topRailY - 35f,
            color = NeonCyan,
            scale = 0.85f
          )
        )
      }
      PowerUpType.SLOW_MO -> {
        slowMoTimer = type.durationSeconds
        floatingTexts.add(
          FloatingText(
            id = idGenerator.getAndIncrement(),
            text = "SLOW-MO ACTIVE",
            x = playerX - 15f,
            y = topRailY - 35f,
            color = NeonAmber,
            scale = 0.85f
          )
        )
      }
      PowerUpType.SCORE_SURGE -> {
        // SURGE_RUSH modifier: double duration
        val surgeDuration = if (dailyModifier == DailyModifier.SURGE_RUSH) type.durationSeconds * 2f else type.durationSeconds
        scoreSurgeTimer = surgeDuration
        surgesCollectedThisRun++
        floatingTexts.add(
          FloatingText(
            id = idGenerator.getAndIncrement(),
            text = "2X SURGE ACTIVE",
            x = playerX - 15f,
            y = topRailY - 35f,
            color = NeonMagenta,
            scale = 0.85f
          )
        )
      }
    }
  }

  private fun checkMilestones(scoreInt: Int) {
    val milestoneList = listOf(500, 1000, 2500, 5000, 7500, 10000, 15000, 20000)
    for (m in milestoneList) {
      if (scoreInt >= m && !reachedMilestones.contains(m)) {
        reachedMilestones.add(m)
        currentMilestoneText = ">> $m MILESTONE REACHED <<"
        milestoneBannerAlpha = 1.0f
        audio?.playMilestone()
        haptics?.powerUp()
        spawnMilestoneCelebrationParticles()
        break
      }
    }
  }

  private fun triggerGameOver() {
    state = GameScreenState.GAME_OVER
    gameOverTimestamp = System.currentTimeMillis()
    screenShakeTrauma = 1.0f
    chromaticFlash = 1.0f

    // Produce ghost recording for the ViewModel to save
    lastGhostRecording = GhostRecording(
      frames = ghostFrames.toList(),
      finalScore = score.toInt(),
      totalDuration = runElapsedTime
    )

    audio?.stopRhythm()
    audio?.playDeathCrash()
    haptics?.crash()

    // Spawn massive explosive debris shockwave
    spawnDeathExplosionParticles(playerX, playerY)
  }

  // --- PARTICLES ENGINE ---

  private fun updateParticles(dt: Float) {
    val iterator = particles.iterator()
    while (iterator.hasNext()) {
      val p = iterator.next()
      p.currentLife -= dt
      if (p.currentLife <= 0f) {
        iterator.remove()
        continue
      }
      p.x += p.vx * dt
      p.y += p.vy * dt
      p.alpha = (p.currentLife / p.maxLife).coerceIn(0f, 1f)
    }
  }

  private fun updateFloatingTexts(dt: Float) {
    val iterator = floatingTexts.iterator()
    while (iterator.hasNext()) {
      val ft = iterator.next()
      ft.life -= dt
      ft.y -= 45f * dt
      ft.alpha = (ft.life / ft.maxLife).coerceIn(0f, 1f)
      if (ft.life <= 0f) {
        iterator.remove()
      }
    }
  }

  private fun spawnPhaseShiftParticles() {
    for (i in 0 until 18) {
      val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
      val speed = Random.nextFloat() * 280f + 80f
      particles.add(
        Particle(
          x = playerX,
          y = playerY,
          vx = cos(angle) * speed - 150f,
          vy = sin(angle) * speed,
          radius = Random.nextFloat() * 6f + 3f,
          alpha = 1.0f,
          maxLife = 0.35f,
          currentLife = 0.35f,
          color = if (playerTargetLane == Lane.TOP) NeonCyan else NeonMagenta,
          type = ParticleType.TRAIL_ORB
        )
      )
    }
  }

  private fun spawnNearMissParticles(x: Float, y: Float) {
    for (i in 0 until 35) {
      val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
      val speed = Random.nextFloat() * 450f + 120f
      particles.add(
        Particle(
          x = x,
          y = y,
          vx = cos(angle) * speed - 120f,
          vy = sin(angle) * speed,
          radius = Random.nextFloat() * 8f + 4f,
          alpha = 1.0f,
          maxLife = 0.55f,
          currentLife = 0.55f,
          color = if (Random.nextBoolean()) NeonAmber else NeonCyan,
          type = ParticleType.NEAR_MISS_BURST
        )
      )
    }
  }

  private fun spawnDeathExplosionParticles(x: Float, y: Float) {
    for (i in 0 until 70) {
      val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
      val speed = Random.nextFloat() * 650f + 100f
      val colors = listOf(NeonRed, NeonMagenta, NeonCyan, NeonAmber, Color.White)
      particles.add(
        Particle(
          x = x,
          y = y,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed,
          radius = Random.nextFloat() * 12f + 4f,
          alpha = 1.0f,
          maxLife = 0.85f,
          currentLife = 0.85f,
          color = colors.random(),
          type = ParticleType.DEATH_EXPLOSION
        )
      )
    }
  }

  private fun spawnShieldBreakParticles(x: Float, y: Float) {
    for (i in 0 until 28) {
      val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
      val speed = Random.nextFloat() * 380f + 90f
      particles.add(
        Particle(
          x = x,
          y = y,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed,
          radius = Random.nextFloat() * 7f + 3f,
          alpha = 1.0f,
          maxLife = 0.45f,
          currentLife = 0.45f,
          color = NeonCyan,
          type = ParticleType.SHIELD_BURST
        )
      )
    }
  }

  private fun spawnPowerUpCollectParticles(x: Float, y: Float, color: Color) {
    for (i in 0 until 22) {
      val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
      val speed = Random.nextFloat() * 320f + 80f
      particles.add(
        Particle(
          x = x,
          y = y,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed,
          radius = Random.nextFloat() * 6f + 3f,
          alpha = 1.0f,
          maxLife = 0.45f,
          currentLife = 0.45f,
          color = color,
          type = ParticleType.POWERUP_COLLECT
        )
      )
    }
  }

  private fun spawnShimmerPhaseThroughParticles(x: Float, y: Float) {
    for (i in 0 until 16) {
      particles.add(
        Particle(
          x = x + (Random.nextFloat() - 0.5f) * 60f,
          y = y + (Random.nextFloat() - 0.5f) * 60f,
          vx = -250f + (Random.nextFloat() - 0.5f) * 100f,
          vy = (Random.nextFloat() - 0.5f) * 80f,
          radius = Random.nextFloat() * 5f + 2f,
          alpha = 0.8f,
          maxLife = 0.35f,
          currentLife = 0.35f,
          color = NeonPurple,
          type = ParticleType.TRAIL_ORB
        )
      )
    }
  }

  private fun spawnMilestoneCelebrationParticles() {
    for (i in 0 until 50) {
      val x = Random.nextFloat() * screenWidth
      val y = Random.nextFloat() * screenHeight * 0.6f
      particles.add(
        Particle(
          x = x,
          y = y,
          vx = (Random.nextFloat() - 0.5f) * 200f,
          vy = -Random.nextFloat() * 250f - 50f,
          radius = Random.nextFloat() * 8f + 4f,
          alpha = 1.0f,
          maxLife = 0.9f,
          currentLife = 0.9f,
          color = listOf(NeonAmber, NeonCyan, NeonMagenta, NeonGreen).random(),
          type = ParticleType.NEAR_MISS_BURST
        )
      )
    }
  }

  private fun spawnAmbientParticles() {
    if (particles.size < 60 && Random.nextFloat() < 0.45f) {
      particles.add(
        Particle(
          x = screenWidth + 50f,
          y = Random.nextFloat() * screenHeight,
          vx = -Random.nextFloat() * 350f - 200f,
          vy = (Random.nextFloat() - 0.5f) * 30f,
          radius = Random.nextFloat() * 3.5f + 1.5f,
          alpha = Random.nextFloat() * 0.5f + 0.2f,
          maxLife = 2.5f,
          currentLife = 2.5f,
          color = currentPhase.tunnelAccent.copy(alpha = 0.6f),
          type = ParticleType.GRID_ENERGY
        )
      )
    }
  }
}
