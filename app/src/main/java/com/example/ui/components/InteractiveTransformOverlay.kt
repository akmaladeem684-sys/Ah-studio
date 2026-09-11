package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.StickerClip
import com.example.domain.model.TextClip
import com.example.domain.model.VideoClip
import com.example.engine.KeyframeInterpolator
import com.example.engine.SelectedTrackElement
import com.example.engine.text.TextLayerRenderer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.TextPrimary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Touch-Based Interactive Transformation Canvas.
 * Supports direct touch Drag, Pinch Scale, Two-Finger Rotation, Corner Transform Handles,
 * Freehand Edge Position Coercion, Selection Switching, and Undo-Compatible Timeline State Updates.
 */
@Composable
fun InteractiveTransformOverlay(
  activeTexts: List<TextClip>,
  activeOverlays: List<VideoClip>,
  activeStickers: List<StickerClip>,
  selectedElement: SelectedTrackElement,
  currentPosMs: Long,
  onSelectElement: (SelectedTrackElement) -> Unit,
  onUpdateText: (TextClip) -> Unit,
  onUpdateOverlay: (VideoClip) -> Unit,
  onUpdateSticker: (StickerClip) -> Unit,
  onDeleteClip: (String) -> Unit,
  onDuplicateClip: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  val currentOnSelectElement by rememberUpdatedState(onSelectElement)
  val currentOnUpdateText by rememberUpdatedState(onUpdateText)
  val currentOnUpdateOverlay by rememberUpdatedState(onUpdateOverlay)
  val currentOnUpdateSticker by rememberUpdatedState(onUpdateSticker)

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .pointerInput(Unit) {
        // Tap on empty canvas background to deselect active transform frame
        detectTapGestures {
          currentOnSelectElement(SelectedTrackElement.None)
        }
      }
  ) {
    val parentWidthPx = constraints.maxWidth.toFloat()
    val parentHeightPx = constraints.maxHeight.toFloat()
    val density = LocalDensity.current

    if (parentWidthPx <= 0f || parentHeightPx <= 0f) return@BoxWithConstraints

    // 1. Render Active Overlays (PIP / Picture-in-Picture)
    activeOverlays.forEach { overlay ->
      val isSelected = selectedElement is SelectedTrackElement.Overlay &&
        (selectedElement as SelectedTrackElement.Overlay).clipId == overlay.id

      val currentOverlay by rememberUpdatedState(overlay)
      val relTime = currentPosMs - overlay.timelineStartMs
      val kf = KeyframeInterpolator.interpolate(overlay, relTime)

      // Base unscaled size
      val baseWidthDp = 160.dp
      val baseHeightDp = 100.dp

      val baseWidthPx = with(density) { baseWidthDp.toPx() }
      val baseHeightPx = with(density) { baseHeightDp.toPx() }

      // Normalized coordinates (-1f..1f) to Screen Center Offset
      val centerXPx = (parentWidthPx / 2f) + (kf.posX * parentWidthPx / 2f)
      val centerYPx = (parentHeightPx / 2f) + (kf.posY * parentHeightPx / 2f)

      val centerXDp = with(density) { centerXPx.toDp() }
      val centerYDp = with(density) { centerYPx.toDp() }

      Box(
        modifier = Modifier
          .offset(
            x = centerXDp - (baseWidthDp / 2f),
            y = centerYDp - (baseHeightDp / 2f)
          )
          .size(baseWidthDp, baseHeightDp)
          .scale(kf.scale)
          .rotate(kf.rotation)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF1E293B))
          .border(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AmberAccent else Color.White.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
          )
          .pointerInput(overlay.id) {
            detectTapGestures {
              currentOnSelectElement(SelectedTrackElement.Overlay(currentOverlay.id))
            }
          }
          .pointerInput(overlay.id) {
            detectTransformGestures { _, pan, zoom, rotationChange ->
              val clip = currentOverlay
              currentOnSelectElement(SelectedTrackElement.Overlay(clip.id))
              val deltaPosX = (pan.x * 2f) / parentWidthPx
              val deltaPosY = (pan.y * 2f) / parentHeightPx
              val newX = (clip.cropOffsetX + deltaPosX).coerceIn(-1.5f, 1.5f)
              val newY = (clip.cropOffsetY + deltaPosY).coerceIn(-1.5f, 1.5f)
              val newScale = (clip.cropScale * zoom).coerceIn(0.15f, 8.0f)
              val newRot = ((clip.rotationDegrees + rotationChange) % 360f).toInt()

              currentOnUpdateOverlay(
                clip.copy(
                  cropOffsetX = newX,
                  cropOffsetY = newY,
                  cropScale = newScale,
                  rotationDegrees = newRot
                )
              )
            }
          }
      ) {
        AsyncImage(
          model = overlay.uri,
          contentDescription = overlay.name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )

        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(AmberAccent)
                .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
              Text(
                text = "PIP",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.Black
                )
              )
            }
            Icon(
              if (overlay.isVideo) Icons.Default.Movie else Icons.Default.Image,
              contentDescription = null,
              tint = AmberAccent,
              modifier = Modifier.size(14.dp)
            )
          }
          Text(
            text = overlay.name,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // If Overlay is Selected, Render Interactive Transform Handles around it
      if (isSelected) {
        TransformHandlesBox(
          centerXPx = centerXPx,
          centerYPx = centerYPx,
          baseWidthPx = baseWidthPx,
          baseHeightPx = baseHeightPx,
          scale = kf.scale,
          rotation = kf.rotation,
          onDelete = { onDeleteClip(overlay.id) },
          onDuplicate = { onDuplicateClip(overlay.id) },
          onReset = {
            onUpdateOverlay(overlay.copy(cropScale = 1.0f, rotationDegrees = 0))
          },
          onTransformHandleDrag = { deltaScale, deltaRotation ->
            val clip = currentOverlay
            val newScale = (clip.cropScale * deltaScale).coerceIn(0.15f, 8.0f)
            val newRot = ((clip.rotationDegrees + deltaRotation) % 360f).toInt()
            currentOnUpdateOverlay(clip.copy(cropScale = newScale, rotationDegrees = newRot))
          }
        )
      }
    }

    // 2. Render Active Stickers
    activeStickers.forEach { sticker ->
      val isSelected = selectedElement is SelectedTrackElement.Sticker &&
        (selectedElement as SelectedTrackElement.Sticker).clipId == sticker.id

      val currentSticker by rememberUpdatedState(sticker)
      val baseSizeDp = 80.dp
      val baseWidthPx = with(density) { baseSizeDp.toPx() }
      val baseHeightPx = with(density) { baseSizeDp.toPx() }

      val centerXPx = (parentWidthPx / 2f) + (sticker.posX * parentWidthPx / 2f)
      val centerYPx = (parentHeightPx / 2f) + (sticker.posY * parentHeightPx / 2f)

      val centerXDp = with(density) { centerXPx.toDp() }
      val centerYDp = with(density) { centerYPx.toDp() }

      Box(
        modifier = Modifier
          .offset(
            x = centerXDp - (baseSizeDp / 2f),
            y = centerYDp - (baseSizeDp / 2f)
          )
          .size(baseSizeDp)
          .scale(sticker.scale)
          .rotate(sticker.rotation)
          .border(
            width = if (isSelected) 2.dp else 0.dp,
            color = if (isSelected) AmberAccent else Color.Transparent,
            shape = RoundedCornerShape(12.dp)
          )
          .pointerInput(sticker.id) {
            detectTapGestures {
              currentOnSelectElement(SelectedTrackElement.Sticker(currentSticker.id))
            }
          }
          .pointerInput(sticker.id) {
            detectTransformGestures { _, pan, zoom, rotationChange ->
              val clip = currentSticker
              currentOnSelectElement(SelectedTrackElement.Sticker(clip.id))
              val deltaPosX = (pan.x * 2f) / parentWidthPx
              val deltaPosY = (pan.y * 2f) / parentHeightPx
              val newX = (clip.posX + deltaPosX).coerceIn(-1.5f, 1.5f)
              val newY = (clip.posY + deltaPosY).coerceIn(-1.5f, 1.5f)
              val newScale = (clip.scale * zoom).coerceIn(0.15f, 8.0f)
              val newRot = (clip.rotation + rotationChange) % 360f

              currentOnUpdateSticker(
                clip.copy(
                  posX = newX,
                  posY = newY,
                  scale = newScale,
                  rotation = newRot
                )
              )
            }
          },
        contentAlignment = Alignment.Center
      ) {
        Text(sticker.emojiOrAsset, fontSize = 46.sp)
      }

      if (isSelected) {
        TransformHandlesBox(
          centerXPx = centerXPx,
          centerYPx = centerYPx,
          baseWidthPx = baseWidthPx,
          baseHeightPx = baseHeightPx,
          scale = sticker.scale,
          rotation = sticker.rotation,
          onDelete = { onDeleteClip(sticker.id) },
          onDuplicate = { onDuplicateClip(sticker.id) },
          onReset = {
            onUpdateSticker(sticker.copy(scale = 1.0f, rotation = 0f))
          },
          onTransformHandleDrag = { deltaScale, deltaRotation ->
            val clip = currentSticker
            val newScale = (clip.scale * deltaScale).coerceIn(0.15f, 8.0f)
            val newRot = (clip.rotation + deltaRotation) % 360f
            currentOnUpdateSticker(clip.copy(scale = newScale, rotation = newRot))
          }
        )
      }
    }

    // 3. Render Active Text Layers
    activeTexts.forEach { textClip ->
      val isSelected = selectedElement is SelectedTrackElement.Text &&
        (selectedElement as SelectedTrackElement.Text).clipId == textClip.id

      val currentTextClip by rememberUpdatedState(textClip)

      val centerXPx = (parentWidthPx / 2f) + (textClip.posX * parentWidthPx / 2f)
      val centerYPx = (parentHeightPx / 2f) + (textClip.posY * parentHeightPx / 2f)

      // Estimate base bounds for handles
      val approxTextLen = textClip.text.length.coerceAtLeast(3)
      val baseWidthDp = maxOf(120.dp, (approxTextLen * textClip.fontSizeSp * 0.45f).dp)
      val baseHeightDp = maxOf(48.dp, (textClip.fontSizeSp * 1.6f).dp)

      val baseWidthPx = with(density) { baseWidthDp.toPx() }
      val baseHeightPx = with(density) { baseHeightDp.toPx() }

      val currentWidthPx = baseWidthPx * textClip.scale
      val currentHeightPx = baseHeightPx * textClip.scale

      val currentWidthDp = with(density) { currentWidthPx.toDp() }
      val currentHeightDp = with(density) { currentHeightPx.toDp() }

      val centerXDp = with(density) { centerXPx.toDp() }
      val centerYDp = with(density) { centerYPx.toDp() }

      // Custom Canvas for crisp TextLayerRenderer
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
          TextLayerRenderer.draw(
            canvas = canvas.nativeCanvas,
            clip = textClip,
            currentPosMs = currentPosMs,
            width = parentWidthPx.toInt(),
            height = parentHeightPx.toInt(),
            context = context
          )
        }
      }

      // Touch Target Bounding Box positioned at exact text coordinates
      Box(
        modifier = Modifier
          .offset(
            x = centerXDp - (currentWidthDp / 2f),
            y = centerYDp - (currentHeightDp / 2f)
          )
          .size(currentWidthDp, currentHeightDp)
          .rotate(textClip.rotation)
          .pointerInput(textClip.id) {
            detectTapGestures {
              currentOnSelectElement(SelectedTrackElement.Text(currentTextClip.id))
            }
          }
          .pointerInput(textClip.id) {
            detectTransformGestures { _, pan, zoom, rotationChange ->
              val clip = currentTextClip
              currentOnSelectElement(SelectedTrackElement.Text(clip.id))
              val deltaPosX = (pan.x * 2f) / parentWidthPx
              val deltaPosY = (pan.y * 2f) / parentHeightPx
              val newX = (clip.posX + deltaPosX).coerceIn(-1.5f, 1.5f)
              val newY = (clip.posY + deltaPosY).coerceIn(-1.5f, 1.5f)
              val newScale = (clip.scale * zoom).coerceIn(0.15f, 8.0f)
              val newRot = (clip.rotation + rotationChange) % 360f

              currentOnUpdateText(
                clip.copy(
                  posX = newX,
                  posY = newY,
                  scale = newScale,
                  rotation = newRot
                )
              )
            }
          }
      )

      if (isSelected) {
        TransformHandlesBox(
          centerXPx = centerXPx,
          centerYPx = centerYPx,
          baseWidthPx = baseWidthPx,
          baseHeightPx = baseHeightPx,
          scale = textClip.scale,
          rotation = textClip.rotation,
          onDelete = { onDeleteClip(textClip.id) },
          onDuplicate = { onDuplicateClip(textClip.id) },
          onReset = {
            onUpdateText(textClip.copy(scale = 1.0f, rotation = 0f))
          },
          onTransformHandleDrag = { deltaScale, deltaRotation ->
            val clip = currentTextClip
            val newScale = (clip.scale * deltaScale).coerceIn(0.15f, 8.0f)
            val newRot = (clip.rotation + deltaRotation) % 360f
            currentOnUpdateText(clip.copy(scale = newScale, rotation = newRot))
          }
        )
      }
    }
  }
}

/**
 * Renders high-contrast bounding box and 4 corner touch handles (Delete, Scale/Rotate, Duplicate, Reset)
 * around the active element.
 */
@Composable
private fun TransformHandlesBox(
  centerXPx: Float,
  centerYPx: Float,
  baseWidthPx: Float,
  baseHeightPx: Float,
  scale: Float,
  rotation: Float,
  onDelete: () -> Unit,
  onDuplicate: () -> Unit,
  onReset: () -> Unit,
  onTransformHandleDrag: (deltaScale: Float, deltaRotation: Float) -> Unit
) {
  val density = LocalDensity.current

  val currentWidthPx = baseWidthPx * scale
  val currentHeightPx = baseHeightPx * scale

  val currentWidthDp = with(density) { currentWidthPx.toDp() }
  val currentHeightDp = with(density) { currentHeightPx.toDp() }

  val centerXDp = with(density) { centerXPx.toDp() }
  val centerYDp = with(density) { centerYPx.toDp() }

  val handleSizeDp = 28.dp
  val halfHandleDp = handleSizeDp / 2f

  Box(
    modifier = Modifier.fillMaxSize()
  ) {
    // 1. Dashed/Solid Accent Bounding Border Box
    Box(
      modifier = Modifier
        .offset(
          x = centerXDp - (currentWidthDp / 2f),
          y = centerYDp - (currentHeightDp / 2f)
        )
        .size(currentWidthDp, currentHeightDp)
        .rotate(rotation)
        .border(
          width = 2.dp,
          color = CyanAccent,
          shape = RoundedCornerShape(8.dp)
        )
    )

    // Corner Positions relative to center with rotation applied
    val rad = Math.toRadians(rotation.toDouble())
    val halfW = currentWidthPx / 2f
    val halfH = currentHeightPx / 2f

    // Corner Offsets from Center
    // Top-Right Corner (Delete)
    val trX = centerXPx + (halfW * cos(rad) - (-halfH) * sin(rad)).toFloat()
    val trY = centerYPx + (halfW * sin(rad) + (-halfH) * cos(rad)).toFloat()

    // Bottom-Right Corner (Scale & Rotate Handle)
    val brX = centerXPx + (halfW * cos(rad) - (halfH) * sin(rad)).toFloat()
    val brY = centerYPx + (halfW * sin(rad) + (halfH) * cos(rad)).toFloat()

    // Top-Left Corner (Duplicate / Copy)
    val tlX = centerXPx + ((-halfW) * cos(rad) - (-halfH) * sin(rad)).toFloat()
    val tlY = centerYPx + ((-halfW) * sin(rad) + (-halfH) * cos(rad)).toFloat()

    // Bottom-Left Corner (Reset Scale/Rotation)
    val blX = centerXPx + ((-halfW) * cos(rad) - (halfH) * sin(rad)).toFloat()
    val blY = centerYPx + ((-halfW) * sin(rad) + (halfH) * cos(rad)).toFloat()

    // Convert corners to DP
    val trXDp = with(density) { trX.toDp() }
    val trYDp = with(density) { trY.toDp() }

    val brXDp = with(density) { brX.toDp() }
    val brYDp = with(density) { brY.toDp() }

    val tlXDp = with(density) { tlX.toDp() }
    val tlYDp = with(density) { tlY.toDp() }

    val blXDp = with(density) { blX.toDp() }
    val blYDp = with(density) { blY.toDp() }

    // 2. Corner Handle Buttons
    // Top-Right: Delete
    Surface(
      onClick = onDelete,
      shape = CircleShape,
      color = Color(0xFFFF5252),
      shadowElevation = 4.dp,
      modifier = Modifier
        .offset(x = trXDp - halfHandleDp, y = trYDp - halfHandleDp)
        .size(handleSizeDp)
        .testTag("handle_delete_button")
    ) {
      Icon(
        Icons.Default.Close,
        contentDescription = "Delete Clip",
        tint = Color.White,
        modifier = Modifier
          .padding(4.dp)
          .fillMaxSize()
      )
    }

    // Top-Left: Duplicate
    Surface(
      onClick = onDuplicate,
      shape = CircleShape,
      color = PurpleAccent,
      shadowElevation = 4.dp,
      modifier = Modifier
        .offset(x = tlXDp - halfHandleDp, y = tlYDp - halfHandleDp)
        .size(handleSizeDp)
        .testTag("handle_duplicate_button")
    ) {
      Icon(
        Icons.Default.ContentCopy,
        contentDescription = "Duplicate Clip",
        tint = Color.White,
        modifier = Modifier
          .padding(5.dp)
          .fillMaxSize()
      )
    }

    // Bottom-Left: Reset Scale & Rotation
    Surface(
      onClick = onReset,
      shape = CircleShape,
      color = Color.DarkGray,
      shadowElevation = 4.dp,
      modifier = Modifier
        .offset(x = blXDp - halfHandleDp, y = blYDp - halfHandleDp)
        .size(handleSizeDp)
        .testTag("handle_reset_button")
    ) {
      Icon(
        Icons.Default.RestartAlt,
        contentDescription = "Reset Transform",
        tint = Color.White,
        modifier = Modifier
          .padding(4.dp)
          .fillMaxSize()
      )
    }

    // Bottom-Right: Single-Finger Drag Scale & Rotation Handle
    var lastTouchDist by remember { mutableFloatStateOf(0f) }
    var lastTouchAngle by remember { mutableFloatStateOf(0f) }

    Surface(
      shape = CircleShape,
      color = CyanAccent,
      shadowElevation = 6.dp,
      modifier = Modifier
        .offset(x = brXDp - halfHandleDp, y = brYDp - halfHandleDp)
        .size(handleSizeDp + 4.dp) // slightly larger for touch ease
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { offset ->
              val touchXPx = brX + offset.x
              val touchYPx = brY + offset.y
              val dx = touchXPx - centerXPx
              val dy = touchYPx - centerYPx
              lastTouchDist = sqrt(dx * dx + dy * dy).coerceAtLeast(10f)
              lastTouchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            },
            onDrag = { change, dragAmount ->
              change.consume()
              val touchXPx = brX + change.position.x
              val touchYPx = brY + change.position.y
              val dx = touchXPx - centerXPx
              val dy = touchYPx - centerYPx

              val currentDist = sqrt(dx * dx + dy * dy).coerceAtLeast(10f)
              val currentAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

              if (lastTouchDist > 0f) {
                val deltaScale = currentDist / lastTouchDist
                val deltaRotation = currentAngle - lastTouchAngle
                onTransformHandleDrag(deltaScale, deltaRotation)
              }

              lastTouchDist = currentDist
              lastTouchAngle = currentAngle
            }
          )
        }
        .testTag("handle_transform_button")
    ) {
      Icon(
        Icons.Default.CropRotate,
        contentDescription = "Drag to Resize and Rotate",
        tint = Color.Black,
        modifier = Modifier
          .padding(4.dp)
          .fillMaxSize()
      )
    }
  }
}
