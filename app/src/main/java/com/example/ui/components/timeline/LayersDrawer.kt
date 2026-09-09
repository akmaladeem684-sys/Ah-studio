package com.example.ui.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Timeline
import com.example.domain.model.TrackHeight
import com.example.domain.model.TrackSettings
import com.example.domain.model.TrackType
import com.example.ui.theme.*

/**
 * Collapsible Left Layers Drawer / Overlay for AH Video Studio.
 *
 * Displays all 5 primary project layers:
 * - V1 Main
 * - V2 Overlay
 * - T1 Subtitle
 * - A1 Master
 * - S1 Sticker
 * (plus FX Filter track)
 *
 * Provides full control over Lock, Visibility/Hide, Mute, Solo, and Height.
 */
@Composable
fun LayersDrawer(
  timeline: Timeline,
  onToggleTrackLock: (TrackType) -> Unit,
  onToggleTrackHide: (TrackType) -> Unit,
  onToggleTrackMute: (TrackType) -> Unit,
  onToggleTrackSolo: (TrackType) -> Unit,
  onCycleTrackHeight: (TrackType) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val layersList = listOf(
    TrackType.MAIN_VIDEO,
    TrackType.OVERLAY,
    TrackType.TEXT,
    TrackType.AUDIO,
    TrackType.STICKER,
    TrackType.EFFECT
  )

  Surface(
    modifier = modifier
      .width(280.dp)
      .fillMaxHeight()
      .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
      .border(1.dp, StudioBorder, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)),
    color = StudioSurface,
    tonalElevation = 8.dp,
    shadowElevation = 12.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
      // Drawer Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(SkyBlueContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Layers,
              contentDescription = "Layers",
              tint = CyanAccent,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Track Layers",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 15.sp
              )
            )
            Text(
              text = "5 Multi-Tracks Active",
              style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 11.sp
              )
            )
          }
        }

        IconButton(
          onClick = onClose,
          modifier = Modifier
            .size(32.dp)
            .testTag("layers_drawer_close_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close Layers",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      HorizontalDivider(color = StudioBorder, thickness = 1.dp)

      Spacer(modifier = Modifier.height(10.dp))

      // List of Layer Cards
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(layersList) { trackType ->
          val settings = timeline.trackSettings[trackType] ?: TrackSettings(trackType)
          LayerCardItem(
            trackType = trackType,
            settings = settings,
            clipCount = when (trackType) {
              TrackType.MAIN_VIDEO -> timeline.videoClips.size
              TrackType.OVERLAY -> timeline.overlayClips.size
              TrackType.TEXT -> timeline.textClips.size
              TrackType.AUDIO -> timeline.audioClips.size
              TrackType.STICKER -> timeline.stickerClips.size
              TrackType.EFFECT -> timeline.effectClips.size
            },
            onToggleLock = { onToggleTrackLock(trackType) },
            onToggleHide = { onToggleTrackHide(trackType) },
            onToggleMute = { onToggleTrackMute(trackType) },
            onToggleSolo = { onToggleTrackSolo(trackType) },
            onCycleHeight = { onCycleTrackHeight(trackType) }
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Bottom Note / Quick dismiss tip
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = StudioSurfaceVariant
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Tap outside or toggle header to close layers panel.",
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextSecondary,
              fontSize = 11.sp
            )
          )
        }
      }
    }
  }
}

@Composable
private fun LayerCardItem(
  trackType: TrackType,
  settings: TrackSettings,
  clipCount: Int,
  onToggleLock: () -> Unit,
  onToggleHide: () -> Unit,
  onToggleMute: () -> Unit,
  onToggleSolo: () -> Unit,
  onCycleHeight: () -> Unit
) {
  val (label, icon, color) = when (trackType) {
    TrackType.MAIN_VIDEO -> Triple("V1 Main", Icons.Default.Movie, VideoTrackColor)
    TrackType.OVERLAY -> Triple("V2 Overlay", Icons.Default.Layers, OverlayTrackColor)
    TrackType.TEXT -> Triple("T1 Subtitle", Icons.Default.TextFields, TextTrackColor)
    TrackType.AUDIO -> Triple("A1 Master", Icons.Default.Audiotrack, AudioTrackColor)
    TrackType.STICKER -> Triple("S1 Sticker", Icons.Default.EmojiEmotions, StickerTrackColor)
    TrackType.EFFECT -> Triple("FX Filter", Icons.Default.AutoFixHigh, EffectTrackColor)
  }

  val hasAudio = trackType == TrackType.MAIN_VIDEO || trackType == TrackType.OVERLAY || trackType == TrackType.AUDIO
  val hasVisual = trackType != TrackType.AUDIO

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .border(
        width = if (settings.isLocked) 1.5.dp else 1.dp,
        color = if (settings.isLocked) RedAccent.copy(alpha = 0.4f) else StudioBorder,
        shape = RoundedCornerShape(10.dp)
      ),
    color = if (settings.isLocked) StudioSurfaceVariant.copy(alpha = 0.7f) else StudioSurfaceVariant,
    tonalElevation = 1.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left: Track Color Indicator + Icon + Label + Clip count
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
          Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = if (settings.isLocked) TextTertiary else TextPrimary
            )
          )
          Text(
            text = "$clipCount clip${if (clipCount == 1) "" else "s"}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              color = TextSecondary
            )
          )
        }
      }

      // Right: Action Buttons (Lock, Eye/Visibility, Mute, Solo, Height)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Lock Button
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (settings.isLocked) RedAccent.copy(alpha = 0.2f) else Color.White)
            .border(0.5.dp, StudioBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onToggleLock)
            .testTag("drawer_track_lock_${trackType.name.lowercase()}"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (settings.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = "Lock Track",
            tint = if (settings.isLocked) RedAccent else TextSecondary,
            modifier = Modifier.size(15.dp)
          )
        }

        // Hide Button (Eye)
        if (hasVisual) {
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(if (settings.isHidden) AmberAccent.copy(alpha = 0.2f) else Color.White)
              .border(0.5.dp, StudioBorder, RoundedCornerShape(6.dp))
              .clickable(onClick = onToggleHide)
              .testTag("drawer_track_hide_${trackType.name.lowercase()}"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (settings.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = "Hide Track",
              tint = if (settings.isHidden) AmberAccent else TextSecondary,
              modifier = Modifier.size(15.dp)
            )
          }
        }

        // Mute Button
        if (hasAudio) {
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(if (settings.isMuted) RedAccent.copy(alpha = 0.2f) else Color.White)
              .border(0.5.dp, StudioBorder, RoundedCornerShape(6.dp))
              .clickable(onClick = onToggleMute)
              .testTag("drawer_track_mute_${trackType.name.lowercase()}"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (settings.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
              contentDescription = "Mute Track",
              tint = if (settings.isMuted) RedAccent else TextSecondary,
              modifier = Modifier.size(15.dp)
            )
          }

          // Solo Button
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(if (settings.isSolo) SkyBlueContainer else Color.White)
              .border(0.5.dp, if (settings.isSolo) CyanAccent else StudioBorder, RoundedCornerShape(6.dp))
              .clickable(onClick = onToggleSolo)
              .testTag("drawer_track_solo_${trackType.name.lowercase()}"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "S",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (settings.isSolo) CyanAccent else TextTertiary
              )
            )
          }
        }

        // Height Cycle Button
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(0.5.dp, StudioBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onCycleHeight)
            .testTag("drawer_track_height_${trackType.name.lowercase()}"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when (settings.height) {
              TrackHeight.COMPACT -> Icons.Default.UnfoldMore
              TrackHeight.NORMAL -> Icons.Default.Height
              TrackHeight.EXPANDED -> Icons.Default.UnfoldLess
            },
            contentDescription = "Track Height",
            tint = TextSecondary,
            modifier = Modifier.size(15.dp)
          )
        }
      }
    }
  }
}
