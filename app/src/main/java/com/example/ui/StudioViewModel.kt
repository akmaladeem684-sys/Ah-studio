package com.example.ui

import android.app.Application
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
  CANVAS
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getDatabase(application)
  val repository = ProjectRepository(database)
  val timelineEngine = TimelineEngine()
  val audioEngine = AudioEngine(application)
  val aiTools = AIToolsService()
  val videoExporter = VideoExporter(application)

  val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val drafts: StateFlow<List<ProjectEntity>> = repository.drafts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val exportedVideos: StateFlow<List<ExportedVideoEntity>> = repository.exportedVideos
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val settings: StateFlow<UserSettings> = StudioPreferencesManager.settings

  // Navigation State
  private val _currentScreen = MutableStateFlow(AppScreen.HOME)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  // Active Project State
  private val _activeProjectId = MutableStateFlow("")
  val activeProjectId: StateFlow<String> = _activeProjectId.asStateFlow()

  private val _activeProjectName = MutableStateFlow("Untitled Project")
  val activeProjectName: StateFlow<String> = _activeProjectName.asStateFlow()

  private val _activeAspectRatio = MutableStateFlow(AspectRatio.RATIO_9_16)
  val activeAspectRatio: StateFlow<AspectRatio> = _activeAspectRatio.asStateFlow()

  private val _activeResolution = MutableStateFlow(Resolution.RES_1080P)
  val activeResolution: StateFlow<Resolution> = _activeResolution.asStateFlow()

  private val _activeFps = MutableStateFlow(FrameRate.FPS_30)
  val activeFps: StateFlow<FrameRate> = _activeFps.asStateFlow()

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
    viewModelScope.launch {
      repository.createSampleProjectIfEmpty()
    }

    // Monitor playback state from TimelineEngine
    viewModelScope.launch {
      timelineEngine.isPlaying.collectLatest { isPlaying ->
        if (isPlaying) {
          startPlaybackLoop()
        } else {
          playbackJob?.cancel()
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
  }

  fun createProjectWithMedia(
    name: String,
    uris: List<String>,
    isVideo: Boolean = true,
    aspectRatio: AspectRatio = AspectRatio.RATIO_9_16
  ) {
    var runningStart = 0L
    val clips = uris.mapIndexed { index, uri ->
      val duration = if (isVideo) 4000L else 3000L
      val clip = VideoClip(
        uri = uri,
        name = if (isVideo) "Video ${index + 1}" else "Photo ${index + 1}",
        timelineStartMs = runningStart,
        durationMs = duration,
        isVideo = isVideo
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

    val loadedTimeline = TimelineSerializer.fromJson(project.timelineJson)
    timelineEngine.loadTimeline(loadedTimeline)
    _currentScreen.value = AppScreen.EDITOR
  }

  fun applyTemplate(template: VideoTemplate) {
    val projectId = UUID.randomUUID().toString()
    _activeProjectId.value = projectId
    _activeProjectName.value = "${template.title} Project"
    _activeAspectRatio.value = template.aspectRatio
    _activeResolution.value = Resolution.RES_1080P
    _activeFps.value = FrameRate.FPS_30

    timelineEngine.loadTimeline(template.createTimeline())
    saveCurrentProject()
    _currentScreen.value = AppScreen.EDITOR
  }

  fun saveCurrentProject() {
    val id = _activeProjectId.value
    if (id.isBlank()) return
    viewModelScope.launch {
      repository.saveProject(
        id = id,
        name = _activeProjectName.value,
        durationMs = timelineEngine.timeline.value.totalDurationMs,
        thumbnailPath = "",
        aspectRatio = _activeAspectRatio.value.label,
        resolution = _activeResolution.value.label,
        fps = _activeFps.value.fps,
        timeline = timelineEngine.timeline.value,
        isDraft = false
      )
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

  fun updateProjectSettings(aspectRatio: AspectRatio, resolution: Resolution, fps: FrameRate) {
    _activeAspectRatio.value = aspectRatio
    _activeResolution.value = resolution
    _activeFps.value = fps
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
      _aiStatusMessage.value = "AI analyzing audio & generating synchronized captions..."
      val captions = aiTools.generateAutoCaptions(
        _activeProjectName.value,
        timelineEngine.timeline.value.totalDurationMs,
        language
      )
      val currentList = timelineEngine.timeline.value.textClips.toMutableList()
      currentList.addAll(captions)
      timelineEngine.loadTimeline(timelineEngine.timeline.value.copy(textClips = currentList))
      _isAIBusy.value = false
      _aiStatusMessage.value = "Generated ${captions.size} auto captions successfully!"
      delay(2000)
      _aiStatusMessage.value = ""
    }
  }

  fun runAITranslateCaptions(targetLanguage: String) {
    viewModelScope.launch {
      _isAIBusy.value = true
      _aiStatusMessage.value = "AI translating captions to $targetLanguage..."
      val translated = aiTools.translateCaptions(timelineEngine.timeline.value.textClips, targetLanguage)
      timelineEngine.loadTimeline(timelineEngine.timeline.value.copy(textClips = translated))
      _isAIBusy.value = false
      _aiStatusMessage.value = "Captions translated to $targetLanguage!"
      delay(2000)
      _aiStatusMessage.value = ""
    }
  }

  fun runAIHighlightAnalysis() {
    viewModelScope.launch {
      _isAIBusy.value = true
      _aiStatusMessage.value = "AI scanning visual motion and highlight moments..."
      val highlights = aiTools.analyzeHighlights(timelineEngine.timeline.value)
      _aiHighlights.value = highlights
      _isAIBusy.value = false
      _aiStatusMessage.value = "Found ${highlights.size} optimal scene moments!"
    }
  }

  fun runAIAutoEdit() {
    viewModelScope.launch {
      _isAIBusy.value = true
      _aiStatusMessage.value = "AI Auto-Edit: Analyzing clips, beat synchronization & pacing..."
      val clips = timelineEngine.timeline.value.videoClips
      val editedTimeline = aiTools.autoEditMontage(clips)
      timelineEngine.loadTimeline(editedTimeline)
      _isAIBusy.value = false
      _aiStatusMessage.value = "Montage generated with transitions & timing!"
      delay(2000)
      _aiStatusMessage.value = ""
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
    audioEngine.release()
  }
}
