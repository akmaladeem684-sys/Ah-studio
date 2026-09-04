package com.example.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.engine.KeyframeInterpolator
import com.example.engine.SelectedTrackElement
import com.example.ui.AppScreen
import com.example.ui.EditorToolbarTab
import com.example.ui.StudioViewModel
import com.example.ui.components.formatDuration
import com.example.ui.components.formatDurationShort
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

  var showRenameDialog by remember { mutableStateOf(false) }

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

      // 3. Multi-Track Timeline Canvas
      MultiTrackTimeline(
        timeline = timeline,
        currentPosMs = currentPosMs,
        zoom = timelineZoom,
        selectedElement = selectedElement,
        onSeek = { viewModel.timelineEngine.setPosition(it) },
        onSelectElement = { viewModel.timelineEngine.selectElement(it) },
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
      )
    }
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

  // Color Matrix for video adjustments
  val adjustments = timeline.adjustments
  val colorMatrix = remember(adjustments) {
    val cm = ColorMatrix()
    cm.setToSaturation(adjustments.saturation)
    cm
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
      // Background Image / Gradient Layer
      if (activeClip != null) {
        val clipBgColor = remember(activeClip.id) {
          when (activeClip.id.hashCode() % 4) {
            0 -> Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)))
            1 -> Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA)))
            2 -> Brush.linearGradient(listOf(Color(0xFF14532D), Color(0xFF166534), Color(0xFF15803D)))
            else -> Brush.linearGradient(listOf(Color(0xFF7C2D12), Color(0xFF9A3412), Color(0xFFC2410C)))
          }
        }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .scale(clipTransform?.scale ?: 1f)
            .rotate(clipTransform?.rotation ?: 0f)
            .background(clipBgColor),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              Icons.Default.Movie,
              contentDescription = null,
              tint = CyanAccent.copy(alpha = 0.8f),
              modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = activeClip.name,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 15.sp
              )
            )
            Text(
              text = "Speed: ${activeClip.speed}x • Frame: ${(currentPosMs / 33)}",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
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

      // Active Overlays (Picture-in-Picture / Floating Media)
      activeOverlays.forEach { overlay ->
        val isOverlaySelected = selectedElement is SelectedTrackElement.Overlay &&
          (selectedElement as SelectedTrackElement.Overlay).clipId == overlay.id

        Box(
          modifier = Modifier
            .align(Alignment.Center)
            .offset(
              x = (overlay.cropOffsetX * 140).dp,
              y = (overlay.cropOffsetY * 140).dp
            )
            .scale(overlay.cropScale)
            .rotate(overlay.rotationDegrees.toFloat())
            .width(160.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
              Brush.linearGradient(
                listOf(
                  Color(0xFF1E1B4B),
                  Color(0xFF312E81),
                  Color(0xFF4338CA)
                )
              )
            )
            .border(
              width = if (isOverlaySelected) 2.dp else 1.dp,
              color = if (isOverlaySelected) AmberAccent else Color.White.copy(alpha = 0.5f),
              shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelectElement(SelectedTrackElement.Overlay(overlay.id)) }
            .padding(6.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxSize(),
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
              Text(
                text = overlay.name,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Opacity: ${(overlay.opacity * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  color = TextSecondary
                )
              )
            }
          }
        }
      }

      // Active Text Overlays (Subtitles & Titles)
      activeTexts.forEach { textClip ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
          contentAlignment = Alignment.BottomCenter
        ) {
          Text(
            text = textClip.text,
            style = MaterialTheme.typography.headlineSmall.copy(
              fontWeight = FontWeight(textClip.fontWeight),
              fontSize = textClip.fontSizeSp.sp,
              color = Color(textClip.textColor),
              textAlign = TextAlign.Center
            ),
            modifier = Modifier
              .background(Color(textClip.backgroundColor), RoundedCornerShape(8.dp))
              .padding(horizontal = 12.dp, vertical = 6.dp)
          )
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
private fun MultiTrackTimeline(
  timeline: Timeline,
  currentPosMs: Long,
  zoom: Float,
  selectedElement: SelectedTrackElement,
  onSeek: (Long) -> Unit,
  onSelectElement: (SelectedTrackElement) -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  val totalDuration = timeline.totalDurationMs.coerceAtLeast(6000L)
  val msPerPixel = remember(zoom) { 20f / zoom } // 20ms per pixel base
  val totalWidthPx = remember(totalDuration, msPerPixel) { (totalDuration / msPerPixel).toInt() }

  Box(
    modifier = modifier
      .background(StudioDarkBg)
      .border(1.dp, StudioBorder)
  ) {
    // Scrollable Tracks Canvas
    Column(
      modifier = Modifier
        .fillMaxSize()
        .horizontalScroll(scrollState)
        .pointerInput(totalDuration, msPerPixel) {
          detectTapGestures { offset ->
            val clickedMs = (offset.x * msPerPixel).toLong().coerceIn(0L, totalDuration)
            onSeek(clickedMs)
          }
        }
    ) {
      // 1. Timecode Ruler
      TimecodeRuler(
        totalDurationMs = totalDuration,
        msPerPixel = msPerPixel,
        modifier = Modifier
          .width((totalDuration / msPerPixel).dp)
          .height(26.dp)
      )

      // 2. Video / Main Track
      TrackRow(
        title = "Video",
        icon = Icons.Default.Movie,
        accentColor = CyanAccent,
        modifier = Modifier.width((totalDuration / msPerPixel).dp)
      ) {
        timeline.videoClips.forEach { clip ->
          val startPx = (clip.timelineStartMs / msPerPixel).dp
          val widthPx = (clip.durationMs / msPerPixel).dp
          val isSelected = selectedElement is SelectedTrackElement.Video &&
            (selectedElement as SelectedTrackElement.Video).clipId == clip.id

          Box(
            modifier = Modifier
              .offset(x = startPx)
              .width(widthPx)
              .height(44.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) StudioSurfaceVariant else Color(0xFF1E293B))
              .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) CyanAccent else StudioBorder,
                RoundedCornerShape(8.dp)
              )
              .clickable { onSelectElement(SelectedTrackElement.Video(clip.id)) }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxSize(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = clip.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                maxLines = 1
              )
              Text(
                text = formatDurationShort(clip.durationMs),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = CyanAccent)
              )
            }
          }
        }
      }

      // 3. Overlay (PIP) Track
      TrackRow(
        title = "Overlay",
        icon = Icons.Default.Layers,
        accentColor = AmberAccent,
        modifier = Modifier.width((totalDuration / msPerPixel).dp)
      ) {
        timeline.overlayClips.forEach { clip ->
          val startPx = (clip.timelineStartMs / msPerPixel).dp
          val widthPx = (clip.durationMs / msPerPixel).dp
          val isSelected = selectedElement is SelectedTrackElement.Overlay &&
            (selectedElement as SelectedTrackElement.Overlay).clipId == clip.id

          Box(
            modifier = Modifier
              .offset(x = startPx)
              .width(widthPx)
              .height(38.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) Color(0xFF78350F) else Color(0xFF451A03))
              .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) AmberAccent else Color(0xFFB45309),
                RoundedCornerShape(6.dp)
              )
              .clickable { onSelectElement(SelectedTrackElement.Overlay(clip.id)) }
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxSize(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  if (clip.isVideo) Icons.Default.Movie else Icons.Default.Image,
                  contentDescription = null,
                  tint = AmberAccent,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = clip.name,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Text(
                text = formatDurationShort(clip.durationMs),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = AmberAccent)
              )
            }
          }
        }
      }

      // 4. Text / Subtitle Track
      TrackRow(
        title = "Text",
        icon = Icons.Default.TextFields,
        accentColor = PurpleAccent,
        modifier = Modifier.width((totalDuration / msPerPixel).dp)
      ) {
        timeline.textClips.forEach { clip ->
          val startPx = (clip.timelineStartMs / msPerPixel).dp
          val widthPx = (clip.durationMs / msPerPixel).dp
          val isSelected = selectedElement is SelectedTrackElement.Text &&
            (selectedElement as SelectedTrackElement.Text).clipId == clip.id

          Box(
            modifier = Modifier
              .offset(x = startPx)
              .width(widthPx)
              .height(34.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF312E81))
              .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) PurpleAccent else Color.Transparent,
                RoundedCornerShape(6.dp)
              )
              .clickable { onSelectElement(SelectedTrackElement.Text(clip.id)) }
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = clip.text,
              style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
              maxLines = 1
            )
          }
        }
      }

      // 4. Audio Track (with Waveform!)
      TrackRow(
        title = "Audio",
        icon = Icons.Default.Audiotrack,
        accentColor = GreenAccent,
        modifier = Modifier.width((totalDuration / msPerPixel).dp)
      ) {
        timeline.audioClips.forEach { clip ->
          val startPx = (clip.timelineStartMs / msPerPixel).dp
          val widthPx = (clip.durationMs / msPerPixel).dp
          val isSelected = selectedElement is SelectedTrackElement.Audio &&
            (selectedElement as SelectedTrackElement.Audio).clipId == clip.id

          Box(
            modifier = Modifier
              .offset(x = startPx)
              .width(widthPx)
              .height(38.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF064E3B))
              .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) GreenAccent else Color.Transparent,
                RoundedCornerShape(6.dp)
              )
              .clickable { onSelectElement(SelectedTrackElement.Audio(clip.id)) }
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxSize(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = clip.title,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Medium),
                maxLines = 1,
                modifier = Modifier.widthIn(max = 90.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              // Waveform representation
              Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                clip.waveformData.take(24).forEach { amp ->
                  Box(
                    modifier = Modifier
                      .width(2.dp)
                      .height((amp * 24).dp)
                      .background(GreenAccent)
                  )
                }
              }
            }
          }
        }
      }
    }

    // Centered / Track Playhead (Red Needle)
    val playheadX = (currentPosMs / msPerPixel).dp
    Box(
      modifier = Modifier
        .offset(x = playheadX - scrollState.value.dp)
        .width(2.dp)
        .fillMaxHeight()
        .background(RedAccent)
    )
  }
}

@Composable
private fun TimecodeRuler(
  totalDurationMs: Long,
  msPerPixel: Float,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .background(StudioSurface)
      .border(1.dp, StudioBorder)
  ) {
    val secondInterval = 1000L
    val count = (totalDurationMs / secondInterval).toInt()
    for (i in 0..count) {
      val secondMs = i * secondInterval
      val xOffset = (secondMs / msPerPixel).dp
      Box(
        modifier = Modifier
          .offset(x = xOffset)
          .width(1.dp)
          .height(if (i % 5 == 0) 14.dp else 8.dp)
          .background(if (i % 5 == 0) TextSecondary else TextTertiary)
      )
      if (i % 5 == 0) {
        Text(
          text = "${i}s",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = TextSecondary),
          modifier = Modifier.offset(x = xOffset + 4.dp, y = 2.dp)
        )
      }
    }
  }
}

@Composable
private fun TrackRow(
  title: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  Box(
    modifier = modifier
      .height(50.dp)
      .background(StudioDarkBg)
      .border(0.5.dp, StudioBorder.copy(alpha = 0.4f))
      .padding(vertical = 4.dp)
  ) {
    content()
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
