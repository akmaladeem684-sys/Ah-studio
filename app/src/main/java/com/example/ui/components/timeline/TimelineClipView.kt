package com.example.ui.components.timeline

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ClipKeyframe
import com.example.engine.KeyframeInterpolator
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
  baseVolume: Float = 1.0f,
  fadeInMs: Long = 0L,
  fadeOutMs: Long = 0L,
  showVolumeEnvelope: Boolean = true,
  onSelectKeyframe: ((String) -> Unit)? = null,
  onMoveKeyframe: ((String, Long) -> Unit)? = null,
  onAddVolumeKeyframe: ((timeMs: Long, volume: Float) -> Unit)? = null,
  onUpdateVolumeKeyframe: ((keyframeId: String, newTimeMs: Long, newVolume: Float) -> Unit)? = null,
  onDeleteVolumeKeyframe: ((keyframeId: String) -> Unit)? = null,
  onSelect: () -> Unit,
  onLongClick: () -> Unit,
  onMoveClip: (deltaMs: Long) -> Unit,
  onTrimLeft: (deltaMs: Long) -> Unit,
  onTrimRight: (deltaMs: Long) -> Unit,
  clipIndex: Int? = null,
  totalClipsInTrack: Int = 1,
  isVideoClip: Boolean = false,
  isBeingReordered: Boolean = false,
  onMoveEarlier: (() -> Unit)? = null,
  onMoveLater: (() -> Unit)? = null,
  onStartReorderDrag: (() -> Unit)? = null,
  onReorderDrag: ((Float) -> Unit)? = null,
  onEndReorderDrag: (() -> Unit)? = null,
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
      .alpha(if (isBeingReordered) 0.85f else if (isLocked) 0.55f else 1.0f)
      .background(
        Brush.horizontalGradient(
          listOf(trackColor.copy(alpha = if (isBeingReordered) 0.95f else 0.85f), trackColor.copy(alpha = 0.65f))
        )
      )
      .border(
        width = if (isBeingReordered) 2.5.dp else if (isSelected || isMultiSelected) 2.dp else 1.dp,
        color = when {
          isBeingReordered -> AmberAccent
          isSelected -> CyanAccent
          isMultiSelected -> AmberAccent
          isLocked -> StudioBorder
          else -> trackColor.copy(alpha = 0.9f)
        },
        shape = RoundedCornerShape(6.dp)
      )
      .testTag("clip_$clipId")
  ) {
    // 0. Filmstrip Perforations Canvas (for video clips)
    if (isVideoClip) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val sprocketW = 5.dp.toPx()
        val sprocketH = 3.dp.toPx()
        val step = 12.dp.toPx()
        var cx = 4.dp.toPx()
        while (cx + sprocketW < size.width) {
          drawRoundRect(
            color = Color.Black.copy(alpha = 0.32f),
            topLeft = Offset(cx, 1.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(sprocketW, sprocketH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
          )
          drawRoundRect(
            color = Color.Black.copy(alpha = 0.32f),
            topLeft = Offset(cx, size.height - 1.dp.toPx() - sprocketH),
            size = androidx.compose.ui.geometry.Size(sprocketW, sprocketH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
          )
          cx += step
        }
      }
    }

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

    // 1.5. Volume Envelope Graph Overlay on Audio Clips
    if (hasAudio && showVolumeEnvelope) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .testTag("clip_envelope_$clipId")
      ) {
        val w = size.width
        val h = size.height
        val maxVol = 1.5f
        val topMargin = 6.dp.toPx()
        val bottomMargin = 6.dp.toPx()
        val usableH = (h - topMargin - bottomMargin).coerceAtLeast(8f)

        fun volToY(v: Float): Float = topMargin + (1f - (v / maxVol).coerceIn(0f, 1f)) * usableH

        // 100% (+0 dB) volume reference line
        val y100 = volToY(1.0f)
        drawLine(
          color = Color.White.copy(alpha = 0.22f),
          start = Offset(0f, y100),
          end = Offset(w, y100),
          strokeWidth = 1.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )

        // 0% (Mute) volume reference line
        val y0 = volToY(0.0f)
        drawLine(
          color = RedAccent.copy(alpha = 0.28f),
          start = Offset(0f, y0),
          end = Offset(w, y0),
          strokeWidth = 1.dp.toPx()
        )

        // Envelope curve path & gradient fill
        val curvePath = Path()
        val fillPath = Path()
        val steps = 70

        for (i in 0..steps) {
          val frac = i.toFloat() / steps.toFloat()
          val sampleTimeMs = (frac * durationMs).toLong()
          val vol = if (keyframes.isNotEmpty()) {
            KeyframeInterpolator.interpolateVolume(keyframes, sampleTimeMs, baseVolume)
          } else {
            var v = baseVolume
            if (fadeInMs > 0L && sampleTimeMs < fadeInMs) {
              v *= (sampleTimeMs.toFloat() / fadeInMs.toFloat()).coerceIn(0f, 1f)
            }
            if (fadeOutMs > 0L && (durationMs - sampleTimeMs) < fadeOutMs) {
              v *= ((durationMs - sampleTimeMs).toFloat() / fadeOutMs.toFloat()).coerceIn(0f, 1f)
            }
            v
          }

          val px = frac * w
          val py = volToY(vol)

          if (i == 0) {
            curvePath.moveTo(px, py)
            fillPath.moveTo(px, y0)
            fillPath.lineTo(px, py)
          } else {
            curvePath.lineTo(px, py)
            fillPath.lineTo(px, py)
          }
        }
        fillPath.lineTo(w, y0)
        fillPath.close()

        drawPath(
          path = fillPath,
          brush = Brush.verticalGradient(
            listOf(AudioTrackColor.copy(alpha = 0.28f), AudioTrackColor.copy(alpha = 0.04f)),
            startY = topMargin,
            endY = y0
          )
        )

        drawPath(
          path = curvePath,
          color = AudioTrackColor.copy(alpha = 0.95f),
          style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
      }
    }

    // Body gesture detector: tap to select, long-press for multi-select, drag to move
    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(clipId, isLocked, hasAudio) {
          if (!isLocked) {
            detectTapGestures(
              onTap = { onSelect() },
              onDoubleTap = { offset ->
                if (hasAudio && onAddVolumeKeyframe != null) {
                  val clickedMs = (offset.x * msPerPixel).toLong().coerceIn(0L, durationMs)
                  val normY = 1f - (offset.y / (heightDp.value * 2.5f)).coerceIn(0f, 1f)
                  val clickedVol = (normY * 1.5f).coerceIn(0f, 1.5f)
                  onAddVolumeKeyframe(clickedMs, clickedVol)
                }
              },
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
          if (clipIndex != null) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(if (isSelected) CyanAccent else Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
                .testTag("clip_index_${clipIndex}_$clipId")
            ) {
              Text(
                text = "#${clipIndex + 1}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) Color.Black else CyanAccent
                )
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
          }

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

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Reorder Nudge & Drag Handle when selected and not locked
          if (isSelected && isVideoClip && !isLocked && totalClipsInTrack > 1) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                .border(0.5.dp, CyanAccent.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 2.dp, vertical = 1.dp)
                .testTag("reorder_controls_$clipId")
            ) {
              if (onMoveEarlier != null) {
                Box(
                  modifier = Modifier
                    .size(18.dp)
                    .clickable { onMoveEarlier() }
                    .testTag("reorder_earlier_$clipId"),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Move Earlier",
                    tint = CyanAccent,
                    modifier = Modifier.size(12.dp)
                  )
                }
              }

              // Reorder Drag Handle
              Box(
                modifier = Modifier
                  .size(18.dp)
                  .pointerInput(clipId) {
                    detectDragGestures(
                      onDragStart = { onStartReorderDrag?.invoke() },
                      onDrag = { change, dragAmount ->
                        change.consume()
                        onReorderDrag?.invoke(dragAmount.x)
                      },
                      onDragEnd = { onEndReorderDrag?.invoke() },
                      onDragCancel = { onEndReorderDrag?.invoke() }
                    )
                  }
                  .testTag("reorder_handle_$clipId"),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.DragHandle,
                  contentDescription = "Drag to Reorder",
                  tint = AmberAccent,
                  modifier = Modifier.size(13.dp)
                )
              }

              if (onMoveLater != null) {
                Box(
                  modifier = Modifier
                    .size(18.dp)
                    .clickable { onMoveLater() }
                    .testTag("reorder_later_$clipId"),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Move Later",
                    tint = CyanAccent,
                    modifier = Modifier.size(12.dp)
                  )
                }
              }
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
      }

      // Keyframe diamonds along the clip (at bottom for video/overlay, or at volume level for audio)
      if (keyframes.isNotEmpty()) {
        keyframes.forEach { kf ->
          val kfX = (kf.timeMs / msPerPixel).dp
          val isKfSelected = kf.id in selectedKeyframeIds
          var kfDragAccumulatorX by remember(kf.id) { mutableFloatStateOf(0f) }
          var kfDragAccumulatorY by remember(kf.id) { mutableFloatStateOf(0f) }

          val kfY = if (hasAudio) {
            val normVol = (kf.volume / 1.5f).coerceIn(0f, 1f)
            ((1f - normVol) * (heightDp.value - 24f).coerceAtLeast(8f) + 4f).dp
          } else {
            heightDp - 22.dp
          }

          Box(
            modifier = Modifier
              .offset(x = kfX - 10.dp, y = kfY - 4.dp)
              .size(24.dp)
              .clip(CircleShape)
              .pointerInput(kf.id) {
                detectTapGestures(
                  onTap = { onSelectKeyframe?.invoke(kf.id) },
                  onDoubleTap = { onDeleteVolumeKeyframe?.invoke(kf.id) },
                  onLongPress = { onDeleteVolumeKeyframe?.invoke(kf.id) }
                )
              }
              .pointerInput(kf.id, msPerPixel, hasAudio) {
                detectDragGestures(
                  onDragStart = {
                    kfDragAccumulatorX = 0f
                    kfDragAccumulatorY = 0f
                    onSelectKeyframe?.invoke(kf.id)
                  },
                  onDrag = { change, dragAmount ->
                    change.consume()
                    kfDragAccumulatorX += dragAmount.x
                    kfDragAccumulatorY += dragAmount.y
                    val deltaMs = (kfDragAccumulatorX * msPerPixel).toLong()

                    if (hasAudio && onUpdateVolumeKeyframe != null) {
                      val trackUsableHeight = (heightDp.value - 24f).coerceAtLeast(10f)
                      val deltaVol = -(dragAmount.y / trackUsableHeight) * 1.5f
                      val newVol = (kf.volume + deltaVol).coerceIn(0f, 2f)
                      val newTime = (kf.timeMs + deltaMs).coerceIn(0L, durationMs)
                      onUpdateVolumeKeyframe(kf.id, newTime, newVol)
                      kfDragAccumulatorX = 0f
                    } else if (kotlin.math.abs(deltaMs) >= 15L) {
                      val newTime = (kf.timeMs + deltaMs).coerceIn(0L, durationMs)
                      onMoveKeyframe?.invoke(kf.id, newTime)
                      kfDragAccumulatorX = 0f
                    }
                  }
                )
              }
              .testTag("keyframe_diamond_${kf.id}"),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(if (isKfSelected) 12.dp else 9.dp)
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
          .width(16.dp)
          .fillMaxHeight()
          .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier
              .width(2.5.dp)
              .height(14.dp)
              .background(Color.Black.copy(alpha = 0.75f))
          )
        }
      }
    }

    // Right Trim Handle (Visible when selected and not locked)
    if (isSelected && !isLocked) {
      var rightTrimAccumulator by remember { mutableFloatStateOf(0f) }
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .width(16.dp)
          .fillMaxHeight()
          .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
          .background(AmberAccent)
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier
              .width(2.5.dp)
              .height(14.dp)
              .background(Color.Black.copy(alpha = 0.75f))
          )
        }
      }
    }
  }
}
