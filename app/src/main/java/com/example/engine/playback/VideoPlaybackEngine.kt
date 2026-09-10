package com.example.engine.playback

import android.content.Context
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
import com.example.engine.media.MediaRelinkManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class VideoPlaybackEngine(
  private val context: Context,
  private val onTimelinePositionChanged: (Long) -> Unit,
  private val onPlaybackEnded: () -> Unit
) {
  private val tag = "VideoPlaybackEngine"

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

  private var currentTimeline: Timeline = Timeline()
  private var currentPosMs: Long = 0L
  private var loadedClipId: String? = null
  private var isSyncingFromPlayer = false

  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private var progressSyncJob: Job? = null

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
          // If we reached the end of the current clip
          handleClipEnded()
        }
      }

      override fun onPlayerError(error: PlaybackException) {
        Log.w(tag, "ExoPlayer playback warning (recovering safely): ${error.message}")
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

  fun updateTimeline(timeline: Timeline) {
    this.currentTimeline = timeline
    // Re-verify current active clip
    syncWithPosition(currentPosMs, forceReload = false)
  }

  fun seekTo(timelinePosMs: Long) {
    currentPosMs = timelinePosMs.coerceIn(0L, currentTimeline.totalDurationMs)
    syncWithPosition(currentPosMs, forceReload = false)
  }

  fun play() {
    _playerError.value = null
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
      // If photo/image clip, synthetic clip, or empty, drive playback using timer
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
    if (player.isPlaying || _isPlaying.value) {
      pause()
    } else {
      play()
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

    if (clip != null && clip.isVideo && isPlayableInPlayer(clip.uri)) {
      val needsReload = forceReload || loadedClipId != clip.id
      if (needsReload) {
        ensureClipLoaded(clip)
      }
      val sourcePosMs = clip.timelineToSourceMs(posMs)
      if (!isSyncingFromPlayer) {
        player.seekTo(sourcePosMs)
      }
      // Apply clip speed and volume
      player.playbackParameters = PlaybackParameters(clip.speed)
      player.volume = if (clip.isMuted) 0f else clip.volume
    } else {
      player.pause()
    }
  }

  private fun ensureClipLoaded(clip: VideoClip) {
    if (!isPlayableInPlayer(clip.uri)) {
      try {
        player.stop()
        player.clearMediaItems()
      } catch (ignored: Exception) {}
      loadedClipId = null
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
      val mediaItem = MediaItem.fromUri(normalizedUri)
      player.setMediaItem(mediaItem)
      player.playbackParameters = PlaybackParameters(clip.speed)
      player.volume = if (clip.isMuted) 0f else clip.volume
      player.prepare()
      loadedClipId = clip.id
    } catch (e: Exception) {
      Log.w(tag, "Failed to load clip URI: ${clip.uri}", e)
      try {
        player.stop()
        player.clearMediaItems()
      } catch (ignored: Exception) {}
      loadedClipId = null
    }
  }

  private fun handleClipEnded() {
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
        delay(25L) // Smooth 40Hz sync
      }
    }
  }

  private fun startSyntheticPlaybackLoop() {
    progressSyncJob?.cancel()
    _isPlaying.value = true
    progressSyncJob = scope.launch {
      val frameIntervalMs = 33L
      while (isActive && _isPlaying.value) {
        val next = currentPosMs + frameIntervalMs
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
        delay(frameIntervalMs)
      }
    }
  }

  fun release() {
    progressSyncJob?.cancel()
    scope.cancel()
    player.release()
  }
}
