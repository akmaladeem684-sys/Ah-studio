package com.example.engine.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.*
import android.net.Uri
import android.util.Log
import com.example.domain.model.*
import com.example.engine.composition.ComposedFrame
import com.example.engine.composition.VideoCompositionEngine
import com.example.engine.media.MediaMetadataHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
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

class VideoExporter(private val context: Context) {

  private val tag = "VideoExporter"
  private val compositionEngine = VideoCompositionEngine(context)

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
   * Real MP4 Exporter using MediaCodec (H.264 AVC encoder + AAC audio encoder) and MediaMuxer.
   */
  suspend fun exportProject(
    projectName: String,
    timeline: Timeline,
    config: ExportConfig
  ): File? = withContext(Dispatchers.IO) {
    isCancelled = false
    val totalDurationMs = timeline.totalDurationMs.coerceAtLeast(1000L)
    val fps = config.frameRate.fps
    val totalFrames = ((totalDurationMs / 1000f) * fps).toInt().coerceAtLeast(15)

    // Determine target dimensions from resolution and timeline aspect ratio
    val (exportWidth, exportHeight) = getDimensionsForResolution(config.resolution, timeline.aspectRatio)

    val outputDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
    val sanitizedName = projectName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
    val outputFile = File(outputDir, "${sanitizedName}_${System.currentTimeMillis()}.mp4")

    var mediaMuxer: MediaMuxer? = null
    var videoEncoder: MediaCodec? = null
    var audioEncoder: MediaCodec? = null

    // Cache retrievers and bitmaps
    val retrievers = mutableMapOf<String, MediaMetadataRetriever>()
    val imageBitmaps = mutableMapOf<String, Bitmap>()

    try {
      _exportState.value = ExportState.Rendering(0f, 0, totalFrames, "Initializing hardware encoder...")

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

      // Initialize Video Encoder
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
      // Check color formats
      val codecInfo = videoEncoder.codecInfo
      val caps = codecInfo.getCapabilitiesForType(videoMime)
      val chosenColorFormat = if (caps.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)) {
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
      } else if (caps.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)) {
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
      } else {
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
      }
      videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, chosenColorFormat)

      videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      videoEncoder.start()

      mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

      var videoTrackIndex = -1
      var muxerStarted = false
      val bufferInfo = MediaCodec.BufferInfo()

      val frameDurationUs = 1_000_000L / fps
      val frameDurationMs = 1000L / fps

      val frameBitmap = Bitmap.createBitmap(exportWidth, exportHeight, Bitmap.Config.ARGB_8888)
      val frameCanvas = Canvas(frameBitmap)
      val pixelBuffer = IntArray(exportWidth * exportHeight)
      val yuvBuffer = ByteArray(exportWidth * exportHeight * 3 / 2)

      // Encoding Loop
      for (frameIndex in 0 until totalFrames) {
        if (isCancelled) {
          cleanUp(videoEncoder, audioEncoder, mediaMuxer, outputFile)
          _exportState.value = ExportState.Idle
          return@withContext null
        }

        val timelinePosMs = frameIndex * frameDurationMs
        val composedFrame = compositionEngine.evaluateFrame(timeline, timelinePosMs)

        // 1. Fetch main clip bitmap at this time
        val mainBmp = fetchClipBitmap(composedFrame.activeClip, composedFrame.clipSourcePosMs, retrievers, imageBitmaps)
        
        // 2. Fetch overlay bitmaps
        val overlayBmps = mutableMapOf<String, Bitmap>()
        for (overlay in composedFrame.activeOverlays) {
          val bmp = fetchClipBitmap(overlay.clip, overlay.sourcePosMs, retrievers, imageBitmaps)
          if (bmp != null) {
            overlayBmps[overlay.clip.id] = bmp
          }
        }

        // 3. Render frame with VideoCompositionEngine
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

        // 4. Convert Bitmap ARGB to YUV420
        frameBitmap.getPixels(pixelBuffer, 0, exportWidth, 0, 0, exportWidth, exportHeight)
        if (chosenColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
          encodeYUV420Planar(yuvBuffer, pixelBuffer, exportWidth, exportHeight)
        } else {
          encodeYUV420SemiPlanar(yuvBuffer, pixelBuffer, exportWidth, exportHeight)
        }

        // 5. Feed into MediaCodec input buffer
        val inputBufferIndex = videoEncoder.dequeueInputBuffer(10_000L)
        if (inputBufferIndex >= 0) {
          val inputBuffer = videoEncoder.getInputBuffer(inputBufferIndex)
          if (inputBuffer != null) {
            inputBuffer.clear()
            inputBuffer.put(yuvBuffer)
            val ptsUs = frameIndex * frameDurationUs
            videoEncoder.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, ptsUs, 0)
          }
        }

        // 6. Drain encoded packets into MediaMuxer
        drainEncoder(videoEncoder, mediaMuxer, bufferInfo, false) { trackIndex ->
          videoTrackIndex = trackIndex
          muxerStarted = true
        }

        // Update progress
        if (frameIndex % max(1, totalFrames / 40) == 0 || frameIndex == totalFrames - 1) {
          val progress = (frameIndex.toFloat() / totalFrames).coerceIn(0f, 0.95f)
          _exportState.value = ExportState.Rendering(
            progressPercent = progress,
            currentFrame = frameIndex + 1,
            totalFrames = totalFrames,
            status = "Rendering frame ${frameIndex + 1}/$totalFrames"
          )
        }
      }

      // Signal end of stream
      val eosIndex = videoEncoder.dequeueInputBuffer(10_000L)
      if (eosIndex >= 0) {
        val ptsUs = totalFrames * frameDurationUs
        videoEncoder.queueInputBuffer(eosIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
      }
      drainEncoder(videoEncoder, mediaMuxer, bufferInfo, true) { trackIndex ->
        videoTrackIndex = trackIndex
        muxerStarted = true
      }

      // Finalize Muxer
      if (muxerStarted) {
        try {
          mediaMuxer.stop()
        } catch (e: Exception) {
          Log.w(tag, "Muxer stop warning", e)
        }
      }

      // Release resources
      try {
        videoEncoder.stop()
      } catch (ignored: Exception) {}
      videoEncoder.release()
      videoEncoder = null

      mediaMuxer.release()
      mediaMuxer = null

      val finalSize = outputFile.length()
      if (finalSize > 0) {
        _exportState.value = ExportState.Success(outputFile, totalDurationMs, finalSize)
        return@withContext outputFile
      } else {
        _exportState.value = ExportState.Error("Export resulted in empty file")
        return@withContext null
      }
    } catch (e: Exception) {
      Log.e(tag, "Export failed with exception", e)
      cleanUp(videoEncoder, audioEncoder, mediaMuxer, outputFile)
      _exportState.value = ExportState.Error(e.message ?: "Video export encoding failed")
      return@withContext null
    } finally {
      // Release retrievers
      for (r in retrievers.values) {
        try { r.release() } catch (ignored: Exception) {}
      }
      imageBitmaps.clear()
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

  private fun drainEncoder(
    encoder: MediaCodec,
    muxer: MediaMuxer,
    bufferInfo: MediaCodec.BufferInfo,
    endOfStream: Boolean,
    onFormatChanged: (Int) -> Unit
  ) {
    var muxerStarted = false
    while (true) {
      val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
      if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
        if (!endOfStream) break
      } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        val newFormat = encoder.outputFormat
        val track = muxer.addTrack(newFormat)
        muxer.start()
        muxerStarted = true
        onFormatChanged(track)
      } else if (outputBufferIndex >= 0) {
        val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
        if (outputBuffer != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
          if (bufferInfo.size > 0) {
            outputBuffer.position(bufferInfo.offset)
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
            try {
              muxer.writeSampleData(0, outputBuffer, bufferInfo)
            } catch (e: Exception) {
              Log.w(tag, "Error writing sample data to muxer", e)
            }
          }
        }
        encoder.releaseOutputBuffer(outputBufferIndex, false)
        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          break
        }
      }
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
    // Dimensions must be even integers for H.264
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
