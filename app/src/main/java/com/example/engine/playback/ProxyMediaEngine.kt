package com.example.engine.playback

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.domain.model.VideoClip
import com.example.engine.memory.EngineMemoryManager
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class PreviewQuality(val maxDimension: Int, val label: String) {
  HIGH(1920, "1080p Native (High Quality)"),
  BALANCED(1280, "720p Proxy (Balanced)"),
  PERFORMANCE(720, "540p Fast Scrub (High FPS)")
}

/**
 * High-performance Proxy Media & Adaptive Preview Engine.
 * Provides low-resolution proxy frame generation, background pre-fetching,
 * and adaptive rendering quality scaling to maintain 60 FPS timeline scrubbing.
 */
class ProxyMediaEngine(private val context: Context) {
  private val tag = "ProxyMediaEngine"
  private val memoryManager = EngineMemoryManager.getInstance(context)
  private val retrieverCache = ConcurrentHashMap<String, MediaMetadataRetriever>()
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  var currentQuality: PreviewQuality = PreviewQuality.BALANCED

  /**
   * Retrieves or extracts a cached preview frame bitmap for a clip at a given source timestamp.
   */
  suspend fun getProxyFrame(clip: VideoClip, sourcePosMs: Long): Bitmap? = withContext(Dispatchers.IO) {
    if (clip.uri.isBlank() || !clip.isVideo) return@withContext null

    val targetDim = currentQuality.maxDimension
    val cacheKey = "proxy_${clip.id}_${sourcePosMs / 100}_$targetDim"

    // Check memory cache first
    memoryManager.renderFrameCache.get(cacheKey)?.let { return@withContext it }

    try {
      val retriever = retrieverCache.getOrPut(clip.uri) {
        MediaMetadataRetriever().apply { setDataSource(context, android.net.Uri.parse(clip.uri)) }
      }

      val timeUs = sourcePosMs * 1000L
      val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return@withContext null

      val scaled = if (frame.width > targetDim || frame.height > targetDim) {
        val scale = targetDim.toFloat() / kotlin.math.max(frame.width, frame.height)
        val sw = (frame.width * scale).toInt().coerceAtLeast(1)
        val sh = (frame.height * scale).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(frame, sw, sh, true)
      } else {
        frame
      }

      memoryManager.renderFrameCache.put(cacheKey, scaled)
      scaled
    } catch (e: Exception) {
      Log.w(tag, "Failed to extract proxy frame for clip ${clip.id} at $sourcePosMs ms", e)
      null
    }
  }

  /**
   * Pre-fetches adjacent frames around the current CTI playhead position for smooth scrubbing.
   */
  fun prefetchFramesAround(clip: VideoClip, currentPosMs: Long, rangeMs: Long = 2000L) {
    scope.launch {
      val startMs = (currentPosMs - rangeMs).coerceAtLeast(0L)
      val endMs = (currentPosMs + rangeMs).coerceAtMost(clip.durationMs)
      var pos = startMs
      while (pos <= endMs) {
        getProxyFrame(clip, pos)
        pos += 250L // prefetch every 250ms interval
      }
    }
  }

  fun release() {
    for ((_, retriever) in retrieverCache) {
      try { retriever.release() } catch (ignored: Exception) {}
    }
    retrieverCache.clear()
  }
}
