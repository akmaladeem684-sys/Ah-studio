package com.example

import com.example.domain.model.*
import com.example.engine.TimelineEngine
import com.example.engine.audio.AudioPeak
import com.example.engine.audio.AudioWaveformManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AudioWaveformTest {

  private lateinit var timelineEngine: TimelineEngine

  @Before
  fun setup() {
    timelineEngine = TimelineEngine()
    AudioWaveformManager.clearCache()
  }

  @Test
  fun testWaveformGeneration_createsValidAmplitudesAndDynamics() {
    val waveform = AudioWaveformManager.generateRichWaveform("test_seed_123", durationMs = 5000L)
    assertTrue("Waveform should have sufficient sample points", waveform.size >= 100)

    // Check all values are within normalized bounds [0.0f, 1.0f]
    waveform.forEach { amp ->
      assertTrue("Amplitude $amp should be >= 0.05f", amp >= 0.05f)
      assertTrue("Amplitude $amp should be <= 1.0f", amp <= 1.0f)
    }

    // Check dynamic range exists (not flat)
    val minVal = waveform.minOrNull() ?: 0f
    val maxVal = waveform.maxOrNull() ?: 0f
    assertTrue("Dynamic range should be significant", (maxVal - minVal) > 0.4f)
  }

  @Test
  fun testWaveformAnalysis_detectsPeaksAndCalculatesRms() {
    // Construct a synthetic waveform with clear peaks
    val samples = listOf(
      0.2f, 0.3f, 0.95f, 0.3f, 0.1f, // Peak at index 2 (prominent)
      0.2f, 0.4f, 0.70f, 0.3f, 0.1f, // Peak at index 7 (regular)
      0.1f, 0.2f, 0.88f, 0.2f, 0.1f  // Peak at index 12 (prominent)
    )

    val analysis = AudioWaveformManager.analyzeWaveform(samples, clipDurationMs = 1500L, peakThreshold = 0.60f)

    assertEquals("Should detect 3 peaks", 3, analysis.peaks.size)
    assertEquals("Should detect 2 prominent peaks", 2, analysis.prominentPeaks.size)
    assertEquals(2, analysis.peaks[0].index)
    assertEquals(7, analysis.peaks[1].index)
    assertEquals(12, analysis.peaks[2].index)

    assertTrue("RMS average should be positive", analysis.rmsAverage > 0.2f)
    assertEquals(0.95f, analysis.maxPeak, 0.001f)
  }

  @Test
  fun testSliceForTrim_preservesTrimRatioAndAlignment() {
    val fullWaveform = (0..99).map { it / 100f } // 100 samples from 0.0 to 0.99
    val totalDurationMs = 10_000L

    // Trim from 2000ms to 6000ms (20% to 60%)
    val sliced = AudioWaveformManager.sliceForTrim(
      fullWaveform = fullWaveform,
      sourceStartMs = 2000L,
      sourceEndMs = 6000L,
      totalSourceDurationMs = totalDurationMs
    )

    assertEquals("Trimmed slice should contain 40 samples", 40, sliced.size)
    assertEquals(0.20f, sliced.first(), 0.02f)
    assertEquals(0.59f, sliced.last(), 0.02f)
  }

  @Test
  fun testPeakSnappingAndNavigation() {
    val peaks = listOf(
      AudioPeak(index = 5, timeMs = 500L, amplitude = 0.9f, isProminent = true),
      AudioPeak(index = 10, timeMs = 1000L, amplitude = 0.85f, isProminent = true),
      AudioPeak(index = 15, timeMs = 1500L, amplitude = 0.92f, isProminent = true)
    )

    // Snap to 1000ms when candidate is 1030ms (within 80ms threshold)
    val snapped = AudioWaveformManager.findNearestPeak(1030L, peaks, snapThresholdMs = 80L)
    assertNotNull("Should snap to nearest peak", snapped)
    assertEquals(1000L, snapped?.timeMs)

    // Outside threshold (diff = 120ms > 80ms)
    val notSnapped = AudioWaveformManager.findNearestPeak(1120L, peaks, snapThresholdMs = 80L)
    assertNull("Should not snap when outside threshold", notSnapped)

    // Navigation
    val nextPeak = AudioWaveformManager.findNextPeak(600L, peaks)
    assertEquals(1000L, nextPeak?.timeMs)

    val prevPeak = AudioWaveformManager.findPrevPeak(950L, peaks)
    assertEquals(500L, prevPeak?.timeMs)
  }

  @Test
  fun testTimelineEngine_jumpToNextAudioPeak_seeksPlayhead() {
    // Add audio clip with waveform
    timelineEngine.setPosition(0L)
    timelineEngine.addAudioClip("Beat Drop Track", durationMs = 6000L)

    val jumped = timelineEngine.jumpToNextAudioPeak()
    assertTrue("Should find and jump to next peak", jumped)
    assertTrue("Playhead should advance to peak time", timelineEngine.currentPositionMs.value > 0L)
  }
}
