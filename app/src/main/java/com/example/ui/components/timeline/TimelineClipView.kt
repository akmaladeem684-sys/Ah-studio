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
  keyframes: List<ClipKeyframe> = emptyList(),
  onSelect: () -> Unit,
  onLongClick: () -> Unit,
  onMoveClip: (deltaMs: Long) -> Unit,
  onTrimLeft: (deltaMs: Long) -> Unit,
  onTrimRight: (deltaMs: Long) -> Unit,
  modifier: Modifier = Modifier
) {
  val startPx = (timelineStartMs / msPerPixel).dp
  val widthPx = (durationMs / msPerPixel).dp.coerceAtLeast(28.dp)

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
        .padding(horizontal = if (isSelected) 10.dp else 6.dp, vertical = 2.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Left info: title + badges
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
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
                .background(Color.Black.copy(alpha = 0.4f))
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
                .background(Color.Black.copy(alpha = 0.4f))
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
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
              Text(
                text = "❄",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = CyanAccent)
              )
            }
          }
        }

        // Waveform preview for audio
        if (waveformData.isNotEmpty()) {
          Row(
            modifier = Modifier
              .weight(1f)
              .height(18.dp)
              .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            waveformData.take(30).forEach { amp ->
              Box(
                modifier = Modifier
                  .width(1.5.dp)
                  .height((amp * 16).dp.coerceAtLeast(2.dp))
                  .background(Color.White.copy(alpha = 0.75f))
              )
            }
          }
        }

        // Right info: Duration
        Text(
          text = formatDurationShort(durationMs),
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.9f)
          )
        )
      }

      // Keyframe diamonds along the bottom of the clip
      if (keyframes.isNotEmpty()) {
        keyframes.forEach { kf ->
          val kfX = (kf.timeMs / msPerPixel).dp
          Box(
            modifier = Modifier
              .offset(x = kfX - 4.dp, y = heightDp - 14.dp)
              .size(6.dp)
              .clip(RoundedCornerShape(1.dp))
              .background(AmberAccent)
          )
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
