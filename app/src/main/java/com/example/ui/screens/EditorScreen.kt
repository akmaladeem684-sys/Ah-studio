package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.data.presets.StockMediaCatalog
import com.example.domain.model.*
import com.example.engine.KeyframeInterpolator
import com.example.engine.SelectedTrackElement
import com.example.engine.export.ExportState
import com.example.engine.media.MediaRelinkManager
import com.example.engine.text.TextLayerRenderer
import com.example.ui.components.InteractiveTransformOverlay
import com.example.ui.AppScreen
import com.example.ui.EditorToolbarTab
import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.zIndex
import com.example.ui.StudioViewModel
import com.example.ui.components.KeyframeAnimationPanel
import com.example.ui.components.TransitionsPanel
import com.example.ui.components.trim.VideoTrimmingToolPanel
import com.example.ui.components.formatDuration
import com.example.ui.components.formatDurationShort
import com.example.ui.components.timeline.*
import com.example.ui.components.timeline.LayersDrawer
import com.example.ui.theme.*
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val projectName by viewModel.activeProjectName.collectAsState()
  val aspectRatio by viewModel.activeAspectRatio.collectAsState()
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val currentPosMs by viewModel.timelineEngine.currentPositionMs.collectAsState()
  val isPlaying by viewModel.timelineEngine.isPlaying.collectAsState()
  val canUndo by viewModel.timelineEngine.canUndo.collectAsState()
  val canRedo by viewModel.timelineEngine.canRedo.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()
  val activeTab by viewModel.activeToolbarTab.collectAsState()
  val isSnapping by viewModel.timelineEngine.isSnappingEnabled.collectAsState()
  val timelineZoom by viewModel.timelineEngine.timelineZoom.collectAsState()
  val selectedClipIds by viewModel.timelineEngine.selectedClipIds.collectAsState()
  val isMultiSelectMode by viewModel.timelineEngine.isMultiSelectMode.collectAsState()
  val snapIndicatorMs by viewModel.timelineEngine.snapIndicatorMs.collectAsState()
  val isMagnetic by viewModel.timelineEngine.isMagneticEnabled.collectAsState()
  val clipboardClips by viewModel.timelineEngine.clipboardClips.collectAsState()
  val selectedKeyframeIds by viewModel.timelineEngine.selectedKeyframeIds.collectAsState()
  val saveState by viewModel.saveState.collectAsState()
  val missingMediaList by viewModel.missingMediaList.collectAsState()
  val activeResolution by viewModel.activeResolution.collectAsState()
  val activeFps by viewModel.activeFps.collectAsState()
  val activeSampleRate by viewModel.activeSampleRate.collectAsState()
  val activeCanvasColor by viewModel.activeCanvasColor.collectAsState()
  val exportState by viewModel.videoExporter.exportState.collectAsState()
  val waveformStyle by viewModel.waveformStyle.collectAsState()
  val selectedTransitionCutIndex by viewModel.timelineEngine.selectedTransitionCutIndex.collectAsState()
  val timelineFps by viewModel.timelineEngine.timelineFps.collectAsState()
  val isFrameSnapping by viewModel.timelineEngine.isFrameSnapping.collectAsState()

  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
  var isLayersOpen by remember { mutableStateOf(false) }

  var showRenameDialog by remember { mutableStateOf(false) }
  var showSpeedDialog by remember { mutableStateOf(false) }
  var showProjectSettingsDialog by remember { mutableStateOf(false) }
  var showExportConfigDialog by remember { mutableStateOf(false) }
  var showRelinkMediaDialog by remember { mutableStateOf(false) }
  var isFullscreenPreview by remember { mutableStateOf(false) }
  var pendingReplaceClipId by remember { mutableStateOf<String?>(null) }
  var draggedTransitionType by remember { mutableStateOf<TransitionType?>(null) }

  val replaceMediaPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null && pendingReplaceClipId != null) {
      val clipId = pendingReplaceClipId!!
      viewModel.timelineEngine.replaceMedia(
        clipId = clipId,
        newUri = uri.toString(),
        newName = "Replaced Media"
      )
      pendingReplaceClipId = null
    }
  }

  // Gallery / Media Picker Launcher for Timeline '+' Button (Videos & Images)
  val timelineMediaPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      uris.forEach { uri ->
        val fileName = try {
          var result: String? = null
          if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
              if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = it.getString(index)
              }
            }
          }
          result ?: uri.lastPathSegment ?: "Imported Media"
        } catch (e: Exception) {
          uri.lastPathSegment ?: "Imported Media"
        }

        val metadata = com.example.engine.media.MediaMetadataHelper.extractMetadata(
          context,
          uri.toString(),
          defaultImageDurationMs = 3000L
        )

        viewModel.timelineEngine.addVideoClip(
          uri = uri.toString(),
          name = fileName,
          isVideo = metadata.isVideo,
          durationMs = metadata.durationMs,
          atPlayhead = false,
          width = metadata.width,
          height = metadata.height,
          rotationDegrees = metadata.rotationDegrees,
          frameRate = metadata.frameRate,
          mimeType = metadata.mimeType,
          hasAudio = metadata.hasAudio
        )
      }
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .background(StudioDarkBg),
      containerColor = StudioDarkBg,
      topBar = {
        EditorTopBar(
          activeResolution = activeResolution,
          exportState = exportState,
          canUndo = canUndo,
          canRedo = canRedo,
          onUndoClick = { viewModel.timelineEngine.undo() },
          onRedoClick = { viewModel.timelineEngine.redo() },
          onBackClick = {
            viewModel.saveCurrentProject()
            viewModel.navigateTo(AppScreen.HOME)
          },
          onSearchClick = { showProjectSettingsDialog = true },
          onResolutionSelect = { res ->
            viewModel.updateProjectSettings(aspectRatio, res, activeFps, activeSampleRate, activeCanvasColor)
          },
          onExportClick = {
            viewModel.saveCurrentProject()
            showExportConfigDialog = true
          }
        )
      },
    bottomBar = {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Active Sub-Tool Panel (if opened)
        AnimatedVisibility(
          visible = activeTab != null,
          enter = slideInVertically { it } + fadeIn(),
          exit = slideOutVertically { it } + fadeOut()
        ) {
          when (activeTab) {
            EditorToolbarTab.MEDIA -> MediaImportPanel(
              viewModel = viewModel,
              onDismiss = { viewModel.setActiveToolbarTab(null) }
            )
            EditorToolbarTab.OVERLAY -> OverlayToolPanel(
              viewModel = viewModel,
              onDismiss = { viewModel.setActiveToolbarTab(null) }
            )
            EditorToolbarTab.EDIT -> EditToolPanel(viewModel)
            EditorToolbarTab.TRIM -> VideoTrimmingToolPanel(
              viewModel = viewModel,
              onDismiss = { viewModel.setActiveToolbarTab(null) }
            )
            EditorToolbarTab.ADJUST -> AdjustToolPanel(viewModel)
            EditorToolbarTab.SPEED -> SpeedToolPanel(viewModel)
            EditorToolbarTab.FILTERS -> FiltersToolPanel(viewModel)
            EditorToolbarTab.EFFECTS -> EffectsToolPanel(viewModel)
            EditorToolbarTab.TRANSITIONS -> TransitionsPanel(
              viewModel = viewModel,
              onStartDragTransition = { draggedTransitionType = it }
            )
            EditorToolbarTab.TEXT -> TextEditorPanel(viewModel)
            EditorToolbarTab.AUDIO -> AudioToolPanel(viewModel)
            EditorToolbarTab.VOLUME -> VolumeToolPanel(viewModel)
            EditorToolbarTab.STICKERS -> StickersToolPanel(viewModel)
            EditorToolbarTab.CHROMA -> ChromaKeyPanel(viewModel)
            EditorToolbarTab.CANVAS -> CanvasPanel(viewModel)
            EditorToolbarTab.KEYFRAME -> KeyframeAnimationPanel(viewModel)
            EditorToolbarTab.CAPTIONS -> CaptionsToolPanel(viewModel)
            EditorToolbarTab.AI -> GenerateMediaToolPanel(viewModel)
            EditorToolbarTab.AI_AVATAR -> AIAvatarToolPanel(viewModel)
            EditorToolbarTab.BACKGROUND -> BackgroundToolPanel(viewModel)
            null -> {}
          }
        }

        // Bottom Navigation Tools Bar
        EditorBottomToolbar(
          activeTab = activeTab,
          onTabSelected = { tab ->
            viewModel.setActiveToolbarTab(if (activeTab == tab) null else tab)
          }
        )
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // Missing Media Warning Bar
      if (missingMediaList.isNotEmpty()) {
        Surface(
          color = AmberAccent.copy(alpha = 0.2f),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showRelinkMediaDialog = true }
            .testTag("missing_media_alert_banner")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${missingMediaList.size} missing media clip${if (missingMediaList.size == 1) "" else "s"} detected",
                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
              )
            }
            Button(
              onClick = { showRelinkMediaDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
              modifier = Modifier
                .height(26.dp)
                .testTag("relink_media_banner_button")
            ) {
              Text("Relink", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
          }
        }
      }

      // 1. VIDEO PREVIEW CONTAINER (Clean, unobscured video surface for text, stickers & PIP)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1.6f)
          .background(Color.Black)
          .testTag("video_preview_container"),
        contentAlignment = Alignment.Center
      ) {
        VideoPreviewSurface(
          timeline = timeline,
          currentPosMs = currentPosMs,
          aspectRatio = aspectRatio,
          selectedElement = selectedElement,
          onSelectElement = { viewModel.timelineEngine.selectElement(it) },
          onUpdateOverlay = { viewModel.timelineEngine.updateOverlayClip(it) },
          onUpdateText = { viewModel.timelineEngine.updateTextClip(it) },
          onUpdateSticker = { viewModel.timelineEngine.updateStickerClip(it) },
          onDeleteClip = { viewModel.timelineEngine.deleteClips(setOf(it)) },
          onDuplicateClip = { viewModel.timelineEngine.duplicateClips(setOf(it)) },
          player = viewModel.playbackEngine.player,
          onToggleFullscreen = { isFullscreenPreview = true },
          onAddMedia = {
            timelineMediaPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
          },
          modifier = Modifier
            .fillMaxSize()
            .testTag("video_preview")
        )
      }

      // 2. CONTROLS BAR BELOW THE VIDEO (Underneath video, not over video)
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .background(StudioDarkBg)
          .border(BorderStroke(1.dp, StudioBorder))
          .padding(horizontal = 12.dp, vertical = 6.dp),
        color = StudioDarkBg
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left: Frame step back (-1F) & Timecode display
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(
              onClick = {
                viewModel.timelineEngine.stepFrames(-1)
                viewModel.playbackEngine.seekTo(viewModel.timelineEngine.currentPositionMs.value)
              },
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(StudioSurface)
                .testTag("control_step_prev")
            ) {
              Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "-1 Frame",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = StudioSurfaceVariant,
              border = BorderStroke(1.dp, StudioBorder)
            ) {
              Text(
                text = "${formatDuration(currentPosMs)} / ${formatDurationShort(timeline.totalDurationMs)}",
                style = MaterialTheme.typography.labelMedium.copy(
                  color = CyanAccent,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                ),
                modifier = Modifier
                  .padding(horizontal = 10.dp, vertical = 5.dp)
                  .testTag("preview_timecode_text")
              )
            }

            IconButton(
              onClick = {
                viewModel.timelineEngine.stepFrames(1)
                viewModel.playbackEngine.seekTo(viewModel.timelineEngine.currentPositionMs.value)
              },
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(StudioSurface)
                .testTag("control_step_next")
            ) {
              Icon(
                Icons.Default.SkipNext,
                contentDescription = "+1 Frame",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          // Center: Main Play / Pause ⏯️ Button below video
          Surface(
            onClick = { viewModel.timelineEngine.togglePlayPause() },
            shape = CircleShape,
            color = if (isPlaying) RedAccent else CyanAccent,
            modifier = Modifier
              .size(46.dp)
              .testTag("below_video_play_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
              )
            }
          }

          // Right: Filters Panel Toggle & Fullscreen
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            IconButton(
              onClick = {
                viewModel.setActiveToolbarTab(
                  if (activeTab == EditorToolbarTab.FILTERS) null else EditorToolbarTab.FILTERS
                )
              },
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (activeTab == EditorToolbarTab.FILTERS) PurpleAccent else StudioSurface)
                .testTag("below_video_filters_button")
            ) {
              Icon(
                Icons.Default.FilterBAndW,
                contentDescription = "Filters Panel",
                tint = if (activeTab == EditorToolbarTab.FILTERS) Color.White else TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }

            IconButton(
              onClick = { isFullscreenPreview = true },
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(StudioSurface)
                .testTag("below_video_fullscreen_button")
            ) {
              Icon(
                Icons.Default.Fullscreen,
                contentDescription = "Fullscreen",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // Inline Filters Panel directly below video preview controls
      if (activeTab == EditorToolbarTab.FILTERS) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("filters_panel_below_video"),
          color = StudioSurface,
          border = BorderStroke(1.dp, StudioBorder)
        ) {
          FiltersToolPanel(viewModel = viewModel)
        }
      }

      var multiTrackZoom by remember { mutableFloatStateOf(1.0f) }

      LaunchedEffect(timeline.audioClips.isEmpty(), timeline.videoClips.isNotEmpty()) {
        if (timeline.audioClips.isEmpty() && timeline.videoClips.isNotEmpty()) {
          viewModel.timelineEngine.ensureAudioTrackExists()
        }
      }

      if (draggedTransitionType != null) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
          shape = RoundedCornerShape(8.dp),
          color = PurpleAccent.copy(alpha = 0.95f),
          border = BorderStroke(1.dp, Color.White)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Transform, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Dragging \"${draggedTransitionType?.displayName}\" ➔ Tap any Cut diamond on timeline",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
              )
            }
            IconButton(
              onClick = { draggedTransitionType = null },
              modifier = Modifier.size(20.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Cancel Drag", tint = Color.White, modifier = Modifier.size(14.dp))
            }
          }
        }
      }

      // STUDIO MULTI-TRACK TIMELINE: Full multi-track video, audio, text, sticker, and effect tracks
      MultiTrackTimeline(
        timeline = timeline,
        currentPosMs = currentPosMs,
        zoom = multiTrackZoom,
        selectedElement = selectedElement,
        selectedClipIds = selectedClipIds,
        isMultiSelectMode = isMultiSelectMode,
        snapIndicatorMs = snapIndicatorMs,
        onSeek = {
          viewModel.timelineEngine.setPosition(it)
          viewModel.playbackEngine.seekTo(it)
        },
        onSelectElement = { viewModel.timelineEngine.selectElement(it) },
        onToggleClipSelection = { viewModel.timelineEngine.toggleSelectClip(it) },
        onZoomChange = { multiTrackZoom = it },
        onReorderVideoClips = { from, to -> viewModel.reorderVideoClips(from, to) },
        onOpenTrimTool = { viewModel.setActiveToolbarTab(EditorToolbarTab.TRIM) },
        onOpenKeyframeTool = { viewModel.setActiveToolbarTab(EditorToolbarTab.KEYFRAME) },
        onOpenTransitionsTool = { viewModel.setActiveToolbarTab(EditorToolbarTab.TRANSITIONS) },
        selectedTransitionCutIndex = selectedTransitionCutIndex,
        onSelectTransitionCut = { cutIdx ->
          viewModel.timelineEngine.setSelectedTransitionCutIndex(cutIdx)
        },
        draggedTransitionType = draggedTransitionType,
        onDropTransition = { cutIdx, type ->
          viewModel.timelineEngine.setTransition(cutIdx, type)
          draggedTransitionType = null
        },
        onAddMedia = {
          try {
            timelineMediaPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
          } catch (e: Exception) {
            viewModel.setActiveToolbarTab(EditorToolbarTab.MEDIA)
          }
        },
        onAddAudio = {
          viewModel.setActiveToolbarTab(EditorToolbarTab.AUDIO)
        },
        onAddText = {
          viewModel.setActiveToolbarTab(EditorToolbarTab.TEXT)
        },
        onSplitClip = {
          val clipId = when (val el = selectedElement) {
            is SelectedTrackElement.Video -> el.clipId
            is SelectedTrackElement.Overlay -> el.clipId
            is SelectedTrackElement.Audio -> el.clipId
            else -> null
          }
          if (clipId != null) {
            viewModel.timelineEngine.splitSelectedClipAtPlayhead()
          }
        },
        onTrimLeftToPlayhead = {
          val clipId = when (val el = selectedElement) {
            is SelectedTrackElement.Video -> el.clipId
            is SelectedTrackElement.Overlay -> el.clipId
            else -> null
          }
          if (clipId != null) {
            viewModel.setClipInPointAtPlayhead(clipId)
          }
        },
        onTrimRightToPlayhead = {
          val clipId = when (val el = selectedElement) {
            is SelectedTrackElement.Video -> el.clipId
            is SelectedTrackElement.Overlay -> el.clipId
            else -> null
          }
          if (clipId != null) {
            viewModel.setClipOutPointAtPlayhead(clipId)
          }
        },
        onDeleteClip = { viewModel.timelineEngine.deleteSelected() },
        onRippleDelete = { viewModel.timelineEngine.rippleDelete() },
        onNormalDelete = { viewModel.timelineEngine.normalDelete() },
        onDuplicateClip = { viewModel.timelineEngine.duplicateClips() },
        onCopyClip = { viewModel.timelineEngine.copySelectedClips() },
        onPasteClip = { viewModel.timelineEngine.pasteClipsAtPlayhead() },
        onToggleMultiSelect = { viewModel.timelineEngine.toggleMultiSelectMode() },
        onNextPeak = { viewModel.jumpToNextAudioPeak() },
        onPrevPeak = { viewModel.jumpToPrevAudioPeak() },
        onNextSilence = { viewModel.jumpToNextAudioSilence() },
        onPrevSilence = { viewModel.jumpToPrevAudioSilence() },
        onRemoveSilence = { viewModel.removeSilenceInSelectedAudioClip() },
        waveformStyle = waveformStyle,
        onToggleWaveformStyle = { viewModel.cycleWaveformStyle() },
        fps = timelineFps,
        isFrameSnapping = isFrameSnapping,
        onStepFrames = { delta ->
          viewModel.timelineEngine.stepFrames(delta)
          viewModel.playbackEngine.seekTo(viewModel.timelineEngine.currentPositionMs.value)
        },
        onSeekToPrevCut = {
          viewModel.timelineEngine.seekToPreviousCut()
          viewModel.playbackEngine.seekTo(viewModel.timelineEngine.currentPositionMs.value)
        },
        onSeekToNextCut = {
          viewModel.timelineEngine.seekToNextCut()
          viewModel.playbackEngine.seekTo(viewModel.timelineEngine.currentPositionMs.value)
        },
        onFpsChange = { viewModel.timelineEngine.setTimelineFps(it) },
        onToggleFrameSnapping = { viewModel.timelineEngine.toggleFrameSnapping() },
        onMoveClip = { clipId, delta -> viewModel.moveClipByDelta(clipId, delta) },
        onTrimClipLeft = { clipId, delta -> viewModel.trimClipLeftByDelta(clipId, delta) },
        onTrimClipRight = { clipId, delta -> viewModel.trimClipRightByDelta(clipId, delta) },
        onToggleTrackLock = { viewModel.timelineEngine.toggleTrackLock(it) },
        onToggleTrackHide = { viewModel.timelineEngine.toggleTrackHide(it) },
        onToggleTrackMute = { viewModel.timelineEngine.toggleTrackMute(it) },
        onToggleTrackSolo = { viewModel.timelineEngine.toggleTrackSolo(it) },
        onCycleTrackHeight = { viewModel.timelineEngine.cycleTrackHeight(it) },
        onSelectKeyframe = { viewModel.timelineEngine.selectKeyframe(it) },
        onMoveKeyframe = { kfId, newTime -> viewModel.timelineEngine.moveKeyframe(kfId, newTime) },
        onAddAudioKeyframe = { clipId, relTime, vol ->
          viewModel.timelineEngine.addAudioVolumeKeyframe(clipId, relTime, vol)
        },
        onUpdateAudioKeyframe = { clipId, kfId, relTime, vol ->
          viewModel.timelineEngine.updateAudioVolumeKeyframe(clipId, kfId, relTime, vol)
        },
        onDeleteAudioKeyframe = { clipId, kfId ->
          viewModel.timelineEngine.deleteAudioVolumeKeyframe(clipId, kfId)
        },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      )

      // HIDDEN SIDEBAR (Old layers panel - keep code but hide: 0% width, takes no space)
      Box(
        modifier = Modifier
          .size(0.dp)
          .testTag("layers_panel")
      )
    }
  }
}

  // Speed Dialog
  if (showSpeedDialog) {
    val activeSpeed = (selectedElement as? SelectedTrackElement.Video)?.let { sel ->
      timeline.videoClips.find { it.id == sel.clipId }?.speed
    } ?: 1.0f
    ClipSpeedDialog(
      currentSpeed = activeSpeed,
      onDismiss = { showSpeedDialog = false },
      onConfirm = { newSpeed ->
        viewModel.timelineEngine.setClipSpeed(speed = newSpeed)
        showSpeedDialog = false
      }
    )
  }

  // Rename Dialog
  if (showRenameDialog) {
    RenameProjectDialog(
      currentName = projectName,
      onDismiss = { showRenameDialog = false },
      onConfirm = {
        viewModel.renameProject(viewModel.activeProjectId.value, it)
        showRenameDialog = false
      }
    )
  }

  // Relink Missing Media Dialog
  if (showRelinkMediaDialog) {
    com.example.ui.components.RelinkMediaDialog(
      missingItems = missingMediaList,
      onDismiss = { showRelinkMediaDialog = false },
      onRelink = { clipId, newUri ->
        viewModel.relinkMedia(clipId, newUri)
      }
    )
  }

  // Project Settings Dialog
  if (showProjectSettingsDialog) {
    com.example.ui.components.ProjectSettingsDialog(
      projectName = projectName,
      currentAspectRatio = aspectRatio,
      currentResolution = activeResolution,
      currentFps = activeFps,
      currentSampleRate = activeSampleRate,
      currentCanvasColor = activeCanvasColor,
      totalDurationMs = timeline.totalDurationMs,
      onDismiss = { showProjectSettingsDialog = false },
      onSaveSettings = { aspect, res, fps, sampleRate, canvasColor ->
        viewModel.updateProjectSettings(aspect, res, fps, sampleRate, canvasColor)
      }
    )
  }

  // Export Configuration Dialog (Media3 Transformer)
  if (showExportConfigDialog) {
    com.example.ui.components.export.ExportConfigurationDialog(
      projectName = projectName,
      totalDurationMs = timeline.totalDurationMs,
      aspectRatio = aspectRatio,
      initialResolution = activeResolution,
      initialFps = activeFps,
      onDismiss = { showExportConfigDialog = false },
      onConfirmExport = { config ->
        showExportConfigDialog = false
        viewModel.saveCurrentProject()
        viewModel.startExport(config)
        viewModel.navigateTo(AppScreen.EXPORT)
      }
    )
  }

  // Immersive Fullscreen Video Preview Dialog
  if (isFullscreenPreview) {
    Dialog(
      onDismissRequest = { isFullscreenPreview = false },
      properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black)
      ) {
        VideoPreviewSurface(
          timeline = timeline,
          currentPosMs = currentPosMs,
          aspectRatio = aspectRatio,
          selectedElement = selectedElement,
          onSelectElement = { viewModel.timelineEngine.selectElement(it) },
          onUpdateOverlay = { viewModel.timelineEngine.updateOverlayClip(it) },
          onUpdateText = { viewModel.timelineEngine.updateTextClip(it) },
          onUpdateSticker = { viewModel.timelineEngine.updateStickerClip(it) },
          onDeleteClip = { viewModel.timelineEngine.deleteClips(setOf(it)) },
          onDuplicateClip = { viewModel.timelineEngine.duplicateClips(setOf(it)) },
          player = viewModel.playbackEngine.player,
          onToggleFullscreen = { isFullscreenPreview = false },
          onAddMedia = {
            timelineMediaPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
          },
          modifier = Modifier.fillMaxSize()
        )

        // Top bar overlay
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)))
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { isFullscreenPreview = false },
            modifier = Modifier
              .size(40.dp)
              .background(Color.Black.copy(alpha = 0.6f), CircleShape)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close Fullscreen", tint = Color.White)
          }

          Text(
            text = projectName,
            style = MaterialTheme.typography.titleMedium.copy(
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          )

          Text(
            text = "${formatDuration(currentPosMs)} / ${formatDurationShort(timeline.totalDurationMs)}",
            style = MaterialTheme.typography.labelMedium.copy(
              color = CyanAccent,
              fontWeight = FontWeight.Bold
            )
          )
        }

        // Bottom playback bar overlay
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
            .padding(horizontal = 24.dp, vertical = 18.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { viewModel.timelineEngine.stepBackwardOneFrame() },
            modifier = Modifier.size(44.dp)
          ) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "-1 Frame", tint = Color.White, modifier = Modifier.size(28.dp))
          }
          Spacer(modifier = Modifier.width(20.dp))
          IconButton(
            onClick = { viewModel.timelineEngine.togglePlayPause() },
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(CyanAccent)
          ) {
            Icon(
              if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = "Play/Pause",
              tint = Color.Black,
              modifier = Modifier.size(32.dp)
            )
          }
          Spacer(modifier = Modifier.width(20.dp))
          IconButton(
            onClick = { viewModel.timelineEngine.stepForwardOneFrame() },
            modifier = Modifier.size(44.dp)
          ) {
            Icon(Icons.Default.SkipNext, contentDescription = "+1 Frame", tint = Color.White, modifier = Modifier.size(28.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun EditorTopBar(
  activeResolution: Resolution,
  exportState: ExportState,
  canUndo: Boolean = false,
  canRedo: Boolean = false,
  onUndoClick: () -> Unit = {},
  onRedoClick: () -> Unit = {},
  onBackClick: () -> Unit,
  onSearchClick: () -> Unit,
  onResolutionSelect: (Resolution) -> Unit,
  onExportClick: () -> Unit
) {
  var showResolutionMenu by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(56.dp)
      .background(Color.Black)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Left: Close (X) + Search + Undo + Redo
    IconButton(
      onClick = onBackClick,
      modifier = Modifier
        .size(48.dp)
        .testTag("close_btn")
    ) {
      Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Close",
        tint = Color.White,
        modifier = Modifier.size(24.dp)
      )
    }

    IconButton(
      onClick = onSearchClick,
      modifier = Modifier
        .size(48.dp)
        .testTag("search_btn")
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "Search",
        tint = Color.White,
        modifier = Modifier.size(24.dp)
      )
    }

    IconButton(
      onClick = onUndoClick,
      enabled = canUndo,
      modifier = Modifier
        .size(40.dp)
        .testTag("top_undo_btn")
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Undo,
        contentDescription = "Undo",
        tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.35f),
        modifier = Modifier.size(20.dp)
      )
    }

    IconButton(
      onClick = onRedoClick,
      enabled = canRedo,
      modifier = Modifier
        .size(40.dp)
        .testTag("top_redo_btn")
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Redo,
        contentDescription = "Redo",
        tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.35f),
        modifier = Modifier.size(20.dp)
      )
    }

    // Center: Empty Spacer
    Spacer(modifier = Modifier.weight(1f))

    // Right: "AI UHD" Dropdown
    Box {
      Surface(
        onClick = { showResolutionMenu = true },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
          .height(38.dp)
          .testTag("quality_spinner")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "AI UHD",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontSize = 12.sp
            )
          )
          Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Quality Options",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      DropdownMenu(
        expanded = showResolutionMenu,
        onDismissRequest = { showResolutionMenu = false },
        modifier = Modifier.background(StudioSurface)
      ) {
        listOf(
          Resolution.RES_4K to "AI UHD (4K 2160p)",
          Resolution.RES_2K to "2K QHD (1440p)",
          Resolution.RES_1080P to "1080p FHD",
          Resolution.RES_720P to "720p HD",
          Resolution.RES_480P to "480p SD"
        ).forEach { (res, label) ->
          DropdownMenuItem(
            text = {
              Text(
                text = label,
                color = if (res == activeResolution) CyanAccent else TextPrimary,
                fontWeight = if (res == activeResolution) FontWeight.Bold else FontWeight.Normal
              )
            },
            onClick = {
              onResolutionSelect(res)
              showResolutionMenu = false
            }
          )
        }
      }
    }

    // Right: Cyan "Export" Button
    val isRendering = exportState is ExportState.Rendering
    Button(
      onClick = onExportClick,
      enabled = !isRendering,
      colors = ButtonDefaults.buttonColors(
        containerColor = CyanAccent,
        contentColor = Color.Black,
        disabledContainerColor = CyanAccent.copy(alpha = 0.6f),
        disabledContentColor = Color.Black
      ),
      shape = RoundedCornerShape(8.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
      modifier = Modifier
        .height(38.dp)
        .padding(start = 12.dp)
        .testTag("export_btn")
    ) {
      if (isRendering) {
        val progress = (exportState as ExportState.Rendering).progressPercent
        CircularProgressIndicator(
          progress = { progress },
          modifier = Modifier.size(14.dp),
          strokeWidth = 2.dp,
          color = Color.Black
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "${(progress * 100).toInt()}%",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontSize = 12.sp
          )
        )
      } else {
        Text(
          text = "Export",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontSize = 13.sp
          )
        )
      }
    }
  }
}

@Composable
fun VideoPreviewSurface(
  timeline: Timeline,
  currentPosMs: Long,
  aspectRatio: AspectRatio,
  selectedElement: SelectedTrackElement = SelectedTrackElement.None,
  onSelectElement: (SelectedTrackElement) -> Unit = {},
  onUpdateOverlay: (VideoClip) -> Unit = {},
  onUpdateText: (TextClip) -> Unit = {},
  onUpdateSticker: (StickerClip) -> Unit = {},
  onDeleteClip: (String) -> Unit = {},
  onDuplicateClip: (String) -> Unit = {},
  player: ExoPlayer? = null,
  onToggleFullscreen: (() -> Unit)? = null,
  onAddMedia: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  // Find current active video clip
  val activeClip = remember(timeline.videoClips, currentPosMs) {
    timeline.videoClips.find {
      currentPosMs >= it.timelineStartMs && currentPosMs < it.timelineStartMs + it.durationMs
    } ?: timeline.videoClips.lastOrNull()
  }

  val activeOverlays = remember(timeline.overlayClips, currentPosMs) {
    timeline.overlayClips.filter {
      currentPosMs >= it.timelineStartMs && currentPosMs < it.timelineStartMs + it.durationMs
    }
  }

  val activeTexts = remember(timeline.textClips, currentPosMs) {
    timeline.textClips.filter {
      currentPosMs >= it.timelineStartMs && currentPosMs < it.timelineStartMs + it.durationMs
    }
  }

  val activeStickers = remember(timeline.stickerClips, currentPosMs) {
    timeline.stickerClips.filter {
      currentPosMs >= it.timelineStartMs && currentPosMs < it.timelineStartMs + it.durationMs
    }
  }

  val activeEffects = remember(timeline.effectClips, currentPosMs) {
    timeline.effectClips.filter {
      currentPosMs >= it.timelineStartMs && currentPosMs < it.timelineStartMs + it.durationMs
    }
  }

  // Keyframe calculations
  val clipTransform = remember(activeClip, currentPosMs) {
    if (activeClip != null) {
      val rel = currentPosMs - activeClip.timelineStartMs
      KeyframeInterpolator.interpolate(activeClip, rel)
    } else null
  }

  // Color Matrix for video adjustments and filter presets matching export pipeline
  val combinedColorFilter = remember(timeline.adjustments, timeline.filter) {
    val androidMatrix = com.example.engine.composition.ColorFilterGenerator.createCombinedMatrix(
      timeline.adjustments,
      timeline.filter
    )
    ColorFilter.colorMatrix(ColorMatrix(androidMatrix.array))
  }

  // Pinch-to-zoom & pan inspection state
  var previewZoomScale by remember { mutableFloatStateOf(1.0f) }
  var previewPanOffset by remember { mutableStateOf(Offset.Zero) }

  Card(
    modifier = modifier
      .aspectRatio(aspectRatio.ratio, matchHeightConstraintsFirst = true)
      .clip(RoundedCornerShape(12.dp))
      .border(1.dp, StudioBorder, RoundedCornerShape(12.dp)),
    colors = CardDefaults.cardColors(containerColor = Color(timeline.canvasBackgroundColor))
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(12.dp))
        .pointerInput(Unit) {
          detectTapGestures(
            onDoubleTap = {
              if (previewZoomScale > 1.05f) {
                previewZoomScale = 1.0f
                previewPanOffset = Offset.Zero
              } else {
                previewZoomScale = 2.0f
              }
            }
          )
        }
        .pointerInput(Unit) {
          detectTransformGestures { _, pan, zoom, _ ->
            if (zoom != 1.0f || previewZoomScale > 1.05f) {
              val oldScale = previewZoomScale
              val newScale = (oldScale * zoom).coerceIn(1.0f, 5.0f)
              previewZoomScale = newScale
              if (newScale > 1.0f) {
                val maxX = (newScale - 1f) * 400f
                val maxY = (newScale - 1f) * 400f
                val newPanX = (previewPanOffset.x + pan.x).coerceIn(-maxX, maxX)
                val newPanY = (previewPanOffset.y + pan.y).coerceIn(-maxY, maxY)
                previewPanOffset = Offset(newPanX, newPanY)
              } else {
                previewPanOffset = Offset.Zero
              }
            }
          }
        }
    ) {
      // Zoomable and Pannable Frame Content Container
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            scaleX = previewZoomScale
            scaleY = previewZoomScale
            translationX = previewPanOffset.x
            translationY = previewPanOffset.y
          }
      ) {
        // Background Video / Image Layer
        if (activeClip != null) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer {
                clipTransform?.let { t ->
                  scaleX = t.scaleX
                  scaleY = t.scaleY
                  rotationZ = t.rotation
                  translationX = t.posX * size.width
                  translationY = t.posY * size.height
                  alpha = t.opacity
                }
              },
            contentAlignment = Alignment.Center
          ) {
            val isRealPlayable = remember(activeClip.uri) {
              MediaRelinkManager.isRealPlayableMedia(context, activeClip.uri)
            }
            if (activeClip.isVideo && isRealPlayable && player != null) {
              AndroidView(
                factory = { ctx ->
                  PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT,
                      ViewGroup.LayoutParams.MATCH_PARENT
                    )
                  }
                },
                modifier = Modifier.fillMaxSize()
              )
            } else if (!activeClip.isVideo && activeClip.uri.isNotBlank() && !activeClip.uri.startsWith("stock://") && !activeClip.uri.startsWith("sample://")) {
              AsyncImage(
                model = activeClip.uri,
                contentDescription = activeClip.name,
                contentScale = ContentScale.Fit,
                colorFilter = combinedColorFilter,
                modifier = Modifier.fillMaxSize()
              )
            } else {
              SyntheticClipPreview(
                clip = activeClip,
                currentPosMs = currentPosMs,
                modifier = Modifier.fillMaxSize()
              )
            }
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .clickable { onAddMedia?.invoke() }
              .testTag("empty_timeline_canvas"),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(52.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF1E293B))
                  .border(1.dp, CyanAccent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.VideoLibrary,
                  contentDescription = "Add Media",
                  tint = CyanAccent,
                  modifier = Modifier.size(26.dp)
                )
              }
              Text(
                text = "Tap to add video or photo",
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = TextPrimary,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp
                )
              )
              Text(
                text = "Clean blank timeline ready for your media",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextTertiary,
                  fontSize = 11.sp
                )
              )
            }
          }
        }

        // Active Visual Effects Overlay (Glitch, Glow, RGB Split, Flash)
        if (activeEffects.isNotEmpty()) {
          activeEffects.forEach { effect ->
            when (effect.effectType) {
              EffectType.GLOW -> {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(CyanAccent.copy(alpha = 0.15f * effect.intensity))
                )
              }
              EffectType.FLASH -> {
                val isFlash = (currentPosMs % 400L) < 200L
                if (isFlash) {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(Color.White.copy(alpha = 0.4f * effect.intensity))
                  )
                }
              }
              EffectType.RGB_SPLIT, EffectType.GLITCH -> {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Color.Red.copy(alpha = 0.1f), Color.Blue.copy(alpha = 0.1f))))
                )
              }
              else -> {}
            }
          }
        }

        // Touch-Based Interactive Transformation Layer (Text, PIP Overlays, Stickers, Shapes)
        InteractiveTransformOverlay(
          activeTexts = activeTexts,
          activeOverlays = activeOverlays,
          activeStickers = activeStickers,
          selectedElement = selectedElement,
          currentPosMs = currentPosMs,
          onSelectElement = onSelectElement,
          onUpdateText = onUpdateText,
          onUpdateOverlay = onUpdateOverlay,
          onUpdateSticker = onUpdateSticker,
          onDeleteClip = onDeleteClip,
          onDuplicateClip = onDuplicateClip,
          modifier = Modifier.fillMaxSize()
        )
      }

      // Floating Zoom Scale Reset Badge (Top-Left overlay when zoomed in)
      if (previewZoomScale > 1.05f) {
        Surface(
          onClick = {
            previewZoomScale = 1.0f
            previewPanOffset = Offset.Zero
          },
          shape = RoundedCornerShape(16.dp),
          color = CyanAccent.copy(alpha = 0.95f),
          contentColor = Color.Black,
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .testTag("reset_zoom_badge")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              Icons.Default.FitScreen,
              contentDescription = "Reset Zoom",
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = "%.1fx (Reset)".format(previewZoomScale),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TimelineControlsBar(
  isPlaying: Boolean,
  currentPosMs: Long,
  totalDurationMs: Long,
  zoom: Float,
  canUndo: Boolean,
  canRedo: Boolean,
  isSnapping: Boolean,
  onTogglePlay: () -> Unit,
  onStop: () -> Unit,
  onStepBack: () -> Unit,
  onStepForward: () -> Unit,
  onUndoClick: () -> Unit,
  onRedoClick: () -> Unit,
  onToggleSnapping: () -> Unit,
  onSplit: () -> Unit,
  onDelete: () -> Unit,
  onAddKeyframe: () -> Unit,
  onAddMedia: () -> Unit,
  onZoomChange: (Float) -> Unit,
  onToggleFullscreen: (() -> Unit)? = null
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .drawBehind {
        drawLine(
          color = StudioBorder,
          start = Offset(0f, size.height),
          end = Offset(size.width, size.height),
          strokeWidth = 1.dp.toPx()
        )
      }
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    // Left: Fullscreen icon & Timecode (e.g., 00:07 / 00:29)
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      if (onToggleFullscreen != null) {
        IconButton(
          onClick = onToggleFullscreen,
          modifier = Modifier
            .size(32.dp)
            .testTag("timeline_fullscreen_button")
        ) {
          Icon(
            Icons.Default.Fullscreen,
            contentDescription = "Fullscreen",
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Text(
        text = "${formatDurationShort(currentPosMs)} / ${formatDurationShort(totalDurationMs)}",
        style = MaterialTheme.typography.labelMedium.copy(
          color = TextPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp
        ),
        modifier = Modifier.testTag("timeline_timecode_display")
      )
    }

    // Center: Frame Step Back, Prominent Sky Blue Play/Pause, Frame Step Forward
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      IconButton(onClick = onStepBack, modifier = Modifier.size(30.dp)) {
        Icon(Icons.Default.SkipPrevious, contentDescription = "-1 Frame", tint = TextSecondary, modifier = Modifier.size(18.dp))
      }

      IconButton(
        onClick = onTogglePlay,
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(CyanAccent)
          .testTag("timeline_play_pause")
      ) {
        Icon(
          if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = "Play/Pause",
          tint = Color.White,
          modifier = Modifier.size(22.dp)
        )
      }

      IconButton(onClick = onStepForward, modifier = Modifier.size(30.dp)) {
        Icon(Icons.Default.SkipNext, contentDescription = "+1 Frame", tint = TextSecondary, modifier = Modifier.size(18.dp))
      }
    }

    // Right: Snapping, Undo, Redo, Quick Split, Delete
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      IconButton(
        onClick = onToggleSnapping,
        modifier = Modifier.size(30.dp).testTag("editor_snapping_button")
      ) {
        Icon(
          Icons.Default.Adjust,
          contentDescription = "Snapping",
          tint = if (isSnapping) CyanAccent else TextTertiary,
          modifier = Modifier.size(17.dp)
        )
      }

      IconButton(
        onClick = onUndoClick,
        enabled = canUndo,
        modifier = Modifier.size(30.dp).testTag("editor_undo_button")
      ) {
        Icon(
          Icons.AutoMirrored.Filled.Undo,
          contentDescription = "Undo",
          tint = if (canUndo) TextPrimary else TextTertiary.copy(alpha = 0.35f),
          modifier = Modifier.size(17.dp)
        )
      }

      IconButton(
        onClick = onRedoClick,
        enabled = canRedo,
        modifier = Modifier.size(30.dp).testTag("editor_redo_button")
      ) {
        Icon(
          Icons.AutoMirrored.Filled.Redo,
          contentDescription = "Redo",
          tint = if (canRedo) TextPrimary else TextTertiary.copy(alpha = 0.35f),
          modifier = Modifier.size(17.dp)
        )
      }

      IconButton(
        onClick = onSplit,
        modifier = Modifier.size(30.dp).testTag("timeline_quick_split")
      ) {
        Icon(
          Icons.Default.CallSplit,
          contentDescription = "Split",
          tint = CyanAccent,
          modifier = Modifier.size(17.dp)
        )
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier.size(30.dp).testTag("timeline_quick_delete")
      ) {
        Icon(
          Icons.Default.Delete,
          contentDescription = "Delete",
          tint = RedAccent,
          modifier = Modifier.size(17.dp)
        )
      }
    }
  }
}

// TIMELINE WITH TIMESTAMPS: 40-50dp
@Composable
private fun TimelineTimestampsRow(
  currentPosMs: Long,
  totalDurationMs: Long,
  isMultiTrackView: Boolean = false,
  onToggleMultiTrackView: (() -> Unit)? = null
) {
  val total = totalDurationMs.coerceAtLeast(1000L)
  val safePos = currentPosMs.coerceIn(0L, total)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(45.dp)
      .background(Color.Black)
      .padding(horizontal = 14.dp, vertical = 6.dp)
      .testTag("timeline_container"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "${formatDurationShort(safePos)} / ${formatDurationShort(total)}",
      color = Color.White,
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.testTag("time_display")
    )

    Text(
      text = "•",
      color = Color.White.copy(alpha = 0.5f),
      modifier = Modifier.padding(horizontal = 6.dp)
    )

    // Timestamps around current time
    val step = (total / 5).coerceAtLeast(2000L)
    val times = listOf(
      (safePos - step).coerceAtLeast(0L),
      safePos,
      (safePos + step).coerceAtMost(total)
    )

    Row(
      modifier = Modifier.weight(1f),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      times.forEachIndexed { index, timeMs ->
        Text(
          text = formatDurationShort(timeMs),
          color = if (index == 1) CyanAccent else Color.White.copy(alpha = 0.6f),
          fontSize = 10.sp,
          fontWeight = if (index == 1) FontWeight.Bold else FontWeight.Normal
        )
      }
    }

    // Toggle Multi-Track vs Compact View Pill
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = if (isMultiTrackView) StudioSurfaceVariant else StudioDarkBg,
      border = BorderStroke(1.dp, if (isMultiTrackView) CyanAccent else StudioBorder),
      modifier = Modifier
        .clickable { onToggleMultiTrackView?.invoke() }
        .testTag("timeline_mode_toggle")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = if (isMultiTrackView) Icons.Default.ViewAgenda else Icons.Default.Tune,
          contentDescription = null,
          tint = if (isMultiTrackView) CyanAccent else AudioTrackColor,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (isMultiTrackView) "Multi-Track" else "Envelopes",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        )
      }
    }
  }
}

// FILM STRIP THUMBNAILS: 80-100dp
@Composable
private fun FilmstripThumbnailsRow(
  timeline: Timeline,
  currentPosMs: Long,
  onSeek: (Long) -> Unit,
  onScrollLeft: () -> Unit,
  onAddMedia: () -> Unit
) {
  val totalDuration = timeline.totalDurationMs.coerceAtLeast(2000L)
  val frameIntervalMs = 500L
  val frameCount = ((totalDuration / frameIntervalMs) + 1).toInt().coerceIn(8, 60)

  val scrollState = rememberScrollState()

  LaunchedEffect(currentPosMs) {
    val progress = (currentPosMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    val targetScroll = (scrollState.maxValue * progress).toInt()
    if (!scrollState.isScrollInProgress) {
      scrollState.scrollTo(targetScroll)
    }
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(90.dp)
      .background(Color.Black)
      .testTag("filmstrip_scroll"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Left scroll button
    IconButton(
      onClick = onScrollLeft,
      modifier = Modifier
        .width(48.dp)
        .fillMaxHeight()
        .testTag("scroll_left_btn")
    ) {
      Icon(
        imageVector = Icons.Default.ChevronLeft,
        contentDescription = "Scroll Left",
        tint = Color.White,
        modifier = Modifier.size(28.dp)
      )
    }

    // Scrollable Thumbnails container
    Row(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .horizontalScroll(scrollState)
        .padding(vertical = 4.dp)
        .testTag("filmstrip_container"),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      for (i in 0 until frameCount) {
        val frameTimeMs = (i * frameIntervalMs).coerceAtMost(totalDuration)
        val isCurrentFrame = kotlin.math.abs(currentPosMs - frameTimeMs) < frameIntervalMs

        val activeClip = timeline.videoClips.find { c ->
          frameTimeMs >= c.timelineStartMs && frameTimeMs < (c.timelineStartMs + c.durationMs)
        } ?: timeline.overlayClips.find { c ->
          frameTimeMs >= c.timelineStartMs && frameTimeMs < (c.timelineStartMs + c.durationMs)
        }

        Box(
          modifier = Modifier
            .width(72.dp)
            .height(82.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
            .border(
              width = if (isCurrentFrame) 2.dp else 1.dp,
              color = if (isCurrentFrame) CyanAccent else Color(0xFF334155),
              shape = RoundedCornerShape(6.dp)
            )
            .clickable { onSeek(frameTimeMs) }
        ) {
          if (activeClip?.uri?.isNotEmpty() == true) {
            AsyncImage(
              model = activeClip.uri,
              contentDescription = "Frame ${i + 1}",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.linearGradient(
                    listOf(
                      Color(0xFF0F172A),
                      Color(0xFF1E293B),
                      Color(0xFF0F172A)
                    )
                  )
                )
                .padding(4.dp),
              verticalArrangement = Arrangement.SpaceBetween,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Box(modifier = Modifier.size(3.dp).background(Color.White.copy(alpha = 0.3f)))
                Box(modifier = Modifier.size(3.dp).background(Color.White.copy(alpha = 0.3f)))
              }
              Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = if (isCurrentFrame) CyanAccent else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = formatDurationShort(frameTimeMs),
                color = if (isCurrentFrame) CyanAccent else Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }

          if (isCurrentFrame) {
            Box(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
                .background(CyanAccent)
            )
          }
        }
      }
    }

    // Right add button
    IconButton(
      onClick = onAddMedia,
      modifier = Modifier
        .width(48.dp)
        .fillMaxHeight()
        .testTag("add_btn")
    ) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "Add Media",
        tint = CyanAccent,
        modifier = Modifier.size(28.dp)
      )
    }
  }
}

// BOTTOM TOOLBAR: CapCut-style horizontal scroll with all tools preserved, icons, labels, AI tags
@Composable
private fun EditorBottomToolbar(
  activeTab: EditorToolbarTab?,
  onTabSelected: (EditorToolbarTab) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(68.dp)
      .background(Color(0xFF141416))
      .border(BorderStroke(0.5.dp, Color(0xFF262628)))
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 6.dp, vertical = 4.dp)
      .testTag("toolbar_scroll"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    // 1. Edit ✂️
    EditorToolbarItem(
      icon = Icons.Default.Edit,
      label = "Edit",
      isSelected = activeTab == EditorToolbarTab.EDIT,
      testTag = "edit_btn",
      onClick = { onTabSelected(EditorToolbarTab.EDIT) }
    )

    // 1b. Trim ✂️
    EditorToolbarItem(
      icon = Icons.Default.ContentCut,
      label = "Trim",
      isSelected = activeTab == EditorToolbarTab.TRIM,
      testTag = "trim_btn",
      onClick = { onTabSelected(EditorToolbarTab.TRIM) }
    )

    // 2. Audio 🎵
    EditorToolbarItem(
      icon = Icons.Default.Audiotrack,
      label = "Audio",
      isSelected = activeTab == EditorToolbarTab.AUDIO,
      testTag = "audio_btn",
      onClick = { onTabSelected(EditorToolbarTab.AUDIO) }
    )

    // 2b. Volume 🔊
    EditorToolbarItem(
      icon = Icons.Default.VolumeUp,
      label = "Volume",
      isSelected = activeTab == EditorToolbarTab.VOLUME,
      testTag = "volume_btn",
      onClick = { onTabSelected(EditorToolbarTab.VOLUME) }
    )

    // 3. Text T
    EditorToolbarItem(
      icon = Icons.Default.TextFields,
      label = "Text",
      isSelected = activeTab == EditorToolbarTab.TEXT,
      testTag = "text_btn",
      onClick = { onTabSelected(EditorToolbarTab.TEXT) }
    )

    // 4. Effects ⭐
    EditorToolbarItem(
      icon = Icons.Default.AutoFixHigh,
      label = "Effects",
      isSelected = activeTab == EditorToolbarTab.EFFECTS,
      testTag = "effects_btn",
      onClick = { onTabSelected(EditorToolbarTab.EFFECTS) }
    )

    // 5. Overlay 📦
    EditorToolbarItem(
      icon = Icons.Default.Layers,
      label = "Overlay",
      isSelected = activeTab == EditorToolbarTab.OVERLAY,
      testTag = "overlay_btn",
      onClick = { onTabSelected(EditorToolbarTab.OVERLAY) }
    )

    // 6. Captions CC
    EditorToolbarItem(
      icon = Icons.Default.ClosedCaption,
      label = "Captions",
      isSelected = activeTab == EditorToolbarTab.CAPTIONS,
      testTag = "captions_btn",
      onClick = { onTabSelected(EditorToolbarTab.CAPTIONS) }
    )

    // 7. Filters 🎨
    EditorToolbarItem(
      icon = Icons.Default.ColorLens,
      label = "Filters",
      isSelected = activeTab == EditorToolbarTab.FILTERS,
      testTag = "filters_btn",
      onClick = { onTabSelected(EditorToolbarTab.FILTERS) }
    )

    // 8. Adjust ◯ with sliders
    EditorToolbarItem(
      icon = Icons.Default.Tune,
      label = "Adjust",
      isSelected = activeTab == EditorToolbarTab.ADJUST,
      testTag = "adjust_btn",
      onClick = { onTabSelected(EditorToolbarTab.ADJUST) }
    )

    // 9. Stickers ⭕
    EditorToolbarItem(
      icon = Icons.Default.EmojiEmotions,
      label = "Stickers",
      isSelected = activeTab == EditorToolbarTab.STICKERS,
      testTag = "stickers_btn",
      onClick = { onTabSelected(EditorToolbarTab.STICKERS) }
    )

    // 10. Generate media ⊕ (AI tag)
    EditorToolbarItem(
      icon = Icons.Default.VideoLibrary,
      label = "Generate media",
      isSelected = activeTab == EditorToolbarTab.AI,
      testTag = "generate_media_btn",
      badgeText = "AI",
      onClick = { onTabSelected(EditorToolbarTab.AI) }
    )

    // 11. AI avatar 👤 (Purple diamond tag)
    EditorToolbarItem(
      icon = Icons.Default.AccountBox,
      label = "AI avatar",
      isSelected = activeTab == EditorToolbarTab.AI_AVATAR,
      testTag = "ai_avatar_btn",
      badgeIcon = Icons.Default.Diamond,
      onClick = { onTabSelected(EditorToolbarTab.AI_AVATAR) }
    )

    // 12. Aspect ratio □
    EditorToolbarItem(
      icon = Icons.Default.CropSquare,
      label = "Aspect ratio",
      isSelected = activeTab == EditorToolbarTab.CANVAS,
      testTag = "aspect_ratio_btn",
      onClick = { onTabSelected(EditorToolbarTab.CANVAS) }
    )

    // 13. Background ▨
    EditorToolbarItem(
      icon = Icons.Default.Texture,
      label = "Background",
      isSelected = activeTab == EditorToolbarTab.BACKGROUND,
      testTag = "background_btn",
      onClick = { onTabSelected(EditorToolbarTab.BACKGROUND) }
    )

    // 14. Speed ⚡
    EditorToolbarItem(
      icon = Icons.Default.Speed,
      label = "Speed",
      isSelected = activeTab == EditorToolbarTab.SPEED,
      testTag = "speed_btn",
      onClick = { onTabSelected(EditorToolbarTab.SPEED) }
    )

    // 15. Transitions 🔄
    EditorToolbarItem(
      icon = Icons.Default.Transform,
      label = "Transitions",
      isSelected = activeTab == EditorToolbarTab.TRANSITIONS,
      testTag = "transitions_btn",
      onClick = { onTabSelected(EditorToolbarTab.TRANSITIONS) }
    )

    // 16. Chroma Key 🟩
    EditorToolbarItem(
      icon = Icons.Default.FilterFrames,
      label = "Chroma",
      isSelected = activeTab == EditorToolbarTab.CHROMA,
      testTag = "chroma_btn",
      onClick = { onTabSelected(EditorToolbarTab.CHROMA) }
    )

    // 17. Keyframe 💎
    EditorToolbarItem(
      icon = Icons.Default.Diamond,
      label = "Keyframe",
      isSelected = activeTab == EditorToolbarTab.KEYFRAME,
      testTag = "keyframe_btn",
      onClick = { onTabSelected(EditorToolbarTab.KEYFRAME) }
    )

    // 18. Media / Add ➕
    EditorToolbarItem(
      icon = Icons.Default.AddPhotoAlternate,
      label = "Media",
      isSelected = activeTab == EditorToolbarTab.MEDIA,
      testTag = "add_btn",
      onClick = { onTabSelected(EditorToolbarTab.MEDIA) }
    )
  }
}

@Composable
private fun EditorToolbarItem(
  icon: ImageVector,
  label: String,
  isSelected: Boolean,
  testTag: String,
  badgeText: String? = null,
  badgeIcon: ImageVector? = null,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .defaultMinSize(minWidth = 62.dp, minHeight = 56.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .background(if (isSelected) CyanAccent.copy(alpha = 0.2f) else Color.Transparent)
      .padding(horizontal = 6.dp, vertical = 4.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(contentAlignment = Alignment.TopEnd) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (isSelected) CyanAccent else Color.White,
          modifier = Modifier.size(24.dp)
        )
        if (badgeText != null) {
          Surface(
            color = CyanAccent,
            shape = RoundedCornerShape(3.dp),
            modifier = Modifier.offset(x = 10.dp, y = (-5).dp)
          ) {
            Text(
              text = badgeText,
              color = Color.Black,
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
            )
          }
        } else if (badgeIcon != null) {
          Icon(
            imageVector = badgeIcon,
            contentDescription = null,
            tint = PurpleAccent,
            modifier = Modifier
              .size(10.dp)
              .offset(x = 8.dp, y = (-4).dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 11.sp,
          color = if (isSelected) CyanAccent else Color(0xFFE2E2E2),
          fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        ),
        maxLines = 1
      )
    }
  }
}

private fun formatDurationShort(timeMs: Long): String {
  val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return String.format("%02d:%02d", minutes, seconds)
}

@Composable
private fun SyntheticClipPreview(
  clip: VideoClip,
  currentPosMs: Long,
  modifier: Modifier = Modifier
) {
  val stockItem = remember(clip.uri) {
    val stockId = clip.uri.removePrefix("stock://")
    StockMediaCatalog.stockItems.find { it.id == stockId }
  }

  val (startColor, endColor, iconEmoji) = remember(clip.id, clip.name, stockItem) {
    if (stockItem != null) {
      Triple(Color(stockItem.gradientStart), Color(stockItem.gradientEnd), stockItem.iconEmoji)
    } else if (clip.name.contains("Mountain", ignoreCase = true) || clip.name.contains("Stream", ignoreCase = true) || clip.uri.contains("nature", ignoreCase = true)) {
      Triple(Color(0xFF0077B6), Color(0xFF00B4D8), "🏔️")
    } else if (clip.name.contains("Skyline", ignoreCase = true) || clip.name.contains("Sunset", ignoreCase = true) || clip.name.contains("Golden", ignoreCase = true) || clip.uri.contains("urban", ignoreCase = true)) {
      Triple(Color(0xFFE85D04), Color(0xFF7209B7), "🌇")
    } else {
      Triple(Color(0xFF1E293B), Color(0xFF0F172A), "🎬")
    }
  }

  val relativeClipPosMs = (currentPosMs - clip.timelineStartMs).coerceIn(0L, clip.durationMs)
  val progress = if (clip.durationMs > 0) relativeClipPosMs.toFloat() / clip.durationMs else 0f

  Box(
    modifier = modifier
      .background(Brush.linearGradient(listOf(startColor, endColor)))
      .drawBehind {
        val scanY = size.height * ((progress * 3f) % 1f)
        drawLine(
          color = Color.White.copy(alpha = 0.08f),
          start = Offset(0f, scanY),
          end = Offset(size.width, scanY),
          strokeWidth = 3f
        )
      },
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(16.dp)
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.35f))
          .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(text = iconEmoji, fontSize = 24.sp)
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = clip.name,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(4.dp))
      Surface(
        color = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(
          text = "${formatDurationShort(relativeClipPosMs)} / ${formatDurationShort(clip.durationMs)}",
          color = CyanAccent,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
    }
  }
}
