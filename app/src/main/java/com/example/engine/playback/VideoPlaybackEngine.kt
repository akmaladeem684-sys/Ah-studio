package com.example.engine.playback

import android.content.Context
import android.graphics.ColorMatrix
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.domain.model.Timeline
import com.example.domain.model.VideoClip
import com.example.engine.composition.ColorFilterGenerator
import com.example.engine.media.MediaRelinkManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Ultra-smooth, production-ready Video Playback & Timeline Scrubbing Engine.
 * Built for 60 FPS preview target with request coalescing, background frame pre-fetching,
 * hardware filter pipeline, and memory safety.
 */
@OptIn(UnstableApi::class)
class VideoPlaybackEngine(
  private val context: Context,
  private val onTimelinePositionChanged: (Long) -> Unit,
  private val onPlaybackEnded: () -> Unit,
  private val proxyEngine: ProxyMediaEngine? = null
) {
  companion object {
    private const val TAG = "VideoPlaybackEngine"
    private const val FRAME_INTERVAL_60FPS_MS = 16L
  }

  val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
    playWhenReady = false
    repeatMode = Player.REPEAT_MODE_OFF
  }

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private val _activeClip = MutableStateFlow<VideoClip?>(null)
  val activeClip: StateFlow<VideoClip?> = _activeClip.asStateFlow()

  private val _playerError = MutableStateFlow<String?>(null)
  val playerError: StateFlow<String?> = _playerError.asStateFlow()

  private val _trimPlaybackPositionMs = MutableStateFlow(0L)
  val trimPlaybackPositionMs: StateFlow<Long> = _trimPlaybackPositionMs.asStateFlow()

  private var currentTimeline: Timeline = Timeline()
  private var currentPosMs: Long = 0L
  private var loadedClipId: String? = null
  private var loadedUri: String? = null
  private var isSyncingFromPlayer = false

  // Scrubbing & Request Coalescing
  private var isScrubbingMode = false
  private val pendingSeekPosUs = AtomicLong(-1L)
  private var coalescedSeekJob: Job? = null

  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private var progressSyncJob: Job? = null

  private var lastAppliedFilterMatrix: FloatArray? = null

  init {
    player.addListener(object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        if (isPlaying) {
          startProgressSync()
        } else {
          progressSyncJob?.cancel()
        }
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
          handleClipEnded()
        }
      }

      override fun onPlayerError(error: PlaybackException) {
        Log.w(TAG, "ExoPlayer playback warning (recovering safely): ${error.message}")
        _playerError.value = null
        try {
          player.stop()
          player.clearMediaItems()
        } catch (ignored: Exception) {}
        loadedClipId = null
        progressSyncJob?.cancel()
        if (_isPlaying.value) {
          startSyntheticPlaybackLoop()
        }
      }
    })
  }

  fun isPlayableInPlayer(uriString: String?): Boolean {
    return MediaRelinkManager.isRealPlayableMedia(context, uriString)
  }

  /**
   * Connects the color filter matrix directly to Media3 ExoPlayer's video effects pipeline.
   */
  fun applyVideoFilter(colorMatrix: ColorMatrix?) {
    try {
      if (colorMatrix == null || ColorFilterGenerator.isIdentityMatrix(colorMatrix)) {
        if (lastAppliedFilterMatrix != null) {
          lastAppliedFilterMatrix = null
          player.setVideoEffects(emptyList())
          if (!player.isPlaying && player.playbackState != Player.STATE_IDLE) {
            player.seekTo(player.currentPosition)
          }
        }
        return
      }

      val glMatrix = ColorFilterGenerator.colorMatrixToGlMatrix(colorMatrix)
      if (lastAppliedFilterMatrix != null && lastAppliedFilterMatrix!!.contentEquals(glMatrix)) {
        return
      }
      lastAppliedFilterMatrix = glMatrix

      val rgbMatrix = object : androidx.media3.effect.RgbMatrix {
        override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = glMatrix
      }

      player.setVideoEffects(listOf(rgbMatrix))
      if (!player.isPlaying && player.playbackState != Player.STATE_IDLE) {
        player.seekTo(player.currentPosition)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to apply video effects to ExoPlayer: ${e.message}", e)
    }
  }

  fun updateTimeline(timeline: Timeline) {
    this.currentTimeline = timeline
    val clip = findClipAt(currentPosMs)
    val matrix = ColorFilterGenerator.createCombinedMatrix(
      timeline.adjustments,
      timeline.filter,
      clip?.filter
    )
    applyVideoFilter(matrix)
    syncWithPosition(currentPosMs, forceReload = false)
  }

  /**
   * Seeks to a specific timeline position. Supports request coalescing during rapid scrubbing.
   */
  fun seekTo(timelinePosMs: Long) {
    seekTo(timelinePosMs, isScrubbing = false)
  }

  /**
   * Overloaded seekTo with scrubbing mode support.
   */
  fun seekTo(timelinePosMs: Long, isScrubbing: Boolean) {
    this.isScrubbingMode = isScrubbing
    val boundedPos = timelinePosMs.coerceIn(0L, currentTimeline.totalDurationMs)
    currentPosMs = boundedPos

    val active = findClipAt(boundedPos)
    _activeClip.value = active

    if (active != null && active.isVideo) {
      val sourcePosMs = active.timelineToSourceMs(boundedPos)

      if (isScrubbing) {
        // Coalesce rapid seek calls to maintain 60 FPS target
        pendingSeekPosUs.set(sourcePosMs)
        scheduleCoalescedSeek(active, sourcePosMs)
      } else {
        // Direct immediate seek for non-scrubbing events (jump / touch release)
        coalescedSeekJob?.cancel()
        syncWithPosition(boundedPos, forceReload = false)
      }
    } else {
      player.pause()
    }
  }

  private fun scheduleCoalescedSeek(clip: VideoClip, targetSourcePosMs: Long) {
    if (coalescedSeekJob?.isActive == true) {
      return
    }

    coalescedSeekJob = scope.launch {
      delay(FRAME_INTERVAL_60FPS_MS) // 16ms window to throttle rapid touch drag events
      val latestPos = pendingSeekPosUs.getAndSet(-1L)
      if (latestPos >= 0L) {
        ensureClipLoaded(clip)
        if (player.playbackState != Player.STATE_IDLE) {
          player.seekTo(latestPos)
        }
        // Asynchronously prefetch surrounding proxy frames in background
        proxyEngine?.prefetchFramesAround(clip, latestPos, 1500L)
      }
    }
  }

  fun play() {
    _playerError.value = null
    if (currentTimeline.totalDurationMs <= 0L) {
      _isPlaying.value = false
      return
    }
    if (currentPosMs >= currentTimeline.totalDurationMs) {
      seekTo(0L)
    }
    val clip = findClipAt(currentPosMs)
    if (clip != null && clip.isVideo && isPlayableInPlayer(clip.uri)) {
      ensureClipLoaded(clip)
      val sourcePosMs = clip.timelineToSourceMs(currentPosMs)
      player.seekTo(sourcePosMs)
      player.play()
    } else {
      player.pause()
      startSyntheticPlaybackLoop()
    }
  }

  fun pause() {
    player.pause()
    progressSyncJob?.cancel()
    _isPlaying.value = false
  }

  fun togglePlayPause() {
    if (isTrimPreviewMode) {
      toggleTrimPlayPause()
      return
    }
    if (player.isPlaying || _isPlaying.value) {
      pause()
    } else {
      play()
    }
  }

  private var isTrimPreviewMode = false
  private var trimPreviewClip: VideoClip? = null
  private var trimRangeStartMs = 0L
  private var trimRangeEndMs = 0L

  val isTrimPreview: Boolean get() = isTrimPreviewMode

  fun previewTrimRange(clip: VideoClip, startMs: Long, endMs: Long, loop: Boolean = true) {
    isTrimPreviewMode = true
    trimPreviewClip = clip
    trimRangeStartMs = startMs.coerceAtLeast(0L)
    trimRangeEndMs = endMs.coerceAtLeast(trimRangeStartMs + 50L)
    _trimPlaybackPositionMs.value = trimRangeStartMs
    progressSyncJob?.cancel()

    if (!isPlayableInPlayer(clip.uri)) {
      return
    }

    try {
      val parsedUri = Uri.parse(clip.uri)
      val normalizedUri = if (parsedUri.scheme == "asset") {
        var path = parsedUri.path ?: ""
        if (path.startsWith("/")) path = path.substring(1)
        if (path.isEmpty()) path = parsedUri.authority ?: ""
        Uri.parse("asset:///$path")
      } else {
        parsedUri
      }

      val clippingConfig = MediaItem.ClippingConfiguration.Builder()
        .setStartPositionMs(trimRangeStartMs)
        .setEndPositionMs(trimRangeEndMs)
        .setStartsAtKeyFrame(false)
        .build()

      val mediaItem = MediaItem.Builder()
        .setUri(normalizedUri)
        .setClippingConfiguration(clippingConfig)
        .build()

      player.stop()
      player.clearMediaItems()
      player.setMediaItem(mediaItem)
      player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
      player.playbackParameters = PlaybackParameters(clip.speed)
      player.volume = if (clip.isMuted) 0f else clip.volume
      player.prepare()
      player.play()
      loadedClipId = "trim_${clip.id}"
    } catch (e: Exception) {
      Log.w(TAG, "Failed to preview trim with Media3 ClippingConfiguration", e)
    }
  }

  fun seekTrimPreview(offsetFromStartMs: Long) {
    if (isTrimPreviewMode) {
      val maxOffset = (trimRangeEndMs - trimRangeStartMs).coerceAtLeast(0L)
      val offset = offsetFromStartMs.coerceIn(0L, maxOffset)
      player.seekTo(offset)
      _trimPlaybackPositionMs.value = trimRangeStartMs + offset
    }
  }

  fun seekTrimPreviewToSourceMs(sourceTimeMs: Long) {
    if (isTrimPreviewMode) {
      val targetSourceMs = sourceTimeMs.coerceIn(trimRangeStartMs, trimRangeEndMs)
      val offset = (targetSourceMs - trimRangeStartMs).coerceAtLeast(0L)
      player.seekTo(offset)
      _trimPlaybackPositionMs.value = targetSourceMs
    }
  }

  fun stepTrimFrame(forward: Boolean, fps: Int = 30) {
    if (isTrimPreviewMode) {
      pauseTrimPreview()
      val frameMs = 1000L / fps
      val currentSourceMs = _trimPlaybackPositionMs.value
      val nextSourceMs = if (forward) currentSourceMs + frameMs else currentSourceMs - frameMs
      seekTrimPreviewToSourceMs(nextSourceMs)
    }
  }

  fun pauseTrimPreview() {
    if (isTrimPreviewMode) {
      player.pause()
    }
  }

  fun playTrimPreview() {
    if (isTrimPreviewMode) {
      player.play()
    }
  }

  fun toggleTrimPlayPause() {
    if (isTrimPreviewMode) {
      if (player.isPlaying) player.pause() else player.play()
    }
  }

  fun exitTrimPreview() {
    if (isTrimPreviewMode) {
      isTrimPreviewMode = false
      trimPreviewClip = null
      player.repeatMode = Player.REPEAT_MODE_OFF
      loadedClipId = null
      syncWithPosition(currentPosMs, forceReload = true)
    }
  }

  fun stepFrame(forward: Boolean, fps: Int = 30) {
    pause()
    val frameDuration = 1000L / fps
    val next = if (forward) currentPosMs + frameDuration else currentPosMs - frameDuration
    seekTo(next)
    onTimelinePositionChanged(currentPosMs)
  }

  private fun findClipAt(posMs: Long): VideoClip? {
    return currentTimeline.videoClips.find {
      posMs >= it.timelineStartMs && posMs < it.timelineStartMs + it.durationMs
    } ?: currentTimeline.videoClips.lastOrNull()
  }

  private fun syncWithPosition(posMs: Long, forceReload: Boolean = false) {
    val clip = findClipAt(posMs)
    _activeClip.value = clip

    val matrix = ColorFilterGenerator.createCombinedMatrix(
      currentTimeline.adjustments,
      currentTimeline.filter,
      clip?.filter
    )
    applyVideoFilter(matrix)

    if (clip != null && clip.isVideo && isPlayableInPlayer(clip.uri)) {
      val effectiveUri = proxyEngine?.getProxyUri(clip) ?: clip.uri
      val needsReload = forceReload || loadedUri != effectiveUri || player.mediaItemCount == 0
      if (needsReload) {
        ensureClipLoaded(clip)
      } else {
        loadedClipId = clip.id
      }
      val sourcePosMs = clip.timelineToSourceMs(posMs)
      if (!isSyncingFromPlayer) {
        player.seekTo(sourcePosMs)
      }
      player.playbackParameters = PlaybackParameters(clip.speed)
      player.volume = if (clip.isMuted) 0f else clip.volume

      if (player.playbackState == Player.STATE_IDLE) {
        player.prepare()
      }
    } else {
      player.pause()
    }
  }

  private fun ensureClipLoaded(clip: VideoClip) {
    val effectiveUri = proxyEngine?.getProxyUri(clip) ?: clip.uri
    if (!isPlayableInPlayer(effectiveUri)) {
      try {
        player.stop()
        player.clearMediaItems()
      } catch (ignored: Exception) {}
      loadedClipId = null
      loadedUri = null
      return
    }

    try {
      val parsedUri = Uri.parse(effectiveUri)
      val normalizedUri = if (parsedUri.scheme == "asset") {
        var path = parsedUri.path ?: ""
        if (path.startsWith("/")) path = path.substring(1)
        if (path.isEmpty()) path = parsedUri.authority ?: ""
        Uri.parse("asset:///$path")
      } else if (parsedUri.scheme == null || parsedUri.scheme == "file") {
        val path = parsedUri.path ?: effectiveUri
        val f = java.io.File(path)
        if (f.exists()) Uri.fromFile(f) else parsedUri
      } else {
        parsedUri
      }
      val mediaItem = MediaItem.fromUri(normalizedUri)
      player.setMediaItem(mediaItem)
      player.playbackParameters = PlaybackParameters(clip.speed)
      player.volume = if (clip.isMuted) 0f else clip.volume
      player.prepare()
      loadedClipId = clip.id
      loadedUri = effectiveUri
    } catch (e: Exception) {
      Log.w(TAG, "Failed to load clip URI: $effectiveUri", e)
      try {
        player.stop()
        player.clearMediaItems()
      } catch (ignored: Exception) {}
      loadedClipId = null
      loadedUri = null
    }
  }

  private fun handleClipEnded() {
    if (isTrimPreviewMode) {
      if (player.repeatMode == Player.REPEAT_MODE_OFF) {
        pauseTrimPreview()
      }
      return
    }
    val active = _activeClip.value ?: return
    val nextPos = active.timelineStartMs + active.durationMs
    if (nextPos >= currentTimeline.totalDurationMs) {
      pause()
      seekTo(0L)
      onTimelinePositionChanged(0L)
      onPlaybackEnded()
    } else {
      seekTo(nextPos)
      onTimelinePositionChanged(nextPos)
      play()
    }
  }

  private fun startProgressSync() {
    progressSyncJob?.cancel()
    progressSyncJob = scope.launch {
      while (isActive && player.isPlaying) {
        if (isTrimPreviewMode) {
          val pos = player.currentPosition
          _trimPlaybackPositionMs.value = (trimRangeStartMs + pos).coerceAtMost(trimRangeEndMs)
        } else {
          val active = _activeClip.value
          if (active != null && active.isVideo) {
            val playerPos = player.currentPosition
            val offsetInClip = ((playerPos - active.sourceStartMs) / active.speed).toLong()
            val calculatedTimeline = (active.timelineStartMs + offsetInClip).coerceAtLeast(active.timelineStartMs)
            
            if (calculatedTimeline >= active.timelineStartMs + active.durationMs) {
              handleClipEnded()
              break
            } else {
              isSyncingFromPlayer = true
              currentPosMs = calculatedTimeline
              onTimelinePositionChanged(currentPosMs)
              isSyncingFromPlayer = false
            }
          }
        }
        delay(FRAME_INTERVAL_60FPS_MS) // Smooth 60 FPS target sync
      }
    }
  }

  private fun startSyntheticPlaybackLoop() {
    progressSyncJob?.cancel()
    _isPlaying.value = true
    progressSyncJob = scope.launch {
      while (isActive && _isPlaying.value) {
        val next = currentPosMs + FRAME_INTERVAL_60FPS_MS
        if (next >= currentTimeline.totalDurationMs) {
          pause()
          seekTo(0L)
          onTimelinePositionChanged(0L)
          onPlaybackEnded()
          break
        } else {
          currentPosMs = next
          onTimelinePositionChanged(currentPosMs)
          val nextClip = findClipAt(currentPosMs)
          if (nextClip != null && nextClip.id != _activeClip.value?.id) {
            syncWithPosition(currentPosMs)
            if (nextClip.isVideo && isPlayableInPlayer(nextClip.uri)) {
              player.play()
              break
            }
          }
        }
        delay(FRAME_INTERVAL_60FPS_MS)
      }
    }
  }

  fun release() {
    coalescedSeekJob?.cancel()
    progressSyncJob?.cancel()
    scope.cancel()
    player.release()
  }
}
