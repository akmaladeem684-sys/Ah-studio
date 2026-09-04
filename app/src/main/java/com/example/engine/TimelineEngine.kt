package com.example.engine

import com.example.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class SelectedTrackElement {
  object None : SelectedTrackElement()
  data class Video(val clipId: String) : SelectedTrackElement()
  data class Overlay(val clipId: String) : SelectedTrackElement()
  data class Audio(val clipId: String) : SelectedTrackElement()
  data class Text(val clipId: String) : SelectedTrackElement()
  data class Sticker(val clipId: String) : SelectedTrackElement()
  data class Effect(val clipId: String) : SelectedTrackElement()
}

class TimelineEngine {

  private val _timeline = MutableStateFlow(Timeline())
  val timeline: StateFlow<Timeline> = _timeline.asStateFlow()

  private val _currentPositionMs = MutableStateFlow(0L)
  val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private val _selectedElement = MutableStateFlow<SelectedTrackElement>(SelectedTrackElement.None)
  val selectedElement: StateFlow<SelectedTrackElement> = _selectedElement.asStateFlow()

  private val _timelineZoom = MutableStateFlow(1.0f) // 0.5f to 3.0f
  val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

  private val _isSnappingEnabled = MutableStateFlow(true)
  val isSnappingEnabled: StateFlow<Boolean> = _isSnappingEnabled.asStateFlow()

  // Undo / Redo history
  private val undoStack = ArrayDeque<Timeline>()
  private val redoStack = ArrayDeque<Timeline>()

  private val _canUndo = MutableStateFlow(false)
  val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

  private val _canRedo = MutableStateFlow(false)
  val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

  fun loadTimeline(newTimeline: Timeline) {
    recordHistory()
    _timeline.value = newTimeline
    _currentPositionMs.value = 0L
    _selectedElement.value = SelectedTrackElement.None
  }

  fun setPosition(positionMs: Long) {
    val total = _timeline.value.totalDurationMs
    val snapped = if (_isSnappingEnabled.value) snapPosition(positionMs) else positionMs
    _currentPositionMs.value = snapped.coerceIn(0L, total)
  }

  fun togglePlayPause() {
    _isPlaying.value = !_isPlaying.value
  }

  fun pause() {
    _isPlaying.value = false
  }

  fun stop() {
    _isPlaying.value = false
    _currentPositionMs.value = 0L
  }

  fun stepForwardOneFrame(fps: Int = 30) {
    pause()
    val frameDuration = 1000L / fps
    setPosition(_currentPositionMs.value + frameDuration)
  }

  fun stepBackwardOneFrame(fps: Int = 30) {
    pause()
    val frameDuration = 1000L / fps
    setPosition(_currentPositionMs.value - frameDuration)
  }

  fun setZoom(zoom: Float) {
    _timelineZoom.value = zoom.coerceIn(0.5f, 3.5f)
  }

  fun toggleSnapping() {
    _isSnappingEnabled.value = !_isSnappingEnabled.value
  }

  fun selectElement(element: SelectedTrackElement) {
    _selectedElement.value = element
  }

  private fun snapPosition(pos: Long, thresholdMs: Long = 150L): Long {
    val snapPoints = mutableListOf(0L, _timeline.value.totalDurationMs)
    _timeline.value.videoClips.forEach {
      snapPoints.add(it.timelineStartMs)
      snapPoints.add(it.timelineStartMs + it.durationMs)
    }
    _timeline.value.overlayClips.forEach {
      snapPoints.add(it.timelineStartMs)
      snapPoints.add(it.timelineStartMs + it.durationMs)
    }
    _timeline.value.textClips.forEach {
      snapPoints.add(it.timelineStartMs)
      snapPoints.add(it.timelineStartMs + it.durationMs)
    }
    val closest = snapPoints.minByOrNull { kotlin.math.abs(it - pos) } ?: pos
    return if (kotlin.math.abs(closest - pos) <= thresholdMs) closest else pos
  }

  // --- History (Undo / Redo) ---

  private fun recordHistory() {
    undoStack.addLast(_timeline.value)
    if (undoStack.size > 30) undoStack.removeFirst()
    redoStack.clear()
    updateHistoryFlags()
  }

  fun undo() {
    if (undoStack.isNotEmpty()) {
      redoStack.addLast(_timeline.value)
      _timeline.value = undoStack.removeLast()
      updateHistoryFlags()
    }
  }

  fun redo() {
    if (redoStack.isNotEmpty()) {
      undoStack.addLast(_timeline.value)
      _timeline.value = redoStack.removeLast()
      updateHistoryFlags()
    }
  }

  private fun updateHistoryFlags() {
    _canUndo.value = undoStack.isNotEmpty()
    _canRedo.value = redoStack.isNotEmpty()
  }

  // --- Video Clip Operations ---

  fun addVideoClip(
    uri: String,
    name: String,
    isVideo: Boolean = true,
    durationMs: Long = 3000L,
    atPlayhead: Boolean = false
  ) {
    recordHistory()
    val currentClips = _timeline.value.videoClips.toMutableList()
    val startMs = if (atPlayhead) {
      _currentPositionMs.value
    } else {
      currentClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
    }
    val newClip = VideoClip(
      uri = uri,
      name = name,
      isVideo = isVideo,
      timelineStartMs = startMs,
      durationMs = durationMs,
      sourceStartMs = 0L,
      sourceEndMs = durationMs
    )
    currentClips.add(newClip)
    currentClips.sortBy { it.timelineStartMs }
    _timeline.value = _timeline.value.copy(videoClips = currentClips)
    _selectedElement.value = SelectedTrackElement.Video(newClip.id)
  }

  // --- Overlay (PIP) Operations ---

  fun addOverlayClip(
    uri: String,
    name: String,
    isVideo: Boolean = true,
    durationMs: Long = 3000L,
    scale: Float = 0.45f,
    posX: Float = 0.25f,
    posY: Float = -0.25f,
    opacity: Float = 1.0f,
    blendMode: String = "Normal"
  ) {
    recordHistory()
    val currentOverlays = _timeline.value.overlayClips.toMutableList()
    val newOverlay = VideoClip(
      uri = uri,
      name = name,
      isVideo = isVideo,
      timelineStartMs = _currentPositionMs.value,
      durationMs = durationMs,
      sourceStartMs = 0L,
      sourceEndMs = durationMs,
      cropScale = scale,
      cropOffsetX = posX,
      cropOffsetY = posY,
      opacity = opacity,
      blendMode = blendMode
    )
    currentOverlays.add(newOverlay)
    currentOverlays.sortBy { it.timelineStartMs }
    _timeline.value = _timeline.value.copy(overlayClips = currentOverlays)
    _selectedElement.value = SelectedTrackElement.Overlay(newOverlay.id)
  }

  fun updateOverlayClip(updated: VideoClip) {
    val list = _timeline.value.overlayClips.map { if (it.id == updated.id) updated else it }
    _timeline.value = _timeline.value.copy(overlayClips = list)
  }

  fun updateSelectedOverlay(update: (VideoClip) -> VideoClip) {
    val selected = _selectedElement.value
    if (selected is SelectedTrackElement.Overlay) {
      val list = _timeline.value.overlayClips.map { clip ->
        if (clip.id == selected.clipId) update(clip) else clip
      }
      _timeline.value = _timeline.value.copy(overlayClips = list)
    }
  }

  fun setOverlayPosition(clipId: String, posX: Float, posY: Float) {
    val list = _timeline.value.overlayClips.map { clip ->
      if (clip.id == clipId) clip.copy(cropOffsetX = posX, cropOffsetY = posY) else clip
    }
    _timeline.value = _timeline.value.copy(overlayClips = list)
  }

  fun setOverlayScale(clipId: String, scale: Float) {
    val list = _timeline.value.overlayClips.map { clip ->
      if (clip.id == clipId) clip.copy(cropScale = scale.coerceIn(0.1f, 3f)) else clip
    }
    _timeline.value = _timeline.value.copy(overlayClips = list)
  }

  fun setOverlayOpacity(clipId: String, opacity: Float) {
    val list = _timeline.value.overlayClips.map { clip ->
      if (clip.id == clipId) clip.copy(opacity = opacity.coerceIn(0f, 1f)) else clip
    }
    _timeline.value = _timeline.value.copy(overlayClips = list)
  }

  fun setOverlayBlendMode(clipId: String, blendMode: String) {
    val list = _timeline.value.overlayClips.map { clip ->
      if (clip.id == clipId) clip.copy(blendMode = blendMode) else clip
    }
    _timeline.value = _timeline.value.copy(overlayClips = list)
  }

  fun splitSelectedClipAtPlayhead(): Boolean {
    val selected = _selectedElement.value
    if (selected is SelectedTrackElement.Video) {
      val clipIndex = _timeline.value.videoClips.indexOfFirst { it.id == selected.clipId }
      if (clipIndex != -1) {
        val clip = _timeline.value.videoClips[clipIndex]
        val playhead = _currentPositionMs.value
        if (playhead > clip.timelineStartMs + 100L && playhead < clip.timelineStartMs + clip.durationMs - 100L) {
          recordHistory()
          val firstDuration = playhead - clip.timelineStartMs
          val secondDuration = clip.durationMs - firstDuration

          val clip1 = clip.copy(
            durationMs = firstDuration,
            sourceEndMs = clip.sourceStartMs + (firstDuration * clip.speed).toLong()
          )
          val clip2 = clip.copy(
            id = UUID.randomUUID().toString(),
            timelineStartMs = playhead,
            durationMs = secondDuration,
            sourceStartMs = clip1.sourceEndMs,
            sourceEndMs = clip.sourceEndMs
          )

          val newList = _timeline.value.videoClips.toMutableList()
          newList[clipIndex] = clip1
          newList.add(clipIndex + 1, clip2)
          _timeline.value = _timeline.value.copy(videoClips = newList)
          _selectedElement.value = SelectedTrackElement.Video(clip2.id)
          return true
        }
      }
    } else if (selected is SelectedTrackElement.Overlay) {
      val clipIndex = _timeline.value.overlayClips.indexOfFirst { it.id == selected.clipId }
      if (clipIndex != -1) {
        val clip = _timeline.value.overlayClips[clipIndex]
        val playhead = _currentPositionMs.value
        if (playhead > clip.timelineStartMs + 100L && playhead < clip.timelineStartMs + clip.durationMs - 100L) {
          recordHistory()
          val firstDuration = playhead - clip.timelineStartMs
          val secondDuration = clip.durationMs - firstDuration

          val clip1 = clip.copy(
            durationMs = firstDuration,
            sourceEndMs = clip.sourceStartMs + (firstDuration * clip.speed).toLong()
          )
          val clip2 = clip.copy(
            id = UUID.randomUUID().toString(),
            timelineStartMs = playhead,
            durationMs = secondDuration,
            sourceStartMs = clip1.sourceEndMs,
            sourceEndMs = clip.sourceEndMs
          )

          val newList = _timeline.value.overlayClips.toMutableList()
          newList[clipIndex] = clip1
          newList.add(clipIndex + 1, clip2)
          _timeline.value = _timeline.value.copy(overlayClips = newList)
          _selectedElement.value = SelectedTrackElement.Overlay(clip2.id)
          return true
        }
      }
    }
    return false
  }

  fun trimClip(clipId: String, newStartMs: Long, newDurationMs: Long) {
    recordHistory()
    val newList = _timeline.value.videoClips.map { clip ->
      if (clip.id == clipId) {
        clip.copy(timelineStartMs = newStartMs, durationMs = newDurationMs.coerceAtLeast(300L))
      } else clip
    }
    val newOverlays = _timeline.value.overlayClips.map { clip ->
      if (clip.id == clipId) {
        clip.copy(timelineStartMs = newStartMs, durationMs = newDurationMs.coerceAtLeast(300L))
      } else clip
    }
    _timeline.value = _timeline.value.copy(videoClips = newList, overlayClips = newOverlays)
  }

  fun deleteSelected(): Boolean {
    val selected = _selectedElement.value
    recordHistory()
    when (selected) {
      is SelectedTrackElement.Video -> {
        val filtered = _timeline.value.videoClips.filterNot { it.id == selected.clipId }
        _timeline.value = _timeline.value.copy(videoClips = filtered)
        _selectedElement.value = SelectedTrackElement.None
        return true
      }
      is SelectedTrackElement.Overlay -> {
        val filtered = _timeline.value.overlayClips.filterNot { it.id == selected.clipId }
        _timeline.value = _timeline.value.copy(overlayClips = filtered)
        _selectedElement.value = SelectedTrackElement.None
        return true
      }
      is SelectedTrackElement.Audio -> {
        val filtered = _timeline.value.audioClips.filterNot { it.id == selected.clipId }
        _timeline.value = _timeline.value.copy(audioClips = filtered)
        _selectedElement.value = SelectedTrackElement.None
        return true
      }
      is SelectedTrackElement.Text -> {
        val filtered = _timeline.value.textClips.filterNot { it.id == selected.clipId }
        _timeline.value = _timeline.value.copy(textClips = filtered)
        _selectedElement.value = SelectedTrackElement.None
        return true
      }
      is SelectedTrackElement.Sticker -> {
        val filtered = _timeline.value.stickerClips.filterNot { it.id == selected.clipId }
        _timeline.value = _timeline.value.copy(stickerClips = filtered)
        _selectedElement.value = SelectedTrackElement.None
        return true
      }
      is SelectedTrackElement.Effect -> {
        val filtered = _timeline.value.effectClips.filterNot { it.id == selected.clipId }
        _timeline.value = _timeline.value.copy(effectClips = filtered)
        _selectedElement.value = SelectedTrackElement.None
        return true
      }
      SelectedTrackElement.None -> return false
    }
  }

  fun duplicateSelected(): Boolean {
    val selected = _selectedElement.value
    recordHistory()
    when (selected) {
      is SelectedTrackElement.Video -> {
        val clip = _timeline.value.videoClips.find { it.id == selected.clipId } ?: return false
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        val list = _timeline.value.videoClips.toMutableList()
        list.add(copy)
        _timeline.value = _timeline.value.copy(videoClips = list)
        _selectedElement.value = SelectedTrackElement.Video(copy.id)
        return true
      }
      is SelectedTrackElement.Overlay -> {
        val clip = _timeline.value.overlayClips.find { it.id == selected.clipId } ?: return false
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        val list = _timeline.value.overlayClips.toMutableList()
        list.add(copy)
        _timeline.value = _timeline.value.copy(overlayClips = list)
        _selectedElement.value = SelectedTrackElement.Overlay(copy.id)
        return true
      }
      is SelectedTrackElement.Text -> {
        val clip = _timeline.value.textClips.find { it.id == selected.clipId } ?: return false
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        val list = _timeline.value.textClips.toMutableList()
        list.add(copy)
        _timeline.value = _timeline.value.copy(textClips = list)
        _selectedElement.value = SelectedTrackElement.Text(copy.id)
        return true
      }
      else -> return false
    }
  }

  fun rotateSelectedClip() {
    val selected = _selectedElement.value
    if (selected is SelectedTrackElement.Video) {
      recordHistory()
      val list = _timeline.value.videoClips.map { clip ->
        if (clip.id == selected.clipId) {
          val next = (clip.rotationDegrees + 90) % 360
          clip.copy(rotationDegrees = next)
        } else clip
      }
      _timeline.value = _timeline.value.copy(videoClips = list)
    } else if (selected is SelectedTrackElement.Overlay) {
      recordHistory()
      val list = _timeline.value.overlayClips.map { clip ->
        if (clip.id == selected.clipId) {
          val next = (clip.rotationDegrees + 90) % 360
          clip.copy(rotationDegrees = next)
        } else clip
      }
      _timeline.value = _timeline.value.copy(overlayClips = list)
    }
  }

  fun flipSelectedClip(horizontal: Boolean) {
    val selected = _selectedElement.value
    if (selected is SelectedTrackElement.Video) {
      recordHistory()
      val list = _timeline.value.videoClips.map { clip ->
        if (clip.id == selected.clipId) {
          if (horizontal) clip.copy(flipHorizontal = !clip.flipHorizontal)
          else clip.copy(flipVertical = !clip.flipVertical)
        } else clip
      }
      _timeline.value = _timeline.value.copy(videoClips = list)
    } else if (selected is SelectedTrackElement.Overlay) {
      recordHistory()
      val list = _timeline.value.overlayClips.map { clip ->
        if (clip.id == selected.clipId) {
          if (horizontal) clip.copy(flipHorizontal = !clip.flipHorizontal)
          else clip.copy(flipVertical = !clip.flipVertical)
        } else clip
      }
      _timeline.value = _timeline.value.copy(overlayClips = list)
    }
  }

  fun setClipSpeed(clipId: String, speed: Float) {
    recordHistory()
    val list = _timeline.value.videoClips.map { clip ->
      if (clip.id == clipId) {
        val oldSpeed = clip.speed
        val newDuration = ((clip.durationMs * oldSpeed) / speed).toLong().coerceAtLeast(200L)
        clip.copy(speed = speed, durationMs = newDuration)
      } else clip
    }
    _timeline.value = _timeline.value.copy(videoClips = list)
  }

  fun freezeFrameAtPlayhead(): Boolean {
    val selected = _selectedElement.value
    if (selected is SelectedTrackElement.Video) {
      val clip = _timeline.value.videoClips.find { it.id == selected.clipId } ?: return false
      recordHistory()
      val freezeClip = VideoClip(
        id = UUID.randomUUID().toString(),
        uri = clip.uri,
        name = "${clip.name} (Freeze)",
        isVideo = false, // static frame
        timelineStartMs = _currentPositionMs.value,
        durationMs = 2500L,
        sourceStartMs = _currentPositionMs.value - clip.timelineStartMs,
        sourceEndMs = _currentPositionMs.value - clip.timelineStartMs
      )
      val list = _timeline.value.videoClips.toMutableList()
      list.add(freezeClip)
      list.sortBy { it.timelineStartMs }
      _timeline.value = _timeline.value.copy(videoClips = list)
      _selectedElement.value = SelectedTrackElement.Video(freezeClip.id)
      return true
    }
    return false
  }

  // --- Adjustments & Filters ---

  fun updateAdjustments(adjustments: VideoAdjustments) {
    _timeline.value = _timeline.value.copy(adjustments = adjustments)
  }

  fun updateFilter(filter: FilterSettings) {
    _timeline.value = _timeline.value.copy(filter = filter)
  }

  fun updateChromaKey(chroma: ChromaKeySettings) {
    _timeline.value = _timeline.value.copy(chromaKey = chroma)
  }

  // --- Audio Operations ---

  fun addAudioClip(title: String, durationMs: Long = 8000L, uri: String = "internal://$title") {
    recordHistory()
    val newAudio = AudioClip(
      title = title,
      uri = uri,
      timelineStartMs = _currentPositionMs.value,
      durationMs = durationMs,
      waveformData = com.example.engine.audio.SoundEffectsCatalog.generateWaveform(title)
    )
    val list = _timeline.value.audioClips.toMutableList()
    list.add(newAudio)
    _timeline.value = _timeline.value.copy(audioClips = list)
    _selectedElement.value = SelectedTrackElement.Audio(newAudio.id)
  }

  // --- Text Operations ---

  fun addTextClip(text: String = "NEW TEXT") {
    recordHistory()
    val newText = TextClip(
      text = text,
      timelineStartMs = _currentPositionMs.value,
      durationMs = 3000L,
      fontSizeSp = 24f,
      fontWeight = 800,
      textColor = 0xFFFFFFFF,
      hasGradient = true,
      gradientColorStart = 0xFF00E5FF,
      gradientColorEnd = 0xFF8B5CF6,
      animationType = "Pop"
    )
    val list = _timeline.value.textClips.toMutableList()
    list.add(newText)
    _timeline.value = _timeline.value.copy(textClips = list)
    _selectedElement.value = SelectedTrackElement.Text(newText.id)
  }

  fun updateTextClip(updated: TextClip) {
    val list = _timeline.value.textClips.map { if (it.id == updated.id) updated else it }
    _timeline.value = _timeline.value.copy(textClips = list)
  }

  // --- Sticker Operations ---

  fun addStickerClip(emojiOrAsset: String) {
    recordHistory()
    val newSticker = StickerClip(
      emojiOrAsset = emojiOrAsset,
      timelineStartMs = _currentPositionMs.value,
      durationMs = 3000L
    )
    val list = _timeline.value.stickerClips.toMutableList()
    list.add(newSticker)
    _timeline.value = _timeline.value.copy(stickerClips = list)
    _selectedElement.value = SelectedTrackElement.Sticker(newSticker.id)
  }

  // --- Effect Operations ---

  fun addEffectClip(effectType: EffectType) {
    recordHistory()
    val newEffect = EffectClip(
      effectType = effectType,
      timelineStartMs = _currentPositionMs.value,
      durationMs = 3000L,
      intensity = 0.8f
    )
    val list = _timeline.value.effectClips.toMutableList()
    list.add(newEffect)
    _timeline.value = _timeline.value.copy(effectClips = list)
    _selectedElement.value = SelectedTrackElement.Effect(newEffect.id)
  }

  // --- Transitions ---

  fun setTransition(clipIndexBefore: Int, type: TransitionType, durationMs: Long = 500L) {
    recordHistory()
    val current = _timeline.value.transitions.toMutableList()
    current.removeAll { it.clipIndexBefore == clipIndexBefore }
    if (type != TransitionType.NONE) {
      current.add(Transition(clipIndexBefore = clipIndexBefore, type = type, durationMs = durationMs))
    }
    _timeline.value = _timeline.value.copy(transitions = current)
  }

  // --- Keyframe System ---

  fun addKeyframeToSelectedClip() {
    val selected = _selectedElement.value
    if (selected is SelectedTrackElement.Video) {
      recordHistory()
      val list = _timeline.value.videoClips.map { clip ->
        if (clip.id == selected.clipId) {
          val relTime = (_currentPositionMs.value - clip.timelineStartMs).coerceAtLeast(0L)
          val existing = clip.keyframes.filterNot { kotlin.math.abs(it.timeMs - relTime) < 100L }
          val newKf = ClipKeyframe(
            timeMs = relTime,
            scale = clip.cropScale,
            rotation = clip.rotationDegrees.toFloat(),
            posX = clip.cropOffsetX,
            posY = clip.cropOffsetY
          )
          clip.copy(keyframes = (existing + newKf).sortedBy { it.timeMs })
        } else clip
      }
      _timeline.value = _timeline.value.copy(videoClips = list)
    }
  }

  fun deleteKeyframeFromSelectedClip() {
    val selected = _selectedElement.value
    if (selected is SelectedTrackElement.Video) {
      recordHistory()
      val list = _timeline.value.videoClips.map { clip ->
        if (clip.id == selected.clipId) {
          val relTime = (_currentPositionMs.value - clip.timelineStartMs).coerceAtLeast(0L)
          clip.copy(keyframes = clip.keyframes.filterNot { kotlin.math.abs(it.timeMs - relTime) < 250L })
        } else clip
      }
      _timeline.value = _timeline.value.copy(videoClips = list)
    }
  }
}
