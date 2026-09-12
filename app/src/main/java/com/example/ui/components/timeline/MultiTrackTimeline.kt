package com.example.ui.components.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.domain.model.Timeline
import com.example.domain.model.TrackHeight
import com.example.domain.model.TrackSettings
import com.example.domain.model.TrackType
import com.example.domain.model.Transition
import com.example.domain.model.TransitionType
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
  onOpenKeyframeTool: (() -> Unit)? = null,
  onOpenTransitionsTool: (() -> Unit)? = null,
  selectedTransitionCutIndex: Int = 0,
  onSelectTransitionCut: ((Int) -> Unit)? = null,
  draggedTransitionType: TransitionType? = null,
  onDropTransition: ((cutIndex: Int, type: TransitionType) -> Unit)? = null,
  onSplitClip: (() -> Unit)? = null,
  onTrimLeftToPlayhead: (() -> Unit)? = null,
  onTrimRightToPlayhead: (() -> Unit)? = null,
  onDeleteClip: (() -> Unit)? = null,
  onRippleDelete: (() -> Unit)? = null,
  onNormalDelete: (() -> Unit)? = null,
  onDuplicateClip: (() -> Unit)? = null,
  onCopyClip: (() -> Unit)? = null,
  onPasteClip: (() -> Unit)? = null,
  onToggleMultiSelect: (() -> Unit)? = null,
  onNextPeak: (() -> Unit)? = null,
  onPrevPeak: (() -> Unit)? = null,
  onNextSilence: (() -> Unit)? = null,
  onPrevSilence: (() -> Unit)? = null,
  onRemoveSilence: (() -> Unit)? = null,
  waveformStyle: WaveformStyle = WaveformStyle.MIRRORED_BARS,
  onToggleWaveformStyle: (() -> Unit)? = null,
  fps: Int = 30,
  isFrameSnapping: Boolean = false,
  onStepFrames: ((Int) -> Unit)? = null,
  onSeekToPrevCut: (() -> Unit)? = null,
  onSeekToNextCut: (() -> Unit)? = null,
  onFpsChange: ((Int) -> Unit)? = null,
  onToggleFrameSnapping: (() -> Unit)? = null,
  showTrackHeaders: Boolean = true,
  selectedKeyframeIds: Set<String> = emptySet(),
  onSelectKeyframe: ((String) -> Unit)? = null,
  onMoveKeyframe: ((String, Long) -> Unit)? = null,
  onAddAudioKeyframe: ((clipId: String, relTimeMs: Long, volume: Float) -> Unit)? = null,
  onUpdateAudioKeyframe: ((clipId: String, keyframeId: String, relTimeMs: Long, volume: Float) -> Unit)? = null,
  onDeleteAudioKeyframe: ((clipId: String, keyframeId: String) -> Unit)? = null,
  onAddMedia: (() -> Unit)? = null,
  onAddAudio: (() -> Unit)? = null,
  onAddText: (() -> Unit)? = null,
  onAddOverlay: (() -> Unit)? = null,
  onAddSticker: (() -> Unit)? = null,
  onAddEffect: (() -> Unit)? = null,
  onEditCover: (() -> Unit)? = null,
  onToggleMuteAllVideo: (() -> Unit)? = null,
  isTracksSyncEnabled: Boolean = true,
  onToggleTracksSync: (() -> Unit)? = null,
  onMoveToPlayhead: (() -> Unit)? = null,
  onSplitAllTracks: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val horizontalScrollState = rememberScrollState()
  val verticalScrollState = rememberScrollState()

  val totalDuration = timeline.totalDurationMs.coerceAtLeast(3000L)
  val msPerPixel = remember(zoom) { (20f / zoom).coerceIn(2.5f, 120f) }
  val maxTimelineMs = remember(timeline, totalDuration) {
    maxOf(
      totalDuration,
      timeline.videoClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L,
      timeline.overlayClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L,
      timeline.audioClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L,
      timeline.textClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L,
      timeline.stickerClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L,
      timeline.effectClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
    ).coerceAtLeast(3000L)
  }
  val trackContentWidthDp = (maxTimelineMs / msPerPixel).dp + 180.dp

  // Reorder dragging state on the Video track
  var draggedVideoIndex by remember { mutableStateOf<Int?>(null) }
  var dragAccumulatorPx by remember { mutableFloatStateOf(0f) }
  var dropTargetIndex by remember { mutableStateOf<Int?>(null) }

  var isMutedAll by remember { mutableStateOf(false) }

  // Touch Scrubbing & Drag gesture state
  var isTouchScrubbing by remember { mutableStateOf(false) }
  var scrubAccumulatorMs by remember { mutableFloatStateOf(currentPosMs.toFloat()) }

  // Sync scrubAccumulatorMs when currentPosMs changes externally
  LaunchedEffect(currentPosMs) {
    if (!isTouchScrubbing) {
      scrubAccumulatorMs = currentPosMs.toFloat()
    }
  }

  // Keep scroll position strictly synchronized with currentPosMs (moves timeline underneath fixed center CTI)
  LaunchedEffect(currentPosMs, msPerPixel) {
    val targetScrollPx = (currentPosMs / msPerPixel).roundToInt()
    if (kotlin.math.abs(horizontalScrollState.value - targetScrollPx) > 1) {
      horizontalScrollState.scrollTo(targetScrollPx)
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxWidth()
      .background(Color.Black)
  ) {
    val hasAnyTrack = timeline.videoClips.isNotEmpty() ||
      timeline.overlayClips.isNotEmpty() ||
      timeline.audioClips.isNotEmpty() ||
      timeline.textClips.isNotEmpty() ||
      timeline.stickerClips.isNotEmpty() ||
      timeline.effectClips.isNotEmpty()

    val timelineViewportWidthDp = (maxWidth - (if (hasAnyTrack) 88.dp else 0.dp)).coerceAtLeast(100.dp)
    val centerPaddingDp = timelineViewportWidthDp / 2

    Box(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxSize()) {
        // 1. Timecode & Ruler Bar (Left: "00:00 / 00:03", Right: dots timeline ruler)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(Color.Black),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Timecode display
          Box(
            modifier = Modifier
              .width(if (hasAnyTrack) 88.dp else 72.dp)
              .fillMaxHeight()
              .padding(start = 12.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            Text(
              text = if (hasAnyTrack) "${formatDurationShort(currentPosMs)} / ${formatDurationShort(timeline.totalDurationMs)}" else "00:00 / 00:00",
              style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              ),
              maxLines = 1,
              modifier = Modifier.testTag("timeline_timecode_text")
            )
          }

          // Timeline Ruler with Time Markers & Dots (Scrolls smoothly under fixed center CTI)
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .horizontalScroll(horizontalScrollState)
          ) {
            Row(modifier = Modifier.fillMaxHeight()) {
              Spacer(modifier = Modifier.width(centerPaddingDp))
              AccurateTimecodeRuler(
                totalDurationMs = totalDuration,
                currentPosMs = currentPosMs,
                msPerPixel = msPerPixel,
                fps = fps,
                isFrameSnapping = isFrameSnapping,
                onSeek = onSeek,
                onDoubleTapSnap = onSeekToNextCut
              )
              Spacer(modifier = Modifier.width(centerPaddingDp))
            }
          }
        }

        // Contextual Timeline Action Toolbar (Split, Trim, Align CTI, Sync Tracks, etc.)
        val canEditAtPlayhead = timeline.videoClips.any { currentPosMs >= it.timelineStartMs && currentPosMs <= it.timelineStartMs + it.durationMs } ||
          timeline.overlayClips.any { currentPosMs >= it.timelineStartMs && currentPosMs <= it.timelineStartMs + it.durationMs } ||
          timeline.audioClips.any { currentPosMs >= it.timelineStartMs && currentPosMs <= it.timelineStartMs + it.durationMs } ||
          timeline.textClips.any { currentPosMs >= it.timelineStartMs && currentPosMs <= it.timelineStartMs + it.durationMs } ||
          timeline.stickerClips.any { currentPosMs >= it.timelineStartMs && currentPosMs <= it.timelineStartMs + it.durationMs } ||
          timeline.effectClips.any { currentPosMs >= it.timelineStartMs && currentPosMs <= it.timelineStartMs + it.durationMs }

        AnimatedVisibility(
          visible = hasAnyTrack && (selectedElement !is SelectedTrackElement.None || timeline.totalDurationMs > 0L),
          enter = slideInVertically { -it } + fadeIn(),
          exit = slideOutVertically { -it } + fadeOut()
        ) {
          TimelineActionToolbar(
            hasSelection = selectedElement !is SelectedTrackElement.None,
            isMultiSelectMode = isMultiSelectMode,
            selectedCount = if (selectedClipIds.isNotEmpty()) selectedClipIds.size else 1,
            canPaste = false,
            isMagnetic = false,
            isTracksSyncEnabled = isTracksSyncEnabled,
            onToggleTracksSync = onToggleTracksSync,
            onMoveToPlayhead = onMoveToPlayhead,
            canEditAtPlayhead = canEditAtPlayhead,
            onSplitAllTracks = onSplitAllTracks,
            onSplit = { onSplitClip?.invoke() },
            onTrimLeft = { onTrimLeftToPlayhead?.invoke() },
            onTrimRight = { onTrimRightToPlayhead?.invoke() },
            onRippleDelete = {
              if (onRippleDelete != null) onRippleDelete.invoke()
              else onDeleteClip?.invoke()
            },
            onNormalDelete = {
              if (onNormalDelete != null) onNormalDelete.invoke()
              else onDeleteClip?.invoke()
            },
            onDuplicate = { onDuplicateClip?.invoke() },
            onCopy = { onCopyClip?.invoke() },
            onPaste = { onPasteClip?.invoke() },
            onSpeedClick = { /* speed */ },
            onReverse = { /* reverse */ },
            onFreezeFrame = { /* freeze */ },
            onReplaceMedia = { /* replace */ },
            onToggleMultiSelect = { onToggleMultiSelect?.invoke() },
            onToggleMagnetic = { /* magnetic */ },
            onOpenTrimTool = onOpenTrimTool,
            onOpenKeyframeTool = onOpenKeyframeTool,
            onOpenTransitionsTool = onOpenTransitionsTool,
            onNextPeak = onNextPeak,
            onPrevPeak = onPrevPeak,
            onNextSilence = onNextSilence,
            onPrevSilence = onPrevSilence,
            onRemoveSilence = onRemoveSilence,
            onToggleWaveformStyle = onToggleWaveformStyle,
            modifier = Modifier.fillMaxWidth()
          )
        }

        if (!hasAnyTrack) {
          // Clean empty timeline view when project has no tracks yet
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .background(Color(0xFF0D0F14)),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.padding(24.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = CyanAccent.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, CyanAccent),
                modifier = Modifier
                  .size(56.dp)
                  .clickable { onAddMedia?.invoke() }
                  .testTag("empty_timeline_add_media_btn")
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Media",
                    tint = CyanAccent,
                    modifier = Modifier.size(30.dp)
                  )
                }
              }
              Text(
                text = "Tap + to add your first video or photo",
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = Color.White.copy(alpha = 0.75f),
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Medium
                )
              )
            }
          }
        } else {
          // 2. Main Timeline Tracks View (Fixed Left Utility Column + Scrollable Multi-Track Lanes)
          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxSize()) {
              // Left Static Column (Track Icons & Controls aligned vertically with track lanes)
              Column(
                modifier = Modifier
                  .width(88.dp)
                  .fillMaxHeight()
                  .background(Color.Black)
                  .padding(start = 6.dp, end = 6.dp)
                  .verticalScroll(verticalScrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                // Row 1: Video Track Left Utility (Mute clip + Cover Card) (64.dp)
                if (timeline.videoClips.isNotEmpty()) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    // Mute Clip Button
                    Column(
                      modifier = Modifier
                        .width(36.dp)
                        .clickable {
                          isMutedAll = !isMutedAll
                          onToggleTrackMute(TrackType.MAIN_VIDEO)
                          onToggleMuteAllVideo?.invoke()
                        }
                        .testTag("mute_clip_btn"),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Center
                    ) {
                      Icon(
                        imageVector = if (isMutedAll) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute clip",
                        tint = if (isMutedAll) RedAccent else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = "Mute\nclip",
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontSize = 9.sp,
                          color = Color.White.copy(alpha = 0.75f),
                          textAlign = TextAlign.Center,
                          lineHeight = 11.sp
                        )
                      )
                    }

                    // Cover Thumbnail Card
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = Color(0xFF222630),
                      border = BorderStroke(1.dp, Color(0xFF333A4A)),
                      modifier = Modifier
                        .width(36.dp)
                        .height(52.dp)
                        .clickable { onEditCover?.invoke() }
                        .testTag("cover_thumbnail_btn")
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Default.Image,
                          contentDescription = null,
                          tint = Color.White.copy(alpha = 0.4f),
                          modifier = Modifier.size(18.dp)
                        )
                        Column(
                          modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                          verticalArrangement = Arrangement.Center,
                          horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                          Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                          )
                          Text(
                            text = "Cover",
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 8.5.sp,
                              fontWeight = FontWeight.Bold,
                              color = Color.White
                            )
                          )
                        }
                      }
                    }
                  }
                }

                // Row 2: Overlay / PIP Track Icon (44.dp)
                if (timeline.overlayClips.isNotEmpty()) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFF1B1F2A))
                      .border(0.5.dp, Color(0xFF2E3547), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Layers,
                      contentDescription = "Overlay Track",
                      tint = OverlayTrackColor,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }

                // Row 3: Audio Track Icon (36.dp)
                if (timeline.audioClips.isNotEmpty()) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFF1B1F2A))
                      .border(0.5.dp, Color(0xFF2E3547), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.MusicNote,
                      contentDescription = "Audio Track",
                      tint = AudioTrackColor,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }

                // Row 4: Text Track Icon (36.dp)
                if (timeline.textClips.isNotEmpty()) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFF1B1F2A))
                      .border(0.5.dp, Color(0xFF2E3547), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "T",
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTrackColor
                      )
                    )
                  }
                }

                // Row 5: Sticker Track Icon (36.dp)
                if (timeline.stickerClips.isNotEmpty()) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFF1B1F2A))
                      .border(0.5.dp, Color(0xFF2E3547), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.EmojiEmotions,
                      contentDescription = "Sticker Track",
                      tint = StickerTrackColor,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }

                // Row 6: Effect Track Icon (36.dp)
                if (timeline.effectClips.isNotEmpty()) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFF1B1F2A))
                      .border(0.5.dp, Color(0xFF2E3547), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.AutoAwesome,
                      contentDescription = "Effect Track",
                      tint = EffectTrackColor,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }

            // Right Horizontally Scrollable Tracks Area with Touch Scrubbing
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(totalDuration, msPerPixel, isFrameSnapping, fps) {
                  detectDragGestures(
                    onDragStart = {
                      isTouchScrubbing = true
                      scrubAccumulatorMs = currentPosMs.toFloat()
                    },
                    onDragEnd = {
                      isTouchScrubbing = false
                    },
                    onDragCancel = {
                      isTouchScrubbing = false
                    },
                    onDrag = { change, dragAmount ->
                      change.consume()
                      val deltaMs = -dragAmount.x * msPerPixel
                      scrubAccumulatorMs = (scrubAccumulatorMs + deltaMs).coerceIn(0f, maxTimelineMs.toFloat())
                      val rawMs = scrubAccumulatorMs.toLong()
                      val targetMs = if (isFrameSnapping) {
                        val frameMs = 1000.0 / fps
                        (Math.round(rawMs / frameMs) * frameMs).toLong().coerceIn(0L, maxTimelineMs)
                      } else rawMs
                      onSeek(targetMs)
                    }
                  )
                }
                .pointerInput(zoom) {
                  detectTransformGestures { _, _, zoomChange, _ ->
                    if (kotlin.math.abs(zoomChange - 1f) > 0.01f) {
                      onZoomChange((zoom * zoomChange).coerceIn(0.25f, 4.5f))
                    }
                  }
                }
            ) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .horizontalScroll(horizontalScrollState)
              ) {
                Column(
                  modifier = Modifier
                    .width(trackContentWidthDp + centerPaddingDp * 2)
                    .fillMaxHeight()
                    .verticalScroll(verticalScrollState)
                    .pointerInput(maxTimelineMs, msPerPixel) {
                      detectTapGestures { offset ->
                        val viewportCenterX = size.width / 2f
                        val deltaPx = offset.x - viewportCenterX
                        val deltaMs = deltaPx * msPerPixel
                        val clickedMs = (currentPosMs + deltaMs.toLong()).coerceIn(0L, maxTimelineMs)
                        onSeek(clickedMs)
                      }
                    },
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  // ==========================================
                  // 1. MAIN VIDEO TRACK (Filmstrip + Add Ending + White Add Media Button)
                  // ==========================================
                  val videoTrackHeight = 64.dp
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(videoTrackHeight)
                      .testTag("main_video_track")
                  ) {
                    Box(
                      modifier = Modifier
                        .fillMaxHeight()
                        .offset(x = centerPaddingDp)
                    ) {
                      // Video Clips positioned absolutely by timelineStartMs
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
                          isMuted = clip.isMuted || isMutedAll,
                          trackColor = VideoTrackColor,
                          heightDp = videoTrackHeight,
                          msPerPixel = msPerPixel,
                          isSelected = isSelected,
                          isMultiSelected = isMulti,
                          isLocked = false,
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
                          uri = clip.uri,
                          isVideo = clip.isVideo,
                          isBeingReordered = isBeingReordered,
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

                      // Transition Cut Badges between adjacent video clips
                      if (timeline.videoClips.size > 1) {
                        for (i in 0 until timeline.videoClips.size - 1) {
                          val clipA = timeline.videoClips[i]
                          val cutPosMs = clipA.timelineStartMs + clipA.durationMs
                          val cutX = (cutPosMs / msPerPixel).dp - 9.dp
                          val existingTransition = timeline.transitions.find { it.clipIndexBefore == i }
                          val isSelectedCut = i == selectedTransitionCutIndex

                          Box(
                            modifier = Modifier
                              .offset(x = cutX, y = (videoTrackHeight - 18.dp) / 2)
                              .size(18.dp)
                              .rotate(45f)
                              .background(
                                if (existingTransition != null) PurpleAccent else Color(0xFF222836),
                                RoundedCornerShape(2.dp)
                              )
                              .border(1.dp, if (isSelectedCut) CyanAccent else Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                              .clickable {
                                onSelectTransitionCut?.invoke(i)
                                onOpenTransitionsTool?.invoke()
                              }
                              .testTag("transition_badge_$i"),
                            contentAlignment = Alignment.Center
                          ) {
                            Icon(
                              imageVector = if (existingTransition != null) Icons.Default.Transform else Icons.Default.Add,
                              contentDescription = "Transition",
                              tint = Color.White,
                              modifier = Modifier.size(10.dp).rotate(-45f)
                            )
                          }
                        }
                      }

                      // Add Media / Add Ending controls at the end of the video track
                      val maxVideoEndMs = timeline.videoClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
                      val addMediaOffset = if (timeline.videoClips.isEmpty()) 0.dp else (maxVideoEndMs / msPerPixel).dp + 8.dp

                      Box(
                        modifier = Modifier
                          .offset(x = addMediaOffset)
                          .align(Alignment.CenterStart)
                      ) {
                        if (timeline.videoClips.isEmpty()) {
                          Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF161E2E),
                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                            modifier = Modifier
                              .width(136.dp)
                              .height(videoTrackHeight)
                              .clickable { onAddMedia?.invoke() }
                              .testTag("add_first_media_btn")
                          ) {
                            Row(
                              modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.Center
                            ) {
                              Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape).background(CyanAccent),
                                contentAlignment = Alignment.Center
                              ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Media", tint = Color.Black, modifier = Modifier.size(16.dp))
                              }
                              Spacer(modifier = Modifier.width(8.dp))
                              Text("Add Media", style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                            }
                          }
                        } else {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                              shape = RoundedCornerShape(6.dp),
                              color = Color(0xFF1E222D),
                              border = BorderStroke(1.dp, Color(0xFF2D3344)),
                              modifier = Modifier
                                .width(110.dp)
                                .height(videoTrackHeight)
                                .clickable { onAddMedia?.invoke() }
                                .testTag("add_ending_btn")
                            ) {
                              Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                              ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add ending", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium, fontSize = 11.5.sp))
                              }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                              shape = RoundedCornerShape(6.dp),
                              color = Color.White,
                              modifier = Modifier
                                .size(38.dp)
                                .clickable { onAddMedia?.invoke() }
                                .testTag("add_media_square_btn")
                            ) {
                              Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "Add Media", tint = Color.Black, modifier = Modifier.size(22.dp))
                              }
                            }
                          }
                        }
                      }
                    }
                  }

                  // ==========================================
                  // 2. OVERLAY / PIP TRACK (44.dp)
                  // ==========================================
                  if (timeline.overlayClips.isNotEmpty()) {
                    val overlayTrackHeight = 44.dp
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(overlayTrackHeight)
                        .testTag("overlay_track_lane")
                    ) {
                      Box(
                        modifier = Modifier
                          .fillMaxHeight()
                          .offset(x = centerPaddingDp)
                      ) {
                        timeline.overlayClips.forEach { clip ->
                          val isSelected = (selectedElement as? SelectedTrackElement.Overlay)?.clipId == clip.id
                          val isMulti = clip.id in selectedClipIds

                          TimelineClipView(
                            clipId = clip.id,
                            title = clip.name.ifBlank { "Overlay" },
                            timelineStartMs = clip.timelineStartMs,
                            durationMs = clip.durationMs,
                            sourceStartMs = clip.sourceStartMs,
                            sourceEndMs = clip.sourceEndMs,
                            currentPlayheadMs = currentPosMs,
                            hasAudio = clip.hasAudio,
                            trackColor = OverlayTrackColor,
                            heightDp = overlayTrackHeight,
                            msPerPixel = msPerPixel,
                            isSelected = isSelected,
                            isMultiSelected = isMulti,
                            isLocked = false,
                            speed = clip.speed,
                            isVideoClip = clip.isVideo,
                            uri = clip.uri,
                            isVideo = clip.isVideo,
                            onSelect = {
                              if (isMultiSelectMode) onToggleClipSelection(clip.id)
                              else {
                                onSelectElement(SelectedTrackElement.Overlay(clip.id))
                                onSeek(clip.timelineStartMs)
                              }
                            },
                            onLongClick = { onToggleClipSelection(clip.id) },
                            onMoveClip = { delta -> onMoveClip(clip.id, delta) },
                            onTrimLeft = { delta -> onTrimClipLeft(clip.id, delta) },
                            onTrimRight = { delta -> onTrimClipRight(clip.id, delta) }
                          )
                        }

                        val maxOverlayEndMs = timeline.overlayClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
                        val addOverlayOffset = (maxOverlayEndMs / msPerPixel).dp + 8.dp

                        Box(
                          modifier = Modifier
                            .offset(x = addOverlayOffset)
                            .align(Alignment.CenterStart)
                        ) {
                          Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E222D),
                            border = BorderStroke(1.dp, Color(0xFF2D3344)),
                            modifier = Modifier
                              .width(115.dp)
                              .height(32.dp)
                              .clickable { onAddOverlay?.invoke() }
                              .testTag("add_overlay_pill_btn")
                          ) {
                            Row(
                              modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                              verticalAlignment = Alignment.CenterVertically
                            ) {
                              Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                              Spacer(modifier = Modifier.width(4.dp))
                              Text("Add overlay", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium))
                            }
                          }
                        }
                      }
                    }
                  }

                  // ==========================================
                  // 3. AUDIO TRACK (+ Add audio button card / waveforms)
                  // ==========================================
                  if (timeline.audioClips.isNotEmpty()) {
                    val audioTrackHeight = 36.dp
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(audioTrackHeight)
                        .testTag("audio_track_lane")
                    ) {
                      Box(
                        modifier = Modifier
                          .fillMaxHeight()
                          .offset(x = centerPaddingDp)
                      ) {
                        timeline.audioClips.forEach { clip ->
                          val isSelected = (selectedElement as? SelectedTrackElement.Audio)?.clipId == clip.id
                          val isMulti = clip.id in selectedClipIds

                          TimelineClipView(
                            clipId = clip.id,
                            title = clip.title.ifBlank { "Audio" },
                            timelineStartMs = clip.timelineStartMs,
                            durationMs = clip.durationMs,
                            trackColor = AudioTrackColor,
                            heightDp = audioTrackHeight,
                            msPerPixel = msPerPixel,
                            isSelected = isSelected,
                            isMultiSelected = isMulti,
                            isLocked = false,
                            waveformData = clip.waveformData,
                            waveformStyle = waveformStyle,
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

                        val maxAudioEndMs = timeline.audioClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
                        val addAudioOffset = (maxAudioEndMs / msPerPixel).dp + 8.dp

                        Box(
                          modifier = Modifier
                            .offset(x = addAudioOffset)
                            .align(Alignment.CenterStart)
                        ) {
                          Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E222D),
                            border = BorderStroke(1.dp, Color(0xFF2D3344)),
                            modifier = Modifier
                              .width(120.dp)
                              .height(34.dp)
                              .clickable { onAddAudio?.invoke() }
                              .testTag("add_audio_pill_btn")
                          ) {
                            Row(
                              modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                              verticalAlignment = Alignment.CenterVertically
                            ) {
                              Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                              Spacer(modifier = Modifier.width(6.dp))
                              Text("Add audio", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium, fontSize = 11.5.sp))
                            }
                          }
                        }
                      }
                    }
                  }

                  // ==========================================
                  // 4. TEXT TRACK (+ Add text button card / text cards)
                  // ==========================================
                  if (timeline.textClips.isNotEmpty()) {
                    val textTrackHeight = 36.dp
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(textTrackHeight)
                        .testTag("text_track_lane")
                    ) {
                      Box(
                        modifier = Modifier
                          .fillMaxHeight()
                          .offset(x = centerPaddingDp)
                      ) {
                        timeline.textClips.forEach { clip ->
                          val isSelected = (selectedElement as? SelectedTrackElement.Text)?.clipId == clip.id
                          val isMulti = clip.id in selectedClipIds

                          TimelineClipView(
                            clipId = clip.id,
                            title = clip.text.ifBlank { "Text" },
                            timelineStartMs = clip.timelineStartMs,
                            durationMs = clip.durationMs,
                            trackColor = TextTrackColor,
                            heightDp = textTrackHeight,
                            msPerPixel = msPerPixel,
                            isSelected = isSelected,
                            isMultiSelected = isMulti,
                            isLocked = false,
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

                        val maxTextEndMs = timeline.textClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
                        val addTextOffset = (maxTextEndMs / msPerPixel).dp + 8.dp

                        Box(
                          modifier = Modifier
                            .offset(x = addTextOffset)
                            .align(Alignment.CenterStart)
                        ) {
                          Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E222D),
                            border = BorderStroke(1.dp, Color(0xFF2D3344)),
                            modifier = Modifier
                              .width(120.dp)
                              .height(34.dp)
                              .clickable { onAddText?.invoke() }
                              .testTag("add_text_pill_btn")
                          ) {
                            Row(
                              modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                              verticalAlignment = Alignment.CenterVertically
                            ) {
                              Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                              Spacer(modifier = Modifier.width(6.dp))
                              Text("Add text", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium, fontSize = 11.5.sp))
                            }
                          }
                        }
                      }
                    }
                  }

                  // ==========================================
                  // 5. STICKER TRACK (36.dp)
                  // ==========================================
                  if (timeline.stickerClips.isNotEmpty()) {
                    val stickerTrackHeight = 36.dp
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(stickerTrackHeight)
                        .testTag("sticker_track_lane")
                    ) {
                      Box(
                        modifier = Modifier
                          .fillMaxHeight()
                          .offset(x = centerPaddingDp)
                      ) {
                        timeline.stickerClips.forEach { clip ->
                          val isSelected = (selectedElement as? SelectedTrackElement.Sticker)?.clipId == clip.id
                          val isMulti = clip.id in selectedClipIds

                          TimelineClipView(
                            clipId = clip.id,
                            title = clip.emojiOrAsset.ifBlank { "Sticker" },
                            timelineStartMs = clip.timelineStartMs,
                            durationMs = clip.durationMs,
                            trackColor = StickerTrackColor,
                            heightDp = stickerTrackHeight,
                            msPerPixel = msPerPixel,
                            isSelected = isSelected,
                            isMultiSelected = isMulti,
                            isLocked = false,
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

                        val maxStickerEndMs = timeline.stickerClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
                        val addStickerOffset = (maxStickerEndMs / msPerPixel).dp + 8.dp

                        Box(
                          modifier = Modifier
                            .offset(x = addStickerOffset)
                            .align(Alignment.CenterStart)
                        ) {
                          Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E222D),
                            border = BorderStroke(1.dp, Color(0xFF2D3344)),
                            modifier = Modifier
                              .width(120.dp)
                              .height(34.dp)
                              .clickable { onAddSticker?.invoke() }
                              .testTag("add_sticker_pill_btn")
                          ) {
                            Row(
                              modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                              verticalAlignment = Alignment.CenterVertically
                            ) {
                              Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                              Spacer(modifier = Modifier.width(6.dp))
                              Text("Add sticker", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium, fontSize = 11.5.sp))
                            }
                          }
                        }
                      }
                    }
                  }

                  // ==========================================
                  // 6. EFFECT TRACK (36.dp)
                  // ==========================================
                  if (timeline.effectClips.isNotEmpty()) {
                    val effectTrackHeight = 36.dp
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(effectTrackHeight)
                        .testTag("effect_track_lane")
                    ) {
                      Box(
                        modifier = Modifier
                          .fillMaxHeight()
                          .offset(x = centerPaddingDp)
                      ) {
                        timeline.effectClips.forEach { clip ->
                          val isSelected = (selectedElement as? SelectedTrackElement.Effect)?.clipId == clip.id
                          val isMulti = clip.id in selectedClipIds

                          TimelineClipView(
                            clipId = clip.id,
                            title = clip.effectType.displayName,
                            timelineStartMs = clip.timelineStartMs,
                            durationMs = clip.durationMs,
                            trackColor = EffectTrackColor,
                            heightDp = effectTrackHeight,
                            msPerPixel = msPerPixel,
                            isSelected = isSelected,
                            isMultiSelected = isMulti,
                            isLocked = false,
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

                        val maxEffectEndMs = timeline.effectClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
                        val addEffectOffset = (maxEffectEndMs / msPerPixel).dp + 8.dp

                        Box(
                          modifier = Modifier
                            .offset(x = addEffectOffset)
                            .align(Alignment.CenterStart)
                        ) {
                          Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E222D),
                            border = BorderStroke(1.dp, Color(0xFF2D3344)),
                            modifier = Modifier
                              .width(120.dp)
                              .height(34.dp)
                              .clickable { onAddEffect?.invoke() }
                              .testTag("add_effect_pill_btn")
                          ) {
                            Row(
                              modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                              verticalAlignment = Alignment.CenterVertically
                            ) {
                              Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                              Spacer(modifier = Modifier.width(6.dp))
                              Text("Add effect", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium, fontSize = 11.5.sp))
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

      // ==========================================
      // FIXED CENTER CTI OVERLAY (Stationed Center Playhead)
      // ==========================================
      if (hasAnyTrack) {
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 88.dp)
            .width(timelineViewportWidthDp)
            .fillMaxHeight()
        ) {
          // Vertical Center Playhead Line
          Box(
            modifier = Modifier
              .align(Alignment.Center)
              .fillMaxHeight()
              .width(2.dp)
              .background(Color.White)
              .border(0.5.dp, Color.Black.copy(alpha = 0.5f))
              .testTag("fixed_center_playhead_line")
          )

          // CTI Top Needle Cap Badge on Ruler
          Box(
            modifier = Modifier
              .align(Alignment.TopCenter)
              .offset(y = 1.dp)
              .width(11.dp)
              .height(18.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(Color.White)
              .border(1.dp, Color(0xFF1E222D), RoundedCornerShape(3.dp))
              .testTag("fixed_center_playhead_cap"),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .width(1.5.dp)
                .height(10.dp)
                .background(Color.Black.copy(alpha = 0.5f))
            )
          }

          // Floating Frame & Timecode Tooltip Bubble when Touch Scrubbing
          if (isTouchScrubbing) {
            val frameNum = (currentPosMs / (1000.0 / fps)).toLong()
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color.Black.copy(alpha = 0.92f),
              border = BorderStroke(1.dp, AmberAccent),
              modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 22.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(
                  text = formatDurationShort(currentPosMs),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberAccent
                  )
                )
                Text(
                  text = "F$frameNum",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                  )
                )
              }
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
