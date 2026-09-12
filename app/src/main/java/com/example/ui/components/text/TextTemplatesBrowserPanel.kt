package com.example.ui.components.text

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

enum class TemplateBrowserMode {
  PREVIEW, // Authentic live preview of template designs
  EDIT     // Interactive editing with custom text & animation speed adjustment
}

@Composable
fun TextTemplatesBrowserPanel(
  viewModel: StudioViewModel,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  // Currently selected text clip if any
  val selectedTextClip = remember(timeline.textClips, selectedElement) {
    if (selectedElement is SelectedTrackElement.Text) {
      timeline.textClips.find { it.id == (selectedElement as SelectedTrackElement.Text).clipId }
    } else null
  }

  // Browser mode & controls
  var browserMode by remember { mutableStateOf(TemplateBrowserMode.PREVIEW) }
  var searchQuery by remember { mutableStateOf("") }
  var previewTextQuery by remember { mutableStateOf("") }
  var globalSpeedMultiplier by remember { mutableFloatStateOf(1.0f) }
  var selectedCategory by remember { mutableStateOf("Trending") }
  val favoriteTemplateIds = remember { mutableStateListOf<String>() }

  // Modal inspection / edit mode state for selected template
  var inspectingTemplate by remember { mutableStateOf<TextTemplateItem?>(null) }
  var inspectingCustomText by remember { mutableStateOf("") }
  var inspectingSpeedMultiplier by remember { mutableFloatStateOf(1.0f) }

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

  val displayedTemplates = remember(selectedCategory, searchQuery, allTemplates, favoriteTemplateIds.toList()) {
    var filtered = allTemplates
    if (selectedCategory == "Favorites") {
      filtered = filtered.filter { it.id in favoriteTemplateIds }
    } else if (selectedCategory != "All") {
      filtered = filtered.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    if (searchQuery.isNotBlank()) {
      filtered = filtered.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
          it.category.contains(searchQuery, ignoreCase = true) ||
          it.sampleText.contains(searchQuery, ignoreCase = true) ||
          it.animationType.contains(searchQuery, ignoreCase = true)
      }
    }
    filtered
  }

  // Action to add / apply template directly to the Screen Editor timeline with custom text & animation speed
  val applyTemplateAction: (TextTemplateItem, String?, Float) -> Unit = { tpl, customTextOverride, speedMult ->
    val textToApply = if (!customTextOverride.isNullOrBlank()) {
      customTextOverride
    } else if (previewTextQuery.isNotBlank()) {
      previewTextQuery
    } else null

    val effectiveAnimDuration = (tpl.animDurationMs / speedMult).toLong().coerceIn(100L, 6000L)

    if (selectedTextClip != null) {
      val textToKeep = textToApply ?: if (selectedTextClip.text.isBlank() || selectedTextClip.text == "Tap to edit" || selectedTextClip.text == "Your Text Here") {
        tpl.sampleText
      } else selectedTextClip.text

      viewModel.timelineEngine.updateTextClip(
        selectedTextClip.copy(
          text = textToKeep,
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

  // LazyListState to track viewport visibility for high-performance 60fps pausing
  val verticalListState = rememberLazyListState()
  val visibleItemKeys by remember {
    derivedStateOf {
      verticalListState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet()
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // 1. Header Bar: Title, Live Status Indicator, and Close
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
            .background(AmberAccent.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
        }
        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Live Text Templates", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
            // Pulsing LIVE dot
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
            )
            Text("LIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
          }
          Text("Vertical Showcase • ${displayedTemplates.size} Live Templates", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
        }
      }

      IconButton(
        onClick = onDismiss,
        modifier = Modifier.size(32.dp)
      ) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    // 2. PREVIEW VS EDIT MODE SEGMENTED TOGGLE
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(StudioSurfaceVariant)
        .padding(3.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      val isPreview = browserMode == TemplateBrowserMode.PREVIEW
      Surface(
        modifier = Modifier
          .weight(1f)
          .height(34.dp)
          .clip(RoundedCornerShape(8.dp))
          .clickable { browserMode = TemplateBrowserMode.PREVIEW },
        color = if (isPreview) AmberAccent else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = "Preview Mode",
            tint = if (isPreview) Color.Black else TextSecondary,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Preview Mode",
            fontSize = 11.sp,
            fontWeight = if (isPreview) FontWeight.Bold else FontWeight.Medium,
            color = if (isPreview) Color.Black else TextSecondary
          )
        }
      }

      val isEdit = browserMode == TemplateBrowserMode.EDIT
      Surface(
        modifier = Modifier
          .weight(1f)
          .height(34.dp)
          .clip(RoundedCornerShape(8.dp))
          .clickable { browserMode = TemplateBrowserMode.EDIT },
        color = if (isEdit) PurpleAccent else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Speed,
            contentDescription = "Edit & Speed Mode",
            tint = if (isEdit) Color.White else TextSecondary,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Edit & Speed Mode",
            fontSize = 11.sp,
            fontWeight = if (isEdit) FontWeight.Bold else FontWeight.Medium,
            color = if (isEdit) Color.White else TextSecondary
          )
        }
      }
    }

    // 3. EDIT MODE CONTROLS: Custom Text Field & Animation Speed Slider
    AnimatedVisibility(
      visible = browserMode == TemplateBrowserMode.EDIT,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF1E1B4B).copy(alpha = 0.35f))
          .border(1.dp, PurpleAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Custom Text Input Field
        OutlinedTextField(
          value = previewTextQuery,
          onValueChange = { previewTextQuery = it },
          placeholder = { Text("Enter custom text to test live across templates...", color = TextSecondary, fontSize = 11.sp) },
          leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp)) },
          trailingIcon = {
            if (previewTextQuery.isNotEmpty()) {
              IconButton(onClick = { previewTextQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
              }
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PurpleAccent,
            unfocusedBorderColor = StudioBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = StudioSurfaceVariant,
            unfocusedContainerColor = StudioSurfaceVariant
          )
        )

        // ANIMATION SPEED SLIDER (Slow Motion to High-Speed Bounce)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.Speed, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
              Text("Animation Speed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Surface(
              color = when {
                globalSpeedMultiplier < 0.85f -> Color(0xFF0EA5E9).copy(alpha = 0.25f)
                globalSpeedMultiplier > 1.25f -> Color(0xFFF43F5E).copy(alpha = 0.25f)
                else -> PurpleAccent.copy(alpha = 0.25f)
              },
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(
                0.5.dp,
                when {
                  globalSpeedMultiplier < 0.85f -> Color(0xFF0EA5E9)
                  globalSpeedMultiplier > 1.25f -> Color(0xFFF43F5E)
                  else -> PurpleAccent
                }
              )
            ) {
              Text(
                text = when {
                  globalSpeedMultiplier < 0.85f -> "${String.format(Locale.US, "%.2f", globalSpeedMultiplier)}x • Slow Motion 🐢"
                  globalSpeedMultiplier > 1.25f -> "${String.format(Locale.US, "%.2f", globalSpeedMultiplier)}x • High-Speed Bounce 🚀"
                  else -> "${String.format(Locale.US, "%.2f", globalSpeedMultiplier)}x • Normal Speed ⚡"
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                  globalSpeedMultiplier < 0.85f -> Color(0xFF38BDF8)
                  globalSpeedMultiplier > 1.25f -> Color(0xFFFB7185)
                  else -> Color.White
                },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Slider(
            value = globalSpeedMultiplier,
            onValueChange = { globalSpeedMultiplier = it },
            valueRange = 0.25f..3.0f,
            modifier = Modifier
              .fillMaxWidth()
              .height(28.dp),
            colors = SliderDefaults.colors(
              thumbColor = if (globalSpeedMultiplier > 1.25f) Color(0xFFF43F5E) else AmberAccent,
              activeTrackColor = if (globalSpeedMultiplier > 1.25f) Color(0xFFF43F5E) else AmberAccent,
              inactiveTrackColor = StudioBorder
            )
          )

          // Quick Speed Preset Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val speedPresets = listOf(
              0.5f to "0.5x Slow",
              1.0f to "1.0x Normal",
              1.5f to "1.5x Dynamic",
              2.5f to "2.5x Bounce"
            )
            speedPresets.forEach { (speedVal, label) ->
              val isSelected = kotlin.math.abs(globalSpeedMultiplier - speedVal) < 0.08f
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .clickable { globalSpeedMultiplier = speedVal },
                color = if (isSelected) PurpleAccent else StudioSurfaceVariant,
                border = BorderStroke(0.5.dp, if (isSelected) AmberAccent else StudioBorder),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = label,
                  fontSize = 8.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) Color.White else TextSecondary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }
      }
    }

    // 4. PREVIEW MODE SIMPLE INPUT BAR
    AnimatedVisibility(
      visible = browserMode == TemplateBrowserMode.PREVIEW,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      OutlinedTextField(
        value = previewTextQuery,
        onValueChange = { previewTextQuery = it },
        placeholder = { Text("Preview Text (e.g. MY NEW VIDEO)...", color = TextSecondary, fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
          if (previewTextQuery.isNotEmpty()) {
            IconButton(onClick = { previewTextQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear preview text", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
          }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = AmberAccent,
          unfocusedBorderColor = StudioBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          focusedContainerColor = StudioSurfaceVariant,
          unfocusedContainerColor = StudioSurfaceVariant
        )
      )
    }

    // 5. Category Pills (Horizontal scrolling filter)
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      item {
        val isFav = selectedCategory == "Favorites"
        FilterChip(
          selected = isFav,
          onClick = { selectedCategory = "Favorites" },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.Star, contentDescription = null, tint = if (isFav) Color.Black else AmberAccent, modifier = Modifier.size(14.dp))
              Text("Favorites (${favoriteTemplateIds.size})", fontSize = 11.sp, fontWeight = if (isFav) FontWeight.Bold else FontWeight.Normal)
            }
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AmberAccent,
            selectedLabelColor = Color.Black,
            containerColor = StudioSurfaceVariant,
            labelColor = TextPrimary
          )
        )
      }

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
            selectedContainerColor = PurpleAccent.copy(alpha = 0.35f),
            selectedLabelColor = Color.White,
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

    // 6. VERTICAL LIVE TEMPLATE FEED
    if (displayedTemplates.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("No templates found in this category", color = TextSecondary, fontSize = 12.sp)
      }
    } else {
      val effectiveFeedSpeed = if (browserMode == TemplateBrowserMode.EDIT) globalSpeedMultiplier else 1.0f

      LazyColumn(
        state = verticalListState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 280.dp, max = 340.dp)
      ) {
        items(displayedTemplates, key = { it.id }) { tpl ->
          val isFavorite = tpl.id in favoriteTemplateIds
          val isVisible = visibleItemKeys.contains(tpl.id)

          VerticalShowcaseTemplateCard(
            tpl = tpl,
            customPreviewText = previewTextQuery.ifBlank { null },
            speedMultiplier = effectiveFeedSpeed,
            isFavorite = isFavorite,
            isVisible = isVisible,
            onToggleFavorite = {
              if (isFavorite) favoriteTemplateIds.remove(tpl.id)
              else favoriteTemplateIds.add(tpl.id)
            },
            onCardClick = {
              inspectingTemplate = tpl
              inspectingCustomText = previewTextQuery.ifBlank { tpl.sampleText }
              inspectingSpeedMultiplier = effectiveFeedSpeed
            },
            onUseTemplate = {
              applyTemplateAction(tpl, previewTextQuery.ifBlank { null }, effectiveFeedSpeed)
            }
          )
        }
      }
    }
  }

  // 7. SELECTED TEMPLATE EDIT / INSPECTION MODAL WITH ANIMATION SPEED SLIDER
  inspectingTemplate?.let { tpl ->
    Dialog(onDismissRequest = { inspectingTemplate = null }) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(6.dp)
          .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.6f))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Modal Header
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
              Text("LIVE ANIMATION", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
              Text("•", color = TextSecondary)
              Text(tpl.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            IconButton(
              onClick = { inspectingTemplate = null },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
          }

          // Large Animated Preview Canvas (Animates live at inspectingSpeedMultiplier)
          LargeLivePreviewCanvas(
            tpl = tpl,
            previewText = inspectingCustomText.ifBlank { tpl.sampleText },
            speedMultiplier = inspectingSpeedMultiplier,
            modifier = Modifier
              .fillMaxWidth()
              .height(135.dp)
          )

          // [Edit Text] Section
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Edit Text", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = AmberAccent))
              Text("Updates live above", fontSize = 10.sp, color = TextSecondary)
            }
            OutlinedTextField(
              value = inspectingCustomText,
              onValueChange = { inspectingCustomText = it },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AmberAccent,
                unfocusedBorderColor = StudioBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = StudioSurfaceVariant,
                unfocusedContainerColor = StudioSurfaceVariant
              )
            )

            // Quick Preset Words
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              val quickWords = listOf("MY NEW VIDEO", "NEW VLOG", "SUMMER ☀️", "50% OFF", "SUBSCRIBE")
              quickWords.forEach { word ->
                Surface(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { inspectingCustomText = word },
                  color = if (inspectingCustomText == word) AmberAccent else StudioSurfaceVariant,
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = word,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (inspectingCustomText == word) Color.Black else TextSecondary,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }

          // ANIMATION SPEED SLIDER (Requirement: slow motion to high-speed bounce)
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(15.dp))
                Text("Animation Speed", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
              }

              Surface(
                color = when {
                  inspectingSpeedMultiplier < 0.85f -> Color(0xFF0EA5E9).copy(alpha = 0.25f)
                  inspectingSpeedMultiplier > 1.25f -> Color(0xFFF43F5E).copy(alpha = 0.25f)
                  else -> AmberAccent.copy(alpha = 0.25f)
                },
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(
                  0.5.dp,
                  when {
                    inspectingSpeedMultiplier < 0.85f -> Color(0xFF0EA5E9)
                    inspectingSpeedMultiplier > 1.25f -> Color(0xFFF43F5E)
                    else -> AmberAccent
                  }
                )
              ) {
                Text(
                  text = when {
                    inspectingSpeedMultiplier < 0.85f -> "${String.format(Locale.US, "%.2f", inspectingSpeedMultiplier)}x • Slow Motion 🐢"
                    inspectingSpeedMultiplier > 1.25f -> "${String.format(Locale.US, "%.2f", inspectingSpeedMultiplier)}x • High-Speed Bounce 🚀"
                    else -> "${String.format(Locale.US, "%.2f", inspectingSpeedMultiplier)}x • Normal Speed ⚡"
                  },
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = when {
                    inspectingSpeedMultiplier < 0.85f -> Color(0xFF38BDF8)
                    inspectingSpeedMultiplier > 1.25f -> Color(0xFFFB7185)
                    else -> AmberAccent
                  },
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Slider(
              value = inspectingSpeedMultiplier,
              onValueChange = { inspectingSpeedMultiplier = it },
              valueRange = 0.25f..3.0f,
              modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
              colors = SliderDefaults.colors(
                thumbColor = if (inspectingSpeedMultiplier > 1.25f) Color(0xFFF43F5E) else AmberAccent,
                activeTrackColor = if (inspectingSpeedMultiplier > 1.25f) Color(0xFFF43F5E) else AmberAccent,
                inactiveTrackColor = StudioBorder
              )
            )

            // Speed Presets for quick selection
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              val modalSpeedPresets = listOf(
                0.5f to "0.5x Slow",
                1.0f to "1.0x Normal",
                1.5f to "1.5x Dynamic",
                2.5f to "2.5x Bounce"
              )
              modalSpeedPresets.forEach { (speedVal, label) ->
                val isSelected = kotlin.math.abs(inspectingSpeedMultiplier - speedVal) < 0.08f
                Surface(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { inspectingSpeedMultiplier = speedVal },
                  color = if (isSelected) AmberAccent else StudioSurfaceVariant,
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = label,
                    fontSize = 8.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.Black else TextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }

          // Style Meta Pills
          val adjustedDurationMs = (tpl.animDurationMs / inspectingSpeedMultiplier).toLong().coerceIn(100L, 6000L)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              color = StudioSurfaceVariant,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ANIMATION", fontSize = 7.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(tpl.animationType, fontSize = 9.sp, color = AmberAccent, fontWeight = FontWeight.Bold, maxLines = 1)
              }
            }
            Surface(
              color = StudioSurfaceVariant,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DURATION", fontSize = 7.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text("${String.format(Locale.US, "%.2f", adjustedDurationMs / 1000f)}s", fontSize = 9.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
              }
            }
            Surface(
              color = StudioSurfaceVariant,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FONT", fontSize = 7.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(tpl.fontFamily, fontSize = 9.sp, color = PurpleAccent, fontWeight = FontWeight.Bold, maxLines = 1)
              }
            }
          }

          // [Use Template] Button
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
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Text("Use Template", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

/**
 * Vertical Showcase Card (Requirement 1, 2, 3, 4, 5, 9)
 * Full-width card with large animated vector canvas rendered via TextLayerRenderer.
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

  // Animation driver: loops smoothly (0 to 2400ms) only when visible in viewport
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

  // Primary layer clip
  val primaryConfig = remember(tpl, effectivePreviewText) {
    tpl.toPrimaryLayerConfig().copy(text = effectivePreviewText)
  }
  val primaryClip = remember(primaryConfig) {
    primaryConfig.toTextClip(timelineStartMs = 0L, durationMs = 3000L)
  }

  // Secondary layer clips (if multi-layer template)
  val secondaryClips = remember(tpl.secondaryLayers) {
    tpl.secondaryLayers.map { sec ->
      sec.toTextClip(timelineStartMs = 0L, durationMs = 3000L)
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onCardClick() },
    colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
    border = BorderStroke(1.dp, if (tpl.isPremium) AmberAccent.copy(alpha = 0.6f) else StudioBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Header: Badge + Name + Category + VIP diamond + Animation Pill + Favorite
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(tpl.badgeEmoji, fontSize = 15.sp)
          Text(
            text = tpl.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
          )
          Surface(
            color = PurpleAccent.copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(0.5.dp, PurpleAccent.copy(alpha = 0.5f))
          ) {
            Text(
              text = tpl.animationType,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = PurpleAccent,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
          if (speedMultiplier != 1.0f) {
            Surface(
              color = if (speedMultiplier > 1.25f) Color(0xFFF43F5E).copy(alpha = 0.2f) else Color(0xFF0EA5E9).copy(alpha = 0.2f),
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = "${String.format(Locale.US, "%.1f", speedMultiplier)}x",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (speedMultiplier > 1.25f) Color(0xFFFB7185) else Color(0xFF38BDF8),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
          if (tpl.isPremium) {
            Surface(
              color = AmberAccent.copy(alpha = 0.2f),
              shape = RoundedCornerShape(4.dp),
              border = BorderStroke(0.5.dp, AmberAccent)
            ) {
              Text(
                text = "💎 VIP",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = AmberAccent,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }

        IconButton(
          onClick = onToggleFavorite,
          modifier = Modifier.size(24.dp)
        ) {
          Icon(
            if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = "Favorite",
            tint = if (isFavorite) AmberAccent else TextSecondary,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      // Large Live Animated Vector Canvas Viewport
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(108.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF080C14))
          .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val nativeCanvas = drawContext.canvas.nativeCanvas
          val w = size.width.toInt()
          val h = size.height.toInt()
          if (w > 0 && h > 0) {
            val hasMultipleLayers = secondaryClips.isNotEmpty()
            val primaryPosY = if (hasMultipleLayers) -0.22f else 0.0f

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

      // Card Bottom Bar: Tap to Inspect Hint + Use Template Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(Icons.Default.TouchApp, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
          Text(
            text = "Tap card to edit text & speed",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
          )
        }

        Button(
          onClick = onUseTemplate,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (tpl.isPremium) AmberAccent else PurpleAccent,
            contentColor = if (tpl.isPremium) Color.Black else Color.White
          ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
          modifier = Modifier.height(28.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
            Text("Use Template", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

/**
 * Extra Large Live Preview Canvas for modal inspection
 */
@Composable
private fun LargeLivePreviewCanvas(
  tpl: TextTemplateItem,
  previewText: String,
  speedMultiplier: Float = 1.0f,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  val infiniteTransition = rememberInfiniteTransition(label = "modal_anim_${tpl.id}_${previewText.hashCode()}")
  val animLoopMs by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 2400f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "modal_loop"
  )

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
        val primaryPosY = if (hasMultipleLayers) -0.22f else 0.0f

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
