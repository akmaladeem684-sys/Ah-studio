package com.example.ui.components.effects

import android.graphics.ColorFilter
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.EffectClip
import com.example.domain.model.EffectType
import com.example.engine.SelectedTrackElement
import com.example.engine.composition.VideoEffectRenderer
import com.example.ui.StudioViewModel
import com.example.ui.theme.*
import kotlin.math.sin

/**
 * Global holder for Before/After compare preview toggle state.
 * When true, active visual effects are bypassed during rendering.
 */
var isBeforeAfterComparing by mutableStateOf(false)

/**
 * Representation of each effect in the unified library with its category and visual configuration.
 */
data class EffectItem(
  val id: String,
  val name: String,
  val effectType: EffectType?, // null represents "None / Original"
  val category: String = "Trending",
  val tag: String = "", // "PRO", "AI", "NEW", "HOT", ""
  val accentColor: Color = Color(0xFF00C2FF),
  val defaultIntensity: Float = 0.8f
)

object EffectsLibraryCatalog {
  val ALL_EFFECTS: List<EffectItem> = listOf(
    // 0. OFF / ORIGINAL BASELINE VIDEO
    EffectItem("none", "Original", null, "All", "", Color(0xFF64748B), 0.0f),

    // TRENDING & TOP PICKS
    EffectItem("ve_glow", "Glow", EffectType.GLOW, "Trending", "HOT", Color(0xFFF59E0B), 0.8f),
    EffectItem("ve_shake", "Camera Shake", EffectType.SHAKE, "Trending", "HOT", Color(0xFFEF4444), 0.85f),
    EffectItem("ve_zoom", "Zoom Pulse", EffectType.ZOOM, "Trending", "HOT", Color(0xFF8B5CF6), 0.8f),
    EffectItem("ve_vhs", "1998 VHS", EffectType.VHS_VINTAGE, "Trending", "HOT", Color(0xFFF59E0B), 0.8f),
    EffectItem("ve_glitch", "Glitch Scan", EffectType.GLITCH, "Trending", "HOT", Color(0xFFEF4444), 0.85f),
    EffectItem("ve_golden", "Golden Hour", EffectType.GOLDEN_HOUR, "Trending", "HOT", Color(0xFFF59E0B), 0.8f),
    EffectItem("be_wings", "Angel Wings", EffectType.ANGEL_WINGS, "Trending", "AI", Color(0xFFFACC15), 0.85f),
    EffectItem("be_eyes", "Laser Eyes", EffectType.GLOW_EYES, "Trending", "AI", Color(0xFFEF4444), 0.9f),

    // CINEMATIC & LIGHTING
    EffectItem("ve_flare", "Lens Flare", EffectType.LENS_FLARE, "Cinematic", "PRO", Color(0xFFF59E0B), 0.85f),
    EffectItem("ve_leak", "Light Leak", EffectType.LIGHT_LEAK, "Cinematic", "", Color(0xFFFB923C), 0.75f),
    EffectItem("ve_bokeh", "Bokeh", EffectType.BOKEH, "Cinematic", "PRO", Color(0xFFE879F9), 0.75f),
    EffectItem("ve_sparks", "Fire Sparks", EffectType.FIRE_SPARK, "Cinematic", "", Color(0xFFEF4444), 0.85f),
    EffectItem("ve_flash", "White Flash", EffectType.FLASH, "Cinematic", "", Color(0xFFFFFFFF), 0.9f),
    EffectItem("ve_blur", "Blur", EffectType.BLUR, "Cinematic", "", Color(0xFF38BDF8), 0.7f),
    EffectItem("ve_mblur", "Motion Blur", EffectType.MOTION_BLUR, "Cinematic", "", Color(0xFF6366F1), 0.75f),
    EffectItem("ve_vignette", "Vignette", EffectType.VIGNETTE, "Cinematic", "", Color(0xFF64748B), 0.8f),
    EffectItem("ve_vertigo", "Vertigo Dolly", EffectType.VERTIGO_DOLLY, "Cinematic", "PRO", Color(0xFFA855F7), 0.8f),
    EffectItem("ve_ripple", "Shockwave", EffectType.RIPPLE, "Cinematic", "", Color(0xFFF43F5E), 0.85f),

    // GLITCH & RETRO
    EffectItem("ve_rgbsplit", "RGB Split", EffectType.RGB_SPLIT, "Glitch & Retro", "", Color(0xFF3B82F6), 0.8f),
    EffectItem("ve_crt", "CRT Screen", EffectType.CRT_TV, "Glitch & Retro", "", Color(0xFF10B981), 0.75f),
    EffectItem("ve_laser", "Laser Grid", EffectType.LASER_GRID, "Glitch & Retro", "PRO", Color(0xFF22C55E), 0.9f),
    EffectItem("ve_strobe", "RGB Strobe", EffectType.STROBE, "Glitch & Retro", "PRO", Color(0xFF38BDF8), 0.9f),
    EffectItem("ve_warp", "Warp Speed", EffectType.WARP_SPEED, "Glitch & Retro", "PRO", Color(0xFFEC4899), 0.9f),
    EffectItem("ve_mirror", "Mirror", EffectType.MIRROR, "Glitch & Retro", "", Color(0xFFEC4899), 0.8f),
    EffectItem("ve_acid", "Acid Trip", EffectType.ACID_TRIP, "Glitch & Retro", "HOT", Color(0xFFD946EF), 0.85f),

    // BODY & FACE
    EffectItem("be_aura", "Neon Aura", EffectType.BODY_AURA, "Body & Face", "AI", Color(0xFF00C2FF), 0.85f),
    EffectItem("be_outline", "Cyber Outline", EffectType.NEON_OUTLINE, "Body & Face", "AI", Color(0xFF00E5FF), 0.9f),
    EffectItem("be_cwings", "Cyber Wings", EffectType.CYBER_WINGS, "Body & Face", "PRO", Color(0xFF8B5CF6), 0.9f),
    EffectItem("be_fire", "Fire Flame", EffectType.FIRE_AURA, "Body & Face", "AI", Color(0xFFF59E0B), 0.9f),
    EffectItem("be_thor", "Lightning", EffectType.LIGHTNING_BODY, "Body & Face", "AI", Color(0xFF38BDF8), 0.9f),
    EffectItem("be_beauty", "AI Skin Smooth", EffectType.FACE_BEAUTY, "Body & Face", "AI", Color(0xFFF472B6), 0.75f),
    EffectItem("be_xray", "Cyber X-Ray", EffectType.SKELETON_XRAY, "Body & Face", "PRO", Color(0xFF06B6D4), 0.9f),
    EffectItem("be_heart", "Heart Trail", EffectType.HEART_TRAIL, "Body & Face", "", Color(0xFFFF3366), 0.8f),
    EffectItem("be_cface", "Cyber Face HUD", EffectType.CYBER_FACE, "Body & Face", "AI", Color(0xFF00E5FF), 0.85f),

    // AI MAGIC
    EffectItem("ae_cyber", "AI Cyberpunk", EffectType.AI_CYBERPUNK_CITY, "AI Magic", "AI", Color(0xFFEC4899), 0.9f),
    EffectItem("ae_disperse", "Thanos Snap", EffectType.AI_PARTICLE_DISPERSE, "AI Magic", "AI", Color(0xFFF59E0B), 0.95f),
    EffectItem("ae_trail", "Neon Ribbons", EffectType.AI_NEON_TRAIL, "AI Magic", "AI", Color(0xFF38BDF8), 0.9f),
    EffectItem("ae_liquid", "Liquid Gold", EffectType.AI_LIQUID_GOLD, "AI Magic", "PRO", Color(0xFFEAB308), 0.85f),
    EffectItem("ae_freeze", "Time Freeze", EffectType.AI_FREEZE_TIME, "AI Magic", "AI", Color(0xFF06B6D4), 0.9f),
    EffectItem("ae_quantum", "Quantum Glitch", EffectType.AI_GLITCH_REALITY, "AI Magic", "AI", Color(0xFFD946EF), 0.95f),
    EffectItem("ae_portal", "Sci-Fi Portal", EffectType.AI_SCI_FI_PORTAL, "AI Magic", "PRO", Color(0xFF00E5FF), 0.85f),

    // PHOTO & ART
    EffectItem("pe_thermal", "Thermal Heat", EffectType.THERMAL_CAMERA, "Photo & Art", "PRO", Color(0xFFEF4444), 0.85f),
    EffectItem("pe_blueprint", "Blueprint CAD", EffectType.BLUEPRINT_CAD, "Photo & Art", "PRO", Color(0xFF38BDF8), 0.85f),
    EffectItem("pe_charcoal", "Charcoal Sketch", EffectType.CHARCOAL_DRAW, "Photo & Art", "", Color(0xFF94A3B8), 0.8f),
    EffectItem("pe_pastel", "Pastel Dream", EffectType.PASTEL_DREAM, "Photo & Art", "", Color(0xFFF472B6), 0.75f),
    EffectItem("pe_colorpop", "Color Pop", EffectType.COLOR_POP_SPLASH, "Photo & Art", "", Color(0xFFF59E0B), 0.8f),
    EffectItem("pe_manga", "Manga Ink", EffectType.MANGA_LINE, "Photo & Art", "HOT", Color(0xFF000000), 0.85f)
  )
}

@Composable
fun EffectsStudioPanel(
  viewModel: StudioViewModel,
  onDismiss: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()
  val canUndo by viewModel.timelineEngine.canUndo.collectAsState()
  val canRedo by viewModel.timelineEngine.canRedo.collectAsState()

  // Currently active effect clip from timeline
  val currentPosMs by viewModel.timelineEngine.currentPositionMs.collectAsState()
  val selectedClipIds by viewModel.timelineEngine.selectedClipIds.collectAsState()

  // Target video clip currently selected or under playhead
  val selectedVideoClip = remember(timeline.videoClips, selectedElement, selectedClipIds, currentPosMs) {
    val selVidId = (selectedElement as? SelectedTrackElement.Video)?.clipId
      ?: selectedClipIds.firstOrNull { id -> timeline.videoClips.any { it.id == id } }
    if (selVidId != null) {
      timeline.videoClips.find { it.id == selVidId }
    } else {
      timeline.videoClips.find { currentPosMs >= it.timelineStartMs && currentPosMs < it.timelineStartMs + it.durationMs }
        ?: timeline.videoClips.firstOrNull()
    }
  }

  val selectedEffectId = (selectedElement as? SelectedTrackElement.Effect)?.clipId
  val activeEffectClip = remember(timeline.effectClips, selectedVideoClip, selectedEffectId, currentPosMs) {
    if (selectedEffectId != null) {
      timeline.effectClips.find { it.id == selectedEffectId }
    } else if (selectedVideoClip != null) {
      timeline.effectClips.find { it.targetClipId == selectedVideoClip.id }
        ?: timeline.effectClips.find { it.timelineStartMs == selectedVideoClip.timelineStartMs && it.durationMs == selectedVideoClip.durationMs }
    } else {
      timeline.effectClips.find { currentPosMs >= it.timelineStartMs && currentPosMs < it.timelineStartMs + it.durationMs }
        ?: timeline.effectClips.lastOrNull()
    }
  }

  // Determine currently active effect type
  val currentActiveEffectType = activeEffectClip?.effectType

  // Single shared master animation clock for all live thumbnails.
  // Using a single clock eliminates Choreographer loop overhead and guarantees buttery smooth 60fps scrolling!
  val infiniteTransition = rememberInfiniteTransition(label = "effects_master_loop")
  val masterAnimTimeMs by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 3000f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "master_anim_time"
  )
  val animTimeMsProvider = rememberUpdatedState(masterAnimTimeMs.toLong())

  // Categories Filter
  var selectedCategory by remember { mutableStateOf("All") }
  val categories = listOf("All", "Trending", "Cinematic", "Glitch & Retro", "Body & Face", "AI Magic", "Photo & Art")

  val filteredEffects = remember(selectedCategory) {
    if (selectedCategory == "All") {
      EffectsLibraryCatalog.ALL_EFFECTS
    } else {
      val noneItem = EffectsLibraryCatalog.ALL_EFFECTS.first { it.effectType == null }
      val matching = EffectsLibraryCatalog.ALL_EFFECTS.filter { it.category == selectedCategory && it.effectType != null }
      listOf(noneItem) + matching
    }
  }

  // Apply or Remove effect
  val onSelectEffect: (EffectItem) -> Unit = { item ->
    if (item.effectType == null) {
      viewModel.timelineEngine.removeEffectFromCurrentClip()
    } else {
      val created = viewModel.timelineEngine.applyEffectToCurrentClip(
        effectType = item.effectType,
        intensity = item.defaultIntensity,
        customName = item.name
      )
      // Select the applied effect so that the intensity adjustment slider attaches immediately
      viewModel.timelineEngine.selectElement(SelectedTrackElement.Effect(created.id))
    }
    // Refresh video preview immediately
    val curPos = viewModel.timelineEngine.currentPositionMs.value
    viewModel.timelineEngine.seekTo(curPos)
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFF070B14))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // -------------------------------------------------------------
    // TOP HEADER: Title, Before/After Compare, Undo/Redo, Close
    // -------------------------------------------------------------
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E68F6), Color(0xFF00C2FF)))),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Effects Library",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
            )
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFF1E68F6).copy(alpha = 0.25f),
              border = BorderStroke(1.dp, Color(0xFF00C2FF))
            ) {
              Text(
                text = "${EffectsLibraryCatalog.ALL_EFFECTS.size - 1} LIVE FX",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00C2FF),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
              )
            }
          }
          Text(
            text = "Live animated visual previews • Tap to apply",
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextSecondary,
              fontSize = 10.sp
            )
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Undo
        IconButton(
          onClick = { viewModel.timelineEngine.undo() },
          enabled = canUndo,
          modifier = Modifier.size(30.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Undo,
            contentDescription = "Undo",
            tint = if (canUndo) AmberAccent else TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
          )
        }

        // Redo
        IconButton(
          onClick = { viewModel.timelineEngine.redo() },
          enabled = canRedo,
          modifier = Modifier.size(30.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Redo,
            contentDescription = "Redo",
            tint = if (canRedo) AmberAccent else TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
          )
        }

        // BEFORE / AFTER COMPARE BUTTON
        Surface(
          onClick = { isBeforeAfterComparing = !isBeforeAfterComparing },
          shape = RoundedCornerShape(8.dp),
          color = if (isBeforeAfterComparing) Color(0xFFEF4444) else Color(0xFF131826),
          border = BorderStroke(
            1.dp,
            if (isBeforeAfterComparing) Color(0xFFEF4444) else Color(0xFF1E293B)
          )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = if (isBeforeAfterComparing) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = if (isBeforeAfterComparing) "ORIGINAL" else "COMPARE",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }

        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = TextSecondary
          )
        }
      }
    }

    // -------------------------------------------------------------
    // EFFECT INTENSITY SLIDER COMPONENT
    // -------------------------------------------------------------
    EffectIntensitySliderCard(
      activeEffectClip = activeEffectClip,
      targetVideoClipName = selectedVideoClip?.name,
      onIntensityChange = { newIntensity ->
        if (activeEffectClip != null) {
          viewModel.timelineEngine.updateEffectIntensity(activeEffectClip.id, newIntensity)
          val curPos = viewModel.timelineEngine.currentPositionMs.value
          viewModel.timelineEngine.seekTo(curPos)
        }
      },
      onReset = {
        if (activeEffectClip != null) {
          viewModel.timelineEngine.updateEffectIntensity(activeEffectClip.id, 0.8f)
          val curPos = viewModel.timelineEngine.currentPositionMs.value
          viewModel.timelineEngine.seekTo(curPos)
        }
      },
      onRemove = {
        if (activeEffectClip != null) {
          viewModel.timelineEngine.deleteEffectClip(activeEffectClip.id)
          val curPos = viewModel.timelineEngine.currentPositionMs.value
          viewModel.timelineEngine.seekTo(curPos)
        }
      }
    )

    // -------------------------------------------------------------
    // CATEGORY FILTER CHIPS ROW
    // -------------------------------------------------------------
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(categories) { cat ->
        val isCatSelected = cat == selectedCategory
        Surface(
          onClick = { selectedCategory = cat },
          shape = RoundedCornerShape(16.dp),
          color = if (isCatSelected) Color(0xFF00C2FF).copy(alpha = 0.22f) else Color(0xFF131826),
          border = BorderStroke(
            1.dp,
            if (isCatSelected) Color(0xFF00C2FF) else Color(0xFF1E293B)
          )
        ) {
          Text(
            text = cat,
            fontSize = 11.sp,
            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isCatSelected) Color(0xFF00C2FF) else TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }
    }

    // -------------------------------------------------------------
    // UNIFIED CLEAN GRID OF ALL EFFECTS (3 COLUMNS)
    // -------------------------------------------------------------
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(300.dp)
    ) {
      LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(
          items = filteredEffects,
          key = { it.id }
        ) { item ->
          val isSelected = if (item.effectType == null) {
            currentActiveEffectType == null
          } else {
            currentActiveEffectType == item.effectType
          }

          EffectThumbnailGridCard(
            item = item,
            isSelected = isSelected,
            animTimeMsProvider = { animTimeMsProvider.value },
            onClick = { onSelectEffect(item) }
          )
        }
      }
    }
  }
}

/**
 * Dedicated slider UI component that allows users to adjust the intensity of the currently applied effect on the selected video clip.
 */
@Composable
fun EffectIntensitySliderCard(
  activeEffectClip: EffectClip?,
  targetVideoClipName: String?,
  onIntensityChange: (Float) -> Unit,
  onReset: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier
) {
  val hasActiveEffect = activeEffectClip != null
  val currentIntensity = activeEffectClip?.intensity ?: 0.8f
  val effectTitle = activeEffectClip?.customName?.ifBlank {
    activeEffectClip.effectType.displayName
  } ?: "No Effect"

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF0D1424),
    border = BorderStroke(
      width = if (hasActiveEffect) 1.5.dp else 1.dp,
      color = if (hasActiveEffect) Color(0xFF00C2FF).copy(alpha = 0.5f) else Color(0xFF1E293B)
    ),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // 1. Header: Effect Name, Target Clip, Intensity Badge & Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(
                if (hasActiveEffect) Color(0xFF00C2FF).copy(alpha = 0.15f) else Color(0xFF1E293B)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Effect Controls",
              tint = if (hasActiveEffect) Color(0xFF00C2FF) else TextSecondary,
              modifier = Modifier.size(16.dp)
            )
          }

          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = if (hasActiveEffect) effectTitle else "Effect Intensity",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (hasActiveEffect) TextPrimary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (hasActiveEffect) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xFF00C2FF).copy(alpha = 0.15f)
                ) {
                  Text(
                    text = "ACTIVE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00C2FF),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }
            }
            Text(
              text = if (hasActiveEffect) {
                targetVideoClipName?.let { "Target: $it" } ?: "Selected Clip"
              } else {
                "Select any effect below to adjust intensity"
              },
              fontSize = 10.sp,
              color = TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Percentage Readout Badge & Action Buttons
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (hasActiveEffect) Color(0xFF00C2FF).copy(alpha = 0.15f) else Color(0xFF131826),
            border = BorderStroke(
              1.dp,
              if (hasActiveEffect) Color(0xFF00C2FF).copy(alpha = 0.4f) else Color(0xFF1E293B)
            )
          ) {
            Text(
              text = if (hasActiveEffect) "${(currentIntensity * 100).toInt()}%" else "--",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (hasActiveEffect) Color(0xFF00C2FF) else TextSecondary,
              modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("effect_intensity_value")
            )
          }

          if (hasActiveEffect) {
            // Reset to default (80%)
            IconButton(
              onClick = onReset,
              modifier = Modifier.size(26.dp)
            ) {
              Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "Reset Intensity",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
              )
            }

            // Remove Effect
            IconButton(
              onClick = onRemove,
              modifier = Modifier.size(26.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove Effect",
                tint = RedAccent,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      // 2. Continuous Intensity Slider Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "0%",
          fontSize = 10.sp,
          fontWeight = FontWeight.Medium,
          color = TextSecondary.copy(alpha = 0.6f)
        )

        Slider(
          value = currentIntensity,
          onValueChange = { if (hasActiveEffect) onIntensityChange(it) },
          enabled = hasActiveEffect,
          valueRange = 0.0f..1.0f,
          modifier = Modifier
            .weight(1f)
            .testTag("effect_intensity_slider"),
          colors = SliderDefaults.colors(
            thumbColor = if (hasActiveEffect) Color(0xFF00C2FF) else Color(0xFF64748B),
            activeTrackColor = if (hasActiveEffect) Color(0xFF00C2FF) else Color(0xFF334155),
            inactiveTrackColor = Color(0xFF1E293B),
            disabledThumbColor = Color(0xFF334155),
            disabledActiveTrackColor = Color(0xFF1E293B),
            disabledInactiveTrackColor = Color(0xFF0D1424)
          )
        )

        Text(
          text = "100%",
          fontSize = 10.sp,
          fontWeight = FontWeight.Medium,
          color = TextSecondary.copy(alpha = 0.6f)
        )
      }

      // 3. Quick Preset Intensity Buttons (25%, 50%, 75%, 100%)
      if (hasActiveEffect) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          listOf(0.25f, 0.50f, 0.75f, 1.00f).forEach { preset ->
            val isPresetSelected = kotlin.math.abs(currentIntensity - preset) < 0.02f
            Surface(
              onClick = { onIntensityChange(preset) },
              shape = RoundedCornerShape(6.dp),
              color = if (isPresetSelected) Color(0xFF00C2FF).copy(alpha = 0.2f) else Color(0xFF131826),
              border = BorderStroke(
                1.dp,
                if (isPresetSelected) Color(0xFF00C2FF) else Color(0xFF1E293B)
              ),
              modifier = Modifier.weight(1f)
            ) {
              Text(
                text = "${(preset * 100).toInt()}%",
                fontSize = 10.sp,
                fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isPresetSelected) Color(0xFF00C2FF) else TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Compact rounded thumbnail card for each effect with live visual preview canvas and clear selection state.
 */
@Composable
private fun EffectThumbnailGridCard(
  item: EffectItem,
  isSelected: Boolean,
  animTimeMsProvider: () -> Long,
  onClick: () -> Unit
) {
  val shape = RoundedCornerShape(12.dp)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    // Thumbnail Box
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1.05f)
        .clip(shape)
        .background(Color(0xFF0D1424))
        .border(
          width = if (isSelected) 2.5.dp else 1.dp,
          color = if (isSelected) Color(0xFF00C2FF) else Color(0xFF1E293B),
          shape = shape
        )
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple(color = Color(0xFF00C2FF))
        ) { onClick() }
        .testTag("effect_card_${item.id}")
    ) {
      // Live Video Thumbnail Canvas showing the actual visual appearance of the effect on video
      LiveEffectThumbnailView(
        item = item,
        animTimeMsProvider = animTimeMsProvider,
        modifier = Modifier.fillMaxSize()
      )

      // Top Left Tag Badge (PRO, AI, HOT, etc.)
      if (item.tag.isNotEmpty()) {
        val tagBg = when (item.tag) {
          "AI" -> Color(0xFF00C2FF)
          "PRO" -> Color(0xFF8B5CF6)
          "HOT" -> Color(0xFFEF4444)
          else -> Color(0xFF10B981)
        }
        Surface(
          shape = RoundedCornerShape(bottomEnd = 6.dp, topStart = 10.dp),
          color = tagBg,
          modifier = Modifier.align(Alignment.TopStart)
        ) {
          Text(
            text = item.tag,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }
      }

      // "ORIGINAL" pill badge on the None item
      if (item.effectType == null) {
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = Color.Black.copy(alpha = 0.65f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 6.dp)
        ) {
          Text(
            text = "ORIGINAL",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
          )
        }
      }

      // Selected Indicator & Glow Scrim
      if (isSelected) {
        // Glowing cyan border tint
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(
                  Color(0xFF00C2FF).copy(alpha = 0.15f),
                  Color.Transparent,
                  Color(0xFF00C2FF).copy(alpha = 0.25f)
                )
              )
            )
        )

        // Top Right Checkmark Circle Badge
        Box(
          modifier = Modifier
            .padding(5.dp)
            .size(20.dp)
            .clip(CircleShape)
            .background(Color(0xFF00C2FF))
            .align(Alignment.TopEnd),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected Effect",
            tint = Color.Black,
            modifier = Modifier.size(13.dp)
          )
        }
      }
    }

    // Effect Name Below Thumbnail
    Text(
      text = item.name,
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      color = if (isSelected) Color(0xFF00C2FF) else TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )
  }
}

/**
 * Live thumbnail Canvas that demonstrates how the effect will look on video.
 * Uses the exact same effect configuration and renderer methods as the editor video player.
 */
@Composable
private fun LiveEffectThumbnailView(
  item: EffectItem,
  animTimeMsProvider: () -> Long,
  modifier: Modifier = Modifier
) {
  val previewClip = remember(item.effectType, item.defaultIntensity) {
    item.effectType?.let { effType ->
      EffectClip(
        id = "preview_${item.id}",
        effectType = effType,
        intensity = item.defaultIntensity,
        timelineStartMs = 0L,
        durationMs = 3000L
      )
    }
  }

  val activeEffectsList = remember(previewClip) {
    if (previewClip != null) listOf(previewClip) else emptyList()
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    drawIntoCanvas { composeCanvas ->
      val canvas = composeCanvas.nativeCanvas
      val w = size.width
      val h = size.height
      if (w <= 0f || h <= 0f) return@drawIntoCanvas

      val cx = w / 2f
      val cy = h / 2f
      val currentRelTime = (animTimeMsProvider() % 3000L)

      if (previewClip == null) {
        // "None / Original" - pristine reference video clip with zero effects
        LiveThumbnailVideoScene.drawSampleVideoFrame(
          canvas = canvas,
          w = w,
          h = h,
          cx = cx,
          cy = cy,
          relTime = currentRelTime,
          colorFilter = null
        )
      } else {
        // 1. Calculate accumulated motion transform using the EXACT video engine logic
        val motion = VideoEffectRenderer.calculateMotionTransform(activeEffectsList, currentRelTime)
        val hasMotion = motion.scaleX != 1f || motion.scaleY != 1f || motion.rotation != 0f ||
                        motion.translationX != 0f || motion.translationY != 0f

        if (hasMotion) {
          canvas.save()
          canvas.translate(cx + motion.translationX * w, cy + motion.translationY * h)
          canvas.rotate(motion.rotation)
          canvas.scale(motion.scaleX, motion.scaleY)
          canvas.translate(-cx, -cy)
        }

        // 2. Calculate ColorMatrix using the EXACT video engine logic
        val effectColorMat = VideoEffectRenderer.calculateEffectColorMatrix(activeEffectsList, currentRelTime)
        val colorFilter: ColorFilter? = if (effectColorMat != null) ColorMatrixColorFilter(effectColorMat) else null

        // 3. Draw the sample video frame with active color grading / filter
        LiveThumbnailVideoScene.drawSampleVideoFrame(
          canvas = canvas,
          w = w,
          h = h,
          cx = cx,
          cy = cy,
          relTime = currentRelTime,
          colorFilter = colorFilter
        )

        // 4. Render the procedural visual effect using the EXACT engine renderer
        VideoEffectRenderer.renderSingleEffect(
          canvas = canvas,
          effect = previewClip,
          intensity = item.defaultIntensity,
          relTime = currentRelTime,
          width = w.toInt(),
          height = h.toInt()
        )

        // 5. If blur or soft focus, add soft diffusion overlay
        if (item.effectType == EffectType.BLUR || item.effectType == EffectType.MOTION_BLUR || item.effectType == EffectType.SOFT_FOCUS) {
          LiveThumbnailVideoScene.drawBlurOverlay(canvas, w, h, item.defaultIntensity)
        }

        if (hasMotion) {
          canvas.restore()
        }
      }
    }
  }
}

/**
 * Realistic miniature video scene renderer drawing a stylized video sample frame.
 * Features a cinematic twilight sky with subtle ambient drift, horizon glow, and a foreground
 * portrait silhouette with neck and shoulders, precisely aligned so body auras, laser eyes,
 * angel wings, lighting flares, glitch scanlines, and particle effects wrap and interact realistically.
 */
private object LiveThumbnailVideoScene {
  private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val subjectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = 2f
  }
  private val diffusePaint = Paint(Paint.ANTI_ALIAS_FLAG)

  fun drawSampleVideoFrame(
    canvas: android.graphics.Canvas,
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    relTime: Long = 0L,
    colorFilter: ColorFilter? = null
  ) {
    // 1. Cinematic Background Gradient (sky & stage lighting with subtle looping ambient drift)
    val driftY = sin(relTime * 0.0018f) * (h * 0.04f)
    val bgShader = LinearGradient(
      0f, -driftY, 0f, h + driftY,
      intArrayOf(
        android.graphics.Color.rgb(20, 24, 46),   // Midnight Navy
        android.graphics.Color.rgb(42, 38, 72),   // Twilight Indigo
        android.graphics.Color.rgb(180, 84, 52),  // Golden Sunset Horizon
        android.graphics.Color.rgb(32, 20, 42)    // Studio Base
      ),
      floatArrayOf(0f, 0.40f, 0.70f, 1f),
      Shader.TileMode.CLAMP
    )
    bgPaint.shader = bgShader
    bgPaint.colorFilter = colorFilter
    canvas.drawRect(0f, 0f, w, h, bgPaint)

    // 2. Horizon / Stage light band
    horizonPaint.colorFilter = colorFilter
    horizonPaint.color = android.graphics.Color.argb(55, 255, 175, 95)
    canvas.drawRect(0f, h * 0.69f, w, h * 0.715f, horizonPaint)

    // 3. Foreground Subject (Stylized video portrait silhouette)
    // Head centered around (cx, cy - h * 0.09f) to match body & facial effect anchors
    subjectPaint.colorFilter = colorFilter
    subjectPaint.color = android.graphics.Color.rgb(12, 15, 24)

    val headRadius = w * 0.14f
    val headCenterY = cy - h * 0.09f
    canvas.drawCircle(cx, headCenterY, headRadius, subjectPaint)

    // Shoulders and Torso Path
    val path = Path()
    val neckWidth = w * 0.065f
    val neckBottomY = cy + h * 0.015f
    path.moveTo(cx - neckWidth, headCenterY + headRadius * 0.65f)
    path.lineTo(cx - neckWidth, neckBottomY)
    path.cubicTo(
      cx - w * 0.15f, cy + h * 0.045f,
      cx - w * 0.30f, cy + h * 0.12f,
      cx - w * 0.35f, h
    )
    path.lineTo(cx + w * 0.35f, h)
    path.cubicTo(
      cx + w * 0.30f, cy + h * 0.12f,
      cx + w * 0.15f, cy + h * 0.045f,
      cx + neckWidth, neckBottomY
    )
    path.lineTo(cx + neckWidth, headCenterY + headRadius * 0.65f)
    path.close()
    canvas.drawPath(path, subjectPaint)

    // Subtle edge rim light separating silhouette from backdrop
    rimPaint.colorFilter = colorFilter
    rimPaint.color = android.graphics.Color.argb(80, 255, 195, 140)
    canvas.drawPath(path, rimPaint)
    canvas.drawCircle(cx, headCenterY, headRadius, rimPaint)
  }

  fun drawBlurOverlay(
    canvas: android.graphics.Canvas,
    w: Float,
    h: Float,
    intensity: Float
  ) {
    diffusePaint.shader = RadialGradient(
      w / 2f, h / 2f, w * 0.6f,
      intArrayOf(
        android.graphics.Color.argb((intensity * 140).toInt().coerceIn(0, 255), 220, 240, 255),
        android.graphics.Color.argb((intensity * 70).toInt().coerceIn(0, 255), 180, 200, 240)
      ),
      floatArrayOf(0f, 1f),
      Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, w, h, diffusePaint)
  }
}
