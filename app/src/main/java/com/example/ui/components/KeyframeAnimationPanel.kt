package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ClipKeyframe
import com.example.domain.model.KeyframeInterpolation
import com.example.engine.SelectedTrackElement
import com.example.ui.StudioViewModel
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RedAccent
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.util.Locale

private enum class KeyframeCategory {
  TRANSFORM,
  COLOR,
  AUDIO,
  EFFECT,
  CURVES
}

@Composable
fun KeyframeAnimationPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()
  val currentPosMs by viewModel.timelineEngine.currentPositionMs.collectAsState()
  val selectedKeyframeIds by viewModel.timelineEngine.selectedKeyframeIds.collectAsState()

  val activeClipData = remember(timeline, selectedElement) {
    viewModel.timelineEngine.getSelectedClipKeyframes()
  }

  var selectedCategory by remember { mutableStateOf(KeyframeCategory.TRANSFORM) }
  var linkScaleXY by remember { mutableStateOf(true) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .border(1.dp, StudioBorder)
      .padding(12.dp)
      .testTag("keyframe_animation_panel")
  ) {
    // Top Bar: Title, Selected Clip info, and Close
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(PurpleAccent.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Diamond, contentDescription = "Keyframe", tint = PurpleAccent, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "Keyframe Animation Studio",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
          )
          val clipLabel = when (selectedElement) {
            is SelectedTrackElement.Video -> "Main Video Clip"
            is SelectedTrackElement.Overlay -> "PIP Overlay Clip"
            is SelectedTrackElement.Audio -> "Audio Track"
            else -> "No clip selected"
          }
          Text(
            text = clipLabel,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
          )
        }
      }

      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    if (activeClipData == null) {
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(StudioSurfaceVariant)
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Layers, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(32.dp))
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Select a clip on the timeline to animate",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
          )
          Spacer(modifier = Modifier.height(8.dp))
          val firstVideo = timeline.videoClips.firstOrNull()
          if (firstVideo != null) {
            Button(
              onClick = {
                viewModel.timelineEngine.selectElement(SelectedTrackElement.Video(firstVideo.id))
              },
              colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
            ) {
              Text("Select Video Clip", fontSize = 12.sp)
            }
          }
        }
      }
      return
    }

    val keyframes = activeClipData.second
    val activeKeyframe = keyframes.find { it.id in selectedKeyframeIds }
      ?: viewModel.timelineEngine.getKeyframeAtPlayhead()
      ?: keyframes.firstOrNull()

    Spacer(modifier = Modifier.height(8.dp))

    // Keyframe Transport / Navigation Controls Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(StudioDarkBg)
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { viewModel.timelineEngine.jumpToPreviousKeyframe() },
          enabled = keyframes.isNotEmpty(),
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Keyframe", tint = if (keyframes.isNotEmpty()) CyanAccent else TextTertiary)
        }

        val keyframeAtHead = viewModel.timelineEngine.getKeyframeAtPlayhead()
        val hasKfAtHead = keyframeAtHead != null

        Button(
          onClick = {
            if (hasKfAtHead) {
              viewModel.timelineEngine.deleteSelectedKeyframes()
            } else {
              viewModel.timelineEngine.addKeyframeToSelectedClip()
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (hasKfAtHead) RedAccent.copy(alpha = 0.8f) else PurpleAccent,
            contentColor = Color.White
          ),
          modifier = Modifier.height(30.dp).testTag("keyframe_add_remove_toggle")
        ) {
          Icon(
            if (hasKfAtHead) Icons.Default.Delete else Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (hasKfAtHead) "Remove KF" else "Add Keyframe", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }

        IconButton(
          onClick = { viewModel.timelineEngine.jumpToNextKeyframe() },
          enabled = keyframes.isNotEmpty(),
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.SkipNext, contentDescription = "Next Keyframe", tint = if (keyframes.isNotEmpty()) CyanAccent else TextTertiary)
        }
      }

      // Clipboard / Multi-select Actions
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { viewModel.timelineEngine.copySelectedKeyframes() },
          enabled = keyframes.isNotEmpty(),
          modifier = Modifier.size(30.dp)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = "Copy Keyframe", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }

        IconButton(
          onClick = { viewModel.timelineEngine.pasteKeyframes() },
          modifier = Modifier.size(30.dp)
        ) {
          Icon(Icons.Default.ContentPaste, contentDescription = "Paste Keyframe", tint = GreenAccent, modifier = Modifier.size(16.dp))
        }

        IconButton(
          onClick = { viewModel.timelineEngine.duplicateSelectedKeyframes() },
          enabled = keyframes.isNotEmpty(),
          modifier = Modifier.size(30.dp)
        ) {
          Icon(Icons.Default.Diamond, contentDescription = "Duplicate Keyframe", tint = AmberAccent, modifier = Modifier.size(16.dp))
        }

        IconButton(
          onClick = { viewModel.timelineEngine.selectAllKeyframesInSelectedClip() },
          enabled = keyframes.isNotEmpty(),
          modifier = Modifier.size(30.dp)
        ) {
          Icon(Icons.Default.SelectAll, contentDescription = "Select All Keyframes", tint = CyanAccent, modifier = Modifier.size(16.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Keyframes overview chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "${keyframes.size} KF${if (keyframes.size == 1) "" else "s"}:",
        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      )

      keyframes.forEachIndexed { index, kf ->
        val isSelected = kf.id in selectedKeyframeIds || (activeKeyframe?.id == kf.id)
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) PurpleAccent else StudioSurfaceVariant)
            .border(1.dp, if (isSelected) Color.White else StudioBorder, RoundedCornerShape(4.dp))
            .clickable { viewModel.timelineEngine.selectKeyframe(kf.id) }
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = "#${index + 1} ${(kf.timeMs / 1000f)}s",
            style = MaterialTheme.typography.bodySmall.copy(
              color = if (isSelected) Color.White else TextSecondary,
              fontSize = 10.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Sub-Categories Navigation (Transform, Color, Audio, Effect, Curves)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      KeyframeCategory.values().forEach { cat ->
        val isCatSelected = selectedCategory == cat
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCatSelected) StudioSurfaceVariant else Color.Transparent)
            .border(1.dp, if (isCatSelected) CyanAccent else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable { selectedCategory = cat }
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = when (cat) {
              KeyframeCategory.TRANSFORM -> "Transform"
              KeyframeCategory.COLOR -> "Color & Filter"
              KeyframeCategory.AUDIO -> "Volume"
              KeyframeCategory.EFFECT -> "Effect"
              KeyframeCategory.CURVES -> "Curves & Easing"
            },
            style = MaterialTheme.typography.bodySmall.copy(
              color = if (isCatSelected) CyanAccent else TextSecondary,
              fontSize = 11.sp,
              fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
            )
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Parameter Sliders & Curve Controls Scroll Area
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      val targetKf = activeKeyframe ?: ClipKeyframe(timeMs = 0L)

      when (selectedCategory) {
        KeyframeCategory.TRANSFORM -> {
          // X Position (-1.0 to 1.0)
          KeyframeSliderRow(
            label = "X Position",
            value = targetKf.posX,
            range = -1.0f..1.0f,
            format = "%.2f",
            onValueChange = { newVal ->
              updateActiveKeyframe(viewModel, targetKf) { it.copy(posX = newVal) }
            },
            onReset = { updateActiveKeyframe(viewModel, targetKf) { it.copy(posX = 0f) } }
          )

          // Y Position (-1.0 to 1.0)
          KeyframeSliderRow(
            label = "Y Position",
            value = targetKf.posY,
            range = -1.0f..1.0f,
            format = "%.2f",
            onValueChange = { newVal ->
              updateActiveKeyframe(viewModel, targetKf) { it.copy(posY = newVal) }
            },
            onReset = { updateActiveKeyframe(viewModel, targetKf) { it.copy(posY = 0f) } }
          )

          // Scale Linking row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Scale Link", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
            IconButton(
              onClick = { linkScaleXY = !linkScaleXY },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                if (linkScaleXY) Icons.Default.Link else Icons.Default.LinkOff,
                contentDescription = "Link Scale",
                tint = if (linkScaleXY) CyanAccent else TextTertiary,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          // Scale X (0.1 to 5.0)
          KeyframeSliderRow(
            label = if (linkScaleXY) "Scale (Uniform)" else "Scale X",
            value = targetKf.scaleX,
            range = 0.1f..5.0f,
            format = "%.2fx",
            onValueChange = { newVal ->
              updateActiveKeyframe(viewModel, targetKf) {
                if (linkScaleXY) it.copy(scaleX = newVal, scaleY = newVal)
                else it.copy(scaleX = newVal)
              }
            },
            onReset = { updateActiveKeyframe(viewModel, targetKf) { it.copy(scaleX = 1f, scaleY = 1f) } }
          )

          if (!linkScaleXY) {
            // Scale Y (0.1 to 5.0)
            KeyframeSliderRow(
              label = "Scale Y",
              value = targetKf.scaleY,
              range = 0.1f..5.0f,
              format = "%.2fx",
              onValueChange = { newVal ->
                updateActiveKeyframe(viewModel, targetKf) { it.copy(scaleY = newVal) }
              },
              onReset = { updateActiveKeyframe(viewModel, targetKf) { it.copy(scaleY = 1f) } }
            )
          }

          // Rotation (-360 to 360)
          KeyframeSliderRow(
            label = "Rotation",
            value = targetKf.rotation,
            range = -360f..360f,
            format = "%.0f°",
            onValueChange = { newVal ->
              updateActiveKeyframe(viewModel, targetKf) { it.copy(rotation = newVal) }
            },
            onReset = { updateActiveKeyframe(viewModel, targetKf) { it.copy(rotation = 0f) } }
          )

          // Opacity (0.0 to 1.0)
          KeyframeSliderRow(
            label = "Opacity",
            value = targetKf.opacity,
            range = 0.0f..1.0f,
            format = "%.0f%%",
            displayMultiplier = 100f,
            onValueChange = { newVal ->
              updateActiveKeyframe(viewModel, targetKf) { it.copy(opacity = newVal) }
            },
            onReset = { updateActiveKeyframe(viewModel, targetKf) { it.copy(opacity = 1f) } }
          )
        }

        KeyframeCategory.COLOR -> {
          // Blur (0.0 to 1.0)
          KeyframeSliderRow(
            label = "Blur",
            value = targetKf.blur,
            range = 0.0f..1.0f,
            format = "%.0f%%",
            displayMultiplier = 100f,
            onValueChange = { newVal ->
              updateActiveKeyframe(viewModel, targetKf) { it.copy(blur = newVal) }
            },
            onReset = { updateActiveKeyframe(viewModel, targetKf) { it.copy(blur = 0f) } }
          )

          // Brightness (-1.0 to 1.0)
          KeyframeSliderRow(
            label = "Brightness",
            value = targetKf.brightness,
            range = -1.0f..1.0f,
            format = "%+.2f",
            onValueChange = { newVal ->
              updateActiveKeyframe(viewModel, targetKf) { it.copy(brightness = newVal) }
            },
            onReset = { updateActiveKeyframe(viewModel, targetKf) { it.copy(brightness = 0f) } }
          )

          // Contrast (0.0 to 3.0)
          KeyframeSliderRow(
            label = "Contrast",
            value = targetKf.contrast,
            range = 0.0f..3.0f,
            format = "%.2fx",
            onValueChange = { newVal ->
              updateActiveKeyframe(viewModel, targetKf) { it.copy(contrast = newVal) }
            },
            onReset = { updateActiveKeyframe(viewModel, targetKf) { it.copy(contrast = 1f) } }
          )

          // Saturation (0.0 to 3.0)
          KeyframeSliderRow(
            label = "Saturation",
            value = targetKf.saturation,
            range = 0.0f..3.0f,
            format = "%.2fx",
            onValueChange = { newVal ->
              updateActiveKeyframe(viewModel, targetKf) { it.copy(saturation = newVal) }
            },
            onReset = { updateActiveKeyframe(viewModel, targetKf) { it.copy(saturation = 1f) } }
          )
        }

        KeyframeCategory.AUDIO -> {
          // Volume (0.0 to 2.0)
          KeyframeSliderRow(
            label = "Track Volume",
            value = targetKf.volume,
            range = 0.0f..2.0f,
            format = "%.0f%%",
            displayMultiplier = 100f,
            onValueChange = { newVal ->
              updateActiveKeyframe(viewModel, targetKf) { it.copy(volume = newVal) }
            },
            onReset = { updateActiveKeyframe(viewModel, targetKf) { it.copy(volume = 1f) } }
          )
        }

        KeyframeCategory.EFFECT -> {
          // Effect Parameter (0.0 to 1.0)
          KeyframeSliderRow(
            label = "Effect Intensity / Param",
            value = targetKf.effectParam,
            range = 0.0f..1.0f,
            format = "%.0f%%",
            displayMultiplier = 100f,
            onValueChange = { newVal ->
              updateActiveKeyframe(viewModel, targetKf) { it.copy(effectParam = newVal) }
            },
            onReset = { updateActiveKeyframe(viewModel, targetKf) { it.copy(effectParam = 0f) } }
          )
        }

        KeyframeCategory.CURVES -> {
          KeyframeCurveEditor(
            keyframe = targetKf,
            onInterpolationChange = { mode ->
              updateActiveKeyframe(viewModel, targetKf) { it.copy(interpolation = mode) }
            },
            onCustomCurveChange = { newPts ->
              updateActiveKeyframe(viewModel, targetKf) {
                it.copy(
                  interpolation = KeyframeInterpolation.CUSTOM_CURVE,
                  customCurvePoints = newPts
                )
              }
            }
          )
        }
      }
    }
  }
}

private fun updateActiveKeyframe(
  viewModel: StudioViewModel,
  targetKf: ClipKeyframe,
  transform: (ClipKeyframe) -> ClipKeyframe
) {
  val keyframeAtHead = viewModel.timelineEngine.getKeyframeAtPlayhead()
  if (keyframeAtHead != null) {
    viewModel.timelineEngine.updateKeyframe(keyframeAtHead.id, transform)
  } else {
    // Automatically add keyframe at playhead with transformed values
    viewModel.timelineEngine.addKeyframeToSelectedClip(transform(targetKf))
  }
}

@Composable
private fun KeyframeSliderRow(
  label: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  format: String,
  displayMultiplier: Float = 1f,
  onValueChange: (Float) -> Unit,
  onReset: () -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(label, style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium))
      Row(verticalAlignment = Alignment.CenterVertically) {
        val displayVal = value * displayMultiplier
        Text(
          text = String.format(Locale.US, format, displayVal),
          style = MaterialTheme.typography.bodySmall.copy(color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onReset, modifier = Modifier.size(20.dp)) {
          Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = TextTertiary, modifier = Modifier.size(12.dp))
        }
      }
    }

    Slider(
      value = value.coerceIn(range.start, range.endInclusive),
      onValueChange = onValueChange,
      valueRange = range,
      colors = SliderDefaults.colors(
        thumbColor = CyanAccent,
        activeTrackColor = CyanAccent,
        inactiveTrackColor = StudioBorder
      ),
      modifier = Modifier.height(24.dp)
    )
  }
}

@Composable
private fun KeyframeCurveEditor(
  keyframe: ClipKeyframe,
  onInterpolationChange: (KeyframeInterpolation) -> Unit,
  onCustomCurveChange: (List<Float>) -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = "Interpolation Curve",
      style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    )

    // Interpolation modes
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      KeyframeInterpolation.values().forEach { mode ->
        val isSelected = keyframe.interpolation == mode
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) PurpleAccent else StudioSurfaceVariant)
            .border(1.dp, if (isSelected) Color.White else StudioBorder, RoundedCornerShape(6.dp))
            .clickable { onInterpolationChange(mode) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Text(
            text = when (mode) {
              KeyframeInterpolation.LINEAR -> "Linear"
              KeyframeInterpolation.EASE_IN -> "Ease In"
              KeyframeInterpolation.EASE_OUT -> "Ease Out"
              KeyframeInterpolation.EASE_IN_OUT -> "Ease In-Out"
              KeyframeInterpolation.CUSTOM_CURVE -> "Custom Curve"
            },
            style = MaterialTheme.typography.bodySmall.copy(
              color = if (isSelected) Color.White else TextSecondary,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          )
        }
      }
    }

    // Interactive Bezier Curve Canvas Preview
    val pts = keyframe.customCurvePoints
    var p1x by remember(pts) { mutableFloatStateOf(pts.getOrNull(0) ?: 0.42f) }
    var p1y by remember(pts) { mutableFloatStateOf(pts.getOrNull(1) ?: 0.0f) }
    var p2x by remember(pts) { mutableFloatStateOf(pts.getOrNull(2) ?: 0.58f) }
    var p2y by remember(pts) { mutableFloatStateOf(pts.getOrNull(3) ?: 1.0f) }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(80.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(StudioDarkBg)
        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
        .padding(8.dp)
    ) {
      Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
        val w = size.width
        val h = size.height

        // Grid lines
        drawLine(StudioBorder, Offset(0f, h), Offset(w, h), strokeWidth = 1f)
        drawLine(StudioBorder, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
        drawLine(StudioBorder, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)

        // Evaluate curve path
        val path = Path()
        path.moveTo(0f, h)

        val mode = keyframe.interpolation
        val steps = 40
        for (i in 1..steps) {
          val t = i.toFloat() / steps
          val factor = when (mode) {
            KeyframeInterpolation.LINEAR -> t
            KeyframeInterpolation.EASE_IN -> t * t
            KeyframeInterpolation.EASE_OUT -> t * (2f - t)
            KeyframeInterpolation.EASE_IN_OUT -> if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t
            KeyframeInterpolation.CUSTOM_CURVE -> {
              com.example.engine.KeyframeInterpolator.solveCubicBezier(t, p1x, p1y, p2x, p2y)
            }
          }
          val x = t * w
          val y = h - (factor.coerceIn(-0.2f, 1.4f) * h)
          path.lineTo(x, y)
        }

        drawPath(
          path = path,
          color = when (mode) {
            KeyframeInterpolation.LINEAR -> CyanAccent
            KeyframeInterpolation.EASE_IN -> GreenAccent
            KeyframeInterpolation.EASE_OUT -> AmberAccent
            KeyframeInterpolation.EASE_IN_OUT -> PinkAccent
            KeyframeInterpolation.CUSTOM_CURVE -> PurpleAccent
          },
          style = Stroke(width = 3.dp.toPx())
        )
      }
    }

    // Custom Curve Presets & Sliders
    AnimatedVisibility(visible = keyframe.interpolation == KeyframeInterpolation.CUSTOM_CURVE) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Curve Presets:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Button(
            onClick = {
              p1x = 0.42f; p1y = 0.0f; p2x = 0.58f; p2y = 1.0f
              onCustomCurveChange(listOf(p1x, p1y, p2x, p2y))
            },
            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant, contentColor = TextPrimary),
            modifier = Modifier.weight(1f).height(28.dp)
          ) {
            Text("S-Curve", fontSize = 10.sp)
          }

          Button(
            onClick = {
              p1x = 0.6f; p1y = -0.28f; p2x = 0.735f; p2y = 0.045f
              onCustomCurveChange(listOf(p1x, p1y, p2x, p2y))
            },
            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant, contentColor = TextPrimary),
            modifier = Modifier.weight(1f).height(28.dp)
          ) {
            Text("Anticipate", fontSize = 10.sp)
          }

          Button(
            onClick = {
              p1x = 0.175f; p1y = 0.885f; p2x = 0.32f; p2y = 1.275f
              onCustomCurveChange(listOf(p1x, p1y, p2x, p2y))
            },
            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant, contentColor = TextPrimary),
            modifier = Modifier.weight(1f).height(28.dp)
          ) {
            Text("Overshoot", fontSize = 10.sp)
          }
        }

        // Control point 1 Y slider
        KeyframeSliderRow(
          label = "Handle 1 Curve Y",
          value = p1y,
          range = -0.5f..1.5f,
          format = "%.2f",
          onValueChange = { newVal ->
            p1y = newVal
            onCustomCurveChange(listOf(p1x, p1y, p2x, p2y))
          },
          onReset = {
            p1y = 0.0f
            onCustomCurveChange(listOf(p1x, p1y, p2x, p2y))
          }
        )

        // Control point 2 Y slider
        KeyframeSliderRow(
          label = "Handle 2 Curve Y",
          value = p2y,
          range = -0.5f..1.5f,
          format = "%.2f",
          onValueChange = { newVal ->
            p2y = newVal
            onCustomCurveChange(listOf(p1x, p1y, p2x, p2y))
          },
          onReset = {
            p2y = 1.0f
            onCustomCurveChange(listOf(p1x, p1y, p2x, p2y))
          }
        )
      }
    }
  }
}
