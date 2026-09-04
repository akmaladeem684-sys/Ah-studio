package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.domain.model.ExportQuality
import com.example.domain.model.FrameRate
import com.example.domain.model.Resolution
import com.example.engine.export.ExportConfig
import com.example.engine.export.ExportState
import com.example.ui.AppScreen
import com.example.ui.StudioViewModel
import com.example.ui.components.PrimaryPillButton
import com.example.ui.components.formatDurationShort
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val exportState by viewModel.videoExporter.exportState.collectAsState()
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val defaultRes by viewModel.activeResolution.collectAsState()
  val defaultFps by viewModel.activeFps.collectAsState()

  var selectedResolution by remember { mutableStateOf(defaultRes) }
  var selectedFps by remember { mutableStateOf(defaultFps) }
  var selectedQuality by remember { mutableStateOf(ExportQuality.HIGH) }

  val config = remember(selectedResolution, selectedFps, selectedQuality) {
    ExportConfig(
      resolution = selectedResolution,
      frameRate = selectedFps,
      quality = selectedQuality
    )
  }

  val estimatedBytes = remember(config, timeline.totalDurationMs) {
    viewModel.videoExporter.calculateEstimatedSizeBytes(timeline.totalDurationMs, config)
  }
  val estimatedMb = remember(estimatedBytes) {
    String.format("%.1f MB", estimatedBytes / (1024f * 1024f))
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(StudioDarkBg),
    containerColor = StudioDarkBg,
    topBar = {
      TopAppBar(
        title = { Text("Export Project", color = TextPrimary, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateTo(AppScreen.EDITOR) }) {
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
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      when (val state = exportState) {
        is ExportState.Idle -> {
          // Export Configuration Options
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(StudioBorder, StudioBorder.copy(alpha = 0.4f))))
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
              // Resolution
              Column {
                Text("Resolution", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  items(Resolution.values()) { res ->
                    FilterChip(
                      selected = selectedResolution == res,
                      onClick = { selectedResolution = res },
                      label = { Text(res.label) },
                      colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = Color.Black)
                    )
                  }
                }
              }

              // Frame Rate
              Column {
                Text("Frame Rate", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  items(FrameRate.values()) { fps ->
                    FilterChip(
                      selected = selectedFps == fps,
                      onClick = { selectedFps = fps },
                      label = { Text("${fps.fps} FPS") },
                      colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleAccent, selectedLabelColor = Color.White)
                    )
                  }
                }
              }

              // Quality
              Column {
                Text("Export Quality / Bitrate", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  items(ExportQuality.values()) { q ->
                    FilterChip(
                      selected = selectedQuality == q,
                      onClick = { selectedQuality = q },
                      label = { Text(q.label) },
                      colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StudioSurfaceVariant, selectedLabelColor = CyanAccent)
                    )
                  }
                }
              }

              Divider(color = StudioBorder)

              // Summary
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("Estimated File Size", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                  Text(estimatedMb, style = MaterialTheme.typography.titleLarge.copy(color = CyanAccent, fontWeight = FontWeight.Bold))
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("Total Duration", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                  Text(formatDurationShort(timeline.totalDurationMs), style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                }
              }
            }
          }

          Spacer(modifier = Modifier.weight(1f))

          PrimaryPillButton(
            text = "Start Export",
            icon = Icons.Default.FileUpload,
            onClick = { viewModel.startExport(config) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("start_export_button")
          )
        }

        is ExportState.Rendering -> {
          // Live Rendering Progress Screen
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Box(
                modifier = Modifier
                  .size(120.dp)
                  .clip(CircleShape)
                  .background(StudioSurface),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator(
                  progress = { state.progressPercent },
                  modifier = Modifier.fillMaxSize(),
                  color = CyanAccent,
                  strokeWidth = 8.dp,
                  trackColor = StudioBorder
                )
                Text(
                  text = "${(state.progressPercent * 100).toInt()}%",
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
              }

              Text(
                text = "Rendering Video...",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
              )
              Text(
                text = "Frame ${state.currentFrame} of ${state.totalFrames}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
              )

              Button(
                onClick = { viewModel.videoExporter.cancelExport() },
                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant, contentColor = RedAccent)
              ) {
                Text("Cancel Export")
              }
            }
          }
        }

        is ExportState.Success -> {
          // Export Succeeded Screen
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Box(
                modifier = Modifier
                  .size(80.dp)
                  .clip(CircleShape)
                  .background(GreenAccent),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Check, contentDescription = "Success", tint = Color.Black, modifier = Modifier.size(44.dp))
              }

              Text(
                text = "Export Complete!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
              )
              Text(
                text = "Saved to: ${state.file.name}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
              )
              Text(
                text = "Size: ${String.format("%.1f MB", state.fileSizeBytes / (1024f * 1024f))}",
                style = MaterialTheme.typography.labelMedium.copy(color = CyanAccent, fontWeight = FontWeight.Bold)
              )

              Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                  onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                      type = "video/mp4"
                      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", state.file)
                      putExtra(Intent.EXTRA_STREAM, uri)
                      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Exported Video"))
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                  modifier = Modifier.testTag("export_share_button")
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Share Video", fontWeight = FontWeight.Bold)
                }

                Button(
                  onClick = { viewModel.navigateTo(AppScreen.EXPORTED_LIBRARY) },
                  colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = Color.White),
                  modifier = Modifier.testTag("export_library_button")
                ) {
                  Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("View in Library", fontWeight = FontWeight.Bold)
                }
              }

              TextButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                Text("Return to Home Screen", color = TextSecondary)
              }
            }
          }
        }

        is ExportState.Error -> {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Icon(Icons.Default.Error, contentDescription = null, tint = RedAccent, modifier = Modifier.size(64.dp))
              Text("Export Failed", style = MaterialTheme.typography.titleLarge.copy(color = RedAccent, fontWeight = FontWeight.Bold))
              Text(state.message, color = TextSecondary)
              PrimaryPillButton(
                text = "Try Again",
                onClick = { viewModel.startExport(config) }
              )
            }
          }
        }
      }
    }
  }
}
