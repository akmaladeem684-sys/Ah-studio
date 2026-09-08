package com.example.ui.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ClipKeyframe
import com.example.engine.audio.AudioWaveformManager
import com.example.ui.components.formatDurationShort
import com.example.ui.theme.*

@Composable
fun TimelineClipView(
  clipId: String,
  title: String,
  timelineStartMs: Long,
  durationMs: Long,
  trackColor: Color,
  heightDp: Dp,
  msPerPixel: Float,
  isSelected: Boolean,
  isMultiSelected: Boolean,
  isLocked: Boolean,
  speed: Float = 1.0f,
  isReversed: Boolean = false,
  isFreeze: Boolean = false,
  waveformData: List<Float> = emptyList(),
  sourceStartMs: Long = 0L,
  sourceEndMs: Long = durationMs,
  hasAudio: Boolean = false,
  isMuted: Boolean = false,
  currentPlayheadMs: Long? = null,
  waveformStyle: WaveformStyle = WaveformStyle.MIRRORED_BARS,
  keyframes: List<ClipKeyframe> = emptyList(),
  selectedKeyframeIds: Set<String> = emptySet(),
  onSelectKeyframe: ((String) -> Unit)? = null,
  onMoveKeyframe: ((String, Long) -> Unit)? = null,
  onSelect: () -> Unit,
  onLongClick: () -> Unit,
  onMoveClip: (deltaMs: Long) -> Unit,
  onTrimLeft: (deltaMs: Long) -> Unit,
  onTrimRight: (deltaMs: Long) -> Unit,
  modifier: Modifier = Modifier
) {
  val startPx = (timelineStartMs / msPerPixel).dp
  val widthPx = (durationMs / msPerPixel).dp.coerceAtLeast(28.dp)

  // Waveform analysis & dynamic slicing for trimmed clips
  val effectiveWaveform = remember(clipId, waveformData, durationMs, sourceStartMs, sourceEndMs, hasAudio) {
    if (waveformData.isNotEmpty()) {
      AudioWaveformManager.sliceForTrim(waveformData, sourceStartMs, sourceEndMs, durationMs)
    } else if (hasAudio) {
      val full = AudioWaveformManager.getOrGenerateWaveform(clipId, clipId, title, durationMs)
      AudioWaveformManager.sliceForTrim(full, sourceStartMs, sourceEndMs, durationMs)
    } else {
      emptyList()
    }
  }

  val waveformAnalysis = remember(effectiveWaveform, durationMs) {
    if (effectiveWaveform.isNotEmpty()) {
      AudioWaveformManager.analyzeWaveform(effectiveWaveform, durationMs)
    } else null
  }

  val relPlayheadMs = remember(currentPlayheadMs, timelineStartMs, durationMs) {
    currentPlayheadMs?.let { ph ->
      if (ph in timelineStartMs..(timelineStartMs + durationMs)) {
        ph - timelineStartMs
      } else null
    }
  }

  val isPlayheadOnPeak = remember(relPlayheadMs, waveformAnalysis) {
    if (relPlayheadMs != null && waveformAnalysis != null) {
      AudioWaveformManager.findNearestPeak(relPlayheadMs, waveformAnalysis.peaks, snapThresholdMs = 80L) != null
    } else false
  }

  // Accumulated drag distances to ensure precision and prevent accidental displacement
  var dragAccumulatorX by remember { mutableFloatStateOf(0f) }

  Box(
    modifier = modifier
      .offset(x = startPx)
      .width(widthPx)
      .height(heightDp - 6.dp)
      .clip(RoundedCornerShape(6.dp))
      .alpha(if (isLocked) 0.55f else 1.0f)
      .background(
        Brush.horizontalGradient(
          listOf(trackColor.copy(alpha = 0.85f), trackColor.copy(alpha = 0.65f))
        )
      )
      .border(
        width = if (isSelected || isMultiSelected) 2.dp else 1.dp,
        color = when {
          isSelected -> CyanAccent
          isMultiSelected -> AmberAccent
          isLocked -> StudioBorder
          else -> trackColor.copy(alpha = 0.9f)
        },
        shape = RoundedCornerShape(6.dp)
      )
      .testTag("clip_$clipId")
  ) {
    // 1. Audio Waveform Canvas Layer (renders full clip width & height)
    if (effectiveWaveform.isNotEmpty()) {
      AudioWaveformCanvas(
        waveformData = effectiveWaveform,
        peaks = waveformAnalysis?.peaks ?: emptyList(),
        clipDurationMs = durationMs,
        playheadPosMs = relPlayheadMs,
        trackColor = trackColor,
        peakColor = AmberAccent,
        crestColor = CyanAccent,
        style = waveformStyle,
        showPeakGuides = true,
        showCenterLine = true,
        isMuted = isMuted,
        modifier = Modifier.fillMaxSize()
      )
    }

    // Body gesture detector: tap to select, long-press for multi-select, drag to move
    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(clipId, isLocked) {
          if (!isLocked) {
            detectTapGestures(
              onTap = { onSelect() },
              onLongPress = { onLongClick() }
            )
          } else {
            detectTapGestures(onTap = { onSelect() })
          }
        }
        .pointerInput(clipId, isLocked, msPerPixel) {
          if (!isLocked) {
            detectDragGestures(
              onDragStart = { dragAccumulatorX = 0f },
              onDrag = { change, dragAmount ->
                change.consume()
                dragAccumulatorX += dragAmount.x
                val deltaMs = (dragAccumulatorX * msPerPixel).toLong()
                if (kotlin.math.abs(deltaMs) >= 15L) {
                  onMoveClip(deltaMs)
                  dragAccumulatorX = 0f
                }
              }
            )
          }
        }
        .padding(horizontal = if (isSelected) 8.dp else 4.dp, vertical = 2.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Left info: title + badges with semi-transparent contrast pill
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .weight(1f, fill = false)
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
          if (isMultiSelected) {
            Box(
              modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(AmberAccent),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(9.dp)
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
          }

          Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
              color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          if (speed != 1.0f) {
            Spacer(modifier = Modifier.width(3.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
              Text(
                text = "${speed}x",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  color = CyanAccent,
                  fontWeight = FontWeight.Bold
                )
              )
            }
          }

          if (isReversed) {
            Spacer(modifier = Modifier.width(3.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
              Text(
                text = "REV",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  color = PinkAccent,
                  fontWeight = FontWeight.Bold
                )
              )
            }
          }

          if (isFreeze) {
            Spacer(modifier = Modifier.width(3.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
              Text(
                text = "❄",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = CyanAccent)
              )
            }
          }

          // Peak badge when waveform analysis found audio peaks
          if (waveformAnalysis != null && waveformAnalysis.peaks.isNotEmpty() && isSelected) {
            Spacer(modifier = Modifier.width(3.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(AmberAccent.copy(alpha = 0.25f))
                .border(0.5.dp, AmberAccent.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
              Text(
                text = "⚡ ${waveformAnalysis.peaks.size}p",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  color = AmberAccent,
                  fontWeight = FontWeight.Bold
                )
              )
            }
          }
        }

        // Center / Right beat snap notification
        if (isPlayheadOnPeak) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(AmberAccent.copy(alpha = 0.85f))
              .padding(horizontal = 4.dp, vertical = 1.dp)
          ) {
            Text(
              text = "🎯 BEAT SNAP",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
              )
            )
          }
        }

        // Right info: Duration in clean dark chip
        Text(
          text = formatDurationShort(durationMs),
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.95f)
          ),
          modifier = Modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
        )
      }

      // Keyframe diamonds along the bottom of the clip
      if (keyframes.isNotEmpty()) {
        keyframes.forEach { kf ->
          val kfX = (kf.timeMs / msPerPixel).dp
          val isKfSelected = kf.id in selectedKeyframeIds
          var kfDragAccumulator by remember(kf.id) { mutableFloatStateOf(0f) }

          Box(
            modifier = Modifier
              .offset(x = kfX - 8.dp, y = heightDp - 22.dp)
              .size(18.dp)
              .clip(CircleShape)
              .clickable { onSelectKeyframe?.invoke(kf.id) }
              .pointerInput(kf.id, msPerPixel) {
                detectDragGestures(
                  onDragStart = {
                    kfDragAccumulator = 0f
                    onSelectKeyframe?.invoke(kf.id)
                  },
                  onDrag = { change, dragAmount ->
                    change.consume()
                    kfDragAccumulator += dragAmount.x
                    val deltaMs = (kfDragAccumulator * msPerPixel).toLong()
                    if (kotlin.math.abs(deltaMs) >= 15L) {
                      val newTime = (kf.timeMs + deltaMs).coerceIn(0L, durationMs)
                      onMoveKeyframe?.invoke(kf.id, newTime)
                      kfDragAccumulator = 0f
                    }
                  }
                )
              }
              .testTag("keyframe_diamond_${kf.id}"),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(if (isKfSelected) 11.dp else 8.dp)
                .rotate(45f)
                .background(
                  color = if (isKfSelected) PurpleAccent else AmberAccent,
                  shape = RoundedCornerShape(1.dp)
                )
                .border(
                  width = if (isKfSelected) 1.5.dp else 0.5.dp,
                  color = if (isKfSelected) Color.White else Color.Black.copy(alpha = 0.7f),
                  shape = RoundedCornerShape(1.dp)
                )
            )
          }
        }
      }
    }

    // Left Trim Handle (Visible when selected and not locked)
    if (isSelected && !isLocked) {
      var leftTrimAccumulator by remember { mutableFloatStateOf(0f) }
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .width(14.dp)
          .fillMaxHeight()
          .background(CyanAccent)
          .testTag("trim_left_$clipId")
          .pointerInput(clipId, msPerPixel) {
            detectDragGestures(
              onDragStart = { leftTrimAccumulator = 0f },
              onDrag = { change, dragAmount ->
                change.consume()
                leftTrimAccumulator += dragAmount.x
                val deltaMs = (leftTrimAccumulator * msPerPixel).toLong()
                if (kotlin.math.abs(deltaMs) >= 15L) {
                  onTrimLeft(deltaMs)
                  leftTrimAccumulator = 0f
                }
              }
            )
          },
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .width(2.dp)
            .height(14.dp)
            .background(Color.Black.copy(alpha = 0.7f))
        )
      }
    }

    // Right Trim Handle (Visible when selected and not locked)
    if (isSelected && !isLocked) {
      var rightTrimAccumulator by remember { mutableFloatStateOf(0f) }
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .width(14.dp)
          .fillMaxHeight()
          .background(CyanAccent)
          .testTag("trim_right_$clipId")
          .pointerInput(clipId, msPerPixel) {
            detectDragGestures(
              onDragStart = { rightTrimAccumulator = 0f },
              onDrag = { change, dragAmount ->
                change.consume()
                rightTrimAccumulator += dragAmount.x
                val deltaMs = (rightTrimAccumulator * msPerPixel).toLong()
                if (kotlin.math.abs(deltaMs) >= 15L) {
                  onTrimRight(deltaMs)
                  rightTrimAccumulator = 0f
                }
              }
            )
          },
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .width(2.dp)
            .height(14.dp)
            .background(Color.Black.copy(alpha = 0.7f))
        )
      }
    }
  }
}
