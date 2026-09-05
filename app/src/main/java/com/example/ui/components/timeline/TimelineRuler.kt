package com.example.ui.components.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.floor

@Composable
fun AccurateTimecodeRuler(
  totalDurationMs: Long,
  currentPosMs: Long,
  msPerPixel: Float,
  onSeek: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  val safeTotalDuration = totalDurationMs.coerceAtLeast(10000L)
  val rulerWidthDp = (safeTotalDuration / msPerPixel).dp

  // Dynamic tick calculation based on zoom level (msPerPixel)
  val (majorIntervalMs, minorIntervalMs) = remember(msPerPixel) {
    when {
      msPerPixel <= 6f -> 1000L to 200L
      msPerPixel <= 15f -> 1000L to 500L
      msPerPixel <= 35f -> 2000L to 1000L
      msPerPixel <= 80f -> 5000L to 1000L
      else -> 10000L to 2000L
    }
  }

  Box(
    modifier = modifier
      .width(rulerWidthDp)
      .height(30.dp)
      .background(StudioSurface)
      .testTag("timeline_timecode_ruler")
      .pointerInput(safeTotalDuration, msPerPixel) {
        detectTapGestures { offset ->
          val clickedMs = (offset.x * msPerPixel).toLong().coerceIn(0L, safeTotalDuration)
          onSeek(clickedMs)
        }
      }
      .pointerInput(safeTotalDuration, msPerPixel) {
        detectDragGestures { change, _ ->
          change.consume()
          val draggedMs = (change.position.x * msPerPixel).toLong().coerceIn(0L, safeTotalDuration)
          onSeek(draggedMs)
        }
      }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasWidth = size.width
      val canvasHeight = size.height

      // Bottom border line
      drawLine(
        color = StudioBorder,
        start = Offset(0f, canvasHeight),
        end = Offset(canvasWidth, canvasHeight),
        strokeWidth = 1f
      )

      val totalTicks = (safeTotalDuration / minorIntervalMs).toInt()
      val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(200, 148, 163, 184) // TextSecondary
        textSize = 9.sp.toPx()
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
      }

      for (i in 0..totalTicks) {
        val tickTimeMs = i * minorIntervalMs
        val x = tickTimeMs / msPerPixel
        val isMajor = tickTimeMs % majorIntervalMs == 0L

        val tickHeight = if (isMajor) canvasHeight * 0.45f else canvasHeight * 0.25f
        val tickColor = if (isMajor) TextSecondary.copy(alpha = 0.8f) else TextTertiary.copy(alpha = 0.4f)

        drawLine(
          color = tickColor,
          start = Offset(x, canvasHeight - tickHeight),
          end = Offset(x, canvasHeight),
          strokeWidth = if (isMajor) 1.5f else 1.0f
        )

        if (isMajor) {
          val label = formatTimecodeRuler(tickTimeMs)
          drawContext.canvas.nativeCanvas.drawText(
            label,
            x + 4f,
            canvasHeight - tickHeight - 3f,
            textPaint
          )
        }
      }

      // Draw ruler playhead marker (head of the needle)
      val playheadX = currentPosMs / msPerPixel
      val headPath = Path().apply {
        moveTo(playheadX - 6.dp.toPx(), 0f)
        lineTo(playheadX + 6.dp.toPx(), 0f)
        lineTo(playheadX + 6.dp.toPx(), canvasHeight * 0.5f)
        lineTo(playheadX, canvasHeight)
        lineTo(playheadX - 6.dp.toPx(), canvasHeight * 0.5f)
        close()
      }
      drawPath(headPath, RedAccent)
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
