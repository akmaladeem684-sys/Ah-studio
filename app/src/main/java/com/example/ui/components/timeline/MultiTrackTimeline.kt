package com.example.ui.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState as rememberVerticalScrollState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.Timeline
import com.example.domain.model.TrackHeight
import com.example.domain.model.TrackSettings
import com.example.domain.model.TrackType
import com.example.engine.SelectedTrackElement
import com.example.engine.TimelineEngine
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
  modifier: Modifier = Modifier
) {
  val horizontalScrollState = rememberScrollState()
  val verticalScrollState = rememberVerticalScrollState()

  val totalDuration = timeline.totalDurationMs.coerceAtLeast(10000L)
  val msPerPixel = remember(zoom) { (20f / zoom).coerceIn(2.5f, 120f) }
  val totalWidthDp = (totalDuration / msPerPixel).dp

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
    Row(modifier = Modifier.fillMaxSize()) {
      // 1. Left Sticky Track Headers Column
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
        // Top empty space aligned with the timecode ruler
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

      // 2. Right Horizontally & Vertically Scrollable Tracks Area
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          // Pinch-to-zoom gesture on the timeline area
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

                Box(
                  modifier = Modifier
                    .width(totalWidthDp)
                    .height(trackHeightDp)
                    .background(if (settings.isHidden) Color(0xFF0F1522) else Color.Transparent)
                    .drawBehind {
                      drawLine(
                        color = StudioBorder.copy(alpha = 0.35f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 0.5.dp.toPx()
                      )
                    }
                    .padding(vertical = 3.dp)
                ) {
                  when (trackType) {
                    TrackType.MAIN_VIDEO -> {
                      timeline.videoClips.forEach { clip ->
                        val isSelected = (selectedElement as? SelectedTrackElement.Video)?.clipId == clip.id
                        val isMulti = clip.id in selectedClipIds
                        TimelineClipView(
                          clipId = clip.id,
                          title = clip.name,
                          timelineStartMs = clip.timelineStartMs,
                          durationMs = clip.durationMs,
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
                          onSelect = {
                            if (isMultiSelectMode) onToggleClipSelection(clip.id)
                            else onSelectElement(SelectedTrackElement.Video(clip.id))
                          },
                          onLongClick = { onToggleClipSelection(clip.id) },
                          onMoveClip = { delta -> onMoveClip(clip.id, delta) },
                          onTrimLeft = { delta -> onTrimClipLeft(clip.id, delta) },
                          onTrimRight = { delta -> onTrimClipRight(clip.id, delta) }
                        )
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
                          speed = clip.speed,
                          isReversed = clip.isReversed,
                          keyframes = clip.keyframes,
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
                          trackColor = AudioTrackColor,
                          heightDp = trackHeightDp,
                          msPerPixel = msPerPixel,
                          isSelected = isSelected,
                          isMultiSelected = isMulti,
                          isLocked = settings.isLocked,
                          waveformData = clip.waveformData,
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

            // Playhead Red Needle (Inside the scrollable area so it moves in absolute sync)
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
