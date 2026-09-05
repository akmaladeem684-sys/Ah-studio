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

data class SnapResult(
  val snappedPosMs: Long,
  val didSnap: Boolean = false,
  val snapLineMs: Long? = null
)

class TimelineEngine {

  private val _timeline = MutableStateFlow(Timeline())
  val timeline: StateFlow<Timeline> = _timeline.asStateFlow()

  private val _currentPositionMs = MutableStateFlow(0L)
  val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private val _selectedElement = MutableStateFlow<SelectedTrackElement>(SelectedTrackElement.None)
  val selectedElement: StateFlow<SelectedTrackElement> = _selectedElement.asStateFlow()

  private val _isMultiSelectMode = MutableStateFlow(false)
  val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

  private val _selectedClipIds = MutableStateFlow<Set<String>>(emptySet())
  val selectedClipIds: StateFlow<Set<String>> = _selectedClipIds.asStateFlow()

  private val _snapIndicatorMs = MutableStateFlow<Long?>(null)
  val snapIndicatorMs: StateFlow<Long?> = _snapIndicatorMs.asStateFlow()

  private val _clipboardClips = MutableStateFlow<List<Any>>(emptyList())
  val clipboardClips: StateFlow<List<Any>> = _clipboardClips.asStateFlow()

  private val _timelineZoom = MutableStateFlow(1.0f) // 0.25f to 4.5f
  val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

  private val _isSnappingEnabled = MutableStateFlow(true)
  val isSnappingEnabled: StateFlow<Boolean> = _isSnappingEnabled.asStateFlow()

  private val _isMagneticEnabled = MutableStateFlow(true)
  val isMagneticEnabled: StateFlow<Boolean> = _isMagneticEnabled.asStateFlow()

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
    _selectedClipIds.value = emptySet()
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
    _timelineZoom.value = zoom.coerceIn(0.25f, 4.5f)
  }

  fun toggleSnapping() {
    _isSnappingEnabled.value = !_isSnappingEnabled.value
  }

  fun toggleMagneticMovement() {
    _isMagneticEnabled.value = !_isMagneticEnabled.value
  }

  fun setMagneticMovement(enabled: Boolean) {
    _isMagneticEnabled.value = enabled
  }

  fun reorderVideoClips(fromIndex: Int, toIndex: Int): Boolean {
    if (isTrackLocked(TrackType.MAIN_VIDEO)) return false
    val clips = _timeline.value.videoClips.toMutableList()
    if (fromIndex !in clips.indices || toIndex !in clips.indices || fromIndex == toIndex) return false
    recordHistory()
    val item = clips.removeAt(fromIndex)
    clips.add(toIndex, item)
    var currentStart = 0L
    for (i in clips.indices) {
      clips[i] = clips[i].copy(timelineStartMs = currentStart)
      currentStart += clips[i].durationMs
    }
    _timeline.value = _timeline.value.copy(videoClips = clips)
    return true
  }

  fun selectElement(element: SelectedTrackElement) {
    _selectedElement.value = element
    when (element) {
      is SelectedTrackElement.Video -> _selectedClipIds.value = setOf(element.clipId)
      is SelectedTrackElement.Overlay -> _selectedClipIds.value = setOf(element.clipId)
      is SelectedTrackElement.Audio -> _selectedClipIds.value = setOf(element.clipId)
      is SelectedTrackElement.Text -> _selectedClipIds.value = setOf(element.clipId)
      is SelectedTrackElement.Sticker -> _selectedClipIds.value = setOf(element.clipId)
      is SelectedTrackElement.Effect -> _selectedClipIds.value = setOf(element.clipId)
      SelectedTrackElement.None -> _selectedClipIds.value = emptySet()
    }
  }

  fun toggleMultiSelectMode() {
    val newMode = !_isMultiSelectMode.value
    _isMultiSelectMode.value = newMode
    if (!newMode) {
      val first = _selectedClipIds.value.firstOrNull()
      if (first != null) {
        _selectedClipIds.value = setOf(first)
        _selectedElement.value = findTrackElementForClip(first)
      } else {
        _selectedClipIds.value = emptySet()
        _selectedElement.value = SelectedTrackElement.None
      }
    }
  }

  fun toggleSelectClip(clipId: String, trackElement: SelectedTrackElement? = null) {
    val element = trackElement ?: findTrackElementForClip(clipId)
    if (_isMultiSelectMode.value) {
      val current = _selectedClipIds.value.toMutableSet()
      if (current.contains(clipId)) {
        current.remove(clipId)
      } else {
        current.add(clipId)
      }
      _selectedClipIds.value = current
      if (current.isNotEmpty()) {
        _selectedElement.value = findTrackElementForClip(current.last())
      } else {
        _selectedElement.value = SelectedTrackElement.None
      }
    } else {
      _selectedClipIds.value = setOf(clipId)
      _selectedElement.value = element
    }
  }

  fun toggleClipSelection(clipId: String, trackElement: SelectedTrackElement? = null) {
    toggleSelectClip(clipId, trackElement)
  }

  fun selectAllClips() {
    val allIds = mutableSetOf<String>()
    allIds.addAll(_timeline.value.videoClips.map { it.id })
    allIds.addAll(_timeline.value.overlayClips.map { it.id })
    allIds.addAll(_timeline.value.textClips.map { it.id })
    allIds.addAll(_timeline.value.audioClips.map { it.id })
    allIds.addAll(_timeline.value.stickerClips.map { it.id })
    allIds.addAll(_timeline.value.effectClips.map { it.id })
    _selectedClipIds.value = allIds
    _isMultiSelectMode.value = true
    val first = allIds.firstOrNull()
    if (first != null) {
      _selectedElement.value = findTrackElementForClip(first)
    }
  }

  fun clearSelection() {
    _selectedClipIds.value = emptySet()
    _selectedElement.value = SelectedTrackElement.None
  }

  fun findTrackElementForClip(clipId: String): SelectedTrackElement {
    if (_timeline.value.videoClips.any { it.id == clipId }) return SelectedTrackElement.Video(clipId)
    if (_timeline.value.overlayClips.any { it.id == clipId }) return SelectedTrackElement.Overlay(clipId)
    if (_timeline.value.audioClips.any { it.id == clipId }) return SelectedTrackElement.Audio(clipId)
    if (_timeline.value.textClips.any { it.id == clipId }) return SelectedTrackElement.Text(clipId)
    if (_timeline.value.stickerClips.any { it.id == clipId }) return SelectedTrackElement.Sticker(clipId)
    if (_timeline.value.effectClips.any { it.id == clipId }) return SelectedTrackElement.Effect(clipId)
    return SelectedTrackElement.None
  }

  fun calculateSnap(
    candidatePosMs: Long,
    thresholdMs: Long = 130L,
    ignoreClipIds: Set<String> = emptySet()
  ): SnapResult {
    if (!_isSnappingEnabled.value) return SnapResult(candidatePosMs, false, null)
    val snapPoints = mutableSetOf(0L, _timeline.value.totalDurationMs, _currentPositionMs.value)
    _timeline.value.videoClips.forEach {
      if (it.id !in ignoreClipIds) {
        snapPoints.add(it.timelineStartMs)
        snapPoints.add(it.timelineStartMs + it.durationMs)
      }
    }
    _timeline.value.overlayClips.forEach {
      if (it.id !in ignoreClipIds) {
        snapPoints.add(it.timelineStartMs)
        snapPoints.add(it.timelineStartMs + it.durationMs)
      }
    }
    _timeline.value.textClips.forEach {
      if (it.id !in ignoreClipIds) {
        snapPoints.add(it.timelineStartMs)
        snapPoints.add(it.timelineStartMs + it.durationMs)
      }
    }
    _timeline.value.audioClips.forEach {
      if (it.id !in ignoreClipIds) {
        snapPoints.add(it.timelineStartMs)
        snapPoints.add(it.timelineStartMs + it.durationMs)
      }
    }
    _timeline.value.stickerClips.forEach {
      if (it.id !in ignoreClipIds) {
        snapPoints.add(it.timelineStartMs)
        snapPoints.add(it.timelineStartMs + it.durationMs)
      }
    }
    _timeline.value.effectClips.forEach {
      if (it.id !in ignoreClipIds) {
        snapPoints.add(it.timelineStartMs)
        snapPoints.add(it.timelineStartMs + it.durationMs)
      }
    }
    val closest = snapPoints.minByOrNull { kotlin.math.abs(it - candidatePosMs) } ?: candidatePosMs
    return if (kotlin.math.abs(closest - candidatePosMs) <= thresholdMs) {
      _snapIndicatorMs.value = closest
      SnapResult(closest, true, closest)
    } else {
      _snapIndicatorMs.value = null
      SnapResult(candidatePosMs, false, null)
    }
  }

  fun clearSnapIndicator() {
    _snapIndicatorMs.value = null
  }

  private fun snapPosition(pos: Long, thresholdMs: Long = 150L): Long {
    return calculateSnap(pos, thresholdMs).snappedPosMs
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
    atPlayhead: Boolean = false,
    width: Int = 1920,
    height: Int = 1080,
    rotationDegrees: Int = 0,
    frameRate: Float = 30f,
    mimeType: String = "video/mp4",
    hasAudio: Boolean = true
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
      sourceEndMs = durationMs,
      width = width,
      height = height,
      naturalRotation = rotationDegrees,
      frameRate = frameRate,
      mimeType = mimeType,
      hasAudio = hasAudio
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
    blendMode: String = "Normal",
    width: Int = 1920,
    height: Int = 1080,
    rotationDegrees: Int = 0,
    frameRate: Float = 30f,
    mimeType: String = "video/mp4",
    hasAudio: Boolean = true
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
      blendMode = blendMode,
      width = width,
      height = height,
      naturalRotation = rotationDegrees,
      frameRate = frameRate,
      mimeType = mimeType,
      hasAudio = hasAudio
    )
    currentOverlays.add(newOverlay)
    currentOverlays.sortBy { it.timelineStartMs }
    _timeline.value = _timeline.value.copy(overlayClips = currentOverlays)
    _selectedElement.value = SelectedTrackElement.Overlay(newOverlay.id)
  }

  fun replaceSelectedMedia(
    newUri: String,
    newName: String,
    width: Int,
    height: Int,
    durationMs: Long,
    isVideo: Boolean,
    rotationDegrees: Int = 0,
    frameRate: Float = 30f,
    mimeType: String = "video/mp4",
    hasAudio: Boolean = true
  ) {
    val selected = _selectedElement.value
    recordHistory()
    if (selected is SelectedTrackElement.Video) {
      val list = _timeline.value.videoClips.map { clip ->
        if (clip.id == selected.clipId) {
          clip.copy(
            uri = newUri,
            name = newName,
            isVideo = isVideo,
            durationMs = durationMs,
            sourceStartMs = 0L,
            sourceEndMs = durationMs,
            width = width,
            height = height,
            naturalRotation = rotationDegrees,
            frameRate = frameRate,
            mimeType = mimeType,
            hasAudio = hasAudio
          )
        } else clip
      }
      _timeline.value = _timeline.value.copy(videoClips = list)
    } else if (selected is SelectedTrackElement.Overlay) {
      val list = _timeline.value.overlayClips.map { clip ->
        if (clip.id == selected.clipId) {
          clip.copy(
            uri = newUri,
            name = newName,
            isVideo = isVideo,
            durationMs = durationMs,
            sourceStartMs = 0L,
            sourceEndMs = durationMs,
            width = width,
            height = height,
            naturalRotation = rotationDegrees,
            frameRate = frameRate,
            mimeType = mimeType,
            hasAudio = hasAudio
          )
        } else clip
      }
      _timeline.value = _timeline.value.copy(overlayClips = list)
    }
  }

  fun toggleSelectedClipMute() {
    val selected = _selectedElement.value
    recordHistory()
    if (selected is SelectedTrackElement.Video) {
      val list = _timeline.value.videoClips.map { clip ->
        if (clip.id == selected.clipId) clip.copy(isMuted = !clip.isMuted) else clip
      }
      _timeline.value = _timeline.value.copy(videoClips = list)
    } else if (selected is SelectedTrackElement.Overlay) {
      val list = _timeline.value.overlayClips.map { clip ->
        if (clip.id == selected.clipId) clip.copy(isMuted = !clip.isMuted) else clip
      }
      _timeline.value = _timeline.value.copy(overlayClips = list)
    }
  }

  fun toggleSelectedClipReverse() {
    val selected = _selectedElement.value
    recordHistory()
    if (selected is SelectedTrackElement.Video) {
      val list = _timeline.value.videoClips.map { clip ->
        if (clip.id == selected.clipId) clip.copy(isReversed = !clip.isReversed) else clip
      }
      _timeline.value = _timeline.value.copy(videoClips = list)
    }
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

  // --- Track Controls ---

  fun isTrackLocked(trackType: TrackType): Boolean {
    return _timeline.value.trackSettings[trackType]?.isLocked == true
  }

  fun toggleTrackLock(trackType: TrackType) {
    recordHistory()
    val settings = _timeline.value.trackSettings.toMutableMap()
    val cur = settings[trackType] ?: TrackSettings(trackType)
    settings[trackType] = cur.copy(isLocked = !cur.isLocked)
    _timeline.value = _timeline.value.copy(trackSettings = settings)
  }

  fun toggleTrackHide(trackType: TrackType) {
    recordHistory()
    val settings = _timeline.value.trackSettings.toMutableMap()
    val cur = settings[trackType] ?: TrackSettings(trackType)
    settings[trackType] = cur.copy(isHidden = !cur.isHidden)
    _timeline.value = _timeline.value.copy(trackSettings = settings)
  }

  fun toggleTrackMute(trackType: TrackType) {
    recordHistory()
    val settings = _timeline.value.trackSettings.toMutableMap()
    val cur = settings[trackType] ?: TrackSettings(trackType)
    settings[trackType] = cur.copy(isMuted = !cur.isMuted)
    _timeline.value = _timeline.value.copy(trackSettings = settings)
  }

  fun toggleTrackSolo(trackType: TrackType) {
    recordHistory()
    val settings = _timeline.value.trackSettings.toMutableMap()
    val cur = settings[trackType] ?: TrackSettings(trackType)
    settings[trackType] = cur.copy(isSolo = !cur.isSolo)
    _timeline.value = _timeline.value.copy(trackSettings = settings)
  }

  fun setTrackHeight(trackType: TrackType, height: TrackHeight) {
    recordHistory()
    val settings = _timeline.value.trackSettings.toMutableMap()
    val cur = settings[trackType] ?: TrackSettings(trackType)
    settings[trackType] = cur.copy(height = height)
    _timeline.value = _timeline.value.copy(trackSettings = settings)
  }

  fun cycleTrackHeight(trackType: TrackType) {
    val cur = _timeline.value.trackSettings[trackType]?.height ?: TrackHeight.NORMAL
    val next = when (cur) {
      TrackHeight.COMPACT -> TrackHeight.NORMAL
      TrackHeight.NORMAL -> TrackHeight.EXPANDED
      TrackHeight.EXPANDED -> TrackHeight.COMPACT
    }
    setTrackHeight(trackType, next)
  }

  fun setAllTrackHeights(height: TrackHeight) {
    recordHistory()
    val settings = _timeline.value.trackSettings.mapValues { it.value.copy(height = height) }
    _timeline.value = _timeline.value.copy(trackSettings = settings)
  }

  // --- Advanced Clip Editing & Multi-Track Operations ---

  fun trimClipLeft(clipId: String, newStartMs: Long, snap: Boolean = true) {
    val element = findTrackElementForClip(clipId)
    when (element) {
      is SelectedTrackElement.Video -> {
        if (isTrackLocked(TrackType.MAIN_VIDEO)) return
        val clip = _timeline.value.videoClips.find { it.id == clipId } ?: return
        val currentEnd = clip.timelineStartMs + clip.durationMs
        val targetStart = if (snap && _isSnappingEnabled.value) calculateSnap(newStartMs, ignoreClipIds = setOf(clipId)).snappedPosMs else newStartMs
        val clampedStart = targetStart.coerceIn(0L, currentEnd - 200L)
        val newDur = currentEnd - clampedStart
        val deltaMs = clampedStart - clip.timelineStartMs
        val newSourceStart = (clip.sourceStartMs + (deltaMs * clip.speed).toLong()).coerceIn(0L, clip.sourceEndMs - 200L)
        recordHistory()
        val list = _timeline.value.videoClips.map {
          if (it.id == clipId) it.copy(timelineStartMs = clampedStart, durationMs = newDur, sourceStartMs = newSourceStart)
          else it
        }
        _timeline.value = _timeline.value.copy(videoClips = list)
      }
      is SelectedTrackElement.Overlay -> {
        if (isTrackLocked(TrackType.OVERLAY)) return
        val clip = _timeline.value.overlayClips.find { it.id == clipId } ?: return
        val currentEnd = clip.timelineStartMs + clip.durationMs
        val targetStart = if (snap && _isSnappingEnabled.value) calculateSnap(newStartMs, ignoreClipIds = setOf(clipId)).snappedPosMs else newStartMs
        val clampedStart = targetStart.coerceIn(0L, currentEnd - 200L)
        val newDur = currentEnd - clampedStart
        val deltaMs = clampedStart - clip.timelineStartMs
        val newSourceStart = (clip.sourceStartMs + (deltaMs * clip.speed).toLong()).coerceIn(0L, clip.sourceEndMs - 200L)
        recordHistory()
        val list = _timeline.value.overlayClips.map {
          if (it.id == clipId) it.copy(timelineStartMs = clampedStart, durationMs = newDur, sourceStartMs = newSourceStart)
          else it
        }
        _timeline.value = _timeline.value.copy(overlayClips = list)
      }
      is SelectedTrackElement.Audio -> {
        if (isTrackLocked(TrackType.AUDIO)) return
        val clip = _timeline.value.audioClips.find { it.id == clipId } ?: return
        val currentEnd = clip.timelineStartMs + clip.durationMs
        val targetStart = if (snap && _isSnappingEnabled.value) calculateSnap(newStartMs, ignoreClipIds = setOf(clipId)).snappedPosMs else newStartMs
        val clampedStart = targetStart.coerceIn(0L, currentEnd - 200L)
        val newDur = currentEnd - clampedStart
        val deltaMs = clampedStart - clip.timelineStartMs
        val newSourceStart = (clip.sourceStartMs + (deltaMs * clip.speed).toLong()).coerceIn(0L, clip.sourceEndMs - 200L)
        recordHistory()
        val list = _timeline.value.audioClips.map {
          if (it.id == clipId) it.copy(timelineStartMs = clampedStart, durationMs = newDur, sourceStartMs = newSourceStart)
          else it
        }
        _timeline.value = _timeline.value.copy(audioClips = list)
      }
      is SelectedTrackElement.Text -> {
        if (isTrackLocked(TrackType.TEXT)) return
        val clip = _timeline.value.textClips.find { it.id == clipId } ?: return
        val currentEnd = clip.timelineStartMs + clip.durationMs
        val targetStart = if (snap && _isSnappingEnabled.value) calculateSnap(newStartMs, ignoreClipIds = setOf(clipId)).snappedPosMs else newStartMs
        val clampedStart = targetStart.coerceIn(0L, currentEnd - 200L)
        val newDur = currentEnd - clampedStart
        recordHistory()
        val list = _timeline.value.textClips.map {
          if (it.id == clipId) it.copy(timelineStartMs = clampedStart, durationMs = newDur)
          else it
        }
        _timeline.value = _timeline.value.copy(textClips = list)
      }
      is SelectedTrackElement.Sticker -> {
        if (isTrackLocked(TrackType.STICKER)) return
        val clip = _timeline.value.stickerClips.find { it.id == clipId } ?: return
        val currentEnd = clip.timelineStartMs + clip.durationMs
        val targetStart = if (snap && _isSnappingEnabled.value) calculateSnap(newStartMs, ignoreClipIds = setOf(clipId)).snappedPosMs else newStartMs
        val clampedStart = targetStart.coerceIn(0L, currentEnd - 200L)
        val newDur = currentEnd - clampedStart
        recordHistory()
        val list = _timeline.value.stickerClips.map {
          if (it.id == clipId) it.copy(timelineStartMs = clampedStart, durationMs = newDur)
          else it
        }
        _timeline.value = _timeline.value.copy(stickerClips = list)
      }
      is SelectedTrackElement.Effect -> {
        if (isTrackLocked(TrackType.EFFECT)) return
        val clip = _timeline.value.effectClips.find { it.id == clipId } ?: return
        val currentEnd = clip.timelineStartMs + clip.durationMs
        val targetStart = if (snap && _isSnappingEnabled.value) calculateSnap(newStartMs, ignoreClipIds = setOf(clipId)).snappedPosMs else newStartMs
        val clampedStart = targetStart.coerceIn(0L, currentEnd - 200L)
        val newDur = currentEnd - clampedStart
        recordHistory()
        val list = _timeline.value.effectClips.map {
          if (it.id == clipId) it.copy(timelineStartMs = clampedStart, durationMs = newDur)
          else it
        }
        _timeline.value = _timeline.value.copy(effectClips = list)
      }
      SelectedTrackElement.None -> {}
    }
  }

  fun trimClipRight(clipId: String, newDurationMs: Long, snap: Boolean = true) {
    val element = findTrackElementForClip(clipId)
    when (element) {
      is SelectedTrackElement.Video -> {
        if (isTrackLocked(TrackType.MAIN_VIDEO)) return
        val clip = _timeline.value.videoClips.find { it.id == clipId } ?: return
        val targetEnd = clip.timelineStartMs + newDurationMs
        val snappedEnd = if (snap && _isSnappingEnabled.value) calculateSnap(targetEnd, ignoreClipIds = setOf(clipId)).snappedPosMs else targetEnd
        val dur = (snappedEnd - clip.timelineStartMs).coerceAtLeast(200L)
        val newSourceEnd = (clip.sourceStartMs + (dur * clip.speed).toLong())
        recordHistory()
        val list = _timeline.value.videoClips.map {
          if (it.id == clipId) it.copy(durationMs = dur, sourceEndMs = newSourceEnd)
          else it
        }
        _timeline.value = _timeline.value.copy(videoClips = list)
      }
      is SelectedTrackElement.Overlay -> {
        if (isTrackLocked(TrackType.OVERLAY)) return
        val clip = _timeline.value.overlayClips.find { it.id == clipId } ?: return
        val targetEnd = clip.timelineStartMs + newDurationMs
        val snappedEnd = if (snap && _isSnappingEnabled.value) calculateSnap(targetEnd, ignoreClipIds = setOf(clipId)).snappedPosMs else targetEnd
        val dur = (snappedEnd - clip.timelineStartMs).coerceAtLeast(200L)
        val newSourceEnd = (clip.sourceStartMs + (dur * clip.speed).toLong())
        recordHistory()
        val list = _timeline.value.overlayClips.map {
          if (it.id == clipId) it.copy(durationMs = dur, sourceEndMs = newSourceEnd)
          else it
        }
        _timeline.value = _timeline.value.copy(overlayClips = list)
      }
      is SelectedTrackElement.Audio -> {
        if (isTrackLocked(TrackType.AUDIO)) return
        val clip = _timeline.value.audioClips.find { it.id == clipId } ?: return
        val targetEnd = clip.timelineStartMs + newDurationMs
        val snappedEnd = if (snap && _isSnappingEnabled.value) calculateSnap(targetEnd, ignoreClipIds = setOf(clipId)).snappedPosMs else targetEnd
        val dur = (snappedEnd - clip.timelineStartMs).coerceAtLeast(200L)
        val newSourceEnd = (clip.sourceStartMs + (dur * clip.speed).toLong())
        recordHistory()
        val list = _timeline.value.audioClips.map {
          if (it.id == clipId) it.copy(durationMs = dur, sourceEndMs = newSourceEnd)
          else it
        }
        _timeline.value = _timeline.value.copy(audioClips = list)
      }
      is SelectedTrackElement.Text -> {
        if (isTrackLocked(TrackType.TEXT)) return
        val clip = _timeline.value.textClips.find { it.id == clipId } ?: return
        val targetEnd = clip.timelineStartMs + newDurationMs
        val snappedEnd = if (snap && _isSnappingEnabled.value) calculateSnap(targetEnd, ignoreClipIds = setOf(clipId)).snappedPosMs else targetEnd
        val dur = (snappedEnd - clip.timelineStartMs).coerceAtLeast(200L)
        recordHistory()
        val list = _timeline.value.textClips.map {
          if (it.id == clipId) it.copy(durationMs = dur)
          else it
        }
        _timeline.value = _timeline.value.copy(textClips = list)
      }
      is SelectedTrackElement.Sticker -> {
        if (isTrackLocked(TrackType.STICKER)) return
        val clip = _timeline.value.stickerClips.find { it.id == clipId } ?: return
        val targetEnd = clip.timelineStartMs + newDurationMs
        val snappedEnd = if (snap && _isSnappingEnabled.value) calculateSnap(targetEnd, ignoreClipIds = setOf(clipId)).snappedPosMs else targetEnd
        val dur = (snappedEnd - clip.timelineStartMs).coerceAtLeast(200L)
        recordHistory()
        val list = _timeline.value.stickerClips.map {
          if (it.id == clipId) it.copy(durationMs = dur)
          else it
        }
        _timeline.value = _timeline.value.copy(stickerClips = list)
      }
      is SelectedTrackElement.Effect -> {
        if (isTrackLocked(TrackType.EFFECT)) return
        val clip = _timeline.value.effectClips.find { it.id == clipId } ?: return
        val targetEnd = clip.timelineStartMs + newDurationMs
        val snappedEnd = if (snap && _isSnappingEnabled.value) calculateSnap(targetEnd, ignoreClipIds = setOf(clipId)).snappedPosMs else targetEnd
        val dur = (snappedEnd - clip.timelineStartMs).coerceAtLeast(200L)
        recordHistory()
        val list = _timeline.value.effectClips.map {
          if (it.id == clipId) it.copy(durationMs = dur)
          else it
        }
        _timeline.value = _timeline.value.copy(effectClips = list)
      }
      SelectedTrackElement.None -> {}
    }
  }

  fun moveClip(clipId: String, newStartMs: Long, snap: Boolean = true) {
    val element = findTrackElementForClip(clipId)
    val duration = when (element) {
      is SelectedTrackElement.Video -> _timeline.value.videoClips.find { it.id == clipId }?.durationMs ?: return
      is SelectedTrackElement.Overlay -> _timeline.value.overlayClips.find { it.id == clipId }?.durationMs ?: return
      is SelectedTrackElement.Audio -> _timeline.value.audioClips.find { it.id == clipId }?.durationMs ?: return
      is SelectedTrackElement.Text -> _timeline.value.textClips.find { it.id == clipId }?.durationMs ?: return
      is SelectedTrackElement.Sticker -> _timeline.value.stickerClips.find { it.id == clipId }?.durationMs ?: return
      is SelectedTrackElement.Effect -> _timeline.value.effectClips.find { it.id == clipId }?.durationMs ?: return
      SelectedTrackElement.None -> return
    }

    var start = newStartMs.coerceAtLeast(0L)
    if (snap && _isSnappingEnabled.value) {
      val startSnap = calculateSnap(start, ignoreClipIds = setOf(clipId))
      if (startSnap.didSnap) {
        start = startSnap.snappedPosMs
      } else {
        val endSnap = calculateSnap(start + duration, ignoreClipIds = setOf(clipId))
        if (endSnap.didSnap) {
          start = (endSnap.snappedPosMs - duration).coerceAtLeast(0L)
        }
      }
    }

    recordHistory()
    when (element) {
      is SelectedTrackElement.Video -> {
        if (isTrackLocked(TrackType.MAIN_VIDEO)) return
        val list = _timeline.value.videoClips.map {
          if (it.id == clipId) it.copy(timelineStartMs = start) else it
        }.sortedBy { it.timelineStartMs }
        _timeline.value = _timeline.value.copy(videoClips = list)
      }
      is SelectedTrackElement.Overlay -> {
        if (isTrackLocked(TrackType.OVERLAY)) return
        val list = _timeline.value.overlayClips.map {
          if (it.id == clipId) it.copy(timelineStartMs = start) else it
        }.sortedBy { it.timelineStartMs }
        _timeline.value = _timeline.value.copy(overlayClips = list)
      }
      is SelectedTrackElement.Audio -> {
        if (isTrackLocked(TrackType.AUDIO)) return
        val list = _timeline.value.audioClips.map {
          if (it.id == clipId) it.copy(timelineStartMs = start) else it
        }.sortedBy { it.timelineStartMs }
        _timeline.value = _timeline.value.copy(audioClips = list)
      }
      is SelectedTrackElement.Text -> {
        if (isTrackLocked(TrackType.TEXT)) return
        val list = _timeline.value.textClips.map {
          if (it.id == clipId) it.copy(timelineStartMs = start) else it
        }.sortedBy { it.timelineStartMs }
        _timeline.value = _timeline.value.copy(textClips = list)
      }
      is SelectedTrackElement.Sticker -> {
        if (isTrackLocked(TrackType.STICKER)) return
        val list = _timeline.value.stickerClips.map {
          if (it.id == clipId) it.copy(timelineStartMs = start) else it
        }.sortedBy { it.timelineStartMs }
        _timeline.value = _timeline.value.copy(stickerClips = list)
      }
      is SelectedTrackElement.Effect -> {
        if (isTrackLocked(TrackType.EFFECT)) return
        val list = _timeline.value.effectClips.map {
          if (it.id == clipId) it.copy(timelineStartMs = start) else it
        }.sortedBy { it.timelineStartMs }
        _timeline.value = _timeline.value.copy(effectClips = list)
      }
      SelectedTrackElement.None -> {}
    }
  }

  fun splitAtPlayhead(targetClipId: String? = null): Boolean {
    val playhead = _currentPositionMs.value
    val clipId = targetClipId
      ?: (_selectedElement.value as? SelectedTrackElement.Video)?.clipId
      ?: (_selectedElement.value as? SelectedTrackElement.Overlay)?.clipId
      ?: (_selectedElement.value as? SelectedTrackElement.Audio)?.clipId
      ?: (_selectedElement.value as? SelectedTrackElement.Text)?.clipId
      ?: (_selectedElement.value as? SelectedTrackElement.Sticker)?.clipId
      ?: (_selectedElement.value as? SelectedTrackElement.Effect)?.clipId
      ?: findClipUnderPlayhead()
      ?: return false

    val element = findTrackElementForClip(clipId)
    when (element) {
      is SelectedTrackElement.Video -> {
        if (isTrackLocked(TrackType.MAIN_VIDEO)) return false
        val index = _timeline.value.videoClips.indexOfFirst { it.id == clipId }
        if (index == -1) return false
        val clip = _timeline.value.videoClips[index]
        if (playhead <= clip.timelineStartMs + 100L || playhead >= clip.timelineStartMs + clip.durationMs - 100L) return false
        recordHistory()
        val firstDur = playhead - clip.timelineStartMs
        val secondDur = clip.durationMs - firstDur
        val clip1 = clip.copy(
          durationMs = firstDur,
          sourceEndMs = clip.sourceStartMs + (firstDur * clip.speed).toLong()
        )
        val clip2 = clip.copy(
          id = UUID.randomUUID().toString(),
          timelineStartMs = playhead,
          durationMs = secondDur,
          sourceStartMs = clip1.sourceEndMs,
          sourceEndMs = clip.sourceEndMs
        )
        val list = _timeline.value.videoClips.toMutableList()
        list[index] = clip1
        list.add(index + 1, clip2)
        _timeline.value = _timeline.value.copy(videoClips = list)
        selectElement(SelectedTrackElement.Video(clip2.id))
        return true
      }
      is SelectedTrackElement.Overlay -> {
        if (isTrackLocked(TrackType.OVERLAY)) return false
        val index = _timeline.value.overlayClips.indexOfFirst { it.id == clipId }
        if (index == -1) return false
        val clip = _timeline.value.overlayClips[index]
        if (playhead <= clip.timelineStartMs + 100L || playhead >= clip.timelineStartMs + clip.durationMs - 100L) return false
        recordHistory()
        val firstDur = playhead - clip.timelineStartMs
        val secondDur = clip.durationMs - firstDur
        val clip1 = clip.copy(
          durationMs = firstDur,
          sourceEndMs = clip.sourceStartMs + (firstDur * clip.speed).toLong()
        )
        val clip2 = clip.copy(
          id = UUID.randomUUID().toString(),
          timelineStartMs = playhead,
          durationMs = secondDur,
          sourceStartMs = clip1.sourceEndMs,
          sourceEndMs = clip.sourceEndMs
        )
        val list = _timeline.value.overlayClips.toMutableList()
        list[index] = clip1
        list.add(index + 1, clip2)
        _timeline.value = _timeline.value.copy(overlayClips = list)
        selectElement(SelectedTrackElement.Overlay(clip2.id))
        return true
      }
      is SelectedTrackElement.Audio -> {
        if (isTrackLocked(TrackType.AUDIO)) return false
        val index = _timeline.value.audioClips.indexOfFirst { it.id == clipId }
        if (index == -1) return false
        val clip = _timeline.value.audioClips[index]
        if (playhead <= clip.timelineStartMs + 100L || playhead >= clip.timelineStartMs + clip.durationMs - 100L) return false
        recordHistory()
        val firstDur = playhead - clip.timelineStartMs
        val secondDur = clip.durationMs - firstDur
        val clip1 = clip.copy(
          durationMs = firstDur,
          sourceEndMs = clip.sourceStartMs + (firstDur * clip.speed).toLong()
        )
        val clip2 = clip.copy(
          id = UUID.randomUUID().toString(),
          timelineStartMs = playhead,
          durationMs = secondDur,
          sourceStartMs = clip1.sourceEndMs,
          sourceEndMs = clip.sourceEndMs
        )
        val list = _timeline.value.audioClips.toMutableList()
        list[index] = clip1
        list.add(index + 1, clip2)
        _timeline.value = _timeline.value.copy(audioClips = list)
        selectElement(SelectedTrackElement.Audio(clip2.id))
        return true
      }
      is SelectedTrackElement.Text -> {
        if (isTrackLocked(TrackType.TEXT)) return false
        val index = _timeline.value.textClips.indexOfFirst { it.id == clipId }
        if (index == -1) return false
        val clip = _timeline.value.textClips[index]
        if (playhead <= clip.timelineStartMs + 100L || playhead >= clip.timelineStartMs + clip.durationMs - 100L) return false
        recordHistory()
        val firstDur = playhead - clip.timelineStartMs
        val secondDur = clip.durationMs - firstDur
        val clip1 = clip.copy(durationMs = firstDur)
        val clip2 = clip.copy(
          id = UUID.randomUUID().toString(),
          timelineStartMs = playhead,
          durationMs = secondDur
        )
        val list = _timeline.value.textClips.toMutableList()
        list[index] = clip1
        list.add(index + 1, clip2)
        _timeline.value = _timeline.value.copy(textClips = list)
        selectElement(SelectedTrackElement.Text(clip2.id))
        return true
      }
      is SelectedTrackElement.Sticker -> {
        if (isTrackLocked(TrackType.STICKER)) return false
        val index = _timeline.value.stickerClips.indexOfFirst { it.id == clipId }
        if (index == -1) return false
        val clip = _timeline.value.stickerClips[index]
        if (playhead <= clip.timelineStartMs + 100L || playhead >= clip.timelineStartMs + clip.durationMs - 100L) return false
        recordHistory()
        val firstDur = playhead - clip.timelineStartMs
        val secondDur = clip.durationMs - firstDur
        val clip1 = clip.copy(durationMs = firstDur)
        val clip2 = clip.copy(
          id = UUID.randomUUID().toString(),
          timelineStartMs = playhead,
          durationMs = secondDur
        )
        val list = _timeline.value.stickerClips.toMutableList()
        list[index] = clip1
        list.add(index + 1, clip2)
        _timeline.value = _timeline.value.copy(stickerClips = list)
        selectElement(SelectedTrackElement.Sticker(clip2.id))
        return true
      }
      is SelectedTrackElement.Effect -> {
        if (isTrackLocked(TrackType.EFFECT)) return false
        val index = _timeline.value.effectClips.indexOfFirst { it.id == clipId }
        if (index == -1) return false
        val clip = _timeline.value.effectClips[index]
        if (playhead <= clip.timelineStartMs + 100L || playhead >= clip.timelineStartMs + clip.durationMs - 100L) return false
        recordHistory()
        val firstDur = playhead - clip.timelineStartMs
        val secondDur = clip.durationMs - firstDur
        val clip1 = clip.copy(durationMs = firstDur)
        val clip2 = clip.copy(
          id = UUID.randomUUID().toString(),
          timelineStartMs = playhead,
          durationMs = secondDur
        )
        val list = _timeline.value.effectClips.toMutableList()
        list[index] = clip1
        list.add(index + 1, clip2)
        _timeline.value = _timeline.value.copy(effectClips = list)
        selectElement(SelectedTrackElement.Effect(clip2.id))
        return true
      }
      SelectedTrackElement.None -> return false
    }
  }

  private fun findClipUnderPlayhead(): String? {
    val pos = _currentPositionMs.value
    _timeline.value.videoClips.find { pos >= it.timelineStartMs && pos < it.timelineStartMs + it.durationMs }?.let { return it.id }
    _timeline.value.overlayClips.find { pos >= it.timelineStartMs && pos < it.timelineStartMs + it.durationMs }?.let { return it.id }
    _timeline.value.audioClips.find { pos >= it.timelineStartMs && pos < it.timelineStartMs + it.durationMs }?.let { return it.id }
    _timeline.value.textClips.find { pos >= it.timelineStartMs && pos < it.timelineStartMs + it.durationMs }?.let { return it.id }
    _timeline.value.stickerClips.find { pos >= it.timelineStartMs && pos < it.timelineStartMs + it.durationMs }?.let { return it.id }
    _timeline.value.effectClips.find { pos >= it.timelineStartMs && pos < it.timelineStartMs + it.durationMs }?.let { return it.id }
    return null
  }

  fun trimClip(clipId: String, newStartMs: Long, newDurationMs: Long) {
    trimClipLeft(clipId, newStartMs, snap = false)
    trimClipRight(clipId, newDurationMs, snap = false)
  }

  fun rippleDelete(clipIds: Set<String> = emptySet()): Boolean {
    val targets = if (clipIds.isNotEmpty()) clipIds else _selectedClipIds.value
    if (targets.isEmpty()) return deleteSelected()
    recordHistory()

    var newVideo = _timeline.value.videoClips
    if (!isTrackLocked(TrackType.MAIN_VIDEO)) {
      val toDelete = newVideo.filter { it.id in targets }.sortedBy { it.timelineStartMs }
      if (toDelete.isNotEmpty()) {
        val remaining = newVideo.filterNot { it.id in targets }.toMutableList()
        for (del in toDelete) {
          remaining.indices.forEach { i ->
            if (remaining[i].timelineStartMs >= del.timelineStartMs) {
              remaining[i] = remaining[i].copy(
                timelineStartMs = (remaining[i].timelineStartMs - del.durationMs).coerceAtLeast(0L)
              )
            }
          }
        }
        newVideo = remaining.sortedBy { it.timelineStartMs }
      }
    }

    var newOverlay = _timeline.value.overlayClips
    if (!isTrackLocked(TrackType.OVERLAY)) {
      val toDelete = newOverlay.filter { it.id in targets }.sortedBy { it.timelineStartMs }
      if (toDelete.isNotEmpty()) {
        val remaining = newOverlay.filterNot { it.id in targets }.toMutableList()
        for (del in toDelete) {
          remaining.indices.forEach { i ->
            if (remaining[i].timelineStartMs >= del.timelineStartMs) {
              remaining[i] = remaining[i].copy(
                timelineStartMs = (remaining[i].timelineStartMs - del.durationMs).coerceAtLeast(0L)
              )
            }
          }
        }
        newOverlay = remaining.sortedBy { it.timelineStartMs }
      }
    }

    var newAudio = _timeline.value.audioClips
    if (!isTrackLocked(TrackType.AUDIO)) {
      val toDelete = newAudio.filter { it.id in targets }.sortedBy { it.timelineStartMs }
      if (toDelete.isNotEmpty()) {
        val remaining = newAudio.filterNot { it.id in targets }.toMutableList()
        for (del in toDelete) {
          remaining.indices.forEach { i ->
            if (remaining[i].timelineStartMs >= del.timelineStartMs) {
              remaining[i] = remaining[i].copy(
                timelineStartMs = (remaining[i].timelineStartMs - del.durationMs).coerceAtLeast(0L)
              )
            }
          }
        }
        newAudio = remaining.sortedBy { it.timelineStartMs }
      }
    }

    var newText = _timeline.value.textClips
    if (!isTrackLocked(TrackType.TEXT)) {
      val toDelete = newText.filter { it.id in targets }.sortedBy { it.timelineStartMs }
      if (toDelete.isNotEmpty()) {
        val remaining = newText.filterNot { it.id in targets }.toMutableList()
        for (del in toDelete) {
          remaining.indices.forEach { i ->
            if (remaining[i].timelineStartMs >= del.timelineStartMs) {
              remaining[i] = remaining[i].copy(
                timelineStartMs = (remaining[i].timelineStartMs - del.durationMs).coerceAtLeast(0L)
              )
            }
          }
        }
        newText = remaining.sortedBy { it.timelineStartMs }
      }
    }

    var newSticker = _timeline.value.stickerClips
    if (!isTrackLocked(TrackType.STICKER)) {
      val toDelete = newSticker.filter { it.id in targets }.sortedBy { it.timelineStartMs }
      if (toDelete.isNotEmpty()) {
        val remaining = newSticker.filterNot { it.id in targets }.toMutableList()
        for (del in toDelete) {
          remaining.indices.forEach { i ->
            if (remaining[i].timelineStartMs >= del.timelineStartMs) {
              remaining[i] = remaining[i].copy(
                timelineStartMs = (remaining[i].timelineStartMs - del.durationMs).coerceAtLeast(0L)
              )
            }
          }
        }
        newSticker = remaining.sortedBy { it.timelineStartMs }
      }
    }

    var newEffect = _timeline.value.effectClips
    if (!isTrackLocked(TrackType.EFFECT)) {
      val toDelete = newEffect.filter { it.id in targets }.sortedBy { it.timelineStartMs }
      if (toDelete.isNotEmpty()) {
        val remaining = newEffect.filterNot { it.id in targets }.toMutableList()
        for (del in toDelete) {
          remaining.indices.forEach { i ->
            if (remaining[i].timelineStartMs >= del.timelineStartMs) {
              remaining[i] = remaining[i].copy(
                timelineStartMs = (remaining[i].timelineStartMs - del.durationMs).coerceAtLeast(0L)
              )
            }
          }
        }
        newEffect = remaining.sortedBy { it.timelineStartMs }
      }
    }

    _timeline.value = _timeline.value.copy(
      videoClips = newVideo,
      overlayClips = newOverlay,
      audioClips = newAudio,
      textClips = newText,
      stickerClips = newSticker,
      effectClips = newEffect
    )
    clearSelection()
    return true
  }

  fun normalDelete(clipIds: Set<String> = emptySet()): Boolean {
    val targets = if (clipIds.isNotEmpty()) clipIds else _selectedClipIds.value
    if (targets.isEmpty()) return deleteSelected()
    recordHistory()
    val newVideo = if (!isTrackLocked(TrackType.MAIN_VIDEO)) _timeline.value.videoClips.filterNot { it.id in targets } else _timeline.value.videoClips
    val newOverlay = if (!isTrackLocked(TrackType.OVERLAY)) _timeline.value.overlayClips.filterNot { it.id in targets } else _timeline.value.overlayClips
    val newAudio = if (!isTrackLocked(TrackType.AUDIO)) _timeline.value.audioClips.filterNot { it.id in targets } else _timeline.value.audioClips
    val newText = if (!isTrackLocked(TrackType.TEXT)) _timeline.value.textClips.filterNot { it.id in targets } else _timeline.value.textClips
    val newSticker = if (!isTrackLocked(TrackType.STICKER)) _timeline.value.stickerClips.filterNot { it.id in targets } else _timeline.value.stickerClips
    val newEffect = if (!isTrackLocked(TrackType.EFFECT)) _timeline.value.effectClips.filterNot { it.id in targets } else _timeline.value.effectClips
    _timeline.value = _timeline.value.copy(
      videoClips = newVideo,
      overlayClips = newOverlay,
      audioClips = newAudio,
      textClips = newText,
      stickerClips = newSticker,
      effectClips = newEffect
    )
    clearSelection()
    return true
  }

  fun deleteSelected(): Boolean {
    val selected = _selectedElement.value
    recordHistory()
    when (selected) {
      is SelectedTrackElement.Video -> {
        if (isTrackLocked(TrackType.MAIN_VIDEO)) return false
        val filtered = _timeline.value.videoClips.filterNot { it.id == selected.clipId }
        _timeline.value = _timeline.value.copy(videoClips = filtered)
        clearSelection()
        return true
      }
      is SelectedTrackElement.Overlay -> {
        if (isTrackLocked(TrackType.OVERLAY)) return false
        val filtered = _timeline.value.overlayClips.filterNot { it.id == selected.clipId }
        _timeline.value = _timeline.value.copy(overlayClips = filtered)
        clearSelection()
        return true
      }
      is SelectedTrackElement.Audio -> {
        if (isTrackLocked(TrackType.AUDIO)) return false
        val filtered = _timeline.value.audioClips.filterNot { it.id == selected.clipId }
        _timeline.value = _timeline.value.copy(audioClips = filtered)
        clearSelection()
        return true
      }
      is SelectedTrackElement.Text -> {
        if (isTrackLocked(TrackType.TEXT)) return false
        val filtered = _timeline.value.textClips.filterNot { it.id == selected.clipId }
        _timeline.value = _timeline.value.copy(textClips = filtered)
        clearSelection()
        return true
      }
      is SelectedTrackElement.Sticker -> {
        if (isTrackLocked(TrackType.STICKER)) return false
        val filtered = _timeline.value.stickerClips.filterNot { it.id == selected.clipId }
        _timeline.value = _timeline.value.copy(stickerClips = filtered)
        clearSelection()
        return true
      }
      is SelectedTrackElement.Effect -> {
        if (isTrackLocked(TrackType.EFFECT)) return false
        val filtered = _timeline.value.effectClips.filterNot { it.id == selected.clipId }
        _timeline.value = _timeline.value.copy(effectClips = filtered)
        clearSelection()
        return true
      }
      SelectedTrackElement.None -> return false
    }
  }

  fun duplicateClips(clipIds: Set<String> = emptySet()): Boolean {
    val targets = if (clipIds.isNotEmpty()) clipIds else _selectedClipIds.value
    if (targets.isEmpty()) return duplicateSelected()
    recordHistory()
    val newVideo = _timeline.value.videoClips.toMutableList()
    val newOverlay = _timeline.value.overlayClips.toMutableList()
    val newAudio = _timeline.value.audioClips.toMutableList()
    val newText = _timeline.value.textClips.toMutableList()
    val newSticker = _timeline.value.stickerClips.toMutableList()
    val newEffect = _timeline.value.effectClips.toMutableList()
    val newSelected = mutableSetOf<String>()

    if (!isTrackLocked(TrackType.MAIN_VIDEO)) {
      _timeline.value.videoClips.filter { it.id in targets }.forEach { clip ->
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        newVideo.add(copy)
        newSelected.add(copy.id)
      }
    }
    if (!isTrackLocked(TrackType.OVERLAY)) {
      _timeline.value.overlayClips.filter { it.id in targets }.forEach { clip ->
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        newOverlay.add(copy)
        newSelected.add(copy.id)
      }
    }
    if (!isTrackLocked(TrackType.AUDIO)) {
      _timeline.value.audioClips.filter { it.id in targets }.forEach { clip ->
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        newAudio.add(copy)
        newSelected.add(copy.id)
      }
    }
    if (!isTrackLocked(TrackType.TEXT)) {
      _timeline.value.textClips.filter { it.id in targets }.forEach { clip ->
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        newText.add(copy)
        newSelected.add(copy.id)
      }
    }
    if (!isTrackLocked(TrackType.STICKER)) {
      _timeline.value.stickerClips.filter { it.id in targets }.forEach { clip ->
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        newSticker.add(copy)
        newSelected.add(copy.id)
      }
    }
    if (!isTrackLocked(TrackType.EFFECT)) {
      _timeline.value.effectClips.filter { it.id in targets }.forEach { clip ->
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        newEffect.add(copy)
        newSelected.add(copy.id)
      }
    }

    _timeline.value = _timeline.value.copy(
      videoClips = newVideo.sortedBy { it.timelineStartMs },
      overlayClips = newOverlay.sortedBy { it.timelineStartMs },
      audioClips = newAudio.sortedBy { it.timelineStartMs },
      textClips = newText.sortedBy { it.timelineStartMs },
      stickerClips = newSticker.sortedBy { it.timelineStartMs },
      effectClips = newEffect.sortedBy { it.timelineStartMs }
    )
    _selectedClipIds.value = newSelected
    if (newSelected.isNotEmpty()) {
      _selectedElement.value = findTrackElementForClip(newSelected.first())
    }
    return true
  }

  fun duplicateSelected(): Boolean {
    val selected = _selectedElement.value
    recordHistory()
    when (selected) {
      is SelectedTrackElement.Video -> {
        if (isTrackLocked(TrackType.MAIN_VIDEO)) return false
        val clip = _timeline.value.videoClips.find { it.id == selected.clipId } ?: return false
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        val list = _timeline.value.videoClips.toMutableList()
        list.add(copy)
        _timeline.value = _timeline.value.copy(videoClips = list)
        selectElement(SelectedTrackElement.Video(copy.id))
        return true
      }
      is SelectedTrackElement.Overlay -> {
        if (isTrackLocked(TrackType.OVERLAY)) return false
        val clip = _timeline.value.overlayClips.find { it.id == selected.clipId } ?: return false
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        val list = _timeline.value.overlayClips.toMutableList()
        list.add(copy)
        _timeline.value = _timeline.value.copy(overlayClips = list)
        selectElement(SelectedTrackElement.Overlay(copy.id))
        return true
      }
      is SelectedTrackElement.Text -> {
        if (isTrackLocked(TrackType.TEXT)) return false
        val clip = _timeline.value.textClips.find { it.id == selected.clipId } ?: return false
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        val list = _timeline.value.textClips.toMutableList()
        list.add(copy)
        _timeline.value = _timeline.value.copy(textClips = list)
        selectElement(SelectedTrackElement.Text(copy.id))
        return true
      }
      is SelectedTrackElement.Audio -> {
        if (isTrackLocked(TrackType.AUDIO)) return false
        val clip = _timeline.value.audioClips.find { it.id == selected.clipId } ?: return false
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        val list = _timeline.value.audioClips.toMutableList()
        list.add(copy)
        _timeline.value = _timeline.value.copy(audioClips = list)
        selectElement(SelectedTrackElement.Audio(copy.id))
        return true
      }
      is SelectedTrackElement.Sticker -> {
        if (isTrackLocked(TrackType.STICKER)) return false
        val clip = _timeline.value.stickerClips.find { it.id == selected.clipId } ?: return false
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        val list = _timeline.value.stickerClips.toMutableList()
        list.add(copy)
        _timeline.value = _timeline.value.copy(stickerClips = list)
        selectElement(SelectedTrackElement.Sticker(copy.id))
        return true
      }
      is SelectedTrackElement.Effect -> {
        if (isTrackLocked(TrackType.EFFECT)) return false
        val clip = _timeline.value.effectClips.find { it.id == selected.clipId } ?: return false
        val copy = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        val list = _timeline.value.effectClips.toMutableList()
        list.add(copy)
        _timeline.value = _timeline.value.copy(effectClips = list)
        selectElement(SelectedTrackElement.Effect(copy.id))
        return true
      }
      else -> return false
    }
  }

  fun copySelectedClips() {
    val targets = _selectedClipIds.value
    if (targets.isEmpty()) return
    val copies = mutableListOf<Any>()
    _timeline.value.videoClips.filter { it.id in targets }.forEach { copies.add(it) }
    _timeline.value.overlayClips.filter { it.id in targets }.forEach { copies.add(it) }
    _timeline.value.audioClips.filter { it.id in targets }.forEach { copies.add(it) }
    _timeline.value.textClips.filter { it.id in targets }.forEach { copies.add(it) }
    _timeline.value.stickerClips.filter { it.id in targets }.forEach { copies.add(it) }
    _timeline.value.effectClips.filter { it.id in targets }.forEach { copies.add(it) }
    _clipboardClips.value = copies
  }

  fun pasteClipsAtPlayhead(): Boolean {
    val items = _clipboardClips.value
    if (items.isEmpty()) return false
    recordHistory()
    val playhead = _currentPositionMs.value
    val newVideo = _timeline.value.videoClips.toMutableList()
    val newOverlay = _timeline.value.overlayClips.toMutableList()
    val newAudio = _timeline.value.audioClips.toMutableList()
    val newText = _timeline.value.textClips.toMutableList()
    val newSticker = _timeline.value.stickerClips.toMutableList()
    val newEffect = _timeline.value.effectClips.toMutableList()
    val newSelected = mutableSetOf<String>()

    items.forEach { item ->
      when (item) {
        is VideoClip -> {
          if (!isTrackLocked(TrackType.MAIN_VIDEO)) {
            val copy = item.copy(id = UUID.randomUUID().toString(), timelineStartMs = playhead)
            newVideo.add(copy)
            newSelected.add(copy.id)
          }
        }
        is AudioClip -> {
          if (!isTrackLocked(TrackType.AUDIO)) {
            val copy = item.copy(id = UUID.randomUUID().toString(), timelineStartMs = playhead)
            newAudio.add(copy)
            newSelected.add(copy.id)
          }
        }
        is TextClip -> {
          if (!isTrackLocked(TrackType.TEXT)) {
            val copy = item.copy(id = UUID.randomUUID().toString(), timelineStartMs = playhead)
            newText.add(copy)
            newSelected.add(copy.id)
          }
        }
        is StickerClip -> {
          if (!isTrackLocked(TrackType.STICKER)) {
            val copy = item.copy(id = UUID.randomUUID().toString(), timelineStartMs = playhead)
            newSticker.add(copy)
            newSelected.add(copy.id)
          }
        }
        is EffectClip -> {
          if (!isTrackLocked(TrackType.EFFECT)) {
            val copy = item.copy(id = UUID.randomUUID().toString(), timelineStartMs = playhead)
            newEffect.add(copy)
            newSelected.add(copy.id)
          }
        }
      }
    }

    _timeline.value = _timeline.value.copy(
      videoClips = newVideo.sortedBy { it.timelineStartMs },
      overlayClips = newOverlay.sortedBy { it.timelineStartMs },
      audioClips = newAudio.sortedBy { it.timelineStartMs },
      textClips = newText.sortedBy { it.timelineStartMs },
      stickerClips = newSticker.sortedBy { it.timelineStartMs },
      effectClips = newEffect.sortedBy { it.timelineStartMs }
    )
    _selectedClipIds.value = newSelected
    if (newSelected.isNotEmpty()) {
      _selectedElement.value = findTrackElementForClip(newSelected.first())
    }
    return true
  }

  fun replaceMedia(
    clipId: String,
    newUri: String,
    newName: String,
    newDurationMs: Long? = null,
    isVideo: Boolean? = null
  ): Boolean {
    val element = findTrackElementForClip(clipId)
    when (element) {
      is SelectedTrackElement.Video -> {
        if (isTrackLocked(TrackType.MAIN_VIDEO)) return false
        recordHistory()
        val list = _timeline.value.videoClips.map { clip ->
          if (clip.id == clipId) {
            clip.copy(
              uri = newUri,
              name = newName,
              isVideo = isVideo ?: clip.isVideo,
              durationMs = newDurationMs ?: clip.durationMs,
              sourceEndMs = newDurationMs ?: clip.sourceEndMs
            )
          } else clip
        }
        _timeline.value = _timeline.value.copy(videoClips = list)
        return true
      }
      is SelectedTrackElement.Overlay -> {
        if (isTrackLocked(TrackType.OVERLAY)) return false
        recordHistory()
        val list = _timeline.value.overlayClips.map { clip ->
          if (clip.id == clipId) {
            clip.copy(
              uri = newUri,
              name = newName,
              isVideo = isVideo ?: clip.isVideo,
              durationMs = newDurationMs ?: clip.durationMs,
              sourceEndMs = newDurationMs ?: clip.sourceEndMs
            )
          } else clip
        }
        _timeline.value = _timeline.value.copy(overlayClips = list)
        return true
      }
      is SelectedTrackElement.Audio -> {
        if (isTrackLocked(TrackType.AUDIO)) return false
        recordHistory()
        val list = _timeline.value.audioClips.map { clip ->
          if (clip.id == clipId) {
            clip.copy(
              uri = newUri,
              title = newName,
              durationMs = newDurationMs ?: clip.durationMs,
              sourceEndMs = newDurationMs ?: clip.sourceEndMs
            )
          } else clip
        }
        _timeline.value = _timeline.value.copy(audioClips = list)
        return true
      }
      else -> return false
    }
  }

  fun toggleReverseSelectedClip(clipId: String? = null): Boolean {
    val targetId = clipId ?: _selectedClipIds.value.firstOrNull() ?: return false
    val element = findTrackElementForClip(targetId)
    recordHistory()
    return when (element) {
      is SelectedTrackElement.Video -> {
        val list = _timeline.value.videoClips.map {
          if (it.id == targetId) it.copy(isReversed = !it.isReversed) else it
        }
        _timeline.value = _timeline.value.copy(videoClips = list)
        true
      }
      is SelectedTrackElement.Overlay -> {
        val list = _timeline.value.overlayClips.map {
          if (it.id == targetId) it.copy(isReversed = !it.isReversed) else it
        }
        _timeline.value = _timeline.value.copy(overlayClips = list)
        true
      }
      is SelectedTrackElement.Audio -> {
        val list = _timeline.value.audioClips.map {
          if (it.id == targetId) it.copy(isReversed = !it.isReversed) else it
        }
        _timeline.value = _timeline.value.copy(audioClips = list)
        true
      }
      else -> false
    }
  }

  fun setClipSpeed(clipId: String? = null, speed: Float): Boolean {
    val targetId = clipId ?: _selectedClipIds.value.firstOrNull() ?: return false
    val element = findTrackElementForClip(targetId)
    val clampedSpeed = speed.coerceIn(0.1f, 10f)
    recordHistory()
    return when (element) {
      is SelectedTrackElement.Video -> {
        val list = _timeline.value.videoClips.map { clip ->
          if (clip.id == targetId) {
            val oldSpeed = clip.speed
            val newDuration = ((clip.durationMs * oldSpeed) / clampedSpeed).toLong().coerceAtLeast(200L)
            clip.copy(speed = clampedSpeed, durationMs = newDuration)
          } else clip
        }
        _timeline.value = _timeline.value.copy(videoClips = list)
        true
      }
      is SelectedTrackElement.Overlay -> {
        val list = _timeline.value.overlayClips.map { clip ->
          if (clip.id == targetId) {
            val oldSpeed = clip.speed
            val newDuration = ((clip.durationMs * oldSpeed) / clampedSpeed).toLong().coerceAtLeast(200L)
            clip.copy(speed = clampedSpeed, durationMs = newDuration)
          } else clip
        }
        _timeline.value = _timeline.value.copy(overlayClips = list)
        true
      }
      is SelectedTrackElement.Audio -> {
        val list = _timeline.value.audioClips.map { clip ->
          if (clip.id == targetId) {
            val oldSpeed = clip.speed
            val newDuration = ((clip.durationMs * oldSpeed) / clampedSpeed).toLong().coerceAtLeast(200L)
            clip.copy(speed = clampedSpeed, durationMs = newDuration)
          } else clip
        }
        _timeline.value = _timeline.value.copy(audioClips = list)
        true
      }
      else -> false
    }
  }

  fun freezeFrameAtPlayhead(freezeDurationMs: Long = 2500L): Boolean {
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
        durationMs = freezeDurationMs,
        sourceStartMs = (_currentPositionMs.value - clip.timelineStartMs).coerceAtLeast(0L),
        sourceEndMs = (_currentPositionMs.value - clip.timelineStartMs).coerceAtLeast(0L)
      )
      val list = _timeline.value.videoClips.toMutableList()
      list.add(freezeClip)
      list.sortBy { it.timelineStartMs }
      _timeline.value = _timeline.value.copy(videoClips = list)
      selectElement(SelectedTrackElement.Video(freezeClip.id))
      return true
    } else if (selected is SelectedTrackElement.Overlay) {
      val clip = _timeline.value.overlayClips.find { it.id == selected.clipId } ?: return false
      recordHistory()
      val freezeClip = VideoClip(
        id = UUID.randomUUID().toString(),
        uri = clip.uri,
        name = "${clip.name} (Freeze)",
        isVideo = false,
        timelineStartMs = _currentPositionMs.value,
        durationMs = freezeDurationMs,
        sourceStartMs = (_currentPositionMs.value - clip.timelineStartMs).coerceAtLeast(0L),
        sourceEndMs = (_currentPositionMs.value - clip.timelineStartMs).coerceAtLeast(0L),
        cropScale = clip.cropScale,
        cropOffsetX = clip.cropOffsetX,
        cropOffsetY = clip.cropOffsetY,
        opacity = clip.opacity
      )
      val list = _timeline.value.overlayClips.toMutableList()
      list.add(freezeClip)
      list.sortBy { it.timelineStartMs }
      _timeline.value = _timeline.value.copy(overlayClips = list)
      selectElement(SelectedTrackElement.Overlay(freezeClip.id))
      return true
    }
    return false
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
