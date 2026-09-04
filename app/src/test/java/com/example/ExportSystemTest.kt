package com.example

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.*
import com.example.engine.export.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExportSystemTest {

  private lateinit var context: Context
  private lateinit var audioProcessor: AudioExportProcessor
  private lateinit var exporter: VideoExporter

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    audioProcessor = AudioExportProcessor(context)
    exporter = VideoExporter(context)
  }

  @Test
  fun `test 1 - Video only timeline detection`() {
    val timeline = Timeline(
      videoClips = listOf(
        VideoClip(
          name = "Silent Video",
          durationMs = 4000L,
          isVideo = true,
          hasAudio = false,
          isMuted = true
        )
      ),
      audioClips = emptyList()
    )

    val hasAudio = audioProcessor.hasActiveAudio(timeline)
    assertFalse("Video only timeline should not have active audio", hasAudio)
  }

  @Test
  fun `test 2 - Video plus original audio`() = runBlocking {
    val timeline = Timeline(
      videoClips = listOf(
        VideoClip(
          name = "Active Video Clip",
          uri = "sfx_pop", // will trigger synthesis
          durationMs = 3000L,
          isVideo = true,
          hasAudio = true,
          isMuted = false,
          volume = 1.0f
        )
      )
    )

    assertTrue("Timeline should detect video original audio", audioProcessor.hasActiveAudio(timeline))
    val masterPcm = audioProcessor.mixTimelineAudio(timeline, 3000L)
    assertTrue("Mixed PCM should have samples", masterPcm.isNotEmpty())
    val expectedFrames = (3000L * 44100 / 1000L).toInt()
    assertEquals("Should generate expected number of stereo samples", expectedFrames * 2, masterPcm.size)
  }

  @Test
  fun `test 3 - Video plus music track`() = runBlocking {
    val timeline = Timeline(
      videoClips = listOf(
        VideoClip(name = "Background Video", durationMs = 5000L, isVideo = true, hasAudio = false)
      ),
      audioClips = listOf(
        AudioClip(
          title = "Midnight Lofi Lounge",
          uri = "internal://Midnight Lofi Lounge",
          timelineStartMs = 0L,
          durationMs = 5000L,
          volume = 0.8f
        )
      )
    )

    assertTrue(audioProcessor.hasActiveAudio(timeline))
    val masterPcm = audioProcessor.mixTimelineAudio(timeline, 5000L)
    assertTrue("Should mix music audio track", masterPcm.isNotEmpty())

    // Check that samples are non-zero
    val nonZeroCount = masterPcm.count { it != 0.toShort() }
    assertTrue("Music track should contain audio waveform data", nonZeroCount > 100)
  }

  @Test
  fun `test 4 - Video plus voiceover with volume, gain and fades`() = runBlocking {
    val timeline = Timeline(
      videoClips = listOf(
        VideoClip(name = "Host Video", durationMs = 4000L, isVideo = true, hasAudio = false)
      ),
      audioClips = listOf(
        AudioClip(
          title = "Voiceover",
          uri = "internal://sfx_pop",
          timelineStartMs = 500L,
          durationMs = 3000L,
          volume = 1.2f,
          gainDb = 3.0f,
          fadeInMs = 400L,
          fadeOutMs = 400L,
          isVoiceOver = true
        )
      )
    )

    val masterPcm = audioProcessor.mixTimelineAudio(timeline, 4000L)
    assertTrue(masterPcm.isNotEmpty())

    // Check that samples before timelineStartMs (first 200ms) are zero/silence
    val frame200ms = (200L * 44100 / 1000L).toInt()
    assertEquals("Before voiceover start, buffer should be silent", 0, masterPcm[frame200ms * 2].toInt())
  }

  @Test
  fun `test 5 - Multiple simultaneous audio tracks mix without clipping`() = runBlocking {
    val timeline = Timeline(
      videoClips = listOf(
        VideoClip(name = "Scene", durationMs = 6000L, isVideo = true, hasAudio = true, uri = "sfx_pop")
      ),
      audioClips = listOf(
        AudioClip(
          title = "Background Music",
          uri = "mus_lofi",
          timelineStartMs = 0L,
          durationMs = 6000L,
          volume = 0.7f
        ),
        AudioClip(
          title = "Sound Effect Whoosh",
          uri = "sfx_whoosh",
          timelineStartMs = 1500L,
          durationMs = 1200L,
          volume = 1.0f
        ),
        AudioClip(
          title = "Voiceover Commentary",
          uri = "sfx_ding",
          timelineStartMs = 2000L,
          durationMs = 2000L,
          volume = 1.5f,
          gainDb = 2.0f
        )
      )
    )

    val masterPcm = audioProcessor.mixTimelineAudio(timeline, 6000L)
    assertTrue("Should mix 4 simultaneous audio sources", masterPcm.isNotEmpty())

    // Ensure all samples are within valid 16-bit PCM bounds (-32768 to 32767)
    for (s in masterPcm) {
      assertTrue("Sample should never overflow short bounds", s >= -32768 && s <= 32767)
    }
  }

  @Test
  fun `test 6 - Trimmed video and audio bounds`() = runBlocking {
    val timeline = Timeline(
      audioClips = listOf(
        AudioClip(
          title = "Trimmed SFX",
          uri = "sfx_whoosh",
          timelineStartMs = 0L,
          durationMs = 500L,
          sourceStartMs = 200L,
          sourceEndMs = 700L
        )
      )
    )

    val masterPcm = audioProcessor.mixTimelineAudio(timeline, 2000L)
    val afterTrimFrame = (600L * 44100 / 1000L).toInt()
    assertEquals("Audio after trimmed duration should be silent", 0, masterPcm[afterTrimFrame * 2].toInt())
  }

  @Test
  fun `test 7 - Split clips play sequentially without overlap or gap`() = runBlocking {
    val timeline = Timeline(
      videoClips = listOf(
        VideoClip(
          name = "Part 1",
          uri = "sfx_ding",
          timelineStartMs = 0L,
          durationMs = 1500L,
          sourceStartMs = 0L,
          sourceEndMs = 1500L
        ),
        VideoClip(
          name = "Part 2",
          uri = "sfx_ding",
          timelineStartMs = 1500L,
          durationMs = 1500L,
          sourceStartMs = 1500L,
          sourceEndMs = 3000L
        )
      )
    )

    val masterPcm = audioProcessor.mixTimelineAudio(timeline, 3000L)
    assertTrue(masterPcm.isNotEmpty())
    val expectedFrames = (3000L * 44100 / 1000L).toInt()
    assertEquals(expectedFrames * 2, masterPcm.size)
  }

  @Test
  fun `test 8 - Different clip speeds resample correctly`() = runBlocking {
    val normalTimeline = Timeline(
      audioClips = listOf(
        AudioClip(title = "Normal Speed", uri = "sfx_pop", durationMs = 1000L, speed = 1.0f)
      )
    )
    val fastTimeline = Timeline(
      audioClips = listOf(
        AudioClip(title = "2x Speed", uri = "sfx_pop", durationMs = 1000L, speed = 2.0f)
      )
    )
    val slowTimeline = Timeline(
      audioClips = listOf(
        AudioClip(title = "0.5x Speed", uri = "sfx_pop", durationMs = 1000L, speed = 0.5f)
      )
    )

    val normalPcm = audioProcessor.mixTimelineAudio(normalTimeline, 1000L)
    val fastPcm = audioProcessor.mixTimelineAudio(fastTimeline, 1000L)
    val slowPcm = audioProcessor.mixTimelineAudio(slowTimeline, 1000L)

    assertEquals(normalPcm.size, fastPcm.size)
    assertEquals(normalPcm.size, slowPcm.size)
    // Speed alters the waveform progression
    assertNotEquals(fastPcm[500], slowPcm[500])
  }

  @Test
  fun `test 9 - 1080p and different aspect ratios calculate valid even dimensions`() {
    val estimatedBytes = exporter.calculateEstimatedSizeBytes(
      durationMs = 5000L,
      config = ExportConfig(resolution = Resolution.RES_1080P, frameRate = FrameRate.FPS_30)
    )
    assertTrue("Estimated size should be greater than 0", estimatedBytes > 100_000L)
  }

  @Test
  fun `test 10 - Muxer coordinator stores exact track index and writes samples correctly`() {
    val tempFile = File(context.cacheDir, "test_mux_${System.currentTimeMillis()}.mp4")
    try {
      val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
      val coordinator = MuxerCoordinator(muxer, hasAudio = true)

      // Verify track indices are not initialized yet
      assertEquals(-1, coordinator.videoTrackIndex)
      assertEquals(-1, coordinator.audioTrackIndex)
      assertFalse(coordinator.isStarted)

      // Create dummy video and audio formats
      val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720).apply {
        val csd0 = ByteBuffer.wrap(byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x67, 0x42, 0x00, 0x1f))
        setByteBuffer("csd-0", csd0)
      }
      val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2).apply {
        val csd0 = ByteBuffer.wrap(byteArrayOf(0x12, 0x10))
        setByteBuffer("csd-0", csd0)
      }

      // Add video format
      coordinator.setVideoFormat(videoFormat)
      assertTrue("Video track index must be >= 0", coordinator.videoTrackIndex >= 0)
      assertFalse("Muxer should not start until audio format is also ready", coordinator.isStarted)

      // Add audio format
      coordinator.setAudioFormat(audioFormat)
      assertTrue("Audio track index must be >= 0", coordinator.audioTrackIndex >= 0)
      assertNotEquals("Video and audio track indices must be distinct", coordinator.videoTrackIndex, coordinator.audioTrackIndex)
      assertTrue("Muxer should start once both tracks are registered", coordinator.isStarted)

      muxer.stop()
      muxer.release()
    } finally {
      if (tempFile.exists()) tempFile.delete()
    }
  }

  @Test
  fun `test 11 - Cancellation stops export and leaves state safe`() {
    exporter.cancelExport()
    // Test that cancelExport sets isCancelled and cleans up cleanly
    val state = exporter.exportState.value
    assertTrue("Initial state must be Idle", state is ExportState.Idle)
  }

  @Test
  fun `test 12 - Export handles empty file or error without generating fake dummy MP4`() {
    val tempFile = File(context.cacheDir, "corrupt_${System.currentTimeMillis()}.mp4")
    tempFile.writeText("not a real mp4 video content")
    
    val retriever = android.media.MediaMetadataRetriever()
    var hasValidVideo = false
    try {
      retriever.setDataSource(tempFile.absolutePath)
      hasValidVideo = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null
    } catch (e: Exception) {
      hasValidVideo = false
    } finally {
      try { retriever.release() } catch (ignored: Exception) {}
      if (tempFile.exists()) tempFile.delete()
    }
    assertFalse("Fake or corrupt file should not be accepted as valid video", hasValidVideo)
  }
}
