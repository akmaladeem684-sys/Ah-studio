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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
  onEditCover: (() -> Unit)? = null,
  onToggleMuteAllVideo: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val horizontalScrollState = rememberScrollState()
  val verticalScrollState = rememberScrollState()

  val totalDuration = timeline.totalDurationMs.coerceAtLeast(3000L)
  val msPerPixel = remember(zoom) { (20f / zoom).coerceIn(2.5f, 120f) }
  val totalWidthDp = (totalDuration / msPerPixel).dp + 220.dp

  // Reorder dragging state on the Video track
  var draggedVideoIndex by remember { mutableStateOf<Int?>(null) }
  var dragAccumulatorPx by remember { mutableFloatStateOf(0f) }
  var dropTargetIndex by remember { mutableStateOf<Int?>(null) }

  var isMutedAll by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .background(Color.Black),
    color = Color.Black
  ) {
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
            .width(88.dp)
            .fillMaxHeight()
            .padding(start = 12.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          Text(
            text = "${formatDurationShort(currentPosMs)} / ${formatDurationShort(timeline.totalDurationMs)}",
            style = MaterialTheme.typography.bodySmall.copy(
              color = Color.White.copy(alpha = 0.85f),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            modifier = Modifier.testTag("timeline_timecode_text")
          )
        }

        // Timeline Ruler with Time Markers & Dots
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .horizontalScroll(horizontalScrollState)
        ) {
          AccurateTimecodeRuler(
            totalDurationMs = totalDuration,
            currentPosMs = currentPosMs,
            msPerPixel = msPerPixel,
            fps = fps,
            isFrameSnapping = isFrameSnapping,
            onSeek = onSeek,
            onDoubleTapSnap = onSeekToNextCut
          )
        }
      }

      // Contextual Timeline Action Toolbar (Split, Delete, etc. when clip is selected)
      AnimatedVisibility(
        visible = selectedElement !is SelectedTrackElement.None,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
      ) {
        TimelineActionToolbar(
          hasSelection = true,
          isMultiSelectMode = isMultiSelectMode,
          selectedCount = if (selectedClipIds.isNotEmpty()) selectedClipIds.size else 1,
          canPaste = false,
          isMagnetic = false,
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

      // 2. Main Timeline Tracks View (Fixed Left Utility Column + Scrollable Multi-Track Lanes)
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxSize()) {
          // Left Static Column (Mute clip, Cover, Audio icon, Text icon)
          Column(
            modifier = Modifier
              .width(88.dp)
              .fillMaxHeight()
              .background(Color.Black)
              .padding(start = 6.dp, end = 6.dp)
              .verticalScroll(verticalScrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Row 1: Mute Clip & Cover Card (Video Track Left utility)
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

            // Row 2: Audio Track Icon (Left utility)
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
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
              )
            }

            // Row 3: Text Track Icon (Left utility)
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
                  color = Color.White.copy(alpha = 0.75f)
                )
              )
            }
          }

          // Right Horizontally Scrollable Tracks Area
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
            Box(
              modifier = Modifier
                .fillMaxSize()
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
                  Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    // Video Clips
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
                        val cutX = (cutPosMs / msPerPixel).dp - 10.dp
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
                            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
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

                    Spacer(modifier = Modifier.width(6.dp))

                    // "+ Add ending" card
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
                        Icon(
                          imageVector = Icons.Default.Add,
                          contentDescription = null,
                          tint = Color.White.copy(alpha = 0.8f),
                          modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = "Add ending",
                          style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp
                          )
                        )
                      }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // "+" White Square Media Add Button
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = Color.White,
                      modifier = Modifier
                        .size(38.dp)
                        .clickable { onAddMedia?.invoke() }
                        .testTag("add_media_square_btn")
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Default.Add,
                          contentDescription = "Add Media",
                          tint = Color.Black,
                          modifier = Modifier.size(22.dp)
                        )
                      }
                    }
                  }
                }

                // ==========================================
                // 2. AUDIO TRACK (+ Add audio button card / waveforms)
                // ==========================================
                val audioTrackHeight = 36.dp
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(audioTrackHeight)
                    .testTag("audio_track_lane")
                ) {
                  Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    if (timeline.audioClips.isNotEmpty()) {
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
                      Spacer(modifier = Modifier.width(6.dp))
                    }

                    // "+ Add audio" Pill Card
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = Color(0xFF1E222D),
                      border = BorderStroke(1.dp, Color(0xFF2D3344)),
                      modifier = Modifier
                        .width(130.dp)
                        .height(34.dp)
                        .clickable { onAddAudio?.invoke() }
                        .testTag("add_audio_pill_btn")
                    ) {
                      Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(
                          imageVector = Icons.Default.Add,
                          contentDescription = null,
                          tint = Color.White.copy(alpha = 0.7f),
                          modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = "Add audio",
                          style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp
                          )
                        )
                      }
                    }
                  }
                }

                // ==========================================
                // 3. TEXT TRACK (+ Add text button card / text cards)
                // ==========================================
                val textTrackHeight = 36.dp
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(textTrackHeight)
                    .testTag("text_track_lane")
                ) {
                  Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    if (timeline.textClips.isNotEmpty()) {
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
                      Spacer(modifier = Modifier.width(6.dp))
                    }

                    // "+ Add text" Pill Card
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = Color(0xFF1E222D),
                      border = BorderStroke(1.dp, Color(0xFF2D3344)),
                      modifier = Modifier
                        .width(130.dp)
                        .height(34.dp)
                        .clickable { onAddText?.invoke() }
                        .testTag("add_text_pill_btn")
                    ) {
                      Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(
                          imageVector = Icons.Default.Add,
                          contentDescription = null,
                          tint = Color.White.copy(alpha = 0.7f),
                          modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = "Add text",
                          style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp
                          )
                        )
                      }
                    }
                  }
                }
              }

              // Playhead White Needle (Moves smoothly in sync with timeline)
              val playheadX = (currentPosMs / msPerPixel).dp
              Box(
                modifier = Modifier
                  .offset(x = playheadX)
                  .width(2.dp)
                  .fillMaxHeight()
                  .background(Color.White)
                  .testTag("white_playhead_line")
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
