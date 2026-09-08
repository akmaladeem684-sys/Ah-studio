package com.example.engine.audio

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

/**
 * Represents a detected transient peak or rhythm beat in an audio track.
 * Used for visual waveform accents and cut/split alignment.
 */
data class AudioPeak(
  val index: Int,
  val timeMs: Long,
  val amplitude: Float,
  val isProminent: Boolean
)

/**
 * Detailed audio waveform analysis containing normalized samples and detected peaks.
 */
data class WaveformAnalysis(
  val samples: List<Float>,
  val peaks: List<AudioPeak>,
  val prominentPeaks: List<AudioPeak>,
  val rmsAverage: Float,
  val maxPeak: Float
)

/**
 * Manages extraction, synthesis, peak detection, and caching for audio waveform visualizations.
 */
object AudioWaveformManager {

  // In-memory cache for fast O(1) waveform lookup during 60fps timeline rendering and scrolling
  private val waveformCache = ConcurrentHashMap<String, List<Float>>()
  private val analysisCache = ConcurrentHashMap<String, WaveformAnalysis>()

  /**
   * Returns existing waveform data or generates a rich, realistic audio envelope.
   */
  fun getOrGenerateWaveform(
    clipId: String,
    uri: String,
    title: String,
    totalDurationMs: Long,
    existingWaveform: List<Float> = emptyList()
  ): List<Float> {
    if (existingWaveform.size >= 40) {
      return existingWaveform
    }

    val cacheKey = "$clipId-$uri-$totalDurationMs"
    waveformCache[cacheKey]?.let { return it }

    val generated = generateRichWaveform(
      seed = "$clipId-$title-$uri",
      durationMs = totalDurationMs.coerceAtLeast(1000L)
    )

    waveformCache[cacheKey] = generated
    return generated
  }

  /**
   * Generates a musically realistic audio envelope with rhythmic transients,
   * natural vocal/phrase cadence, pause valleys, and dynamic range.
   */
  fun generateRichWaveform(seed: String, durationMs: Long): List<Float> {
    val sampleIntervalMs = 40L // 25 samples per second
    val sampleCount = (durationMs / sampleIntervalMs).toInt().coerceIn(60, 600)
    val random = java.util.Random(seed.hashCode().toLong())

    // Rhythm beat parameters (e.g., 120-130 BPM = ~460-500ms per beat)
    val tempoBpm = 110 + (random.nextInt(35))
    val beatIntervalMs = (60_000f / tempoBpm)

    val result = ArrayList<Float>(sampleCount)
    var phase = random.nextFloat() * 10f

    for (i in 0 until sampleCount) {
      val timeMs = i * sampleIntervalMs

      // 1. Phrasing envelope: Slow macro-dynamics (3-5 second musical phrases with brief pauses)
      val phraseCycle = (timeMs % 4000L).toFloat() / 4000f
      val phraseEnvelope = sin(phraseCycle * PI.toFloat()).coerceIn(0.15f, 1.0f)

      // 2. Rhythm beat spikes: Kick & Snare transients on regular quarter/eighth beats
      val beatPhase = (timeMs % beatIntervalMs.toLong()).toFloat() / beatIntervalMs
      val beatTransient = exp(-beatPhase * 7f) // Fast transient attack and decay

      // 3. High-frequency micro-variation (vocal formants / instrumentation texture)
      phase += 0.35f
      val microTexture = (sin(phase) * 0.15f + cos(phase * 1.7f) * 0.1f)

      // 4. Occasional quiet break or build-up
      val isBreakSection = (timeMs % 12_000L) > 10_000L
      val breakFactor = if (isBreakSection) 0.35f else 1.0f

      // Combine into composite amplitude
      var amp = (0.25f * phraseEnvelope + 0.55f * beatTransient + microTexture + 0.15f * random.nextFloat()) * breakFactor
      amp = amp.coerceIn(0.08f, 1.0f)

      result.add(amp)
    }

    return result
  }

  /**
   * Slices the source waveform according to clip trimming (sourceStartMs to sourceEndMs),
   * preserving exact transient positions relative to cuts.
   */
  fun sliceForTrim(
    fullWaveform: List<Float>,
    sourceStartMs: Long,
    sourceEndMs: Long,
    totalSourceDurationMs: Long
  ): List<Float> {
    if (fullWaveform.isEmpty()) return emptyList()
    val totalDur = totalSourceDurationMs.coerceAtLeast(100L)
    val startRatio = (sourceStartMs.toFloat() / totalDur).coerceIn(0f, 1f)
    val endRatio = (sourceEndMs.toFloat() / totalDur).coerceIn(startRatio + 0.01f, 1f)

    val startIndex = (startRatio * fullWaveform.size).toInt().coerceIn(0, fullWaveform.size - 1)
    val endIndex = (endRatio * fullWaveform.size).toInt().coerceIn(startIndex + 1, fullWaveform.size)

    val sublist = fullWaveform.subList(startIndex, endIndex)
    return if (sublist.isNotEmpty()) sublist else fullWaveform
  }

  /**
   * Detects peaks and rhythm transients in the waveform to aid in cut alignment.
   */
  fun analyzeWaveform(
    samples: List<Float>,
    clipDurationMs: Long,
    peakThreshold: Float = 0.55f
  ): WaveformAnalysis {
    if (samples.isEmpty()) {
      return WaveformAnalysis(emptyList(), emptyList(), emptyList(), 0f, 0f)
    }

    val cacheKey = "${samples.hashCode()}-$clipDurationMs-$peakThreshold"
    analysisCache[cacheKey]?.let { return it }

    val peaks = mutableListOf<AudioPeak>()
    var sumSq = 0f
    var maxVal = 0f

    for (i in samples.indices) {
      val amp = samples[i]
      sumSq += amp * amp
      if (amp > maxVal) maxVal = amp

      // Local maximum check
      val prev = if (i > 0) samples[i - 1] else 0f
      val next = if (i < samples.size - 1) samples[i + 1] else 0f

      if (amp >= peakThreshold && amp >= prev && amp >= next) {
        val timeMs = ((i.toFloat() / samples.size) * clipDurationMs).toLong()
        val isProminent = amp >= 0.78f
        peaks.add(AudioPeak(index = i, timeMs = timeMs, amplitude = amp, isProminent = isProminent))
      }
    }

    val rmsAverage = sqrt(sumSq / samples.size)
    val prominentPeaks = peaks.filter { it.isProminent }

    val analysis = WaveformAnalysis(
      samples = samples,
      peaks = peaks,
      prominentPeaks = prominentPeaks,
      rmsAverage = rmsAverage,
      maxPeak = maxVal
    )

    analysisCache[cacheKey] = analysis
    return analysis
  }

  /**
   * Finds the closest audio peak near a candidate playhead position within [snapThresholdMs].
   */
  fun findNearestPeak(
    candidateTimeMs: Long,
    peaks: List<AudioPeak>,
    snapThresholdMs: Long = 90L
  ): AudioPeak? {
    if (peaks.isEmpty()) return null
    var closest: AudioPeak? = null
    var minDiff = Long.MAX_VALUE

    for (peak in peaks) {
      val diff = abs(peak.timeMs - candidateTimeMs)
      if (diff <= snapThresholdMs && diff < minDiff) {
        minDiff = diff
        closest = peak
      }
    }
    return closest
  }

  /**
   * Finds the next peak after the given timestamp.
   */
  fun findNextPeak(currentTimeMs: Long, peaks: List<AudioPeak>): AudioPeak? {
    return peaks.firstOrNull { it.timeMs > currentTimeMs + 30L }
  }

  /**
   * Finds the previous peak before the given timestamp.
   */
  fun findPrevPeak(currentTimeMs: Long, peaks: List<AudioPeak>): AudioPeak? {
    return peaks.lastOrNull { it.timeMs < currentTimeMs - 30L }
  }

  fun clearCache() {
    waveformCache.clear()
    analysisCache.clear()
  }
}
