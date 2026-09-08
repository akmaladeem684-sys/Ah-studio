package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.example.data.presets.MediaPlaceholder
import com.example.data.presets.PlaceholderType
import com.example.data.presets.TemplatesCatalog
import com.example.data.presets.TextPlaceholder
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
  var activeTemplateForSetup by remember { mutableStateOf<VideoTemplate?>(null) }

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
            onCustomize = { activeTemplateForSetup = template },
            onQuickCreate = { viewModel.applyTemplate(template) }
          )
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
      }
    }
  }

  // Media Selection & Placeholder Customizer Dialog
  activeTemplateForSetup?.let { template ->
    TemplateSetupDialog(
      template = template,
      onDismiss = { activeTemplateForSetup = null },
      onApply = { mediaMap, textMap ->
        viewModel.applyTemplate(
          template = template,
          mediaReplacements = mediaMap,
          textReplacements = textMap
        )
        activeTemplateForSetup = null
      }
    )
  }
}

@Composable
private fun TemplateCard(
  template: VideoTemplate,
  onCustomize: () -> Unit,
  onQuickCreate: () -> Unit
) {
  val videoCount = template.mediaPlaceholders.count { it.placeholderType == PlaceholderType.VIDEO }
  val photoCount = template.mediaPlaceholders.count { it.placeholderType == PlaceholderType.IMAGE }

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
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = template.title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            Text(
              text = template.category,
              style = MaterialTheme.typography.labelSmall.copy(color = CyanAccent, fontWeight = FontWeight.SemiBold)
            )
          }

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

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = template.description,
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Placeholder feature tags
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (videoCount > 0) {
            SlotChip(label = "🎥 $videoCount Videos")
          }
          if (photoCount > 0) {
            SlotChip(label = "🖼️ $photoCount Photos")
          }
          if (template.textPlaceholders.isNotEmpty()) {
            SlotChip(label = "✏️ ${template.textPlaceholders.size} Texts")
          }
          SlotChip(label = "🎵 Audio")
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Primary action: Select Media & Customize
          Button(
            onClick = onCustomize,
            modifier = Modifier
              .weight(1f)
              .height(42.dp)
              .testTag("use_template_${template.id}"),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
            shape = RoundedCornerShape(21.dp)
          ) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Select Media", fontWeight = FontWeight.Bold)
          }

          // Secondary quick action: Direct creation with demo placeholders
          OutlinedButton(
            onClick = onQuickCreate,
            modifier = Modifier
              .height(42.dp)
              .testTag("quick_create_${template.id}"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(StudioBorder, StudioBorder))),
            shape = RoundedCornerShape(21.dp)
          ) {
            Text("Quick Create", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
  }
}

@Composable
private fun SlotChip(label: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(StudioDarkBg)
      .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
      .padding(horizontal = 6.dp, vertical = 3.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
    )
  }
}

@Composable
fun TemplateSetupDialog(
  template: VideoTemplate,
  onDismiss: () -> Unit,
  onApply: (mediaMap: Map<String, String>, textMap: Map<String, String>) -> Unit
) {
  val mediaReplacements = remember { mutableStateMapOf<String, String>() }
  val textReplacements = remember {
    mutableStateMapOf<String, String>().apply {
      template.textPlaceholders.forEach { put(it.slotId, it.defaultText) }
    }
  }

  var activePickingSlotId by remember { mutableStateOf<String?>(null) }
  var activePickingType by remember { mutableStateOf<PlaceholderType?>(null) }

  val mediaPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    val slotId = activePickingSlotId
    if (uri != null && slotId != null) {
      mediaReplacements[slotId] = uri.toString()
    }
    activePickingSlotId = null
    activePickingType = null
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.9f)
        .clip(RoundedCornerShape(20.dp)),
      color = StudioSurface,
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(StudioBorder, StudioBorder)))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = template.title,
              style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
            )
            Text(
              text = "${template.category} • ${template.aspectRatio.label} • ${formatDurationShort(template.durationMs)}",
              style = MaterialTheme.typography.labelSmall.copy(color = CyanAccent)
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = StudioBorder)
        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable content
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Section 1: Media Placeholders
          Text(
            text = "1. SELECT MEDIA PLACEHOLDERS",
            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
          )

          template.mediaPlaceholders.forEachIndexed { index, placeholder ->
            val isSelected = mediaReplacements.containsKey(placeholder.slotId)
            val selectedUri = mediaReplacements[placeholder.slotId]

            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  activePickingSlotId = placeholder.slotId
                  activePickingType = placeholder.placeholderType
                  val requestType = if (placeholder.placeholderType == PlaceholderType.VIDEO) {
                    ActivityResultContracts.PickVisualMedia.VideoOnly
                  } else {
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                  }
                  mediaPickerLauncher.launch(PickVisualMediaRequest(requestType))
                },
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) CyanAccent.copy(alpha = 0.12f) else StudioDarkBg
              ),
              border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                  if (isSelected) listOf(CyanAccent, CyanAccent) else listOf(StudioBorder, StudioBorder)
                )
              ),
              shape = RoundedCornerShape(12.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) CyanAccent else StudioSurface),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = if (placeholder.placeholderType == PlaceholderType.VIDEO) Icons.Default.Videocam else Icons.Default.Image,
                    contentDescription = null,
                    tint = if (isSelected) Color.Black else TextPrimary,
                    modifier = Modifier.size(22.dp)
                  )
                }

                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                      text = "Slot ${index + 1}: ${placeholder.label}",
                      style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                      text = "(${formatDurationShort(placeholder.requiredDurationMs)})",
                      style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                  }

                  Text(
                    text = if (isSelected) "Media selected: ${selectedUri?.substringAfterLast("/")}" else "Tap to choose ${placeholder.placeholderType.name.lowercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(color = if (isSelected) CyanAccent else TextSecondary)
                  )
                }

                if (isSelected) {
                  IconButton(
                    onClick = { mediaReplacements.remove(placeholder.slotId) },
                    modifier = Modifier.size(32.dp)
                  ) {
                    Icon(Icons.Default.Clear, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(18.dp))
                  }
                } else {
                  Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add", tint = CyanAccent, modifier = Modifier.size(22.dp))
                }
              }
            }
          }

          // Section 2: Text Placeholders
          if (template.textPlaceholders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "2. CUSTOMIZE TEXT",
              style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
            )

            template.textPlaceholders.forEach { txtPlaceholder ->
              OutlinedTextField(
                value = textReplacements[txtPlaceholder.slotId] ?: "",
                onValueChange = { textReplacements[txtPlaceholder.slotId] = it },
                label = { Text(txtPlaceholder.label) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = CyanAccent,
                  unfocusedBorderColor = StudioBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary,
                  focusedContainerColor = StudioDarkBg,
                  unfocusedContainerColor = StudioDarkBg
                ),
                shape = RoundedCornerShape(10.dp)
              )
            }
          }

          // Section 3: Audio Soundtrack
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioDarkBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(StudioBorder, StudioBorder))),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.MusicNote, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(24.dp))
              Column {
                Text(
                  text = "Included Audio: ${template.audioTitle}",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                )
                Text(
                  text = "Fully customizable keyframes, speed, volume, and transitions in editor.",
                  style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .height(46.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            shape = RoundedCornerShape(23.dp)
          ) {
            Text("Cancel")
          }

          Button(
            onClick = {
              onApply(mediaReplacements.toMap(), textReplacements.toMap())
            },
            modifier = Modifier
              .weight(1.5f)
              .height(46.dp)
              .testTag("apply_template_project_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
            shape = RoundedCornerShape(23.dp)
          ) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create Project", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
