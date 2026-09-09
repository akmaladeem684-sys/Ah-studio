package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.engine.SelectedTrackElement
import com.example.engine.audio.SoundEffectsCatalog
import com.example.ui.StudioViewModel
import com.example.ui.components.formatDuration
import com.example.ui.components.text.TextStudioPanel
import com.example.ui.theme.*

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
      EditorActionTile(icon = Icons.Default.CallSplit, label = "Split", color = CyanAccent) {
        viewModel.timelineEngine.splitSelectedClipAtPlayhead()
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
  var currentFilter by remember(timeline.filter) { mutableStateOf(timeline.filter) }

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
        text = "Color Filters & LUTs",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(FilterType.values()) { type ->
        val isSelected = currentFilter.type == type
        FilterChip(
          selected = isSelected,
          onClick = {
            currentFilter = currentFilter.copy(type = type)
            viewModel.timelineEngine.updateFilter(currentFilter)
          },
          label = { Text(type.displayName) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PurpleAccent,
            selectedLabelColor = Color.White,
            containerColor = StudioSurfaceVariant,
            labelColor = TextPrimary
          )
        )
      }
    }

    if (currentFilter.type != FilterType.NONE) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Filter Intensity", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
          Text("${(currentFilter.intensity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(color = PurpleAccent, fontWeight = FontWeight.Bold))
        }
        Slider(
          value = currentFilter.intensity,
          onValueChange = {
            currentFilter = currentFilter.copy(intensity = it)
            viewModel.timelineEngine.updateFilter(currentFilter)
          },
          valueRange = 0f..1f,
          colors = SliderDefaults.colors(thumbColor = PurpleAccent, activeTrackColor = PurpleAccent)
        )
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
fun AudioToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf("SFX") } // "SFX", "Music", "Voiceover"
  val isRecording by viewModel.audioEngine.isRecording.collectAsState()
  val recordDuration by viewModel.audioEngine.recordingDurationMs.collectAsState()

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

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = selectedTab == "SFX",
        onClick = { selectedTab = "SFX" },
        label = { Text("Sound Effects") },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = Color.Black)
      )
      FilterChip(
        selected = selectedTab == "Music",
        onClick = { selectedTab = "Music" },
        label = { Text("Music Tracks") },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleAccent, selectedLabelColor = Color.White)
      )
      FilterChip(
        selected = selectedTab == "Voiceover",
        onClick = { selectedTab = "Voiceover" },
        label = { Text("Voiceover Record") },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RedAccent, selectedLabelColor = Color.White)
      )
    }

    when (selectedTab) {
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
      "Voiceover" -> {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = if (isRecording) "Recording Voiceover: ${formatDuration(recordDuration)}" else "Ready to Record Audio",
            style = MaterialTheme.typography.bodyMedium.copy(color = if (isRecording) RedAccent else TextPrimary, fontWeight = FontWeight.Bold)
          )
          Button(
            onClick = {
              if (isRecording) {
                val file = viewModel.audioEngine.stopVoiceRecording()
                viewModel.timelineEngine.addAudioClip("Voiceover Recording", 4000L, file.absolutePath)
              } else {
                viewModel.audioEngine.startVoiceRecording {}
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) RedAccent else CyanAccent, contentColor = Color.Black),
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
          ) {
            Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = "Mic")
          }
        }
      }
    }
  }
}

@Composable
fun StickersToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val stickerList = listOf("🎬", "🔥", "✨", "💯", "🚀", "⚡", "❤️", "🤩", "🎉", "👑", "👍", "💥", "🎯", "🎵", "🏆", "🌟")

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
        text = "Stickers & Badges",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      items(stickerList) { emoji ->
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(StudioSurfaceVariant)
            .clickable {
              viewModel.timelineEngine.addStickerClip(emoji)
            },
          contentAlignment = Alignment.Center
        ) {
          Text(emoji, fontSize = 26.sp)
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
