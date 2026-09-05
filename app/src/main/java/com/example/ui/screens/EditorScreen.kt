package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.domain.model.*
import com.example.engine.KeyframeInterpolator
import com.example.engine.SelectedTrackElement
import com.example.ui.AppScreen
import com.example.ui.EditorToolbarTab
import com.example.ui.StudioViewModel
import com.example.ui.components.formatDuration
import com.example.ui.components.formatDurationShort
import com.example.ui.components.timeline.*
import com.example.ui.theme.*
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
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

  var showRenameDialog by remember { mutableStateOf(false) }
  var showSpeedDialog by remember { mutableStateOf(false) }
  var pendingReplaceClipId by remember { mutableStateOf<String?>(null) }

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

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(StudioDarkBg),
    containerColor = StudioDarkBg,
    topBar = {
      EditorTopBar(
        projectName = projectName,
        canUndo = canUndo,
        canRedo = canRedo,
        isSnapping = isSnapping,
        onBackClick = {
          viewModel.saveCurrentProject()
          viewModel.navigateTo(AppScreen.HOME)
        },
        onRenameClick = { showRenameDialog = true },
        onUndoClick = { viewModel.timelineEngine.undo() },
        onRedoClick = { viewModel.timelineEngine.redo() },
        onToggleSnapping = { viewModel.timelineEngine.toggleSnapping() },
        onExportClick = {
          viewModel.saveCurrentProject()
          viewModel.navigateTo(AppScreen.EXPORT)
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
            EditorToolbarTab.ADJUST -> AdjustToolPanel(viewModel)
            EditorToolbarTab.SPEED -> SpeedToolPanel(viewModel)
            EditorToolbarTab.FILTERS -> FiltersToolPanel(viewModel)
            EditorToolbarTab.EFFECTS -> EffectsToolPanel(viewModel)
            EditorToolbarTab.TRANSITIONS -> TransitionsToolPanel(viewModel)
            EditorToolbarTab.TEXT -> TextEditorPanel(viewModel)
            EditorToolbarTab.AUDIO -> AudioToolPanel(viewModel)
            EditorToolbarTab.STICKERS -> StickersToolPanel(viewModel)
            EditorToolbarTab.CHROMA -> ChromaKeyPanel(viewModel)
            EditorToolbarTab.CANVAS -> CanvasPanel(viewModel)
            EditorToolbarTab.AI -> {
              // Quick AI trigger
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(StudioSurface)
                  .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("AI Tools", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                  IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                  }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(
                    onClick = {
                      viewModel.runAIAutoCaptions()
                      viewModel.setActiveToolbarTab(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                  ) {
                    Text("Auto Captions")
                  }
                  Button(
                    onClick = {
                      viewModel.runAIAutoEdit()
                      viewModel.setActiveToolbarTab(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = Color.White)
                  ) {
                    Text("Auto Edit Reel")
                  }
                }
              }
            }
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
      // 1. Video Preview Player (Aspect ratio preserved)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(8.dp),
        contentAlignment = Alignment.Center
      ) {
        VideoPreviewSurface(
          timeline = timeline,
          currentPosMs = currentPosMs,
          aspectRatio = aspectRatio,
          selectedElement = selectedElement,
          onSelectElement = { viewModel.timelineEngine.selectElement(it) },
          player = viewModel.playbackEngine.player,
          modifier = Modifier.fillMaxSize()
        )
      }

      // 2. Timeline Control Toolbar (Play/Pause, Step, Split, Delete, Keyframe)
      TimelineControlsBar(
        isPlaying = isPlaying,
        currentPosMs = currentPosMs,
        totalDurationMs = timeline.totalDurationMs,
        zoom = timelineZoom,
        onTogglePlay = { viewModel.timelineEngine.togglePlayPause() },
        onStop = { viewModel.timelineEngine.stop() },
        onStepBack = { viewModel.timelineEngine.stepBackwardOneFrame() },
        onStepForward = { viewModel.timelineEngine.stepForwardOneFrame() },
        onSplit = { viewModel.timelineEngine.splitSelectedClipAtPlayhead() },
        onDelete = { viewModel.timelineEngine.deleteSelected() },
        onAddKeyframe = { viewModel.timelineEngine.addKeyframeToSelectedClip() },
        onAddMedia = { viewModel.setActiveToolbarTab(EditorToolbarTab.MEDIA) },
        onZoomChange = { viewModel.timelineEngine.setZoom(it) }
      )

      // 3. Timeline Quick Action Toolbar
      TimelineActionToolbar(
        hasSelection = selectedElement !is SelectedTrackElement.None || selectedClipIds.isNotEmpty(),
        isMultiSelectMode = isMultiSelectMode,
        selectedCount = selectedClipIds.size,
        canPaste = clipboardClips.isNotEmpty(),
        isMagnetic = isMagnetic,
        onSplit = { viewModel.timelineEngine.splitAtPlayhead() },
        onTrimLeft = {
          val activeClipId = (selectedElement as? SelectedTrackElement.Video)?.clipId
            ?: (selectedElement as? SelectedTrackElement.Overlay)?.clipId
            ?: (selectedElement as? SelectedTrackElement.Audio)?.clipId
            ?: (selectedElement as? SelectedTrackElement.Text)?.clipId
            ?: (selectedElement as? SelectedTrackElement.Sticker)?.clipId
            ?: (selectedElement as? SelectedTrackElement.Effect)?.clipId
            ?: selectedClipIds.firstOrNull()
          if (activeClipId != null) {
            viewModel.timelineEngine.trimClipLeft(activeClipId, currentPosMs)
          }
        },
        onTrimRight = {
          val activeClipId = (selectedElement as? SelectedTrackElement.Video)?.clipId
            ?: (selectedElement as? SelectedTrackElement.Overlay)?.clipId
            ?: (selectedElement as? SelectedTrackElement.Audio)?.clipId
            ?: (selectedElement as? SelectedTrackElement.Text)?.clipId
            ?: (selectedElement as? SelectedTrackElement.Sticker)?.clipId
            ?: (selectedElement as? SelectedTrackElement.Effect)?.clipId
            ?: selectedClipIds.firstOrNull()
          if (activeClipId != null) {
            val clipStart = when (val el = viewModel.timelineEngine.findTrackElementForClip(activeClipId)) {
              is SelectedTrackElement.Video -> timeline.videoClips.find { c -> c.id == activeClipId }?.timelineStartMs
              is SelectedTrackElement.Overlay -> timeline.overlayClips.find { c -> c.id == activeClipId }?.timelineStartMs
              is SelectedTrackElement.Audio -> timeline.audioClips.find { c -> c.id == activeClipId }?.timelineStartMs
              is SelectedTrackElement.Text -> timeline.textClips.find { c -> c.id == activeClipId }?.timelineStartMs
              is SelectedTrackElement.Sticker -> timeline.stickerClips.find { c -> c.id == activeClipId }?.timelineStartMs
              is SelectedTrackElement.Effect -> timeline.effectClips.find { c -> c.id == activeClipId }?.timelineStartMs
              else -> null
            } ?: 0L
            val newDuration = (currentPosMs - clipStart).coerceAtLeast(100L)
            viewModel.timelineEngine.trimClipRight(activeClipId, newDuration)
          }
        },
        onRippleDelete = { viewModel.timelineEngine.rippleDelete() },
        onNormalDelete = { viewModel.timelineEngine.normalDelete() },
        onDuplicate = { viewModel.timelineEngine.duplicateClips() },
        onCopy = { viewModel.timelineEngine.copySelectedClips() },
        onPaste = { viewModel.timelineEngine.pasteClipsAtPlayhead() },
        onSpeedClick = { showSpeedDialog = true },
        onReverse = { viewModel.timelineEngine.toggleReverseSelectedClip() },
        onFreezeFrame = { viewModel.timelineEngine.freezeFrameAtPlayhead() },
        onReplaceMedia = {
          val activeClipId = (selectedElement as? SelectedTrackElement.Video)?.clipId
            ?: (selectedElement as? SelectedTrackElement.Overlay)?.clipId
            ?: selectedClipIds.firstOrNull()
          if (activeClipId != null) {
            pendingReplaceClipId = activeClipId
            replaceMediaPickerLauncher.launch(
              PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageAndVideo
              )
            )
          }
        },
        onToggleMultiSelect = { viewModel.timelineEngine.toggleMultiSelectMode() },
        onToggleMagnetic = { viewModel.timelineEngine.toggleMagneticMovement() }
      )

      // 4. Professional Multi-Track Timeline Canvas
      MultiTrackTimeline(
        timeline = timeline,
        currentPosMs = currentPosMs,
        zoom = timelineZoom,
        selectedElement = selectedElement,
        selectedClipIds = selectedClipIds,
        isMultiSelectMode = isMultiSelectMode,
        snapIndicatorMs = snapIndicatorMs,
        onSeek = { viewModel.timelineEngine.setPosition(it) },
        onSelectElement = { viewModel.timelineEngine.selectElement(it) },
        onToggleClipSelection = { viewModel.timelineEngine.toggleSelectClip(it) },
        onZoomChange = { viewModel.timelineEngine.setZoom(it) },
        onMoveClip = { clipId, deltaMs ->
          val currentStart = when (viewModel.timelineEngine.findTrackElementForClip(clipId)) {
            is SelectedTrackElement.Video -> timeline.videoClips.find { it.id == clipId }?.timelineStartMs
            is SelectedTrackElement.Overlay -> timeline.overlayClips.find { it.id == clipId }?.timelineStartMs
            is SelectedTrackElement.Audio -> timeline.audioClips.find { it.id == clipId }?.timelineStartMs
            is SelectedTrackElement.Text -> timeline.textClips.find { it.id == clipId }?.timelineStartMs
            is SelectedTrackElement.Sticker -> timeline.stickerClips.find { it.id == clipId }?.timelineStartMs
            is SelectedTrackElement.Effect -> timeline.effectClips.find { it.id == clipId }?.timelineStartMs
            else -> null
          } ?: 0L
          viewModel.timelineEngine.moveClip(clipId, currentStart + deltaMs)
        },
        onTrimClipLeft = { clipId, deltaMs ->
          val currentStart = when (viewModel.timelineEngine.findTrackElementForClip(clipId)) {
            is SelectedTrackElement.Video -> timeline.videoClips.find { it.id == clipId }?.timelineStartMs
            is SelectedTrackElement.Overlay -> timeline.overlayClips.find { it.id == clipId }?.timelineStartMs
            is SelectedTrackElement.Audio -> timeline.audioClips.find { it.id == clipId }?.timelineStartMs
            is SelectedTrackElement.Text -> timeline.textClips.find { it.id == clipId }?.timelineStartMs
            is SelectedTrackElement.Sticker -> timeline.stickerClips.find { it.id == clipId }?.timelineStartMs
            is SelectedTrackElement.Effect -> timeline.effectClips.find { it.id == clipId }?.timelineStartMs
            else -> null
          } ?: 0L
          viewModel.timelineEngine.trimClipLeft(clipId, currentStart + deltaMs)
        },
        onTrimClipRight = { clipId, deltaMs ->
          val currentDur = when (viewModel.timelineEngine.findTrackElementForClip(clipId)) {
            is SelectedTrackElement.Video -> timeline.videoClips.find { it.id == clipId }?.durationMs
            is SelectedTrackElement.Overlay -> timeline.overlayClips.find { it.id == clipId }?.durationMs
            is SelectedTrackElement.Audio -> timeline.audioClips.find { it.id == clipId }?.durationMs
            is SelectedTrackElement.Text -> timeline.textClips.find { it.id == clipId }?.durationMs
            is SelectedTrackElement.Sticker -> timeline.stickerClips.find { it.id == clipId }?.durationMs
            is SelectedTrackElement.Effect -> timeline.effectClips.find { it.id == clipId }?.durationMs
            else -> null
          } ?: 1000L
          viewModel.timelineEngine.trimClipRight(clipId, currentDur + deltaMs)
        },
        onToggleTrackLock = { viewModel.timelineEngine.toggleTrackLock(it) },
        onToggleTrackHide = { viewModel.timelineEngine.toggleTrackHide(it) },
        onToggleTrackMute = { viewModel.timelineEngine.toggleTrackMute(it) },
        onToggleTrackSolo = { viewModel.timelineEngine.toggleTrackSolo(it) },
        onCycleTrackHeight = { viewModel.timelineEngine.cycleTrackHeight(it) },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1.1f)
      )
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
}

@Composable
private fun EditorTopBar(
  projectName: String,
  canUndo: Boolean,
  canRedo: Boolean,
  isSnapping: Boolean,
  onBackClick: () -> Unit,
  onRenameClick: () -> Unit,
  onUndoClick: () -> Unit,
  onRedoClick: () -> Unit,
  onToggleSnapping: () -> Unit,
  onExportClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(StudioDarkBg)
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onBackClick,
        modifier = Modifier.testTag("editor_back_button")
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
      }
      Row(
        modifier = Modifier
          .clickable(onClick = onRenameClick)
          .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = projectName,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp),
          maxLines = 1
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TextTertiary, modifier = Modifier.size(14.dp))
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      // Snapping indicator
      IconButton(
        onClick = onToggleSnapping,
        modifier = Modifier.testTag("editor_snapping_button")
      ) {
        Icon(
          Icons.Default.Adjust,
          contentDescription = "Snapping",
          tint = if (isSnapping) CyanAccent else TextTertiary
        )
      }

      // Undo
      IconButton(
        onClick = onUndoClick,
        enabled = canUndo,
        modifier = Modifier.testTag("editor_undo_button")
      ) {
        Icon(
          Icons.AutoMirrored.Filled.Undo,
          contentDescription = "Undo",
          tint = if (canUndo) TextPrimary else TextTertiary.copy(alpha = 0.4f)
        )
      }

      // Redo
      IconButton(
        onClick = onRedoClick,
        enabled = canRedo,
        modifier = Modifier.testTag("editor_redo_button")
      ) {
        Icon(
          Icons.AutoMirrored.Filled.Redo,
          contentDescription = "Redo",
          tint = if (canRedo) TextPrimary else TextTertiary.copy(alpha = 0.4f)
        )
      }

      Spacer(modifier = Modifier.width(4.dp))

      // Export Button
      Button(
        onClick = onExportClick,
        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier
          .height(34.dp)
          .testTag("editor_export_button")
      ) {
        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Export", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
  player: ExoPlayer? = null,
  modifier: Modifier = Modifier
) {
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
    ) {
      // Background Video / Image Layer
      if (activeClip != null) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .scale(clipTransform?.scale ?: 1f)
            .rotate(clipTransform?.rotation ?: 0f),
          contentAlignment = Alignment.Center
        ) {
          if (activeClip.isVideo && player != null) {
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
          } else {
            AsyncImage(
              model = activeClip.uri,
              contentDescription = activeClip.name,
              contentScale = ContentScale.Fit,
              colorFilter = combinedColorFilter,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("Empty Timeline", color = TextTertiary)
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

      // Active Stickers
      activeStickers.forEach { sticker ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(sticker.emojiOrAsset, fontSize = 42.sp)
        }
      }

      // Active Overlays (Picture-in-Picture / Floating Media with Keyframe Interpolation)
      activeOverlays.forEach { overlay ->
        val isOverlaySelected = selectedElement is SelectedTrackElement.Overlay &&
          (selectedElement as SelectedTrackElement.Overlay).clipId == overlay.id

        val relTime = currentPosMs - overlay.timelineStartMs
        val kf = com.example.engine.KeyframeInterpolator.interpolate(overlay, relTime)

        Box(
          modifier = Modifier
            .align(Alignment.Center)
            .offset(
              x = (kf.posX * 140).dp,
              y = (kf.posY * 140).dp
            )
            .scale(kf.scale)
            .rotate(kf.rotation)
            .alpha(kf.opacity)
            .width(160.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .border(
              width = if (isOverlaySelected) 2.dp else 1.dp,
              color = if (isOverlaySelected) AmberAccent else Color.White.copy(alpha = 0.5f),
              shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelectElement(SelectedTrackElement.Overlay(overlay.id)) }
        ) {
          AsyncImage(
            model = overlay.uri,
            contentDescription = overlay.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )

          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(AmberAccent)
                  .padding(horizontal = 4.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "PIP",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                  )
                )
              }
              Icon(
                if (overlay.isVideo) Icons.Default.Movie else Icons.Default.Image,
                contentDescription = null,
                tint = AmberAccent,
                modifier = Modifier.size(14.dp)
              )
            }
            Text(
              text = overlay.name,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // Active Text Overlays (Subtitles & Titles with Intro Animations)
      activeTexts.forEach { textClip ->
        val relTime = (currentPosMs - textClip.timelineStartMs).coerceAtLeast(0L)
        val animFactor = if (relTime < textClip.animDurationMs) {
          (relTime.toFloat() / textClip.animDurationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
        } else 1f

        val (animScale, animAlpha) = when (textClip.animationType.lowercase()) {
          "pop" -> Pair(0.4f + 0.6f * animFactor, animFactor)
          "zoom" -> Pair(0.2f + 0.8f * animFactor, animFactor)
          "fade" -> Pair(1f, animFactor)
          "slide" -> Pair(1f, animFactor)
          "bounce" -> Pair(0.5f + 0.5f * animFactor, animFactor)
          "none" -> Pair(1f, 1f)
          else -> Pair(1f, animFactor)
        }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .offset(
                x = (textClip.posX * 120).dp,
                y = (textClip.posY * 160).dp
              )
              .rotate(textClip.rotation)
              .scale(textClip.scale * animScale)
              .alpha(animAlpha)
              .background(
                if (textClip.hasBackground) Color(textClip.backgroundColor) else Color.Transparent,
                RoundedCornerShape(8.dp)
              )
              .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = textClip.text,
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight(textClip.fontWeight),
                fontSize = textClip.fontSizeSp.sp,
                color = Color(textClip.textColor),
                textAlign = TextAlign.Center
              )
            )
          }
        }
      }

      // Top-right Timecode overlay
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color.Black.copy(alpha = 0.7f))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "${formatDuration(currentPosMs)} / ${formatDurationShort(timeline.totalDurationMs)}",
          style = MaterialTheme.typography.labelSmall.copy(
            color = CyanAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
        )
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
  onTogglePlay: () -> Unit,
  onStop: () -> Unit,
  onStepBack: () -> Unit,
  onStepForward: () -> Unit,
  onSplit: () -> Unit,
  onDelete: () -> Unit,
  onAddKeyframe: () -> Unit,
  onAddMedia: () -> Unit,
  onZoomChange: (Float) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    // Left: Playback controls
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onTogglePlay,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(CyanAccent)
          .testTag("timeline_play_pause")
      ) {
        Icon(
          if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = "Play/Pause",
          tint = Color.Black
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = TextSecondary)
      }

      IconButton(onClick = onStepBack, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Default.SkipPrevious, contentDescription = "-1 Frame", tint = TextSecondary)
      }

      IconButton(onClick = onStepForward, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Default.SkipNext, contentDescription = "+1 Frame", tint = TextSecondary)
      }

      IconButton(
        onClick = onAddMedia,
        modifier = Modifier
          .size(36.dp)
          .testTag("timeline_add_media")
      ) {
        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Media", tint = CyanAccent)
      }
    }

    // Right: Action shortcuts (Split, Keyframe, Delete, Zoom)
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onSplit, modifier = Modifier.size(36.dp).testTag("timeline_quick_split")) {
        Icon(Icons.Default.CallSplit, contentDescription = "Split", tint = CyanAccent)
      }

      IconButton(onClick = onAddKeyframe, modifier = Modifier.size(36.dp).testTag("timeline_quick_keyframe")) {
        Icon(Icons.Default.Diamond, contentDescription = "Keyframe", tint = PurpleAccent)
      }

      IconButton(onClick = onDelete, modifier = Modifier.size(36.dp).testTag("timeline_quick_delete")) {
        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedAccent)
      }
    }
  }
}

@Composable
private fun EditorBottomToolbar(
  activeTab: EditorToolbarTab?,
  onTabSelected: (EditorToolbarTab) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .border(1.dp, StudioBorder)
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 8.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    EditorTabItem(icon = Icons.Default.VideoLibrary, label = "Media", isSelected = activeTab == EditorToolbarTab.MEDIA) {
      onTabSelected(EditorToolbarTab.MEDIA)
    }
    EditorTabItem(icon = Icons.Default.Layers, label = "Overlay", isSelected = activeTab == EditorToolbarTab.OVERLAY) {
      onTabSelected(EditorToolbarTab.OVERLAY)
    }
    EditorTabItem(icon = Icons.Default.Edit, label = "Edit", isSelected = activeTab == EditorToolbarTab.EDIT) {
      onTabSelected(EditorToolbarTab.EDIT)
    }
    EditorTabItem(icon = Icons.Default.Audiotrack, label = "Audio", isSelected = activeTab == EditorToolbarTab.AUDIO) {
      onTabSelected(EditorToolbarTab.AUDIO)
    }
    EditorTabItem(icon = Icons.Default.TextFields, label = "Text", isSelected = activeTab == EditorToolbarTab.TEXT) {
      onTabSelected(EditorToolbarTab.TEXT)
    }
    EditorTabItem(icon = Icons.Default.AutoAwesome, label = "AI Suite", isSelected = activeTab == EditorToolbarTab.AI) {
      onTabSelected(EditorToolbarTab.AI)
    }
    EditorTabItem(icon = Icons.Default.FilterVintage, label = "Filters", isSelected = activeTab == EditorToolbarTab.FILTERS) {
      onTabSelected(EditorToolbarTab.FILTERS)
    }
    EditorTabItem(icon = Icons.Default.AutoFixHigh, label = "Effects", isSelected = activeTab == EditorToolbarTab.EFFECTS) {
      onTabSelected(EditorToolbarTab.EFFECTS)
    }
    EditorTabItem(icon = Icons.Default.Transform, label = "Transitions", isSelected = activeTab == EditorToolbarTab.TRANSITIONS) {
      onTabSelected(EditorToolbarTab.TRANSITIONS)
    }
    EditorTabItem(icon = Icons.Default.Tune, label = "Adjust", isSelected = activeTab == EditorToolbarTab.ADJUST) {
      onTabSelected(EditorToolbarTab.ADJUST)
    }
    EditorTabItem(icon = Icons.Default.Speed, label = "Speed", isSelected = activeTab == EditorToolbarTab.SPEED) {
      onTabSelected(EditorToolbarTab.SPEED)
    }
    EditorTabItem(icon = Icons.Default.EmojiEmotions, label = "Stickers", isSelected = activeTab == EditorToolbarTab.STICKERS) {
      onTabSelected(EditorToolbarTab.STICKERS)
    }
    EditorTabItem(icon = Icons.Default.ColorLens, label = "Chroma", isSelected = activeTab == EditorToolbarTab.CHROMA) {
      onTabSelected(EditorToolbarTab.CHROMA)
    }
    EditorTabItem(icon = Icons.Default.AspectRatio, label = "Canvas", isSelected = activeTab == EditorToolbarTab.CANVAS) {
      onTabSelected(EditorToolbarTab.CANVAS)
    }
  }
}

@Composable
private fun EditorTabItem(
  icon: ImageVector,
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .background(if (isSelected) StudioSurfaceVariant else Color.Transparent)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isSelected) CyanAccent else TextSecondary,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 10.sp,
          color = if (isSelected) CyanAccent else TextSecondary,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
      )
    }
  }
}
