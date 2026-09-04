package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.presets.TemplatesCatalog
import com.example.data.presets.VideoTemplate
import com.example.ui.AppScreen
import com.example.ui.StudioViewModel
import com.example.ui.components.formatDurationShort
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  var selectedCategory by remember { mutableStateOf("All") }
  val categories = listOf("All", "Trending Reels", "Travel & Vlog", "Effects Heavy", "Aesthetic Quotes")

  val templates = remember(selectedCategory) {
    if (selectedCategory == "All") TemplatesCatalog.templates
    else TemplatesCatalog.templates.filter { it.category == selectedCategory }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(StudioDarkBg),
    containerColor = StudioDarkBg,
    topBar = {
      TopAppBar(
        title = { Text("Video Templates", color = TextPrimary, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioDarkBg)
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Category Selector
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories) { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            label = { Text(cat) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = CyanAccent,
              selectedLabelColor = Color.Black,
              containerColor = StudioSurface,
              labelColor = TextPrimary
            )
          )
        }
      }

      // Templates List
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        items(templates) { template ->
          TemplateCard(
            template = template,
            onUseTemplate = { viewModel.applyTemplate(template) }
          )
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
      }
    }
  }
}

@Composable
private fun TemplateCard(
  template: VideoTemplate,
  onUseTemplate: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .testTag("template_${template.id}"),
    colors = CardDefaults.cardColors(containerColor = StudioSurface),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(StudioBorder, StudioBorder.copy(alpha = 0.5f))))
  ) {
    Column {
      // Banner Preview
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp)
          .background(
            Brush.linearGradient(
              listOf(Color(template.thumbnailGradientStart), Color(template.thumbnailGradientEnd))
            )
          )
          .padding(16.dp)
      ) {
        // Emoji Badge
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f)),
          contentAlignment = Alignment.Center
        ) {
          Text(template.iconEmoji, fontSize = 26.sp)
        }

        // Duration & Aspect Ratio Tags
        Row(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${template.aspectRatio.label} • ${formatDurationShort(template.durationMs)}",
            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
          )
        }
      }

      // Content & Action
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = template.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
          )
          if (template.isPro) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(CyanAccent, PurpleAccent)))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text("PRO", style = MaterialTheme.typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp))
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = template.description,
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = onUseTemplate,
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
          shape = RoundedCornerShape(21.dp)
        ) {
          Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Use Template", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
