package com.example.ui.components.text

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.TextClip
import com.example.engine.SelectedTrackElement
import com.example.engine.text.TextLayerRenderer
import com.example.ui.StudioViewModel
import com.example.ui.theme.*
import com.example.util.FontManager
import com.example.util.FontOption
import java.util.UUID

enum class TextEditorSecondaryTab(val label: String, val icon: ImageVector) {
  TEMPLATES("Templates", Icons.Default.GridView),
  FONTS("Fonts", Icons.Default.Title),
  STYLES("Styles", Icons.Default.Palette),
  EFFECTS("Effects", Icons.Default.AutoAwesome),
  ANIMATIONS("Animations", Icons.Default.PlayCircle)
}

enum class FormattingControlTab(val label: String, val icon: ImageVector) {
  FONT("Aa Font", Icons.Default.Title),
  SIZE("Size", Icons.Default.FormatSize),
  COLOR("Color", Icons.Default.Palette),
  ALIGN("Align", Icons.Default.FormatAlignLeft),
  ANIMATION("Animation", Icons.Default.PlayCircle),
  EFFECTS("Effects", Icons.Default.AutoAwesome),
  SPACING("Spacing", Icons.Default.FormatLineSpacing),
  STYLE("Style", Icons.Default.FormatBold),
  SHADOW("Shadow", Icons.Default.Tonality),
  STROKE("Stroke", Icons.Default.BorderColor)
}

/**
 * Utility to detect RTL languages like Urdu, Arabic, Persian, Pashto, etc.
 */
fun isRtlScript(text: String): Boolean {
  if (text.isBlank()) return false
  return text.any { char ->
    val block = Character.UnicodeBlock.of(char)
    block == Character.UnicodeBlock.ARABIC ||
      block == Character.UnicodeBlock.ARABIC_SUPPLEMENT ||
      block == Character.UnicodeBlock.ARABIC_EXTENDED_A ||
      block == Character.UnicodeBlock.ARABIC_EXTENDED_B ||
      block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
      block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B ||
      (char >= '\u0600' && char <= '\u06FF') ||
      (char >= '\u0750' && char <= '\u077F') ||
      (char >= '\u08A0' && char <= '\u08FF') ||
      (char >= '\uFB50' && char <= '\uFDFF') ||
      (char >= '\uFE70' && char <= '\uFEFF')
  }
}

@Composable
fun TextStudioPanel(
  viewModel: StudioViewModel,
  selectedTextClip: TextClip? = null,
  onDismiss: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val installedPlugins by viewModel.installedPlugins.collectAsState()

  // Manage draft clip state
  val draftClip = remember(selectedTextClip) {
    selectedTextClip ?: TextClip(
      id = UUID.randomUUID().toString(),
      text = "CREATE AMAZING VIDEOS",
      fontFamily = "Impact",
      fontSizeSp = 36f,
      fontWeight = 900,
      textColor = 0xFF00E5FF,
      hasShadow = true,
      shadowColor = 0xFF000000,
      shadowBlur = 8f,
      animationType = "Pop",
      animDurationMs = 600L
    )
  }

  var activeClip by remember(draftClip) { mutableStateOf(draftClip) }
  val onUpdateActiveClip: (TextClip) -> Unit = { updated ->
    activeClip = updated
    if (selectedTextClip != null) {
      viewModel.timelineEngine.updateTextClip(updated)
    }
  }

  var activeTab by remember { mutableStateOf(TextEditorSecondaryTab.TEMPLATES) }
  var activeFormattingTab by remember { mutableStateOf(FormattingControlTab.FONT) }
  var fontOptionsList by remember { mutableStateOf(FontManager.getAvailableFonts(context)) }

  // Hero Live Animation State
  var isHeroPlaying by remember { mutableStateOf(true) }
  var heroSpeedMultiplier by remember { mutableStateOf(1.0f) }
  var heroReplayTrigger by remember { mutableIntStateOf(0) }
  var showFullscreenPreview by remember { mutableStateOf(false) }

  val isRtl = remember(activeClip.text) { isRtlScript(activeClip.text) }

  LaunchedEffect(installedPlugins) {
    fontOptionsList = FontManager.getAvailableFonts(context)
  }

  val fontPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      val imported = FontManager.importFont(context, uri)
      if (imported != null) {
        fontOptionsList = FontManager.getAvailableFonts(context)
        onUpdateActiveClip(
          activeClip.copy(fontFamily = imported.id, customFontPath = imported.filePath)
        )
        Toast.makeText(context, "Imported font: ${imported.name}", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "Failed to load font file", Toast.LENGTH_SHORT).show()
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFF070B14))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 1. Header with Title, RTL Badge & Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1E68F6), Color(0xFF00C2FF)))),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Tt",
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            color = Color.White
          )
        }
        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = if (selectedTextClip != null) "Edit Text Layer" else "Text Editing",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )

            if (isRtl) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF059669).copy(alpha = 0.25f),
                border = BorderStroke(1.dp, Color(0xFF10B981))
              ) {
                Text(
                  text = "RTL Urdu/Arabic 🇵🇰🇸🇦",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF34D399),
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
              }
            }
          }
          Text(
            text = "130+ Live Animated Templates • Fonts • Styles • Effects • Animations",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Fullscreen Inspect Button
        IconButton(
          onClick = { showFullscreenPreview = true },
          modifier = Modifier
            .size(32.dp)
            .background(Color(0xFF1E293B), CircleShape)
        ) {
          Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen Preview", tint = CyanAccent, modifier = Modifier.size(18.dp))
        }

        if (selectedTextClip != null) {
          Button(
            onClick = {
              viewModel.timelineEngine.updateTextClip(activeClip)
              if (onDismiss != null) onDismiss() else viewModel.setActiveToolbarTab(null)
            },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E68F6), contentColor = Color.White),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        } else {
          Button(
            onClick = {
              val currentPos = viewModel.timelineEngine.currentPositionMs.value
              val totalDuration = viewModel.timelineEngine.timeline.value.totalDurationMs.coerceAtLeast(1000L)
              val calculatedDuration = 3000L.coerceAtMost(maxOf(1000L, totalDuration - currentPos))
              val newClip = activeClip.copy(
                id = UUID.randomUUID().toString(),
                timelineStartMs = currentPos,
                durationMs = calculatedDuration
              )
              viewModel.timelineEngine.addTextClipObject(newClip)
              viewModel.timelineEngine.selectElement(SelectedTrackElement.Text(newClip.id))
              if (onDismiss != null) onDismiss() else viewModel.setActiveToolbarTab(null)
            },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E68F6), contentColor = Color.White),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }

        IconButton(
          onClick = {
            if (onDismiss != null) onDismiss() else viewModel.setActiveToolbarTab(null)
          },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }
    }

    // 2. HERO LIVE VIEW (PREVIEW CARD)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(145.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF0D1424), Color(0xFF030712))))
        .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(14.dp))
    ) {
      val infiniteTransition = rememberInfiniteTransition(label = "hero_anim_${activeClip.text.hashCode()}_$heroReplayTrigger")
      val animLoopMs by if (isHeroPlaying) {
        infiniteTransition.animateFloat(
          initialValue = 0f,
          targetValue = 2400f,
          animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
          ),
          label = "hero_loop_ms"
        )
      } else {
        remember { mutableFloatStateOf(1200f) }
      }

      Canvas(modifier = Modifier.fillMaxSize()) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val w = size.width.toInt()
        val h = size.height.toInt()
        if (w > 0 && h > 0) {
          TextLayerRenderer.drawTemplatePreview(
            canvas = nativeCanvas,
            clip = activeClip,
            previewLoopMs = animLoopMs.toLong(),
            width = w,
            height = h,
            context = context,
            speedMultiplier = heroSpeedMultiplier
          )
        }
      }

      // Live View Top Left Badge: LIVE VIEW 60FPS HD
      Row(
        modifier = Modifier
          .padding(8.dp)
          .align(Alignment.TopStart),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color.Black.copy(alpha = 0.65f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isHeroPlaying) Color(0xFF00E5FF) else Color(0xFFEF4444))
            )
            Text(
              text = if (isHeroPlaying) "LIVE VIEW" else "PAUSED",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "60FPS",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = CyanAccent
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(4.dp),
          color = Color(0xFF1E68F6).copy(alpha = 0.3f),
          border = BorderStroke(1.dp, Color(0xFF1E68F6))
        ) {
          Text(
            text = "HD",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }
      }

      // Live Overlay Controls Bottom Right Bar (Replay, Play/Pause, Fullscreen)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp)
          .align(Alignment.BottomEnd),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${activeClip.animationType} • ${activeClip.fontFamily}",
          fontSize = 10.sp,
          color = TextSecondary,
          modifier = Modifier.padding(start = 4.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          IconButton(
            onClick = { heroReplayTrigger++ },
            modifier = Modifier
              .size(28.dp)
              .background(Color.Black.copy(alpha = 0.65f), CircleShape)
          ) {
            Icon(Icons.Default.Replay, contentDescription = "Replay", tint = Color.White, modifier = Modifier.size(15.dp))
          }

          IconButton(
            onClick = { isHeroPlaying = !isHeroPlaying },
            modifier = Modifier
              .size(28.dp)
              .background(Color.Black.copy(alpha = 0.65f), CircleShape)
          ) {
            Icon(
              if (isHeroPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = "Toggle Play",
              tint = Color.White,
              modifier = Modifier.size(15.dp)
            )
          }

          IconButton(
            onClick = { showFullscreenPreview = true },
            modifier = Modifier
              .size(28.dp)
              .background(Color(0xFF1E68F6), CircleShape)
          ) {
            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(15.dp))
          }
        }
      }
    }

    // 3. LARGE "ADD TEXT" BUTTON DIRECTLY BELOW LIVE VIEW
    Surface(
      onClick = {
        val currentPos = viewModel.timelineEngine.currentPositionMs.value
        val totalDuration = viewModel.timelineEngine.timeline.value.totalDurationMs.coerceAtLeast(1000L)
        val calculatedDuration = 3000L.coerceAtMost(maxOf(1000L, totalDuration - currentPos))
        val newClip = activeClip.copy(
          id = UUID.randomUUID().toString(),
          timelineStartMs = currentPos,
          durationMs = calculatedDuration
        )
        viewModel.timelineEngine.addTextClipObject(newClip)
        viewModel.timelineEngine.selectElement(SelectedTrackElement.Text(newClip.id))
        Toast.makeText(context, "Added new text layer!", Toast.LENGTH_SHORT).show()
      },
      shape = RoundedCornerShape(14.dp),
      color = Color.Transparent,
      modifier = Modifier.fillMaxWidth()
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .background(Brush.horizontalGradient(listOf(Color(0xFF1E68F6), Color(0xFF00A3FF))))
          .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left: + T Icon inside subtle rounded badge
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = "T",
                  fontWeight = FontWeight.Black,
                  fontSize = 12.sp,
                  color = Color.White
                )
              }
            }

            Text(
              text = "Add Text",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = Color.White
            )
          }

          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }
      }
    }

    // 4. PRIMARY SUB-NAVIGATION BUTTONS: TEMPLATES | FONTS | STYLES | EFFECTS | ANIMATIONS
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(vertical = 2.dp)
    ) {
      items(TextEditorSecondaryTab.values()) { tab ->
        val isSelected = activeTab == tab
        val bgBrush = if (isSelected) {
          Brush.horizontalGradient(listOf(Color(0xFF1E68F6), Color(0xFF00A3FF)))
        } else {
          SolidColor(Color(0xFF0F172A))
        }

        Surface(
          onClick = { activeTab = tab },
          shape = RoundedCornerShape(12.dp),
          color = Color.Transparent,
          border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
          Box(
            modifier = Modifier
              .background(bgBrush)
              .padding(horizontal = 14.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = tab.label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = Color.White,
                fontSize = 12.sp
              )
            }
          }
        }
      }
    }

    // 5. DEDICATED TEXT INPUT & FORMATTING CONTROLS PANEL
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1424)),
      border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // PROMINENT TEXT INPUT FIELD
        OutlinedTextField(
          value = activeClip.text,
          onValueChange = { newText ->
            onUpdateActiveClip(activeClip.copy(text = newText))
          },
          placeholder = { Text("Type your text…", color = TextSecondary, fontSize = 13.sp) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.TextFields,
              contentDescription = null,
              tint = Color(0xFF00C2FF),
              modifier = Modifier.size(18.dp)
            )
          },
          trailingIcon = {
            if (activeClip.text.isNotEmpty()) {
              IconButton(
                onClick = { onUpdateActiveClip(activeClip.copy(text = "")) },
                modifier = Modifier.size(20.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
              }
            }
          },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 2,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00A3FF),
            unfocusedBorderColor = Color(0xFF1E293B),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = Color(0xFF070B14),
            unfocusedContainerColor = Color(0xFF070B14)
          ),
          shape = RoundedCornerShape(12.dp)
        )

        // Quick Preset Script Suggestion Pills
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          contentPadding = PaddingValues(vertical = 1.dp)
        ) {
          val samples = listOf(
            "🇵🇰 اردو: خوبصورت اردو متن",
            "🇸🇦 Arabic: الخط العربي الجميل",
            "⚡ Kinetic: MAKE IT HAPPEN",
            "📹 Vlog: DAILY VLOG #42",
            "🔥 Sale: 50% OFF TODAY",
            "🎬 Intro: WELCOME TO MY CHANNEL"
          )
          items(samples) { sample ->
            val cleanText = sample.substringAfter(": ").trim()
            Surface(
              onClick = { onUpdateActiveClip(activeClip.copy(text = cleanText)) },
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF162032),
              border = BorderStroke(1.dp, Color(0xFF26354F))
            ) {
              Text(
                text = sample,
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

        // FORMATTING CONTROLS SELECTOR BAR: FONT | SIZE | COLOR | ALIGN | ANIMATION | EFFECTS | SPACING | STYLE | SHADOW | STROKE
        Text(
          text = "FORMATTING CONTROLS",
          style = MaterialTheme.typography.labelSmall.copy(
            color = Color(0xFF00C2FF),
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 1.sp
          )
        )

        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          contentPadding = PaddingValues(vertical = 1.dp)
        ) {
          items(FormattingControlTab.values()) { fTab ->
            val isSelected = activeFormattingTab == fTab
            Surface(
              onClick = { activeFormattingTab = fTab },
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) Color(0xFF1E68F6) else Color(0xFF162032),
              border = BorderStroke(1.dp, if (isSelected) Color(0xFF00C2FF) else Color(0xFF26354F))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = fTab.icon,
                  contentDescription = fTab.label,
                  tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = fTab.label,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                )
              }
            }
          }
        }

        // DYNAMIC SUB-PANEL FOR SELECTED FORMATTING CONTROL
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070B14))
            .border(1.dp, Color(0xFF162032), RoundedCornerShape(12.dp))
            .padding(10.dp)
        ) {
          when (activeFormattingTab) {
            FormattingControlTab.FONT -> LiveFontsTabSection(
              clip = activeClip,
              availableFonts = fontOptionsList,
              onSelectFont = { fontId, path ->
                onUpdateActiveClip(activeClip.copy(fontFamily = fontId, customFontPath = path))
              },
              onImportFont = { fontPickerLauncher.launch(arrayOf("*/*")) }
            )
            FormattingControlTab.SIZE -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Font Size: ${activeClip.fontSizeSp.toInt()} sp",
                  style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  listOf("Small" to 18f, "Med" to 28f, "Large" to 42f, "Title" to 60f, "Hero" to 80f).forEach { (presetName, sizeVal) ->
                    Surface(
                      onClick = { onUpdateActiveClip(activeClip.copy(fontSizeSp = sizeVal)) },
                      shape = RoundedCornerShape(6.dp),
                      color = if (activeClip.fontSizeSp == sizeVal) Color(0xFF1E68F6) else Color(0xFF162032)
                    ) {
                      Text(
                        text = presetName,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                      )
                    }
                  }
                }
              }

              Slider(
                value = activeClip.fontSizeSp,
                onValueChange = { onUpdateActiveClip(activeClip.copy(fontSizeSp = it)) },
                valueRange = 12f..120f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF00C2FF), activeTrackColor = Color(0xFF1E68F6))
              )
            }
            FormattingControlTab.COLOR -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Color Palette",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
              )
              val palette = listOf(
                0xFFFFFFFF, 0xFF00E5FF, 0xFF1E68F6, 0xFFFF007F, 0xFFFFD700, 0xFF10B981,
                0xFF8B5CF6, 0xFFF59E0B, 0xFFEF4444, 0xFFFFEA00, 0xFFEC4899, 0xFF000000
              )
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(palette) { colorHex ->
                  val isSelected = activeClip.textColor == colorHex
                  Box(
                    modifier = Modifier
                      .size(30.dp)
                      .clip(CircleShape)
                      .background(Color(colorHex))
                      .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) Color(0xFF00C2FF) else Color.White.copy(alpha = 0.4f), CircleShape)
                      .clickable { onUpdateActiveClip(activeClip.copy(textColor = colorHex)) }
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Opacity: ${(activeClip.opacity * 100).toInt()}%",
                  style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                )
                Slider(
                  value = activeClip.opacity,
                  onValueChange = { onUpdateActiveClip(activeClip.copy(opacity = it)) },
                  valueRange = 0.1f..1.0f,
                  modifier = Modifier.weight(1f).padding(start = 12.dp),
                  colors = SliderDefaults.colors(thumbColor = Color(0xFF00C2FF), activeTrackColor = Color(0xFF1E68F6))
                )
              }
            }
            FormattingControlTab.ALIGN -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Text Alignment & Transform",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  listOf("Left", "Center", "Right").forEach { align ->
                    val isSel = activeClip.alignment.equals(align, true)
                    Surface(
                      onClick = { onUpdateActiveClip(activeClip.copy(alignment = align)) },
                      shape = RoundedCornerShape(8.dp),
                      color = if (isSel) Color(0xFF1E68F6) else Color(0xFF162032),
                      border = BorderStroke(1.dp, if (isSel) Color(0xFF00C2FF) else Color(0xFF26354F))
                    ) {
                      Text(
                        text = align,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                      )
                    }
                  }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Surface(
                    onClick = { onUpdateActiveClip(activeClip.copy(isAllCaps = !activeClip.isAllCaps)) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (activeClip.isAllCaps) Color(0xFF1E68F6) else Color(0xFF162032),
                    border = BorderStroke(1.dp, if (activeClip.isAllCaps) Color(0xFF00C2FF) else Color(0xFF26354F))
                  ) {
                    Text(
                      text = "UPPERCASE",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                  }
                }
              }
            }
            FormattingControlTab.ANIMATION -> LiveAnimationsTabSection(
              clip = activeClip,
              onUpdate = { updated -> onUpdateActiveClip(updated) }
            )
            FormattingControlTab.EFFECTS -> LiveEffectsTabSection(
              clip = activeClip,
              onUpdate = { updated -> onUpdateActiveClip(updated) }
            )
            FormattingControlTab.SPACING -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Letter & Line Spacing",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("Letter Spacing: ${activeClip.letterSpacing.toInt()}px", fontSize = 10.sp, color = TextSecondary)
                  Slider(
                    value = activeClip.letterSpacing,
                    onValueChange = { onUpdateActiveClip(activeClip.copy(letterSpacing = it)) },
                    valueRange = 0f..30f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00C2FF), activeTrackColor = Color(0xFF1E68F6))
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text("Line Spacing: ${String.format("%.1f", activeClip.lineSpacing)}x", fontSize = 10.sp, color = TextSecondary)
                  Slider(
                    value = activeClip.lineSpacing,
                    onValueChange = { onUpdateActiveClip(activeClip.copy(lineSpacing = it)) },
                    valueRange = 0.8f..2.5f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00C2FF), activeTrackColor = Color(0xFF1E68F6))
                  )
                }
              }
            }
            FormattingControlTab.STYLE -> LiveStylesTabSection(
              clip = activeClip,
              onUpdate = { updated -> onUpdateActiveClip(updated) }
            )
            FormattingControlTab.SHADOW -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Drop Shadow Effect",
                  style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                Switch(
                  checked = activeClip.hasShadow,
                  onCheckedChange = { onUpdateActiveClip(activeClip.copy(hasShadow = it)) },
                  colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00C2FF), checkedTrackColor = Color(0xFF1E68F6))
                )
              }

              if (activeClip.hasShadow) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Blur: ${activeClip.shadowBlur.toInt()}px", fontSize = 10.sp, color = TextSecondary)
                    Slider(
                      value = activeClip.shadowBlur,
                      onValueChange = { onUpdateActiveClip(activeClip.copy(shadowBlur = it)) },
                      valueRange = 1f..30f,
                      colors = SliderDefaults.colors(thumbColor = Color(0xFF00C2FF), activeTrackColor = Color(0xFF1E68F6))
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Offset: ${activeClip.shadowOffsetX.toInt()}px", fontSize = 10.sp, color = TextSecondary)
                    Slider(
                      value = activeClip.shadowOffsetX,
                      onValueChange = { onUpdateActiveClip(activeClip.copy(shadowOffsetX = it, shadowOffsetY = it)) },
                      valueRange = 0f..20f,
                      colors = SliderDefaults.colors(thumbColor = Color(0xFF00C2FF), activeTrackColor = Color(0xFF1E68F6))
                    )
                  }
                }
              }
            }
            FormattingControlTab.STROKE -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Text Outline / Stroke",
                  style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "${activeClip.strokeWidth.toInt()} px",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF00C2FF)
                )
              }

              Slider(
                value = activeClip.strokeWidth,
                onValueChange = { onUpdateActiveClip(activeClip.copy(strokeWidth = it)) },
                valueRange = 0f..20f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF00C2FF), activeTrackColor = Color(0xFF1E68F6))
              )
            }
          }
        }
      }
    }

    // 6. MAIN SECONDARY TAB VIEW CONTENT (When Templates tab is active)
    if (activeTab == TextEditorSecondaryTab.TEMPLATES) {
      LiveTemplatesTabSection(
        clip = activeClip,
        userTextOverride = activeClip.text,
        onApplyTemplate = { updatedClip -> onUpdateActiveClip(updatedClip) },
        onOpenFullscreenInspect = { showFullscreenPreview = true }
      )
    }
  }

  // Fullscreen Live Preview Modal
  if (showFullscreenPreview) {
    TextStudioFullscreenPreviewModal(
      clip = activeClip,
      onDismiss = { showFullscreenPreview = false },
      onApplyToTimeline = {
        val currentPos = viewModel.timelineEngine.currentPositionMs.value
        val totalDuration = viewModel.timelineEngine.timeline.value.totalDurationMs.coerceAtLeast(1000L)
        val calculatedDuration = 3000L.coerceAtMost(maxOf(1000L, totalDuration - currentPos))
        val newClip = activeClip.copy(
          id = UUID.randomUUID().toString(),
          timelineStartMs = currentPos,
          durationMs = calculatedDuration
        )
        viewModel.timelineEngine.addTextClipObject(newClip)
        viewModel.timelineEngine.selectElement(SelectedTrackElement.Text(newClip.id))
        showFullscreenPreview = false
        if (onDismiss != null) onDismiss() else viewModel.setActiveToolbarTab(null)
      }
    )
  }
}

// -------------------------------------------------------------
// TAB 1: LIVE TEMPLATES SECTION (130+ REAL Animated Graphic Templates)
// -------------------------------------------------------------
@Composable
private fun LiveTemplatesTabSection(
  clip: TextClip,
  userTextOverride: String,
  onApplyTemplate: (TextClip) -> Unit,
  onOpenFullscreenInspect: () -> Unit
) {
  var selectedCategory by remember { mutableStateOf("Trending") }
  var favoriteTemplateIds by remember { mutableStateOf(setOf("kinetic_burst", "neon_glow_master", "urdu_nastaliq_royal")) }
  var viewColumns by remember { mutableStateOf(2) }

  val displayedTemplates = remember(selectedCategory, favoriteTemplateIds) {
    when (selectedCategory) {
      "All" -> ALL_TEXT_TEMPLATES
      "Favorites" -> ALL_TEXT_TEMPLATES.filter { it.id in favoriteTemplateIds }
      "Trending" -> ALL_TEXT_TEMPLATES.filter {
        it.category.equals("Trending", ignoreCase = true) || it.isTrending || it.tags.contains("Trending", ignoreCase = true)
      }
      else -> ALL_TEXT_TEMPLATES.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // 28 Category Filter Pills
    val categoryPills = listOf(
      "Trending" to "Trending 🔥",
      "Favorites" to "Favorites ❤️",
      "Kinetic" to "Kinetic ⚡",
      "Minimal" to "Minimal ✨",
      "Neon" to "Neon 💡",
      "3D" to "3D 🧊",
      "Cinematic" to "Cinematic 🎬",
      "Glow" to "Glow 🌟",
      "Typewriter" to "Typewriter ⌨️",
      "Social Media" to "Social 📱",
      "Vlog" to "Vlog 📹",
      "Urdu" to "Urdu 🇵🇰",
      "Arabic" to "Arabic 🇸🇦",
      "Business" to "Business 💼",
      "Travel" to "Travel ✈️",
      "Motivation" to "Motivation 🏆",
      "Sports" to "Sports ⚽",
      "Gaming" to "Gaming 🎮",
      "Luxury" to "Luxury 💎",
      "Fashion" to "Fashion 👗",
      "Music" to "Music 🎵",
      "News" to "News 📰",
      "Sale" to "Sale 🏷️",
      "Intro" to "Intro 👋",
      "Outro" to "Outro 🎬",
      "Creative" to "Creative 🎨",
      "All" to "All 130+"
    )

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(vertical = 2.dp)
    ) {
      items(categoryPills) { (catKey, catLabel) ->
        val isSelected = selectedCategory == catKey
        val bgBrush = if (isSelected) {
          Brush.horizontalGradient(listOf(Color(0xFF1E68F6), Color(0xFF00A3FF)))
        } else {
          SolidColor(Color(0xFF0F172A))
        }

        Surface(
          onClick = { selectedCategory = catKey },
          shape = RoundedCornerShape(20.dp),
          color = Color.Transparent,
          border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
          Box(
            modifier = Modifier
              .background(bgBrush)
              .padding(horizontal = 14.dp, vertical = 7.dp)
          ) {
            Text(
              text = catLabel,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = Color.White
            )
          }
        }
      }
    }

    // Section Title & Grid Toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "🎨 $selectedCategory (${displayedTemplates.size} Templates)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
      )

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Tap to apply",
          style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
        )

        IconButton(
          onClick = { viewColumns = if (viewColumns == 2) 1 else 2 },
          modifier = Modifier
            .size(32.dp)
            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
        ) {
          Icon(
            if (viewColumns == 2) Icons.Default.ViewAgenda else Icons.Default.GridView,
            contentDescription = "Toggle Grid/List View",
            tint = CyanAccent,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    // Live Template Cards Vertical Grid Layout
    if (displayedTemplates.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(120.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("No templates found in $selectedCategory", color = TextSecondary, fontSize = 12.sp)
      }
    } else {
      Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
        LazyVerticalGrid(
          columns = GridCells.Fixed(viewColumns),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(displayedTemplates, key = { it.id }) { tpl ->
            val isSelected = clip.fontFamily.equals(tpl.fontFamily, true) && clip.textColor == tpl.textColor
            val isFav = tpl.id in favoriteTemplateIds

            LiveAnimatedTemplateCard(
              template = tpl,
              userTextOverride = userTextOverride,
              isSelected = isSelected,
              isFavorite = isFav,
              onToggleFavorite = {
                favoriteTemplateIds = if (isFav) favoriteTemplateIds - tpl.id else favoriteTemplateIds + tpl.id
              },
              onClick = {
                val textToKeep = if (clip.text.isBlank() || clip.text == "Tap to edit" || clip.text == "Your Text Here") {
                  tpl.sampleText
                } else clip.text

                onApplyTemplate(
                  clip.copy(
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
              },
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// LIVE ANIMATED TEMPLATE CARD COMPONENT
// -------------------------------------------------------------
@Composable
private fun LiveAnimatedTemplateCard(
  template: TextTemplateItem,
  userTextOverride: String,
  isSelected: Boolean,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val shape = RoundedCornerShape(14.dp)

  val displayText = if (userTextOverride.isNotBlank() && userTextOverride != "Tap to edit" && userTextOverride != "Your Text Here") {
    userTextOverride
  } else {
    template.sampleText
  }

  val primaryConfig = remember(template, displayText) {
    template.toPrimaryLayerConfig().copy(text = displayText)
  }
  val primaryClip = remember(primaryConfig) {
    primaryConfig.toTextClip(timelineStartMs = 0L, durationMs = 3000L)
  }

  val infiniteTransition = rememberInfiniteTransition(label = "tpl_card_anim_${template.id}_${displayText.hashCode()}")
  val animLoopMs by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 2400f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "tpl_loop_ms"
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .clickable { onClick() },
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1424)),
    border = BorderStroke(
      width = if (isSelected) 2.dp else 1.dp,
      color = if (isSelected) Color(0xFF00C2FF) else Color(0xFF1E293B)
    )
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Live Animated Canvas Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(110.dp)
          .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
          .background(Color(0xFF030712))
      ) {
        // Real Live Animated Vector Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
          val nativeCanvas = drawContext.canvas.nativeCanvas
          val w = size.width.toInt()
          val h = size.height.toInt()
          if (w > 0 && h > 0) {
            TextLayerRenderer.drawTemplatePreview(
              canvas = nativeCanvas,
              clip = primaryClip,
              previewLoopMs = animLoopMs.toLong(),
              width = w,
              height = h,
              context = context
            )
          }
        }

        // Badge Emoji & HD/PRO (Top Left)
        Row(
          modifier = Modifier
            .padding(6.dp)
            .align(Alignment.TopStart),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (template.hdOrPro.equals("PRO", true)) Color(0xFFF59E0B) else Color(0xFF1E3A8A)
          ) {
            Text(
              text = "${template.badgeEmoji} ${template.hdOrPro.uppercase()}",
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = if (template.hdOrPro.equals("PRO", true)) Color.Black else Color.White,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
          }
        }

        // Top Right Row: Cyan Diamond Badge & Favorite Toggle
        Row(
          modifier = Modifier
            .padding(4.dp)
            .align(Alignment.TopEnd),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          if (template.isPremium || template.hdOrPro.equals("PRO", true)) {
            Box(
              modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E5FF)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "◆",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
              )
            }
          }

          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = "Favorite",
              tint = if (isFavorite) Color(0xFFF43F5E) else Color.White.copy(alpha = 0.7f),
              modifier = Modifier.size(16.dp)
            )
          }
        }
      } // End Canvas Box

      // Details Footer
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF0D1424))
          .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = template.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${template.category} • ${template.animationType}",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        if (isSelected) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 2: LIVE FONTS SECTION (Live Vector Canvas Typography)
// -------------------------------------------------------------
@Composable
private fun LiveFontsTabSection(
  clip: TextClip,
  availableFonts: List<FontOption>,
  onSelectFont: (fontId: String, customPath: String?) -> Unit,
  onImportFont: () -> Unit
) {
  var selectedCategory by remember { mutableStateOf("Trending") }
  val context = LocalContext.current

  val displayedFonts = remember(selectedCategory, availableFonts) {
    FontCatalog.getFontsForCategory(selectedCategory, availableFonts)
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // Actions Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Typefaces & Typography",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (selectedCategory == "Brand Fonts") {
          TextButton(
            onClick = {
              FontCatalog.addBrandFont(
                BrandFontPreset(
                  id = "brand_${System.currentTimeMillis()}",
                  name = "Custom Brand Font",
                  fontFamily = clip.fontFamily,
                  customFontPath = clip.customFontPath,
                  defaultColor = clip.textColor,
                  fontWeight = clip.fontWeight
                )
              )
            },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(2.dp))
            Text("Save Brand Font", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }

        TextButton(
          onClick = onImportFont,
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Icon(Icons.Default.FileOpen, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
          Spacer(Modifier.width(2.dp))
          Text("Import TTF/OTF", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // 8 Font Categories
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      contentPadding = PaddingValues(vertical = 2.dp)
    ) {
      items(FontCatalog.FONT_CATEGORIES) { cat ->
        val isSelected = selectedCategory == cat
        FilterChip(
          selected = isSelected,
          onClick = { selectedCategory = cat },
          label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF1E68F6).copy(alpha = 0.3f),
            selectedLabelColor = Color(0xFF00C2FF),
            containerColor = Color(0xFF162032),
            labelColor = TextSecondary
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = Color(0xFF26354F),
            selectedBorderColor = Color(0xFF00C2FF)
          )
        )
      }
    }

    // Fonts Carousel
    if (displayedFonts.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(90.dp),
        contentAlignment = Alignment.Center
      ) {
        if (selectedCategory == "My Fonts") {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No imported fonts yet.", color = TextSecondary, fontSize = 11.sp)
            TextButton(onClick = onImportFont) {
              Text("Tap here to import .ttf or .otf files", color = Color(0xFF00C2FF), fontSize = 11.sp)
            }
          }
        } else {
          Text("No fonts available in $selectedCategory", color = TextSecondary, fontSize = 11.sp)
        }
      }
    } else {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
      ) {
        items(displayedFonts) { fontOpt ->
          val isSelected = clip.fontFamily.equals(fontOpt.id, true) ||
            (clip.customFontPath != null && clip.customFontPath == fontOpt.filePath)

          val fontSampleClip = remember(clip, fontOpt) {
            clip.copy(fontFamily = fontOpt.id, customFontPath = fontOpt.filePath)
          }

          val infiniteTransition = rememberInfiniteTransition(label = "font_anim_${fontOpt.id}")
          val animLoopMs by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2400f,
            animationSpec = infiniteRepeatable(
              animation = tween(durationMillis = 2400, easing = LinearEasing),
              repeatMode = RepeatMode.Restart
            ),
            label = "font_loop_ms"
          )

          Card(
            modifier = Modifier
              .width(160.dp)
              .height(110.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable { onSelectFont(fontOpt.id, fontOpt.filePath) },
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) Color(0xFF1E68F6).copy(alpha = 0.25f) else Color(0xFF162032)
            ),
            border = BorderStroke(
              if (isSelected) 2.dp else 1.dp,
              if (isSelected) Color(0xFF00C2FF) else Color(0xFF26354F)
            )
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
                Text(
                  text = fontOpt.name,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00C2FF), modifier = Modifier.size(14.dp))
                }
              }

              // Live Animated Canvas Rendering for the font
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(65.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF030712))
              ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val nativeCanvas = drawContext.canvas.nativeCanvas
                  val w = size.width.toInt()
                  val h = size.height.toInt()
                  if (w > 0 && h > 0) {
                    TextLayerRenderer.drawTemplatePreview(
                      canvas = nativeCanvas,
                      clip = fontSampleClip,
                      previewLoopMs = animLoopMs.toLong(),
                      width = w,
                      height = h,
                      context = context
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 3: LIVE STYLES SECTION
// -------------------------------------------------------------
@Composable
private fun LiveStylesTabSection(
  clip: TextClip,
  onUpdate: (TextClip) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // Quick Style Presets Row
    Text("Live Style Presets", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
    val stylePresets = listOf(
      "Clean White" to Pair(0xFFFFFFFFL, false),
      "Glow Neon" to Pair(0xFF00E5FFL, true),
      "Gold Luxury" to Pair(0xFFFFD700L, true),
      "Hot Pink" to Pair(0xFFFF007FL, true),
      "Emerald Green" to Pair(0xFF10B981L, false),
      "Cyber Purple" to Pair(0xFF8B5CF6L, true)
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(stylePresets) { (name, props) ->
        val colorHex = props.first
        val glow = props.second
        Surface(
          onClick = {
            onUpdate(
              clip.copy(
                textColor = colorHex,
                hasShadow = glow,
                shadowColor = if (glow) colorHex else 0xFF000000,
                shadowBlur = if (glow) 12f else 4f
              )
            )
          },
          shape = RoundedCornerShape(8.dp),
          color = Color(colorHex).copy(alpha = 0.2f),
          border = BorderStroke(1.dp, Color(colorHex))
        ) {
          Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(colorHex),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }
    }

    // Format Toggles (B, I, U, TT)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = clip.fontWeight >= 700,
          onClick = { onUpdate(clip.copy(fontWeight = if (clip.fontWeight >= 700) 400 else 800)) },
          label = { Text("B", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1E68F6), selectedLabelColor = Color.White)
        )
        FilterChip(
          selected = clip.isItalic,
          onClick = { onUpdate(clip.copy(isItalic = !clip.isItalic)) },
          label = { Text("I", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1E68F6), selectedLabelColor = Color.White)
        )
        FilterChip(
          selected = clip.isUnderline,
          onClick = { onUpdate(clip.copy(isUnderline = !clip.isUnderline)) },
          label = { Text("U", textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1E68F6), selectedLabelColor = Color.White)
        )
        FilterChip(
          selected = clip.isAllCaps,
          onClick = { onUpdate(clip.copy(isAllCaps = !clip.isAllCaps)) },
          label = { Text("TT", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1E68F6), selectedLabelColor = Color.White)
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("Left", "Center", "Right").forEach { align ->
          FilterChip(
            selected = clip.alignment.equals(align, true),
            onClick = { onUpdate(clip.copy(alignment = align)) },
            label = { Text(align, fontSize = 10.sp) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleAccent, selectedLabelColor = Color.White)
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 4: LIVE EFFECTS SECTION
// -------------------------------------------------------------
@Composable
private fun LiveEffectsTabSection(
  clip: TextClip,
  onUpdate: (TextClip) -> Unit
) {
  val context = LocalContext.current

  val effectsList = listOf(
    "None" to Pair("Normal Default", clip.copy(subtitleStyle = "Classic", hasShadow = false, strokeWidth = 0f, hasGradient = false)),
    "Neon Glow" to Pair("Cyber Neon Pulse", clip.copy(subtitleStyle = "Neon Glow", textColor = 0xFF00FFFF, hasShadow = true, shadowColor = 0xFF00FFFF, shadowBlur = 12f, strokeWidth = 2f, strokeColor = 0xFF003366)),
    "3D Extrusion" to Pair("Isometric 3D Extruded", clip.copy(subtitleStyle = "3D Extrusion", hasShadow = true, shadowColor = 0xFF4C1D95, shadowOffsetX = 6f, shadowOffsetY = 6f, strokeWidth = 2.5f, strokeColor = 0xFF1E1035)),
    "Chrome Metal" to Pair("Metallic Mirror Shine", clip.copy(subtitleStyle = "Chrome Metal", hasGradient = true, gradientColorStart = 0xFFE2E8F0, gradientColorEnd = 0xFF64748B, strokeWidth = 2f, strokeColor = 0xFF0F172A)),
    "Comic Pop" to Pair("Retro Pop Art Halftone", clip.copy(subtitleStyle = "Comic Pop", textColor = 0xFFFFEA00, strokeWidth = 4f, strokeColor = 0xFF000000, hasShadow = true, shadowColor = 0xFFFF0055)),
    "Glitch RGB" to Pair("Digital RGB Channel Shift", clip.copy(subtitleStyle = "Glitch RGB", animationType = "Shake", textColor = 0xFF00FFCC, strokeWidth = 2f, strokeColor = 0xFFFF0055)),
    "Curved Arc" to Pair("Circular Curved Arc", clip.copy(subtitleStyle = "Curved Arc", textColor = 0xFFFFD700, strokeWidth = 1f)),
    "Glassmorphism" to Pair("Frosted Translucent Blur", clip.copy(subtitleStyle = "Glassmorphism", hasBackground = true, backgroundColor = 0x88FFFFFF, cornerRadius = 12f, bgPadding = 10f))
  )

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("Artistic Live Text Effects", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(effectsList) { (fxName, fxData) ->
        val fxDesc = fxData.first
        val fxClip = fxData.second
        val isSelected = clip.subtitleStyle.equals(fxName, true) || (fxName == "None" && clip.subtitleStyle == "Classic")

        val infiniteTransition = rememberInfiniteTransition(label = "fx_anim_$fxName")
        val animLoopMs by infiniteTransition.animateFloat(
          initialValue = 0f,
          targetValue = 2400f,
          animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
          ),
          label = "fx_loop_ms"
        )

        Card(
          modifier = Modifier
            .width(150.dp)
            .height(115.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onUpdate(fxClip) },
          colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E68F6).copy(alpha = 0.25f) else Color(0xFF162032)
          ),
          border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF00C2FF) else Color(0xFF26354F))
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = fxName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                maxLines = 1
              )
              if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00C2FF), modifier = Modifier.size(14.dp))
              }
            }

            // Live Canvas Preview
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF030712))
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val nativeCanvas = drawContext.canvas.nativeCanvas
                val w = size.width.toInt()
                val h = size.height.toInt()
                if (w > 0 && h > 0) {
                  TextLayerRenderer.drawTemplatePreview(
                    canvas = nativeCanvas,
                    clip = fxClip,
                    previewLoopMs = animLoopMs.toLong(),
                    width = w,
                    height = h,
                    context = context
                  )
                }
              }
            }

            Text(
              text = fxDesc,
              style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 8.sp),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 5: LIVE ANIMATIONS SECTION
// -------------------------------------------------------------
@Composable
private fun LiveAnimationsTabSection(
  clip: TextClip,
  onUpdate: (TextClip) -> Unit
) {
  var animCategory by remember { mutableStateOf("In") }
  val context = LocalContext.current

  val inAnimations = listOf("Pop", "Fade", "Slide", "Zoom", "Bounce", "Typewriter", "Shake", "Drop", "Flip")
  val outAnimations = listOf("Fade Out", "Slide Down", "Zoom Out", "Pop Out", "Wipe")
  val loopAnimations = listOf("Pulse", "Float", "Shake Loop", "Wave", "Rainbow", "Glitch")

  val currentList = when (animCategory) {
    "In" -> inAnimations
    "Out" -> outAnimations
    else -> loopAnimations
  }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // Category Chips & Duration Display
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("In", "Out", "Loop").forEach { cat ->
          val isSelected = animCategory == cat
          FilterChip(
            selected = isSelected,
            onClick = { animCategory = cat },
            label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF1E68F6),
              selectedLabelColor = Color.White,
              containerColor = Color(0xFF162032)
            )
          )
        }
      }

      Text(
        text = "Duration: ${(clip.animDurationMs / 1000f)}s",
        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF00C2FF), fontWeight = FontWeight.Bold, fontSize = 11.sp)
      )
    }

    // Live Animation Cards Carousel
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(currentList) { anim ->
        val isSelected = clip.animationType.equals(anim, true)
        val animClip = remember(clip, anim) { clip.copy(animationType = anim) }

        val infiniteTransition = rememberInfiniteTransition(label = "anim_preview_$anim")
        val animLoopMs by infiniteTransition.animateFloat(
          initialValue = 0f,
          targetValue = 2400f,
          animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
          ),
          label = "anim_loop_ms"
        )

        Card(
          modifier = Modifier
            .width(135.dp)
            .height(105.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onUpdate(clip.copy(animationType = anim)) },
          colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E68F6).copy(alpha = 0.25f) else Color(0xFF162032)
          ),
          border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF00C2FF) else Color(0xFF26354F))
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = anim,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                maxLines = 1
              )
              if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00C2FF), modifier = Modifier.size(14.dp))
              }
            }

            // Live Canvas Preview
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF030712))
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val nativeCanvas = drawContext.canvas.nativeCanvas
                val w = size.width.toInt()
                val h = size.height.toInt()
                if (w > 0 && h > 0) {
                  TextLayerRenderer.drawTemplatePreview(
                    canvas = nativeCanvas,
                    clip = animClip,
                    previewLoopMs = animLoopMs.toLong(),
                    width = w,
                    height = h,
                    context = context
                  )
                }
              }
            }
          }
        }
      }
    }

    // Animation Speed / Duration Slider
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Speed / Duration", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
      Slider(
        value = clip.animDurationMs.toFloat(),
        onValueChange = { onUpdate(clip.copy(animDurationMs = it.toLong())) },
        valueRange = 100f..3000f,
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 12.dp),
        colors = SliderDefaults.colors(thumbColor = Color(0xFF00C2FF), activeTrackColor = Color(0xFF1E68F6))
      )
    }
  }
}

// -------------------------------------------------------------
// FULLSCREEN LIVE INSPECTION PREVIEW MODAL
// -------------------------------------------------------------
@Composable
private fun TextStudioFullscreenPreviewModal(
  clip: TextClip,
  onDismiss: () -> Unit,
  onApplyToTimeline: () -> Unit
) {
  val context = LocalContext.current

  var isPlaying by remember { mutableStateOf(true) }
  var speedMultiplier by remember { mutableStateOf(1.0f) }
  var replayTrigger by remember { mutableIntStateOf(0) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = Color(0xFF030712)
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        val infiniteTransition = rememberInfiniteTransition(label = "fs_anim_${clip.text.hashCode()}_$replayTrigger")
        val animLoopMs by if (isPlaying) {
          infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2400f,
            animationSpec = infiniteRepeatable(
              animation = tween(durationMillis = 2400, easing = LinearEasing),
              repeatMode = RepeatMode.Restart
            ),
            label = "fs_loop_ms"
          )
        } else {
          remember { mutableFloatStateOf(1200f) }
        }

        // Fullscreen Live Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
          val nativeCanvas = drawContext.canvas.nativeCanvas
          val w = size.width.toInt()
          val h = size.height.toInt()
          if (w > 0 && h > 0) {
            TextLayerRenderer.drawTemplatePreview(
              canvas = nativeCanvas,
              clip = clip,
              previewLoopMs = animLoopMs.toLong(),
              width = w,
              height = h,
              context = context,
              speedMultiplier = speedMultiplier
            )
          }
        }

        // Top Overlay Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp)
            .align(Alignment.TopStart),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.75f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF00E5FF))
              )
              Text(
                text = "FULLSCREEN LIVE INSPECTOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(36.dp)
              .background(Color.Black.copy(alpha = 0.75f), CircleShape)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        // Bottom Controls Bar
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
            .align(Alignment.BottomCenter)
            .background(Color(0xFF0D1424).copy(alpha = 0.9f), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = clip.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Font: ${clip.fontFamily} • Animation: ${clip.animationType}",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }

            // Speed Selector Pills
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf(0.5f, 1.0f, 1.5f, 2.5f).forEach { spd ->
                val isSel = speedMultiplier == spd
                Surface(
                  onClick = { speedMultiplier = spd },
                  shape = RoundedCornerShape(6.dp),
                  color = if (isSel) Color(0xFF00C2FF) else Color(0xFF1E293B)
                ) {
                  Text(
                    text = "${spd}x",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) Color.Black else Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              IconButton(
                onClick = { replayTrigger++ },
                modifier = Modifier
                  .size(38.dp)
                  .background(Color(0xFF1E293B), CircleShape)
              ) {
                Icon(Icons.Default.Replay, contentDescription = "Replay", tint = Color.White)
              }

              IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                  .size(38.dp)
                  .background(Color(0xFF00C2FF), CircleShape)
              ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = Color.Black)
              }
            }

            Button(
              onClick = onApplyToTimeline,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E68F6), contentColor = Color.White),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Apply to Canvas & Timeline", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }
    }
  }
}
