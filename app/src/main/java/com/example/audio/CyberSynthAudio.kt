package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class CyberSynthAudio(private val coroutineScope: CoroutineScope) {
  private val sampleRate = 44100
  private var audioTrack: AudioTrack? = null
  private val audioQueue = ConcurrentLinkedQueue<ShortArray>()
  private var playbackJob: Job? = null
  private var rhythmJob: Job? = null

  var sfxEnabled: Boolean = true
  var musicEnabled: Boolean = true

  private var currentBpm: Double = 120.0

  init {
    initAudioTrack()
    startPlaybackLoop()
  }

  private fun initAudioTrack() {
    try {
      val minBufSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
      )
      val bufSize = (minBufSize * 2).coerceAtLeast(sampleRate / 4)

      audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(bufSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

      audioTrack?.play()
    } catch (e: Exception) {
      Log.e("CyberSynthAudio", "AudioTrack init failed: ${e.message}")
    }
  }

  private fun startPlaybackLoop() {
    playbackJob = coroutineScope.launch(Dispatchers.Default) {
      val chunkSize = 512
      val mixBuffer = FloatArray(chunkSize)
      val outBuffer = ShortArray(chunkSize)

      while (isActive) {
        if (!sfxEnabled && !musicEnabled) {
          audioQueue.clear()
          delay(50)
          continue
        }

        val items = mutableListOf<ShortArray>()
        while (items.size < 8) {
          val item = audioQueue.poll() ?: break
          items.add(item)
        }

        if (items.isNotEmpty()) {
          // Play each item sequentially or in small stream
          for (sample in items) {
            audioTrack?.write(sample, 0, sample.size)
          }
        } else {
          delay(10)
        }
      }
    }
  }

  fun startRhythm(speedMultiplier: Float = 1.0f) {
    rhythmJob?.cancel()
    currentBpm = (110.0 + (speedMultiplier - 1.0) * 40.0).coerceIn(110.0, 180.0)

    rhythmJob = coroutineScope.launch(Dispatchers.Default) {
      var beat = 0
      while (isActive) {
        if (musicEnabled) {
          val beatIntervalMs = (60000.0 / currentBpm / 2.0).toLong()
          if (beat % 2 == 0) {
            // Kick / Sub Bass Pulse
            playCyberKick()
          } else {
            // Hi-hat / synth pulse
            playCyberHiHat()
          }
          beat = (beat + 1) % 16
          delay(beatIntervalMs)
        } else {
          delay(200)
        }
      }
    }
  }

  fun updateSpeed(speedMultiplier: Float) {
    currentBpm = (110.0 + (speedMultiplier - 1.0) * 45.0).coerceIn(110.0, 190.0)
  }

  fun stopRhythm() {
    rhythmJob?.cancel()
    rhythmJob = null
  }

  // --- SOUND EFFECTS ---

  fun playPhaseShift(toTop: Boolean) {
    if (!sfxEnabled) return
    val duration = 0.08
    val numSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(numSamples)
    val startFreq = if (toTop) 280.0 else 560.0
    val endFreq = if (toTop) 620.0 else 220.0

    for (i in 0 until numSamples) {
      val t = i.toDouble() / sampleRate
      val progress = i.toDouble() / numSamples
      val freq = startFreq + (endFreq - startFreq) * progress
      val env = sin(progress * PI)
      val sample = (sin(2.0 * PI * freq * t) * 0.45 + sin(4.0 * PI * freq * t) * 0.15) * env
      buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
    }
    audioQueue.offer(buffer)
  }

  fun playNearMiss(streak: Int) {
    if (!sfxEnabled) return
    val duration = 0.22
    val numSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(numSamples)
    val baseFreq = (660.0 + (streak.coerceAtMost(10) * 60.0))

    for (i in 0 until numSamples) {
      val t = i.toDouble() / sampleRate
      val progress = i.toDouble() / numSamples
      val env = exp(-progress * 7.0)
      // Rich chime with fundamental and harmonics
      val s1 = sin(2.0 * PI * baseFreq * t)
      val s2 = sin(2.0 * PI * (baseFreq * 1.5) * t) * 0.6
      val s3 = sin(2.0 * PI * (baseFreq * 2.0) * t) * 0.4
      val sample = (s1 + s2 + s3) / 2.0 * env * 0.7
      buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
    }
    audioQueue.offer(buffer)
  }

  fun playPowerUpCollect() {
    if (!sfxEnabled) return
    val duration = 0.25
    val numSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(numSamples)
    val notes = doubleArrayOf(440.0, 554.37, 659.25, 880.0)

    for (i in 0 until numSamples) {
      val t = i.toDouble() / sampleRate
      val noteIndex = ((i.toDouble() / numSamples) * notes.size).toInt().coerceIn(0, notes.size - 1)
      val freq = notes[noteIndex]
      val env = 1.0 - (i.toDouble() / numSamples * 0.5)
      val sample = sin(2.0 * PI * freq * t) * env * 0.5
      buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
    }
    audioQueue.offer(buffer)
  }

  fun playShieldDeflect() {
    if (!sfxEnabled) return
    val duration = 0.18
    val numSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(numSamples)

    for (i in 0 until numSamples) {
      val t = i.toDouble() / sampleRate
      val progress = i.toDouble() / numSamples
      val freq = 450.0 + (1.0 - progress) * 300.0
      val noise = (Math.random() * 2.0 - 1.0) * 0.3
      val env = exp(-progress * 6.0)
      val sample = (sin(2.0 * PI * freq * t) * 0.7 + noise) * env * 0.6
      buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
    }
    audioQueue.offer(buffer)
  }

  fun playDeathCrash() {
    if (!sfxEnabled) return
    val duration = 0.45
    val numSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(numSamples)

    for (i in 0 until numSamples) {
      val t = i.toDouble() / sampleRate
      val progress = i.toDouble() / numSamples
      val subFreq = 90.0 * exp(-progress * 3.0)
      val sub = sin(2.0 * PI * subFreq * t) * 0.6
      val noise = (Math.random() * 2.0 - 1.0) * 0.5
      val env = exp(-progress * 4.5)
      val sample = (sub + noise) * env * 0.8
      buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
    }
    audioQueue.offer(buffer)
  }

  fun playMilestone() {
    if (!sfxEnabled) return
    val duration = 0.40
    val numSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(numSamples)
    val chords = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)

    for (i in 0 until numSamples) {
      val t = i.toDouble() / sampleRate
      val progress = i.toDouble() / numSamples
      val env = exp(-progress * 3.0)
      var mix = 0.0
      for (chord in chords) {
        mix += sin(2.0 * PI * chord * t) * 0.25
      }
      val sample = mix * env * 0.75
      buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
    }
    audioQueue.offer(buffer)
  }

  private fun playCyberKick() {
    val duration = 0.06
    val numSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(numSamples)

    for (i in 0 until numSamples) {
      val t = i.toDouble() / sampleRate
      val progress = i.toDouble() / numSamples
      val freq = 120.0 * (1.0 - progress * 0.7)
      val env = exp(-progress * 12.0)
      val sample = sin(2.0 * PI * freq * t) * env * 0.25
      buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
    }
    audioQueue.offer(buffer)
  }

  private fun playCyberHiHat() {
    val duration = 0.03
    val numSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(numSamples)

    for (i in 0 until numSamples) {
      val progress = i.toDouble() / numSamples
      val noise = (Math.random() * 2.0 - 1.0)
      val env = exp(-progress * 25.0)
      val sample = noise * env * 0.12
      buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
    }
    audioQueue.offer(buffer)
  }

  fun release() {
    playbackJob?.cancel()
    rhythmJob?.cancel()
    try {
      audioTrack?.stop()
      audioTrack?.release()
    } catch (ignored: Exception) {}
    audioTrack = null
  }
}
