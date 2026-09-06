package com.example.ui.screens

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
              viewModel.timelineEngine.addEffectClip(effect)
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
      Column {
        Text("Keying Intensity: ${(chroma.intensity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
        Slider(
          value = chroma.intensity,
          onValueChange = {
            chroma = chroma.copy(intensity = it)
            viewModel.timelineEngine.updateChromaKey(chroma)
          },
          colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
        )
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
