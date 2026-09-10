package com.example.engine.export

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGLContext
import android.util.Log
import android.view.Surface
import com.example.domain.model.TimelineProject
import com.example.engine.composition.gpu.EglCore
import com.example.engine.composition.gpu.GpuCompositionRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

class VideoExporter(private val context: Context) {

    companion object {
        private const val TAG = "VideoExporter"
        private const val MIME_TYPE_VIDEO = MediaFormat.MIMETYPE_VIDEO_AVC // H.264
        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 1
        private const val TIMEOUT_USEC = 10000L
    }

    suspend fun exportVideo(
        project: TimelineProject,
        outputFile: File,
        width: Int = 1080,
        height: Int = 1920,
        bitRate: Int = 8_000_000,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.Default) {
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: Surface? = null
        var eglCore: EglCore? = null

        try {
            if (outputFile.exists()) {
                outputFile.delete()
            }

            // 1. Prepare Video Encoder Format
            val format = MediaFormat.createVideoFormat(MIME_TYPE_VIDEO, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                setInteger(
                    MediaFormat.KEY_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileMain
                )
            }

            encoder = MediaCodec.createEncoderByType(MIME_TYPE_VIDEO)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            // 2. Setup EGL Offscreen Rendering Surface
            eglCore = EglCore(null, EglCore.FLAG_RECORDABLE)
            val windowSurface = eglCore.createWindowSurface(inputSurface)
            windowSurface.makeCurrent()

            // 3. Setup Muxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var isMuxerStarted = false

            val renderer = GpuCompositionRenderer(context)
            val totalDurationUs = project.durationUs
            val frameIntervalUs = 1_000_000L / FRAME_RATE
            var currentPresentationTimeUs = 0L
            var lastEncodedPts = -1L

            val bufferInfo = MediaCodec.BufferInfo()

            // 4. Frame by Frame Render Loop
            while (currentPresentationTimeUs <= totalDurationUs) {
                // Render project frame via OpenGL
                renderer.renderFrameAt(currentPresentationTimeUs, width, height)

                // EGL timestamp & buffer swap
                windowSurface.setPresentationTime(currentPresentationTimeUs * 1000L) // Nanoseconds
                windowSurface.swapBuffers()

                // Drain Encoder Buffers
                var encoderOutputDone = false
                while (!encoderOutputDone) {
                    val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                    when {
                        outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            encoderOutputDone = true
                        }
                        outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (isMuxerStarted) {
                                throw IllegalStateException("Format changed twice")
                            }
                            val newFormat = encoder.outputFormat
                            videoTrackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            isMuxerStarted = true
                        }
                        outputBufferIndex >= 0 -> {
                            val encodedData: ByteBuffer = encoder.getOutputBuffer(outputBufferIndex)
                                ?: throw RuntimeException("Encoder output buffer $outputBufferIndex was null")

                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }

                            if (bufferInfo.size != 0 && isMuxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)

                                // Prevent PTS Crash (Strictly Increasing Timestamps)
                                if (bufferInfo.presentationTimeUs <= lastEncodedPts) {
                                    bufferInfo.presentationTimeUs = lastEncodedPts + 1000L
                                }
                                lastEncodedPts = bufferInfo.presentationTimeUs

                                muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                            }

                            encoder.releaseOutputBuffer(outputBufferIndex, false)
                        }
                    }
                }

                val progress = (currentPresentationTimeUs.toFloat() / totalDurationUs.toFloat()).coerceIn(0f, 1f)
                onProgress(progress)

                currentPresentationTimeUs += frameIntervalUs
            }

            // 5. Signal End of Stream
            encoder.signalEndOfInputStream()
            var eosDone = false
            while (!eosDone) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                if (outputBufferIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eosDone = true
                    }
                    val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                    if (encodedData != null && bufferInfo.size > 0 && isMuxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }

            // Cleanup & Flush
            windowSurface.release()
            renderer.release()
            onProgress(1.0f)
            Result.success(outputFile)

        } catch (e: Exception) {
            Log.e(TAG, "Export Pipeline Failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                encoder?.stop()
                encoder?.release()
                inputSurface?.release()
                eglCore?.release()
                muxer?.stop()
                muxer?.release()
            } catch (ignored: Exception) {
                // Ignore safe cleanup release crashes
            }
        }
    }
}
