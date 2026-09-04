package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.StudioViewModel
import com.example.ui.components.PrimaryPillButton
import com.example.ui.components.formatDurationShort
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISuiteScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val isAIBusy by viewModel.isAIBusy.collectAsState()
  val statusMessage by viewModel.aiStatusMessage.collectAsState()
  val highlights by viewModel.aiHighlights.collectAsState()
  val timeline by viewModel.timelineEngine.timeline.collectAsState()

  var selectedLanguage by remember { mutableStateOf("English") }
  val languages = listOf("English", "Spanish", "French", "German", "Japanese", "Chinese")

  var ttsText by remember { mutableStateOf("Welcome to AH Video Studio, the ultimate creative suite.") }
  var ttsPitch by remember { mutableStateOf(1.0f) }
  var ttsRate by remember { mutableStateOf(1.0f) }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(StudioDarkBg),
    containerColor = StudioDarkBg,
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AI Creative Suite", color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.horizontalGradient(listOf(CyanAccent, PurpleAccent)))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text("POWERED BY GEMINI", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
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
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // AI Processing Indicator
      if (isAIBusy || statusMessage.isNotBlank()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CyanAccent, PurpleAccent)))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (isAIBusy) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CyanAccent, strokeWidth = 3.dp)
                Spacer(modifier = Modifier.width(12.dp))
              } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenAccent)
                Spacer(modifier = Modifier.width(12.dp))
              }
              Text(
                text = statusMessage.ifBlank { "Processing AI request..." },
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Medium)
              )
            }
          }
        }
      }

      // Feature 1: AI Auto Captions
      item {
        AIFeatureCard(
          title = "AI Auto Captions & Subtitles",
          description = "Transcribe speech automatically with synchronized word timing, animations, and multi-language support.",
          icon = Icons.Default.Subtitles
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Select Language", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(languages) { lang ->
                FilterChip(
                  selected = selectedLanguage == lang,
                  onClick = { selectedLanguage = lang },
                  label = { Text(lang) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = Color.Black)
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = { viewModel.runAIAutoCaptions(selectedLanguage) },
                enabled = !isAIBusy,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(18.dp)
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate Captions", fontWeight = FontWeight.Bold)
              }

              if (timeline.textClips.isNotEmpty()) {
                OutlinedButton(
                  onClick = {
                    val srt = viewModel.aiTools.exportSrt(timeline.textClips)
                    clipboardManager.setText(AnnotatedString(srt))
                    Toast.makeText(context, "SRT Subtitles copied to clipboard!", Toast.LENGTH_SHORT).show()
                  },
                  shape = RoundedCornerShape(18.dp)
                ) {
                  Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Export .SRT", color = TextPrimary)
                }
              }
            }
          }
        }
      }

      // Feature 2: AI Voice Studio (Text-to-Speech)
      item {
        AIFeatureCard(
          title = "AI Voice Studio (Text-to-Speech)",
          description = "Convert any written script or narration into natural offline voiceover speech instantly.",
          icon = Icons.Default.RecordVoiceOver
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = ttsText,
              onValueChange = { ttsText = it },
              placeholder = { Text("Enter script for AI voice synthesis...") },
              modifier = Modifier.fillMaxWidth(),
              maxLines = 3,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurpleAccent,
                unfocusedBorderColor = StudioBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = {
                  viewModel.audioEngine.synthesizeTts(ttsText, ttsPitch, ttsRate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = Color.White),
                shape = RoundedCornerShape(18.dp)
              ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Speak Preview", fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = {
                  viewModel.audioEngine.synthesizeTts(ttsText, ttsPitch, ttsRate)
                  viewModel.timelineEngine.addAudioClip("AI Voice: $ttsText", 5000L)
                  Toast.makeText(context, "Added AI Voiceover to Timeline!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(18.dp)
              ) {
                Text("+ Add to Track", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Feature 3: AI Video Highlight Detection
      item {
        AIFeatureCard(
          title = "AI Highlight & Viral Moments",
          description = "Analyze motion intensity, composition, and pacing to detect optimal viral hooks.",
          icon = Icons.Default.FlashOn
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = { viewModel.runAIHighlightAnalysis() },
              enabled = !isAIBusy,
              colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant, contentColor = CyanAccent),
              shape = RoundedCornerShape(18.dp)
            ) {
              Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Detect Highlights", fontWeight = FontWeight.Bold)
            }

            if (highlights.isNotEmpty()) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                highlights.forEach { h ->
                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(h.title, style = MaterialTheme.typography.labelMedium.copy(color = CyanAccent, fontWeight = FontWeight.Bold))
                        Text(h.description, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                        Text(
                          "${formatDurationShort(h.startTimeMs)} - ${formatDurationShort(h.endTimeMs)}",
                          style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                        )
                      }
                      Button(
                        onClick = {
                          viewModel.timelineEngine.setPosition(h.startTimeMs)
                          viewModel.navigateTo(AppScreen.EDITOR)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                      ) {
                        Text("Jump to Hook", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Feature 4: AI Auto Edit Montage
      item {
        AIFeatureCard(
          title = "AI Auto-Edit Montage",
          description = "Automatically sequence video clips, synchronize cuts with transitions, and assemble a complete reel.",
          icon = Icons.Default.MovieFilter
        ) {
          PrimaryPillButton(
            text = "Generate Auto Reel",
            icon = Icons.Default.AutoFixHigh,
            onClick = {
              viewModel.runAIAutoEdit()
              viewModel.navigateTo(AppScreen.EDITOR)
            },
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      item { Spacer(modifier = Modifier.height(32.dp)) }
    }
  }
}

@Composable
private fun AIFeatureCard(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  content: @Composable ColumnScope.() -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = StudioSurface),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(StudioBorder, StudioBorder.copy(alpha = 0.5f))))
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(CyanAccent.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
          )
          Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
          )
        }
      }

      content()
    }
  }
}
