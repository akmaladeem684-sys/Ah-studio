package com.example.ui.components.export

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.AspectRatio
import com.example.domain.model.ExportQuality
import com.example.domain.model.FrameRate
import com.example.domain.model.Resolution
import com.example.engine.export.ExportConfig
import com.example.ui.components.formatDuration
import com.example.ui.theme.*
import kotlin.math.roundToInt

/**
 * Dialog for comprehensive export configuration.
 * Allows users to choose resolution, bitrate (presets and fine custom slider), and frame rate
 * before rendering the final project using Media3 Transformer.
 */
@Composable
fun ExportConfigurationDialog(
  projectName: String,
  totalDurationMs: Long,
  aspectRatio: AspectRatio,
  initialResolution: Resolution = Resolution.RES_1080P,
  initialFps: FrameRate = FrameRate.FPS_30,
  initialQuality: ExportQuality = ExportQuality.HIGH,
  initialBitrateKbps: Int = 12000,
  onDismiss: () -> Unit,
  onConfirmExport: (config: ExportConfig) -> Unit
) {
  var selectedResolution by remember { mutableStateOf(initialResolution) }
  var selectedFps by remember { mutableStateOf(initialFps) }
  var selectedQuality by remember { mutableStateOf(initialQuality) }
  var customBitrateKbps by remember { mutableIntStateOf(initialBitrateKbps) }
  var isCustomBitrateMode by remember { mutableStateOf(initialQuality == ExportQuality.CUSTOM) }

  // Calculate dimensions based on aspect ratio
  val dimensions = remember(selectedResolution, aspectRatio) {
    calculateExportDimensions(selectedResolution, aspectRatio)
  }

  // Active config representation
  val currentConfig = remember(selectedResolution, selectedFps, selectedQuality, customBitrateKbps, isCustomBitrateMode) {
    ExportConfig(
      resolution = selectedResolution,
      frameRate = selectedFps,
      quality = if (isCustomBitrateMode) ExportQuality.CUSTOM else selectedQuality,
      customBitrateKbps = customBitrateKbps
    )
  }

  // Live estimated file size calculation
  val estimatedSizeBytes = remember(currentConfig, totalDurationMs) {
    calculateEstimatedSize(totalDurationMs, currentConfig)
  }

  val estimatedMbString = remember(estimatedSizeBytes) {
    val mb = estimatedSizeBytes / (1024f * 1024f)
    if (mb < 1f) {
      String.format("%.2f MB", mb)
    } else {
      String.format("%.1f MB", mb)
    }
  }

  // Effective bitrate in Mbps for display
  val effectiveBitrateMbps = remember(currentConfig) {
    if (isCustomBitrateMode) {
      customBitrateKbps / 1000f
    } else {
      val base = when (selectedResolution) {
        Resolution.RES_480P -> 2.0f
        Resolution.RES_720P -> 4.5f
        Resolution.RES_1080P -> 8.5f
        Resolution.RES_2K -> 14.0f
        Resolution.RES_4K -> 25.0f
      }
      base * selectedQuality.bitrateMultiplier * (selectedFps.fps / 30f)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.92f)
        .testTag("export_config_dialog"),
      shape = RoundedCornerShape(20.dp),
      color = StudioSurface,
      tonalElevation = 8.dp,
      border = BorderStroke(1.dp, StudioBorder)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // --- Header ---
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SkyBlueContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.VideoSettings,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Export Configuration",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              )
              Text(
                text = "Render with Media3 Transformer",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = CyanAccent,
                  fontWeight = FontWeight.SemiBold
                )
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("cancel_export_dialog_btn")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Media3 Engine & Project Info Banner ---
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          color = StudioSurfaceVariant,
          border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.6f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = projectName.ifBlank { "Untitled Project" },
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                ),
                maxLines = 1
              )
              Text(
                text = "Duration: ${formatDuration(totalDurationMs)} • Aspect Ratio: ${aspectRatio.label}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
              )
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = SkyBlueContainer
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.Speed,
                  contentDescription = null,
                  tint = CyanAccent,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Media3 Transformer",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyanAccentDark,
                    fontSize = 10.sp
                  )
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Scrollable Settings Body ---
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // ==============================
          // 1. Resolution Selection
          // ==============================
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Resolution",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              )
              Text(
                text = "${dimensions.first} × ${dimensions.second} px",
                style = MaterialTheme.typography.labelMedium.copy(
                  color = CyanAccent,
                  fontWeight = FontWeight.Bold
                )
              )
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(Resolution.values()) { res ->
                val isSelected = selectedResolution == res
                val isRecommended = res == Resolution.RES_1080P
                val resDims = calculateExportDimensions(res, aspectRatio)

                Surface(
                  onClick = { selectedResolution = res },
                  shape = RoundedCornerShape(12.dp),
                  color = if (isSelected) SkyBlueContainer else StudioSurfaceVariant,
                  border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) CyanAccent else StudioBorder
                  ),
                  modifier = Modifier
                    .width(110.dp)
                    .height(68.dp)
                    .testTag("resolution_chip_${res.label}")
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(8.dp),
                    verticalArrangement = Arrangement.Center
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = res.label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                          fontWeight = FontWeight.Bold,
                          color = if (isSelected) CyanAccentDark else TextPrimary
                        )
                      )
                      if (isRecommended) {
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = CyanAccent
                        ) {
                          Text(
                            text = "REC",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                              color = Color.White,
                              fontSize = 8.sp,
                              fontWeight = FontWeight.Bold
                            )
                          )
                        }
                      }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${resDims.first}×${resDims.second}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                      )
                    )
                  }
                }
              }
            }
          }

          HorizontalDivider(color = StudioBorder)

          // ==============================
          // 2. Frame Rate (FPS) Selection
          // ==============================
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Frame Rate (FPS)",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              )
              Text(
                text = "${selectedFps.fps} frames/sec",
                style = MaterialTheme.typography.labelMedium.copy(
                  color = PurpleAccent,
                  fontWeight = FontWeight.Bold
                )
              )
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(FrameRate.values()) { fps ->
                val isSelected = selectedFps == fps
                val label = when (fps) {
                  FrameRate.FPS_24 -> "24 Cinematic"
                  FrameRate.FPS_25 -> "25 PAL"
                  FrameRate.FPS_30 -> "30 Standard"
                  FrameRate.FPS_50 -> "50 High"
                  FrameRate.FPS_60 -> "60 Smooth"
                }

                Surface(
                  onClick = { selectedFps = fps },
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSelected) PurpleAccent.copy(alpha = 0.15f) else StudioSurfaceVariant,
                  border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) PurpleAccent else StudioBorder
                  ),
                  modifier = Modifier
                    .height(44.dp)
                    .testTag("fps_chip_${fps.fps}")
                ) {
                  Box(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) PurpleAccent else TextPrimary
                      )
                    )
                  }
                }
              }
            }
          }

          HorizontalDivider(color = StudioBorder)

          // ==============================
          // 3. Bitrate & Quality Settings
          // ==============================
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Bitrate & Encoding Quality",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              )
              Text(
                text = String.format("%.1f Mbps", effectiveBitrateMbps),
                style = MaterialTheme.typography.labelMedium.copy(
                  color = CyanAccent,
                  fontWeight = FontWeight.Bold
                )
              )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Quality Presets
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              ExportQuality.values().forEach { quality ->
                val isSelected = if (quality == ExportQuality.CUSTOM) isCustomBitrateMode else (!isCustomBitrateMode && selectedQuality == quality)
                Surface(
                  onClick = {
                    if (quality == ExportQuality.CUSTOM) {
                      isCustomBitrateMode = true
                      selectedQuality = ExportQuality.CUSTOM
                    } else {
                      isCustomBitrateMode = false
                      selectedQuality = quality
                    }
                  },
                  shape = RoundedCornerShape(8.dp),
                  color = if (isSelected) SkyBlueContainer else StudioSurfaceVariant,
                  border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) CyanAccent else StudioBorder
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("quality_preset_${quality.name}")
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = when (quality) {
                        ExportQuality.LOW -> "Low"
                        ExportQuality.MEDIUM -> "Med"
                        ExportQuality.HIGH -> "High"
                        ExportQuality.CUSTOM -> "Custom"
                      },
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) CyanAccentDark else TextPrimary
                      )
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Bitrate Slider & Quick Shortcuts
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              color = StudioSurfaceVariant,
              border = BorderStroke(1.dp, StudioBorder)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Bitrate Slider",
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.SemiBold,
                      color = TextSecondary
                    )
                  )
                  Text(
                    text = "${(customBitrateKbps / 1000f).roundToInt()} Mbps (${customBitrateKbps} Kbps)",
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (isCustomBitrateMode) CyanAccentDark else TextSecondary
                    )
                  )
                }

                Slider(
                  value = customBitrateKbps.toFloat(),
                  onValueChange = { value ->
                    customBitrateKbps = value.roundToInt()
                    isCustomBitrateMode = true
                    selectedQuality = ExportQuality.CUSTOM
                  },
                  valueRange = 1000f..50000f,
                  steps = 97, // 500 Kbps increments
                  colors = SliderDefaults.colors(
                    thumbColor = CyanAccent,
                    activeTrackColor = CyanAccent,
                    inactiveTrackColor = StudioBorder
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bitrate_slider")
                )

                // Quick Bitrate Shortcut Chips
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  listOf(4000 to "4M", 8000 to "8M", 12000 to "12M", 20000 to "20M", 35000 to "35M").forEach { (kbps, label) ->
                    val isChipSelected = isCustomBitrateMode && customBitrateKbps == kbps
                    Surface(
                      onClick = {
                        customBitrateKbps = kbps
                        isCustomBitrateMode = true
                        selectedQuality = ExportQuality.CUSTOM
                      },
                      shape = RoundedCornerShape(6.dp),
                      color = if (isChipSelected) CyanAccent else StudioSurface,
                      border = BorderStroke(1.dp, if (isChipSelected) CyanAccent else StudioBorder),
                      modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .testTag("bitrate_shortcut_$label")
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Text(
                          text = label,
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isChipSelected) Color.White else TextSecondary
                          )
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          HorizontalDivider(color = StudioBorder)

          // ==============================
          // 4. Output Summary & Pipeline Card
          // ==============================
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = StudioSurfaceVariant.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, StudioBorder)
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Estimated File Size",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Text(
                  text = estimatedMbString,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                  )
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Video & Audio Codec",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Text(
                  text = "H.264 (AVC) • AAC 48kHz Stereo",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                  )
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Container Format",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Text(
                  text = "MP4 (MPEG-4 Part 14)",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                  )
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Render Engine",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Text(
                  text = "Media3 Transformer HW Pipeline",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = GreenAccent
                  )
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Action Buttons ---
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("dismiss_export_dialog_btn"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, StudioBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
          ) {
            Text("Cancel", fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = {
              onConfirmExport(currentConfig)
            },
            modifier = Modifier
              .weight(2f)
              .height(48.dp)
              .testTag("confirm_export_dialog_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = CyanAccent,
              contentColor = Color.White
            )
          ) {
            Icon(Icons.Default.MovieFilter, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Render with Media3",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }
    }
  }
}

/**
 * Calculates export pixel dimensions honoring project aspect ratio and target resolution.
 */
internal fun calculateExportDimensions(res: Resolution, aspect: AspectRatio): Pair<Int, Int> {
  val longEdge = when (res) {
    Resolution.RES_480P -> 854
    Resolution.RES_720P -> 1280
    Resolution.RES_1080P -> 1920
    Resolution.RES_2K -> 2560
    Resolution.RES_4K -> 3840
  }
  val isLandscape = aspect.ratio >= 1.0f
  val ratioMultiplier = if (isLandscape) aspect.ratio else 1.0f / aspect.ratio
  val shortEdge = (longEdge / ratioMultiplier).toInt()
  val w = if (isLandscape) longEdge else shortEdge
  val h = if (isLandscape) shortEdge else longEdge
  val evenW = (w / 2) * 2
  val evenH = (h / 2) * 2
  return Pair(evenW.coerceAtLeast(320), evenH.coerceAtLeast(320))
}

/**
 * Computes estimated file size in bytes based on duration and export config.
 */
internal fun calculateEstimatedSize(durationMs: Long, config: ExportConfig): Long {
  val durationSec = (durationMs / 1000f).coerceAtLeast(1f)
  val effectiveBitrate = if (config.quality == ExportQuality.CUSTOM && config.customBitrateKbps > 0) {
    config.customBitrateKbps * 1000L
  } else {
    val baseBitrate = when (config.resolution) {
      Resolution.RES_480P -> 2_000_000L
      Resolution.RES_720P -> 4_500_000L
      Resolution.RES_1080P -> 8_500_000L
      Resolution.RES_2K -> 14_000_000L
      Resolution.RES_4K -> 25_000_000L
    }
    (baseBitrate * config.quality.bitrateMultiplier * (config.frameRate.fps / 30f)).toLong()
  }
  return (effectiveBitrate * durationSec / 8).toLong()
}
