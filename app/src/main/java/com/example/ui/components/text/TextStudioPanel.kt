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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TextClip
import com.example.engine.SelectedTrackElement
import com.example.ui.StudioViewModel
import com.example.ui.components.formatDurationShort
import com.example.ui.theme.*
import com.example.util.FontManager
import com.example.util.FontOption

data class TextTemplateItem(
  val id: String,
  val name: String,
  val category: String, // "Urdu", "English", "Social", "Cinematic"
  val sampleText: String,
  val fontFamily: String,
  val fontSizeSp: Float = 28f,
  val fontWeight: Int = 800,
  val textColor: Long = 0xFFFFFFFF,
  val hasGradient: Boolean = false,
  val gradientColorStart: Long = 0xFF00E5FF,
  val gradientColorEnd: Long = 0xFF8B5CF6,
  val strokeWidth: Float = 0f,
  val strokeColor: Long = 0xFF000000,
  val hasShadow: Boolean = true,
  val shadowColor: Long = 0x88000000,
  val hasBackground: Boolean = false,
  val backgroundColor: Long = 0xCC000000,
  val cornerRadius: Float = 12f,
  val bgPadding: Float = 16f,
  val animationType: String = "Pop",
  val badgeEmoji: String = "✨"
)

val TEXT_TEMPLATES = listOf(
  TextTemplateItem(
    id = "urdu_calligraphy",
    name = "اردو خطاطی (Urdu Calligraphy)",
    category = "Urdu",
    sampleText = "اردو خطاطی و خوبصورت عنوان",
    fontFamily = "jameel_nastaliq",
    fontSizeSp = 32f,
    fontWeight = 800,
    textColor = 0xFFFFD700,
    strokeWidth = 2.5f,
    strokeColor = 0xFF1E1035,
    hasShadow = true,
    shadowColor = 0xFF000000,
    hasBackground = true,
    backgroundColor = 0xEE111827,
    cornerRadius = 16f,
    animationType = "Zoom",
    badgeEmoji = "🇵🇰"
  ),
  TextTemplateItem(
    id = "urdu_news_header",
    name = "اردو اہم خبر (News Ticker)",
    category = "Urdu",
    sampleText = "اہم خبر • تازہ ترین اپڈیٹ",
    fontFamily = "nastaleeq",
    fontSizeSp = 28f,
    fontWeight = 900,
    textColor = 0xFFFFFFFF,
    hasBackground = true,
    backgroundColor = 0xFFDC2626,
    cornerRadius = 8f,
    bgPadding = 18f,
    animationType = "Slide",
    badgeEmoji = "📰"
  ),
  TextTemplateItem(
    id = "urdu_poetry",
    name = "اردو شاعری (Urdu Poetry Card)",
    category = "Urdu",
    sampleText = "دل ناداں تجھے ہوا کیا ہے",
    fontFamily = "gulzar",
    fontSizeSp = 30f,
    fontWeight = 800,
    textColor = 0xFFFFF8DC,
    strokeWidth = 1f,
    strokeColor = 0xFF000000,
    hasBackground = true,
    backgroundColor = 0xEE2A134D,
    cornerRadius = 20f,
    animationType = "Fade",
    badgeEmoji = "📜"
  ),
  TextTemplateItem(
    id = "lower_third_blue",
    name = "Lower Third Professional",
    category = "English",
    sampleText = "JOHN DOE • VIDEO PRODUCER",
    fontFamily = "Bebas",
    fontSizeSp = 24f,
    fontWeight = 800,
    textColor = 0xFF00E5FF,
    hasBackground = true,
    backgroundColor = 0xEE0F172A,
    cornerRadius = 8f,
    animationType = "Slide",
    badgeEmoji = "💼"
  ),
  TextTemplateItem(
    id = "neon_glow_magenta",
    name = "Neon Glow Pulse",
    category = "English",
    sampleText = "NEON NIGHTS",
    fontFamily = "Impact",
    fontSizeSp = 34f,
    fontWeight = 900,
    textColor = 0xFFFF007F,
    hasGradient = true,
    gradientColorStart = 0xFFFF007F,
    gradientColorEnd = 0xFF00F0FF,
    strokeWidth = 3f,
    strokeColor = 0xFF00F0FF,
    hasShadow = true,
    shadowColor = 0xFFFF007F,
    animationType = "Pop",
    badgeEmoji = "⚡"
  ),
  TextTemplateItem(
    id = "retro_gold",
    name = "Retro Vintage Gold",
    category = "English",
    sampleText = "GOLDEN ERA VINTAGE",
    fontFamily = "Playfair",
    fontSizeSp = 30f,
    fontWeight = 800,
    textColor = 0xFFFFC107,
    strokeWidth = 1.5f,
    strokeColor = 0xFFFFE082,
    hasBackground = true,
    backgroundColor = 0xDD1C1204,
    cornerRadius = 10f,
    animationType = "Fade",
    badgeEmoji = "🏆"
  ),
  TextTemplateItem(
    id = "social_vlog_red",
    name = "Social Vlog Pill",
    category = "Social",
    sampleText = "LIKE & SUBSCRIBE 👍",
    fontFamily = "Montserrat",
    fontSizeSp = 26f,
    fontWeight = 900,
    textColor = 0xFFFFFFFF,
    hasBackground = true,
    backgroundColor = 0xFFEF4444,
    cornerRadius = 30f,
    bgPadding = 20f,
    animationType = "Bounce",
    badgeEmoji = "🔴"
  ),
  TextTemplateItem(
    id = "cinematic_movie",
    name = "Cinematic Movie Title",
    category = "Cinematic",
    sampleText = "T H E  J O U R N E Y",
    fontFamily = "Cinematic",
    fontSizeSp = 30f,
    fontWeight = 700,
    textColor = 0xFFF8FAFC,
    hasShadow = true,
    shadowColor = 0xCC000000,
    animationType = "Fade",
    badgeEmoji = "🎬"
  ),
  TextTemplateItem(
    id = "cyberpunk_tech",
    name = "Cyberpunk Tech Matrix",
    category = "English",
    sampleText = "CYBER MATRIX 2088",
    fontFamily = "Futuristic",
    fontSizeSp = 28f,
    fontWeight = 900,
    textColor = 0xFF00F0FF,
    strokeWidth = 2f,
    strokeColor = 0xFF8B5CF6,
    hasBackground = true,
    backgroundColor = 0xEE030712,
    animationType = "Typewriter",
    badgeEmoji = "🤖"
  ),
  TextTemplateItem(
    id = "bold_caption_yellow",
    name = "High Visibility Caption",
    category = "Social",
    sampleText = "WATCH UNTIL THE END! 🔥",
    fontFamily = "Impact",
    fontSizeSp = 28f,
    fontWeight = 900,
    textColor = 0xFFFFEA00,
    strokeWidth = 4f,
    strokeColor = 0xFF000000,
    hasShadow = true,
    shadowColor = 0xFF000000,
    animationType = "Pop",
    badgeEmoji = "🔥"
  )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextStudioPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  // Auto-select existing text clip or auto-create one if none exists
  val selectedTextClip = remember(timeline.textClips, selectedElement) {
    if (selectedElement is SelectedTrackElement.Text) {
      timeline.textClips.find { it.id == (selectedElement as SelectedTrackElement.Text).clipId }
    } else {
      timeline.textClips.firstOrNull()
    }
  }

  // Select active text element if already present
  LaunchedEffect(timeline.textClips, selectedElement) {
    if (timeline.textClips.isNotEmpty() && selectedElement !is SelectedTrackElement.Text) {
      val firstClip = timeline.textClips.first()
      viewModel.timelineEngine.selectElement(SelectedTrackElement.Text(firstClip.id))
    }
  }

  var activeSubTab by remember { mutableStateOf("Templates") } // "Templates", "Urdu Fonts", "English Fonts", "Style & Color", "Motion & Position", "Captions"
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
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // 1. Header with Title & Action Buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CyanAccent.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.TextFields, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
        }
        Column {
          Text(
            text = "Text & Subtitle Studio",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
          )
          Text(
            text = "Urdu & English Fonts • Templates • Styles",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Add New Layer
        FilledTonalButton(
          onClick = { viewModel.timelineEngine.addTextClip("New Title / نیا عنوان") },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          colors = ButtonDefaults.filledTonalButtonColors(containerColor = CyanAccent, contentColor = Color.Black)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Add Text", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Close Button
        IconButton(
          onClick = { viewModel.setActiveToolbarTab(null) },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }
    }

    val activeClip = selectedTextClip ?: TextClip(text = "Your Text Here")

    // 2. Text Input Editor & Quick Sample Suggestions
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
      border = BorderStroke(1.dp, StudioBorder)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = activeClip.text,
          onValueChange = { newText ->
            if (selectedTextClip != null) {
              viewModel.timelineEngine.updateTextClip(activeClip.copy(text = newText))
            } else if (newText.isNotBlank()) {
              val newClip = activeClip.copy(
                id = java.util.UUID.randomUUID().toString(),
                text = newText,
                timelineStartMs = viewModel.timelineEngine.currentPositionMs.value
              )
              viewModel.timelineEngine.addTextClipObject(newClip)
            }
          },
          label = { Text("Write Custom Content (Urdu / English)", fontSize = 11.sp, color = CyanAccent) },
          placeholder = { Text("Type here in Urdu or English...", color = TextSecondary, fontSize = 13.sp) },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 3,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanAccent,
            unfocusedBorderColor = StudioBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = StudioSurface,
            unfocusedContainerColor = StudioSurface
          )
        )

        // Quick Preset Sample Text Chips (Urdu & English)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Quick Samples:", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val samples = listOf(
              "🇵🇰 اردو خطاطی" to "اردو خطاطی و خوبصورت عنوان",
              "🇵🇰 اہم خبر" to "اہم خبر • تازہ ترین اپڈیٹ",
              "🇵🇰 شاعری" to "دل ناداں تجھے ہوا کیا ہے",
              "🔤 Title" to "CREATIVE VIDEO TITLE",
              "🔤 Vlog" to "Like & Subscribe 👍"
            )
            items(samples) { (label, sampleStr) ->
              SuggestionChip(
                onClick = {
                  if (selectedTextClip != null) {
                    viewModel.timelineEngine.updateTextClip(activeClip.copy(text = sampleStr))
                  } else {
                    val newClip = activeClip.copy(
                      id = java.util.UUID.randomUUID().toString(),
                      text = sampleStr,
                      timelineStartMs = viewModel.timelineEngine.currentPositionMs.value
                    )
                    viewModel.timelineEngine.addTextClipObject(newClip)
                  }
                },
                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                  containerColor = StudioSurface,
                  labelColor = TextPrimary
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                  enabled = true,
                  borderColor = StudioBorder
                )
              )
            }
          }
        }
      }
    }

    // 3. Navigation Sub-Tabs
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      val tabs = listOf("Templates", "Urdu Fonts", "English Fonts", "Style & Color", "Motion & Position", "Captions")
      items(tabs) { tab ->
        val isSelected = activeSubTab == tab
        FilterChip(
          selected = isSelected,
          onClick = { activeSubTab = tab },
          label = {
            Text(
              text = tab,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              fontSize = 12.sp
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = when (tab) {
              "Urdu Fonts" -> Color(0xFF10B981) // Emerald Green
              "Templates" -> AmberAccent
              else -> CyanAccent
            },
            selectedLabelColor = Color.Black,
            containerColor = StudioSurfaceVariant,
            labelColor = TextPrimary
          )
        )
      }
    }

    HorizontalDivider(color = StudioBorder, thickness = 0.5.dp)

    // 4. Sub-Tab Content Router
    when (activeSubTab) {
      "Templates" -> TemplatesSubTab(
        clip = activeClip,
        onApplyTemplate = { updatedClip ->
          if (selectedTextClip != null) {
            viewModel.timelineEngine.updateTextClip(updatedClip)
          } else {
            val newClip = updatedClip.copy(
              id = java.util.UUID.randomUUID().toString(),
              timelineStartMs = viewModel.timelineEngine.currentPositionMs.value
            )
            viewModel.timelineEngine.addTextClipObject(newClip)
          }
        }
      )
      "Urdu Fonts" -> UrduFontsSubTab(
        clip = activeClip,
        fonts = fontOptionsList.filter { it.category == "Urdu" || it.id.lowercase().contains("urdu") || it.id.lowercase().contains("nastaliq") },
        onSelectFont = { fontId, path ->
          if (selectedTextClip != null) {
            viewModel.timelineEngine.updateTextClip(activeClip.copy(fontFamily = fontId, customFontPath = path))
          } else {
            val newClip = activeClip.copy(
              id = java.util.UUID.randomUUID().toString(),
              fontFamily = fontId,
              customFontPath = path,
              timelineStartMs = viewModel.timelineEngine.currentPositionMs.value
            )
            viewModel.timelineEngine.addTextClipObject(newClip)
          }
        },
        onImportFont = { fontPickerLauncher.launch(arrayOf("*/*")) }
      )
      "English Fonts" -> EnglishFontsSubTab(
        clip = activeClip,
        fonts = fontOptionsList.filter { it.category != "Urdu" },
        onSelectFont = { fontId, path ->
          if (selectedTextClip != null) {
            viewModel.timelineEngine.updateTextClip(activeClip.copy(fontFamily = fontId, customFontPath = path))
          } else {
            val newClip = activeClip.copy(
              id = java.util.UUID.randomUUID().toString(),
              fontFamily = fontId,
              customFontPath = path,
              timelineStartMs = viewModel.timelineEngine.currentPositionMs.value
            )
            viewModel.timelineEngine.addTextClipObject(newClip)
          }
        },
        onImportFont = { fontPickerLauncher.launch(arrayOf("*/*")) }
      )
      "Style & Color" -> StyleAndColorSettings(
        clip = activeClip,
        onUpdate = {
          if (selectedTextClip != null) {
            viewModel.timelineEngine.updateTextClip(it)
          } else {
            val newClip = it.copy(
              id = java.util.UUID.randomUUID().toString(),
              timelineStartMs = viewModel.timelineEngine.currentPositionMs.value
            )
            viewModel.timelineEngine.addTextClipObject(newClip)
          }
        }
      )
      "Motion & Position" -> MotionAndPositionSettings(
        clip = activeClip,
        onUpdate = {
          if (selectedTextClip != null) {
            viewModel.timelineEngine.updateTextClip(it)
          } else {
            val newClip = it.copy(
              id = java.util.UUID.randomUUID().toString(),
              timelineStartMs = viewModel.timelineEngine.currentPositionMs.value
            )
            viewModel.timelineEngine.addTextClipObject(newClip)
          }
        }
      )
      "Captions" -> CaptionsAndTimingSettings(
        clip = activeClip,
        allClips = timeline.textClips,
        onSelectClip = { clipId ->
          viewModel.timelineEngine.selectElement(SelectedTrackElement.Text(clipId))
        },
        onUpdate = { viewModel.timelineEngine.updateTextClip(it) },
        onAddSegment = {
          viewModel.timelineEngine.addTextClip("Next caption")
        },
        onDeleteSegment = {
          viewModel.timelineEngine.deleteSelected()
        }
      )
    }
  }
}

// --- SUB TAB 1: TEXT TEMPLATES ---
@Composable
private fun TemplatesSubTab(
  clip: TextClip,
  onApplyTemplate: (TextClip) -> Unit
) {
  val installedPlugins by com.example.engine.plugin.PluginManager.installedPlugins.collectAsState()
  
  var pluginTemplates by remember { mutableStateOf(emptyList<TextTemplateItem>()) }

  // Dynamically extract and build template presets from all installed and enabled plugins
  LaunchedEffect(installedPlugins) {
    val list = mutableListOf<TextTemplateItem>()
    for (plugin in installedPlugins) {
      if (!plugin.isEnabled) continue
      for (item in plugin.manifest.items) {
        val cat = if (item.categoryKey.isNotBlank()) {
          com.example.domain.plugin.PluginCategory.fromKey(item.categoryKey)
        } else {
          plugin.manifest.category
        }
        if (cat == com.example.domain.plugin.PluginCategory.TEXT_TEMPLATE) {
          val category = (item.parameters["category"] as? String)
            ?: (item.parameters["type"] as? String)
            ?: "Templates"
          val sample = (item.parameters["sampleText"] as? String)
            ?: (item.parameters["text"] as? String)
            ?: (item.parameters["title"] as? String)
            ?: item.name
          val fontFam = (item.parameters["fontFamily"] as? String)
            ?: (item.parameters["font"] as? String)
            ?: if (category.contains("urdu", true) || category.contains("islamic", true)) "jameel_nastaliq" else "Sans-Serif"
          val fontSize = (item.parameters["fontSizeSp"] as? Number)?.toFloat()
            ?: (item.parameters["fontSize"] as? Number)?.toFloat()
            ?: 28f
          val fontWeight = (item.parameters["fontWeight"] as? Number)?.toInt()
            ?: (item.parameters["weight"] as? Number)?.toInt()
            ?: 700

          fun parseColor(v: Any?, def: Long): Long {
            if (v == null) return def
            if (v is Number) return v.toLong()
            val s = v.toString().trim()
            return try {
              if (s.startsWith("#")) {
                val hex = s.substring(1)
                if (hex.length == 6) ("FF$hex").toLong(16) else hex.toLong(16)
              } else s.toLong()
            } catch (_: Exception) { def }
          }

          val textColor = parseColor(item.parameters["textColor"] ?: item.parameters["color"], 0xFFFFFFFFL)
          val bgColor = parseColor(item.parameters["backgroundColor"] ?: item.parameters["bg_color"], 0xCC000000L)
          val strokeColor = parseColor(item.parameters["strokeColor"], 0xFF000000L)
          val shadowColor = parseColor(item.parameters["shadowColor"], 0x88000000L)
          val gradStart = parseColor(item.parameters["gradientColorStart"], 0xFF00E5FFL)
          val gradEnd = parseColor(item.parameters["gradientColorEnd"], 0xFF8B5CF6L)
          
          val strokeWidth = (item.parameters["strokeWidth"] as? Number)?.toFloat() ?: 0f
          val hasShadow = (item.parameters["hasShadow"] as? Boolean) ?: (item.parameters["shadow"] as? Boolean) ?: true
          val hasGradient = (item.parameters["hasGradient"] as? Boolean) ?: (item.parameters["gradient"] as? Boolean) ?: false
          val hasBg = (item.parameters["hasBackground"] as? Boolean) ?: true
          val anim = (item.parameters["animationType"] as? String) ?: (item.parameters["animation"] as? String) ?: "Pop"

          list.add(
            TextTemplateItem(
              id = item.id,
              name = item.name,
              category = category,
              sampleText = sample,
              fontFamily = fontFam,
              fontSizeSp = fontSize,
              fontWeight = fontWeight,
              textColor = textColor,
              hasGradient = hasGradient,
              gradientColorStart = gradStart,
              gradientColorEnd = gradEnd,
              strokeWidth = strokeWidth,
              strokeColor = strokeColor,
              hasShadow = hasShadow,
              shadowColor = shadowColor,
              hasBackground = hasBg,
              backgroundColor = bgColor,
              cornerRadius = 12f,
              bgPadding = 16f,
              animationType = anim,
              badgeEmoji = item.emoji
            )
          )
        }
      }
    }
    pluginTemplates = list
  }

  val allTemplates = remember(pluginTemplates) { TEXT_TEMPLATES + pluginTemplates }

  val categories = remember(allTemplates) {
    listOf("All") + allTemplates.map { it.category }.distinct()
  }
  var selectedCategory by remember { mutableStateOf("All") }

  val displayedTemplates = remember(selectedCategory, allTemplates) {
    if (selectedCategory == "All") allTemplates
    else allTemplates.filter { it.category.equals(selectedCategory, ignoreCase = true) }
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Ready-To-Use Text Templates",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      Text(
        text = if (pluginTemplates.isNotEmpty()) "${allTemplates.size} Styles (${pluginTemplates.size} Plugins)" else "${allTemplates.size} Styles",
        style = MaterialTheme.typography.labelSmall.copy(color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      )
    }

    // Category Filter Chips Row (All, Urdu, English, Captions, Quotes, Business, YouTube, Islamic, Reels)
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      contentPadding = PaddingValues(vertical = 2.dp)
    ) {
      items(categories) { catName ->
        val isSelected = selectedCategory == catName
        FilterChip(
          selected = isSelected,
          onClick = { selectedCategory = catName },
          label = { Text(catName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PurpleAccent.copy(alpha = 0.25f),
            selectedLabelColor = PurpleAccent,
            containerColor = StudioSurfaceVariant,
            labelColor = TextSecondary
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = StudioBorder,
            selectedBorderColor = PurpleAccent
          )
        )
      }
    }

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(vertical = 4.dp)
    ) {
      items(displayedTemplates) { tpl ->
        Card(
          modifier = Modifier
            .width(180.dp)
            .height(115.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              val sampleToUse = if (clip.text.isBlank() || clip.text == "Tap to edit" || clip.text == "Your Text Here") {
                tpl.sampleText
              } else clip.text

              onApplyTemplate(
                clip.copy(
                  text = sampleToUse,
                  fontFamily = tpl.fontFamily,
                  customFontPath = null,
                  fontSizeSp = tpl.fontSizeSp,
                  fontWeight = tpl.fontWeight,
                  textColor = tpl.textColor,
                  hasGradient = tpl.hasGradient,
                  gradientColorStart = tpl.gradientColorStart,
                  gradientColorEnd = tpl.gradientColorEnd,
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
          border = BorderStroke(
            1.5.dp,
            if (clip.fontFamily.equals(tpl.fontFamily, true) && clip.textColor == tpl.textColor) AmberAccent else StudioBorder
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(tpl.badgeEmoji, fontSize = 18.sp)
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = when {
                  tpl.category.equals("Urdu", true) || tpl.category.equals("Islamic", true) -> Color(0xFF10B981).copy(alpha = 0.2f)
                  tpl.category.equals("Business", true) -> AmberAccent.copy(alpha = 0.2f)
                  tpl.category.equals("YouTube", true) -> Color(0xFFFF3B30).copy(alpha = 0.2f)
                  else -> CyanAccent.copy(alpha = 0.2f)
                }
              ) {
                Text(
                  text = tpl.category,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = when {
                    tpl.category.equals("Urdu", true) || tpl.category.equals("Islamic", true) -> Color(0xFF10B981)
                    tpl.category.equals("Business", true) -> AmberAccent
                    tpl.category.equals("YouTube", true) -> Color(0xFFFF3B30)
                    else -> CyanAccent
                  },
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
            }

            Text(
              text = tpl.name,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            // Preview Box inside Template Card
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(tpl.backgroundColor.toInt()))
                .padding(4.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = tpl.sampleText,
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

// --- SUB TAB 2: URDU FONTS ---
@Composable
private fun UrduFontsSubTab(
  clip: TextClip,
  fonts: List<FontOption>,
  onSelectFont: (fontId: String, customPath: String?) -> Unit,
  onImportFont: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("🇵🇰 Urdu Calligraphy & Nastaliq Fonts", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
      }
      TextButton(onClick = onImportFont, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
        Icon(Icons.Default.FileOpen, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Import Font (+)", color = AmberAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(vertical = 4.dp)
    ) {
      items(fonts) { fontOpt ->
        val isSelected = clip.fontFamily.equals(fontOpt.id, true) ||
            (clip.customFontPath != null && clip.customFontPath == fontOpt.filePath)

        Card(
          modifier = Modifier
            .width(160.dp)
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
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
              }
            }

            // Urdu Calligraphy Sample Box
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(StudioSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (fontOpt.nativeSample.isNotBlank()) fontOpt.nativeSample else "جمیل نوری نستعلیق",
                color = Color(0xFFFFD700),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }
    }
  }
}

// --- SUB TAB 3: ENGLISH FONTS ---
@Composable
private fun EnglishFontsSubTab(
  clip: TextClip,
  fonts: List<FontOption>,
  onSelectFont: (fontId: String, customPath: String?) -> Unit,
  onImportFont: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("🔤 English Typefaces & Display Fonts", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
      TextButton(onClick = onImportFont, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
        Icon(Icons.Default.FileOpen, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Import TTF/OTF", color = CyanAccent, fontSize = 11.sp)
      }
    }

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(vertical = 4.dp)
    ) {
      items(fonts) { fontOpt ->
        val isSelected = clip.fontFamily.equals(fontOpt.id, true) ||
            (clip.customFontPath != null && clip.customFontPath == fontOpt.filePath)

        Card(
          modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelectFont(fontOpt.id, fontOpt.filePath) },
          colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CyanAccent.copy(alpha = 0.2f) else StudioSurfaceVariant
          ),
          border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) CyanAccent else StudioBorder
          )
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
              }
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(StudioSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (fontOpt.nativeSample.isNotBlank()) fontOpt.nativeSample else "SAMPLE TEXT",
                color = CyanAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }
    }
  }
}

// --- SUB TAB 4: STYLE & COLOR ---
@Composable
private fun StyleAndColorSettings(
  clip: TextClip,
  onUpdate: (TextClip) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // 1. Text Formatting Toggles (Bold, Italic, Underline, ALL CAPS)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Format & Style", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Bold
        FilterChip(
          selected = clip.fontWeight >= 700,
          onClick = { onUpdate(clip.copy(fontWeight = if (clip.fontWeight >= 700) 400 else 800)) },
          label = { Text("B", fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = Color.Black)
        )

        // Italic
        FilterChip(
          selected = clip.isItalic,
          onClick = { onUpdate(clip.copy(isItalic = !clip.isItalic)) },
          label = { Text("I", fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleAccent, selectedLabelColor = Color.White)
        )

        // Underline
        FilterChip(
          selected = clip.isUnderline,
          onClick = { onUpdate(clip.copy(isUnderline = !clip.isUnderline)) },
          label = { Text("U", textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AmberAccent, selectedLabelColor = Color.Black)
        )

        // All Caps
        FilterChip(
          selected = clip.isAllCaps,
          onClick = { onUpdate(clip.copy(isAllCaps = !clip.isAllCaps)) },
          label = { Text("TT", fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF10B981), selectedLabelColor = Color.Black)
        )
      }
    }

    // Alignment Setup
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Text Alignment", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Left", "Center", "Right").forEach { align ->
          FilterChip(
            selected = clip.alignment.equals(align, true),
            onClick = { onUpdate(clip.copy(alignment = align)) },
            label = { Text(align) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = Color.Black)
          )
        }
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
        valueRange = 12f..100f,
        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
      )
    }

    // Text Color Swatches
    Text("Text Color", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
    val palette = listOf(
      0xFFFFFFFF, 0xFFFFD700, 0xFF00E5FF, 0xFFFF007F, 0xFF10B981, 0xFF8B5CF6,
      0xFFF59E0B, 0xFFEF4444, 0xFFFFEA00, 0xFF3B82F6, 0xFFEC4899, 0xFF000000
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(palette) { colorHex ->
        val isSelected = clip.textColor == colorHex
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(colorHex))
            .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) CyanAccent else Color.White.copy(alpha = 0.4f), CircleShape)
            .clickable { onUpdate(clip.copy(textColor = colorHex)) }
        )
      }
    }

    // Gradient Fill
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Gradient Fill Effect", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
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
        listOf("Horizontal", "Vertical", "Diagonal").forEach { dir ->
          FilterChip(
            selected = clip.gradientDirection.equals(dir, true),
            onClick = { onUpdate(clip.copy(gradientDirection = dir)) },
            label = { Text(dir) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleAccent, selectedLabelColor = Color.White)
          )
        }
      }
    }

    // Outline Stroke Width
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Outline Stroke: ${clip.strokeWidth.toInt()}px", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
      Slider(
        value = clip.strokeWidth,
        onValueChange = { onUpdate(clip.copy(strokeWidth = it)) },
        valueRange = 0f..16f,
        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        colors = SliderDefaults.colors(thumbColor = AmberAccent, activeTrackColor = AmberAccent)
      )
    }

    // Drop Shadow
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

    // Rounded Background Box
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Background Box Badge", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
      Switch(
        checked = clip.hasBackground,
        onCheckedChange = { onUpdate(clip.copy(hasBackground = it)) },
        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
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
            colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981))
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("Padding: ${clip.bgPadding.toInt()}dp", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
          Slider(
            value = clip.bgPadding,
            onValueChange = { onUpdate(clip.copy(bgPadding = it)) },
            valueRange = 4f..32f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981))
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

// --- SUB TAB 5: MOTION & POSITION ---
@Composable
private fun MotionAndPositionSettings(
  clip: TextClip,
  onUpdate: (TextClip) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("Entrance Motion Animation", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
    val animList = listOf("Pop", "Fade", "Slide", "Zoom", "Bounce", "Typewriter", "Shake", "None")
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

    // Position Coordinates & Quick Placement
    Text("Quick Position Preset", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
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

// --- SUB TAB 6: CAPTIONS & TIMING ---
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
    // Timing Controls
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Start: ${formatDurationShort(clip.timelineStartMs)}", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
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
    }

    // Segment Actions
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
  }
}
