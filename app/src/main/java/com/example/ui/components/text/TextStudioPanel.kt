package com.example.ui.components.text

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TextClip
import com.example.domain.model.WordTiming
import com.example.engine.SelectedTrackElement
import com.example.ui.StudioViewModel
import com.example.ui.components.formatDurationShort
import com.example.ui.theme.*
import com.example.util.FontManager
import com.example.util.FontOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextStudioPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  val selectedTextClip = remember(timeline, selectedElement) {
    if (selectedElement is SelectedTrackElement.Text) {
      timeline.textClips.find { it.id == (selectedElement as SelectedTrackElement.Text).clipId }
    } else timeline.textClips.firstOrNull()
  }

  var activeSubTab by remember { mutableStateOf("Typography") } // "Typography", "Style", "Motion", "Captions"
  val availableFonts = remember { FontManager.getAvailableFonts(context) }
  var fontOptionsList by remember { mutableStateOf(availableFonts) }

  val fontPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      val imported = FontManager.importFont(context, uri)
      if (imported != null) {
        fontOptionsList = FontManager.getAvailableFonts(context)
        selectedTextClip?.let { clip ->
          viewModel.timelineEngine.updateTextClip(
            clip.copy(fontFamily = imported.id, customFontPath = imported.filePath)
          )
        }
        Toast.makeText(context, "Font imported: ${imported.name}", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "Failed to load font file", Toast.LENGTH_SHORT).show()
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
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.TextFields, contentDescription = null, tint = CyanAccent)
        Text(
          text = "Text & Subtitle Studio",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilledTonalButton(
          onClick = { viewModel.timelineEngine.addTextClip("NEW TITLE") },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          colors = ButtonDefaults.filledTonalButtonColors(containerColor = CyanAccent, contentColor = Color.Black)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Add Text", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }
    }

    if (selectedTextClip == null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(140.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(StudioSurfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("No text or caption selected", color = TextSecondary)
          Button(
            onClick = { viewModel.timelineEngine.addTextClip("NEW TITLE") },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
          ) {
            Text("Create First Text Layer")
          }
        }
      }
      return
    }

    // Text Input Field
    OutlinedTextField(
      value = selectedTextClip.text,
      onValueChange = { newText ->
        viewModel.timelineEngine.updateTextClip(selectedTextClip.copy(text = newText))
      },
      label = { Text("Text / Caption Content") },
      modifier = Modifier.fillMaxWidth(),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CyanAccent,
        unfocusedBorderColor = StudioBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
      )
    )

    // Sub-tab selectors
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val tabs = listOf("Typography", "Style & Color", "Motion & Position", "Captions & Timing")
      items(tabs) { tab ->
        val isSelected = activeSubTab == tab
        FilterChip(
          selected = isSelected,
          onClick = { activeSubTab = tab },
          label = { Text(tab) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent,
            selectedLabelColor = Color.Black,
            containerColor = StudioSurfaceVariant,
            labelColor = TextPrimary
          )
        )
      }
    }

    HorizontalDivider(color = StudioBorder, thickness = 0.5.dp)

    // Sub-tab contents
    when (activeSubTab) {
      "Typography" -> TypographySettings(
        clip = selectedTextClip,
        fonts = fontOptionsList,
        onUpdate = { viewModel.timelineEngine.updateTextClip(it) },
        onImportFont = {
          fontPickerLauncher.launch(arrayOf("*/*"))
        }
      )
      "Style & Color" -> StyleAndColorSettings(
        clip = selectedTextClip,
        onUpdate = { viewModel.timelineEngine.updateTextClip(it) }
      )
      "Motion & Position" -> MotionAndPositionSettings(
        clip = selectedTextClip,
        onUpdate = { viewModel.timelineEngine.updateTextClip(it) }
      )
      "Captions & Timing" -> CaptionsAndTimingSettings(
        clip = selectedTextClip,
        allClips = timeline.textClips,
        onSelectClip = { clipId ->
          viewModel.timelineEngine.selectElement(SelectedTrackElement.Text(clipId))
        },
        onUpdate = { viewModel.timelineEngine.updateTextClip(it) },
        onAddSegment = {
          val nextStart = selectedTextClip.timelineStartMs + selectedTextClip.durationMs
          viewModel.timelineEngine.addTextClip("Next caption")
          val last = timeline.textClips.lastOrNull()
          if (last != null) {
            viewModel.timelineEngine.updateTextClip(
              last.copy(
                timelineStartMs = nextStart,
                durationMs = 2500L,
                subtitleStyle = selectedTextClip.subtitleStyle,
                fontSizeSp = selectedTextClip.fontSizeSp,
                posY = selectedTextClip.posY
              )
            )
          }
        },
        onDeleteSegment = {
          viewModel.timelineEngine.deleteSelected()
        }
      )
    }
  }
}

@Composable
private fun TypographySettings(
  clip: TextClip,
  fonts: List<FontOption>,
  onUpdate: (TextClip) -> Unit,
  onImportFont: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // Fonts Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Font Family", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
      TextButton(onClick = onImportFont, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
        Icon(Icons.Default.FileOpen, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Import Font (+)", color = AmberAccent, fontSize = 12.sp)
      }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(fonts) { fontOpt ->
        val isSelected = clip.fontFamily.equals(fontOpt.id, true) ||
            (clip.customFontPath != null && clip.customFontPath == fontOpt.filePath)
        FilterChip(
          selected = isSelected,
          onClick = {
            onUpdate(clip.copy(fontFamily = fontOpt.id, customFontPath = fontOpt.filePath))
          },
          label = { Text(fontOpt.name) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AmberAccent,
            selectedLabelColor = Color.Black
          )
        )
      }
    }

    // Font Size Slider
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Font Size: ${clip.fontSizeSp.toInt()} sp", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
      Slider(
        value = clip.fontSizeSp,
        onValueChange = { onUpdate(clip.copy(fontSizeSp = it)) },
        valueRange = 12f..96f,
        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
      )
    }

    // Font Weight & Italic
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Weight & Style", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val weights = listOf(400 to "Normal", 700 to "Bold", 900 to "Black")
        weights.forEach { (w, name) ->
          FilterChip(
            selected = clip.fontWeight == w,
            onClick = { onUpdate(clip.copy(fontWeight = w)) },
            label = { Text(name) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = Color.Black)
          )
        }
        FilterChip(
          selected = clip.isItalic,
          onClick = { onUpdate(clip.copy(isItalic = !clip.isItalic)) },
          label = { Text("Italic") },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleAccent, selectedLabelColor = Color.White)
        )
      }
    }

    // Alignment
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Alignment", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val alignments = listOf("Left", "Center", "Right")
        alignments.forEach { align ->
          FilterChip(
            selected = clip.alignment.equals(align, true),
            onClick = { onUpdate(clip.copy(alignment = align)) },
            label = { Text(align) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = Color.Black)
          )
        }
      }
    }

    // Letter Spacing & Line Spacing
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Letter Spacing: ${clip.letterSpacing.toInt()}", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        Slider(
          value = clip.letterSpacing,
          onValueChange = { onUpdate(clip.copy(letterSpacing = it)) },
          valueRange = -2f..10f,
          colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text("Line Spacing: ${(clip.lineSpacing * 10).toInt() / 10f}x", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        Slider(
          value = clip.lineSpacing,
          onValueChange = { onUpdate(clip.copy(lineSpacing = it)) },
          valueRange = 0.8f..2.2f,
          colors = SliderDefaults.colors(thumbColor = PurpleAccent, activeTrackColor = PurpleAccent)
        )
      }
    }
  }
}

@Composable
private fun StyleAndColorSettings(
  clip: TextClip,
  onUpdate: (TextClip) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // Text Color Swatches
    Text("Text Color", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
    val palette = listOf(0xFFFFFFFF, 0xFF00E5FF, 0xFFFFEB3B, 0xFFF59E0B, 0xFFEC4899, 0xFF8B5CF6, 0xFF10B981, 0xFFEF4444, 0xFF000000)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      items(palette) { colorHex ->
        val isSelected = clip.textColor == colorHex
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(colorHex))
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) CyanAccent else Color.White.copy(alpha = 0.4f), CircleShape)
            .clickable { onUpdate(clip.copy(textColor = colorHex)) }
        )
      }
    }

    // Gradient Toggle & Controls
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Gradient Fill", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
      Switch(
        checked = clip.hasGradient,
        onCheckedChange = { onUpdate(clip.copy(hasGradient = it)) },
        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = CyanAccent.copy(alpha = 0.5f))
      )
    }

    if (clip.hasGradient) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        val directions = listOf("Horizontal", "Vertical", "Diagonal")
        directions.forEach { dir ->
          FilterChip(
            selected = clip.gradientDirection.equals(dir, true),
            onClick = { onUpdate(clip.copy(gradientDirection = dir)) },
            label = { Text(dir) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleAccent, selectedLabelColor = Color.White)
          )
        }
      }
    }

    // Stroke Controls
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Outline Stroke: ${clip.strokeWidth.toInt()}px", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
      Slider(
        value = clip.strokeWidth,
        onValueChange = { onUpdate(clip.copy(strokeWidth = it)) },
        valueRange = 0f..12f,
        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        colors = SliderDefaults.colors(thumbColor = AmberAccent, activeTrackColor = AmberAccent)
      )
    }

    // Shadow Controls
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Drop Shadow", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
      Switch(
        checked = clip.hasShadow,
        onCheckedChange = { onUpdate(clip.copy(hasShadow = it)) },
        colors = SwitchDefaults.colors(checkedThumbColor = AmberAccent)
      )
    }

    // Rounded Background Controls
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Rounded Background", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
      Switch(
        checked = clip.hasBackground,
        onCheckedChange = { onUpdate(clip.copy(hasBackground = it)) },
        colors = SwitchDefaults.colors(checkedThumbColor = GreenAccent)
      )
    }

    if (clip.hasBackground) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Corner Radius: ${clip.cornerRadius.toInt()}dp", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
          Slider(
            value = clip.cornerRadius,
            onValueChange = { onUpdate(clip.copy(cornerRadius = it)) },
            valueRange = 0f..32f,
            colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("Padding: ${clip.bgPadding.toInt()}dp", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
          Slider(
            value = clip.bgPadding,
            onValueChange = { onUpdate(clip.copy(bgPadding = it)) },
            valueRange = 4f..32f,
            colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
          )
        }
      }
    }

    // Opacity
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Opacity: ${(clip.opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
      Slider(
        value = clip.opacity,
        onValueChange = { onUpdate(clip.copy(opacity = it)) },
        valueRange = 0.1f..1f,
        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
      )
    }
  }
}

@Composable
private fun MotionAndPositionSettings(
  clip: TextClip,
  onUpdate: (TextClip) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // Animations Selector
    Text("Entrance / Motion Animation", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
    val animList = listOf("Fade", "Slide", "Zoom", "Pop", "Bounce", "Typewriter", "Shake", "None")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(animList) { anim ->
        val isSelected = clip.animationType.equals(anim, true)
        FilterChip(
          selected = isSelected,
          onClick = { onUpdate(clip.copy(animationType = anim)) },
          label = { Text(anim) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent,
            selectedLabelColor = Color.Black
          )
        )
      }
    }

    // Animation Duration
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Anim Duration: ${clip.animDurationMs}ms", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
      Slider(
        value = clip.animDurationMs.toFloat(),
        onValueChange = { onUpdate(clip.copy(animDurationMs = it.toLong())) },
        valueRange = 100f..2000f,
        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
      )
    }

    // Scale & Rotation
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Scale: ${(clip.scale * 10).toInt() / 10f}x", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        Slider(
          value = clip.scale,
          onValueChange = { onUpdate(clip.copy(scale = it)) },
          valueRange = 0.2f..3.0f,
          colors = SliderDefaults.colors(thumbColor = PurpleAccent, activeTrackColor = PurpleAccent)
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text("Rotation: ${clip.rotation.toInt()}°", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        Slider(
          value = clip.rotation,
          onValueChange = { onUpdate(clip.copy(rotation = it)) },
          valueRange = -180f..180f,
          colors = SliderDefaults.colors(thumbColor = PurpleAccent, activeTrackColor = PurpleAccent)
        )
      }
    }

    // Position Coordinates (-1f to 1f normalized)
    Text("Position (Coordinate System Unified with Export)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Position X: ${(clip.posX * 100).toInt() / 100f}", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        Slider(
          value = clip.posX,
          onValueChange = { onUpdate(clip.copy(posX = it)) },
          valueRange = -1f..1f,
          colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text("Position Y: ${(clip.posY * 100).toInt() / 100f}", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        Slider(
          value = clip.posY,
          onValueChange = { onUpdate(clip.copy(posY = it)) },
          valueRange = -1f..1f,
          colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
        )
      }
    }

    // Quick Position Buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedButton(
        onClick = { onUpdate(clip.copy(posX = 0f, posY = -0.35f)) },
        modifier = Modifier.weight(1f)
      ) {
        Text("Top Header", fontSize = 11.sp)
      }
      OutlinedButton(
        onClick = { onUpdate(clip.copy(posX = 0f, posY = 0f)) },
        modifier = Modifier.weight(1f)
      ) {
        Text("Center", fontSize = 11.sp)
      }
      OutlinedButton(
        onClick = { onUpdate(clip.copy(posX = 0f, posY = 0.35f)) },
        modifier = Modifier.weight(1f)
      ) {
        Text("Subtitle (Bottom)", fontSize = 11.sp)
      }
    }
  }
}

@Composable
private fun CaptionsAndTimingSettings(
  clip: TextClip,
  allClips: List<TextClip>,
  onSelectClip: (String) -> Unit,
  onUpdate: (TextClip) -> Unit,
  onAddSegment: () -> Unit,
  onDeleteSegment: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // Subtitle Styles
    Text("Subtitle Style", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
    val styles = listOf("Classic", "Bold", "HighlightWord", "Karaoke", "Animated")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(styles) { style ->
        val isSelected = clip.subtitleStyle.equals(style, true)
        FilterChip(
          selected = isSelected,
          onClick = {
            val updated = when (style) {
              "Bold" -> clip.copy(subtitleStyle = style, fontWeight = 900, strokeWidth = 3.5f, textColor = 0xFFFFE600)
              "Classic" -> clip.copy(subtitleStyle = style, fontWeight = 700, strokeWidth = 2f, textColor = 0xFFFFFFFF, hasShadow = true)
              "HighlightWord" -> clip.copy(subtitleStyle = style, highlightColor = 0xFFFFEB3B)
              "Karaoke" -> clip.copy(subtitleStyle = style, highlightColor = 0xFF00E5FF)
              "Animated" -> clip.copy(subtitleStyle = style, animationType = "Pop")
              else -> clip.copy(subtitleStyle = style)
            }
            onUpdate(updated)
          },
          label = { Text(style) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (style == "Karaoke") PurpleAccent else CyanAccent,
            selectedLabelColor = if (style == "Karaoke") Color.White else Color.Black
          )
        )
      }
    }

    // Highlight Color Picker
    if (clip.subtitleStyle.equals("HighlightWord", true) || clip.subtitleStyle.equals("Karaoke", true)) {
      Text("Highlight Word Color", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
      val highlightColors = listOf(0xFFFFEB3B, 0xFF00E5FF, 0xFF10B981, 0xFFF59E0B, 0xFFEC4899)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        highlightColors.forEach { c ->
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(Color(c))
              .border(if (clip.highlightColor == c) 2.dp else 1.dp, Color.White, CircleShape)
              .clickable { onUpdate(clip.copy(highlightColor = c)) }
          )
        }
      }
    }

    // Direct Timing Controls
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Start: ${formatDurationShort(clip.timelineStartMs)} (${clip.timelineStartMs}ms)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { onUpdate(clip.copy(timelineStartMs = (clip.timelineStartMs - 200L).coerceAtLeast(0L))) }) {
            Icon(Icons.Default.Remove, contentDescription = "-200ms", tint = CyanAccent)
          }
          Slider(
            value = clip.timelineStartMs.toFloat(),
            onValueChange = { onUpdate(clip.copy(timelineStartMs = it.toLong())) },
            valueRange = 0f..60000f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
          )
          IconButton(onClick = { onUpdate(clip.copy(timelineStartMs = clip.timelineStartMs + 200L)) }) {
            Icon(Icons.Default.Add, contentDescription = "+200ms", tint = CyanAccent)
          }
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text("Duration: ${formatDurationShort(clip.durationMs)} (${clip.durationMs}ms)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { onUpdate(clip.copy(durationMs = (clip.durationMs - 200L).coerceAtLeast(500L))) }) {
            Icon(Icons.Default.Remove, contentDescription = "-200ms", tint = PurpleAccent)
          }
          Slider(
            value = clip.durationMs.toFloat(),
            onValueChange = { onUpdate(clip.copy(durationMs = it.toLong())) },
            valueRange = 500f..15000f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = PurpleAccent, activeTrackColor = PurpleAccent)
          )
          IconButton(onClick = { onUpdate(clip.copy(durationMs = clip.durationMs + 200L)) }) {
            Icon(Icons.Default.Add, contentDescription = "+200ms", tint = PurpleAccent)
          }
        }
      }
    }

    // Segment actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = onAddSegment,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
      ) {
        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Add Next Caption", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
      Button(
        onClick = onDeleteSegment,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
      ) {
        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Delete", fontSize = 11.sp)
      }
    }

    // Word-Level Timing Editor
    if (clip.words.isNotEmpty()) {
      Text("Word-Level Timing (${clip.words.size} words)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        itemsIndexed(clip.words) { idx, w ->
          Card(
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            modifier = Modifier.padding(2.dp)
          ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(w.word, fontWeight = FontWeight.Bold, color = AmberAccent, fontSize = 12.sp)
              Text("${w.startMs}ms (+${w.durationMs}ms)", fontSize = 9.sp, color = TextSecondary)
            }
          }
        }
      }
    }

    // Caption Segments List for Quick Selection
    Text("All Caption Segments (${allClips.size})", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 140.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      items(allClips) { c ->
        val isCurrent = c.id == clip.id
        Card(
          onClick = { onSelectClip(c.id) },
          colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) CyanAccent.copy(alpha = 0.2f) else StudioSurfaceVariant
          ),
          border = if (isCurrent) BorderStroke(1.dp, CyanAccent) else null,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = c.text,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal),
              modifier = Modifier.weight(1f)
            )
            Text(
              text = "${formatDurationShort(c.timelineStartMs)} - ${formatDurationShort(c.timelineStartMs + c.durationMs)}",
              style = MaterialTheme.typography.labelSmall.copy(color = if (isCurrent) CyanAccent else TextSecondary, fontSize = 10.sp)
            )
          }
        }
      }
    }
  }
}
