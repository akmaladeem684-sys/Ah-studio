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

    // Grid of Template Cards
    if (displayedTemplates.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("No templates found in this category", color = TextSecondary, fontSize = 12.sp)
      }
    } else {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(displayedTemplates) { tpl ->
          val isFavorite = tpl.id in favoriteTemplateIds

          Card(
            modifier = Modifier
              .width(175.dp)
              .height(120.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                // Apply directly to active text without duplicate layer
                if (selectedTextClip != null) {
                  viewModel.timelineEngine.updateTextClip(
                    selectedTextClip.copy(
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
                } else {
                  // Create single new text layer with template
                  val currentPos = viewModel.timelineEngine.currentPositionMs.value
                  val newClip = TextClip(
                    id = UUID.randomUUID().toString(),
                    text = tpl.sampleText,
                    timelineStartMs = currentPos,
                    durationMs = 3000L,
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
                  viewModel.timelineEngine.addTextClipObject(newClip)
                  viewModel.timelineEngine.selectElement(SelectedTrackElement.Text(newClip.id))
                }
              },
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            border = BorderStroke(1.dp, StudioBorder)
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(tpl.badgeEmoji, fontSize = 14.sp)
                  Text(tpl.category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                }
                IconButton(
                  onClick = {
                    if (isFavorite) favoriteTemplateIds.remove(tpl.id)
                    else favoriteTemplateIds.add(tpl.id)
                  },
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

              Text(
                text = tpl.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )

              // Visual preview banner
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
}
