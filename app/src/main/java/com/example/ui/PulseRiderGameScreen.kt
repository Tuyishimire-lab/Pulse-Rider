package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RunRecord
import com.example.game.FloatingText
import com.example.game.GameEngine
import com.example.game.GamePhase
import com.example.game.GameScreenState
import com.example.game.Lane
import com.example.game.Obstacle
import com.example.game.ObstacleType
import com.example.game.Particle
import com.example.game.ParticleType
import com.example.game.PowerUp
import com.example.game.PowerUpType
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PulseRiderGameScreen(
  viewModel: PulseRiderViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val topRuns by viewModel.topRuns.collectAsStateWithLifecycle()
  val totalRunsCount by viewModel.totalRunsCount.collectAsStateWithLifecycle()
  val totalNearMisses by viewModel.totalNearMisses.collectAsStateWithLifecycle()
  val bestStreak by viewModel.bestStreak.collectAsStateWithLifecycle()

  val engine = viewModel.gameEngine

  // Animation ticks & Game Loop State
  var lastFrameTimeNanos by remember { mutableLongStateOf(0L) }
  var frameTick by remember { mutableLongStateOf(0L) }
  var prevGameState by remember { mutableStateOf(engine.state) }

  val infiniteTransition = rememberInfiniteTransition(label = "cyberGlow")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )
  val gridScrollOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 100f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "gridScroll"
  )

  // 60 FPS Continuous Game Loop
  LaunchedEffect(Unit) {
    while (true) {
      withFrameNanos { frameTimeNanos ->
        if (lastFrameTimeNanos != 0L) {
          val dt = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f)
          engine.update(dt)
          frameTick++

          // Check if state transitioned to GAME_OVER to persist run
          if (engine.state == GameScreenState.GAME_OVER && prevGameState == GameScreenState.PLAYING) {
            viewModel.handleGameOver(
              finalScore = engine.score.toInt(),
              nearMisses = engine.nearMissCount,
              maxStreak = engine.maxStreak,
              distance = engine.distanceTraveled.toInt(),
              phase = engine.currentPhase.phaseNumber
            )
          }
          prevGameState = engine.state
        }
        lastFrameTimeNanos = frameTimeNanos
      }
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(CyberBackground)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) {
        engine.onScreenTapped()
      }
      .testTag("game_tap_area")
  ) {
    val width = constraints.maxWidth.toFloat()
    val height = constraints.maxHeight.toFloat()

    LaunchedEffect(width, height) {
      if (width > 0 && height > 0) {
        engine.updateDimensions(width, height)
      }
    }

    // --- RENDER CANVAS (GAME WORLD & EFFECTS) ---
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Screen Shake translation
      val shakeAmount = engine.screenShakeTrauma * engine.screenShakeTrauma * 24f
      val shakeX = if (shakeAmount > 0f) (Random.nextFloat() - 0.5f) * shakeAmount else 0f
      val shakeY = if (shakeAmount > 0f) (Random.nextFloat() - 0.5f) * shakeAmount else 0f

      withTransform({
        translate(shakeX, shakeY)
      }) {
        // 1. Cyberpunk Tron Perspective Grid Tunnel
        drawPerspectiveTunnel(
          width = size.width,
          height = size.height,
          phase = engine.currentPhase,
          scrollOffset = gridScrollOffset,
          speedIntensity = engine.speedLinesIntensity
        )

        // 2. Dual Energy Rails
        drawEnergyRails(
          topRailY = engine.topRailY,
          bottomRailY = engine.bottomRailY,
          width = size.width,
          phase = engine.currentPhase,
          frameTick = frameTick
        )

        // 3. Speed Lines
        drawSpeedLines(
          width = size.width,
          height = size.height,
          intensity = engine.speedLinesIntensity,
          phase = engine.currentPhase
        )

        // 4. Power-Ups
        drawPowerUps(
          powerUps = engine.powerUps,
          topRailY = engine.topRailY,
          bottomRailY = engine.bottomRailY
        )

        // 5. Obstacles
        drawObstacles(
          obstacles = engine.obstacles,
          topRailY = engine.topRailY,
          bottomRailY = engine.bottomRailY
        )

        // 6. Player Orb & Trailing Energy Ribbon
        drawPlayer(
          engine = engine,
          pulseScale = pulseScale,
          frameTick = frameTick
        )

        // 7. Dynamic Particles (Explosions, Near-Miss Bursts, Trail Sparks)
        drawParticles(particles = engine.particles)

        // 8. Floating Texts (+50 NEAR-MISS! x2, Power-up toasts)
        drawFloatingTexts(floatingTexts = engine.floatingTexts)
      }

      // 9. Chromatic Aberration & Death Flash
      if (engine.chromaticFlash > 0f) {
        drawRect(
          color = Color.White.copy(alpha = (engine.chromaticFlash * 0.4f).coerceIn(0f, 1f)),
          size = size
        )
        drawRect(
          color = NeonRed.copy(alpha = (engine.chromaticFlash * 0.35f).coerceIn(0f, 1f)),
          size = size
        )
      }

      // 10. Slow-Mo Chrono Warp Vignette Overlay
      if (engine.slowMoTimer > 0f || engine.timeScale < 0.8f) {
        drawRect(
          brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, NeonAmber.copy(alpha = 0.25f)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.width * 0.8f
          ),
          size = size
        )
      }

      // 11. Score Surge Aura Vignette Overlay
      if (engine.scoreSurgeTimer > 0f) {
        drawRect(
          brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, NeonMagenta.copy(alpha = 0.28f)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.width * 0.85f
          ),
          size = size
        )
      }
    }

    // --- HUD OVERLAY (WHEN PLAYING) ---
    if (engine.state == GameScreenState.PLAYING) {
      InGameHud(
        score = engine.score.toInt(),
        multiplier = engine.currentMultiplier,
        nearMisses = engine.nearMissCount,
        phase = engine.currentPhase,
        hasShield = engine.hasShield,
        slowMoTimer = engine.slowMoTimer,
        scoreSurgeTimer = engine.scoreSurgeTimer,
        milestoneText = engine.currentMilestoneText,
        milestoneAlpha = engine.milestoneBannerAlpha,
        modifier = Modifier.fillMaxSize()
      )
    }

    // --- ATTRACT / TITLE SCREEN OVERLAY ---
    if (engine.state == GameScreenState.ATTRACT) {
      AttractScreenOverlay(
        highScore = uiState.localHighScore,
        bestStreak = bestStreak ?: 1,
        totalRuns = totalRunsCount,
        isSoundFx = uiState.isSoundFxEnabled,
        isMusic = uiState.isMusicSynthEnabled,
        isHaptics = uiState.isHapticsEnabled,
        pulseScale = pulseScale,
        onToggleSound = { viewModel.toggleSoundFx() },
        onToggleMusic = { viewModel.toggleMusicSynth() },
        onToggleHaptics = { viewModel.toggleHaptics() },
        onShowLeaderboard = { viewModel.setShowLeaderboard(true) },
        onStart = { engine.startNewGame() },
        modifier = Modifier.fillMaxSize()
      )
    }

    // --- GAME OVER SCORE CARD OVERLAY ---
    if (engine.state == GameScreenState.GAME_OVER) {
      GameOverScoreCard(
        score = engine.score.toInt(),
        highScore = uiState.localHighScore,
        isNewBest = uiState.isNewHighScore,
        nearMisses = engine.nearMissCount,
        maxStreak = engine.maxStreak,
        distance = engine.distanceTraveled.toInt(),
        phase = engine.currentPhase,
        onRetry = { engine.startNewGame() },
        onShare = {
          viewModel.shareScoreCard(
            context = context,
            score = engine.score.toInt(),
            streak = engine.maxStreak,
            nearMisses = engine.nearMissCount,
            phaseName = engine.currentPhase.name
          )
        },
        onShowLeaderboard = { viewModel.setShowLeaderboard(true) },
        modifier = Modifier.fillMaxSize()
      )
    }

    // --- LEADERBOARD & RUN HISTORY MODAL ---
    if (uiState.showLeaderboard) {
      LeaderboardDialog(
        topRuns = topRuns,
        totalNearMisses = totalNearMisses ?: 0,
        totalRuns = totalRunsCount,
        onDismiss = { viewModel.setShowLeaderboard(false) }
      )
    }
  }
}

// ==========================================
// CANVAS DRAWING HELPERS
// ==========================================

private fun DrawScope.drawPerspectiveTunnel(
  width: Float,
  height: Float,
  phase: GamePhase,
  scrollOffset: Float,
  speedIntensity: Float
) {
  val centerY = height * 0.48f
  val horizonX = width * 0.5f

  // Gradient background space
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        CyberBackground,
        phase.tunnelAccent.copy(alpha = 0.08f),
        CyberBackground
      )
    ),
    size = Size(width, height)
  )

  // Perspective Horizon Lines (Ceiling & Floor Grid)
  val numCeilingLines = 7
  for (i in 1..numCeilingLines) {
    val yRatio = (i.toFloat() / numCeilingLines)
    val y = (centerY * (1f - yRatio * yRatio))
    val alpha = (0.05f + yRatio * 0.18f).coerceIn(0f, 0.4f)
    drawLine(
      color = phase.primaryColor.copy(alpha = alpha),
      start = Offset(0f, y),
      end = Offset(width, y),
      strokeWidth = 1.2f
    )
  }

  val numFloorLines = 7
  for (i in 1..numFloorLines) {
    val yRatio = (i.toFloat() / numFloorLines)
    val y = centerY + (height - centerY) * (yRatio * yRatio)
    val alpha = (0.05f + yRatio * 0.22f).coerceIn(0f, 0.5f)
    drawLine(
      color = phase.secondaryColor.copy(alpha = alpha),
      start = Offset(0f, y),
      end = Offset(width, y),
      strokeWidth = 1.4f
    )
  }

  // Perspective Angled Rays converging to Horizon center
  val numRays = 9
  for (i in 0 until numRays) {
    val x = (width / (numRays - 1)) * i
    drawLine(
      color = phase.primaryColor.copy(alpha = 0.12f),
      start = Offset(horizonX, centerY),
      end = Offset(x, 0f),
      strokeWidth = 1.5f
    )
    drawLine(
      color = phase.secondaryColor.copy(alpha = 0.14f),
      start = Offset(horizonX, centerY),
      end = Offset(x, height),
      strokeWidth = 1.5f
    )
  }

  // Scrolling vertical grid lines
  val colWidth = 120f
  val offsetMod = (scrollOffset * 4f) % colWidth
  var curX = -offsetMod
  while (curX < width + colWidth) {
    drawLine(
      color = phase.tunnelAccent.copy(alpha = 0.08f),
      start = Offset(curX, 0f),
      end = Offset(curX, height),
      strokeWidth = 1f,
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 25f), 0f)
    )
    curX += colWidth
  }
}

private fun DrawScope.drawEnergyRails(
  topRailY: Float,
  bottomRailY: Float,
  width: Float,
  phase: GamePhase,
  frameTick: Long
) {
  // Top Energy Rail (Glow + Bright Core)
  drawLine(
    brush = Brush.horizontalGradient(
      colors = listOf(
        phase.primaryColor.copy(alpha = 0.2f),
        phase.primaryColor,
        phase.primaryColor.copy(alpha = 0.6f)
      )
    ),
    start = Offset(0f, topRailY),
    end = Offset(width, topRailY),
    strokeWidth = 6f
  )
  drawLine(
    color = Color.White,
    start = Offset(0f, topRailY),
    end = Offset(width, topRailY),
    strokeWidth = 2f
  )

  // Bottom Energy Rail (Glow + Bright Core)
  drawLine(
    brush = Brush.horizontalGradient(
      colors = listOf(
        phase.secondaryColor.copy(alpha = 0.2f),
        phase.secondaryColor,
        phase.secondaryColor.copy(alpha = 0.6f)
      )
    ),
    start = Offset(0f, bottomRailY),
    end = Offset(width, bottomRailY),
    strokeWidth = 6f
  )
  drawLine(
    color = Color.White,
    start = Offset(0f, bottomRailY),
    end = Offset(width, bottomRailY),
    strokeWidth = 2f
  )

  // Animated Energy Nodes on Rails
  val pulseX = ((frameTick * 14) % width.toLong()).toFloat()
  drawCircle(
    color = phase.primaryColor,
    radius = 6f,
    center = Offset(pulseX, topRailY)
  )
  val pulseX2 = ((frameTick * 18 + (width / 2f).toLong()) % width.toLong()).toFloat()
  drawCircle(
    color = phase.secondaryColor,
    radius = 6f,
    center = Offset(pulseX2, bottomRailY)
  )
}

private fun DrawScope.drawSpeedLines(
  width: Float,
  height: Float,
  intensity: Float,
  phase: GamePhase
) {
  if (intensity <= 0.2f) return
  val numLines = (intensity * 14).toInt()

  for (i in 0 until numLines) {
    val seed = (i * 137)
    val y = (seed % height.toInt()).toFloat()
    val lineLen = (Random(seed).nextFloat() * 180f + 100f) * intensity
    val x = (Random(seed + 1).nextFloat() * (width - lineLen))
    drawLine(
      color = phase.primaryColor.copy(alpha = 0.25f * intensity),
      start = Offset(x, y),
      end = Offset(x + lineLen, y),
      strokeWidth = 2f,
      cap = StrokeCap.Round
    )
  }
}

private fun DrawScope.drawPowerUps(
  powerUps: List<PowerUp>,
  topRailY: Float,
  bottomRailY: Float
) {
  for (pup in powerUps) {
    val railY = if (pup.lane == com.example.game.Lane.TOP) topRailY else bottomRailY
    val radius = 26f + sin(pup.pulsePhase) * 4f

    // Outer spinning energy ring
    rotate(degrees = pup.pulsePhase * 45f, pivot = Offset(pup.x, railY)) {
      drawCircle(
        color = pup.type.color.copy(alpha = 0.4f),
        radius = radius + 8f,
        center = Offset(pup.x, railY),
        style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f))
      )
    }

    // Glowing core
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color.White, pup.type.color, Color.Transparent),
        center = Offset(pup.x, railY),
        radius = radius
      ),
      radius = radius,
      center = Offset(pup.x, railY)
    )

    // Inner icon symbol
    drawCircle(
      color = Color.White,
      radius = 7f,
      center = Offset(pup.x, railY)
    )
  }
}

private fun DrawScope.drawObstacles(
  obstacles: List<Obstacle>,
  topRailY: Float,
  bottomRailY: Float
) {
  for (obs in obstacles) {
    val railY = if (obs.lane == com.example.game.Lane.TOP) topRailY else bottomRailY
    val left = obs.x - obs.width / 2f
    val top = railY - obs.height / 2f

    val isGhost = obs.type == ObstacleType.SHIMMER_GHOST
    val alpha = if (isGhost) (0.35f + sin(obs.shimmerPhase) * 0.35f).coerceIn(0.15f, 0.85f) else 1.0f

    val baseColor = if (isGhost) NeonPurple else obs.color

    // Outer Glow Border
    drawRoundRect(
      color = baseColor.copy(alpha = 0.35f * alpha),
      topLeft = Offset(left - 6f, top - 6f),
      size = Size(obs.width + 12f, obs.height + 12f),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )

    // Solid Neon Barrier Body
    drawRoundRect(
      brush = Brush.linearGradient(
        colors = listOf(
          CyberSurfaceVariant.copy(alpha = alpha),
          baseColor.copy(alpha = 0.65f * alpha)
        )
      ),
      topLeft = Offset(left, top),
      size = Size(obs.width, obs.height),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
    )

    // Wireframe Outline
    drawRoundRect(
      color = baseColor.copy(alpha = alpha),
      topLeft = Offset(left, top),
      size = Size(obs.width, obs.height),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
      style = Stroke(width = 3.5f)
    )

    // Hazard Stripes inside obstacle
    val numStripes = 3
    for (i in 0..numStripes) {
      val stripeX = left + (obs.width / numStripes) * i
      drawLine(
        color = baseColor.copy(alpha = 0.5f * alpha),
        start = Offset(stripeX, top),
        end = Offset(stripeX - 15f, top + obs.height),
        strokeWidth = 2.5f
      )
    }

    // Bright Center Hotspot
    drawCircle(
      color = Color.White.copy(alpha = 0.8f * alpha),
      radius = 4f,
      center = Offset(obs.x, railY)
    )
  }
}

private fun DrawScope.drawPlayer(
  engine: GameEngine,
  pulseScale: Float,
  frameTick: Long
) {
  val trail = engine.playerTrail

  // 1. Trailing Energy Ribbon
  if (trail.size >= 2) {
    val path = Path()
    path.moveTo(trail[0].x, trail[0].y)

    for (i in 1 until trail.size) {
      val p0 = trail[i - 1]
      val p1 = trail[i]
      val midX = (p0.x + p1.x) / 2f
      val midY = (p0.y + p1.y) / 2f
      path.quadraticTo(p0.x, p0.y, midX, midY)
    }

    // Outer Ribbon Glow
    drawPath(
      path = path,
      brush = Brush.horizontalGradient(
        colors = listOf(
          Color.Transparent,
          engine.currentPhase.secondaryColor.copy(alpha = 0.5f),
          engine.currentPhase.primaryColor.copy(alpha = 0.85f)
        ),
        startX = (engine.playerX - 250f).coerceAtLeast(0f),
        endX = engine.playerX
      ),
      style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Inner Hot White Ribbon Core
    drawPath(
      path = path,
      brush = Brush.horizontalGradient(
        colors = listOf(
          Color.Transparent,
          Color.White.copy(alpha = 0.7f),
          Color.White
        ),
        startX = (engine.playerX - 160f).coerceAtLeast(0f),
        endX = engine.playerX
      ),
      style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
  }

  // 2. Active Shield Bubble
  if (engine.hasShield) {
    val shieldRadius = engine.playerRadius * 1.6f
    rotate(degrees = frameTick * 3f, pivot = Offset(engine.playerX, engine.playerY)) {
      drawCircle(
        color = NeonCyan.copy(alpha = 0.35f),
        radius = shieldRadius,
        center = Offset(engine.playerX, engine.playerY),
        style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f))
      )
    }
    drawCircle(
      color = NeonCyan.copy(alpha = 0.18f),
      radius = shieldRadius,
      center = Offset(engine.playerX, engine.playerY)
    )
  }

  // 3. Score Surge Flame Aura
  if (engine.scoreSurgeTimer > 0f) {
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(NeonMagenta.copy(alpha = 0.6f), Color.Transparent),
        center = Offset(engine.playerX, engine.playerY),
        radius = engine.playerRadius * 2.2f
      ),
      radius = engine.playerRadius * 2.2f,
      center = Offset(engine.playerX, engine.playerY)
    )
  }

  // 4. Player Orb Outer Glow
  val currentRadius = engine.playerRadius * pulseScale
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        engine.currentPhase.primaryColor,
        engine.currentPhase.secondaryColor.copy(alpha = 0.5f),
        Color.Transparent
      ),
      center = Offset(engine.playerX, engine.playerY),
      radius = currentRadius * 1.8f
    ),
    radius = currentRadius * 1.8f,
    center = Offset(engine.playerX, engine.playerY)
  )

  // 5. Solid Core Orb
  drawCircle(
    brush = Brush.linearGradient(
      colors = listOf(NeonCyan, NeonMagenta),
      start = Offset(engine.playerX - currentRadius, engine.playerY - currentRadius),
      end = Offset(engine.playerX + currentRadius, engine.playerY + currentRadius)
    ),
    radius = currentRadius,
    center = Offset(engine.playerX, engine.playerY)
  )

  // 6. White Hot Center Star
  drawCircle(
    color = Color.White,
    radius = currentRadius * 0.45f,
    center = Offset(engine.playerX, engine.playerY)
  )
}

private fun DrawScope.drawParticles(particles: List<Particle>) {
  for (p in particles) {
    val drawColor = p.color.copy(alpha = p.alpha)
    when (p.type) {
      ParticleType.TRAIL_ORB, ParticleType.POWERUP_COLLECT, ParticleType.GRID_ENERGY -> {
        drawCircle(
          color = drawColor,
          radius = p.radius,
          center = Offset(p.x, p.y)
        )
      }
      ParticleType.NEAR_MISS_BURST -> {
        drawCircle(
          color = Color.White.copy(alpha = p.alpha),
          radius = p.radius * 0.4f,
          center = Offset(p.x, p.y)
        )
        drawCircle(
          color = drawColor,
          radius = p.radius,
          center = Offset(p.x, p.y),
          style = Stroke(width = 2f)
        )
      }
      ParticleType.DEATH_EXPLOSION -> {
        drawCircle(
          color = drawColor,
          radius = p.radius,
          center = Offset(p.x, p.y)
        )
        // Spiky debris line
        drawLine(
          color = Color.White.copy(alpha = p.alpha * 0.7f),
          start = Offset(p.x, p.y),
          end = Offset(p.x - p.vx * 0.04f, p.y - p.vy * 0.04f),
          strokeWidth = 2.5f
        )
      }
      ParticleType.SHIELD_BURST -> {
        drawRect(
          color = drawColor,
          topLeft = Offset(p.x - p.radius, p.y - p.radius),
          size = Size(p.radius * 2f, p.radius * 2f)
        )
      }
      else -> {
        drawCircle(
          color = drawColor,
          radius = p.radius,
          center = Offset(p.x, p.y)
        )
      }
    }
  }
}

private fun DrawScope.drawFloatingTexts(floatingTexts: List<com.example.game.FloatingText>) {
  for (ft in floatingTexts) {
    drawContext.canvas.nativeCanvas.apply {
      val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(
          (ft.alpha * 255).toInt().coerceIn(0, 255),
          (ft.color.red * 255).toInt(),
          (ft.color.green * 255).toInt(),
          (ft.color.blue * 255).toInt()
        )
        textSize = 38f * ft.scale
        isFakeBoldText = true
        setShadowLayer(14f, 0f, 0f, android.graphics.Color.BLACK)
      }
      drawText(ft.text, ft.x, ft.y, paint)
    }
  }
}

// ==========================================
// COMPOSABLE HUD & OVERLAYS
// ==========================================

@Composable
fun InGameHud(
  score: Int,
  multiplier: Int,
  nearMisses: Int,
  phase: GamePhase,
  hasShield: Boolean,
  slowMoTimer: Float,
  scoreSurgeTimer: Float,
  milestoneText: String?,
  milestoneAlpha: Float,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .statusBarsPadding()
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    // Top Row: Score & Multiplier
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      // Left: Score & Phase
      Column {
        Text(
          text = "$score",
          color = TextPrimary,
          fontSize = 38.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.5.sp
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .background(phase.primaryColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
              .border(1.dp, phase.primaryColor, RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "PHASE ${phase.phaseNumber} • ${phase.name}",
              color = phase.primaryColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      // Right: Multiplier & Near-Miss Counter
      Column(horizontalAlignment = Alignment.End) {
        // Multiplier Pill
        Box(
          modifier = Modifier
            .background(
              brush = Brush.horizontalGradient(
                colors = listOf(
                  if (multiplier > 1) NeonAmber else CyberSurfaceVariant,
                  if (multiplier > 1) NeonMagenta else CyberSurface
                )
              ),
              shape = RoundedCornerShape(20.dp)
            )
            .border(
              width = 1.5.dp,
              color = if (multiplier > 1) NeonAmber else CyberSurfaceVariant,
              shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (multiplier > 1) {
              Text("🔥 ", fontSize = 14.sp)
            }
            Text(
              text = "${multiplier}X STREAK",
              color = if (multiplier > 1) Color.Black else TextSecondary,
              fontSize = 15.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "⚡ $nearMisses NEAR-MISSES",
          color = NeonCyan,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // Active Power-ups Bar (Below Top Row)
    Row(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 65.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (hasShield) {
        PowerUpStatusPill(
          icon = Icons.Default.Shield,
          label = "SHIELD",
          color = NeonCyan
        )
      }
      if (slowMoTimer > 0f) {
        PowerUpStatusPill(
          icon = Icons.Default.Timer,
          label = "CHRONO ${"%.1f".format(slowMoTimer)}s",
          color = NeonAmber
        )
      }
      if (scoreSurgeTimer > 0f) {
        PowerUpStatusPill(
          icon = Icons.Default.ElectricBolt,
          label = "SURGE 2X ${"%.1f".format(scoreSurgeTimer)}s",
          color = NeonMagenta
        )
      }
    }

    // Milestone Celebration Banner
    if (milestoneText != null && milestoneAlpha > 0f) {
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .alpha(milestoneAlpha)
          .background(
            brush = Brush.horizontalGradient(listOf(NeonMagenta, NeonAmber, NeonCyan)),
            shape = RoundedCornerShape(12.dp)
          )
          .border(2.dp, Color.White, RoundedCornerShape(12.dp))
          .padding(horizontal = 24.dp, vertical = 14.dp)
      ) {
        Text(
          text = milestoneText,
          color = Color.Black,
          fontSize = 18.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          textAlign = TextAlign.Center
        )
      }
    }

    // One-Tap Prompt Hint at bottom
    Text(
      text = "TAP ANYWHERE TO PHASE SHIFT",
      color = TextMuted.copy(alpha = 0.6f),
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = 16.dp)
    )
  }
}

@Composable
fun PowerUpStatusPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  color: Color
) {
  Row(
    modifier = Modifier
      .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
      .border(1.dp, color, RoundedCornerShape(12.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = color,
      modifier = Modifier.size(14.dp)
    )
    Text(
      text = label,
      color = color,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
  }
}

// ==========================================
// ATTRACT / TITLE SCREEN
// ==========================================

@Composable
fun AttractScreenOverlay(
  highScore: Int,
  bestStreak: Int,
  totalRuns: Int,
  isSoundFx: Boolean,
  isMusic: Boolean,
  isHaptics: Boolean,
  pulseScale: Float,
  onToggleSound: () -> Unit,
  onToggleMusic: () -> Unit,
  onToggleHaptics: () -> Unit,
  onShowLeaderboard: () -> Unit,
  onStart: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(24.dp)
  ) {
    // Top Bar with Audio & Leaderboard Controls
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(
          onClick = onToggleMusic,
          modifier = Modifier
            .size(44.dp)
            .background(CyberSurface, CircleShape)
            .border(1.dp, if (isMusic) NeonMagenta else CyberSurfaceVariant, CircleShape)
            .testTag("toggle_music_button")
        ) {
          Icon(
            imageVector = if (isMusic) Icons.Default.MusicNote else Icons.Default.MusicOff,
            contentDescription = "Toggle Music",
            tint = if (isMusic) NeonMagenta else TextMuted
          )
        }
        IconButton(
          onClick = onToggleSound,
          modifier = Modifier
            .size(44.dp)
            .background(CyberSurface, CircleShape)
            .border(1.dp, if (isSoundFx) NeonCyan else CyberSurfaceVariant, CircleShape)
            .testTag("toggle_sound_button")
        ) {
          Icon(
            imageVector = if (isSoundFx) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = "Toggle SFX",
            tint = if (isSoundFx) NeonCyan else TextMuted
          )
        }
        IconButton(
          onClick = onToggleHaptics,
          modifier = Modifier
            .size(44.dp)
            .background(CyberSurface, CircleShape)
            .border(1.dp, if (isHaptics) NeonAmber else CyberSurfaceVariant, CircleShape)
            .testTag("toggle_haptics_button")
        ) {
          Icon(
            imageVector = Icons.Default.Vibration,
            contentDescription = "Toggle Haptics",
            tint = if (isHaptics) NeonAmber else TextMuted
          )
        }
      }

      IconButton(
        onClick = onShowLeaderboard,
        modifier = Modifier
          .size(44.dp)
          .background(CyberSurface, CircleShape)
          .border(1.dp, NeonCyan, CircleShape)
          .testTag("open_leaderboard_button")
      ) {
        Icon(
          imageVector = Icons.Default.Leaderboard,
          contentDescription = "Leaderboard",
          tint = NeonCyan
        )
      }
    }

    // Center Title & High Score
    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .widthIn(max = 440.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "PULSE",
        color = NeonCyan,
        fontSize = 46.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 4.sp
      )
      Text(
        text = "RIDER",
        color = NeonMagenta,
        fontSize = 46.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 4.sp
      )

      Text(
        text = "CYBERPUNK NEON RUNNER",
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
      )

      // High Score Banner
      Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "BEST RECORD",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "$highScore",
            color = NeonAmber,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
          )
          Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Text(
              text = "🔥 Best Streak: x$bestStreak",
              color = TextSecondary,
              fontSize = 12.sp,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "🎮 Runs: $totalRuns",
              color = TextSecondary,
              fontSize = 12.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Tap to Ride Pulse prompt button
      Box(
        modifier = Modifier
          .scale(pulseScale)
          .background(
            brush = Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta)),
            shape = RoundedCornerShape(30.dp)
          )
          .border(2.dp, Color.White, RoundedCornerShape(30.dp))
          .clickable { onStart() }
          .padding(horizontal = 36.dp, vertical = 16.dp)
          .testTag("start_game_button")
      ) {
        Text(
          text = "TAP TO RIDE THE PULSE",
          color = Color.Black,
          fontSize = 16.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp
        )
      }
    }

    // Bottom Controls & How to Play Guide
    Card(
      colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.85f)),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .border(1.dp, CyberSurfaceVariant, RoundedCornerShape(12.dp))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        HowToPlayItem(icon = "👆", label = "1-Tap", desc = "Shift Rails")
        HowToPlayItem(icon = "⚡", label = "Graze", desc = "Near-Miss 50pts")
        HowToPlayItem(icon = "🛡️", label = "Pickups", desc = "Shields & Surge")
      }
    }
  }
}

@Composable
fun HowToPlayItem(icon: String, label: String, desc: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = icon, fontSize = 16.sp)
    Text(
      text = label,
      color = TextPrimary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
    Text(
      text = desc,
      color = TextSecondary,
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace
    )
  }
}

// ==========================================
// GAME OVER SCORE CARD (SHAREABLE)
// ==========================================

@Composable
fun GameOverScoreCard(
  score: Int,
  highScore: Int,
  isNewBest: Boolean,
  nearMisses: Int,
  maxStreak: Int,
  distance: Int,
  phase: GamePhase,
  onRetry: () -> Unit,
  onShare: () -> Unit,
  onShowLeaderboard: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.75f))
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(20.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = CyberSurface),
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 420.dp)
        .border(
          width = 2.dp,
          brush = Brush.linearGradient(listOf(NeonMagenta, NeonCyan, NeonAmber)),
          shape = RoundedCornerShape(20.dp)
        )
        .testTag("game_over_card")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header
        Text(
          text = "PULSE TERMINATED",
          color = NeonRed,
          fontSize = 14.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Final Score
        Text(
          text = "$score",
          color = TextPrimary,
          fontSize = 48.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 2.sp
        )

        if (isNewBest) {
          Box(
            modifier = Modifier
              .background(NeonAmber, RoundedCornerShape(20.dp))
              .padding(horizontal = 14.dp, vertical = 4.dp)
          ) {
            Text(
              text = "🏆 NEW HIGH SCORE!",
              color = Color.Black,
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )
          }
        } else {
          Text(
            text = "BEST: $highScore",
            color = TextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Run Stats Grid
        Card(
          colors = CardDefaults.cardColors(containerColor = CyberBackground),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberSurfaceVariant, RoundedCornerShape(12.dp))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              StatColumn(title = "NEAR-MISSES", value = "$nearMisses", color = NeonCyan)
              StatColumn(title = "MAX STREAK", value = "x$maxStreak", color = NeonAmber)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              StatColumn(title = "DISTANCE", value = "${distance}m", color = TextPrimary)
              StatColumn(title = "PHASE REACHED", value = phase.name, color = phase.primaryColor)
            }
          }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Action Buttons: RETRY and SHARE
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("retry_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Retry",
              tint = Color.Black,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "RETRY",
              color = Color.Black,
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )
          }

          Button(
            onClick = onShare,
            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("share_button")
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Share",
              tint = Color.Black,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "SHARE",
              color = Color.Black,
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = onShowLeaderboard,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "VIEW RUN RECORDS",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
fun StatColumn(title: String, value: String, color: Color) {
  Column {
    Text(
      text = title,
      color = TextMuted,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
    Text(
      text = value,
      color = color,
      fontSize = 18.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace
    )
  }
}

// ==========================================
// LEADERBOARD & STATS DIALOG
// ==========================================

@Composable
fun LeaderboardDialog(
  topRuns: List<RunRecord>,
  totalNearMisses: Int,
  totalRuns: Int,
  onDismiss: () -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = CyberSurface),
      shape = RoundedCornerShape(18.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, NeonCyan, RoundedCornerShape(18.dp))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Leaderboard,
              contentDescription = "Leaderboard",
              tint = NeonCyan
            )
            Text(
              text = "TOP RUN RECORDS",
              color = TextPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Total Stats summary bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(CyberBackground, RoundedCornerShape(8.dp))
            .padding(10.dp),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          Text(
            text = "Total Runs: $totalRuns",
            color = TextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "⚡ Near-Misses: $totalNearMisses",
            color = NeonAmber,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (topRuns.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(150.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "NO RUNS RECORDED YET.\nDROP INTO THE PULSE!",
              color = TextMuted,
              fontSize = 12.sp,
              fontFamily = FontFamily.Monospace,
              textAlign = TextAlign.Center
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(260.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            itemsIndexed(topRuns) { index, run ->
              val rankColor = when (index) {
                0 -> NeonAmber
                1 -> Color(0xFFC0C0C0)
                2 -> Color(0xFFCD7F32)
                else -> TextSecondary
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(CyberBackground, RoundedCornerShape(8.dp))
                  .border(0.8.dp, CyberSurfaceVariant, RoundedCornerShape(8.dp))
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Text(
                    text = "#${index + 1}",
                    color = rankColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                  )
                  Column {
                    Text(
                      text = "${run.score} pts",
                      color = TextPrimary,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace
                    )
                    Text(
                      text = "Streak x${run.maxStreak} • ⚡ ${run.nearMissCount}",
                      color = TextMuted,
                      fontSize = 10.sp,
                      fontFamily = FontFamily.Monospace
                    )
                  }
                }

                Text(
                  text = dateFormat.format(Date(run.timestamp)),
                  color = TextMuted,
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "CLOSE",
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
