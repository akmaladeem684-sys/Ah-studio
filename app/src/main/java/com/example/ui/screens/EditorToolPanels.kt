package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.domain.model.*
import com.example.engine.SelectedTrackElement
import com.example.engine.audio.SoundEffectsCatalog
import com.example.ui.StudioViewModel
import com.example.ui.components.filter.StudioFilterPreviewCard
import com.example.ui.components.filter.StudioPluginFilterPreviewCard
import com.example.ui.components.formatDuration
import com.example.ui.components.text.TextStudioPanel
import com.example.ui.components.timeline.AudioVolumeEnvelopeGraph
import com.example.ui.theme.*
import java.io.File

@Composable
fun EditToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Edit Clip Operations",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      EditorActionTile(icon = Icons.Default.VolumeUp, label = "Volume", color = GreenAccent) {
        viewModel.setActiveToolbarTab(com.example.ui.EditorToolbarTab.VOLUME)
      }
      EditorActionTile(icon = Icons.Default.CallSplit, label = "Split", color = CyanAccent) {
        viewModel.timelineEngine.splitSelectedClipAtPlayhead()
      }
      EditorActionTile(icon = Icons.Default.ContentCut, label = "Trim Tool", color = AmberAccent) {
        viewModel.setActiveToolbarTab(com.example.ui.EditorToolbarTab.TRIM)
      }
      EditorActionTile(icon = Icons.Default.Delete, label = "Delete", color = RedAccent) {
        viewModel.timelineEngine.deleteSelected()
      }
      EditorActionTile(icon = Icons.Default.ContentCopy, label = "Duplicate", color = PurpleAccent) {
        viewModel.timelineEngine.duplicateSelected()
      }
      EditorActionTile(icon = Icons.Default.AcUnit, label = "Freeze Frame", color = CyanAccent) {
        viewModel.timelineEngine.freezeFrameAtPlayhead()
      }
      EditorActionTile(icon = Icons.Default.RotateRight, label = "Rotate 90°", color = TextPrimary) {
        viewModel.timelineEngine.rotateSelectedClip()
      }
      EditorActionTile(icon = Icons.Default.Flip, label = "Flip H", color = TextPrimary) {
        viewModel.timelineEngine.flipSelectedClip(horizontal = true)
      }
      EditorActionTile(icon = Icons.Default.SwapVert, label = "Flip V", color = TextPrimary) {
        viewModel.timelineEngine.flipSelectedClip(horizontal = false)
      }
    }

    // Embedded Volume Slider Control
    VolumeSliderSection(viewModel = viewModel)
  }
}

@Composable
fun VolumeSliderSection(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  val targetClipId = when (selectedElement) {
    is SelectedTrackElement.Video -> (selectedElement as SelectedTrackElement.Video).clipId
    is SelectedTrackElement.Overlay -> (selectedElement as SelectedTrackElement.Overlay).clipId
    is SelectedTrackElement.Audio -> (selectedElement as SelectedTrackElement.Audio).clipId
    else -> timeline.videoClips.firstOrNull()?.id ?: timeline.audioClips.firstOrNull()?.id
  }

  val videoClip = timeline.videoClips.find { it.id == targetClipId }
  val overlayClip = timeline.overlayClips.find { it.id == targetClipId }
  val audioClip = timeline.audioClips.find { it.id == targetClipId }

  val clipName = videoClip?.name ?: overlayClip?.name ?: audioClip?.title ?: "Selected Track / Clip"
  val currentVol = videoClip?.volume ?: overlayClip?.volume ?: audioClip?.volume ?: 1.0f
  val isMuted = videoClip?.isMuted ?: overlayClip?.isMuted ?: audioClip?.isMuted ?: false

  var sliderVal by remember(targetClipId, currentVol, isMuted) {
    mutableFloatStateOf(if (isMuted) 0f else currentVol)
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = StudioSurfaceVariant,
    border = BorderStroke(1.dp, StudioBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = if (isMuted || sliderVal == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            contentDescription = null,
            tint = if (isMuted || sliderVal == 0f) RedAccent else GreenAccent,
            modifier = Modifier.size(20.dp)
          )
          Column {
            Text(
              text = "Volume Gain Adjustment",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            Text(
              text = clipName,
              style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Decrease Volume Button (-)
          IconButton(
            onClick = {
              val newVol = (sliderVal - 0.10f).coerceIn(0f, 3.0f)
              sliderVal = newVol
              viewModel.timelineEngine.setClipVolume(targetClipId, newVol)
            },
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .background(StudioSurface)
              .border(1.dp, StudioBorder, CircleShape)
              .testTag("volume_decrease_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Remove,
              contentDescription = "Decrease Volume",
              tint = TextPrimary,
              modifier = Modifier.size(16.dp)
            )
          }

          // Clickable Percentage Display Chip
          Surface(
            onClick = {
              val nextVol = when {
                isMuted || sliderVal == 0f -> 1.0f
                sliderVal < 0.5f -> 0.5f
                sliderVal < 1.0f -> 1.0f
                sliderVal < 1.5f -> 1.5f
                sliderVal < 2.0f -> 2.0f
                sliderVal < 3.0f -> 3.0f
                else -> 1.0f
              }
              sliderVal = nextVol
              viewModel.timelineEngine.setClipVolume(targetClipId, nextVol)
            },
            shape = RoundedCornerShape(6.dp),
            color = if (isMuted || sliderVal == 0f) RedAccent.copy(alpha = 0.2f) else GreenAccent.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, if (isMuted || sliderVal == 0f) RedAccent else GreenAccent),
            modifier = Modifier.testTag("volume_percentage_chip")
          ) {
            Text(
              text = if (isMuted || sliderVal == 0f) "Muted" else "${(sliderVal * 100).toInt()}%",
              style = MaterialTheme.typography.labelMedium.copy(
                color = if (isMuted || sliderVal == 0f) RedAccent else GreenAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              ),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }

          // Increase Volume Button (+)
          IconButton(
            onClick = {
              val newVol = if (isMuted || sliderVal == 0f) 1.0f else (sliderVal + 0.10f).coerceIn(0f, 3.0f)
              sliderVal = newVol
              viewModel.timelineEngine.setClipVolume(targetClipId, newVol)
            },
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .background(GreenAccent.copy(alpha = 0.25f))
              .border(1.dp, GreenAccent, CircleShape)
              .testTag("volume_increase_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Increase Volume",
              tint = GreenAccent,
              modifier = Modifier.size(18.dp)
            )
          }

          // Mute / Speaker Toggle Button
          IconButton(
            onClick = {
              if (isMuted || sliderVal == 0f) {
                val newVol = if (currentVol > 0f) currentVol else 1.0f
                sliderVal = newVol
                viewModel.timelineEngine.setClipVolume(targetClipId, newVol)
              } else {
                viewModel.timelineEngine.toggleClipMute(targetClipId)
              }
            },
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .background(StudioSurface)
              .border(1.dp, StudioBorder, CircleShape)
              .testTag("volume_mute_btn")
          ) {
            Icon(
              imageVector = if (isMuted || sliderVal == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
              contentDescription = "Mute Toggle",
              tint = if (isMuted || sliderVal == 0f) RedAccent else GreenAccent,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      Slider(
        value = sliderVal,
        onValueChange = { newValue ->
          sliderVal = newValue
          viewModel.timelineEngine.setClipVolume(targetClipId, newValue)
        },
        valueRange = 0f..3.0f,
        colors = SliderDefaults.colors(
          thumbColor = GreenAccent,
          activeTrackColor = GreenAccent,
          inactiveTrackColor = StudioBorder
        ),
        modifier = Modifier.testTag("volume_gain_slider")
      )

      val presets = listOf(
        0.0f to "Mute",
        0.5f to "50%",
        1.0f to "100%",
        1.5f to "150%",
        2.0f to "200% Boost",
        3.0f to "300% Max"
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        presets.forEach { (volVal, label) ->
          val isSelected = (sliderVal - volVal).let { kotlin.math.abs(it) < 0.05f }
          Surface(
            onClick = {
              sliderVal = volVal
              viewModel.timelineEngine.setClipVolume(targetClipId, volVal)
            },
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) GreenAccent.copy(alpha = 0.25f) else StudioSurface,
            border = BorderStroke(1.dp, if (isSelected) GreenAccent else StudioBorder),
            modifier = Modifier.testTag("vol_preset_${label.replace("%", "").replace(" ", "_").lowercase()}")
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) GreenAccent else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 10.sp
              ),
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun VolumeToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = GreenAccent)
        Text(
          text = "Track / Clip Volume Gain",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
      }
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    VolumeSliderSection(viewModel = viewModel)
  }
}

@Composable
fun AdjustToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  var currentAdjustments by remember(timeline.adjustments) { mutableStateOf(timeline.adjustments) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp)
      .heightIn(max = 340.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Color Grading & Adjustments",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      Row {
        TextButton(onClick = {
          currentAdjustments = VideoAdjustments()
          viewModel.timelineEngine.updateAdjustments(currentAdjustments)
        }) {
          Text("Reset All", color = RedAccent, style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      item {
        AdjustmentSlider(
          label = "Brightness",
          value = currentAdjustments.brightness,
          valueRange = -0.5f..0.5f,
          onValueChange = {
            currentAdjustments = currentAdjustments.copy(brightness = it)
            viewModel.timelineEngine.updateAdjustments(currentAdjustments)
          }
        )
      }
      item {
        AdjustmentSlider(
          label = "Contrast",
          value = currentAdjustments.contrast,
          valueRange = 0.5f..1.5f,
          onValueChange = {
            currentAdjustments = currentAdjustments.copy(contrast = it)
            viewModel.timelineEngine.updateAdjustments(currentAdjustments)
          }
        )
      }
      item {
        AdjustmentSlider(
          label = "Saturation",
          value = currentAdjustments.saturation,
          valueRange = 0.0f..2.0f,
          onValueChange = {
            currentAdjustments = currentAdjustments.copy(saturation = it)
            viewModel.timelineEngine.updateAdjustments(currentAdjustments)
          }
        )
      }
      item {
        AdjustmentSlider(
          label = "Exposure",
          value = currentAdjustments.exposure,
          valueRange = -0.5f..0.5f,
          onValueChange = {
            currentAdjustments = currentAdjustments.copy(exposure = it)
            viewModel.timelineEngine.updateAdjustments(currentAdjustments)
          }
        )
      }
      item {
        AdjustmentSlider(
          label = "Temperature",
          value = currentAdjustments.temperature,
          valueRange = -0.5f..0.5f,
          onValueChange = {
            currentAdjustments = currentAdjustments.copy(temperature = it)
            viewModel.timelineEngine.updateAdjustments(currentAdjustments)
          }
        )
      }
      item {
        AdjustmentSlider(
          label = "Tint",
          value = currentAdjustments.tint,
          valueRange = -0.5f..0.5f,
          onValueChange = {
            currentAdjustments = currentAdjustments.copy(tint = it)
            viewModel.timelineEngine.updateAdjustments(currentAdjustments)
          }
        )
      }
      item {
        AdjustmentSlider(
          label = "Highlights",
          value = currentAdjustments.highlights,
          valueRange = -1.0f..1.0f,
          onValueChange = {
            currentAdjustments = currentAdjustments.copy(highlights = it)
            viewModel.timelineEngine.updateAdjustments(currentAdjustments)
          }
        )
      }
      item {
        AdjustmentSlider(
          label = "Shadows",
          value = currentAdjustments.shadows,
          valueRange = -1.0f..1.0f,
          onValueChange = {
            currentAdjustments = currentAdjustments.copy(shadows = it)
            viewModel.timelineEngine.updateAdjustments(currentAdjustments)
          }
        )
      }
      item {
        AdjustmentSlider(
          label = "Sharpness",
          value = currentAdjustments.sharpness,
          valueRange = 0f..2.0f,
          onValueChange = {
            currentAdjustments = currentAdjustments.copy(sharpness = it)
            viewModel.timelineEngine.updateAdjustments(currentAdjustments)
          }
        )
      }
      item {
        AdjustmentSlider(
          label = "Vignette",
          value = currentAdjustments.vignette,
          valueRange = 0f..1.0f,
          onValueChange = {
            currentAdjustments = currentAdjustments.copy(vignette = it)
            viewModel.timelineEngine.updateAdjustments(currentAdjustments)
          }
        )
      }
      item {
        AdjustmentSlider(
          label = "Film Grain",
          value = currentAdjustments.grain,
          valueRange = 0f..1.0f,
          onValueChange = {
            currentAdjustments = currentAdjustments.copy(grain = it)
            viewModel.timelineEngine.updateAdjustments(currentAdjustments)
          }
        )
      }
    }
  }
}

@Composable
private fun AdjustmentSlider(
  label: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(label, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
      Text(
        String.format("%.2f", value),
        style = MaterialTheme.typography.bodySmall.copy(color = CyanAccent, fontWeight = FontWeight.Bold)
      )
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      colors = SliderDefaults.colors(
        thumbColor = CyanAccent,
        activeTrackColor = CyanAccent,
        inactiveTrackColor = StudioBorder
      )
    )
  }
}

@Composable
fun SpeedToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  val selectedClip = remember(timeline, selectedElement) {
    if (selectedElement is SelectedTrackElement.Video) {
      timeline.videoClips.find { it.id == (selectedElement as SelectedTrackElement.Video).clipId }
    } else timeline.videoClips.firstOrNull()
  }

  val speedPresets = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Speed & Curve Ramping",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    Text(
      text = "Current Speed: ${selectedClip?.speed ?: 1.0f}x",
      style = MaterialTheme.typography.bodyMedium.copy(color = CyanAccent, fontWeight = FontWeight.Bold)
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(speedPresets) { speed ->
        val isSelected = selectedClip?.speed == speed
        FilterChip(
          selected = isSelected,
          onClick = {
            selectedClip?.let {
              viewModel.timelineEngine.setClipSpeed(it.id, speed)
            }
          },
          label = { Text("${speed}x") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent,
            selectedLabelColor = Color.Black,
            containerColor = StudioSurfaceVariant,
            labelColor = TextPrimary
          )
        )
      }
    }

    // Speed curve presets
    Text(
      text = "Curve Presets",
      style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("Montage Ramp", "Hero Slow-Mo", "Bullet Time", "Jump Flash").forEach { curveName ->
        AssistChip(
          onClick = {
            selectedClip?.let {
              val targetSpeed = if (curveName.contains("Slow")) 0.5f else 1.5f
              viewModel.timelineEngine.setClipSpeed(it.id, targetSpeed)
            }
          },
          label = { Text(curveName, fontSize = 11.sp) },
          colors = AssistChipDefaults.assistChipColors(containerColor = StudioSurfaceVariant, labelColor = TextPrimary)
        )
      }
    }
  }
}

@Composable
fun FiltersToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()
  
  // Identify selected clip if any
  val selectedClip = remember(timeline, selectedElement) {
    when (selectedElement) {
      is SelectedTrackElement.Video -> timeline.videoClips.find { it.id == (selectedElement as SelectedTrackElement.Video).clipId }
      is SelectedTrackElement.Overlay -> timeline.overlayClips.find { it.id == (selectedElement as SelectedTrackElement.Overlay).clipId }
      else -> null
    }
  }

  // Active filter either from selected clip or global timeline filter
  val activeFilterState = selectedClip?.filter ?: timeline.filter
  var currentFilter by remember(activeFilterState) { mutableStateOf(activeFilterState) }
  var selectedPluginItemId by remember { mutableStateOf<String?>(null) }
  var selectedCategory by remember { mutableStateOf("All") }
  val categories = listOf("All", "Pro Enhancements", "Cinematic & Nature", "Aesthetic Looks", "Installed Plugins")

  val installedPlugins by viewModel.installedPlugins.collectAsState()
  val pluginFilters = remember(installedPlugins) {
    com.example.engine.plugin.PluginManager.getEnabledItemsForCategory(
      com.example.domain.plugin.PluginCategory.FILTER
    )
  }

  val displayFilters = remember(selectedCategory) {
    when (selectedCategory) {
      "All" -> FilterType.values().toList()
      "Pro Enhancements" -> FilterType.values().filter { it.category == "Pro Enhancements" || it == FilterType.NONE }
      "Cinematic & Nature" -> FilterType.values().filter { it.category == "Cinematic & Nature" || it == FilterType.NONE }
      "Aesthetic Looks" -> FilterType.values().filter { it.category == "Aesthetic Looks" || it == FilterType.NONE }
      else -> emptyList()
    }
  }

  // Active preview video reference from timeline clip
  val previewUri = remember(selectedClip, timeline) {
    selectedClip?.uri?.ifBlank { null }
      ?: timeline.videoClips.firstOrNull { it.uri.isNotBlank() }?.uri
      ?: ""
  }
  val previewSourceStartMs = selectedClip?.sourceStartMs ?: 0L

  // Scroll state & visible item tracking so off-screen cards pause video decoding
  val filtersScrollState = rememberLazyListState()
  val visibleIndices by remember {
    derivedStateOf {
      filtersScrollState.layoutInfo.visibleItemsInfo.map { it.index }.toSet()
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(PurpleAccent.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
        }
        Column {
          Text(
            text = "Professional Video Filters",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
          )
          Text(
            text = if (selectedClip != null) "Editing: ${selectedClip.name}" else "Editing: Project Timeline (Global)",
            style = MaterialTheme.typography.bodySmall.copy(color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (currentFilter.type != FilterType.NONE || selectedPluginItemId != null) {
          TextButton(
            onClick = {
              currentFilter = FilterSettings(type = FilterType.NONE, intensity = 1.0f)
              selectedPluginItemId = null
              viewModel.timelineEngine.updateFilter(currentFilter, selectedClip?.id)
              viewModel.timelineEngine.updateAdjustments(VideoAdjustments())
            },
            modifier = Modifier.testTag("filter_reset_button")
          ) {
            Text("Reset", color = TextSecondary, fontSize = 12.sp)
          }
        }
        IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }
    }

    // Category Tabs
    ScrollableTabRow(
      selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
      edgePadding = 0.dp,
      containerColor = Color.Transparent,
      contentColor = PurpleAccent,
      divider = {},
      modifier = Modifier.fillMaxWidth()
    ) {
      categories.forEach { cat ->
        val isSelected = selectedCategory == cat
        Tab(
          selected = isSelected,
          onClick = { selectedCategory = cat },
          text = {
            Text(
              text = cat,
              color = if (isSelected) PurpleAccent else TextSecondary,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              fontSize = 12.sp
            )
          }
        )
      }
    }

    // Live Animated Filter Previews Grid / Row
    if (selectedCategory != "Installed Plugins") {
      LazyRow(
        state = filtersScrollState,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
      ) {
        itemsIndexed(displayFilters) { index, type ->
          val isSelected = currentFilter.type == type
          val isVisible = index in visibleIndices || visibleIndices.isEmpty()

          StudioFilterPreviewCard(
            type = type,
            isSelected = isSelected,
            isVisible = isVisible,
            videoUri = previewUri,
            sourceStartMs = previewSourceStartMs,
            onClick = {
              currentFilter = currentFilter.copy(type = type)
              viewModel.timelineEngine.updateFilter(currentFilter, selectedClip?.id)
            }
          )
        }
      }
    } else {
      // Installed Plugins Section
      if (pluginFilters.isNotEmpty()) {
        LazyRow(
          state = filtersScrollState,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
          itemsIndexed(pluginFilters) { index, (plugin, filterItem) ->
            val isSelected = selectedPluginItemId == filterItem.id
            val isVisible = index in visibleIndices || visibleIndices.isEmpty()

            StudioPluginFilterPreviewCard(
              item = filterItem,
              plugin = plugin,
              isSelected = isSelected,
              isVisible = isVisible,
              videoUri = previewUri,
              sourceStartMs = previewSourceStartMs,
              onClick = {
                selectedPluginItemId = filterItem.id
                currentFilter = currentFilter.copy(type = FilterType.CINEMATIC, intensity = 1.0f)
                viewModel.timelineEngine.updateFilter(currentFilter, selectedClip?.id)
                viewModel.timelineEngine.updateAdjustments(
                  VideoAdjustments(
                    brightness = filterItem.brightness,
                    contrast = filterItem.contrast,
                    saturation = filterItem.saturation,
                    temperature = filterItem.temperature,
                    tint = filterItem.tint,
                    vignette = filterItem.vignette
                  )
                )
              }
            )
          }
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(StudioSurfaceVariant, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text("No third-party filter plugins installed yet", color = TextSecondary, fontSize = 12.sp)
        }
      }
    }

    // Filter Intensity Slider & Quick Controls
    if (currentFilter.type != FilterType.NONE) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = StudioSurfaceVariant,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "${currentFilter.type.displayName} Intensity",
                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
              )
            }
            Text(
              text = "${(currentFilter.intensity * 100).toInt()}%",
              style = MaterialTheme.typography.bodySmall.copy(color = PurpleAccent, fontWeight = FontWeight.Bold)
            )
          }

          Slider(
            value = currentFilter.intensity,
            onValueChange = {
              currentFilter = currentFilter.copy(intensity = it)
              viewModel.timelineEngine.updateFilter(currentFilter, selectedClip?.id)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
              thumbColor = PurpleAccent,
              activeTrackColor = PurpleAccent,
              inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.testTag("filter_intensity_slider")
          )

          // Quick Preset Chips & Compare / Apply to All actions
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Preset percentage buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf(0.25f, 0.50f, 0.75f, 1.0f).forEach { preset ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (kotlin.math.abs(currentFilter.intensity - preset) < 0.05f) PurpleAccent.copy(alpha = 0.2f) else Color.Transparent,
                  border = BorderStroke(1.dp, if (kotlin.math.abs(currentFilter.intensity - preset) < 0.05f) PurpleAccent else StudioBorder),
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      currentFilter = currentFilter.copy(intensity = preset)
                      viewModel.timelineEngine.updateFilter(currentFilter, selectedClip?.id)
                    }
                ) {
                  Text(
                    text = "${(preset * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (kotlin.math.abs(currentFilter.intensity - preset) < 0.05f) PurpleAccent else TextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                  )
                }
              }
            }

            // Apply to all clips button
            TextButton(
              onClick = {
                viewModel.timelineEngine.applyFilterToAllClips(currentFilter)
              },
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
              modifier = Modifier.testTag("filter_apply_all_button")
            ) {
              Icon(Icons.Default.DoneAll, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Apply to All", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }
  }
}

@Composable
fun EffectsToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()
  val selectedEffectId = (selectedElement as? SelectedTrackElement.Effect)?.clipId
  val activeEffectClip = timeline.effectClips.find { it.id == selectedEffectId } ?: timeline.effectClips.lastOrNull()

  var selectedCategory by remember { mutableStateOf("All") }
  val categories = listOf("All", "Basic", "Motion", "Light", "Distortion")

  val filteredEffects = remember(selectedCategory) {
    if (selectedCategory == "All") EffectType.values().toList()
    else EffectType.values().filter { it.category == selectedCategory }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Visual Effects Library",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(categories) { cat ->
        FilterChip(
          selected = selectedCategory == cat,
          onClick = { selectedCategory = cat },
          label = { Text(cat) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent,
            selectedLabelColor = Color.Black,
            containerColor = StudioSurfaceVariant,
            labelColor = TextPrimary
          )
        )
      }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      items(filteredEffects) { effect ->
        Card(
          modifier = Modifier
            .size(width = 110.dp, height = 80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              val newClip = viewModel.timelineEngine.addEffectClip(effect)
              viewModel.timelineEngine.selectElement(SelectedTrackElement.Effect(newClip.id))
            },
          colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(StudioBorder, CyanAccent.copy(alpha = 0.5f))))
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(24.dp))
            Text(
              text = effect.displayName,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp),
              maxLines = 1
            )
          }
        }
      }
    }

    // Active Effect Inspector
    if (activeEffectClip != null) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(StudioBorder, PurpleAccent.copy(alpha = 0.5f))))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(PurpleAccent)
              )
              Text(
                text = "${activeEffectClip.effectType.displayName} Effect",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
              )
              Text(
                text = "(${activeEffectClip.effectType.category})",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
              )
            }
            IconButton(
              onClick = { viewModel.timelineEngine.deleteEffectClip(activeEffectClip.id) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete effect", tint = RedAccent, modifier = Modifier.size(18.dp))
            }
          }

          // Intensity Slider
          Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Effect Intensity", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
              Text("${(activeEffectClip.intensity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(color = CyanAccent, fontWeight = FontWeight.Bold))
            }
            Slider(
              value = activeEffectClip.intensity,
              valueRange = 0.0f..1.0f,
              onValueChange = {
                viewModel.timelineEngine.updateEffectIntensity(activeEffectClip.id, it)
              },
              colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
            )
          }

          // Duration Controls
          Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Timeline Duration", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
              Text(String.format(java.util.Locale.US, "%.1fs", activeEffectClip.durationMs / 1000f), style = MaterialTheme.typography.bodySmall.copy(color = AmberAccent, fontWeight = FontWeight.Bold))
            }
            Slider(
              value = (activeEffectClip.durationMs / 1000f).coerceIn(0.5f, 15f),
              valueRange = 0.5f..15f,
              onValueChange = {
                viewModel.timelineEngine.updateEffectClip(activeEffectClip.copy(durationMs = (it * 1000).toLong()))
              },
              colors = SliderDefaults.colors(thumbColor = AmberAccent, activeTrackColor = AmberAccent)
            )
          }

          // Keyframe Support Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Keyframes: ${activeEffectClip.keyframes.size}",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = {
                  viewModel.timelineEngine.selectElement(SelectedTrackElement.Effect(activeEffectClip.id))
                  viewModel.timelineEngine.addEffectKeyframe(activeEffectClip.id)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp)
              ) {
                Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Keyframe", fontSize = 11.sp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun TransitionsToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  var transitionDurationMs by remember { mutableStateOf(500L) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Transition Effects",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      items(TransitionType.values()) { type ->
        Card(
          modifier = Modifier
            .size(width = 100.dp, height = 75.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              // Apply transition to first clip boundary
              viewModel.timelineEngine.setTransition(0, type, transitionDurationMs)
            },
          colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(StudioBorder, PurpleAccent.copy(alpha = 0.5f))))
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Transform, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(24.dp))
            Text(
              text = type.displayName,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp),
              maxLines = 1
            )
          }
        }
      }
    }
  }
}

@Composable
fun TextEditorPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  TextStudioPanel(viewModel = viewModel, modifier = modifier)
}

@Composable
fun AudioFadeControlsCard(
  audioClip: AudioClip,
  onFadeInChanged: (Long) -> Unit,
  onFadeOutChanged: (Long) -> Unit,
  onAddKeyframeAtPlayhead: () -> Unit,
  onResetEnvelope: () -> Unit,
  onApplyAutoFadesAll: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface, RoundedCornerShape(8.dp))
      .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Fade-In Section
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
        Text("Fade-In Duration", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
      }
      Text("${(audioClip.fadeInMs / 1000f).formatSec()}s", style = MaterialTheme.typography.labelSmall.copy(color = CyanAccent, fontWeight = FontWeight.Bold))
    }

    Slider(
      value = audioClip.fadeInMs.toFloat(),
      onValueChange = { onFadeInChanged(it.toLong()) },
      valueRange = 0f..(audioClip.durationMs / 2f).coerceAtLeast(100f),
      colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent, inactiveTrackColor = StudioBorder),
      modifier = Modifier.height(24.dp)
    )

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      listOf(0L to "Off", 300L to "0.3s", 500L to "0.5s", 1000L to "1.0s", 2000L to "2.0s").forEach { (ms, label) ->
        val isSelected = kotlin.math.abs(audioClip.fadeInMs - ms) < 50L
        Surface(
          onClick = { onFadeInChanged(ms) },
          shape = RoundedCornerShape(4.dp),
          color = if (isSelected) CyanAccent else StudioSurfaceVariant,
          border = BorderStroke(1.dp, if (isSelected) CyanAccent else StudioBorder)
        ) {
          Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = if (isSelected) Color.Black else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }
    }

    HorizontalDivider(color = StudioBorder, thickness = 0.5.dp)

    // Fade-Out Section
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
        Text("Fade-Out Duration", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
      }
      Text("${(audioClip.fadeOutMs / 1000f).formatSec()}s", style = MaterialTheme.typography.labelSmall.copy(color = PurpleAccent, fontWeight = FontWeight.Bold))
    }

    Slider(
      value = audioClip.fadeOutMs.toFloat(),
      onValueChange = { onFadeOutChanged(it.toLong()) },
      valueRange = 0f..(audioClip.durationMs / 2f).coerceAtLeast(100f),
      colors = SliderDefaults.colors(thumbColor = PurpleAccent, activeTrackColor = PurpleAccent, inactiveTrackColor = StudioBorder),
      modifier = Modifier.height(24.dp)
    )

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      listOf(0L to "Off", 300L to "0.3s", 500L to "0.5s", 1000L to "1.0s", 2000L to "2.0s").forEach { (ms, label) ->
        val isSelected = kotlin.math.abs(audioClip.fadeOutMs - ms) < 50L
        Surface(
          onClick = { onFadeOutChanged(ms) },
          shape = RoundedCornerShape(4.dp),
          color = if (isSelected) PurpleAccent else StudioSurfaceVariant,
          border = BorderStroke(1.dp, if (isSelected) PurpleAccent else StudioBorder)
        ) {
          Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = if (isSelected) Color.White else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }
    }

    HorizontalDivider(color = StudioBorder, thickness = 0.5.dp)

    // Keyframing Quick Buttons Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Button(
        onClick = onAddKeyframeAtPlayhead,
        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
      ) {
        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("+ Keyframe at Playhead", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }

      OutlinedButton(
        onClick = onApplyAutoFadesAll,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, GreenAccent),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
      ) {
        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Auto-Fade All Tracks", fontSize = 10.sp, color = GreenAccent, fontWeight = FontWeight.Bold)
      }

      IconButton(
        onClick = onResetEnvelope,
        modifier = Modifier.size(32.dp)
      ) {
        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Keyframes", tint = TextSecondary, modifier = Modifier.size(18.dp))
      }
    }
  }
}

private fun Float.formatSec(): String {
  return String.format(java.util.Locale.US, "%.1f", this)
}

@Composable
fun AudioToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableStateOf("Fades & Keyframes") } // "Fades & Keyframes", "Import", "Voiceover", "SFX", "Music"
  val isRecording by viewModel.audioEngine.isRecording.collectAsState()
  val recordDuration by viewModel.audioEngine.recordingDurationMs.collectAsState()
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  // Mic permission launcher for voice recording
  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      viewModel.audioEngine.startVoiceRecording {}
    } else {
      Toast.makeText(context, "Microphone permission required to record audio", Toast.LENGTH_SHORT).show()
    }
  }

  // File picker launcher for importing audio files directly from device
  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val fileName = getFileNameFromUri(context, uri) ?: "Imported Audio"
        val metadata = com.example.engine.media.MediaMetadataHelper.extractMetadata(context, uri.toString())
        val durationMs = if (metadata.durationMs > 0L) metadata.durationMs else 6000L

        val fileObj = try {
          val path = uri.path
          if (path != null && File(path).exists()) File(path) else null
        } catch (e: Exception) { null }
        val waveform = if (fileObj != null) viewModel.audioEngine.extractWaveformFromFile(fileObj) else null

        viewModel.timelineEngine.addAudioClip(
          title = fileName,
          durationMs = durationMs,
          uri = uri.toString(),
          waveformData = waveform
        )
        Toast.makeText(context, "Imported \"$fileName\" with automatic fade transitions!", Toast.LENGTH_SHORT).show()
      } catch (e: Exception) {
        Toast.makeText(context, "Failed to import audio: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Audio & Sound Design",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    // Filter Chips Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = selectedTab == "Fades & Keyframes",
        onClick = { selectedTab = "Fades & Keyframes" },
        label = { Text("Fades & Keyframes") },
        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AmberAccent, selectedLabelColor = Color.Black)
      )
      FilterChip(
        selected = selectedTab == "Import",
        onClick = { selectedTab = "Import" },
        label = { Text("Import Audio") },
        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenAccent, selectedLabelColor = Color.Black)
      )
      FilterChip(
        selected = selectedTab == "Voiceover",
        onClick = { selectedTab = "Voiceover" },
        label = { Text("Voiceover Record") },
        leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RedAccent, selectedLabelColor = Color.White)
      )
      FilterChip(
        selected = selectedTab == "SFX",
        onClick = { selectedTab = "SFX" },
        label = { Text("Sound Effects") },
        leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = Color.Black)
      )
      FilterChip(
        selected = selectedTab == "Music",
        onClick = { selectedTab = "Music" },
        label = { Text("Music Tracks") },
        leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleAccent, selectedLabelColor = Color.White)
      )
    }

    when (selectedTab) {
      "Fades & Keyframes" -> {
        val selectedAudioClip = timeline.audioClips.find { clip ->
          (selectedElement as? SelectedTrackElement.Audio)?.clipId == clip.id
        } ?: timeline.audioClips.firstOrNull()

        if (selectedAudioClip != null) {
          val playheadMs by viewModel.timelineEngine.currentPositionMs.collectAsState()

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(StudioSurfaceVariant)
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Audiotrack, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                Text(
                  text = selectedAudioClip.title.ifBlank { "Audio Track" },
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
              }
            }

            // Interactive Volume Envelope Graph
            AudioVolumeEnvelopeGraph(
              audioClip = selectedAudioClip,
              clipDurationMs = selectedAudioClip.durationMs,
              currentPlayheadMs = playheadMs,
              showControlsHeader = true,
              onAddKeyframe = { relTime, vol ->
                viewModel.timelineEngine.addAudioVolumeKeyframe(selectedAudioClip.id, relTime, vol)
              },
              onUpdateKeyframe = { kfId, newTime, newVol ->
                viewModel.timelineEngine.updateAudioVolumeKeyframe(selectedAudioClip.id, kfId, newTime, newVol)
              },
              onDeleteKeyframe = { kfId ->
                viewModel.timelineEngine.deleteAudioVolumeKeyframe(selectedAudioClip.id, kfId)
              },
              onFadeInChanged = { newFadeIn ->
                viewModel.timelineEngine.setAudioFade(selectedAudioClip.id, newFadeIn, selectedAudioClip.fadeOutMs)
              },
              onFadeOutChanged = { newFadeOut ->
                viewModel.timelineEngine.setAudioFade(selectedAudioClip.id, selectedAudioClip.fadeInMs, newFadeOut)
              },
              onApplyPresetFade = { presetType ->
                when (presetType) {
                  "fadeIn" -> viewModel.timelineEngine.setAudioFade(selectedAudioClip.id, 1000L, selectedAudioClip.fadeOutMs)
                  "fadeOut" -> viewModel.timelineEngine.setAudioFade(selectedAudioClip.id, selectedAudioClip.fadeInMs, 1000L)
                  else -> viewModel.timelineEngine.applyAudioFadeKeyframes(selectedAudioClip.id)
                }
              },
              onResetEnvelope = {
                viewModel.timelineEngine.resetAudioVolumeEnvelope(selectedAudioClip.id)
              },
              onBaseVolumeChanged = { newVol ->
                viewModel.timelineEngine.setAudioClipBaseVolume(selectedAudioClip.id, newVol)
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
            )

            // Fade Sliders & Quick Presets
            AudioFadeControlsCard(
              audioClip = selectedAudioClip,
              onFadeInChanged = { fadeIn ->
                viewModel.timelineEngine.setAudioFade(selectedAudioClip.id, fadeIn, selectedAudioClip.fadeOutMs)
              },
              onFadeOutChanged = { fadeOut ->
                viewModel.timelineEngine.setAudioFade(selectedAudioClip.id, selectedAudioClip.fadeInMs, fadeOut)
              },
              onAddKeyframeAtPlayhead = {
                val relTime = (playheadMs - selectedAudioClip.timelineStartMs).coerceIn(0L, selectedAudioClip.durationMs)
                val currentVol = com.example.engine.KeyframeInterpolator.interpolateVolume(selectedAudioClip, relTime)
                viewModel.timelineEngine.addAudioVolumeKeyframe(selectedAudioClip.id, relTime, currentVol)
              },
              onResetEnvelope = {
                viewModel.timelineEngine.resetAudioVolumeEnvelope(selectedAudioClip.id)
              },
              onApplyAutoFadesAll = {
                viewModel.timelineEngine.applyAutoFadesToAllAudioClips()
              }
            )
          }
        } else {
          // Empty State if no audio clip
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text("No Audio Clips on Timeline", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary))
              Text("Import audio or record a voiceover to edit volume keyframes and fade transitions.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, textAlign = TextAlign.Center))
              Button(
                onClick = { viewModel.timelineEngine.ensureAudioTrackExists() },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
              ) {
                Text("+ Create Master Audio Track", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
      "Import" -> {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
          colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
          border = BorderStroke(1.dp, GreenAccent.copy(alpha = 0.5f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.AudioFile, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(26.dp))
              Text(
                "Add Audio Files From Device",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
              )
            }
            Text(
              "Select MP3, WAV, AAC, M4A, OGG, or FLAC files directly from your phone's storage to add background music or voice clips.",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, textAlign = TextAlign.Center)
            )
            Button(
              onClick = { audioPickerLauncher.launch("audio/*") },
              colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = Color.Black),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Browse & Import Audio", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
      "Voiceover" -> {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioSurfaceVariant)
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              if (isRecording) Icons.Default.RadioButtonChecked else Icons.Default.Mic,
              contentDescription = null,
              tint = if (isRecording) RedAccent else CyanAccent,
              modifier = Modifier.size(24.dp)
            )
            Text(
              text = if (isRecording) "Recording Voiceover: ${formatDuration(recordDuration)}" else "Record Voiceover",
              style = MaterialTheme.typography.titleSmall.copy(
                color = if (isRecording) RedAccent else TextPrimary,
                fontWeight = FontWeight.Bold
              )
            )
          }

          if (isRecording) {
            val currentDb by viewModel.audioEngine.currentDecibels.collectAsState()
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              val normalizedAmp = ((currentDb + 60f) / 60f).coerceIn(0.1f, 1.0f)
              repeat(16) { index ->
                val barHeight = (12 * normalizedAmp * (0.5f + 0.5f * kotlin.math.sin(index * 0.8f + System.currentTimeMillis() * 0.005f))).dp.coerceAtLeast(4.dp)
                Box(
                  modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RedAccent)
                )
              }
            }
          } else {
            Text(
              "Tap record to capture live audio from your microphone at current playhead.",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, textAlign = TextAlign.Center)
            )
          }

          Button(
            onClick = {
              if (isRecording) {
                val finalDuration = if (recordDuration > 200L) recordDuration else 2000L
                val file = viewModel.audioEngine.stopVoiceRecording()
                val waveform = viewModel.audioEngine.extractWaveformFromFile(file)
                viewModel.timelineEngine.addAudioClip(
                  title = "Voiceover",
                  durationMs = finalDuration,
                  uri = file.absolutePath,
                  waveformData = waveform
                )
                Toast.makeText(context, "Voiceover recorded and saved!", Toast.LENGTH_SHORT).show()
              } else {
                val hasPermission = ContextCompat.checkSelfPermission(
                  context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                  viewModel.audioEngine.startVoiceRecording {}
                } else {
                  micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isRecording) RedAccent else CyanAccent,
              contentColor = Color.Black
            ),
            shape = CircleShape,
            modifier = Modifier.size(60.dp)
          ) {
            Icon(
              if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
              contentDescription = "Mic Record",
              modifier = Modifier.size(28.dp)
            )
          }
        }
      }
      "SFX" -> {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(SoundEffectsCatalog.effects) { sfx ->
            Card(
              modifier = Modifier
                .size(width = 130.dp, height = 90.dp)
                .clip(RoundedCornerShape(12.dp)),
              colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(sfx.icon, fontSize = 20.sp)
                  IconButton(
                    onClick = { viewModel.audioEngine.playPreviewSfx(sfx.id) },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = CyanAccent)
                  }
                }
                Text(sfx.title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary), maxLines = 1)
                Button(
                  onClick = {
                    viewModel.audioEngine.playPreviewSfx(sfx.id)
                    viewModel.timelineEngine.addAudioClip(sfx.title, sfx.durationMs)
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                  contentPadding = PaddingValues(0.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                ) {
                  Text("+ Add", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
      "Music" -> {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(SoundEffectsCatalog.musicTracks) { track ->
            Card(
              modifier = Modifier
                .size(width = 150.dp, height = 95.dp)
                .clip(RoundedCornerShape(12.dp)),
              colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Text(track.icon, fontSize = 20.sp)
                Text(track.title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary), maxLines = 1)
                Button(
                  onClick = { viewModel.timelineEngine.addAudioClip(track.title, track.durationMs) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                  contentPadding = PaddingValues(0.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = Color.White)
                ) {
                  Text("+ Add to Track", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }

    // Audio Track Volume Gain Slider
    VolumeSliderSection(viewModel = viewModel)
  }
}

@Composable
fun StickersToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()
  val installedPlugins by viewModel.installedPlugins.collectAsState()

  val selectedSticker = remember(selectedElement, timeline.stickerClips) {
    (selectedElement as? SelectedTrackElement.Sticker)?.let { sel ->
      timeline.stickerClips.find { it.id == sel.clipId }
    }
  }

  var selectedCategory by remember { mutableStateOf("Badges") }
  var searchQuery by remember { mutableStateOf("") }
  var isSearchActive by remember { mutableStateOf(false) }

  val pluginStickers = remember(installedPlugins) {
    com.example.engine.plugin.PluginManager.getEnabledItemsForCategory(
      com.example.domain.plugin.PluginCategory.STICKER
    )
  }

  val filteredItems = remember(selectedCategory, searchQuery, isSearchActive) {
    if (isSearchActive && searchQuery.isNotBlank()) {
      val q = searchQuery.trim().lowercase()
      com.example.data.presets.StickersCatalog.getAllStickers().filter { item ->
        item.name.lowercase().contains(q) ||
          item.symbolOrAsset.contains(q) ||
          item.category.lowercase().contains(q) ||
          item.tags.any { it.lowercase().contains(q) }
      }
    } else {
      com.example.data.presets.StickersCatalog.getItemsForCategory(selectedCategory)
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.EmojiEmotions, contentDescription = null, tint = AmberAccent)
        Text(
          text = "Stickers & Badges",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
          onClick = {
            isSearchActive = !isSearchActive
            if (!isSearchActive) searchQuery = ""
          }
        ) {
          Icon(
            imageVector = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.Search,
            contentDescription = "Search Stickers",
            tint = if (isSearchActive) AmberAccent else TextSecondary
          )
        }
        IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }
    }

    // Search bar if open
    if (isSearchActive) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search stickers, emojis, badges...", color = TextTertiary, fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AmberAccent) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
            }
          }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = StudioSurfaceVariant,
          unfocusedContainerColor = StudioSurfaceVariant,
          focusedBorderColor = AmberAccent,
          unfocusedBorderColor = Color.Transparent,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
      )
    }

    // Selected Sticker Inspector Bar (if a sticker is currently active on timeline)
    if (selectedSticker != null) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = StudioSurfaceVariant,
        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = if (selectedSticker.badgeType != null) "🏷️ ${selectedSticker.badgeType.displayName}" else "🎬 ${selectedSticker.emojiOrAsset}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = AmberAccent)
              )
              Text(
                text = "Layer: ${selectedSticker.durationMs / 1000f}s",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              // Duplicate Button
              IconButton(
                onClick = {
                  viewModel.timelineEngine.addStickerClip(
                    emojiOrAsset = selectedSticker.emojiOrAsset,
                    animationType = selectedSticker.animationType,
                    badgeType = selectedSticker.badgeType,
                    category = selectedSticker.category
                  )
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = CyanAccent, modifier = Modifier.size(18.dp))
              }
              // Delete Button
              IconButton(
                onClick = { viewModel.timelineEngine.deleteSticker(selectedSticker.id) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
              }
            }
          }

          // Opacity Slider
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("Opacity", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary), modifier = Modifier.width(52.dp))
            Slider(
              value = selectedSticker.opacity,
              onValueChange = { viewModel.timelineEngine.updateStickerOpacity(selectedSticker.id, it) },
              valueRange = 0.1f..1.0f,
              colors = SliderDefaults.colors(
                thumbColor = AmberAccent,
                activeTrackColor = AmberAccent,
                inactiveTrackColor = StudioSurface
              ),
              modifier = Modifier.weight(1f)
            )
            Text("${(selectedSticker.opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary), modifier = Modifier.width(36.dp))
          }

          // Animation Selection
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Motion / Animation", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(StickerAnimationType.values()) { animType ->
                val isAnimSelected = selectedSticker.animationType == animType
                FilterChip(
                  selected = isAnimSelected,
                  onClick = { viewModel.timelineEngine.updateStickerAnimation(selectedSticker.id, animType) },
                  label = { Text(animType.displayName, fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AmberAccent,
                    selectedLabelColor = StudioBlack,
                    containerColor = StudioSurface,
                    labelColor = TextSecondary
                  ),
                  border = BorderStroke(1.dp, if (isAnimSelected) AmberAccent else Color.Transparent)
                )
              }
            }
          }
        }
      }
    }

    // Category Tabs (when not searching)
    if (!isSearchActive) {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(com.example.data.presets.StickersCatalog.CATEGORIES) { cat ->
          val isCatSelected = selectedCategory == cat
          val icon = when (cat) {
            "Badges" -> "🏷️"
            "Animated Stickers" -> "⚡"
            "Trending Stickers" -> "🔥"
            "Emoji & Emotions" -> "😀"
            "Love & Hearts" -> "❤️"
            "Funny & Memes" -> "🗿"
            "Animals" -> "🐶"
            "Food & Drinks" -> "🍕"
            "Travel" -> "✈️"
            "Nature" -> "🌲"
            "Flowers" -> "🌸"
            "Weather" -> "☀️"
            "Sports" -> "⚽"
            "Gaming" -> "🎮"
            "Celebration" -> "🎉"
            "Birthday" -> "🎂"
            "Wedding" -> "💍"
            "Islamic" -> "🕌"
            "Ramadan & Eid" -> "🌙"
            "Business" -> "💼"
            "Shopping" -> "🛍️"
            "Sale & Discount" -> "💸"
            "Social Media" -> "▶️"
            "Arrows & Shapes" -> "➡️"
            "Speech Bubbles" -> "💬"
            "Decorative Elements" -> "✨"
            else -> "🎨"
          }

          FilterChip(
            selected = isCatSelected,
            onClick = { selectedCategory = cat },
            label = { Text("$icon $cat", fontSize = 12.sp, fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = AmberAccent,
              selectedLabelColor = StudioBlack,
              containerColor = StudioSurfaceVariant,
              labelColor = TextSecondary
            ),
            border = BorderStroke(1.dp, if (isCatSelected) AmberAccent else Color.Transparent)
          )
        }

        if (pluginStickers.isNotEmpty()) {
          item {
            val isCatSelected = selectedCategory == "Plugins"
            FilterChip(
              selected = isCatSelected,
              onClick = { selectedCategory = "Plugins" },
              label = { Text("🧩 Plugins (${pluginStickers.size})", fontSize = 12.sp, fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PurpleAccent,
                selectedLabelColor = Color.White,
                containerColor = StudioSurfaceVariant,
                labelColor = PurpleAccent
              ),
              border = BorderStroke(1.dp, if (isCatSelected) PurpleAccent else Color.Transparent)
            )
          }
        }
      }
    }

    // Grid content
    if (selectedCategory == "Plugins" && !isSearchActive) {
      // Plugin items
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().height(90.dp)
      ) {
        items(pluginStickers) { (plugin, stickerItem) ->
          val stickerAsset = stickerItem.file.ifBlank { stickerItem.emoji }
          Card(
            modifier = Modifier
              .size(width = 110.dp, height = 80.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                viewModel.timelineEngine.addStickerClip(
                  emojiOrAsset = stickerAsset,
                  category = "Plugins"
                )
              },
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            border = BorderStroke(1.dp, CyanAccent)
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              Text(stickerItem.emoji, fontSize = 28.sp)
              Text(
                text = stickerItem.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 10.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    } else if (selectedCategory == "Badges" && !isSearchActive) {
      // Badges layout (Horizontal scrollable or 2-row grid)
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().height(105.dp)
      ) {
        items(com.example.data.presets.StickersCatalog.BADGES) { badgeItem ->
          val badge = badgeItem.badgeType ?: BadgeType.NEW
          Card(
            modifier = Modifier
              .width(155.dp)
              .height(95.dp)
              .clip(RoundedCornerShape(14.dp))
              .clickable {
                viewModel.timelineEngine.addBadge(badge)
              },
            colors = CardDefaults.cardColors(
              containerColor = Color(badge.primaryColor).copy(alpha = 0.18f)
            ),
            border = BorderStroke(1.5.dp, Color(badge.primaryColor))
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(badge.primaryColor),
                modifier = Modifier.padding(bottom = 6.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(badge.icon, fontSize = 13.sp)
                  Text(
                    text = badge.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = Color.White, fontSize = 11.sp)
                  )
                }
              }
              Text(
                text = badge.subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    } else {
      // General Sticker Grid (or search results)
      if (filteredItems.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxWidth().height(80.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("No stickers found for \"$searchQuery\"", color = TextTertiary, fontSize = 13.sp)
        }
      } else {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth().height(85.dp)
        ) {
          items(filteredItems) { item ->
            val isAnimated = item.defaultAnimation != StickerAnimationType.NONE
            Card(
              modifier = Modifier
                .width(68.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  if (item.badgeType != null) {
                    viewModel.timelineEngine.addBadge(item.badgeType)
                  } else {
                    viewModel.timelineEngine.addStickerClip(
                      emojiOrAsset = item.symbolOrAsset,
                      animationType = item.defaultAnimation,
                      category = item.category
                    )
                  }
                },
              colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
              border = BorderStroke(
                width = if (isAnimated) 1.dp else 0.dp,
                color = if (isAnimated) AmberAccent.copy(alpha = 0.6f) else Color.Transparent
              )
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Box(
                  modifier = Modifier.weight(1f),
                  contentAlignment = Alignment.Center
                ) {
                  Text(item.symbolOrAsset, fontSize = 28.sp)
                }
                Text(
                  text = item.name,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = if (isAnimated) AmberAccent else TextSecondary,
                    textAlign = TextAlign.Center
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
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
fun ChromaKeyPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  var chroma by remember(timeline.chromaKey) { mutableStateOf(timeline.chromaKey) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Chroma Key (Green Screen)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Enable Chroma Key", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
      Switch(
        checked = chroma.enabled,
        onCheckedChange = {
          chroma = chroma.copy(enabled = it)
          viewModel.timelineEngine.updateChromaKey(chroma)
        },
        colors = SwitchDefaults.colors(checkedThumbColor = GreenAccent)
      )
    }

    if (chroma.enabled) {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // 1. Color Selection
        item {
          Text("Key Color", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(4.dp))
          val presetColors = listOf(
            0xFF00FF00L to "Green",
            0xFF0000FFL to "Blue",
            0xFFFF00FFL to "Magenta",
            0xFFFF0000L to "Red",
            0xFF000000L to "Black",
            0xFFFFFFFFL to "White"
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            presetColors.forEach { (colorVal, label) ->
              val isSelected = chroma.targetColor == colorVal
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(colorVal))
                  .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) CyanAccent else StudioBorder,
                    shape = CircleShape
                  )
                  .clickable {
                    chroma = chroma.copy(targetColor = colorVal)
                    viewModel.timelineEngine.updateChromaKey(chroma)
                  },
                contentAlignment = Alignment.Center
              ) {
                if (isSelected) {
                  Icon(
                    Icons.Default.Check,
                    contentDescription = label,
                    tint = if (colorVal == 0xFFFFFFFFL) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }

        // 2. Similarity Slider
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Similarity", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Text("${(chroma.similarity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary))
          }
          Slider(
            value = chroma.similarity,
            valueRange = 0.05f..0.95f,
            onValueChange = {
              chroma = chroma.copy(similarity = it, intensity = it)
              viewModel.timelineEngine.updateChromaKey(chroma)
            },
            colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
          )
        }

        // 3. Smoothness Slider
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Smoothness", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Text("${(chroma.smoothness * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary))
          }
          Slider(
            value = chroma.smoothness,
            valueRange = 0.01f..0.50f,
            onValueChange = {
              chroma = chroma.copy(smoothness = it)
              viewModel.timelineEngine.updateChromaKey(chroma)
            },
            colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
          )
        }

        // 4. Spill Suppression Slider
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Spill Suppression", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Text("${(chroma.spillSuppression * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary))
          }
          Slider(
            value = chroma.spillSuppression,
            valueRange = 0.0f..1.0f,
            onValueChange = {
              chroma = chroma.copy(spillSuppression = it, spillReduction = it)
              viewModel.timelineEngine.updateChromaKey(chroma)
            },
            colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
          )
        }

        // 5. Edge Control Slider
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Edge Control", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Text(String.format("%.2f", chroma.edgeControl), style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary))
          }
          Slider(
            value = chroma.edgeControl,
            valueRange = -0.5f..0.5f,
            onValueChange = {
              chroma = chroma.copy(edgeControl = it)
              viewModel.timelineEngine.updateChromaKey(chroma)
            },
            colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
          )
        }

        // 6. Background Type Selection
        item {
          Text("Background Replacement", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(4.dp))
          val bgTypes = listOf("Transparent", "SolidColor", "Image")
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bgTypes.forEach { type ->
              FilterChip(
                selected = chroma.backgroundType == type,
                onClick = {
                  chroma = chroma.copy(backgroundType = type)
                  viewModel.timelineEngine.updateChromaKey(chroma)
                },
                label = {
                  Text(when (type) {
                    "Transparent" -> "Transparent"
                    "SolidColor" -> "Solid Color"
                    else -> "Video / Image"
                  })
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CyanAccent,
                  selectedLabelColor = Color.Black,
                  containerColor = StudioSurfaceVariant,
                  labelColor = TextPrimary
                )
              )
            }
          }
        }

        if (chroma.backgroundType == "SolidColor") {
          item {
            Text("Solid Background Color", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Spacer(modifier = Modifier.height(4.dp))
            val solidPalette = listOf(
              0xFF000000L to "Black",
              0xFFFFFFFFL to "White",
              0xFF1E3A8AL to "Navy",
              0xFF7C3AEDL to "Purple",
              0xFFEF4444L to "Coral"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              solidPalette.forEach { (colorVal, name) ->
                val isSelected = chroma.backgroundColor == colorVal
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(colorVal))
                    .border(
                      width = if (isSelected) 3.dp else 1.dp,
                      color = if (isSelected) AmberAccent else StudioBorder,
                      shape = CircleShape
                    )
                    .clickable {
                      chroma = chroma.copy(backgroundColor = colorVal)
                      viewModel.timelineEngine.updateChromaKey(chroma)
                    }
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
fun CanvasPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val currentAspect by viewModel.activeAspectRatio.collectAsState()
  val currentRes by viewModel.activeResolution.collectAsState()
  val currentFps by viewModel.activeFps.collectAsState()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Canvas & Aspect Ratio",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(AspectRatio.values()) { ratio ->
        FilterChip(
          selected = currentAspect == ratio,
          onClick = {
            viewModel.updateProjectSettings(ratio, currentRes, currentFps)
          },
          label = { Text(ratio.label) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent,
            selectedLabelColor = Color.Black
          )
        )
      }
    }
  }
}

@Composable
fun CaptionsToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val currentPos by viewModel.timelineEngine.currentPositionMs.collectAsState()
  val textClips = timeline.textClips

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.ClosedCaption, contentDescription = null, tint = CyanAccent)
        Text(
          text = "Auto Captions & Subtitles",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
      }
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    Text(
      text = "Generate synchronized subtitle captions automatically from dialogue or create manual caption cards.",
      style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = { viewModel.runAIAutoCaptions() },
        modifier = Modifier
          .weight(1f)
          .height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("AI Auto-Captions", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }

      OutlinedButton(
        onClick = {
          viewModel.timelineEngine.addTextClip(
            text = "Subtitle Caption",
            timelineStartMs = currentPos,
            durationMs = 2500L
          )
        },
        modifier = Modifier
          .weight(1f)
          .height(44.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(StudioBorder, CyanAccent))),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = CyanAccent)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add Manual Subtitle", fontSize = 12.sp)
      }
    }

    if (textClips.isNotEmpty()) {
      Text(
        text = "Subtitles on Timeline (${textClips.size})",
        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
      )
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(textClips) { clip ->
          Card(
            modifier = Modifier
              .widthIn(min = 100.dp, max = 160.dp)
              .clip(RoundedCornerShape(8.dp))
              .clickable {
                viewModel.timelineEngine.selectElement(SelectedTrackElement.Text(clip.id))
                viewModel.timelineEngine.seekTo(clip.timelineStartMs)
              },
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                text = clip.text,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                maxLines = 1
              )
              Text(
                text = formatDuration(clip.timelineStartMs),
                style = MaterialTheme.typography.labelSmall.copy(color = CyanAccent, fontSize = 10.sp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EditorActionTile(
  icon: ImageVector,
  label: String,
  color: Color,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .size(width = 80.dp, height = 72.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium),
        maxLines = 1
      )
    }
  }
}

@Composable
fun BackgroundToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val currentAspect by viewModel.activeAspectRatio.collectAsState()
  val currentRes by viewModel.activeResolution.collectAsState()
  val currentFps by viewModel.activeFps.collectAsState()
  val currentSampleRate by viewModel.activeSampleRate.collectAsState()
  val activeCanvasColor by viewModel.activeCanvasColor.collectAsState()

  val colors = listOf(
    0xFF000000 to "Black",
    0xFF0F172A to "Slate",
    0xFF1E293B to "Charcoal",
    0xFFFFFFFF to "White",
    0xFF0A192F to "Navy",
    0xFF2E1065 to "Purple",
    0xFF064E3B to "Emerald",
    0xFF450A0A to "Crimson",
    0xFF083344 to "Cyan",
    0xFF18181B to "Zinc"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Texture, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
        Text(
          text = "Canvas Background",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
      }
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    Text(
      text = "Select solid or styled background color for canvas",
      style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
    )

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      items(colors) { (colorLong, name) ->
        val isSelected = activeCanvasColor == colorLong
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.clickable {
            viewModel.updateProjectSettings(
              aspectRatio = currentAspect,
              resolution = currentRes,
              fps = currentFps,
              sampleRate = currentSampleRate,
              canvasColor = colorLong
            )
          }
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(Color(colorLong))
              .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) CyanAccent else Color.White.copy(alpha = 0.3f),
                shape = CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
            if (isSelected) {
              Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (colorLong == 0xFFFFFFFF) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (isSelected) CyanAccent else TextSecondary,
              fontSize = 10.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          )
        }
      }
    }
  }
}

@Composable
fun GenerateMediaToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  var prompt by remember { mutableStateOf("") }
  val isAIBusy by viewModel.isAIBusy.collectAsState()
  val context = androidx.compose.ui.platform.LocalContext.current

  val samplePrompts = listOf(
    "Neon Cyberpunk City",
    "Golden Sunset Over Ocean",
    "Anime Lo-Fi Room",
    "Deep Space Galaxy"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = CyanAccent, shape = RoundedCornerShape(4.dp)) {
          Text(
            text = "AI",
            color = Color.Black,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }
        Text(
          text = "Generate Media (AI)",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
      }
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    OutlinedTextField(
      value = prompt,
      onValueChange = { prompt = it },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("ai_media_prompt_input"),
      placeholder = { Text("Describe image, clip, or sticker to generate...", color = TextTertiary) },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CyanAccent,
        unfocusedBorderColor = StudioBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
      )
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(samplePrompts) { sample ->
        Surface(
          onClick = { prompt = sample },
          shape = RoundedCornerShape(12.dp),
          color = StudioSurfaceVariant,
          border = BorderStroke(1.dp, StudioBorder)
        ) {
          Text(
            text = sample,
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = {
          val activePrompt = prompt.ifBlank { "Cinematic AI Visual Asset" }
          viewModel.timelineEngine.addStickerClip("🌟 $activePrompt")
          android.widget.Toast.makeText(context, "Generated media added to timeline!", android.widget.Toast.LENGTH_SHORT).show()
          viewModel.setActiveToolbarTab(null)
        },
        enabled = !isAIBusy,
        modifier = Modifier.weight(1f).testTag("generate_media_submit_btn"),
        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
      ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Generate Media", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }

      Button(
        onClick = {
          viewModel.runAIAutoEdit()
          viewModel.setActiveToolbarTab(null)
        },
        enabled = !isAIBusy,
        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = Color.White)
      ) {
        Text("Auto Reel", fontSize = 12.sp)
      }
    }

    OutlinedButton(
      onClick = {
        viewModel.setActiveToolbarTab(null)
        viewModel.navigateTo(com.example.ui.AppScreen.AI_SUITE)
      },
      modifier = Modifier.fillMaxWidth(),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
    ) {
      Text("Open Full AI Suite (Captions, Speech, Highlights)", fontSize = 11.sp)
    }
  }
}

@Composable
fun AIAvatarToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  var avatarScript by remember { mutableStateOf("Welcome to our video presentation! Powered by AI.") }
  var selectedAvatar by remember { mutableStateOf("Sophia (Presenter)") }
  val context = androidx.compose.ui.platform.LocalContext.current

  val avatars = listOf(
    "Sophia (Presenter)" to "👩‍💼",
    "Alex (Tech Host)" to "👨‍💻",
    "Marcus (Storyteller)" to "🎙️",
    "Emma (Lifestyle)" to "✨",
    "Cyber Host" to "🤖"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Diamond, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
        Text(
          text = "AI Avatar Presenter",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
      }
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    Text("Choose Avatar Persona:", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(avatars) { (name, emoji) ->
        val isSelected = selectedAvatar == name
        Surface(
          onClick = { selectedAvatar = name },
          shape = RoundedCornerShape(10.dp),
          color = if (isSelected) PurpleAccent.copy(alpha = 0.25f) else StudioSurfaceVariant,
          border = BorderStroke(1.dp, if (isSelected) PurpleAccent else StudioBorder)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(emoji, fontSize = 14.sp)
            Text(
              text = name,
              style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) Color.White else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
              )
            )
          }
        }
      }
    }

    OutlinedTextField(
      value = avatarScript,
      onValueChange = { avatarScript = it },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("ai_avatar_script_input"),
      placeholder = { Text("What should the avatar say?", color = TextTertiary) },
      maxLines = 3,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PurpleAccent,
        unfocusedBorderColor = StudioBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
      )
    )

    Button(
      onClick = {
        val script = avatarScript.ifBlank { "Hello from AI Avatar!" }
        viewModel.runAITextToSpeech(script)
        viewModel.timelineEngine.addTextClip(
          text = "[${selectedAvatar.substringBefore(" ")}]: $script",
          durationMs = 4000L
        )
        android.widget.Toast.makeText(context, "AI Avatar & speech added to timeline!", android.widget.Toast.LENGTH_SHORT).show()
        viewModel.setActiveToolbarTab(null)
      },
      modifier = Modifier.fillMaxWidth().testTag("add_ai_avatar_btn"),
      colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = Color.White)
    ) {
      Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Insert AI Avatar to Timeline", fontWeight = FontWeight.Bold)
    }
  }
}
