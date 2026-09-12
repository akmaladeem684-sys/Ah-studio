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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TextClip
import com.example.engine.SelectedTrackElement
import com.example.ui.StudioViewModel
import com.example.ui.theme.*
import com.example.util.FontManager
import com.example.util.FontOption
import java.util.UUID

enum class TextEditorSecondaryTab(val label: String, val iconEmoji: String) {
  TEMPLATES("Templates", "🎨"),
  FONTS("Fonts", "🔤"),
  STYLES("Styles", "✨"),
  EFFECTS("Effects", "🔮"),
  ANIMATIONS("Animations", "🎬")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextStudioPanel(
  viewModel: StudioViewModel,
  onDismiss: (() -> Unit)? = null,
  initialTab: TextEditorSecondaryTab = TextEditorSecondaryTab.TEMPLATES,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  // Get currently selected text clip if an existing text element is selected
  val selectedTextClip = remember(timeline.textClips, selectedElement) {
    if (selectedElement is SelectedTrackElement.Text) {
      timeline.textClips.find { it.id == (selectedElement as SelectedTrackElement.Text).clipId }
    } else {
      null
    }
  }

  // Draft clip for creating/customizing a new text layer
  var draftClip by remember {
    mutableStateOf(
      TextClip(
        text = "Your Text Here",
        fontSizeSp = 28f,
        textColor = 0xFFFFFFFF
      )
    )
  }

  // When selected clip changes or updates, keep in sync
  val activeClip = selectedTextClip ?: draftClip

  val onUpdateActiveClip: (TextClip) -> Unit = { updated ->
    if (selectedTextClip != null) {
      viewModel.timelineEngine.updateTextClip(updated)
    } else {
      draftClip = updated
    }
  }

  // Secondary tabs: Templates, Fonts, Styles, Effects, Animations
  var activeTab by remember { mutableStateOf(initialTab) }

  val installedPlugins by viewModel.installedPlugins.collectAsState()
  var fontOptionsList by remember { mutableStateOf(FontManager.getAvailableFonts(context)) }

  LaunchedEffect(installedPlugins) {
    fontOptionsList = FontManager.getAvailableFonts(context)
  }

  val fontPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      val imported = FontManager.importFont(context, uri)
      if (imported != null) {
        fontOptionsList = FontManager.getAvailableFonts(context)
        onUpdateActiveClip(
          activeClip.copy(fontFamily = imported.id, customFontPath = imported.filePath)
        )
        Toast.makeText(context, "Imported font: ${imported.name}", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "Failed to load font file", Toast.LENGTH_SHORT).show()
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // 1. Header with Title & Action (Add Text / Done)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CyanAccent.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.TextFields, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
        }
        Column {
          Text(
            text = if (selectedTextClip != null) "Edit Text Layer" else "Add Text & Titles",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
          )
          Text(
            text = "Templates • Fonts • Styles • Effects • Animations",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (selectedTextClip != null) {
          FilledTonalButton(
            onClick = {
              viewModel.timelineEngine.updateTextClip(activeClip)
              if (onDismiss != null) onDismiss() else viewModel.setActiveToolbarTab(null)
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CyanAccent, contentColor = Color.Black)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        } else {
          FilledTonalButton(
            onClick = {
              val currentPos = viewModel.timelineEngine.currentPositionMs.value
              val totalDuration = viewModel.timelineEngine.timeline.value.totalDurationMs.coerceAtLeast(1000L)
              val calculatedDuration = 3000L.coerceAtMost(maxOf(1000L, totalDuration - currentPos))
              val newClip = draftClip.copy(
                id = UUID.randomUUID().toString(),
                timelineStartMs = currentPos,
                durationMs = calculatedDuration
              )
              viewModel.timelineEngine.addTextClipObject(newClip)
              viewModel.timelineEngine.selectElement(SelectedTrackElement.Text(newClip.id))
              if (onDismiss != null) onDismiss() else viewModel.setActiveToolbarTab(null)
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CyanAccent, contentColor = Color.Black)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add Text", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }

        IconButton(
          onClick = {
            if (onDismiss != null) onDismiss() else viewModel.setActiveToolbarTab(null)
          },
          modifier = Modifier.size(30.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }
    }

    // 2. Text Input Card
    OutlinedTextField(
      value = activeClip.text,
      onValueChange = { newText ->
        onUpdateActiveClip(activeClip.copy(text = newText))
      },
      label = { Text("Text Content (Urdu & English Supported)", fontSize = 10.sp, color = CyanAccent) },
      placeholder = { Text("Type here in English, اردو or any script...", color = TextSecondary, fontSize = 12.sp) },
      modifier = Modifier.fillMaxWidth(),
      maxLines = 2,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CyanAccent,
        unfocusedBorderColor = StudioBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = StudioSurfaceVariant,
        unfocusedContainerColor = StudioSurfaceVariant
      )
    )

    // 3. Secondary Navigation / Tabs: Templates | Fonts | Styles | Effects | Animations
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      items(TextEditorSecondaryTab.values()) { tab ->
        val isSelected = activeTab == tab
        FilterChip(
          selected = isSelected,
          onClick = { activeTab = tab },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(tab.iconEmoji, fontSize = 12.sp)
              Text(
                text = tab.label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
              )
            }
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = when (tab) {
              TextEditorSecondaryTab.TEMPLATES -> AmberAccent
              TextEditorSecondaryTab.FONTS -> Color(0xFF10B981)
              TextEditorSecondaryTab.STYLES -> CyanAccent
              TextEditorSecondaryTab.EFFECTS -> PurpleAccent
              TextEditorSecondaryTab.ANIMATIONS -> Color(0xFFF43F5E)
            },
            selectedLabelColor = if (tab == TextEditorSecondaryTab.EFFECTS) Color.White else Color.Black,
            containerColor = StudioSurfaceVariant,
            labelColor = TextPrimary
          )
        )
      }
    }

    HorizontalDivider(color = StudioBorder, thickness = 0.5.dp)

    // 4. Secondary Tab Content View
    when (activeTab) {
      TextEditorSecondaryTab.TEMPLATES -> TemplatesSection(
        clip = activeClip,
        onApplyTemplate = { updatedClip -> onUpdateActiveClip(updatedClip) }
      )
      TextEditorSecondaryTab.FONTS -> FontsSection(
        clip = activeClip,
        availableFonts = fontOptionsList,
        onSelectFont = { fontId, path ->
          onUpdateActiveClip(activeClip.copy(fontFamily = fontId, customFontPath = path))
        },
        onImportFont = { fontPickerLauncher.launch(arrayOf("*/*")) }
      )
      TextEditorSecondaryTab.STYLES -> StylesSection(
        clip = activeClip,
        onUpdate = { updated -> onUpdateActiveClip(updated) }
      )
      TextEditorSecondaryTab.EFFECTS -> EffectsSection(
        clip = activeClip,
        onUpdate = { updated -> onUpdateActiveClip(updated) }
      )
      TextEditorSecondaryTab.ANIMATIONS -> AnimationsSection(
        clip = activeClip,
        onUpdate = { updated -> onUpdateActiveClip(updated) }
      )
    }
  }
}

// -------------------------------------------------------------
// TAB 1: TEMPLATES SECTION (41 Categories with search & previews)
// -------------------------------------------------------------
@Composable
private fun TemplatesSection(
  clip: TextClip,
  onApplyTemplate: (TextClip) -> Unit
) {
  var selectedCategory by remember { mutableStateOf("Trending") }
  var searchQuery by remember { mutableStateOf("") }

  val displayedTemplates = remember(selectedCategory, searchQuery) {
    var list = if (selectedCategory == "All") ALL_TEXT_TEMPLATES
    else ALL_TEXT_TEMPLATES.filter { it.category.equals(selectedCategory, ignoreCase = true) }

    if (searchQuery.isNotBlank()) {
      list = list.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
          it.category.contains(searchQuery, ignoreCase = true) ||
          it.sampleText.contains(searchQuery, ignoreCase = true)
      }
    }
    list
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // 41 Category Pills
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      contentPadding = PaddingValues(vertical = 2.dp)
    ) {
      items(TemplateCategories.ALL_CATEGORIES) { cat ->
        val isSelected = selectedCategory == cat.name
        FilterChip(
          selected = isSelected,
          onClick = { selectedCategory = cat.name },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(cat.iconEmoji, fontSize = 11.sp)
              Text(cat.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AmberAccent.copy(alpha = 0.25f),
            selectedLabelColor = AmberAccent,
            containerColor = StudioSurfaceVariant,
            labelColor = TextSecondary
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = StudioBorder,
            selectedBorderColor = AmberAccent
          )
        )
      }
    }

    // Templates Horizontal Carousel Cards
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(vertical = 2.dp)
    ) {
      items(displayedTemplates) { tpl ->
        val isSelected = clip.fontFamily.equals(tpl.fontFamily, true) && clip.textColor == tpl.textColor

        Card(
          modifier = Modifier
            .width(165.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              val textToKeep = if (clip.text.isBlank() || clip.text == "Tap to edit" || clip.text == "Your Text Here") {
                tpl.sampleText
              } else clip.text

              onApplyTemplate(
                clip.copy(
                  text = textToKeep,
                  fontFamily = tpl.fontFamily,
                  fontSizeSp = tpl.fontSizeSp,
                  fontWeight = tpl.fontWeight,
                  textColor = tpl.textColor,
                  hasGradient = tpl.hasGradient,
                  gradientColorStart = tpl.gradientColorStart,
                  gradientColorEnd = tpl.gradientColorEnd,
                  gradientDirection = tpl.gradientDirection,
                  strokeWidth = tpl.strokeWidth,
                  strokeColor = tpl.strokeColor,
                  hasShadow = tpl.hasShadow,
                  shadowColor = tpl.shadowColor,
                  hasBackground = tpl.hasBackground,
                  backgroundColor = tpl.backgroundColor,
                  cornerRadius = tpl.cornerRadius,
                  bgPadding = tpl.bgPadding,
                  animationType = tpl.animationType
                )
              )
            },
          colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
          border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) AmberAccent else StudioBorder)
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
              Text(tpl.badgeEmoji, fontSize = 16.sp)
              Text(tpl.category, fontSize = 9.sp, color = AmberAccent, fontWeight = FontWeight.Bold)
            }

            Text(
              text = tpl.name,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(tpl.backgroundColor.toInt()))
                .padding(horizontal = 6.dp, vertical = 4.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (clip.text.isNotBlank() && clip.text != "Tap to edit") clip.text else tpl.sampleText,
                color = Color(tpl.textColor.toInt()),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
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

// -------------------------------------------------------------
// TAB 2: FONTS SECTION (8 Categories + Real typography rendering)
// -------------------------------------------------------------
@Composable
private fun FontsSection(
  clip: TextClip,
  availableFonts: List<FontOption>,
  onSelectFont: (fontId: String, customPath: String?) -> Unit,
  onImportFont: () -> Unit
) {
  var selectedCategory by remember { mutableStateOf("Trending") }

  val displayedFonts = remember(selectedCategory, availableFonts) {
    FontCatalog.getFontsForCategory(selectedCategory, availableFonts)
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // Top Row with Import and Brand Font actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Typefaces & Typography",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (selectedCategory == "Brand Fonts") {
          TextButton(
            onClick = {
              FontCatalog.addBrandFont(
                BrandFontPreset(
                  id = "brand_${System.currentTimeMillis()}",
                  name = "Custom Brand Font",
                  fontFamily = clip.fontFamily,
                  customFontPath = clip.customFontPath,
                  defaultColor = clip.textColor,
                  fontWeight = clip.fontWeight
                )
              )
            },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(2.dp))
            Text("Save Brand Font", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }

        TextButton(
          onClick = onImportFont,
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Icon(Icons.Default.FileOpen, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
          Spacer(Modifier.width(2.dp))
          Text("Import TTF/OTF", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // 8 Font Categories: My Fonts | Brand Fonts | Trending | Urdu | English | Classic | New | Whimsical
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      contentPadding = PaddingValues(vertical = 2.dp)
    ) {
      items(FontCatalog.FONT_CATEGORIES) { cat ->
        val isSelected = selectedCategory == cat
        FilterChip(
          selected = isSelected,
          onClick = { selectedCategory = cat },
          label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.25f),
            selectedLabelColor = Color(0xFF10B981),
            containerColor = StudioSurfaceVariant,
            labelColor = TextSecondary
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = StudioBorder,
            selectedBorderColor = Color(0xFF10B981)
          )
        )
      }
    }

    // Fonts List Carousel with realistic text previews
    if (displayedFonts.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(90.dp),
        contentAlignment = Alignment.Center
      ) {
        if (selectedCategory == "My Fonts") {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No imported fonts yet.", color = TextSecondary, fontSize = 11.sp)
            TextButton(onClick = onImportFont) {
              Text("Tap here to import .ttf or .otf files", color = Color(0xFF10B981), fontSize = 11.sp)
            }
          }
        } else {
          Text("No fonts available in $selectedCategory", color = TextSecondary, fontSize = 11.sp)
        }
      }
    } else {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
      ) {
        items(displayedFonts) { fontOpt ->
          val isSelected = clip.fontFamily.equals(fontOpt.id, true) ||
            (clip.customFontPath != null && clip.customFontPath == fontOpt.filePath)

          val composeFont = FontManager.getComposeFontFamily(fontOpt.id, fontOpt.filePath)

          Card(
            modifier = Modifier
              .width(160.dp)
              .height(105.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable { onSelectFont(fontOpt.id, fontOpt.filePath) },
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) Color(0xFF10B981).copy(alpha = 0.2f) else StudioSurfaceVariant
            ),
            border = BorderStroke(
              if (isSelected) 2.dp else 1.dp,
              if (isSelected) Color(0xFF10B981) else StudioBorder
            )
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
                Text(
                  text = fontOpt.name,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                }
              }

              // Realistic Live Preview rendering with the actual font family
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(StudioSurface)
                  .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = if (clip.text.isNotBlank() && clip.text != "Tap to edit") clip.text else fontOpt.nativeSample,
                  color = if (isSelected) Color(0xFF10B981) else TextPrimary,
                  fontFamily = composeFont,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 3: STYLES SECTION (Colors, Gradients, Stroke, Shadow, Box)
// -------------------------------------------------------------
@Composable
private fun StylesSection(
  clip: TextClip,
  onUpdate: (TextClip) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // Format Toggles (B, I, U, TT) & Alignment
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(
          selected = clip.fontWeight >= 700,
          onClick = { onUpdate(clip.copy(fontWeight = if (clip.fontWeight >= 700) 400 else 800)) },
          label = { Text("B", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = Color.Black)
        )
        FilterChip(
          selected = clip.isItalic,
          onClick = { onUpdate(clip.copy(isItalic = !clip.isItalic)) },
          label = { Text("I", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = Color.Black)
        )
        FilterChip(
          selected = clip.isUnderline,
          onClick = { onUpdate(clip.copy(isUnderline = !clip.isUnderline)) },
          label = { Text("U", textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = Color.Black)
        )
        FilterChip(
          selected = clip.isAllCaps,
          onClick = { onUpdate(clip.copy(isAllCaps = !clip.isAllCaps)) },
          label = { Text("TT", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = Color.Black)
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("Left", "Center", "Right").forEach { align ->
          FilterChip(
            selected = clip.alignment.equals(align, true),
            onClick = { onUpdate(clip.copy(alignment = align)) },
            label = { Text(align, fontSize = 10.sp) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleAccent, selectedLabelColor = Color.White)
          )
        }
      }
    }

    // Color Swatches
    Text("Text Color Palette", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
    val palette = listOf(
      0xFFFFFFFF, 0xFFFFD700, 0xFF00E5FF, 0xFFFF007F, 0xFF10B981, 0xFF8B5CF6,
      0xFFF59E0B, 0xFFEF4444, 0xFFFFEA00, 0xFF3B82F6, 0xFFEC4899, 0xFF000000
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(palette) { colorHex ->
        val isSelected = clip.textColor == colorHex
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(colorHex))
            .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) CyanAccent else Color.White.copy(alpha = 0.4f), CircleShape)
            .clickable { onUpdate(clip.copy(textColor = colorHex)) }
        )
      }
    }

    // Font Size & Letter Spacing
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Size: ${clip.fontSizeSp.toInt()} sp", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
        Slider(
          value = clip.fontSizeSp,
          onValueChange = { onUpdate(clip.copy(fontSizeSp = it)) },
          valueRange = 12f..100f,
          colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text("Letter Spacing: ${clip.letterSpacing.toInt()}px", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
        Slider(
          value = clip.letterSpacing,
          onValueChange = { onUpdate(clip.copy(letterSpacing = it)) },
          valueRange = 0f..20f,
          colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
        )
      }
    }

    // Outline Stroke & Shadow Switches
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Outline: ${clip.strokeWidth.toInt()}px", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        Slider(
          value = clip.strokeWidth,
          onValueChange = { onUpdate(clip.copy(strokeWidth = it)) },
          valueRange = 0f..16f,
          modifier = Modifier.width(90.dp),
          colors = SliderDefaults.colors(thumbColor = AmberAccent, activeTrackColor = AmberAccent)
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Drop Shadow", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        Switch(
          checked = clip.hasShadow,
          onCheckedChange = { onUpdate(clip.copy(hasShadow = it)) },
          colors = SwitchDefaults.colors(checkedThumbColor = AmberAccent)
        )
      }
    }

    // Background Box & Gradient Switches
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Background Box", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        Switch(
          checked = clip.hasBackground,
          onCheckedChange = { onUpdate(clip.copy(hasBackground = it)) },
          colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Gradient Fill", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        Switch(
          checked = clip.hasGradient,
          onCheckedChange = { onUpdate(clip.copy(hasGradient = it)) },
          colors = SwitchDefaults.colors(checkedThumbColor = PurpleAccent)
        )
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 4: EFFECTS SECTION (3D, Neon, Glitch, Chrome, Arc, Comic)
// -------------------------------------------------------------
@Composable
private fun EffectsSection(
  clip: TextClip,
  onUpdate: (TextClip) -> Unit
) {
  val effectsList = listOf(
    "None" to "Normal Default",
    "Neon Glow" to "Cyber Neon Pulse",
    "3D Extrusion" to "Isometric 3D Extruded",
    "Chrome Metal" to "Metallic Mirror Shine",
    "Comic Pop" to "Retro Pop Art Halftone",
    "Glitch RGB" to "Digital RGB Channel Shift",
    "Curved Arc" to "Circular Curved Arc",
    "Glassmorphism" to "Frosted Translucent Blur"
  )

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("Artistic Text Effects & Transformations", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(effectsList) { (fxName, fxDesc) ->
        val isSelected = clip.subtitleStyle == fxName || (fxName == "None" && clip.subtitleStyle == "Classic")

        Card(
          modifier = Modifier
            .width(140.dp)
            .height(95.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable {
              when (fxName) {
                "Neon Glow" -> onUpdate(
                  clip.copy(
                    subtitleStyle = fxName,
                    textColor = 0xFF00FFFF,
                    hasShadow = true,
                    shadowColor = 0xFF00FFFF,
                    shadowBlur = 12f,
                    strokeWidth = 2f,
                    strokeColor = 0xFF003366
                  )
                )
                "3D Extrusion" -> onUpdate(
                  clip.copy(
                    subtitleStyle = fxName,
                    hasShadow = true,
                    shadowColor = 0xFF4C1D95,
                    shadowOffsetX = 6f,
                    shadowOffsetY = 6f,
                    strokeWidth = 2.5f,
                    strokeColor = 0xFF1E1035
                  )
                )
                "Chrome Metal" -> onUpdate(
                  clip.copy(
                    subtitleStyle = fxName,
                    hasGradient = true,
                    gradientColorStart = 0xFFE2E8F0,
                    gradientColorEnd = 0xFF64748B,
                    strokeWidth = 2f,
                    strokeColor = 0xFF0F172A
                  )
                )
                "Comic Pop" -> onUpdate(
                  clip.copy(
                    subtitleStyle = fxName,
                    textColor = 0xFFFFEA00,
                    strokeWidth = 4f,
                    strokeColor = 0xFF000000,
                    hasShadow = true,
                    shadowColor = 0xFFFF0055
                  )
                )
                "Glitch RGB" -> onUpdate(
                  clip.copy(
                    subtitleStyle = fxName,
                    animationType = "Shake",
                    textColor = 0xFF00FFCC,
                    strokeWidth = 2f,
                    strokeColor = 0xFFFF0055
                  )
                )
                else -> onUpdate(clip.copy(subtitleStyle = "Classic"))
              }
            },
          colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PurpleAccent.copy(alpha = 0.25f) else StudioSurfaceVariant
          ),
          border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) PurpleAccent else StudioBorder)
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
              Text("🔮", fontSize = 14.sp)
              if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(14.dp))
              }
            }
            Text(
              text = fxName,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
              maxLines = 1
            )
            Text(
              text = fxDesc,
              style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 5: ANIMATIONS SECTION (In, Out, Loop + Duration)
// -------------------------------------------------------------
@Composable
private fun AnimationsSection(
  clip: TextClip,
  onUpdate: (TextClip) -> Unit
) {
  var animCategory by remember { mutableStateOf("In") }

  val inAnimations = listOf("Pop", "Fade", "Slide", "Zoom", "Bounce", "Typewriter", "Shake", "Drop", "Flip")
  val outAnimations = listOf("Fade Out", "Slide Down", "Zoom Out", "Pop Out", "Wipe")
  val loopAnimations = listOf("Pulse", "Float", "Shake Loop", "Wave", "Rainbow")

  val currentList = when (animCategory) {
    "In" -> inAnimations
    "Out" -> outAnimations
    else -> loopAnimations
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // In / Out / Loop Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("In", "Out", "Loop").forEach { cat ->
          val isSelected = animCategory == cat
          FilterChip(
            selected = isSelected,
            onClick = { animCategory = cat },
            label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFFF43F5E),
              selectedLabelColor = Color.White
            )
          )
        }
      }

      Text(
        text = "Duration: ${(clip.animDurationMs / 1000f)}s",
        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
      )
    }

    // Animation presets carousel
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(currentList) { anim ->
        val isSelected = clip.animationType.equals(anim, true)
        FilterChip(
          selected = isSelected,
          onClick = { onUpdate(clip.copy(animationType = anim)) },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.Animation, contentDescription = null, modifier = Modifier.size(12.dp))
              Text(anim, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFF43F5E),
            selectedLabelColor = Color.White,
            containerColor = StudioSurfaceVariant,
            labelColor = TextPrimary
          )
        )
      }
    }

    // Animation Duration Slider
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Speed / Duration", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
      Slider(
        value = clip.animDurationMs.toFloat(),
        onValueChange = { onUpdate(clip.copy(animDurationMs = it.toLong())) },
        valueRange = 100f..3000f,
        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        colors = SliderDefaults.colors(thumbColor = Color(0xFFF43F5E), activeTrackColor = Color(0xFFF43F5E))
      )
    }
  }
}
