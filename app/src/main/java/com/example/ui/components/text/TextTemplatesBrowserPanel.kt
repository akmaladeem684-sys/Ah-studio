package com.example.ui.components.text

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TextClip
import com.example.engine.SelectedTrackElement
import com.example.ui.StudioViewModel
import com.example.ui.theme.*
import java.util.UUID

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

  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("Trending") }
  val favoriteTemplateIds = remember { mutableStateListOf<String>() }

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
          it.sampleText.contains(searchQuery, ignoreCase = true)
      }
    }
    filtered
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Header & Search
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
          Text("Text Templates Studio", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
          Text("${allTemplates.size} Professional Styles • 41 Categories", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
        }
      }

      IconButton(
        onClick = onDismiss,
        modifier = Modifier.size(32.dp)
      ) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    // Search Input Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search templates (e.g. Neon, KATSEYE, Free Fire)...", color = TextSecondary, fontSize = 12.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
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

    // 41 Category Pills Horizontal Scroll
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

    // Grid/Row of Template Cards
    if (displayedTemplates.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(140.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("No templates found in this category", color = TextSecondary, fontSize = 12.sp)
      }
    } else {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(displayedTemplates, key = { it.id }) { tpl ->
          val isFavorite = tpl.id in favoriteTemplateIds

          val applyTemplateAction = {
            if (selectedTextClip != null) {
              val textToKeep = if (selectedTextClip.text.isBlank() || selectedTextClip.text == "Tap to edit" || selectedTextClip.text == "Your Text Here") {
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
                  hasBackground = tpl.hasBackground,
                  backgroundColor = tpl.backgroundColor,
                  cornerRadius = tpl.cornerRadius,
                  bgPadding = tpl.bgPadding,
                  opacity = tpl.opacity,
                  animationType = tpl.animationType,
                  animDurationMs = tpl.animDurationMs
                )
              )
            } else {
              val currentPos = viewModel.timelineEngine.currentPositionMs.value
              val newClip = TextClip(
                id = UUID.randomUUID().toString(),
                text = tpl.sampleText,
                timelineStartMs = currentPos,
                durationMs = 3000L,
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
                hasBackground = tpl.hasBackground,
                backgroundColor = tpl.backgroundColor,
                cornerRadius = tpl.cornerRadius,
                bgPadding = tpl.bgPadding,
                opacity = tpl.opacity,
                animationType = tpl.animationType,
                animDurationMs = tpl.animDurationMs
              )
              viewModel.timelineEngine.addTextClipObject(newClip)
              viewModel.timelineEngine.selectElement(SelectedTrackElement.Text(newClip.id))
            }
          }

          TrendingTemplateCardItem(
            tpl = tpl,
            isFavorite = isFavorite,
            onToggleFavorite = {
              if (isFavorite) favoriteTemplateIds.remove(tpl.id)
              else favoriteTemplateIds.add(tpl.id)
            },
            onUseTemplate = applyTemplateAction
          )
        }
      }
    }
  }
}

@Composable
private fun TrendingTemplateCardItem(
  tpl: TextTemplateItem,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  onUseTemplate: () -> Unit
) {
  // Live animated preview loop for template card
  val infiniteTransition = rememberInfiniteTransition(label = "template_preview_anim")
  val animProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "progress"
  )

  // Compute live visual transform according to animation type
  val scale = remember(tpl.animationType, animProgress) {
    when (tpl.animationType.lowercase()) {
      "pop", "pop in" -> if (animProgress < 0.4f) (animProgress / 0.4f) * 1.15f else if (animProgress < 0.6f) 1.15f - (animProgress - 0.4f) / 0.2f * 0.15f else 1f
      "bounce" -> if (animProgress < 0.5f) (1f + kotlin.math.sin(animProgress * kotlin.math.PI.toFloat() * 4f) * 0.2f * (1f - animProgress * 2f)) else 1f
      "zoom", "zoom in" -> 0.7f + 0.3f * (animProgress.coerceAtMost(0.4f) / 0.4f)
      "scale", "scale in" -> 1.3f - 0.3f * (animProgress.coerceAtMost(0.4f) / 0.4f)
      "glow", "glow pulse" -> 1f + kotlin.math.sin(animProgress * kotlin.math.PI.toFloat() * 2f) * 0.05f
      else -> 1f
    }
  }

  val alpha = remember(tpl.animationType, animProgress) {
    when (tpl.animationType.lowercase()) {
      "fade", "cinematic reveal", "blur" -> (animProgress * 2.5f).coerceIn(0.4f, 1f)
      "neon", "neon flicker" -> if (animProgress < 0.3f && ((animProgress * 20).toInt() % 2 == 0)) 0.4f else 1f
      "glitch" -> if (animProgress < 0.4f && ((animProgress * 20).toInt() % 3 == 0)) 0.7f else 1f
      else -> 1f
    }
  }

  val displayText = remember(tpl.sampleText, tpl.animationType, animProgress) {
    if (tpl.animationType.equals("Typewriter", ignoreCase = true)) {
      val len = tpl.sampleText.length
      val count = (len * (animProgress * 1.6f).coerceIn(0f, 1f)).toInt().coerceIn(0, len)
      tpl.sampleText.substring(0, count) + if (count < len) "▌" else ""
    } else {
      tpl.sampleText
    }
  }

  Card(
    modifier = Modifier
      .width(180.dp)
      .height(135.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable { onUseTemplate() },
    colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
    border = BorderStroke(1.dp, if (tpl.isPremium) AmberAccent.copy(alpha = 0.5f) else StudioBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Header: Badge + Category + Diamond Indicator + Favorite
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(tpl.badgeEmoji, fontSize = 13.sp)
          Text(
            text = tpl.category,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (tpl.isPremium) AmberAccent else PurpleAccent
          )
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
          modifier = Modifier.size(20.dp)
        ) {
          Icon(
            if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = "Favorite",
            tint = if (isFavorite) AmberAccent else TextSecondary,
            modifier = Modifier.size(15.dp)
          )
        }
      }

      // Live Animated Preview Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(42.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(if (tpl.hasBackground) Color(tpl.backgroundColor.toInt()) else Color(0x33000000))
          .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = displayText,
          color = Color(tpl.textColor.toInt()).copy(alpha = alpha),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .scale(scale)
        )
      }

      // Footer: Template Name & "Use" button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
          Text(
            text = tpl.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 10.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = tpl.animationType,
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 8.sp),
            maxLines = 1
          )
        }

        Button(
          onClick = onUseTemplate,
          shape = RoundedCornerShape(6.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (tpl.isPremium) AmberAccent else PurpleAccent,
            contentColor = Color.Black
          ),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
          modifier = Modifier.height(24.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
            Text("Use", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
