package com.example.engine.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.*
import android.net.Uri
import android.util.Log
import android.view.Surface
import com.example.domain.model.*
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
                Log.e(tag, "Failed to add video track", e)
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
                Log.e(tag, "Failed to add audio track", e)
            }
        }
    }

    private fun checkStart() {
        val videoReady = videoTrackIndex >= 0
        val audioReady = !hasAudio || audioTrackIndex >= 0

        if (videoReady && audioReady && !isStarted) {
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
                    Log.w(tag, "Dropped frame on muxer flush: ${e.message}")
                }
            }
        }
        pendingQueue.clear()
    }

    private fun enforceMonotonicPts(isAudio: Boolean, requestedPtsUs: Long): Long {
        return if (isAudio) {
            val pts = if (requestedPtsUs <= lastAudioPtsUs) lastAudioPtsUs + 1000L else requestedPtsUs
            lastAudioPtsUs = pts
            pts
        } else {
            val pts = if (requestedPtsUs <= lastVideoPtsUs) lastVideoPtsUs + 1000L else requestedPtsUs
            lastVideoPtsUs = pts
            pts
        }
    }

    @Synchronized
    fun writeVideoSample(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 || info.size <= 0) return

        if (isStarted && videoTrackIndex >= 0) {
            val pts = enforceMonotonicPts(false, info.presentationTimeUs)
            info.presentationTimeUs = pts
            try {
                mediaMuxer.writeSampleData(videoTrackIndex, buffer, info)
            } catch (e: Exception) {
                Log.w(tag, "Error writing video sample", e)
            }
        } else {
            val bytes = ByteArray(info.size)
            buffer.position(info.offset)
            buffer.get(bytes)
            pendingQueue.add(QueuedPacket(false, bytes, info.presentationTimeUs, info.flags))
        }
    }

    @Synchronized
    fun writeAudioSample(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 || info.size <= 0) return

        if (isStarted && audioTrackIndex >= 0) {
            val pts = enforceMonotonicPts(true, info.presentationTimeUs)
            info.presentationTimeUs = pts
            try {
                mediaMuxer.writeSampleData(audioTrackIndex, buffer, info)
            } catch (e: Exception) {
                Log.w(tag, "Error writing audio sample", e)
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
    private val audioProcessor = AudioExportProcessor(context)

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    @Volatile
    private var isCancelled = false

    fun cancelExport() {
        isCancelled = true
    }

    fun release() {
        isCancelled = true
        audioProcessor.clearCache()
    }

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

    private fun getDimensionsForResolution(resolution: Resolution, aspectRatio: AspectRatio): Pair<Int, Int> {
        val baseH = when (resolution) {
            Resolution.RES_480P -> 480
            Resolution.RES_720P -> 720
            Resolution.RES_1080P -> 1080
            Resolution.RES_2K -> 1440
            Resolution.RES_4K -> 2160
        }
        val ratio = when (aspectRatio) {
            AspectRatio.RATIO_9_16 -> 9f / 16f
            AspectRatio.RATIO_16_9 -> 16f / 9f
            AspectRatio.RATIO_1_1 -> 1f
            AspectRatio.RATIO_4_5 -> 4f / 5f
        }
        var width = (baseH * ratio).toInt()
        var height = baseH
        width = (width / 16) * 16
        height = (height / 16) * 16
        return Pair(width.coerceAtLeast(320), height.coerceAtLeast(320))
    }

    suspend fun exportProject(
        projectName: String,
        timeline: Timeline,
        config: ExportConfig = ExportConfig()
    ): File? = withContext(Dispatchers.IO) {
        isCancelled = false
        val totalDurationMs = timeline.totalDurationMs.coerceAtLeast(1000L)

        val outputDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
        val sanitizedName = projectName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val outputFile = File(outputDir, "${sanitizedName}_${System.currentTimeMillis()}.mp4")

        val (exportWidth, exportHeight) = getDimensionsForResolution(config.resolution, timeline.aspectRatio)
        val fps = config.frameRate.fps
        val totalFrames = ((totalDurationMs / 1000f) * fps).toInt().coerceAtLeast(15)

        var mediaMuxer: MediaMuxer? = null
        var videoEncoder: MediaCodec? = null
        var audioEncoder: MediaCodec? = null
        var eglCore: EglCore? = null
        var windowSurface: WindowSurface? = null
        var gpuRenderer: GpuCompositionRenderer? = null
        var encoderInputSurface: Surface? = null

        try {
            _exportState.value = ExportState.Rendering(0.02f, 0, totalFrames, "Preparing audio tracks...")

            val hasAudio = audioProcessor.hasActiveAudio(timeline)
            var masterPcm = ShortArray(0)
            if (hasAudio) {
                masterPcm = audioProcessor.mixTimelineAudio(timeline, totalDurationMs) { isCancelled }
            }

            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerCoordinator = MuxerCoordinator(mediaMuxer, hasAudio && masterPcm.isNotEmpty())

            val videoMime = MediaFormat.MIMETYPE_VIDEO_AVC
            val bitrate = (config.customBitrateKbps * 1000).coerceIn(1_500_000, 25_000_000)

            val videoFormat = MediaFormat.createVideoFormat(videoMime, exportWidth, exportHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain)
            }

            videoEncoder = MediaCodec.createEncoderByType(videoMime)
            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoderInputSurface = videoEncoder.createInputSurface()

            eglCore = EglCore(null, EglCore.FLAG_RECORDABLE)
            windowSurface = WindowSurface(eglCore, encoderInputSurface, false)
            windowSurface.makeCurrent()

            gpuRenderer = GpuCompositionRenderer(context)
            gpuRenderer.initGl()

            videoEncoder.start()

            if (hasAudio && masterPcm.isNotEmpty()) {
                val audioMime = MediaFormat.MIMETYPE_AUDIO_AAC
                val audioFormat = MediaFormat.createAudioFormat(audioMime, audioProcessor.sampleRate, audioProcessor.channelCount).apply {
                    setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
                }
                audioEncoder = MediaCodec.createEncoderByType(audioMime)
                audioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                audioEncoder.start()

                encodePcmAudio(audioEncoder, muxerCoordinator, masterPcm, audioProcessor.sampleRate, audioProcessor.channelCount)
            }

            val bufferInfo = MediaCodec.BufferInfo()
            val frameIntervalUs = 1_000_000L / fps

            for (frame in 0 until totalFrames) {
                if (isCancelled) {
                    cleanUp(videoEncoder, audioEncoder, encoderInputSurface, windowSurface, eglCore, gpuRenderer, mediaMuxer, outputFile)
                    _exportState.value = ExportState.Idle
                    return@withContext null
                }

                val currentPtsUs = frame * frameIntervalUs

                gpuRenderer.renderTimelineFrame(timeline, currentPtsUs / 1000L, exportWidth, exportHeight)
                windowSurface.setPresentationTime(currentPtsUs * 1000L)
                windowSurface.swapBuffers()

                drainEncoder(videoEncoder, muxerCoordinator, bufferInfo, false, isAudio = false)

                val progress = (frame.toFloat() / totalFrames.toFloat()).coerceIn(0.05f, 0.95f)
                _exportState.value = ExportState.Rendering(progress, frame, totalFrames, "Rendering frame $frame of $totalFrames")
            }

            videoEncoder.signalEndOfInputStream()
            drainEncoder(videoEncoder, muxerCoordinator, bufferInfo, true, isAudio = false)

            cleanUp(videoEncoder, audioEncoder, encoderInputSurface, windowSurface, eglCore, gpuRenderer, mediaMuxer, null)

            if (outputFile.exists() && outputFile.length() > 1024L) {
                _exportState.value = ExportState.Success(outputFile, totalDurationMs, outputFile.length())
                return@withContext outputFile
            } else {
                _exportState.value = ExportState.Error("Output file empty or failed.")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e(tag, "Export failed: ${e.message}", e)
            cleanUp(videoEncoder, audioEncoder, encoderInputSurface, windowSurface, eglCore, gpuRenderer, mediaMuxer, outputFile)
            _exportState.value = ExportState.Error(e.message ?: "Export failure")
            return@withContext null
        }
    }

    private fun encodePcmAudio(
        audioEncoder: MediaCodec,
        muxerCoordinator: MuxerCoordinator,
        pcmData: ShortArray,
        sampleRate: Int,
        channelCount: Int
    ) {
        val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        val shortBuffer = byteBuffer.asShortBuffer()
        shortBuffer.put(pcmData)
        val audioBytes = byteBuffer.array()

        var inputOffset = 0
        val bufferInfo = MediaCodec.BufferInfo()
        var ptsUs = 0L
        var inputDone = false

        while (!inputDone) {
            val inIndex = audioEncoder.dequeueInputBuffer(5000L)
            if (inIndex >= 0) {
                val inputBuf = audioEncoder.getInputBuffer(inIndex)
                if (inputBuf != null) {
                    inputBuf.clear()
                    val remaining = audioBytes.size - inputOffset
                    val chunkSize = minOf(remaining, inputBuf.remaining())
                    if (chunkSize > 0) {
                        inputBuf.put(audioBytes, inputOffset, chunkSize)
                        audioEncoder.queueInputBuffer(inIndex, 0, chunkSize, ptsUs, 0)
                        inputOffset += chunkSize
                        val samplesFed = chunkSize / (2 * channelCount)
                        ptsUs += ((samplesFed.toDouble() / sampleRate) * 1_000_000L).toLong()
                    } else {
                        audioEncoder.queueInputBuffer(inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    }
                }
            }
            drainEncoder(audioEncoder, muxerCoordinator, bufferInfo, false, isAudio = true)
        }

        drainEncoder(audioEncoder, muxerCoordinator, bufferInfo, true, isAudio = true)
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxerCoordinator: MuxerCoordinator,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        isAudio: Boolean
    ) {
        val timeoutUs = 5000L
        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (isAudio) {
                        muxerCoordinator.setAudioFormat(encoder.outputFormat)
                    } else {
                        muxerCoordinator.setVideoFormat(encoder.outputFormat)
                    }
                }
                outputIndex >= 0 -> {
                    val encodedData = encoder.getOutputBuffer(outputIndex)
                    if (encodedData != null && bufferInfo.size > 0) {
                        if (isAudio) {
                            muxerCoordinator.writeAudioSample(encodedData, bufferInfo)
                        } else {
                            muxerCoordinator.writeVideoSample(encodedData, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }
    }

    private fun cleanUp(
        videoEncoder: MediaCodec?,
        audioEncoder: MediaCodec?,
        surface: Surface?,
        windowSurface: WindowSurface?,
        eglCore: EglCore?,
        gpuRenderer: GpuCompositionRenderer?,
        mediaMuxer: MediaMuxer?,
        failedFile: File?
    ) {
        try { videoEncoder?.stop() } catch (ignored: Exception) {}
        try { videoEncoder?.release() } catch (ignored: Exception) {}
        try { audioEncoder?.stop() } catch (ignored: Exception) {}
        try { audioEncoder?.release() } catch (ignored: Exception) {}
        try { surface?.release() } catch (ignored: Exception) {}
        try { windowSurface?.release() } catch (ignored: Exception) {}
        try { gpuRenderer?.release() } catch (ignored: Exception) {}
        try { eglCore?.release() } catch (ignored: Exception) {}
        try { mediaMuxer?.stop() } catch (ignored: Exception) {}
        try { mediaMuxer?.release() } catch (ignored: Exception) {}
        if (failedFile != null && failedFile.exists()) {
            failedFile.delete()
        }
    }
}
