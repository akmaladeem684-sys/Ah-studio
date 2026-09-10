package com.example.ui.components.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Timeline
import com.example.domain.model.TrackHeight
import com.example.domain.model.TrackSettings
import com.example.domain.model.TrackType
import com.example.domain.model.VideoClip
import com.example.engine.SelectedTrackElement
import com.example.ui.components.formatDurationShort
import com.example.ui.theme.*

@Composable
fun MultiTrackTimeline(
  timeline: Timeline,
  currentPosMs: Long,
  zoom: Float,
  selectedElement: SelectedTrackElement,
  selectedClipIds: Set<String>,
  isMultiSelectMode: Boolean,
  snapIndicatorMs: Long?,
  onSeek: (Long) -> Unit,
  onSelectElement: (SelectedTrackElement) -> Unit,
  onToggleClipSelection: (String) -> Unit,
  onZoomChange: (Float) -> Unit,
  onMoveClip: (clipId: String, deltaMs: Long) -> Unit,
  onTrimClipLeft: (clipId: String, deltaMs: Long) -> Unit,
  onTrimClipRight: (clipId: String, deltaMs: Long) -> Unit,
  onToggleTrackLock: (TrackType) -> Unit,
  onToggleTrackHide: (TrackType) -> Unit,
  onToggleTrackMute: (TrackType) -> Unit,
  onToggleTrackSolo: (TrackType) -> Unit,
  onCycleTrackHeight: (TrackType) -> Unit,
  onReorderVideoClips: ((fromIndex: Int, toIndex: Int) -> Unit)? = null,
  onOpenTrimTool: (() -> Unit)? = null,
  onSplitClip: (() -> Unit)? = null,
  onTrimLeftToPlayhead: (() -> Unit)? = null,
  onTrimRightToPlayhead: (() -> Unit)? = null,
  onDeleteClip: (() -> Unit)? = null,
  showTrackHeaders: Boolean = false,
  selectedKeyframeIds: Set<String> = emptySet(),
  onSelectKeyframe: ((String) -> Unit)? = null,
  onMoveKeyframe: ((String, Long) -> Unit)? = null,
  onAddAudioKeyframe: ((clipId: String, relTimeMs: Long, volume: Float) -> Unit)? = null,
  onUpdateAudioKeyframe: ((clipId: String, keyframeId: String, relTimeMs: Long, volume: Float) -> Unit)? = null,
  onDeleteAudioKeyframe: ((clipId: String, keyframeId: String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val horizontalScrollState = rememberScrollState()
  val verticalScrollState = rememberScrollState()

  val totalDuration = timeline.totalDurationMs.coerceAtLeast(10000L)
  val msPerPixel = remember(zoom) { (20f / zoom).coerceIn(2.5f, 120f) }
  val totalWidthDp = (totalDuration / msPerPixel).dp

  var showTrackHeadersState by remember { mutableStateOf(showTrackHeaders) }
  var isReorderBarExpanded by remember { mutableStateOf(false) }

  // Reorder dragging state on the Video track
  var draggedVideoIndex by remember { mutableStateOf<Int?>(null) }
  var dragAccumulatorPx by remember { mutableFloatStateOf(0f) }
  var dropTargetIndex by remember { mutableStateOf<Int?>(null) }

  val tracks = listOf(
    TrackType.MAIN_VIDEO,
    TrackType.OVERLAY,
    TrackType.TEXT,
    TrackType.AUDIO,
    TrackType.STICKER,
    TrackType.EFFECT
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioDarkBg)
      .border(1.dp, StudioBorder),
    color = StudioDarkBg
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // 0. Top Multi-Track Control Toolbar (Tracks toggle, Reorder bar toggle, Zoom, Timecode)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(34.dp)
          .background(StudioSurface)
          .drawBehind {
            drawLine(
              color = StudioBorder,
              start = Offset(0f, size.height),
              end = Offset(size.width, size.height),
              strokeWidth = 1.dp.toPx()
            )
          }
          .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Left Actions: Tracks Toggle & Reorder Sequencer Toggle
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Toggle Track Headers Button
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (showTrackHeadersState) StudioSurfaceVariant else StudioDarkBg,
            border = BorderStroke(1.dp, if (showTrackHeadersState) CyanAccent else StudioBorder),
            modifier = Modifier
              .clickable { showTrackHeadersState = !showTrackHeadersState }
              .testTag("toggle_track_headers_btn")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.ViewStream,
                contentDescription = "Track Headers",
                tint = if (showTrackHeadersState) CyanAccent else TextSecondary,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "Tracks",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (showTrackHeadersState) Color.White else TextSecondary
                )
              )
            }
          }

          // Toggle Video Clips Reorder Sequencer Strip
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isReorderBarExpanded) CyanAccent.copy(alpha = 0.2f) else StudioDarkBg,
            border = BorderStroke(1.dp, if (isReorderBarExpanded) CyanAccent else StudioBorder),
            modifier = Modifier
              .clickable { isReorderBarExpanded = !isReorderBarExpanded }
              .testTag("toggle_reorder_bar_btn")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Reorder Clips",
                tint = if (isReorderBarExpanded) CyanAccent else AmberAccent,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Reorder Clips (${timeline.videoClips.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isReorderBarExpanded) CyanAccent else Color.White
                )
              )
            }
          }

          // Trim Tool Shortcut Button
          if (onOpenTrimTool != null) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(AmberAccent.copy(alpha = 0.15f))
                .border(0.5.dp, AmberAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .clickable { onOpenTrimTool() }
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("timeline_open_trim_tool"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.ContentCut,
                  contentDescription = "Trim Tool",
                  tint = AmberAccent,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "Trim Tool",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberAccent
                  )
                )
              }
            }
          }
        }

        // Right Actions: Zoom controls and Timecode Display
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          IconButton(
            onClick = { onZoomChange((zoom * 0.8f).coerceIn(0.25f, 4.5f)) },
            modifier = Modifier.size(24.dp).testTag("timeline_zoom_out_btn")
          ) {
            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = TextSecondary, modifier = Modifier.size(14.dp))
          }
          Text(
            text = "${(zoom * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextSecondary)
          )
          IconButton(
            onClick = { onZoomChange((zoom * 1.25f).coerceIn(0.25f, 4.5f)) },
            modifier = Modifier.size(24.dp).testTag("timeline_zoom_in_btn")
          ) {
            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = TextSecondary, modifier = Modifier.size(14.dp))
          }

          Spacer(modifier = Modifier.width(4.dp))

          Text(
            text = "${formatDurationShort(currentPosMs)} / ${formatDurationShort(totalDuration)}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = RedAccent
            ),
            modifier = Modifier.testTag("timeline_timecode_display")
          )
        }
      }

      // 0.5. Expandable Video Clip Sequencer Strip (Tactile Reordering & Clip Navigation)
      if (isReorderBarExpanded && timeline.videoClips.isNotEmpty()) {
        VideoClipSequencerStrip(
          videoClips = timeline.videoClips,
          currentPosMs = currentPosMs,
          selectedClipId = (selectedElement as? SelectedTrackElement.Video)?.clipId,
          onSelectClip = { clip ->
            onSelectElement(SelectedTrackElement.Video(clip.id))
            onSeek(clip.timelineStartMs)
          },
          onReorder = { from, to -> onReorderVideoClips?.invoke(from, to) },
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(StudioDarkBg)
            .drawBehind {
              drawLine(
                color = StudioBorder,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx()
              )
            }
            .padding(horizontal = 8.dp, vertical = 3.dp)
        )
      }

      // 0.6. Contextual Timeline Action Toolbar (Split, Trim Left/Right, Ripple Delete, Open Trim Tool)
      if (selectedElement !is SelectedTrackElement.None) {
        TimelineActionToolbar(
          hasSelection = true,
          isMultiSelectMode = isMultiSelectMode,
          selectedCount = if (selectedClipIds.isNotEmpty()) selectedClipIds.size else 1,
          canPaste = false,
          isMagnetic = false,
          onSplit = { onSplitClip?.invoke() },
          onTrimLeft = { onTrimLeftToPlayhead?.invoke() },
          onTrimRight = { onTrimRightToPlayhead?.invoke() },
          onRippleDelete = { onDeleteClip?.invoke() },
          onNormalDelete = { onDeleteClip?.invoke() },
          onDuplicate = { /* duplicate */ },
          onCopy = { /* copy */ },
          onPaste = { /* paste */ },
          onSpeedClick = { /* speed */ },
          onReverse = { /* reverse */ },
          onFreezeFrame = { /* freeze */ },
          onReplaceMedia = { /* replace */ },
          onToggleMultiSelect = { /* multi */ },
          onToggleMagnetic = { /* magnetic */ },
          onOpenTrimTool = onOpenTrimTool,
          onNextPeak = null,
          modifier = Modifier.fillMaxWidth()
        )
      }

      // Main Timeline Tracks Area: Headers on left, Tracks on right
      Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
        // 1. Left Sticky Track Headers Column (Shown when showTrackHeadersState = true)
        if (showTrackHeadersState) {
          Column(
            modifier = Modifier
              .width(108.dp)
              .fillMaxHeight()
              .background(StudioSurface)
              .drawBehind {
                drawLine(
                  color = StudioBorder,
                  start = Offset(size.width, 0f),
                  end = Offset(size.width, size.height),
                  strokeWidth = 1.dp.toPx()
                )
              }
          ) {
            // Top empty space aligned with timecode ruler
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(StudioSurface)
                .drawBehind {
                  drawLine(
                    color = StudioBorder,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                  )
                }
            )

            // Scrollable headers matching vertical track content
            Column(
              modifier = Modifier
                .weight(1f)
                .verticalScroll(verticalScrollState)
            ) {
              tracks.forEach { trackType ->
                val settings = timeline.trackSettings[trackType] ?: TrackSettings(trackType)
                TrackHeaderControl(
                  trackType = trackType,
                  settings = settings,
                  onToggleLock = { onToggleTrackLock(trackType) },
                  onToggleHide = { onToggleTrackHide(trackType) },
                  onToggleMute = { onToggleTrackMute(trackType) },
                  onToggleSolo = { onToggleTrackSolo(trackType) },
                  onCycleHeight = { onCycleTrackHeight(trackType) }
                )
              }
            }
          }
        }

        // 2. Right Horizontally & Vertically Scrollable Tracks Area
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .pointerInput(zoom) {
              detectTransformGestures { _, _, zoomChange, _ ->
                if (kotlin.math.abs(zoomChange - 1f) > 0.01f) {
                  onZoomChange((zoom * zoomChange).coerceIn(0.25f, 4.5f))
                }
              }
            }
        ) {
          Column(modifier = Modifier.fillMaxSize()) {
            // Top Timecode Ruler (scrolls horizontally with tracks)
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .horizontalScroll(horizontalScrollState)
            ) {
              AccurateTimecodeRuler(
                totalDurationMs = totalDuration,
                currentPosMs = currentPosMs,
                msPerPixel = msPerPixel,
                onSeek = onSeek
              )
            }

            // Main Multi-Track Canvas
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
            ) {
              Column(
                modifier = Modifier
                  .width(totalWidthDp)
                  .fillMaxHeight()
                  .verticalScroll(verticalScrollState)
                  .pointerInput(totalDuration, msPerPixel) {
                    detectTapGestures { offset ->
                      val clickedMs = (offset.x * msPerPixel).toLong().coerceIn(0L, totalDuration)
                      onSeek(clickedMs)
                    }
                  }
              ) {
                tracks.forEach { trackType ->
                  val settings = timeline.trackSettings[trackType] ?: TrackSettings(trackType)
                  val trackHeightDp = settings.height.toDp()

                  val trackBadgeColor = when (trackType) {
                    TrackType.MAIN_VIDEO -> VideoTrackColor
                    TrackType.OVERLAY -> OverlayTrackColor
                    TrackType.TEXT -> TextTrackColor
                    TrackType.AUDIO -> AudioTrackColor
                    TrackType.STICKER -> StickerTrackColor
                    TrackType.EFFECT -> EffectTrackColor
                  }

                  val trackBadgeText = when (trackType) {
                    TrackType.MAIN_VIDEO -> "V1"
                    TrackType.OVERLAY -> "V2"
                    TrackType.TEXT -> "T1"
                    TrackType.AUDIO -> "A1"
                    TrackType.STICKER -> "S1"
                    TrackType.EFFECT -> "FX"
                  }

                  // Lane Row Container
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(trackHeightDp)
                      .background(if (settings.isLocked) StudioSurface.copy(alpha = 0.5f) else StudioDarkBg)
                      .drawBehind {
                        // Lane bottom separator
                        drawLine(
                          color = StudioBorder,
                          start = Offset(0f, size.height),
                          end = Offset(size.width, size.height),
                          strokeWidth = 1.dp.toPx()
                        )
                      }
                      .testTag("track_lane_${trackType.name}")
                  ) {
                    // Small left track badge indicator when sticky headers are collapsed
                    if (!showTrackHeadersState) {
                      Surface(
                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                        color = trackBadgeColor.copy(alpha = 0.85f),
                        modifier = Modifier
                          .align(Alignment.CenterStart)
                          .padding(start = 2.dp)
                      ) {
                        Text(
                          text = trackBadgeText,
                          style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp
                          ),
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                      }
                    }

                    when (trackType) {
                      TrackType.MAIN_VIDEO -> {
                        // 1. Render Video Clips with filmstrip look, index badges, and reorder controls
                        timeline.videoClips.forEachIndexed { index, clip ->
                          val isSelected = (selectedElement as? SelectedTrackElement.Video)?.clipId == clip.id
                          val isMulti = clip.id in selectedClipIds
                          val isBeingReordered = (draggedVideoIndex == index)

                          TimelineClipView(
                            clipId = clip.id,
                            title = clip.name,
                            timelineStartMs = clip.timelineStartMs,
                            durationMs = clip.durationMs,
                            sourceStartMs = clip.sourceStartMs,
                            sourceEndMs = clip.sourceEndMs,
                            currentPlayheadMs = currentPosMs,
                            hasAudio = clip.hasAudio && clip.isVideo,
                            isMuted = clip.isMuted || settings.isMuted,
                            trackColor = VideoTrackColor,
                            heightDp = trackHeightDp,
                            msPerPixel = msPerPixel,
                            isSelected = isSelected,
                            isMultiSelected = isMulti,
                            isLocked = settings.isLocked,
                            speed = clip.speed,
                            isReversed = clip.isReversed,
                            isFreeze = !clip.isVideo,
                            keyframes = clip.keyframes,
                            selectedKeyframeIds = selectedKeyframeIds,
                            onSelectKeyframe = onSelectKeyframe,
                            onMoveKeyframe = onMoveKeyframe,
                            clipIndex = index,
                            totalClipsInTrack = timeline.videoClips.size,
                            isVideoClip = true,
                            isBeingReordered = isBeingReordered,
                            onMoveEarlier = if (index > 0) { { onReorderVideoClips?.invoke(index, index - 1) } } else null,
                            onMoveLater = if (index < timeline.videoClips.size - 1) { { onReorderVideoClips?.invoke(index, index + 1) } } else null,
                            onStartReorderDrag = {
                              draggedVideoIndex = index
                              dragAccumulatorPx = 0f
                              dropTargetIndex = index
                            },
                            onReorderDrag = { deltaPx ->
                              dragAccumulatorPx += deltaPx
                              val currentCenterMs = clip.timelineStartMs + (clip.durationMs / 2) + (dragAccumulatorPx * msPerPixel).toLong()
                              var computedTarget = 0
                              for (i in timeline.videoClips.indices) {
                                val c = timeline.videoClips[i]
                                val cMid = c.timelineStartMs + (c.durationMs / 2)
                                if (currentCenterMs > cMid) {
                                  computedTarget = i
                                }
                              }
                              dropTargetIndex = computedTarget.coerceIn(0, timeline.videoClips.size - 1)
                            },
                            onEndReorderDrag = {
                              val from = draggedVideoIndex
                              val to = dropTargetIndex
                              if (from != null && to != null && from != to) {
                                onReorderVideoClips?.invoke(from, to)
                              }
                              draggedVideoIndex = null
                              dragAccumulatorPx = 0f
                              dropTargetIndex = null
                            },
                            onSelect = {
                              if (isMultiSelectMode) onToggleClipSelection(clip.id)
                              else {
                                onSelectElement(SelectedTrackElement.Video(clip.id))
                                onSeek(clip.timelineStartMs)
                              }
                            },
                            onLongClick = { onToggleClipSelection(clip.id) },
                            onMoveClip = { delta -> onMoveClip(clip.id, delta) },
                            onTrimLeft = { delta -> onTrimClipLeft(clip.id, delta) },
                            onTrimRight = { delta -> onTrimClipRight(clip.id, delta) }
                          )
                        }

                        // 2. Transition Cut Badges between adjacent video clips
                        if (timeline.videoClips.size > 1) {
                          for (i in 0 until timeline.videoClips.size - 1) {
                            val clipA = timeline.videoClips[i]
                            val cutPosMs = clipA.timelineStartMs + clipA.durationMs
                            val cutX = (cutPosMs / msPerPixel).dp - 8.dp
                            val existingTransition = timeline.transitions.find { it.clipIndexBefore == i }

                            Box(
                              modifier = Modifier
                                .offset(x = cutX, y = (trackHeightDp - 18.dp) / 2)
                                .size(16.dp)
                                .rotate(45f)
                                .background(
                                  if (existingTransition != null) CyanAccent else StudioSurfaceVariant,
                                  RoundedCornerShape(2.dp)
                                )
                                .border(
                                  1.dp,
                                  if (existingTransition != null) Color.White else StudioBorder,
                                  RoundedCornerShape(2.dp)
                                )
                                .testTag("transition_badge_$i"),
                              contentAlignment = Alignment.Center
                            ) {
                              Icon(
                                imageVector = Icons.Default.Transform,
                                contentDescription = existingTransition?.type?.displayName ?: "Transition",
                                tint = if (existingTransition != null) Color.Black else TextSecondary,
                                modifier = Modifier.size(10.dp).rotate(-45f)
                              )
                            }
                          }
                        }

                        // 3. Drop Insertion Indicator when dragging to reorder
                        if (draggedVideoIndex != null && dropTargetIndex != null && dropTargetIndex != draggedVideoIndex) {
                          val fromIdx = draggedVideoIndex!!
                          val toIdx = dropTargetIndex!!
                          val targetClip = timeline.videoClips[toIdx]
                          val insertPosMs = if (toIdx > fromIdx) {
                            targetClip.timelineStartMs + targetClip.durationMs
                          } else {
                            targetClip.timelineStartMs
                          }
                          val insertX = (insertPosMs / msPerPixel).dp

                          Box(
                            modifier = Modifier
                              .offset(x = insertX - 2.dp)
                              .width(4.dp)
                              .height(trackHeightDp)
                              .background(CyanAccent)
                              .testTag("reorder_drop_indicator")
                          ) {
                            Surface(
                              shape = RoundedCornerShape(4.dp),
                              color = Color.Black.copy(alpha = 0.88f),
                              border = BorderStroke(1.dp, CyanAccent),
                              modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-18).dp)
                            ) {
                              Text(
                                text = "Move #${fromIdx + 1} ➔ #${toIdx + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                  fontSize = 9.sp,
                                  fontWeight = FontWeight.Bold,
                                  color = CyanAccent
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                              )
                            }
                          }
                        }
                      }

                      TrackType.OVERLAY -> {
                        timeline.overlayClips.forEach { clip ->
                          val isSelected = (selectedElement as? SelectedTrackElement.Overlay)?.clipId == clip.id
                          val isMulti = clip.id in selectedClipIds
                          TimelineClipView(
                            clipId = clip.id,
                            title = clip.name,
                            timelineStartMs = clip.timelineStartMs,
                            durationMs = clip.durationMs,
                            trackColor = OverlayTrackColor,
                            heightDp = trackHeightDp,
                            msPerPixel = msPerPixel,
                            isSelected = isSelected,
                            isMultiSelected = isMulti,
                            isLocked = settings.isLocked,
                            onSelect = {
                              if (isMultiSelectMode) onToggleClipSelection(clip.id)
                              else onSelectElement(SelectedTrackElement.Overlay(clip.id))
                            },
                            onLongClick = { onToggleClipSelection(clip.id) },
                            onMoveClip = { delta -> onMoveClip(clip.id, delta) },
                            onTrimLeft = { delta -> onTrimClipLeft(clip.id, delta) },
                            onTrimRight = { delta -> onTrimClipRight(clip.id, delta) }
                          )
                        }
                      }

                      TrackType.TEXT -> {
                        timeline.textClips.forEach { clip ->
                          val isSelected = (selectedElement as? SelectedTrackElement.Text)?.clipId == clip.id
                          val isMulti = clip.id in selectedClipIds
                          TimelineClipView(
                            clipId = clip.id,
                            title = clip.text,
                            timelineStartMs = clip.timelineStartMs,
                            durationMs = clip.durationMs,
                            trackColor = TextTrackColor,
                            heightDp = trackHeightDp,
                            msPerPixel = msPerPixel,
                            isSelected = isSelected,
                            isMultiSelected = isMulti,
                            isLocked = settings.isLocked,
                            onSelect = {
                              if (isMultiSelectMode) onToggleClipSelection(clip.id)
                              else onSelectElement(SelectedTrackElement.Text(clip.id))
                            },
                            onLongClick = { onToggleClipSelection(clip.id) },
                            onMoveClip = { delta -> onMoveClip(clip.id, delta) },
                            onTrimLeft = { delta -> onTrimClipLeft(clip.id, delta) },
                            onTrimRight = { delta -> onTrimClipRight(clip.id, delta) }
                          )
                        }
                      }

                      TrackType.AUDIO -> {
                        timeline.audioClips.forEach { clip ->
                          val isSelected = (selectedElement as? SelectedTrackElement.Audio)?.clipId == clip.id
                          val isMulti = clip.id in selectedClipIds
                          TimelineClipView(
                            clipId = clip.id,
                            title = clip.title,
                            timelineStartMs = clip.timelineStartMs,
                            durationMs = clip.durationMs,
                            sourceStartMs = clip.sourceStartMs,
                            sourceEndMs = clip.sourceEndMs,
                            currentPlayheadMs = currentPosMs,
                            hasAudio = true,
                            isMuted = clip.isMuted || settings.isMuted,
                            trackColor = AudioTrackColor,
                            heightDp = trackHeightDp,
                            msPerPixel = msPerPixel,
                            isSelected = isSelected,
                            isMultiSelected = isMulti,
                            isLocked = settings.isLocked,
                            waveformData = clip.waveformData,
                            keyframes = clip.keyframes,
                            selectedKeyframeIds = selectedKeyframeIds,
                            baseVolume = clip.volume,
                            fadeInMs = clip.fadeInMs,
                            fadeOutMs = clip.fadeOutMs,
                            showVolumeEnvelope = true,
                            onSelectKeyframe = onSelectKeyframe,
                            onMoveKeyframe = onMoveKeyframe,
                            onAddVolumeKeyframe = { relTime, vol ->
                              onAddAudioKeyframe?.invoke(clip.id, relTime, vol)
                            },
                            onUpdateVolumeKeyframe = { kfId, relTime, vol ->
                              onUpdateAudioKeyframe?.invoke(clip.id, kfId, relTime, vol)
                            },
                            onDeleteVolumeKeyframe = { kfId ->
                              onDeleteAudioKeyframe?.invoke(clip.id, kfId)
                            },
                            onSelect = {
                              if (isMultiSelectMode) onToggleClipSelection(clip.id)
                              else onSelectElement(SelectedTrackElement.Audio(clip.id))
                            },
                            onLongClick = { onToggleClipSelection(clip.id) },
                            onMoveClip = { delta -> onMoveClip(clip.id, delta) },
                            onTrimLeft = { delta -> onTrimClipLeft(clip.id, delta) },
                            onTrimRight = { delta -> onTrimClipRight(clip.id, delta) }
                          )
                        }
                      }

                      TrackType.STICKER -> {
                        timeline.stickerClips.forEach { clip ->
                          val isSelected = (selectedElement as? SelectedTrackElement.Sticker)?.clipId == clip.id
                          val isMulti = clip.id in selectedClipIds
                          TimelineClipView(
                            clipId = clip.id,
                            title = clip.emojiOrAsset,
                            timelineStartMs = clip.timelineStartMs,
                            durationMs = clip.durationMs,
                            trackColor = StickerTrackColor,
                            heightDp = trackHeightDp,
                            msPerPixel = msPerPixel,
                            isSelected = isSelected,
                            isMultiSelected = isMulti,
                            isLocked = settings.isLocked,
                            onSelect = {
                              if (isMultiSelectMode) onToggleClipSelection(clip.id)
                              else onSelectElement(SelectedTrackElement.Sticker(clip.id))
                            },
                            onLongClick = { onToggleClipSelection(clip.id) },
                            onMoveClip = { delta -> onMoveClip(clip.id, delta) },
                            onTrimLeft = { delta -> onTrimClipLeft(clip.id, delta) },
                            onTrimRight = { delta -> onTrimClipRight(clip.id, delta) }
                          )
                        }
                      }

                      TrackType.EFFECT -> {
                        timeline.effectClips.forEach { clip ->
                          val isSelected = (selectedElement as? SelectedTrackElement.Effect)?.clipId == clip.id
                          val isMulti = clip.id in selectedClipIds
                          TimelineClipView(
                            clipId = clip.id,
                            title = clip.effectType.displayName,
                            timelineStartMs = clip.timelineStartMs,
                            durationMs = clip.durationMs,
                            trackColor = EffectTrackColor,
                            heightDp = trackHeightDp,
                            msPerPixel = msPerPixel,
                            isSelected = isSelected,
                            isMultiSelected = isMulti,
                            isLocked = settings.isLocked,
                            onSelect = {
                              if (isMultiSelectMode) onToggleClipSelection(clip.id)
                              else onSelectElement(SelectedTrackElement.Effect(clip.id))
                            },
                            onLongClick = { onToggleClipSelection(clip.id) },
                            onMoveClip = { delta -> onMoveClip(clip.id, delta) },
                            onTrimLeft = { delta -> onTrimClipLeft(clip.id, delta) },
                            onTrimRight = { delta -> onTrimClipRight(clip.id, delta) }
                          )
                        }
                      }
                    }
                  }
                }
              }

              // Snapping Guide Line Indicator (Vertical line across all tracks)
              if (snapIndicatorMs != null) {
                val snapX = (snapIndicatorMs / msPerPixel).dp
                Box(
                  modifier = Modifier
                    .offset(x = snapX)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(CyanAccent)
                )
              }

              // Playhead Red Needle (Inside scrollable area so it moves in absolute sync)
              val playheadX = (currentPosMs / msPerPixel).dp
              Box(
                modifier = Modifier
                  .offset(x = playheadX)
                  .width(2.dp)
                  .fillMaxHeight()
                  .background(RedAccent)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun VideoClipSequencerStrip(
  videoClips: List<VideoClip>,
  currentPosMs: Long,
  selectedClipId: String?,
  onSelectClip: (VideoClip) -> Unit,
  onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Row(
    modifier = modifier
      .horizontalScroll(scrollState)
      .testTag("video_clip_sequencer_strip"),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    videoClips.forEachIndexed { index, clip ->
      val isSelected = clip.id == selectedClipId
      val isPlaying = currentPosMs in clip.timelineStartMs until (clip.timelineStartMs + clip.durationMs)

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) StudioSurfaceVariant else StudioSurface,
        border = BorderStroke(
          width = if (isSelected) 1.5.dp else 1.dp,
          color = when {
            isSelected -> CyanAccent
            isPlaying -> AmberAccent
            else -> StudioBorder
          }
        ),
        modifier = Modifier
          .fillMaxHeight()
          .widthIn(min = 130.dp, max = 200.dp)
          .clickable { onSelectClip(clip) }
          .testTag("sequencer_card_$index")
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Left: Index Badge + Title/Duration
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(20.dp)
                .background(if (isSelected) CyanAccent else VideoTrackColor, RoundedCornerShape(4.dp)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) Color.Black else Color.White
                )
              )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = clip.name,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = formatDurationShort(clip.durationMs),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  color = if (isPlaying) AmberAccent else TextSecondary
                )
              )
            }
          }

          // Right: Swap Left / Right Nudge buttons
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            if (index > 0) {
              Box(
                modifier = Modifier
                  .size(22.dp)
                  .clickable { onReorder(index, index - 1) }
                  .testTag("sequencer_move_left_$index"),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.ArrowBack,
                  contentDescription = "Move Left",
                  tint = CyanAccent,
                  modifier = Modifier.size(13.dp)
                )
              }
            }

            if (index < videoClips.size - 1) {
              Box(
                modifier = Modifier
                  .size(22.dp)
                  .clickable { onReorder(index, index + 1) }
                  .testTag("sequencer_move_right_$index"),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.ArrowForward,
                  contentDescription = "Move Right",
                  tint = CyanAccent,
                  modifier = Modifier.size(13.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}
