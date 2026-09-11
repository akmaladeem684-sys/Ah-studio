package com.example.ui.components.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun AccurateTimecodeRuler(
  totalDurationMs: Long,
  currentPosMs: Long,
  msPerPixel: Float,
  onSeek: (Long) -> Unit,
  fps: Int = 30,
  isFrameSnapping: Boolean = false,
  onDoubleTapSnap: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val safeTotalDuration = totalDurationMs.coerceAtLeast(10000L)
  val rulerWidthDp = (safeTotalDuration / msPerPixel).dp
  val frameDurationMs = 1000.0 / fps

  var isScrubbing by remember { mutableStateOf(false) }
  var scrubPreviewMs by remember { mutableLongStateOf(currentPosMs) }

  // Dynamic tick calculation based on zoom level (msPerPixel)
  val (majorIntervalMs, minorIntervalMs, showFrameTicks) = remember(msPerPixel, fps) {
    when {
      msPerPixel <= 3f -> Triple(500L, (1000L / fps).coerceAtLeast(1L), true) // Sub-frame/Frame precision
      msPerPixel <= 8f -> Triple(1000L, (1000L / fps).coerceAtLeast(1L), true) // 1 frame precision
      msPerPixel <= 18f -> Triple(1000L, 200L, false)
      msPerPixel <= 35f -> Triple(2000L, 500L, false)
      msPerPixel <= 80f -> Triple(5000L, 1000L, false)
      else -> Triple(10000L, 2000L, false)
    }
  }

  Box(
    modifier = modifier
      .width(rulerWidthDp)
      .height(34.dp)
      .background(StudioSurface)
      .testTag("timeline_timecode_ruler")
      .pointerInput(safeTotalDuration, msPerPixel, isFrameSnapping, fps) {
        detectTapGestures(
          onDoubleTap = { offset ->
            if (onDoubleTapSnap != null) {
              onDoubleTapSnap()
            } else {
              val rawMs = (offset.x * msPerPixel).toLong().coerceIn(0L, safeTotalDuration)
              val targetMs = if (isFrameSnapping) {
                (Math.round(rawMs / frameDurationMs) * frameDurationMs).toLong()
              } else rawMs
              onSeek(targetMs)
            }
          },
          onTap = { offset ->
            val rawMs = (offset.x * msPerPixel).toLong().coerceIn(0L, safeTotalDuration)
            val targetMs = if (isFrameSnapping) {
              (Math.round(rawMs / frameDurationMs) * frameDurationMs).toLong()
            } else rawMs
            onSeek(targetMs)
          }
        )
      }
      .pointerInput(safeTotalDuration, msPerPixel, isFrameSnapping, fps) {
        detectDragGestures(
          onDragStart = { offset ->
            isScrubbing = true
            val rawMs = (offset.x * msPerPixel).toLong().coerceIn(0L, safeTotalDuration)
            val targetMs = if (isFrameSnapping) {
              (Math.round(rawMs / frameDurationMs) * frameDurationMs).toLong()
            } else rawMs
            scrubPreviewMs = targetMs
            onSeek(targetMs)
          },
          onDragEnd = {
            isScrubbing = false
          },
          onDragCancel = {
            isScrubbing = false
          },
          onDrag = { change, _ ->
            change.consume()
            val rawMs = (change.position.x * msPerPixel).toLong().coerceIn(0L, safeTotalDuration)
            val targetMs = if (isFrameSnapping) {
              (Math.round(rawMs / frameDurationMs) * frameDurationMs).toLong()
            } else rawMs
            scrubPreviewMs = targetMs
            onSeek(targetMs)
          }
        )
      }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasWidth = size.width
      val canvasHeight = size.height

      // Subtle ruler top gradient background
      drawRect(
        brush = Brush.verticalGradient(
          listOf(StudioDarkBg, StudioSurface)
        )
      )

      // Bottom border line
      drawLine(
        color = StudioBorder,
        start = Offset(0f, canvasHeight),
        end = Offset(canvasWidth, canvasHeight),
        strokeWidth = 1.dp.toPx()
      )

      val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(220, 203, 213, 225) // Slate-300 for crisp readability
        textSize = 9.sp.toPx()
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
      }

      val frameTextPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(190, 6, 182, 212) // Cyan-500 for frame tags
        textSize = 7.5.sp.toPx()
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
      }

      val totalTicks = (safeTotalDuration / minorIntervalMs).toInt()

      for (i in 0..totalTicks) {
        val tickTimeMs = i * minorIntervalMs
        val x = tickTimeMs / msPerPixel
        val isMajor = tickTimeMs % majorIntervalMs == 0L
        val isHalfMajor = tickTimeMs % (majorIntervalMs / 2) == 0L

        val tickHeight = when {
          isMajor -> canvasHeight * 0.55f
          isHalfMajor -> canvasHeight * 0.35f
          showFrameTicks -> canvasHeight * 0.20f
          else -> canvasHeight * 0.25f
        }

        val tickColor = when {
          isMajor -> TextPrimary.copy(alpha = 0.9f)
          isHalfMajor -> TextSecondary.copy(alpha = 0.6f)
          showFrameTicks && (i % 5 == 0) -> CyanAccent.copy(alpha = 0.6f)
          else -> TextTertiary.copy(alpha = 0.35f)
        }

        drawLine(
          color = tickColor,
          start = Offset(x, canvasHeight - tickHeight),
          end = Offset(x, canvasHeight),
          strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
        )

        if (isMajor) {
          val label = formatTimecodeRuler(tickTimeMs)
          drawContext.canvas.nativeCanvas.drawText(
            label,
            x + 4f,
            canvasHeight - tickHeight - 4f,
            textPaint
          )
        } else if (showFrameTicks && isHalfMajor && msPerPixel <= 5f) {
          // Draw frame index mark
          val frameNum = msToFrameIndex(tickTimeMs, fps)
          drawContext.canvas.nativeCanvas.drawText(
            "F$frameNum",
            x + 3f,
            canvasHeight - tickHeight - 3f,
            frameTextPaint
          )
        }
      }
    }
  }
}

private fun formatTimecodeRuler(ms: Long): String {
  val totalSeconds = ms / 1000
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  val millis = ms % 1000
  return if (minutes > 0) {
    String.format("%02d:%02d", minutes, seconds)
  } else {
    String.format("%d.%ds", seconds, millis / 100)
  }
}
