package com.example.ui.components.text

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.TextClip
import com.example.engine.SelectedTrackElement
import com.example.engine.text.TextLayerRenderer
import com.example.ui.StudioViewModel
import com.example.ui.theme.*
import java.util.Locale
import java.util.UUID

enum class TextStudioMainTab(val label: String) {
  TEMPLATES("Templates"),
  FONTS("Fonts"),
  STYLES("Styles"),
  EFFECTS("Effects"),
  ANIMATIONS("Animations")
}

enum class TemplateViewLayout {
  GRID_2COL, // 2-Column Large Preview Cards (Section 5 & 20)
  GRID_4COL, // 4-Column Compact Grid
  FEED       // Vertical List Showcase
}

/**
 * Checks if a string contains RTL (Urdu, Arabic, Persian) characters.
 */
fun isRtlText(text: String): Boolean {
  for (char in text) {
    val block = Character.UnicodeBlock.of(char)
    if (block == Character.UnicodeBlock.ARABIC ||
      block == Character.UnicodeBlock.ARABIC_SUPPLEMENT ||
      block == Character.UnicodeBlock.ARABIC_EXTENDED_A ||
      block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
      block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B
    ) {
      return true
    }
  }
  return false
}

@Composable
fun TextTemplatesBrowserPanel(
  viewModel: StudioViewModel,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("text_studio_prefs", Context.MODE_PRIVATE) }

  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  // Currently selected text clip if any
  val selectedTextClip = remember(timeline.textClips, selectedElement) {
    if (selectedElement is SelectedTrackElement.Text) {
      timeline.textClips.find { it.id == (selectedElement as SelectedTrackElement.Text).clipId }
    } else null
  }

  // Active top tab (Templates, Fonts, Styles, Effects, Animations)
  var activeMainTab by remember { mutableStateOf(TextStudioMainTab.TEMPLATES) }

  // Persisted Favorites & Recent templates
  val favoriteTemplateIds = remember {
    val saved = prefs.getStringSet("favorite_templates", emptySet()) ?: emptySet()
    mutableStateListOf<String>().apply { addAll(saved) }
  }

  val recentTemplateIds = remember {
    val saved = prefs.getString("recent_templates", "") ?: ""
    val list = saved.split(",").filter { it.isNotBlank() }
    mutableStateListOf<String>().apply { addAll(list) }
  }

  val toggleFavorite: (String) -> Unit = { id ->
    if (favoriteTemplateIds.contains(id)) {
      favoriteTemplateIds.remove(id)
    } else {
      favoriteTemplateIds.add(id)
    }
    prefs.edit().putStringSet("favorite_templates", favoriteTemplateIds.toSet()).apply()
  }

  val addRecent: (String) -> Unit = { id ->
    recentTemplateIds.remove(id)
    recentTemplateIds.add(0, id)
    while (recentTemplateIds.size > 20) {
      recentTemplateIds.removeAt(recentTemplateIds.size - 1)
    }
    prefs.edit().putString("recent_templates", recentTemplateIds.joinToString(",")).apply()
  }

  // User input text (Section 23: Large editable field, default 'Your Text Here')
  var userText by remember(selectedTextClip?.id) {
    mutableStateOf(selectedTextClip?.text?.ifBlank { "Your Text Here" } ?: "Your Text Here")
  }

  // Layout mode: 2-Col (default for mobile so preview is large), 4-Col, or Feed
  var viewLayout by remember { mutableStateOf(TemplateViewLayout.GRID_2COL) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var globalSpeedMultiplier by remember { mutableFloatStateOf(1.0f) }

  // Modal inspection / large preview state
  var inspectingTemplate by remember { mutableStateOf<TextTemplateItem?>(null) }
  var inspectingCustomText by remember { mutableStateOf("") }
  var inspectingSpeedMultiplier by remember { mutableFloatStateOf(1.0f) }
  var inspectingIsPlaying by remember { mutableStateOf(true) }
  var inspectingReplayTrigger by remember { mutableIntStateOf(0) }

  val installedPlugins by com.example.engine.plugin.PluginManager.installedPlugins.collectAsState()
  var pluginTemplates by remember { mutableStateOf(emptyList<TextTemplateItem>()) }

  // Load dynamically installed plugin templates
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
            ?: "New"
          val sample = (item.parameters["sampleText"] as? String)
            ?: (item.parameters["text"] as? String)
            ?: item.name
          val fontFam = (item.parameters["fontFamily"] as? String) ?: "Sans-Serif"

          list.add(
            TextTemplateItem(
              id = item.id,
              name = item.name,
              category = category,
              sampleText = sample,
              fontFamily = fontFam,
              badgeEmoji = item.emoji
            )
          )
        }
      }
    }
    pluginTemplates = list
  }

  val allTemplates = remember(pluginTemplates) {
    ALL_TEXT_TEMPLATES + pluginTemplates
  }

  // Filter templates by category and search query
  val displayedTemplates = remember(
    selectedCategory,
    searchQuery,
    allTemplates,
    favoriteTemplateIds.toList(),
    recentTemplateIds.toList()
  ) {
    var filtered = allTemplates
    when (selectedCategory) {
      "Favorites" -> {
        filtered = filtered.filter { it.id in favoriteTemplateIds }
      }
      "Trending" -> {
        filtered = filtered.filter {
          it.category.equals("Trending", ignoreCase = true) || it.isTrending || it.tags.contains("Trending", ignoreCase = true)
        }
      }
      "Recently Used" -> {
        val idOrder = recentTemplateIds.mapIndexed { idx, id -> id to idx }.toMap()
        filtered = filtered.filter { it.id in recentTemplateIds }
          .sortedBy { idOrder[it.id] ?: 999 }
      }
      "All" -> {
        // Show all
      }
      else -> {
        filtered = filtered.filter { it.category.equals(selectedCategory, ignoreCase = true) }
      }
    }

    if (searchQuery.isNotBlank()) {
      filtered = filtered.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
          it.category.contains(searchQuery, ignoreCase = true) ||
          it.sampleText.contains(searchQuery, ignoreCase = true) ||
          it.animationType.contains(searchQuery, ignoreCase = true) ||
          it.tags.contains(searchQuery, ignoreCase = true)
      }
    }
    filtered
  }

  // Action to add / apply template directly to the timeline
  val applyTemplateAction: (TextTemplateItem, String?, Float) -> Unit = { tpl, customTextOverride, speedMult ->
    addRecent(tpl.id)
    val textToApply = if (!customTextOverride.isNullOrBlank()) {
      customTextOverride
    } else if (userText.isNotBlank()) {
      userText
    } else tpl.sampleText

    val effectiveAnimDuration = (tpl.animDurationMs / speedMult).toLong().coerceIn(100L, 6000L)

    if (selectedTextClip != null) {
      viewModel.timelineEngine.updateTextClip(
        selectedTextClip.copy(
          text = textToApply,
          fontFamily = tpl.fontFamily,
          fontSizeSp = tpl.fontSizeSp,
          fontWeight = tpl.fontWeight,
          isItalic = tpl.isItalic,
          isUnderline = tpl.isUnderline,
          isAllCaps = tpl.isAllCaps,
          alignment = tpl.alignment,
          letterSpacing = tpl.letterSpacing,
          lineSpacing = tpl.lineSpacing,
          textColor = tpl.textColor,
          hasGradient = tpl.hasGradient,
          gradientColorStart = tpl.gradientColorStart,
          gradientColorEnd = tpl.gradientColorEnd,
          gradientDirection = tpl.gradientDirection,
          strokeWidth = tpl.strokeWidth,
          strokeColor = tpl.strokeColor,
          hasShadow = tpl.hasShadow,
          shadowColor = tpl.shadowColor,
          shadowBlur = tpl.shadowBlur,
          shadowOffsetX = tpl.shadowOffsetX,
          shadowOffsetY = tpl.shadowOffsetY,
          hasGlow = tpl.hasGlow,
          glowColor = tpl.glowColor,
          glowRadius = tpl.glowRadius,
          hasBackground = tpl.hasBackground,
          backgroundColor = tpl.backgroundColor,
          backgroundShape = tpl.backgroundShape,
          cornerRadius = tpl.cornerRadius,
          bgPadding = tpl.bgPadding,
          opacity = tpl.opacity,
          posX = tpl.posX,
          posY = tpl.posY,
          scale = tpl.scale,
          rotation = tpl.rotation,
          animationType = tpl.animationType,
          animationIn = tpl.animationIn,
          animationOut = tpl.animationOut,
          animDurationMs = effectiveAnimDuration,
          animationDelayMs = tpl.animationDelayMs,
          animationEasing = tpl.animationEasing
        )
      )
      if (tpl.secondaryLayers.isNotEmpty()) {
        val playhead = selectedTextClip.timelineStartMs
        val dur = selectedTextClip.durationMs
        tpl.secondaryLayers.forEach { sec ->
          val newSecClip = sec.toTextClip(playhead, dur).copy(
            animDurationMs = (sec.animDurationMs / speedMult).toLong().coerceIn(100L, 6000L)
          )
          viewModel.timelineEngine.addTextClipObject(newSecClip)
        }
      }
    } else {
      val playhead = viewModel.timelineEngine.currentPositionMs.value
      val clips = tpl.toTextClips(
        timelineStartMs = playhead,
        durationMs = 3000L,
        customTextOverride = textToApply
      )
      clips.forEach { clip ->
        val scaledClip = clip.copy(
          animDurationMs = (clip.animDurationMs / speedMult).toLong().coerceIn(100L, 6000L)
        )
        viewModel.timelineEngine.addTextClipObject(scaledClip)
      }
      if (clips.isNotEmpty()) {
        viewModel.timelineEngine.selectElement(SelectedTrackElement.Text(clips.first().id))
      }
    }
  }

  // Create & Add a new text layer (Section 22: + Add Text button)
  val addNewTextAction: () -> Unit = {
    val playhead = viewModel.timelineEngine.currentPositionMs.value
    val newClip = TextClip(
      id = UUID.randomUUID().toString(),
      text = if (userText.isNotBlank()) userText else "Your Text Here",
      timelineStartMs = playhead,
      durationMs = 3000L,
      fontSizeSp = 30f,
      textColor = 0xFFFFFFFF,
      animationType = "Pop"
    )
    viewModel.timelineEngine.addTextClipObject(newClip)
    viewModel.timelineEngine.selectElement(SelectedTrackElement.Text(newClip.id))
  }

  val isRtl = isRtlText(userText)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFF0B0F19))
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // -------------------------------------------------------------
    // SECTION 22: TOP TEXT STUDIO HEADER
    // -------------------------------------------------------------
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFF59E0B)))),
          contentAlignment = Alignment.Center
        ) {
          Text("Tt", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
        }
        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              "Text Studio",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            // LIVE badge indicator
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                  modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                )
                Text("LIVE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
              }
            }
          }
          Text(
            "Templates • Fonts • Styles • Effects • Animations",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Right side: + Add Text button
        Button(
          onClick = addNewTextAction,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = AmberAccent,
            contentColor = Color.Black
          ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Text("+ Add Text", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Close button: X
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(30.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }
    }

    // -------------------------------------------------------------
    // SECTION 23: TEXT INPUT (TEXT CONTENT - Urdu & English Supported)
    // -------------------------------------------------------------
    OutlinedTextField(
      value = userText,
      onValueChange = { newText ->
        userText = newText
        if (selectedTextClip != null) {
          viewModel.timelineEngine.updateTextClip(selectedTextClip.copy(text = newText))
        }
      },
      label = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          Text("TEXT CONTENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
          Text("• English & اردو Supported", fontSize = 9.sp, color = TextSecondary)
        }
      },
      placeholder = { Text("Type here in English, اردو or any script...", color = TextSecondary, fontSize = 11.sp) },
      trailingIcon = {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 6.dp)) {
          Text(
            "${userText.length} chars",
            fontSize = 9.sp,
            color = TextSecondary,
            modifier = Modifier.padding(end = 4.dp)
          )
          if (userText.isNotEmpty()) {
            IconButton(
              onClick = {
                userText = ""
                if (selectedTextClip != null) {
                  viewModel.timelineEngine.updateTextClip(selectedTextClip.copy(text = ""))
                }
              },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.Clear, contentDescription = "Clear text", tint = TextSecondary, modifier = Modifier.size(14.dp))
            }
          }
        }
      },
      singleLine = true,
      textStyle = LocalTextStyle.current.copy(
        fontSize = 13.sp,
        textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Content
      ),
      modifier = Modifier.fillMaxWidth(),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AmberAccent,
        unfocusedBorderColor = Color(0xFF1E293B),
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = Color(0xFF131826),
        unfocusedContainerColor = Color(0xFF131826)
      )
    )

    // -------------------------------------------------------------
    // SECTION 24: MAIN TABS (Templates | Fonts | Styles | Effects | Animations)
    // -------------------------------------------------------------
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(TextStudioMainTab.values()) { tab ->
        val isSelected = activeMainTab == tab
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { activeMainTab = tab },
          color = if (isSelected) AmberAccent else Color(0xFF1E293B),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = tab.label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }
    }

    // -------------------------------------------------------------
    // TAB CONTENTS
    // -------------------------------------------------------------
    when (activeMainTab) {
      TextStudioMainTab.TEMPLATES -> {
        // ---------------------------------------------------------
        // SECTION 13 & 20: SEARCH BAR + VIEW LAYOUT TOGGLE
        // ---------------------------------------------------------
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Search input
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search neon, 3D, Urdu, cinematic, sale...", color = TextSecondary, fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary, modifier = Modifier.size(14.dp))
                }
              }
            },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .height(42.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AmberAccent,
              unfocusedBorderColor = Color(0xFF1E293B),
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedContainerColor = Color(0xFF131826),
              unfocusedContainerColor = Color(0xFF131826)
            )
          )

          // View Layout selector (2-Col, 4-Col, Feed)
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF131826))
              .padding(2.dp)
          ) {
            IconButton(
              onClick = { viewLayout = TemplateViewLayout.GRID_2COL },
              modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (viewLayout == TemplateViewLayout.GRID_2COL) AmberAccent else Color.Transparent)
            ) {
              Icon(
                Icons.Default.GridView,
                contentDescription = "2-Col Grid",
                tint = if (viewLayout == TemplateViewLayout.GRID_2COL) Color.Black else TextSecondary,
                modifier = Modifier.size(16.dp)
              )
            }

            IconButton(
              onClick = { viewLayout = TemplateViewLayout.GRID_4COL },
              modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (viewLayout == TemplateViewLayout.GRID_4COL) AmberAccent else Color.Transparent)
            ) {
              Icon(
                Icons.Default.Apps,
                contentDescription = "4-Col Compact",
                tint = if (viewLayout == TemplateViewLayout.GRID_4COL) Color.Black else TextSecondary,
                modifier = Modifier.size(16.dp)
              )
            }

            IconButton(
              onClick = { viewLayout = TemplateViewLayout.FEED },
              modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (viewLayout == TemplateViewLayout.FEED) AmberAccent else Color.Transparent)
            ) {
              Icon(
                Icons.Default.ViewAgenda,
                contentDescription = "Feed Showcase",
                tint = if (viewLayout == TemplateViewLayout.FEED) Color.Black else TextSecondary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        // ---------------------------------------------------------
        // SECTION 25: CATEGORY NAVIGATION (Horizontal Scrolling Bar)
        // ---------------------------------------------------------
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          // Favorites category
          item {
            val isFav = selectedCategory == "Favorites"
            FilterChip(
              selected = isFav,
              onClick = { selectedCategory = "Favorites" },
              label = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(
                    if (isFav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFav) Color.Black else Color(0xFFF43F5E),
                    modifier = Modifier.size(13.dp)
                  )
                  Text("Favorites (${favoriteTemplateIds.size})", fontSize = 11.sp, fontWeight = if (isFav) FontWeight.Bold else FontWeight.Normal)
                }
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AmberAccent,
                selectedLabelColor = Color.Black,
                containerColor = Color(0xFF131826),
                labelColor = TextPrimary
              )
            )
          }

          // Recently Used category
          item {
            val isRecent = selectedCategory == "Recently Used"
            FilterChip(
              selected = isRecent,
              onClick = { selectedCategory = "Recently Used" },
              label = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = if (isRecent) Color.Black else Color(0xFF38BDF8),
                    modifier = Modifier.size(13.dp)
                  )
                  Text("Recently Used (${recentTemplateIds.size})", fontSize = 11.sp, fontWeight = if (isRecent) FontWeight.Bold else FontWeight.Normal)
                }
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AmberAccent,
                selectedLabelColor = Color.Black,
                containerColor = Color(0xFF131826),
                labelColor = TextPrimary
              )
            )
          }

          // All 28 predefined categories
          items(TemplateCategories.ALL_CATEGORIES) { catInfo ->
            val isSelected = selectedCategory == catInfo.name
            FilterChip(
              selected = isSelected,
              onClick = { selectedCategory = catInfo.name },
              label = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(catInfo.iconEmoji, fontSize = 12.sp)
                  Text(catInfo.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF6366F1),
                selectedLabelColor = Color.White,
                containerColor = Color(0xFF131826),
                labelColor = TextSecondary
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = isSelected,
                borderColor = Color(0xFF1E293B),
                selectedBorderColor = Color(0xFF6366F1)
              )
            )
          }
        }

        // ---------------------------------------------------------
        // SECTION 5 & 20: LIVE TEMPLATE PREVIEW CARDS
        // ---------------------------------------------------------
        if (displayedTemplates.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("No templates found", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Text("Try searching a different keyword or category", color = TextSecondary, fontSize = 11.sp)
            }
          }
        } else {
          val effectiveFeedSpeed = globalSpeedMultiplier

          when (viewLayout) {
            TemplateViewLayout.GRID_2COL -> {
              val gridState = rememberLazyGridState()
              val visibleKeys by remember {
                derivedStateOf {
                  gridState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet()
                }
              }

              LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 320.dp, max = 440.dp)
              ) {
                items(displayedTemplates, key = { it.id }) { tpl ->
                  val isFavorite = tpl.id in favoriteTemplateIds
                  val isCardVisible = visibleKeys.isEmpty() || visibleKeys.contains(tpl.id)

                  ModernGridTemplateCard(
                    tpl = tpl,
                    customPreviewText = userText.ifBlank { null },
                    speedMultiplier = effectiveFeedSpeed,
                    isFavorite = isFavorite,
                    isVisible = isCardVisible,
                    onToggleFavorite = { toggleFavorite(tpl.id) },
                    onCardClick = {
                      inspectingTemplate = tpl
                      inspectingCustomText = userText.ifBlank { tpl.sampleText }
                      inspectingSpeedMultiplier = effectiveFeedSpeed
                      inspectingIsPlaying = true
                    },
                    onQuickUse = {
                      applyTemplateAction(tpl, userText.ifBlank { null }, effectiveFeedSpeed)
                    }
                  )
                }
              }
            }

            TemplateViewLayout.GRID_4COL -> {
              val gridState = rememberLazyGridState()
              val visibleKeys by remember {
                derivedStateOf {
                  gridState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet()
                }
              }

              LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 320.dp, max = 440.dp)
              ) {
                items(displayedTemplates, key = { it.id }) { tpl ->
                  val isFavorite = tpl.id in favoriteTemplateIds
                  val isCardVisible = visibleKeys.isEmpty() || visibleKeys.contains(tpl.id)

                  GridShowcaseTemplateCard(
                    tpl = tpl,
                    customPreviewText = userText.ifBlank { null },
                    speedMultiplier = effectiveFeedSpeed,
                    isFavorite = isFavorite,
                    isSelected = selectedTextClip != null && selectedTextClip.fontFamily.equals(tpl.fontFamily, ignoreCase = true),
                    isVisible = isCardVisible,
                    onToggleFavorite = { toggleFavorite(tpl.id) },
                    onCardClick = {
                      inspectingTemplate = tpl
                      inspectingCustomText = userText.ifBlank { tpl.sampleText }
                      inspectingSpeedMultiplier = effectiveFeedSpeed
                      inspectingIsPlaying = true
                    },
                    onInspectClick = {
                      inspectingTemplate = tpl
                      inspectingCustomText = userText.ifBlank { tpl.sampleText }
                      inspectingSpeedMultiplier = effectiveFeedSpeed
                      inspectingIsPlaying = true
                    }
                  )
                }
              }
            }

            TemplateViewLayout.FEED -> {
              val listState = rememberLazyListState()
              val visibleKeys by remember {
                derivedStateOf {
                  listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet()
                }
              }

              LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 320.dp, max = 440.dp)
              ) {
                items(displayedTemplates, key = { it.id }) { tpl ->
                  val isFavorite = tpl.id in favoriteTemplateIds
                  val isVisible = visibleKeys.contains(tpl.id)

                  VerticalShowcaseTemplateCard(
                    tpl = tpl,
                    customPreviewText = userText.ifBlank { null },
                    speedMultiplier = effectiveFeedSpeed,
                    isFavorite = isFavorite,
                    isVisible = isVisible,
                    onToggleFavorite = { toggleFavorite(tpl.id) },
                    onCardClick = {
                      inspectingTemplate = tpl
                      inspectingCustomText = userText.ifBlank { tpl.sampleText }
                      inspectingSpeedMultiplier = effectiveFeedSpeed
                      inspectingIsPlaying = true
                    },
                    onUseTemplate = {
                      applyTemplateAction(tpl, userText.ifBlank { null }, effectiveFeedSpeed)
                    }
                  )
                }
              }
            }
          }
        }
      }

      TextStudioMainTab.FONTS -> {
        // Quick Font family selector
        FontsQuickPicker(
          selectedFont = selectedTextClip?.fontFamily ?: "Impact",
          onSelectFont = { fontName ->
            if (selectedTextClip != null) {
              viewModel.timelineEngine.updateTextClip(selectedTextClip.copy(fontFamily = fontName))
            }
          }
        )
      }

      TextStudioMainTab.STYLES -> {
        // Text styling quick panel
        StylesQuickPicker(
          clip = selectedTextClip,
          onUpdate = { updated -> viewModel.timelineEngine.updateTextClip(updated) }
        )
      }

      TextStudioMainTab.EFFECTS -> {
        // Glow / Shadow / Gradient quick panel
        EffectsQuickPicker(
          clip = selectedTextClip,
          onUpdate = { updated -> viewModel.timelineEngine.updateTextClip(updated) }
        )
      }

      TextStudioMainTab.ANIMATIONS -> {
        // Animation in/out quick panel
        AnimationsQuickPicker(
          clip = selectedTextClip,
          onUpdate = { updated -> viewModel.timelineEngine.updateTextClip(updated) }
        )
      }
    }
  }

  // -------------------------------------------------------------
  // SECTION 12: FULLSCREEN / LARGE TEMPLATE PREVIEW MODAL
  // -------------------------------------------------------------
  inspectingTemplate?.let { tpl ->
    val isFav = tpl.id in favoriteTemplateIds

    Dialog(onDismissRequest = { inspectingTemplate = null }) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(6.dp)
          .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Header: Name, Category, Close
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFEF4444))
              )
              Text("LIVE PREVIEW", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
              Text("•", color = TextSecondary)
              Text(tpl.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            IconButton(
              onClick = { inspectingTemplate = null },
              modifier = Modifier.size(26.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
          }

          // Large Live Animated Vector Preview Canvas
          LargeLivePreviewCanvas(
            tpl = tpl,
            previewText = inspectingCustomText.ifBlank { tpl.sampleText },
            speedMultiplier = inspectingSpeedMultiplier,
            isPlaying = inspectingIsPlaying,
            replayTrigger = inspectingReplayTrigger,
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp)
          )

          // SECTION 12: CONTROLS (Play, Pause, Replay) + Speed Multiplier
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              // Play / Pause button
              FilledTonalIconButton(
                onClick = { inspectingIsPlaying = !inspectingIsPlaying },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  if (inspectingIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = if (inspectingIsPlaying) "Pause" else "Play",
                  modifier = Modifier.size(16.dp)
                )
              }

              // Replay button
              FilledTonalIconButton(
                onClick = {
                  inspectingReplayTrigger++
                  inspectingIsPlaying = true
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.Replay, contentDescription = "Replay", modifier = Modifier.size(16.dp))
              }

              // Favorite toggle
              FilledTonalIconButton(
                onClick = { toggleFavorite(tpl.id) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  if (isFav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                  contentDescription = "Favorite",
                  tint = if (isFav) Color(0xFFF43F5E) else TextSecondary,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            // Speed multiplier indicator
            Surface(
              color = AmberAccent.copy(alpha = 0.2f),
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(0.5.dp, AmberAccent)
            ) {
              Text(
                text = "${String.format(Locale.US, "%.1f", inspectingSpeedMultiplier)}x Speed",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AmberAccent,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }

          // Quick Speed chips (0.5x, 1x, 1.5x, 2.5x)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val speedOptions = listOf(0.5f to "0.5x Slow", 1.0f to "1.0x Normal", 1.5f to "1.5x Fast", 2.5f to "2.5x Bounce")
            speedOptions.forEach { (sp, label) ->
              val isSel = kotlin.math.abs(inspectingSpeedMultiplier - sp) < 0.08f
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .clickable { inspectingSpeedMultiplier = sp },
                color = if (isSel) AmberAccent else Color(0xFF1E293B),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = label,
                  fontSize = 8.sp,
                  fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSel) Color.Black else TextSecondary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }

          // Edit text field inside modal
          OutlinedTextField(
            value = inspectingCustomText,
            onValueChange = { inspectingCustomText = it },
            label = { Text("Custom Preview Text", fontSize = 9.sp, color = AmberAccent) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AmberAccent,
              unfocusedBorderColor = Color(0xFF1E293B),
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedContainerColor = Color(0xFF131826),
              unfocusedContainerColor = Color(0xFF131826)
            )
          )

          // Meta Info Pills
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
              Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ANIMATION", fontSize = 7.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(tpl.animationType, fontSize = 9.sp, color = AmberAccent, fontWeight = FontWeight.Bold, maxLines = 1)
              }
            }
            Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
              Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DURATION", fontSize = 7.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(tpl.durationBadge, fontSize = 9.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
              }
            }
            Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
              Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FONT", fontSize = 7.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(tpl.fontFamily, fontSize = 9.sp, color = Color(0xFFA855F7), fontWeight = FontWeight.Bold, maxLines = 1)
              }
            }
          }

          // Apply Template Button
          Button(
            onClick = {
              applyTemplateAction(tpl, inspectingCustomText.ifBlank { null }, inspectingSpeedMultiplier)
              inspectingTemplate = null
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(42.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
              Text("Apply Template", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

/**
 * 2-Column Modern Template Card (Section 5 Design)
 * Displays large animated vector preview, ▶ LIVE badge, Template Name + Heart, Category • Duration
 */
@Composable
private fun ModernGridTemplateCard(
  tpl: TextTemplateItem,
  customPreviewText: String?,
  speedMultiplier: Float,
  isFavorite: Boolean,
  isVisible: Boolean,
  onToggleFavorite: () -> Unit,
  onCardClick: () -> Unit,
  onQuickUse: () -> Unit
) {
  val context = LocalContext.current
  val effectivePreviewText = if (!customPreviewText.isNullOrBlank()) customPreviewText else tpl.sampleText

  val infiniteTransition = rememberInfiniteTransition(label = "card_anim_${tpl.id}_${effectivePreviewText.hashCode()}")
  val animLoopMs by if (isVisible) {
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 2400f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 2400, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "loop_ms"
    )
  } else {
    remember { mutableFloatStateOf(0f) }
  }

  val primaryConfig = remember(tpl, effectivePreviewText) {
    tpl.toPrimaryLayerConfig().copy(text = effectivePreviewText)
  }
  val primaryClip = remember(primaryConfig) {
    primaryConfig.toTextClip(timelineStartMs = 0L, durationMs = 3000L)
  }

  val secondaryClips = remember(tpl.secondaryLayers) {
    tpl.secondaryLayers.map { sec ->
      sec.toTextClip(timelineStartMs = 0L, durationMs = 3000L)
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onCardClick() },
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
    border = BorderStroke(1.dp, if (tpl.isPremium) AmberAccent.copy(alpha = 0.5f) else Color(0xFF1E293B))
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // 1. Large Live Animated Canvas Viewport
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(100.dp)
          .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
          .background(Color(0xFF090D16))
          .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val nativeCanvas = drawContext.canvas.nativeCanvas
          val w = size.width.toInt()
          val h = size.height.toInt()
          if (w > 0 && h > 0) {
            val hasMultipleLayers = secondaryClips.isNotEmpty()
            val primaryPosY = if (hasMultipleLayers) -0.20f else 0.0f

            TextLayerRenderer.drawTemplatePreview(
              canvas = nativeCanvas,
              clip = primaryClip,
              previewLoopMs = animLoopMs.toLong(),
              width = w,
              height = h,
              context = context,
              overridePosY = primaryPosY,
              speedMultiplier = speedMultiplier
            )

            if (hasMultipleLayers) {
              secondaryClips.forEachIndexed { idx, secClip ->
                val secPosY = 0.25f + (idx * 0.25f)
                TextLayerRenderer.drawTemplatePreview(
                  canvas = nativeCanvas,
                  clip = secClip,
                  previewLoopMs = animLoopMs.toLong(),
                  width = w,
                  height = h,
                  context = context,
                  overridePosY = secPosY,
                  speedMultiplier = speedMultiplier
                )
              }
            }
          }
        }

        // Top-Left: ▶ LIVE badge
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
              modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
            )
            Text("LIVE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
          }
        }

        // Top-Right: HD / PRO badge & Duration
        Row(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          if (tpl.isPremium) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF00E5FF).copy(alpha = 0.25f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
              Text("PRO", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E5FF))
            }
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(Color(0x99000000))
              .padding(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Text(tpl.durationBadge, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }

      // 2. Card Bottom Bar (Template Name, Favorite, Category • Style)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = tpl.name,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )

          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(20.dp)
          ) {
            Icon(
              if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription = "Favorite",
              tint = if (isFavorite) Color(0xFFF43F5E) else TextSecondary,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${tpl.category} • ${tpl.animationType}",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )

          // Quick Use button
          Text(
            text = "+ Use",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = AmberAccent,
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .clickable { onQuickUse() }
              .padding(horizontal = 4.dp, vertical = 1.dp)
          )
        }
      }
    }
  }
}

/**
 * 4-Column Screenshot Grid Card (CapCut style compact view)
 */
@Composable
private fun GridShowcaseTemplateCard(
  tpl: TextTemplateItem,
  customPreviewText: String?,
  speedMultiplier: Float,
  isFavorite: Boolean,
  isSelected: Boolean,
  isVisible: Boolean,
  onToggleFavorite: () -> Unit,
  onCardClick: () -> Unit,
  onInspectClick: () -> Unit
) {
  val context = LocalContext.current
  val effectivePreviewText = if (!customPreviewText.isNullOrBlank()) customPreviewText else tpl.sampleText

  val infiniteTransition = rememberInfiniteTransition(label = "grid_anim_${tpl.id}_${effectivePreviewText.hashCode()}")
  val animLoopMs by if (isVisible) {
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 2400f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 2400, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "grid_loop_ms"
    )
  } else {
    remember { mutableFloatStateOf(0f) }
  }

  val primaryConfig = remember(tpl, effectivePreviewText) {
    tpl.toPrimaryLayerConfig().copy(text = effectivePreviewText)
  }
  val primaryClip = remember(primaryConfig) {
    primaryConfig.toTextClip(timelineStartMs = 0L, durationMs = 3000L)
  }

  val secondaryClips = remember(tpl.secondaryLayers) {
    tpl.secondaryLayers.map { sec ->
      sec.toTextClip(timelineStartMs = 0L, durationMs = 3000L)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(1.0f)
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF131826))
      .border(
        width = if (isSelected) 2.dp else 0.5.dp,
        color = if (isSelected) AmberAccent else Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp)
      )
      .clickable { onCardClick() },
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val nativeCanvas = drawContext.canvas.nativeCanvas
      val w = size.width.toInt()
      val h = size.height.toInt()
      if (w > 0 && h > 0) {
        val hasMultipleLayers = secondaryClips.isNotEmpty()
        val primaryPosY = if (hasMultipleLayers) -0.20f else 0.0f

        TextLayerRenderer.drawTemplatePreview(
          canvas = nativeCanvas,
          clip = primaryClip,
          previewLoopMs = animLoopMs.toLong(),
          width = w,
          height = h,
          context = context,
          overridePosY = primaryPosY,
          speedMultiplier = speedMultiplier
        )

        if (hasMultipleLayers) {
          secondaryClips.forEachIndexed { idx, secClip ->
            val secPosY = 0.25f + (idx * 0.25f)
            TextLayerRenderer.drawTemplatePreview(
              canvas = nativeCanvas,
              clip = secClip,
              previewLoopMs = animLoopMs.toLong(),
              width = w,
              height = h,
              context = context,
              overridePosY = secPosY,
              speedMultiplier = speedMultiplier
            )
          }
        }
      }
    }

    if (tpl.isPremium) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(3.dp)
          .size(13.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(Color(0xFF00E5FF).copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Diamond, contentDescription = "VIP", tint = Color(0xFF00E5FF), modifier = Modifier.size(9.dp))
      }
    }

    if (isFavorite) {
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(3.dp)
          .size(12.dp)
          .clip(CircleShape)
          .background(Color(0xFFF43F5E)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Favorite, contentDescription = "Fav", tint = Color.White, modifier = Modifier.size(8.dp))
      }
    }
  }
}

/**
 * Vertical Showcase Card (Feed layout)
 */
@Composable
private fun VerticalShowcaseTemplateCard(
  tpl: TextTemplateItem,
  customPreviewText: String?,
  speedMultiplier: Float,
  isFavorite: Boolean,
  isVisible: Boolean,
  onToggleFavorite: () -> Unit,
  onCardClick: () -> Unit,
  onUseTemplate: () -> Unit
) {
  val context = LocalContext.current
  val effectivePreviewText = if (!customPreviewText.isNullOrBlank()) customPreviewText else tpl.sampleText

  val infiniteTransition = rememberInfiniteTransition(label = "feed_anim_${tpl.id}_${effectivePreviewText.hashCode()}")
  val animLoopMs by if (isVisible) {
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 2400f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 2400, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "loop_ms"
    )
  } else {
    remember { mutableFloatStateOf(0f) }
  }

  val primaryConfig = remember(tpl, effectivePreviewText) {
    tpl.toPrimaryLayerConfig().copy(text = effectivePreviewText)
  }
  val primaryClip = remember(primaryConfig) {
    primaryConfig.toTextClip(timelineStartMs = 0L, durationMs = 3000L)
  }

  val secondaryClips = remember(tpl.secondaryLayers) {
    tpl.secondaryLayers.map { sec ->
      sec.toTextClip(timelineStartMs = 0L, durationMs = 3000L)
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onCardClick() },
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
    border = BorderStroke(1.dp, if (tpl.isPremium) AmberAccent.copy(alpha = 0.5f) else Color(0xFF1E293B))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(tpl.badgeEmoji, fontSize = 14.sp)
          Text(tpl.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp))
          Surface(
            color = Color(0xFF6366F1).copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = tpl.animationType,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF818CF8),
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }

        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(24.dp)) {
          Icon(
            if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Favorite",
            tint = if (isFavorite) Color(0xFFF43F5E) else TextSecondary,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(100.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF090D16))
          .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val nativeCanvas = drawContext.canvas.nativeCanvas
          val w = size.width.toInt()
          val h = size.height.toInt()
          if (w > 0 && h > 0) {
            val hasMultipleLayers = secondaryClips.isNotEmpty()
            val primaryPosY = if (hasMultipleLayers) -0.20f else 0.0f

            TextLayerRenderer.drawTemplatePreview(
              canvas = nativeCanvas,
              clip = primaryClip,
              previewLoopMs = animLoopMs.toLong(),
              width = w,
              height = h,
              context = context,
              overridePosY = primaryPosY,
              speedMultiplier = speedMultiplier
            )

            if (hasMultipleLayers) {
              secondaryClips.forEachIndexed { idx, secClip ->
                val secPosY = 0.25f + (idx * 0.25f)
                TextLayerRenderer.drawTemplatePreview(
                  canvas = nativeCanvas,
                  clip = secClip,
                  previewLoopMs = animLoopMs.toLong(),
                  width = w,
                  height = h,
                  context = context,
                  overridePosY = secPosY,
                  speedMultiplier = speedMultiplier
                )
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${tpl.category} • ${tpl.durationBadge}",
          style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
        )

        Button(
          onClick = onUseTemplate,
          shape = RoundedCornerShape(6.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
          modifier = Modifier.height(26.dp)
        ) {
          Text("Use Template", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * Large Live Preview Canvas for modal inspection with play/pause and replay controls
 */
@Composable
private fun LargeLivePreviewCanvas(
  tpl: TextTemplateItem,
  previewText: String,
  speedMultiplier: Float = 1.0f,
  isPlaying: Boolean = true,
  replayTrigger: Int = 0,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  val infiniteTransition = rememberInfiniteTransition(label = "modal_anim_${tpl.id}_${previewText.hashCode()}_$replayTrigger")
  val animLoopMs by if (isPlaying) {
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 2400f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 2400, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "modal_loop"
    )
  } else {
    remember { mutableFloatStateOf(600f) }
  }

  val primaryConfig = remember(tpl, previewText) {
    tpl.toPrimaryLayerConfig().copy(text = previewText)
  }
  val primaryClip = remember(primaryConfig) {
    primaryConfig.toTextClip(timelineStartMs = 0L, durationMs = 3000L)
  }

  val secondaryClips = remember(tpl.secondaryLayers) {
    tpl.secondaryLayers.map { sec ->
      sec.toTextClip(timelineStartMs = 0L, durationMs = 3000L)
    }
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF070B14))
      .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val nativeCanvas = drawContext.canvas.nativeCanvas
      val w = size.width.toInt()
      val h = size.height.toInt()
      if (w > 0 && h > 0) {
        val hasMultipleLayers = secondaryClips.isNotEmpty()
        val primaryPosY = if (hasMultipleLayers) -0.20f else 0.0f

        TextLayerRenderer.drawTemplatePreview(
          canvas = nativeCanvas,
          clip = primaryClip,
          previewLoopMs = animLoopMs.toLong(),
          width = w,
          height = h,
          context = context,
          overridePosY = primaryPosY,
          speedMultiplier = speedMultiplier
        )

        if (hasMultipleLayers) {
          secondaryClips.forEachIndexed { idx, secClip ->
            val secPosY = 0.25f + (idx * 0.25f)
            TextLayerRenderer.drawTemplatePreview(
              canvas = nativeCanvas,
              clip = secClip,
              previewLoopMs = animLoopMs.toLong(),
              width = w,
              height = h,
              context = context,
              overridePosY = secPosY,
              speedMultiplier = speedMultiplier
            )
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// COMPATIBLE SECONDARY TAB PICKERS (Fonts, Styles, Effects, Animations)
// -------------------------------------------------------------
@Composable
private fun FontsQuickPicker(
  selectedFont: String,
  onSelectFont: (String) -> Unit
) {
  val fonts = listOf(
    "Impact", "Montserrat", "Playfair Display", "Bebas Neue",
    "Pacifico", "Sans-Serif", "Serif", "Monospace",
    "Courier New", "Georgia", "Comic Sans", "Brush Script"
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 200.dp, max = 280.dp)
      .padding(vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text("SELECT FONT FAMILY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      items(fonts) { font ->
        val isSel = selectedFont.equals(font, ignoreCase = true)
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelectFont(font) },
          color = if (isSel) AmberAccent else Color(0xFF131826),
          border = BorderStroke(1.dp, if (isSel) AmberAccent else Color(0xFF1E293B)),
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(font, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else TextPrimary)
            if (isSel) {
              Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StylesQuickPicker(
  clip: TextClip?,
  onUpdate: (TextClip) -> Unit
) {
  if (clip == null) {
    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
      Text("Add or select a text layer to customize styles", color = TextSecondary, fontSize = 11.sp)
    }
    return
  }

  val colors = listOf(
    0xFFFFFFFF, 0xFFFACC15, 0xFFF43F5E, 0xFF38BDF8, 0xFF4ADE80, 0xFFA855F7, 0xFFFB923C, 0xFF000000
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 200.dp, max = 280.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text("TEXT COLOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      colors.forEach { colLong ->
        val isSel = clip.textColor == colLong
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(colLong))
            .border(if (isSel) 2.dp else 1.dp, if (isSel) AmberAccent else Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable { onUpdate(clip.copy(textColor = colLong)) }
        )
      }
    }

    Text("FONT SIZE: ${clip.fontSizeSp.toInt()} sp", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
    Slider(
      value = clip.fontSizeSp,
      onValueChange = { onUpdate(clip.copy(fontSizeSp = it)) },
      valueRange = 12f..72f,
      colors = SliderDefaults.colors(thumbColor = AmberAccent, activeTrackColor = AmberAccent)
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = clip.fontWeight >= 700,
        onClick = { onUpdate(clip.copy(fontWeight = if (clip.fontWeight >= 700) 400 else 800)) },
        label = { Text("Bold", fontSize = 11.sp) }
      )
      FilterChip(
        selected = clip.isItalic,
        onClick = { onUpdate(clip.copy(isItalic = !clip.isItalic)) },
        label = { Text("Italic", fontSize = 11.sp) }
      )
      FilterChip(
        selected = clip.isUnderline,
        onClick = { onUpdate(clip.copy(isUnderline = !clip.isUnderline)) },
        label = { Text("Underline", fontSize = 11.sp) }
      )
    }
  }
}

@Composable
private fun EffectsQuickPicker(
  clip: TextClip?,
  onUpdate: (TextClip) -> Unit
) {
  if (clip == null) {
    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
      Text("Add or select a text layer to customize effects", color = TextSecondary, fontSize = 11.sp)
    }
    return
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 200.dp, max = 280.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("SHADOW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      Switch(
        checked = clip.hasShadow,
        onCheckedChange = { onUpdate(clip.copy(hasShadow = it)) }
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("GLOW ACCENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      Switch(
        checked = clip.hasGlow,
        onCheckedChange = { onUpdate(clip.copy(hasGlow = it)) }
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("GRADIENT TEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      Switch(
        checked = clip.hasGradient,
        onCheckedChange = { onUpdate(clip.copy(hasGradient = it)) }
      )
    }
  }
}

@Composable
private fun AnimationsQuickPicker(
  clip: TextClip?,
  onUpdate: (TextClip) -> Unit
) {
  if (clip == null) {
    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
      Text("Add or select a text layer to customize animation", color = TextSecondary, fontSize = 11.sp)
    }
    return
  }

  val animTypes = listOf(
    "Fade In", "Slide Up", "Slide Left", "Slide Right",
    "Zoom In", "Zoom Out", "Bounce", "Typewriter",
    "Word Reveal", "Letter Reveal", "Neon Pulse", "Cinematic Reveal"
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 200.dp, max = 280.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text("CURRENT ANIMATION: ${clip.animationType}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      items(animTypes) { animName ->
        val isSel = clip.animationType.equals(animName, ignoreCase = true)
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onUpdate(clip.copy(animationType = animName)) },
          color = if (isSel) AmberAccent else Color(0xFF131826),
          border = BorderStroke(1.dp, if (isSel) AmberAccent else Color(0xFF1E293B)),
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(animName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else TextPrimary)
            if (isSel) {
              Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
          }
        }
      }
    }
  }
}
