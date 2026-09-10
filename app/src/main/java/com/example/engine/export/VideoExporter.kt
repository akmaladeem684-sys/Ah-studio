package com.example.engine.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.*
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.*
import com.example.domain.model.*
import com.example.engine.composition.ComposedFrame
import com.example.engine.composition.VideoCompositionEngine
import com.example.engine.media.MediaRelinkManager
import com.example.engine.composition.gpu.EglCore
import com.example.engine.composition.gpu.GpuCompositionRenderer
import com.example.engine.composition.gpu.WindowSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

data class ExportConfig(
  val resolution: Resolution = Resolution.RES_1080P,
  val frameRate: FrameRate = FrameRate.FPS_30,
  val quality: ExportQuality = ExportQuality.HIGH,
  val customBitrateKbps: Int = 12000
)

sealed class ExportState {
  object Idle : ExportState()
  data class Rendering(
    val progressPercent: Float,
    val currentFrame: Int,
    val totalFrames: Int,
    val status: String = "Encoding video..."
  ) : ExportState()
  data class Success(val file: File, val durationMs: Long, val fileSizeBytes: Long) : ExportState()
  data class Error(val message: String) : ExportState()
}

/**
 * MuxerCoordinator manages dynamic track registration and sample writing to MediaMuxer.
 * It ensures:
 * 1. Tracks are registered before MediaMuxer is started.
 * 2. Exact track indices returned by MediaMuxer are stored and used (never hardcoded 0).
 * 3. Samples produced prior to muxer start are safely buffered and flushed.
 */
class MuxerCoordinator(
  private val mediaMuxer: MediaMuxer,
  private val hasAudio: Boolean
) {
  private val tag = "MuxerCoordinator"

  var videoTrackIndex: Int = -1
    private set
  var audioTrackIndex: Int = -1
    private set
  var isStarted: Boolean = false
    private set

  private var lastVideoPtsUs: Long = -1L
  private var lastAudioPtsUs: Long = -1L

  private class QueuedPacket(
    val isAudio: Boolean,
    val data: ByteArray,
    val presentationTimeUs: Long,
    val flags: Int
  ) : Comparable<QueuedPacket> {
    override fun compareTo(other: QueuedPacket): Int {
      return presentationTimeUs.compareTo(other.presentationTimeUs)
    }
  }

  private val pendingQueue = mutableListOf<QueuedPacket>()

  @Synchronized
  fun setVideoFormat(format: MediaFormat) {
    if (videoTrackIndex < 0) {
      try {
        videoTrackIndex = mediaMuxer.addTrack(format)
        Log.d(tag, "Added video track with index $videoTrackIndex")
        checkStart()
      } catch (e: Exception) {
        Log.e(tag, "Failed to add video track to muxer", e)
      }
    }
  }

  @Synchronized
  fun setAudioFormat(format: MediaFormat) {
    if (audioTrackIndex < 0) {
      try {
        audioTrackIndex = mediaMuxer.addTrack(format)
        Log.d(tag, "Added audio track with index $audioTrackIndex")
        checkStart()
      } catch (e: Exception) {
        Log.e(tag, "Failed to add audio track to muxer", e)
      }
    }
  }

  @Synchronized
  fun forceStartWithoutAudio() {
    if (!isStarted && videoTrackIndex >= 0) {
      Log.w(tag, "Force starting MediaMuxer without audio track")
      try {
        mediaMuxer.start()
        isStarted = true
        flushPendingQueue()
      } catch (e: Exception) {
        Log.e(tag, "Failed to force start MediaMuxer", e)
      }
    }
  }

  private fun checkStart() {
    val videoReady = videoTrackIndex >= 0
    val audioReady = !hasAudio || audioTrackIndex >= 0

    if (videoReady && audioReady && !isStarted) {
      Log.d(tag, "Starting MediaMuxer with videoTrack=$videoTrackIndex, audioTrack=$audioTrackIndex")
      try {
        mediaMuxer.start()
        isStarted = true
        flushPendingQueue()
      } catch (e: Exception) {
        Log.e(tag, "Failed to start MediaMuxer", e)
      }
    }
  }

  private fun flushPendingQueue() {
    // Sort queued packets chronologically to guarantee proper interleaved timestamp order
    pendingQueue.sort()

    for (packet in pendingQueue) {
      val track = if (packet.isAudio) audioTrackIndex else videoTrackIndex
      if (track >= 0) {
        val pts = enforceMonotonicPts(packet.isAudio, packet.presentationTimeUs)
        val buf = ByteBuffer.wrap(packet.data)
        val info = MediaCodec.BufferInfo().apply {
          set(0, packet.data.size, pts, packet.flags)
        }
        try {
          mediaMuxer.writeSampleData(track, buf, info)
        } catch (e: Exception) {
          Log.w(tag, "Error flushing queued packet to track $track (pts=$pts)", e)
        }
      }
    }
    pendingQueue.clear()
  }

  private fun enforceMonotonicPts(isAudio: Boolean, requestedPtsUs: Long): Long {
    return if (isAudio) {
      val pts = if (requestedPtsUs <= lastAudioPtsUs) lastAudioPtsUs + 1L else requestedPtsUs
      lastAudioPtsUs = pts
      pts
    } else {
      val pts = if (requestedPtsUs <= lastVideoPtsUs) lastVideoPtsUs + 1L else requestedPtsUs
      lastVideoPtsUs = pts
      pts
    }
  }

  @Synchronized
  fun writeVideoSample(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 || info.size <= 0) {
      return
    }

    if (isStarted && videoTrackIndex >= 0) {
      val pts = enforceMonotonicPts(false, info.presentationTimeUs)
      info.presentationTimeUs = pts
      try {
        mediaMuxer.writeSampleData(videoTrackIndex, buffer, info)
      } catch (e: Exception) {
        Log.w(tag, "Error writing video sample (pts=$pts)", e)
      }
    } else {
      val bytes = ByteArray(info.size)
      buffer.position(info.offset)
      buffer.get(bytes)
      pendingQueue.add(QueuedPacket(false, bytes, info.presentationTimeUs, info.flags))

      // Guard: if audio hasn't started after substantial video frames, force start
      if (pendingQueue.size > 120 && videoTrackIndex >= 0) {
        forceStartWithoutAudio()
      }
    }
  }

  @Synchronized
  fun writeAudioSample(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 || info.size <= 0) {
      return
    }

    if (isStarted && audioTrackIndex >= 0) {
      val pts = enforceMonotonicPts(true, info.presentationTimeUs)
      info.presentationTimeUs = pts
      try {
        mediaMuxer.writeSampleData(audioTrackIndex, buffer, info)
      } catch (e: Exception) {
        Log.w(tag, "Error writing audio sample (pts=$pts)", e)
      }
    } else {
      val bytes = ByteArray(info.size)
      buffer.position(info.offset)
      buffer.get(bytes)
      pendingQueue.add(QueuedPacket(true, bytes, info.presentationTimeUs, info.flags))
    }
  }
}

class VideoExporter(private val context: Context) {

  private val tag = "VideoExporter"
  private val compositionEngine = VideoCompositionEngine(context)
  private val audioProcessor = AudioExportProcessor(context)

  private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
  val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

  @Volatile
  private var isCancelled = false

  fun calculateEstimatedSizeBytes(durationMs: Long, config: ExportConfig): Long {
    val durationSec = (durationMs / 1000f).coerceAtLeast(1f)
    val baseBitrate = when (config.resolution) {
      Resolution.RES_480P -> 2_000_000L
      Resolution.RES_720P -> 4_500_000L
      Resolution.RES_1080P -> 8_500_000L
      Resolution.RES_2K -> 14_000_000L
      Resolution.RES_4K -> 25_000_000L
    }
    val adjustedBitrate = (baseBitrate * config.quality.bitrateMultiplier * (config.frameRate.fps / 30f)).toLong()
    return (adjustedBitrate * durationSec / 8).toLong()
  }

  fun cancelExport() {
    isCancelled = true
  }

  /**
   * Evaluates if a timeline can be exported directly via Media3 Transformer.
   */
  fun canExportWithMedia3Transformer(timeline: Timeline): Boolean {
    if (timeline.videoClips.isEmpty()) return false
    // Timelines with custom canvas overlays, text layers, or stickers utilize hardware composition engine
    if (timeline.overlayClips.isNotEmpty() || timeline.textClips.isNotEmpty() || timeline.stickerClips.isNotEmpty()) {
      return false
    }
    // All video clips must refer to physical playable media on device
    return timeline.videoClips.all { clip ->
      clip.isVideo && clip.uri.isNotBlank() && MediaRelinkManager.isRealPlayableMedia(context, clip.uri)
    }
  }

  /**
   * High-level MP4 Exporter using Media3 Transformer to guarantee stable container generation,
   * automated timestamp synchronization, and reliable track index handling.
   */
  suspend fun exportWithMedia3Transformer(
    timeline: Timeline,
    outputFile: File,
    config: ExportConfig
  ): File? = withContext(Dispatchers.Main) {
    var completed = false
    var exportError: Throwable? = null
    val latch = java.util.concurrent.CountDownLatch(1)

    try {
      _exportState.value = ExportState.Rendering(0.05f, 0, 100, "Configuring Media3 Transformer pipeline...")

      val editedMediaItems = mutableListOf<EditedMediaItem>()
      for (clip in timeline.videoClips) {
        val uri = Uri.parse(clip.uri)
        val mediaItem = MediaItem.Builder().setUri(uri).build()
        val editedItem = EditedMediaItem.Builder(mediaItem)
          .setRemoveAudio(clip.isMuted || !clip.hasAudio)
          .build()
        editedMediaItems.add(editedItem)
      }

      if (editedMediaItems.isEmpty()) return@withContext null

      val sequence = EditedMediaItemSequence(editedMediaItems)
      val composition = Composition.Builder(sequence).build()

      val transformer = Transformer.Builder(context)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .setAudioMimeType(MimeTypes.AUDIO_AAC)
        .addListener(object : Transformer.Listener {
          override fun onCompleted(composition: Composition, exportResult: ExportResult) {
            completed = true
            latch.countDown()
          }

          override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
            exportError = exportException
            latch.countDown()
          }
        })
        .build()

      transformer.start(composition, outputFile.absolutePath)

      val progressHolder = ProgressHolder()
      while (latch.count > 0 && !isCancelled) {
        val state = transformer.getProgress(progressHolder)
        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
          val progress = (progressHolder.progress / 100f).coerceIn(0.05f, 0.95f)
          _exportState.value = ExportState.Rendering(
            progressPercent = progress,
            currentFrame = progressHolder.progress,
            totalFrames = 100,
            status = "Exporting with Media3 Transformer (${progressHolder.progress}%)"
          )
        }
        kotlinx.coroutines.delay(150)
      }

      if (isCancelled) {
        transformer.cancel()
        _exportState.value = ExportState.Idle
        if (outputFile.exists()) outputFile.delete()
        return@withContext null
      }

      if (exportError != null) {
        Log.w(tag, "Media3 Transformer error: ${exportError?.message}. Falling back to composition engine.")
        if (outputFile.exists()) outputFile.delete()
        return@withContext null
      }

      if (completed && outputFile.exists() && outputFile.length() > 1024L) {
        val isValid = validatePlayableMp4(outputFile)
        if (isValid) {
          _exportState.value = ExportState.Success(outputFile, timeline.totalDurationMs, outputFile.length())
          return@withContext outputFile
        }
      }
    } catch (e: Throwable) {
      Log.w(tag, "Media3 Transformer setup exception: ${e.message}", e)
      if (outputFile.exists()) outputFile.delete()
    }
    return@withContext null
  }

  /**
   * Production MP4 Exporter using Media3 Transformer when applicable,
   * with seamless fallback to the robust hardware composition pipeline.
   */
  suspend fun exportProject(
    projectName: String,
    timeline: Timeline,
    config: ExportConfig = ExportConfig()
  ): File? = withContext(Dispatchers.IO) {
    isCancelled = false
    val totalDurationMs = timeline.totalDurationMs.coerceAtLeast(1000L)

    // Verify storage capacity
    val outputDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
    val estimatedBytes = calculateEstimatedSizeBytes(totalDurationMs, config)
    val usableSpace = outputDir.usableSpace
    if (usableSpace < estimatedBytes + 15 * 1024 * 1024L) {
      _exportState.value = ExportState.Error(
        "Insufficient storage space. Need at least ${((estimatedBytes / (1024 * 1024)) + 15)} MB free."
      )
      return@withContext null
    }

    val sanitizedName = projectName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
    val outputFile = File(outputDir, "${sanitizedName}_${System.currentTimeMillis()}.mp4")

    // 1. Attempt export via Media3 Transformer if eligible
    if (canExportWithMedia3Transformer(timeline)) {
      val transformerResult = exportWithMedia3Transformer(timeline, outputFile, config)
      if (transformerResult != null) {
        return@withContext transformerResult
      }
      Log.i(tag, "Media3 Transformer pipeline deferred to hardware composition engine")
    }

    // 2. Hardware composition pipeline with strict MediaCodec & MediaMuxer lifecycle
    return@withContext exportWithHardwarePipeline(projectName, timeline, config, outputFile)
  }

  /**
   * Hardware composition engine using MediaCodec + MediaMuxer with strict lifecycle,
   * timestamp monotonic synchronization, and precise track index handling.
   */
  suspend fun exportWithHardwarePipeline(
    projectName: String,
    timeline: Timeline,
    config: ExportConfig,
    outputFile: File
  ): File? = withContext(Dispatchers.IO) {
    val totalDurationMs = timeline.totalDurationMs.coerceAtLeast(1000L)
    val fps = config.frameRate.fps
    val totalFrames = ((totalDurationMs / 1000f) * fps).toInt().coerceAtLeast(15)

    // Determine target dimensions from resolution and timeline aspect ratio
    val (exportWidth, exportHeight) = getDimensionsForResolution(config.resolution, timeline.aspectRatio)

    var mediaMuxer: MediaMuxer? = null
    var videoEncoder: MediaCodec? = null
    var audioEncoder: MediaCodec? = null
    var eglCore: EglCore? = null
    var windowSurface: WindowSurface? = null
    var gpuRenderer: GpuCompositionRenderer? = null
    var encoderInputSurface: Surface? = null

    // Cache retrievers and bitmaps
    val retrievers = mutableMapOf<String, MediaMetadataRetriever>()
    val imageBitmaps = mutableMapOf<String, Bitmap>()

    try {
      _exportState.value = ExportState.Rendering(0.02f, 0, totalFrames, "Preparing audio & video tracks...")

      // 1. Process & Mix all Audio Tracks
      val hasAudioSources = audioProcessor.hasActiveAudio(timeline)
      var masterPcm: ShortArray = ShortArray(0)
      var hasAudio = false

      if (hasAudioSources) {
        _exportState.value = ExportState.Rendering(0.05f, 0, totalFrames, "Mixing audio tracks...")
        try {
          masterPcm = audioProcessor.mixTimelineAudio(timeline, totalDurationMs) { isCancelled }
        } catch (e: Exception) {
          Log.w(tag, "Audio mixing encountered error: ${e.message}. Continuing with fallback audio.", e)
          masterPcm = ShortArray(0)
        }
        if (isCancelled) {
          cleanUp(null, null, null, outputFile)
          _exportState.value = ExportState.Idle
          return@withContext null
        }
        hasAudio = masterPcm.isNotEmpty()
      }

      // Preload image bitmaps
      for (clip in timeline.videoClips + timeline.overlayClips) {
        if (!clip.isVideo && clip.uri.isNotBlank()) {
          try {
            val uri = Uri.parse(clip.uri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
              val bmp = BitmapFactory.decodeStream(stream)
              if (bmp != null) {
                imageBitmaps[clip.uri] = bmp
              }
            }
          } catch (e: Exception) {
            Log.w(tag, "Failed to load image for clip: ${clip.name}", e)
          }
        }
      }

      // 2. Initialize Video Encoder (H.264 / AVC)
      _exportState.value = ExportState.Rendering(0.08f, 0, totalFrames, "Configuring hardware encoders...")
      val videoMime = MediaFormat.MIMETYPE_VIDEO_AVC
      val bitrate = (calculateEstimatedSizeBytes(totalDurationMs, config) * 8 / (totalDurationMs / 1000f)).toInt()
        .coerceIn(1_500_000, 20_000_000)

      val videoFormat = MediaFormat.createVideoFormat(videoMime, exportWidth, exportHeight).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
        setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
      }

      videoEncoder = MediaCodec.createEncoderByType(videoMime)
      val codecInfo = videoEncoder.codecInfo
      val caps = codecInfo.getCapabilitiesForType(videoMime)
      val chosenColorFormat = if (caps.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)) {
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
      } else if (caps.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)) {
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
      } else {
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
      }

      var useGpuSurface = false

      val supportsSurface = caps.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
      if (supportsSurface) {
        try {
          videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
          videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
          val surface = videoEncoder.createInputSurface()
          encoderInputSurface = surface

          val core = EglCore(null, EglCore.FLAG_RECORDABLE)
          val winSurface = WindowSurface(core, surface, false)
          winSurface.makeCurrent()
          val renderer = GpuCompositionRenderer(context)
          renderer.initGl()

          eglCore = core
          windowSurface = winSurface
          gpuRenderer = renderer

          videoEncoder.start()
          useGpuSurface = true
          Log.i(tag, "GPU hardware surface composition pipeline activated ($exportWidth x $exportHeight @ ${fps}fps)")
        } catch (e: Throwable) {
          Log.w(tag, "GPU surface initialization fallback to buffer pipeline: ${e.message}")
          useGpuSurface = false
          try { windowSurface?.release() } catch (ignored: Exception) {}
          try { eglCore?.release() } catch (ignored: Exception) {}
          try { gpuRenderer?.release() } catch (ignored: Exception) {}
          windowSurface = null
          eglCore = null
          gpuRenderer = null
          try { encoderInputSurface?.release() } catch (ignored: Exception) {}
          encoderInputSurface = null

          // Safely release and recreate fresh encoder instance for byte-buffer pipeline
          try { videoEncoder.stop() } catch (ignored: Exception) {}
          try { videoEncoder.release() } catch (ignored: Exception) {}
          videoEncoder = MediaCodec.createEncoderByType(videoMime)
        }
      }

      if (!useGpuSurface) {
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, chosenColorFormat)
        videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoEncoder.start()
      }

      // 3. Initialize Audio Encoder (AAC) if audio is present
      val audioSampleRate = audioProcessor.sampleRate
      val audioChannels = audioProcessor.channelCount
      if (hasAudio) {
        try {
          val audioMime = MediaFormat.MIMETYPE_AUDIO_AAC
          val aacFormat = MediaFormat.createAudioFormat(audioMime, audioSampleRate, audioChannels).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 192_000)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
          }
          audioEncoder = MediaCodec.createEncoderByType(audioMime)
          audioEncoder.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
          audioEncoder.start()
        } catch (e: Exception) {
          Log.w(tag, "Audio encoder configuration failed: ${e.message}. Continuing export video-only.", e)
          audioEncoder = null
          hasAudio = false
        }
      }

      // 4. Initialize MediaMuxer & MuxerCoordinator
      mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
      val coordinator = MuxerCoordinator(mediaMuxer, hasAudio)

      val frameDurationUs = 1_000_000L / fps
      val frameDurationMs = 1000L / fps

      val frameBitmap = if (!useGpuSurface) Bitmap.createBitmap(exportWidth, exportHeight, Bitmap.Config.ARGB_8888) else null
      val frameCanvas = if (frameBitmap != null) Canvas(frameBitmap) else null
      val pixelBuffer = if (!useGpuSurface) IntArray(exportWidth * exportHeight) else null
      val yuvBuffer = if (!useGpuSurface) ByteArray(exportWidth * exportHeight * 3 / 2) else null

      val totalAudioFrames = masterPcm.size / audioChannels
      var fedAudioFrames = 0

      // 5. Main Interleaved Video & Audio Encoding Loop
      for (frameIndex in 0 until totalFrames) {
        if (isCancelled) {
          try { windowSurface?.release() } catch (ignored: Exception) {}
          try { eglCore?.release() } catch (ignored: Exception) {}
          try { gpuRenderer?.release() } catch (ignored: Exception) {}
          try { encoderInputSurface?.release() } catch (ignored: Exception) {}
          cleanUp(videoEncoder, audioEncoder, mediaMuxer, outputFile)
          _exportState.value = ExportState.Idle
          return@withContext null
        }

        val timelinePosMs = frameIndex * frameDurationMs
        val composedFrame = compositionEngine.evaluateFrame(timeline, timelinePosMs)

        if (useGpuSurface && gpuRenderer != null && windowSurface != null) {
          // Hardware GPU rendering directly to encoder surface
          val mainBmp = fetchClipBitmap(composedFrame.activeClip, composedFrame.clipSourcePosMs, retrievers, imageBitmaps)
          val mainTexId = if (mainBmp != null) {
            gpuRenderer.uploadImageTexture("main_${composedFrame.activeClip?.id ?: "none"}", mainBmp)
          } else 0

          val overlayTexMap = mutableMapOf<String, Int>()
          for (overlay in composedFrame.activeOverlays) {
            val bmp = fetchClipBitmap(overlay.clip, overlay.sourcePosMs, retrievers, imageBitmaps)
            if (bmp != null) {
              val texId = gpuRenderer.uploadImageTexture("overlay_${overlay.clip.id}", bmp)
              overlayTexMap[overlay.clip.id] = texId
            }
          }

          gpuRenderer.render(
            frame = composedFrame,
            mainTextureId = mainTexId,
            isMainOes = false,
            mainTexMatrix = null,
            overlayTextures = overlayTexMap,
            viewportWidth = exportWidth,
            viewportHeight = exportHeight,
            timelineAdjustments = timeline.adjustments,
            timelineFilter = timeline.filter,
            chromaKey = timeline.chromaKey
          )

          val ptsNs = frameIndex * frameDurationUs * 1000L
          windowSurface.setPresentationTime(ptsNs)
          windowSurface.swapBuffers()
        } else {
          // CPU buffer fallback for headless environments
          val mainBmp = fetchClipBitmap(composedFrame.activeClip, composedFrame.clipSourcePosMs, retrievers, imageBitmaps)
          val overlayBmps = mutableMapOf<String, Bitmap>()
          for (overlay in composedFrame.activeOverlays) {
            val bmp = fetchClipBitmap(overlay.clip, overlay.sourcePosMs, retrievers, imageBitmaps)
            if (bmp != null) {
              overlayBmps[overlay.clip.id] = bmp
            }
          }

          if (frameBitmap != null && frameCanvas != null && pixelBuffer != null && yuvBuffer != null) {
            frameBitmap.eraseColor(android.graphics.Color.BLACK)
            compositionEngine.renderFrame(
              canvas = frameCanvas,
              frame = composedFrame,
              mainBitmap = mainBmp,
              overlayBitmaps = overlayBmps,
              canvasWidth = exportWidth,
              canvasHeight = exportHeight,
              chromaKey = timeline.chromaKey
            )

            frameBitmap.getPixels(pixelBuffer, 0, exportWidth, 0, 0, exportWidth, exportHeight)
            if (chosenColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
              encodeYUV420Planar(yuvBuffer, pixelBuffer, exportWidth, exportHeight)
            } else {
              encodeYUV420SemiPlanar(yuvBuffer, pixelBuffer, exportWidth, exportHeight)
            }

            var videoFed = false
            var attempts = 0
            while (!videoFed && attempts < 50) {
              val inIndex = videoEncoder.dequeueInputBuffer(10_000L)
              if (inIndex >= 0) {
                val inBuf = videoEncoder.getInputBuffer(inIndex)
                if (inBuf != null) {
                  inBuf.clear()
                  inBuf.put(yuvBuffer)
                  val ptsUs = frameIndex * frameDurationUs
                  videoEncoder.queueInputBuffer(inIndex, 0, yuvBuffer.size, ptsUs, 0)
                  videoFed = true
                }
              } else {
                drainVideoEncoder(videoEncoder, coordinator, false)
                attempts++
              }
            }
          }
        }

        // Drain video encoder
        drainVideoEncoder(videoEncoder, coordinator, false)

        // Feed corresponding audio packets up to current timeline position
        if (hasAudio && audioEncoder != null) {
          val targetAudioFrames = (((frameIndex + 1).toLong() * totalAudioFrames) / totalFrames).toInt()
          while (fedAudioFrames < targetAudioFrames && fedAudioFrames < totalAudioFrames) {
            val framesToFeed = min(1024, totalAudioFrames - fedAudioFrames)
            val inIndex = audioEncoder.dequeueInputBuffer(5000L)
            if (inIndex >= 0) {
              val inBuf = audioEncoder.getInputBuffer(inIndex)
              if (inBuf != null) {
                inBuf.clear()
                inBuf.order(ByteOrder.LITTLE_ENDIAN)
                val shortBuf = inBuf.asShortBuffer()
                shortBuf.put(masterPcm, fedAudioFrames * audioChannels, framesToFeed * audioChannels)
                val byteSize = framesToFeed * audioChannels * 2
                val audioPtsUs = (fedAudioFrames.toLong() * 1_000_000L) / audioSampleRate
                audioEncoder.queueInputBuffer(inIndex, 0, byteSize, audioPtsUs, 0)
                fedAudioFrames += framesToFeed
              }
            } else {
              drainAudioEncoder(audioEncoder, coordinator, false)
              break
            }
          }
          drainAudioEncoder(audioEncoder, coordinator, false)
        }

        // Update progress
        if (frameIndex % max(1, totalFrames / 40) == 0 || frameIndex == totalFrames - 1) {
          val progress = 0.1f + (0.85f * (frameIndex.toFloat() / totalFrames))
          _exportState.value = ExportState.Rendering(
            progressPercent = progress.coerceIn(0f, 0.95f),
            currentFrame = frameIndex + 1,
            totalFrames = totalFrames,
            status = "Rendering frame ${frameIndex + 1}/$totalFrames"
          )
        }
      }

      // 6. Signal End-Of-Stream to Video Encoder
      if (useGpuSurface) {
        try {
          videoEncoder.signalEndOfInputStream()
        } catch (e: Exception) {
          Log.w(tag, "signalEndOfInputStream: ${e.message}")
        }
      } else {
        val videoEosPtsUs = totalFrames * frameDurationUs
        var videoEosQueued = false
        var eosAttempts = 0
        while (!videoEosQueued && eosAttempts < 100) {
          val inIndex = videoEncoder.dequeueInputBuffer(10_000L)
          if (inIndex >= 0) {
            videoEncoder.queueInputBuffer(inIndex, 0, 0, videoEosPtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            videoEosQueued = true
          } else {
            drainVideoEncoder(videoEncoder, coordinator, false)
            eosAttempts++
          }
        }
      }

      // Drain video encoder until EOS
      drainVideoEncoder(videoEncoder, coordinator, true)

      // 7. Signal End-Of-Stream to Audio Encoder
      if (hasAudio && audioEncoder != null) {
        // Feed any remaining audio samples
        while (fedAudioFrames < totalAudioFrames) {
          val framesToFeed = min(1024, totalAudioFrames - fedAudioFrames)
          val inIndex = audioEncoder.dequeueInputBuffer(10_000L)
          if (inIndex >= 0) {
            val inBuf = audioEncoder.getInputBuffer(inIndex)
            if (inBuf != null) {
              inBuf.clear()
              inBuf.order(ByteOrder.LITTLE_ENDIAN)
              val shortBuf = inBuf.asShortBuffer()
              shortBuf.put(masterPcm, fedAudioFrames * audioChannels, framesToFeed * audioChannels)
              val byteSize = framesToFeed * audioChannels * 2
              val audioPtsUs = (fedAudioFrames.toLong() * 1_000_000L) / audioSampleRate
              audioEncoder.queueInputBuffer(inIndex, 0, byteSize, audioPtsUs, 0)
              fedAudioFrames += framesToFeed
            }
          } else {
            drainAudioEncoder(audioEncoder, coordinator, false)
          }
        }

        // Send Audio EOS
        var audioEosQueued = false
        var audioEosAttempts = 0
        val audioEosPtsUs = (totalAudioFrames.toLong() * 1_000_000L) / audioSampleRate
        while (!audioEosQueued && audioEosAttempts < 100) {
          val inIndex = audioEncoder.dequeueInputBuffer(10_000L)
          if (inIndex >= 0) {
            audioEncoder.queueInputBuffer(inIndex, 0, 0, audioEosPtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            audioEosQueued = true
          } else {
            drainAudioEncoder(audioEncoder, coordinator, false)
            audioEosAttempts++
          }
        }

        drainAudioEncoder(audioEncoder, coordinator, true)
      }

      // 8. Finalize Muxer
      if (coordinator.isStarted) {
        try {
          mediaMuxer.stop()
        } catch (e: Exception) {
          Log.w(tag, "Muxer stop warning", e)
        }
      }

      // Release hardware encoders and muxer
      try { videoEncoder.stop() } catch (ignored: Exception) {}
      videoEncoder.release()
      videoEncoder = null

      try { audioEncoder?.stop() } catch (ignored: Exception) {}
      audioEncoder?.release()
      audioEncoder = null

      mediaMuxer.release()
      mediaMuxer = null

      // Validate output file
      val finalSize = outputFile.length()
      if (finalSize > 1024L) {
        // Validate playable media headers
        val isValid = validatePlayableMp4(outputFile)
        if (isValid) {
          _exportState.value = ExportState.Success(outputFile, totalDurationMs, finalSize)
          return@withContext outputFile
        } else {
          _exportState.value = ExportState.Error("Exported MP4 header validation failed")
          cleanUp(null, null, null, outputFile)
          return@withContext null
        }
      } else {
        _exportState.value = ExportState.Error("Export resulted in incomplete or empty file")
        cleanUp(null, null, null, outputFile)
        return@withContext null
      }
    } catch (e: OutOfMemoryError) {
      Log.e(tag, "Export OutOfMemoryError", e)
      cleanUp(videoEncoder, audioEncoder, mediaMuxer, outputFile)
      _exportState.value = ExportState.Error("Out of memory during video rendering")
      return@withContext null
    } catch (e: Exception) {
      Log.e(tag, "Export failed with exception", e)
      cleanUp(videoEncoder, audioEncoder, mediaMuxer, outputFile)
      val errorMsg = when {
        e is MediaCodec.CodecException -> "Encoder failure: ${e.diagnosticInfo}"
        e.message != null -> e.message!!
        else -> "Video export pipeline failed"
      }
      _exportState.value = ExportState.Error(errorMsg)
      return@withContext null
    } finally {
      try { windowSurface?.release() } catch (ignored: Exception) {}
      try { eglCore?.release() } catch (ignored: Exception) {}
      try { gpuRenderer?.release() } catch (ignored: Exception) {}
      try { encoderInputSurface?.release() } catch (ignored: Exception) {}
      for (r in retrievers.values) {
        try { r.release() } catch (ignored: Exception) {}
      }
      retrievers.clear()
      imageBitmaps.clear()
      audioProcessor.clearCache()
    }
  }

  fun release() {
    isCancelled = true
    audioProcessor.clearCache()
  }

  private fun drainVideoEncoder(
    encoder: MediaCodec,
    coordinator: MuxerCoordinator,
    endOfStream: Boolean
  ) {
    val bufferInfo = MediaCodec.BufferInfo()
    var attempts = 0
    while (attempts < 50) {
      val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 5000L)
      if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
        if (!endOfStream) break
        attempts++
      } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        val newFormat = encoder.outputFormat
        coordinator.setVideoFormat(newFormat)
      } else if (outputBufferIndex >= 0) {
        val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
        if (outputBuffer != null) {
          coordinator.writeVideoSample(outputBuffer, bufferInfo)
        }
        encoder.releaseOutputBuffer(outputBufferIndex, false)
        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          break
        }
      }
    }
  }

  private fun drainAudioEncoder(
    encoder: MediaCodec,
    coordinator: MuxerCoordinator,
    endOfStream: Boolean
  ) {
    val bufferInfo = MediaCodec.BufferInfo()
    var attempts = 0
    while (attempts < 50) {
      val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 5000L)
      if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
        if (!endOfStream) break
        attempts++
      } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        val newFormat = encoder.outputFormat
        coordinator.setAudioFormat(newFormat)
      } else if (outputBufferIndex >= 0) {
        val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
        if (outputBuffer != null) {
          coordinator.writeAudioSample(outputBuffer, bufferInfo)
        }
        encoder.releaseOutputBuffer(outputBufferIndex, false)
        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          break
        }
      }
    }
  }

  private fun validatePlayableMp4(file: File): Boolean {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(file.absolutePath)
      val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
      hasVideo != null
    } catch (e: Exception) {
      Log.w(tag, "Failed to validate MP4 with retriever", e)
      false
    } finally {
      try { retriever.release() } catch (ignored: Exception) {}
    }
  }

  private fun fetchClipBitmap(
    clip: VideoClip?,
    sourcePosMs: Long,
    retrievers: MutableMap<String, MediaMetadataRetriever>,
    imageBitmaps: MutableMap<String, Bitmap>
  ): Bitmap? {
    if (clip == null || clip.uri.isBlank()) return null

    if (!clip.isVideo) {
      return imageBitmaps[clip.uri]
    }

    val retriever = retrievers.getOrPut(clip.uri) {
      MediaMetadataRetriever().apply {
        try {
          val uri = Uri.parse(clip.uri)
          if (uri.scheme == "content" || uri.scheme == "file") {
            setDataSource(context, uri)
          } else {
            setDataSource(clip.uri)
          }
        } catch (e: Exception) {
          Log.w(tag, "Could not set data source on retriever for ${clip.uri}", e)
        }
      }
    }

    return try {
      val sourceUs = sourcePosMs * 1000L
      retriever.getFrameAtTime(sourceUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        ?: retriever.getFrameAtTime(sourceUs, MediaMetadataRetriever.OPTION_CLOSEST)
    } catch (e: Exception) {
      null
    }
  }

  private fun cleanUp(
    videoEncoder: MediaCodec?,
    audioEncoder: MediaCodec?,
    mediaMuxer: MediaMuxer?,
    outputFile: File
  ) {
    try { videoEncoder?.stop() } catch (ignored: Exception) {}
    try { videoEncoder?.release() } catch (ignored: Exception) {}
    try { audioEncoder?.stop() } catch (ignored: Exception) {}
    try { audioEncoder?.release() } catch (ignored: Exception) {}
    try { mediaMuxer?.release() } catch (ignored: Exception) {}
    if (outputFile.exists()) {
      outputFile.delete()
    }
  }

  private fun getDimensionsForResolution(res: Resolution, aspect: AspectRatio): Pair<Int, Int> {
    val longEdge = when (res) {
      Resolution.RES_480P -> 854
      Resolution.RES_720P -> 1280
      Resolution.RES_1080P -> 1920
      Resolution.RES_2K -> 2560
      Resolution.RES_4K -> 3840
    }
    val shortEdge = (longEdge / aspect.ratio).toInt()
    val w = if (aspect.ratio >= 1.0f) longEdge else shortEdge
    val h = if (aspect.ratio >= 1.0f) shortEdge else longEdge
    val evenW = (w / 2) * 2
    val evenH = (h / 2) * 2
    return Pair(evenW.coerceAtLeast(320), evenH.coerceAtLeast(320))
  }

  private fun encodeYUV420SemiPlanar(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
    val frameSize = width * height
    var yIndex = 0
    var uvIndex = frameSize

    var a: Int
    var R: Int
    var G: Int
    var B: Int
    var Y: Int
    var U: Int
    var V: Int
    var index = 0

    for (j in 0 until height) {
      for (i in 0 until width) {
        val pixel = argb[index++]
        R = (pixel and 0xff0000) shr 16
        G = (pixel and 0xff00) shr 8
        B = pixel and 0xff

        Y = ((66 * R + 129 * G + 25 * B + 128) shr 8) + 16
        U = ((-38 * R - 74 * G + 112 * B + 128) shr 8) + 128
        V = ((112 * R - 94 * G - 18 * B + 128) shr 8) + 128

        yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()

        if (j % 2 == 0 && index % 2 == 0) {
          yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
          yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
        }
      }
    }
  }

  private fun encodeYUV420Planar(yuv420p: ByteArray, argb: IntArray, width: Int, height: Int) {
    val frameSize = width * height
    val qFrameSize = frameSize / 4
    var yIndex = 0
    var uIndex = frameSize
    var vIndex = frameSize + qFrameSize

    var R: Int
    var G: Int
    var B: Int
    var Y: Int
    var U: Int
    var V: Int
    var index = 0

    for (j in 0 until height) {
      for (i in 0 until width) {
        val pixel = argb[index++]
        R = (pixel and 0xff0000) shr 16
        G = (pixel and 0xff00) shr 8
        B = pixel and 0xff

        Y = ((66 * R + 129 * G + 25 * B + 128) shr 8) + 16
        U = ((-38 * R - 74 * G + 112 * B + 128) shr 8) + 128
        V = ((112 * R - 94 * G - 18 * B + 128) shr 8) + 128

        yuv420p[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()

        if (j % 2 == 0 && index % 2 == 0) {
          yuv420p[uIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
          yuv420p[vIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
        }
      }
    }
  }
}
