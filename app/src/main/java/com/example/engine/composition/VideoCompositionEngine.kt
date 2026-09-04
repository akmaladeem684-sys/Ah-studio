package com.example.engine.composition

import android.content.Context
import android.graphics.*
import com.example.domain.model.*
import com.example.engine.KeyframeInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class ComposedFrame(
  val timelinePosMs: Long,
  val activeClip: VideoClip?,
  val clipSourcePosMs: Long,
  val activeOverlays: List<ComposedOverlay>,
  val activeTexts: List<ComposedText>,
  val activeStickers: List<ComposedSticker>,
  val activeTransition: ComposedTransition?,
  val colorMatrix: ColorMatrix,
  val colorFilter: ColorMatrixColorFilter
)

data class ComposedOverlay(
  val clip: VideoClip,
  val sourcePosMs: Long,
  val posX: Float,
  val posY: Float,
  val scale: Float,
  val rotation: Float,
  val opacity: Float,
  val blendMode: String
)

data class ComposedText(
  val clip: TextClip,
  val posX: Float,
  val posY: Float,
  val scale: Float,
  val rotation: Float,
  val opacity: Float
)

data class ComposedSticker(
  val clip: StickerClip,
  val posX: Float,
  val posY: Float,
  val scale: Float,
  val rotation: Float,
  val opacity: Float
)

data class ComposedTransition(
  val type: TransitionType,
  val progress: Float, // 0.0f to 1.0f
  val clipBefore: VideoClip,
  val clipAfter: VideoClip
)

class VideoCompositionEngine(private val context: Context) {

  /**
   * Calculates the exact state of all timeline elements at any timestamp.
   */
  fun evaluateFrame(timeline: Timeline, posMs: Long): ComposedFrame {
    val activeClip = timeline.videoClips.find {
      posMs >= it.timelineStartMs && posMs < it.timelineStartMs + it.durationMs
    } ?: timeline.videoClips.lastOrNull()

    val sourcePosMs = activeClip?.timelineToSourceMs(posMs) ?: 0L

    // Check transition
    var activeTransition: ComposedTransition? = null
    for (tr in timeline.transitions) {
      if (tr.clipIndexBefore >= 0 && tr.clipIndexBefore < timeline.videoClips.size - 1) {
        val clipBefore = timeline.videoClips[tr.clipIndexBefore]
        val clipAfter = timeline.videoClips[tr.clipIndexBefore + 1]
        val transitionStart = clipBefore.timelineStartMs + clipBefore.durationMs - (tr.durationMs / 2)
        val transitionEnd = transitionStart + tr.durationMs

        if (posMs in transitionStart until transitionEnd) {
          val progress = ((posMs - transitionStart).toFloat() / tr.durationMs).coerceIn(0f, 1f)
          activeTransition = ComposedTransition(
            type = tr.type,
            progress = progress,
            clipBefore = clipBefore,
            clipAfter = clipAfter
          )
          break
        }
      }
    }

    // Overlays with keyframes
    val overlays = timeline.overlayClips.filter {
      posMs >= it.timelineStartMs && posMs < it.timelineStartMs + it.durationMs
    }.map { clip ->
      val rel = posMs - clip.timelineStartMs
      val kf = KeyframeInterpolator.interpolate(clip, rel)
      ComposedOverlay(
        clip = clip,
        sourcePosMs = clip.timelineToSourceMs(posMs),
        posX = kf.posX,
        posY = kf.posY,
        scale = kf.scale,
        rotation = kf.rotation,
        opacity = kf.opacity,
        blendMode = clip.blendMode
      )
    }

    // Texts
    val texts = timeline.textClips.filter {
      posMs >= it.timelineStartMs && posMs < it.timelineStartMs + it.durationMs
    }.map { clip ->
      val rel = posMs - clip.timelineStartMs
      var textScale = clip.scale
      var textOpacity = clip.opacity
      var textPosY = clip.posY

      // Text intro animation
      if (rel < clip.animDurationMs) {
        val animProgress = (rel.toFloat() / clip.animDurationMs).coerceIn(0f, 1f)
        when (clip.animationType) {
          "Fade" -> textOpacity *= animProgress
          "Pop" -> textScale *= (animProgress * 1.15f).coerceAtMost(1f)
          "Slide" -> textPosY += (1f - animProgress) * 0.2f
          "Zoom" -> textScale *= (0.3f + 0.7f * animProgress)
        }
      }

      ComposedText(
        clip = clip,
        posX = clip.posX,
        posY = textPosY,
        scale = textScale,
        rotation = clip.rotation,
        opacity = textOpacity
      )
    }

    // Stickers
    val stickers = timeline.stickerClips.filter {
      posMs >= it.timelineStartMs && posMs < it.timelineStartMs + it.durationMs
    }.map { clip ->
      ComposedSticker(
        clip = clip,
        posX = clip.posX,
        posY = clip.posY,
        scale = clip.scale,
        rotation = clip.rotation,
        opacity = clip.opacity
      )
    }

    // Adjustments & Filters
    val colorMatrix = ColorFilterGenerator.createCombinedMatrix(timeline.adjustments, timeline.filter)
    val colorFilter = ColorMatrixColorFilter(colorMatrix)

    return ComposedFrame(
      timelinePosMs = posMs,
      activeClip = activeClip,
      clipSourcePosMs = sourcePosMs,
      activeOverlays = overlays,
      activeTexts = texts,
      activeStickers = stickers,
      activeTransition = activeTransition,
      colorMatrix = colorMatrix,
      colorFilter = colorFilter
    )
  }

  /**
   * Renders the composed frame onto an Android Canvas for export or preview capture.
   */
  fun renderFrame(
    canvas: Canvas,
    frame: ComposedFrame,
    mainBitmap: Bitmap?,
    overlayBitmaps: Map<String, Bitmap>,
    canvasWidth: Int,
    canvasHeight: Int,
    chromaKey: ChromaKeySettings = ChromaKeySettings()
  ) {
    // 1. Draw canvas background
    canvas.drawColor(Color.BLACK)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    paint.colorFilter = frame.colorFilter

    // 2. Draw Main Clip Bitmap
    if (mainBitmap != null && !mainBitmap.isRecycled) {
      val clip = frame.activeClip
      val scaleX = canvasWidth.toFloat() / mainBitmap.width
      val scaleY = canvasHeight.toFloat() / mainBitmap.height
      val baseScale = max(scaleX, scaleY)

      val matrix = Matrix()
      // Center bitmap
      matrix.postTranslate(-mainBitmap.width / 2f, -mainBitmap.height / 2f)

      if (clip != null) {
        val rel = frame.timelinePosMs - clip.timelineStartMs
        val kf = KeyframeInterpolator.interpolate(clip, rel)
        matrix.postScale(
          if (clip.flipHorizontal) -1f else 1f,
          if (clip.flipVertical) -1f else 1f
        )
        matrix.postRotate((clip.rotationDegrees + kf.rotation) % 360)
        matrix.postScale(baseScale * clip.cropScale * kf.scale, baseScale * clip.cropScale * kf.scale)
        matrix.postTranslate(
          (canvasWidth / 2f) + (clip.cropOffsetX + kf.posX) * (canvasWidth / 2f),
          (canvasHeight / 2f) + (clip.cropOffsetY + kf.posY) * (canvasHeight / 2f)
        )
      } else {
        matrix.postScale(baseScale, baseScale)
        matrix.postTranslate(canvasWidth / 2f, canvasHeight / 2f)
      }

      // Handle transition blending if active
      if (frame.activeTransition != null) {
        val tr = frame.activeTransition
        when (tr.type) {
          TransitionType.FADE -> {
            paint.alpha = ((1f - tr.progress) * 255).toInt().coerceIn(0, 255)
            canvas.drawBitmap(mainBitmap, matrix, paint)
          }
          TransitionType.WIPE -> {
            canvas.save()
            val clipRight = canvasWidth * (1f - tr.progress)
            canvas.clipRect(0f, 0f, clipRight, canvasHeight.toFloat())
            canvas.drawBitmap(mainBitmap, matrix, paint)
            canvas.restore()
          }
          TransitionType.SLIDE_LEFT -> {
            matrix.postTranslate(-canvasWidth * tr.progress, 0f)
            canvas.drawBitmap(mainBitmap, matrix, paint)
          }
          TransitionType.ZOOM_IN -> {
            val zoom = 1f + tr.progress * 0.5f
            matrix.postScale(zoom, zoom, canvasWidth / 2f, canvasHeight / 2f)
            canvas.drawBitmap(mainBitmap, matrix, paint)
          }
          else -> {
            paint.alpha = 255
            canvas.drawBitmap(mainBitmap, matrix, paint)
          }
        }
      } else {
        paint.alpha = 255
        canvas.drawBitmap(mainBitmap, matrix, paint)
      }
    }

    // 3. Draw Overlays (PIP)
    for (composedOverlay in frame.activeOverlays) {
      val rawOverlayBitmap = overlayBitmaps[composedOverlay.clip.id]
      if (rawOverlayBitmap != null && !rawOverlayBitmap.isRecycled) {
        val overlayBitmap = if (chromaKey.enabled) {
          ChromaKeyProcessor.applyChromaKey(rawOverlayBitmap, chromaKey)
        } else rawOverlayBitmap

        val overlayMatrix = Matrix()
        overlayMatrix.postTranslate(-overlayBitmap.width / 2f, -overlayBitmap.height / 2f)
        overlayMatrix.postRotate(composedOverlay.rotation)
        val overlayScale = (canvasWidth.toFloat() / overlayBitmap.width) * composedOverlay.scale * 0.5f
        overlayMatrix.postScale(overlayScale, overlayScale)

        val targetX = (canvasWidth / 2f) + (composedOverlay.posX * canvasWidth / 2f)
        val targetY = (canvasHeight / 2f) + (composedOverlay.posY * canvasHeight / 2f)
        overlayMatrix.postTranslate(targetX, targetY)

        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        overlayPaint.alpha = (composedOverlay.opacity * 255).toInt().coerceIn(0, 255)

        // Blend mode support
        when (composedOverlay.blendMode.lowercase()) {
          "screen" -> overlayPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
          "multiply" -> overlayPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
          "overlay" -> overlayPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
          "lighten" -> overlayPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
          else -> overlayPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }

        canvas.drawBitmap(overlayBitmap, overlayMatrix, overlayPaint)
      }
    }

    // 4. Draw Text Overlays
    for (composedText in frame.activeTexts) {
      drawTextClip(canvas, composedText, canvasWidth, canvasHeight)
    }

    // 5. Draw Stickers
    for (sticker in frame.activeStickers) {
      drawStickerClip(canvas, sticker, canvasWidth, canvasHeight)
    }
  }

  private fun drawTextClip(canvas: Canvas, composedText: ComposedText, width: Int, height: Int) {
    val clip = composedText.clip
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = (clip.fontSizeSp * (width / 400f)) * composedText.scale
      isFakeBoldText = clip.fontWeight >= 700
      color = clip.textColor.toInt()
      alpha = (composedText.opacity * 255).toInt().coerceIn(0, 255)
      textAlign = Paint.Align.CENTER
    }

    val centerX = (width / 2f) + (composedText.posX * width / 2f)
    val centerY = (height / 2f) + (composedText.posY * height / 2f)

    canvas.save()
    canvas.translate(centerX, centerY)
    canvas.rotate(composedText.rotation)

    val textBounds = Rect()
    textPaint.getTextBounds(clip.text, 0, clip.text.length, textBounds)
    val pad = 16f

    // Draw Background Rect
    if (clip.hasBackground) {
      val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = clip.backgroundColor.toInt()
        alpha = (composedText.opacity * Color.alpha(clip.backgroundColor.toInt()) / 255f * 255).toInt().coerceIn(0, 255)
      }
      val rect = RectF(
        textBounds.left - pad,
        textBounds.top - pad,
        textBounds.right + pad,
        textBounds.bottom + pad
      )
      canvas.drawRoundRect(rect, 12f, 12f, bgPaint)
    }

    // Draw Stroke
    if (clip.strokeWidth > 0f) {
      val strokePaint = Paint(textPaint).apply {
        style = Paint.Style.STROKE
        strokeWidth = clip.strokeWidth * (width / 400f)
        color = clip.strokeColor.toInt()
        alpha = (composedText.opacity * 255).toInt().coerceIn(0, 255)
      }
      canvas.drawText(clip.text, 0f, 0f, strokePaint)
    }

    // Draw Gradient
    if (clip.hasGradient) {
      val shader = LinearGradient(
        textBounds.left.toFloat(), 0f,
        textBounds.right.toFloat(), 0f,
        clip.gradientColorStart.toInt(),
        clip.gradientColorEnd.toInt(),
        Shader.TileMode.CLAMP
      )
      textPaint.shader = shader
    }

    // Draw Text Fill
    canvas.drawText(clip.text, 0f, 0f, textPaint)
    canvas.restore()
  }

  private fun drawStickerClip(canvas: Canvas, sticker: ComposedSticker, width: Int, height: Int) {
    val clip = sticker.clip
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = 54f * sticker.scale * (width / 400f)
      alpha = (sticker.opacity * 255).toInt().coerceIn(0, 255)
      textAlign = Paint.Align.CENTER
    }

    val centerX = (width / 2f) + (sticker.posX * width / 2f)
    val centerY = (height / 2f) + (sticker.posY * height / 2f)

    canvas.save()
    canvas.translate(centerX, centerY)
    canvas.rotate(sticker.rotation)
    canvas.drawText(clip.emojiOrAsset, 0f, 0f, paint)
    canvas.restore()
  }
}
