package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Style
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
import com.example.data.firebase.FirebaseTemplateManager
import com.example.data.presets.PlaceholderType
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
  val firebaseTemplates by FirebaseTemplateManager.templates.collectAsState()

  val filteredTemplates = remember(firebaseTemplates, selectedCategory) {
    if (selectedCategory == "All") firebaseTemplates
    else firebaseTemplates.filter { it.category.equals(selectedCategory, ignoreCase = true) }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(StudioDarkBg),
    containerColor = StudioDarkBg,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Templates Community", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Real Firebase dynamic video templates", color = TextSecondary, fontSize = 11.sp)
          }
        },
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
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Category Selector
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(TemplatesCatalog.categories) { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            label = { Text(cat) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = CyanAccent,
              selectedLabelColor = Color.Black,
              containerColor = StudioSurface,
              labelColor = TextPrimary
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = selectedCategory == cat,
              selectedBorderColor = CyanAccent,
              borderColor = StudioBorder
            )
          )
        }
      }

      // Templates List or Empty State
      if (filteredTemplates.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
          ) {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(StudioSurfaceVariant),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Outlined.Style, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = if (selectedCategory == "All") "No Templates in Firebase Yet" else "No Templates in $selectedCategory",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Templates are created exclusively through the Template Creator flow. Design in Screen Editor and export to publish here automatically.",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          items(filteredTemplates, key = { it.id }) { template ->
            TemplateCard(
              template = template,
              onUseTemplate = {
                viewModel.applyTemplate(template)
              },
              onCardClick = {
                FirebaseTemplateManager.recordTemplateView(template.id, template.creatorId)
              }
            )
          }
          item { Spacer(modifier = Modifier.height(32.dp)) }
        }
      }
    }
  }
}

@Composable
private fun TemplateCard(
  template: VideoTemplate,
  onUseTemplate: () -> Unit,
  onCardClick: () -> Unit
) {
  val videoCount = template.mediaPlaceholders.count { it.placeholderType == PlaceholderType.VIDEO }
  val photoCount = template.mediaPlaceholders.count { it.placeholderType == PlaceholderType.IMAGE }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .clickable { onCardClick() }
      .testTag("template_${template.id}"),
    colors = CardDefaults.cardColors(containerColor = StudioSurface),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.linearGradient(listOf(StudioBorder, StudioBorder.copy(alpha = 0.5f)))
    )
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
          .padding(14.dp)
      ) {
        // Emoji Badge / Preview Icon
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f)),
          contentAlignment = Alignment.Center
        ) {
          Text(template.iconEmoji, fontSize = 22.sp)
        }

        // Duration & Aspect Ratio Tags
        Row(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.75f))
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
      Column(modifier = Modifier.padding(14.dp)) {
        // Creator Row (Avatar, Name, Handle, Views & Cuts)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(CyanAccent, PurpleAccent))),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = template.creatorName.take(1).uppercase().ifBlank { "C" },
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = template.creatorName.ifBlank { "Verified Creator" },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
              )
              Text(
                text = template.creatorHandle.ifBlank { "@creator" },
                style = MaterialTheme.typography.labelSmall.copy(color = CyanAccent, fontSize = 11.sp)
              )
            }
          }

          // Category Badge
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = StudioSurfaceVariant
          ) {
            Text(
              text = template.category,
              style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title & Description
        Text(
          text = template.title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        if (template.description.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = template.description,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            maxLines = 2
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Statistics Row: Views and Uses / Cuts
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StudioSurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Text(
              text = "👁️ ${template.viewsCount} views",
              style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
            )
            Text(
              text = "✂️ ${template.usesCount} cuts",
              style = MaterialTheme.typography.labelSmall.copy(color = CyanAccent, fontWeight = FontWeight.Bold)
            )
          }

          if (videoCount > 0 || photoCount > 0) {
            Text(
              text = if (videoCount > 0) "$videoCount clips" else "$photoCount photos",
              style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Use Template Action Button
        Button(
          onClick = onUseTemplate,
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("use_template_${template.id}"),
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Use Template", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}
