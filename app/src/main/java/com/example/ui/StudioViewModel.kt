package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AIToolsService
import com.example.ai.VideoHighlightSegment
import com.example.data.local.AppDatabase
import com.example.data.local.ExportedVideoEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.TimelineSerializer
import com.example.data.presets.TemplatesCatalog
import com.example.data.presets.VideoTemplate
import com.example.data.repository.ProjectRepository
import com.example.domain.StudioPreferencesManager
import com.example.domain.UserSettings
import com.example.domain.model.*
import com.example.engine.SelectedTrackElement
import com.example.engine.TimelineEngine
import com.example.engine.audio.AudioEngine
import com.example.engine.export.ExportConfig
import com.example.engine.export.ExportState
import com.example.engine.export.VideoExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.local.CrashRecoveryEntity
import com.example.engine.media.MediaRelinkManager

enum class ProjectSaveStatus {
  SAVED,
  SAVING,
  UNSAVED
}

data class ProjectSaveState(
  val status: ProjectSaveStatus = ProjectSaveStatus.SAVED,
  val lastSavedTimeMs: Long = System.currentTimeMillis()
)

enum class AppScreen {
  HOME,
  EDITOR,
  EXPORT,
  TEMPLATES,
  AI_SUITE,
  EXPORTED_LIBRARY,
  SETTINGS
}

enum class EditorToolbarTab {
  MEDIA,
  OVERLAY,
  EDIT,
  TRIM,
  AUDIO,
  TEXT,
  STICKERS,
  EFFECTS,
  FILTERS,
  TRANSITIONS,
  ADJUST,
  SPEED,
  CHROMA,
  AI,
  CANVAS,
  KEYFRAME,
  CAPTIONS,
  BACKGROUND,
  AI_AVATAR
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getDatabase(application)
  val repository = ProjectRepository(database)
  val timelineEngine = TimelineEngine()
  val audioEngine = AudioEngine(application)
  val aiTools = AIToolsService(application)
  val compositionEngine = com.example.engine.composition.VideoCompositionEngine(application)
  val videoExporter = VideoExporter(application)

  private var isSyncingFromPlayback = false

  val playbackEngine = com.example.engine.playback.VideoPlaybackEngine(
    context = application,
    onTimelinePositionChanged = { posMs ->
      isSyncingFromPlayback = true
      timelineEngine.setPosition(posMs)
      isSyncingFromPlayback = false
    },
    onPlaybackEnded = {
      timelineEngine.pause()
    }
  )

  val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val drafts: StateFlow<List<ProjectEntity>> = repository.drafts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val exportedVideos: StateFlow<List<ExportedVideoEntity>> = repository.exportedVideos
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val settings: StateFlow<UserSettings> = StudioPreferencesManager.settings

  // Navigation State - timeline interface as primary workspace
  private val _currentScreen = MutableStateFlow(AppScreen.EDITOR)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  // Active Project State
  private val _activeProjectId = MutableStateFlow("demo_project_cinema")
  val activeProjectId: StateFlow<String> = _activeProjectId.asStateFlow()

  private val _activeProjectName = MutableStateFlow("Cinematic Travel Reel")
  val activeProjectName: StateFlow<String> = _activeProjectName.asStateFlow()

  private val _activeAspectRatio = MutableStateFlow(AspectRatio.RATIO_9_16)
  val activeAspectRatio: StateFlow<AspectRatio> = _activeAspectRatio.asStateFlow()

  private val _activeResolution = MutableStateFlow(Resolution.RES_1080P)
  val activeResolution: StateFlow<Resolution> = _activeResolution.asStateFlow()

  private val _activeFps = MutableStateFlow(FrameRate.FPS_30)
  val activeFps: StateFlow<FrameRate> = _activeFps.asStateFlow()

  private val _activeSampleRate = MutableStateFlow(48000)
  val activeSampleRate: StateFlow<Int> = _activeSampleRate.asStateFlow()

  private val _activeCanvasColor = MutableStateFlow(0xFF000000)
  val activeCanvasColor: StateFlow<Long> = _activeCanvasColor.asStateFlow()

  // Save State & Missing Media Tracking
  private val _saveState = MutableStateFlow(ProjectSaveState())
  val saveState: StateFlow<ProjectSaveState> = _saveState.asStateFlow()

  private val _missingMediaList = MutableStateFlow<List<MissingMediaItem>>(emptyList())
  val missingMediaList: StateFlow<List<MissingMediaItem>> = _missingMediaList.asStateFlow()

  val activeRecoverySession: StateFlow<CrashRecoveryEntity?> = repository.activeRecoverySession
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  // Active Bottom Sheet/Tool in Editor
  private val _activeToolbarTab = MutableStateFlow<EditorToolbarTab?>(null)
  val activeToolbarTab: StateFlow<EditorToolbarTab?> = _activeToolbarTab.asStateFlow()

  // AI Operation States
  private val _isAIBusy = MutableStateFlow(false)
  val isAIBusy: StateFlow<Boolean> = _isAIBusy.asStateFlow()

  private val _aiStatusMessage = MutableStateFlow("")
  val aiStatusMessage: StateFlow<String> = _aiStatusMessage.asStateFlow()

  private val _aiHighlights = MutableStateFlow<List<VideoHighlightSegment>>(emptyList())
  val aiHighlights: StateFlow<List<VideoHighlightSegment>> = _aiHighlights.asStateFlow()

  // Playback timer job
  private var playbackJob: Job? = null
  private var autoSaveJob: Job? = null

  init {
    // 1. Immediately seed default cinematic timeline so timeline UI has full non-null content on frame 1
    val defaultTimeline = Timeline(
      videoClips = listOf(
        VideoClip(
          id = "sample_clip_1",
          uri = "sample://nature_stream",
          name = "Cinematic Mountain Stream",
          isVideo = true,
          timelineStartMs = 0L,
          durationMs = 4500L,
          sourceStartMs = 0L,
          sourceEndMs = 4500L,
          speed = 1.0f,
          volume = 1.0f
        ),
        VideoClip(
          id = "sample_clip_2",
          uri = "sample://urban_sunset",
          name = "Golden Hour Skyline",
          isVideo = true,
          timelineStartMs = 4500L,
          durationMs = 5500L,
          sourceStartMs = 0L,
          sourceEndMs = 5500L,
          speed = 1.0f,
          volume = 1.0f
        ),
        VideoClip(
          id = "sample_clip_3",
          uri = "sample://cyber_neon",
          name = "Cyberpunk Night Drive",
          isVideo = true,
          timelineStartMs = 10000L,
          durationMs = 5000L,
          sourceStartMs = 0L,
          sourceEndMs = 5000L,
          speed = 1.0f,
          volume = 0.9f
        )
      ),
      audioClips = listOf(
        AudioClip(
          id = "sample_audio_1",
          uri = "internal://lofi_chill_beat",
          title = "Chill Lofi Dreams (Original Mix)",
          timelineStartMs = 0L,
          durationMs = 15000L,
          volume = 0.85f,
          fadeInMs = 800L,
          fadeOutMs = 1200L,
          waveformData = listOf(0.2f, 0.4f, 0.6f, 0.9f, 0.7f, 0.5f, 0.8f, 1.0f, 0.6f, 0.4f, 0.7f, 0.8f, 0.5f, 0.3f, 0.6f, 0.7f, 0.4f, 0.2f)
        )
      ),
      textClips = listOf(
        TextClip(
          id = "sample_text_1",
          text = "CINEMATIC JOURNEY",
          timelineStartMs = 500L,
          durationMs = 3500L,
          fontFamily = "Sans-Serif",
          fontSizeSp = 28f,
          fontWeight = 900,
          textColor = 0xFFFFFFFF,
          hasGradient = true,
          gradientColorStart = 0xFF00E5FF,
          gradientColorEnd = 0xFF8B5CF6,
          strokeWidth = 2f,
          strokeColor = 0xAA000000,
          posY = -0.2f,
          animationType = "Pop"
        ),
        TextClip(
          id = "sample_text_2",
          text = "Shot on AH Video Studio Pro",
          timelineStartMs = 4600L,
          durationMs = 4000L,
          fontFamily = "Default",
          fontSizeSp = 18f,
          textColor = 0xFFE2E8F0,
          posY = 0.35f,
          animationType = "Fade"
        )
      ),
      stickerClips = listOf(
        StickerClip(
          id = "sample_sticker_1",
          emojiOrAsset = "✨",
          timelineStartMs = 1000L,
          durationMs = 3000L,
          posX = 0.35f,
          posY = -0.3f,
          scale = 1.2f
        )
      ),
      effectClips = listOf(
        EffectClip(
          id = "sample_effect_1",
          effectType = EffectType.GLOW,
          timelineStartMs = 4000L,
          durationMs = 2000L,
          intensity = 0.75f
        )
      ),
      transitions = listOf(
        Transition(
          id = "sample_trans_1",
          clipIndexBefore = 0,
          type = TransitionType.DISSOLVE,
          durationMs = 600L
        )
      ),
      adjustments = VideoAdjustments(
        brightness = 0.05f,
        contrast = 1.1f,
        saturation = 1.15f,
        vignette = 0.15f
      ),
      filter = FilterSettings(
        type = FilterType.CINEMATIC,
        intensity = 0.85f
      )
    )
    timelineEngine.loadTimeline(defaultTimeline)

    viewModelScope.launch {
      try {
        repository.createSampleProjectIfEmpty()
      } catch (e: Exception) {
        android.util.Log.e("StudioViewModel", "Error creating sample project", e)
      }
    }

    // Sync Timeline changes with Playback Engine, mark unsaved, and persist recovery snapshot
    viewModelScope.launch {
      timelineEngine.timeline.collectLatest { timeline ->
        playbackEngine.updateTimeline(timeline)
        if (_activeProjectId.value.isNotBlank() && _currentScreen.value == AppScreen.EDITOR) {
          _saveState.value = _saveState.value.copy(status = ProjectSaveStatus.UNSAVED)
          // Debounce crash recovery snapshot so every keystroke or trim is immediately protected
          delay(1200L)
          val currentSettings = ProjectSettings(
            aspectRatio = _activeAspectRatio.value,
            resolution = _activeResolution.value,
            fps = _activeFps.value,
            sampleRateHz = _activeSampleRate.value,
            canvasBackgroundColor = _activeCanvasColor.value,
            totalDurationMs = timeline.totalDurationMs
          )
          repository.saveCrashRecoverySession(
            projectId = _activeProjectId.value,
            projectName = _activeProjectName.value,
            timeline = timeline,
            settings = currentSettings
          )
        }
      }
    }

    // Monitor playback state from TimelineEngine
    viewModelScope.launch {
      timelineEngine.isPlaying.collectLatest { isPlaying ->
        if (isPlaying) {
          playbackEngine.play()
        } else {
          playbackEngine.pause()
        }
      }
    }

    // Sync seeking from timeline UI into playback engine
    viewModelScope.launch {
      timelineEngine.currentPositionMs.collectLatest { posMs ->
        if (!isSyncingFromPlayback && !timelineEngine.isPlaying.value) {
          playbackEngine.seekTo(posMs)
        }
      }
    }

    // Periodic auto-save
    startAutoSave()
  }

  fun navigateTo(screen: AppScreen) {
    if (screen != AppScreen.EDITOR) {
      timelineEngine.pause()
    }
    _currentScreen.value = screen
  }

  fun setActiveToolbarTab(tab: EditorToolbarTab?) {
    _activeToolbarTab.value = tab
  }

  fun createNewProject(
    name: String,
    aspectRatio: AspectRatio,
    resolution: Resolution,
    fps: FrameRate,
    initialMediaClips: List<VideoClip> = emptyList()
  ) {
    val projectId = UUID.randomUUID().toString()
    _activeProjectId.value = projectId
    _activeProjectName.value = name.ifBlank { "Project ${System.currentTimeMillis() % 10000}" }
    _activeAspectRatio.value = aspectRatio
    _activeResolution.value = resolution
    _activeFps.value = fps
    _activeSampleRate.value = 48000
    _activeCanvasColor.value = 0xFF000000

    val initialTimeline = if (initialMediaClips.isNotEmpty()) {
      Timeline(videoClips = initialMediaClips)
    } else {
      // Create a default initial clip to make the project immediately responsive and interactive
      Timeline(
        videoClips = listOf(
          VideoClip(
            name = "Scene 1",
            durationMs = 4000L,
            isVideo = true
          )
        )
      )
    }

    timelineEngine.loadTimeline(initialTimeline)
    saveCurrentProject()
    _currentScreen.value = AppScreen.EDITOR
    checkMissingMedia()
  }

  fun createProjectWithMedia(
    name: String,
    uris: List<String>,
    isVideo: Boolean = true,
    aspectRatio: AspectRatio = AspectRatio.RATIO_9_16
  ) {
    var runningStart = 0L
    val appContext = getApplication<Application>().applicationContext
    val clips = uris.mapIndexed { index, uri ->
      val meta = com.example.engine.media.MediaMetadataHelper.extractMetadata(appContext, uri)
      val duration = meta.durationMs
      val clip = VideoClip(
        uri = uri,
        name = if (meta.isVideo) "Video ${index + 1}" else "Photo ${index + 1}",
        timelineStartMs = runningStart,
        durationMs = duration,
        sourceStartMs = 0L,
        sourceEndMs = duration,
        isVideo = meta.isVideo,
        width = meta.width,
        height = meta.height,
        naturalRotation = meta.rotationDegrees,
        frameRate = meta.frameRate,
        mimeType = meta.mimeType,
        hasAudio = meta.hasAudio
      )
      runningStart += duration
      clip
    }
    createNewProject(
      name = name,
      aspectRatio = aspectRatio,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      initialMediaClips = clips
    )
  }

  fun loadProject(project: ProjectEntity) {
    _activeProjectId.value = project.id
    _activeProjectName.value = project.name
    _activeAspectRatio.value = AspectRatio.values().find { it.label == project.aspectRatio } ?: AspectRatio.RATIO_9_16
    _activeResolution.value = Resolution.values().find { it.label == project.resolution } ?: Resolution.RES_1080P
    _activeFps.value = FrameRate.values().find { it.fps == project.fps } ?: FrameRate.FPS_30
    _activeSampleRate.value = project.sampleRate
    _activeCanvasColor.value = project.canvasColor

    val pkg = TimelineSerializer.fromPackageJson(project.timelineJson)
    val rawTimeline = pkg?.timeline ?: TimelineSerializer.fromJson(project.timelineJson)
    val appContext = getApplication<Application>().applicationContext
    val loadedTimeline = rawTimeline.copy(
      videoClips = rawTimeline.videoClips.map { clip ->
        if (clip.uri.startsWith("asset://") && !MediaRelinkManager.isRealPlayableMedia(appContext, clip.uri)) {
          val safeUri = if (clip.name.contains("Mountain", ignoreCase = true) || clip.name.contains("Stream", ignoreCase = true)) {
            "sample://nature_stream"
          } else {
            "sample://urban_sunset"
          }
          clip.copy(uri = safeUri)
        } else clip
      }
    )
    if (pkg != null && pkg.settings.sampleRateHz > 0) {
      _activeSampleRate.value = pkg.settings.sampleRateHz
      _activeCanvasColor.value = pkg.settings.canvasBackgroundColor
      _activeAspectRatio.value = pkg.settings.aspectRatio
      _activeResolution.value = pkg.settings.resolution
      _activeFps.value = pkg.settings.fps
    }
    timelineEngine.loadTimeline(loadedTimeline)
    _saveState.value = ProjectSaveState(ProjectSaveStatus.SAVED, project.lastEditedTime)
    _currentScreen.value = AppScreen.EDITOR
    checkMissingMedia()
  }

  fun applyTemplate(
    template: VideoTemplate,
    mediaReplacements: Map<String, String> = emptyMap(),
    textReplacements: Map<String, String> = emptyMap()
  ) {
    val projectId = UUID.randomUUID().toString()
    _activeProjectId.value = projectId
    _activeProjectName.value = "${template.title} Project"
    _activeAspectRatio.value = template.aspectRatio
    _activeResolution.value = template.resolution
    _activeFps.value = template.fps
    _activeSampleRate.value = 48000
    _activeCanvasColor.value = 0xFF000000

    val generatedTimeline = template.createTimeline(mediaReplacements, textReplacements)
    timelineEngine.loadTimeline(generatedTimeline)
    saveCurrentProject()
    _currentScreen.value = AppScreen.EDITOR
    checkMissingMedia()
  }

  fun saveCurrentProject(isManual: Boolean = false) {
    val id = _activeProjectId.value
    if (id.isBlank()) return
    viewModelScope.launch {
      _saveState.value = _saveState.value.copy(status = ProjectSaveStatus.SAVING)
      val currentTimeline = timelineEngine.timeline.value
      val missingList = MediaRelinkManager.detectMissingMedia(getApplication(), currentTimeline)
      _missingMediaList.value = missingList

      repository.saveProject(
        id = id,
        name = _activeProjectName.value,
        durationMs = currentTimeline.totalDurationMs,
        thumbnailPath = "",
        aspectRatio = _activeAspectRatio.value.label,
        resolution = _activeResolution.value.label,
        fps = _activeFps.value.fps,
        timeline = currentTimeline,
        isDraft = false,
        sampleRate = _activeSampleRate.value,
        canvasColor = _activeCanvasColor.value,
        hasMissingMedia = missingList.isNotEmpty()
      )
      _saveState.value = ProjectSaveState(ProjectSaveStatus.SAVED, System.currentTimeMillis())
    }
  }

  fun manualSaveProject() {
    saveCurrentProject(isManual = true)
  }

  fun restoreCrashRecoverySession() {
    viewModelScope.launch {
      val session: CrashRecoveryEntity = repository.getActiveRecoverySession() ?: return@launch
      _activeProjectId.value = session.projectId.ifBlank { UUID.randomUUID().toString() }
      _activeProjectName.value = session.projectName.ifBlank { "Recovered Project" }
      val pkg = TimelineSerializer.fromPackageJson(session.timelineJson)
      if (pkg != null && (pkg.timeline.videoClips.isNotEmpty() || pkg.timeline.audioClips.isNotEmpty() || pkg.timeline.textClips.isNotEmpty())) {
        _activeAspectRatio.value = pkg.settings.aspectRatio
        _activeResolution.value = pkg.settings.resolution
        _activeFps.value = pkg.settings.fps
        _activeSampleRate.value = pkg.settings.sampleRateHz
        _activeCanvasColor.value = pkg.settings.canvasBackgroundColor
        timelineEngine.loadTimeline(pkg.timeline)
      } else {
        val recoveredTimeline = TimelineSerializer.fromJson(session.timelineJson)
        timelineEngine.loadTimeline(recoveredTimeline)
      }
      _saveState.value = ProjectSaveState(ProjectSaveStatus.UNSAVED, session.timestamp)
      _currentScreen.value = AppScreen.EDITOR
      checkMissingMedia()
    }
  }

  fun discardCrashRecoverySession() {
    viewModelScope.launch {
      repository.clearCrashRecoverySession()
    }
  }

  fun restorePreviousProject() {
    viewModelScope.launch {
      val mostRecent = allProjects.value.firstOrNull()
      if (mostRecent != null) {
        loadProject(mostRecent)
      }
    }
  }

  fun reorderVideoClips(fromIndex: Int, toIndex: Int) {
    val clips = timelineEngine.timeline.value.videoClips
    if (fromIndex in clips.indices && toIndex in clips.indices && fromIndex != toIndex) {
      val movedClip = clips[fromIndex]
      val success = timelineEngine.reorderVideoClips(fromIndex, toIndex)
      if (success) {
        val updatedClips = timelineEngine.timeline.value.videoClips
        val newClip = updatedClips.find { it.id == movedClip.id }
        if (newClip != null) {
          timelineEngine.selectElement(SelectedTrackElement.Video(newClip.id))
          timelineEngine.setPosition(newClip.timelineStartMs)
          playbackEngine.seekTo(newClip.timelineStartMs)
        }
      }
    }
  }

  fun checkMissingMedia() {
    viewModelScope.launch(Dispatchers.IO) {
      val missing = MediaRelinkManager.detectMissingMedia(getApplication(), timelineEngine.timeline.value)
      _missingMediaList.value = missing
      if (_activeProjectId.value.isNotBlank()) {
        repository.updateMissingMediaStatus(_activeProjectId.value, missing.isNotEmpty())
      }
    }
  }

  fun relinkMedia(clipId: String, newUri: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val currentTimeline = timelineEngine.timeline.value
      val updated = MediaRelinkManager.relinkClip(getApplication(), currentTimeline, clipId, newUri)
      withContext(Dispatchers.Main) {
        timelineEngine.loadTimeline(updated)
        saveCurrentProject(isManual = true)
        checkMissingMedia()
      }
    }
  }

  fun renameProject(id: String, newName: String) {
    viewModelScope.launch {
      repository.renameProject(id, newName)
      if (_activeProjectId.value == id) {
        _activeProjectName.value = newName
      }
    }
  }

  fun duplicateProject(id: String) {
    viewModelScope.launch {
      repository.duplicateProject(id)
    }
  }

  // --- Precision Video Trimming (Media3) ---

  val trimPlaybackPosition = playbackEngine.trimPlaybackPositionMs

  fun previewClipTrim(clip: VideoClip, startMs: Long, endMs: Long, loop: Boolean = true) {
    playbackEngine.previewTrimRange(clip, startMs, endMs, loop)
  }

  fun seekTrimPreview(offsetFromStartMs: Long) {
    playbackEngine.seekTrimPreview(offsetFromStartMs)
  }

  fun seekTrimPreviewToSourceMs(sourceTimeMs: Long) {
    playbackEngine.seekTrimPreviewToSourceMs(sourceTimeMs)
  }

  fun stepTrimFrame(forward: Boolean) {
    playbackEngine.stepTrimFrame(forward)
  }

  fun toggleTrimPlayPause() {
    playbackEngine.toggleTrimPlayPause()
  }

  fun exitTrimPreview() {
    playbackEngine.exitTrimPreview()
  }

  fun moveClipByDelta(clipId: String, deltaMs: Long, snap: Boolean = true) {
    timelineEngine.moveClipByDelta(clipId, deltaMs, snap)
  }

  fun trimClipLeftByDelta(clipId: String, deltaMs: Long, snap: Boolean = true) {
    timelineEngine.trimClipLeftByDelta(clipId, deltaMs, snap)
    val clip = timelineEngine.timeline.value.videoClips.find { it.id == clipId }
      ?: timelineEngine.timeline.value.overlayClips.find { it.id == clipId }
    if (clip != null) {
      playbackEngine.seekTo(clip.timelineStartMs)
    }
  }

  fun trimClipRightByDelta(clipId: String, deltaMs: Long, snap: Boolean = true) {
    timelineEngine.trimClipRightByDelta(clipId, deltaMs, snap)
    val clip = timelineEngine.timeline.value.videoClips.find { it.id == clipId }
      ?: timelineEngine.timeline.value.overlayClips.find { it.id == clipId }
    if (clip != null) {
      playbackEngine.seekTo((clip.timelineStartMs + clip.durationMs - 1L).coerceAtLeast(clip.timelineStartMs))
    }
  }

  fun applyClipTrim(clipId: String, newSourceStartMs: Long, newSourceEndMs: Long) {
    val success = timelineEngine.trimClipSourceRange(clipId, newSourceStartMs, newSourceEndMs, rippleContiguous = true)
    playbackEngine.exitTrimPreview()
    if (success) {
      val clip = timelineEngine.timeline.value.videoClips.find { it.id == clipId }
      if (clip != null) {
        timelineEngine.setPosition(clip.timelineStartMs)
        playbackEngine.seekTo(clip.timelineStartMs)
      }
    }
  }

  fun resetClipTrim(clipId: String) {
    val success = timelineEngine.resetClipTrim(clipId)
    playbackEngine.exitTrimPreview()
    if (success) {
      val clip = timelineEngine.timeline.value.videoClips.find { it.id == clipId }
      if (clip != null) {
        timelineEngine.setPosition(clip.timelineStartMs)
        playbackEngine.seekTo(clip.timelineStartMs)
      }
    }
  }

  fun setClipInPointAtPlayhead(clipId: String) {
    timelineEngine.setClipInPointAtPlayhead(clipId)
    playbackEngine.exitTrimPreview()
  }

  fun setClipOutPointAtPlayhead(clipId: String) {
    timelineEngine.setClipOutPointAtPlayhead(clipId)
    playbackEngine.exitTrimPreview()
  }

  // --- Real-time Audio Waveform & Peak/Silence Actions ---

  private val _waveformStyle = MutableStateFlow(com.example.ui.components.timeline.WaveformStyle.MIRRORED_BARS)
  val waveformStyle: StateFlow<com.example.ui.components.timeline.WaveformStyle> = _waveformStyle.asStateFlow()

  fun cycleWaveformStyle() {
    val current = _waveformStyle.value
    val next = when (current) {
      com.example.ui.components.timeline.WaveformStyle.MIRRORED_BARS -> com.example.ui.components.timeline.WaveformStyle.SOLID_ENVELOPE
      com.example.ui.components.timeline.WaveformStyle.SOLID_ENVELOPE -> com.example.ui.components.timeline.WaveformStyle.BASELINE_UPWARD
      com.example.ui.components.timeline.WaveformStyle.BASELINE_UPWARD -> com.example.ui.components.timeline.WaveformStyle.MIRRORED_BARS
    }
    _waveformStyle.value = next
  }

  fun setWaveformStyle(style: com.example.ui.components.timeline.WaveformStyle) {
    _waveformStyle.value = style
  }

  fun jumpToNextAudioPeak() {
    val jumped = timelineEngine.jumpToNextAudioPeak()
    if (jumped) {
      playbackEngine.seekTo(timelineEngine.currentPositionMs.value)
    }
  }

  fun jumpToPrevAudioPeak() {
    val jumped = timelineEngine.jumpToPrevAudioPeak()
    if (jumped) {
      playbackEngine.seekTo(timelineEngine.currentPositionMs.value)
    }
  }

  fun jumpToNextAudioSilence() {
    val jumped = timelineEngine.jumpToNextAudioSilence()
    if (jumped) {
      playbackEngine.seekTo(timelineEngine.currentPositionMs.value)
    }
  }

  fun jumpToPrevAudioSilence() {
    val jumped = timelineEngine.jumpToPrevAudioSilence()
    if (jumped) {
      playbackEngine.seekTo(timelineEngine.currentPositionMs.value)
    }
  }

  fun removeSilenceInSelectedAudioClip() {
    val selectedId = (timelineEngine.selectedElement.value as? SelectedTrackElement.Audio)?.clipId
    if (selectedId != null) {
      val removed = timelineEngine.removeSilenceFromAudioClip(selectedId)
      if (removed) {
        playbackEngine.seekTo(timelineEngine.currentPositionMs.value)
      }
    }
  }

  fun deleteProject(id: String) {
    viewModelScope.launch {
      repository.deleteProject(id)
    }
  }

  fun deleteExportedVideo(id: String) {
    viewModelScope.launch {
      repository.deleteExportedVideo(id)
    }
  }

  fun updateProjectSettings(
    aspectRatio: AspectRatio,
    resolution: Resolution,
    fps: FrameRate,
    sampleRate: Int = _activeSampleRate.value,
    canvasColor: Long = _activeCanvasColor.value
  ) {
    _activeAspectRatio.value = aspectRatio
    _activeResolution.value = resolution
    _activeFps.value = fps
    _activeSampleRate.value = sampleRate
    _activeCanvasColor.value = canvasColor
    // Update timeline canvas background color as well
    timelineEngine.setCanvasBackgroundColor(canvasColor)
    saveCurrentProject()
  }

  private fun startPlaybackLoop() {
    playbackJob?.cancel()
    playbackJob = viewModelScope.launch {
      val frameIntervalMs = 33L // ~30 fps update rate
      while (isActive && timelineEngine.isPlaying.value) {
        val next = timelineEngine.currentPositionMs.value + frameIntervalMs
        if (next >= timelineEngine.timeline.value.totalDurationMs) {
          timelineEngine.setPosition(0L) // Loop or pause at end
          timelineEngine.pause()
          break
        } else {
          timelineEngine.setPosition(next)
        }
        delay(frameIntervalMs)
      }
    }
  }

  private fun startAutoSave() {
    autoSaveJob?.cancel()
    autoSaveJob = viewModelScope.launch {
      while (isActive) {
        delay(15000L) // 15s auto-save
        if (_activeProjectId.value.isNotBlank() && _currentScreen.value == AppScreen.EDITOR) {
          saveCurrentProject()
        }
      }
    }
  }

  // --- AI Operations ---

  fun runAIAutoCaptions(language: String = "English") {
    viewModelScope.launch {
      _isAIBusy.value = true
      _aiStatusMessage.value = "AI analyzing actual imported audio & transcribing..."
      try {
        val result = aiTools.generateAutoCaptions(timelineEngine.timeline.value, language)
        val captions = result.getOrThrow()
        if (captions.isEmpty()) {
          _aiStatusMessage.value = "No spoken words detected in imported audio."
        } else {
          val currentList = timelineEngine.timeline.value.textClips.toMutableList()
          currentList.addAll(captions)
          timelineEngine.loadTimeline(timelineEngine.timeline.value.copy(textClips = currentList))
          _aiStatusMessage.value = "Generated ${captions.size} auto captions successfully!"
        }
      } catch (e: Exception) {
        _aiStatusMessage.value = e.message ?: "AI Captions unavailable. Configure backend/API credentials."
      } finally {
        _isAIBusy.value = false
        delay(4000)
        _aiStatusMessage.value = ""
      }
    }
  }

  fun runAITranslateCaptions(targetLanguage: String) {
    viewModelScope.launch {
      _isAIBusy.value = true
      _aiStatusMessage.value = "AI translating captions to $targetLanguage (preserving timings)..."
      try {
        val result = aiTools.translateCaptions(timelineEngine.timeline.value.textClips, targetLanguage)
        val translated = result.getOrThrow()
        timelineEngine.loadTimeline(timelineEngine.timeline.value.copy(textClips = translated))
        _aiStatusMessage.value = "Captions translated to $targetLanguage!"
      } catch (e: Exception) {
        _aiStatusMessage.value = e.message ?: "Translation unavailable."
      } finally {
        _isAIBusy.value = false
        delay(4000)
        _aiStatusMessage.value = ""
      }
    }
  }

  fun runAIBackgroundRemoval(inputBitmap: Bitmap, onResult: (Bitmap, Bitmap) -> Unit) {
    viewModelScope.launch {
      _isAIBusy.value = true
      _aiStatusMessage.value = "AI computing color clustering and edge alpha matting..."
      try {
        val cutoutRes = aiTools.removeBackground(inputBitmap)
        val maskRes = aiTools.generateAlphaMask(inputBitmap)
        val cutout = cutoutRes.getOrThrow()
        val mask = maskRes.getOrThrow()
        onResult(cutout, mask)
        _aiStatusMessage.value = "Background removal complete!"
      } catch (e: Exception) {
        _aiStatusMessage.value = e.message ?: "Background removal failed."
      } finally {
        _isAIBusy.value = false
        delay(3000)
        _aiStatusMessage.value = ""
      }
    }
  }

  fun runAINoiseReduction() {
    viewModelScope.launch {
      _isAIBusy.value = true
      _aiStatusMessage.value = "AI Noise Reduction: Sampling noise floor & applying spectral suppression..."
      try {
        val timeline = timelineEngine.timeline.value
        var audioFile: File? = null
        val firstAudio = timeline.audioClips.firstOrNull()
        if (firstAudio != null && firstAudio.uri.isNotBlank()) {
          val candidate = File(firstAudio.uri)
          if (candidate.exists() && candidate.length() > 0L) audioFile = candidate
        }
        if (audioFile == null && timeline.videoClips.isNotEmpty()) {
          audioFile = audioEngine.extractAudioFromVideo(timeline.videoClips.first().uri)
        }

        if (audioFile == null || !audioFile.exists() || audioFile.length() == 0L) {
          throw IllegalStateException("No audio source found on timeline to denoise. Please import a clip with audio.")
        }

        val denoisedResult = aiTools.reduceAudioNoise(audioFile)
        val denoisedFile = denoisedResult.getOrThrow()

        val newAudioClip = AudioClip(
          id = UUID.randomUUID().toString(),
          title = "Denoised Audio",
          uri = denoisedFile.absolutePath,
          timelineStartMs = 0L,
          durationMs = timeline.totalDurationMs.coerceAtLeast(3000L),
          volume = 1.0f
        )
        val updatedAudioClips = timeline.audioClips.toMutableList().apply { add(newAudioClip) }
        timelineEngine.loadTimeline(timeline.copy(audioClips = updatedAudioClips))
        _aiStatusMessage.value = "Noise reduction applied to timeline!"
      } catch (e: Exception) {
        _aiStatusMessage.value = e.message ?: "Noise reduction failed."
      } finally {
        _isAIBusy.value = false
        delay(3500)
        _aiStatusMessage.value = ""
      }
    }
  }

  fun runAITextToSpeech(text: String, pitch: Float = 1.0f, speed: Float = 1.0f) {
    viewModelScope.launch {
      _isAIBusy.value = true
      _aiStatusMessage.value = "Synthesizing actual voice audio..."
      try {
        val result = aiTools.synthesizeSpeech(text, pitch, speed)
        val file = result.getOrThrow()
        val durationMs = ((text.split(" ").size / (2.5f * speed)) * 1000L).toLong().coerceIn(1500L, 30000L)
        val newAudioClip = AudioClip(
          id = UUID.randomUUID().toString(),
          title = "AI Voice: ${text.take(20)}...",
          uri = file.absolutePath,
          timelineStartMs = timelineEngine.currentPositionMs.value,
          durationMs = durationMs,
          volume = 1.0f
        )
        val currentAudio = timelineEngine.timeline.value.audioClips.toMutableList().apply { add(newAudioClip) }
        timelineEngine.loadTimeline(timelineEngine.timeline.value.copy(audioClips = currentAudio))
        _aiStatusMessage.value = "AI Voice audio added to timeline audio track!"
      } catch (e: Exception) {
        _aiStatusMessage.value = e.message ?: "Voice synthesis failed."
      } finally {
        _isAIBusy.value = false
        delay(3000)
        _aiStatusMessage.value = ""
      }
    }
  }

  fun runAIHighlightAnalysis() {
    viewModelScope.launch {
      _isAIBusy.value = true
      _aiStatusMessage.value = "AI scanning visual motion and highlight moments..."
      try {
        val result = aiTools.analyzeHighlights(timelineEngine.timeline.value)
        val highlights = result.getOrThrow()
        _aiHighlights.value = highlights
        _aiStatusMessage.value = "Found ${highlights.size} optimal scene moments!"
      } catch (e: Exception) {
        _aiStatusMessage.value = e.message ?: "Highlight analysis unavailable."
      } finally {
        _isAIBusy.value = false
        delay(3500)
        _aiStatusMessage.value = ""
      }
    }
  }

  fun runAIAutoEdit() {
    viewModelScope.launch {
      _isAIBusy.value = true
      _aiStatusMessage.value = "AI Auto-Edit: Analyzing clips, beat synchronization & pacing..."
      try {
        val clips = timelineEngine.timeline.value.videoClips
        val result = aiTools.autoEditMontage(clips)
        val editedTimeline = result.getOrThrow()
        timelineEngine.loadTimeline(editedTimeline)
        _aiStatusMessage.value = "Montage generated with transitions & timing!"
      } catch (e: Exception) {
        _aiStatusMessage.value = e.message ?: "Auto-edit failed."
      } finally {
        _isAIBusy.value = false
        delay(2500)
        _aiStatusMessage.value = ""
      }
    }
  }

  // --- Export Operation ---

  fun startExport(config: ExportConfig) {
    viewModelScope.launch {
      val file = videoExporter.exportProject(
        projectName = _activeProjectName.value,
        timeline = timelineEngine.timeline.value,
        config = config
      )
      if (file != null) {
        repository.recordExport(
          projectId = _activeProjectId.value,
          title = "${_activeProjectName.value}.mp4",
          filePath = file.absolutePath,
          durationMs = timelineEngine.timeline.value.totalDurationMs,
          resolution = config.resolution.label,
          fps = config.frameRate.fps,
          fileSizeBytes = file.length()
        )
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    playbackJob?.cancel()
    autoSaveJob?.cancel()
    playbackEngine.release()
    audioEngine.release()
    videoExporter.release()
    compositionEngine.releaseGpu()
  }
}
