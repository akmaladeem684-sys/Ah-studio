package com.example.ui.components.effects

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.EffectClip
import com.example.domain.model.EffectType
import com.example.engine.SelectedTrackElement
import com.example.ui.StudioViewModel
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Global holder for Before/After compare preview toggle state.
 * When true, active visual effects are bypassed during rendering.
 */
var isBeforeAfterComparing by mutableStateOf(false)

/**
 * Representation of each effect in the unified library.
 */
data class EffectItem(
  val id: String,
  val name: String,
  val effectType: EffectType?, // null represents "None / Original"
  val emoji: String = "✨",
  val tag: String = "", // "PRO", "AI", "NEW", "HOT", ""
  val accentColor: Color = Color(0xFF00C2FF),
  val defaultIntensity: Float = 0.8f
)

object EffectsLibraryCatalog {
  val ALL_EFFECTS: List<EffectItem> = listOf(
    // 0. OFF / NONE
    EffectItem("none", "None", null, "🚫", "", Color(0xFF64748B), 0.0f),

    // BASIC & CINEMATIC EFFECTS
    EffectItem("ve_blur", "Blur", EffectType.BLUR, "🌫️", "", Color(0xFF38BDF8), 0.7f),
    EffectItem("ve_glow", "Glow", EffectType.GLOW, "✨", "HOT", Color(0xFFF59E0B), 0.8f),
    EffectItem("ve_shake", "Camera Shake", EffectType.SHAKE, "📳", "HOT", Color(0xFFEF4444), 0.85f),
    EffectItem("ve_zoom", "Zoom Pulse", EffectType.ZOOM, "🔍", "HOT", Color(0xFF8B5CF6), 0.8f),
    EffectItem("ve_flash", "White Flash", EffectType.FLASH, "⚡", "", Color(0xFFFFFFFF), 0.9f),
    EffectItem("ve_glitch", "Glitch Scan", EffectType.GLITCH, "👾", "HOT", Color(0xFFEF4444), 0.85f),
    EffectItem("ve_rgbsplit", "RGB Split", EffectType.RGB_SPLIT, "🔴", "", Color(0xFF3B82F6), 0.8f),
    EffectItem("ve_vhs", "1998 VHS", EffectType.VHS_VINTAGE, "📼", "HOT", Color(0xFFF59E0B), 0.8f),
    EffectItem("ve_mblur", "Motion Blur", EffectType.MOTION_BLUR, "💨", "", Color(0xFF6366F1), 0.75f),
    EffectItem("ve_sharpen", "Sharpen", EffectType.SHARPEN, "🗡️", "", Color(0xFF10B981), 0.6f),
    EffectItem("ve_vignette", "Vignette", EffectType.VIGNETTE, "🌑", "", Color(0xFF64748B), 0.8f),
    EffectItem("ve_flare", "Lens Flare", EffectType.LENS_FLARE, "☀️", "PRO", Color(0xFFF59E0B), 0.85f),
    EffectItem("ve_leak", "Light Leak", EffectType.LIGHT_LEAK, "🌅", "", Color(0xFFFB923C), 0.75f),
    EffectItem("ve_bokeh", "Bokeh", EffectType.BOKEH, "🔮", "PRO", Color(0xFFE879F9), 0.7f),
    EffectItem("ve_golden", "Golden Hour", EffectType.GOLDEN_HOUR, "🌅", "HOT", Color(0xFFF59E0B), 0.8f),
    EffectItem("ve_sparks", "Fire Sparks", EffectType.FIRE_SPARK, "💥", "", Color(0xFFEF4444), 0.85f),
    EffectItem("ve_laser", "Laser Grid", EffectType.LASER_GRID, "📟", "PRO", Color(0xFF22C55E), 0.9f),
    EffectItem("ve_strobe", "RGB Strobe", EffectType.STROBE, "🚨", "PRO", Color(0xFF38BDF8), 0.9f),
    EffectItem("ve_warp", "Warp Speed", EffectType.WARP_SPEED, "🚀", "PRO", Color(0xFFEC4899), 0.9f),
    EffectItem("ve_vertigo", "Vertigo Dolly", EffectType.VERTIGO_DOLLY, "🌀", "PRO", Color(0xFFA855F7), 0.8f),
    EffectItem("ve_ripple", "Shockwave", EffectType.RIPPLE, "🔘", "", Color(0xFFF43F5E), 0.85f),
    EffectItem("ve_crt", "CRT Screen", EffectType.CRT_TV, "📺", "", Color(0xFF10B981), 0.75f),
    EffectItem("ve_mirror", "Mirror", EffectType.MIRROR, "🪞", "", Color(0xFFEC4899), 0.8f),

    // ADVANCED BODY & AI EFFECTS
    EffectItem("be_aura", "Neon Aura", EffectType.BODY_AURA, "🧘", "AI", Color(0xFF00C2FF), 0.85f),
    EffectItem("be_outline", "Cyber Outline", EffectType.NEON_OUTLINE, "✍️", "AI", Color(0xFF00E5FF), 0.9f),
    EffectItem("be_eyes", "Laser Eyes", EffectType.GLOW_EYES, "👀", "AI", Color(0xFFEF4444), 0.9f),
    EffectItem("be_wings", "Angel Wings", EffectType.ANGEL_WINGS, "🪽", "AI", Color(0xFFFACC15), 0.85f),
    EffectItem("be_cwings", "Cyber Wings", EffectType.CYBER_WINGS, "🤖", "PRO", Color(0xFF8B5CF6), 0.9f),
    EffectItem("be_fire", "Fire Flame", EffectType.FIRE_AURA, "🔥", "AI", Color(0xFFF59E0B), 0.9f),
    EffectItem("be_thor", "Lightning", EffectType.LIGHTNING_BODY, "🌩️", "AI", Color(0xFF38BDF8), 0.9f),
    EffectItem("be_beauty", "AI Skin Smooth", EffectType.FACE_BEAUTY, "✨", "AI", Color(0xFFF472B6), 0.75f),
    EffectItem("be_xray", "Cyber X-Ray", EffectType.SKELETON_XRAY, "💀", "PRO", Color(0xFF06B6D4), 0.9f),
    EffectItem("be_ghost", "Ghost Echo", EffectType.GHOST_CLONE, "👻", "AI", Color(0xFFA855F7), 0.8f),

    // ADVANCED AI GENERATIVE EFFECTS
    EffectItem("ae_cyber", "AI Cyberpunk", EffectType.AI_CYBERPUNK_CITY, "⚡", "AI", Color(0xFFEC4899), 0.9f),
    EffectItem("ae_disperse", "Thanos Snap", EffectType.AI_PARTICLE_DISPERSE, "🌌", "AI", Color(0xFFF59E0B), 0.95f),
    EffectItem("ae_manga", "AI Anime", EffectType.AI_MANGA_UNIVERSE, "🎎", "AI", Color(0xFFF43F5E), 0.85f),
    EffectItem("ae_ghibli", "AI Ghibli", EffectType.AI_ANIME_WORLD, "🍃", "AI", Color(0xFF10B981), 0.9f),
    EffectItem("ae_trail", "Neon Ribbons", EffectType.AI_NEON_TRAIL, "⚡", "AI", Color(0xFF38BDF8), 0.9f),
    EffectItem("ae_liquid", "Liquid Gold", EffectType.AI_LIQUID_GOLD, "🧈", "PRO", Color(0xFFEAB308), 0.85f),
    EffectItem("ae_freeze", "Time Freeze", EffectType.AI_FREEZE_TIME, "⏱️", "AI", Color(0xFF06B6D4), 0.9f),
    EffectItem("ae_quantum", "Quantum Glitch", EffectType.AI_GLITCH_REALITY, "⚛️", "AI", Color(0xFFD946EF), 0.95f)
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
  val selectedEffectId = (selectedElement as? SelectedTrackElement.Effect)?.clipId
  val activeEffectClip = remember(timeline.effectClips, selectedEffectId) {
    timeline.effectClips.find { it.id == selectedEffectId } ?: timeline.effectClips.lastOrNull()
  }

  // Determine currently active effect type
  val currentActiveEffectType = activeEffectClip?.effectType

  // Apply or Remove effect
  val onSelectEffect: (EffectItem) -> Unit = { item ->
    if (item.effectType == null) {
      // Remove current active effect clip if "None" selected
      if (activeEffectClip != null) {
        viewModel.timelineEngine.deleteEffectClip(activeEffectClip.id)
      }
    } else {
      if (activeEffectClip != null) {
        // Update existing clip to new effect type
        viewModel.timelineEngine.updateEffectClip(
          activeEffectClip.copy(
            effectType = item.effectType,
            customName = item.name,
            intensity = item.defaultIntensity
          )
        )
      } else {
        // Create new effect clip on timeline
        val newClip = viewModel.timelineEngine.addEffectClip(item.effectType)
        viewModel.timelineEngine.updateEffectClip(
          newClip.copy(
            intensity = item.defaultIntensity,
            customName = item.name
          )
        )
        viewModel.timelineEngine.selectElement(SelectedTrackElement.Effect(newClip.id))
      }
    }
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
                text = "${EffectsLibraryCatalog.ALL_EFFECTS.size - 1} FX",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00C2FF),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
              )
            }
          }
          Text(
            text = "Tap any effect for real-time live preview",
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
    // ACTIVE EFFECT INTENSITY ADJUSTMENT BAR (When effect applied)
    // -------------------------------------------------------------
    AnimatedVisibility(
      visible = activeEffectClip != null,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      if (activeEffectClip != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF0D1424),
          border = BorderStroke(1.dp, Color(0xFF1E68F6).copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "Intensity",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )

            Slider(
              value = activeEffectClip.intensity,
              onValueChange = {
                viewModel.timelineEngine.updateEffectIntensity(activeEffectClip.id, it)
              },
              valueRange = 0.0f..1.0f,
              modifier = Modifier.weight(1f),
              colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00C2FF),
                activeTrackColor = Color(0xFF1E68F6),
                inactiveTrackColor = Color(0xFF1E293B)
              )
            )

            Text(
              text = "${(activeEffectClip.intensity * 100).toInt()}%",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF00C2FF)
            )

            IconButton(
              onClick = { viewModel.timelineEngine.deleteEffectClip(activeEffectClip.id) },
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
    }

    // -------------------------------------------------------------
    // UNIFIED CLEAN GRID OF ALL EFFECTS (3 COLUMNS)
    // -------------------------------------------------------------
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(310.dp)
    ) {
      LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(
          items = EffectsLibraryCatalog.ALL_EFFECTS,
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
            onClick = { onSelectEffect(item) }
          )
        }
      }
    }
  }
}

/**
 * Compact rounded thumbnail card for each effect with live preview canvas animation and clear selection state.
 */
@Composable
private fun EffectThumbnailGridCard(
  item: EffectItem,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val shape = RoundedCornerShape(12.dp)

  // Infinite transition driving the thumbnail animation
  val infiniteTransition = rememberInfiniteTransition(label = "fx_thumb_${item.id}")
  val animTime by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 6.28f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "anim_val"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    // Thumbnail Box
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1.1f)
        .clip(shape)
        .background(
          if (isSelected) {
            Brush.verticalGradient(
              listOf(Color(0xFF00C2FF).copy(alpha = 0.25f), Color(0xFF1E68F6).copy(alpha = 0.35f))
            )
          } else {
            SolidColor(Color(0xFF0D1424))
          }
        )
        .border(
          width = if (isSelected) 2.dp else 1.dp,
          color = if (isSelected) Color(0xFF00C2FF) else Color(0xFF1E293B),
          shape = shape
        )
        .clickable { onClick() }
    ) {
      if (item.effectType == null) {
        // "None / Off" Thumbnail
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Block,
            contentDescription = "Off",
            tint = if (isSelected) Color(0xFF00C2FF) else Color(0xFF64748B),
            modifier = Modifier.size(28.dp)
          )
        }
      } else {
        // Live Preview Canvas for Effect
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width
          val h = size.height
          val cx = w / 2f
          val cy = h / 2f

          drawEffectThumbnailGraphics(
            effectType = item.effectType,
            animTime = animTime,
            accentColor = item.accentColor,
            cx = cx,
            cy = cy,
            w = w,
            h = h
          )
        }

        // Emoji Overlay Centered
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .align(Alignment.Center),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = item.emoji,
            fontSize = 12.sp
          )
        }
      }

      // Top Left Tag Badge (PRO, AI, HOT, etc.)
      if (item.tag.isNotEmpty()) {
        val tagBg = when (item.tag) {
          "AI" -> Color(0xFF00C2FF)
          "PRO" -> Color(0xFF8B5CF6)
          "HOT" -> Color(0xFFEF4444)
          else -> Color(0xFF10B981)
        }
        Surface(
          shape = RoundedCornerShape(bottomEnd = 6.dp, topStart = 12.dp),
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

      // Selected Checkmark Badge on Top Right
      if (isSelected) {
        Box(
          modifier = Modifier
            .padding(4.dp)
            .size(18.dp)
            .clip(CircleShape)
            .background(Color(0xFF00C2FF))
            .align(Alignment.TopEnd),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected",
            tint = Color.Black,
            modifier = Modifier.size(12.dp)
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
 * Custom Canvas renderer drawing live animated graphics representing each effect type in its thumbnail.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEffectThumbnailGraphics(
  effectType: EffectType,
  animTime: Float,
  accentColor: Color,
  cx: Float,
  cy: Float,
  w: Float,
  h: Float
) {
  when (effectType) {
    EffectType.BLUR, EffectType.SOFT_FOCUS -> {
      val radius = (minOf(w, h) * 0.28f) + (sin(animTime) * 4.dp.toPx())
      drawCircle(color = accentColor.copy(alpha = 0.35f), radius = radius, center = Offset(cx, cy))
      drawCircle(color = accentColor.copy(alpha = 0.15f), radius = radius * 1.5f, center = Offset(cx, cy))
    }
    EffectType.GLOW, EffectType.HALO_GLOW, EffectType.GOLDEN_HOUR -> {
      val radius = (minOf(w, h) * 0.25f) + (sin(animTime) * 6.dp.toPx())
      drawCircle(color = accentColor.copy(alpha = 0.5f), radius = radius, center = Offset(cx, cy))
      drawCircle(color = Color.White.copy(alpha = 0.8f), radius = radius * 0.4f, center = Offset(cx, cy))
    }
    EffectType.SHAKE, EffectType.VERTIGO_DOLLY -> {
      val offsetX = sin(animTime * 3) * 6.dp.toPx()
      val offsetY = cos(animTime * 3) * 4.dp.toPx()
      drawRect(
        color = accentColor.copy(alpha = 0.3f),
        topLeft = Offset(w * 0.2f + offsetX, h * 0.2f + offsetY),
        size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.6f),
        style = Stroke(width = 2.dp.toPx())
      )
    }
    EffectType.ZOOM, EffectType.SKATER_ZOOM -> {
      val scale = 0.2f + (sin(animTime) + 1f) * 0.2f
      drawCircle(color = accentColor.copy(alpha = 0.4f), radius = w * scale, center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))
      drawCircle(color = accentColor.copy(alpha = 0.2f), radius = w * scale * 1.5f, center = Offset(cx, cy), style = Stroke(width = 1.dp.toPx()))
    }
    EffectType.FLASH, EffectType.STROBE -> {
      val alpha = 0.2f + (sin(animTime * 4) + 1f) * 0.35f
      drawRect(color = Color.White.copy(alpha = alpha))
    }
    EffectType.GLITCH, EffectType.RGB_SPLIT -> {
      val shift = sin(animTime * 5) * 5.dp.toPx()
      drawRect(color = Color.Red.copy(alpha = 0.4f), topLeft = Offset(cx - 15.dp.toPx() + shift, cy - 10.dp.toPx()), size = androidx.compose.ui.geometry.Size(30.dp.toPx(), 20.dp.toPx()))
      drawRect(color = Color.Cyan.copy(alpha = 0.4f), topLeft = Offset(cx - 15.dp.toPx() - shift, cy - 10.dp.toPx()), size = androidx.compose.ui.geometry.Size(30.dp.toPx(), 20.dp.toPx()))
    }
    EffectType.LENS_FLARE, EffectType.LIGHT_LEAK, EffectType.SOLAR_FLARE -> {
      val flareX = cx + cos(animTime) * (w * 0.3f)
      drawLine(color = accentColor.copy(alpha = 0.7f), start = Offset(0f, cy), end = Offset(w, cy), strokeWidth = 2.dp.toPx())
      drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(flareX, cy))
    }
    EffectType.BODY_AURA, EffectType.NEON_OUTLINE, EffectType.FIRE_AURA -> {
      val auraR = (minOf(w, h) * 0.35f) + (sin(animTime * 2) * 4.dp.toPx())
      drawCircle(color = accentColor.copy(alpha = 0.35f), radius = auraR, center = Offset(cx, cy), style = Stroke(width = 3.dp.toPx()))
      val pX = cx + cos(animTime * 3) * auraR
      val pY = cy + sin(animTime * 3) * auraR
      drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(pX, pY))
    }
    EffectType.AI_CYBERPUNK_CITY, EffectType.AI_NEON_TRAIL, EffectType.LASER_GRID -> {
      val lineY = (sin(animTime) + 1f) * 0.5f * h
      drawLine(color = accentColor, start = Offset(0f, lineY), end = Offset(w, lineY), strokeWidth = 2.dp.toPx())
      drawRect(color = accentColor.copy(alpha = 0.15f), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, h))
    }
    else -> {
      // Default live orbital particle
      val orbitX = cx + cos(animTime * 2) * (w * 0.25f)
      val orbitY = cy + sin(animTime * 2) * (h * 0.25f)
      drawCircle(color = accentColor.copy(alpha = 0.3f), radius = w * 0.25f, center = Offset(cx, cy), style = Stroke(width = 1.5.dp.toPx()))
      drawCircle(color = accentColor, radius = 4.dp.toPx(), center = Offset(orbitX, orbitY))
    }
  }
}
