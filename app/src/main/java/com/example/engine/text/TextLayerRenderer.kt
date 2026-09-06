package com.example.engine.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.domain.model.TextClip
import com.example.domain.model.WordTiming
import com.example.util.FontManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

data class EvaluatedTextState(
  val visibleText: String,
  val posX: Float,
  val posY: Float,
  val scale: Float,
  val rotation: Float,
  val opacity: Float,
  val activeWordIndex: Int = -1,
  val words: List<WordTiming> = emptyList()
)

object TextLayerRenderer {

  fun evaluateAnimation(clip: TextClip, currentPosMs: Long): EvaluatedTextState {
    val relTime = (currentPosMs - clip.timelineStartMs).coerceAtLeast(0L)
    val animDur = clip.animDurationMs.coerceAtLeast(100L)
    val progress = (relTime.toFloat() / animDur).coerceIn(0f, 1f)

    var animAlpha = 1.0f
    var animScale = 1.0f
    var animPosX = clip.posX
    var animPosY = clip.posY
    var animRotation = clip.rotation
    var visibleText = clip.text

    when (clip.animationType.lowercase()) {
      "fade" -> {
        animAlpha = progress
      }
      "slide" -> {
        val easeOut = 1f - (1f - progress) * (1f - progress)
        animPosY = clip.posY + (1f - easeOut) * 0.18f
        animAlpha = progress
      }
      "zoom" -> {
        val easeOut = 1f - (1f - progress) * (1f - progress)
        animScale = 0.2f + 0.8f * easeOut
        animAlpha = progress
      }
      "pop" -> {
        animScale = if (progress < 0.7f) {
          progress / 0.7f * 1.18f
        } else {
          1.18f - (progress - 0.7f) / 0.3f * 0.18f
        }
        animAlpha = (progress * 2f).coerceAtMost(1f)
      }
      "bounce" -> {
        val spring = 1f - cos(progress * PI.toFloat() * 2.5f) * exp(-progress * 3.5f)
        animScale = spring
        animAlpha = (progress * 2.5f).coerceAtMost(1f)
      }
      "typewriter" -> {
        val totalChars = clip.text.length
        val charCount = (totalChars * progress).toInt().coerceIn(0, totalChars)
        visibleText = if (charCount < totalChars) {
          clip.text.substring(0, charCount) + if ((relTime / 250) % 2 == 0L) "|" else ""
        } else {
          clip.text
        }
      }
      "shake" -> {
        val t = relTime.toFloat() / 45f
        animPosX += sin(t * 1.6f) * 0.012f
        animPosY += cos(t * 2.1f) * 0.012f
        animRotation += sin(t * 1.3f) * 2.2f
      }
      else -> {
        // "None" or fallback
      }
    }

    // Resolve words and active word index for word-level timing
    val resolvedWords = if (clip.words.isNotEmpty()) {
      clip.words
    } else {
      // Auto-partition words across duration
      val tokens = clip.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
      if (tokens.isNotEmpty()) {
        val wordDur = clip.durationMs / tokens.size
        tokens.mapIndexed { idx, token ->
          WordTiming(
            word = token,
            startMs = idx * wordDur,
            durationMs = wordDur
          )
        }
      } else emptyList()
    }

    val activeIdx = resolvedWords.indexOfFirst { w ->
      relTime >= w.startMs && relTime < (w.startMs + w.durationMs)
    }.let {
      if (it == -1 && resolvedWords.isNotEmpty() && relTime >= clip.durationMs) {
        resolvedWords.lastIndex
      } else it
    }

    return EvaluatedTextState(
      visibleText = visibleText,
      posX = animPosX,
      posY = animPosY,
      scale = clip.scale * animScale,
      rotation = animRotation,
      opacity = (clip.opacity * animAlpha).coerceIn(0f, 1f),
      activeWordIndex = activeIdx,
      words = resolvedWords
    )
  }

  fun draw(
    canvas: Canvas,
    clip: TextClip,
    currentPosMs: Long,
    width: Int,
    height: Int,
    context: Context
  ) {
    if (width <= 0 || height <= 0 || clip.opacity <= 0f) return

    val state = evaluateAnimation(clip, currentPosMs)
    if (state.opacity <= 0f || state.visibleText.isEmpty()) return

    // Reference scaling base (360 width standard)
    val scaleFactor = width.toFloat() / 360f
    val baseFontSize = clip.fontSizeSp * scaleFactor * state.scale

    val typeface = FontManager.loadTypeface(
      context = context,
      fontFamily = clip.fontFamily,
      customFontPath = clip.customFontPath,
      fontWeight = if (clip.subtitleStyle.equals("Bold", true)) 900 else clip.fontWeight,
      isItalic = clip.isItalic
    )

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      this.textSize = baseFontSize
      this.letterSpacing = clip.letterSpacing / 10f
      this.color = clip.textColor.toInt()
      this.alpha = (state.opacity * 255).toInt().coerceIn(0, 255)
    }

    // Alignment setup
    paint.textAlign = when (clip.alignment.lowercase()) {
      "left" -> Paint.Align.LEFT
      "right" -> Paint.Align.RIGHT
      else -> Paint.Align.CENTER
    }

    val centerX = (width / 2f) + (state.posX * width / 2f)
    val centerY = (height / 2f) + (state.posY * height / 2f)

    canvas.save()
    canvas.translate(centerX, centerY)
    canvas.rotate(state.rotation)

    val isKaraoke = clip.subtitleStyle.equals("Karaoke", true)
    val isHighlightWord = clip.subtitleStyle.equals("HighlightWord", true) || clip.subtitleStyle.equals("Highlight word", true)
    val isAnimatedCaptions = clip.subtitleStyle.equals("Animated", true) || clip.subtitleStyle.equals("Animated captions", true)

    val lines = state.visibleText.split("\n")
    val fontMetrics = paint.fontMetrics
    val lineHeight = (fontMetrics.descent - fontMetrics.ascent) * clip.lineSpacing
    val totalTextHeight = lines.size * lineHeight

    // Measure bounding box for background
    var maxLineWidth = 0f
    lines.forEach { line ->
      val w = paint.measureText(line)
      if (w > maxLineWidth) maxLineWidth = w
    }

    val padX = clip.bgPadding * scaleFactor
    val padY = (clip.bgPadding * 0.7f) * scaleFactor

    val bgLeft = when (clip.alignment.lowercase()) {
      "left" -> -padX
      "right" -> -maxLineWidth - padX
      else -> -maxLineWidth / 2f - padX
    }
    val bgTop = -totalTextHeight / 2f - padY
    val bgRight = bgLeft + maxLineWidth + (padX * 2)
    val bgBottom = bgTop + totalTextHeight + (padY * 2)

    // 1. Draw Background
    if (clip.hasBackground || clip.subtitleStyle.equals("Bold", true)) {
      val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (clip.hasBackground) clip.backgroundColor.toInt() else 0xCC000000.toInt()
        val bgAlpha = (state.opacity * (Color.alpha(color) / 255f) * 255).toInt().coerceIn(0, 255)
        alpha = bgAlpha
      }
      val cornerRad = clip.cornerRadius * scaleFactor
      canvas.drawRoundRect(RectF(bgLeft, bgTop, bgRight, bgBottom), cornerRad, cornerRad, bgPaint)
    }

    // 2. Draw Text Lines
    val startY = -totalTextHeight / 2f - fontMetrics.ascent

    if ((isHighlightWord || isKaraoke || isAnimatedCaptions) && state.words.isNotEmpty()) {
      drawWordLevelSubtitle(
        canvas = canvas,
        clip = clip,
        state = state,
        paint = paint,
        scaleFactor = scaleFactor,
        startY = startY,
        lineHeight = lineHeight,
        isKaraoke = isKaraoke,
        isHighlight = isHighlightWord,
        isAnimated = isAnimatedCaptions
      )
    } else {
      lines.forEachIndexed { i, line ->
        val y = startY + (i * lineHeight)
        drawSingleLineText(canvas, clip, line, 0f, y, paint, scaleFactor, state.opacity)
      }
    }

    canvas.restore()
  }

  private fun drawSingleLineText(
    canvas: Canvas,
    clip: TextClip,
    text: String,
    x: Float,
    y: Float,
    paint: Paint,
    scaleFactor: Float,
    opacity: Float
  ) {
    // Shadow
    if (clip.hasShadow || clip.subtitleStyle.equals("Classic", true)) {
      val sColor = if (clip.hasShadow) clip.shadowColor.toInt() else 0xDD000000.toInt()
      val sBlur = if (clip.hasShadow) clip.shadowBlur * scaleFactor else 4f * scaleFactor
      val sOffX = if (clip.hasShadow) clip.shadowOffsetX * scaleFactor else 2f * scaleFactor
      val sOffY = if (clip.hasShadow) clip.shadowOffsetY * scaleFactor else 2f * scaleFactor
      paint.setShadowLayer(sBlur, sOffX, sOffY, sColor)
    } else {
      paint.clearShadowLayer()
    }

    // Stroke
    val effectiveStrokeWidth = if (clip.subtitleStyle.equals("Bold", true)) {
      max(clip.strokeWidth, 3.5f) * scaleFactor
    } else {
      clip.strokeWidth * scaleFactor
    }

    if (effectiveStrokeWidth > 0f) {
      val strokePaint = Paint(paint).apply {
        style = Paint.Style.STROKE
        strokeWidth = effectiveStrokeWidth
        color = clip.strokeColor.toInt()
        alpha = (opacity * 255).toInt().coerceIn(0, 255)
      }
      canvas.drawText(text, x, y, strokePaint)
    }

    // Gradient or Solid Fill
    if (clip.hasGradient) {
      val textWidth = paint.measureText(text)
      val bounds = Rect()
      paint.getTextBounds(text, 0, text.length, bounds)

      val (x0, y0, x1, y1) = when (clip.gradientDirection.lowercase()) {
        "vertical" -> listOf(0f, y - bounds.height(), 0f, y)
        "diagonal" -> listOf(-textWidth / 2f, y - bounds.height(), textWidth / 2f, y)
        else -> listOf(-textWidth / 2f, y, textWidth / 2f, y)
      }
      paint.shader = LinearGradient(
        x0, y0, x1, y1,
        clip.gradientColorStart.toInt(),
        clip.gradientColorEnd.toInt(),
        Shader.TileMode.CLAMP
      )
    } else {
      paint.shader = null
      paint.color = if (clip.subtitleStyle.equals("Bold", true) && clip.textColor == 0xFFFFFFFF) {
        0xFFFFE600.toInt() // Vibrant yellow for bold captions
      } else {
        clip.textColor.toInt()
      }
    }

    paint.style = Paint.Style.FILL
    paint.alpha = (opacity * 255).toInt().coerceIn(0, 255)
    canvas.drawText(text, x, y, paint)
    paint.clearShadowLayer()
  }

  private fun drawWordLevelSubtitle(
    canvas: Canvas,
    clip: TextClip,
    state: EvaluatedTextState,
    paint: Paint,
    scaleFactor: Float,
    startY: Float,
    lineHeight: Float,
    isKaraoke: Boolean,
    isHighlight: Boolean,
    isAnimated: Boolean
  ) {
    val words = state.words
    val totalWidth = words.sumOf { paint.measureText(it.word).toDouble() }.toFloat() +
        (words.size - 1) * paint.measureText(" ")

    var currentX = -totalWidth / 2f
    val spaceWidth = paint.measureText(" ")

    words.forEachIndexed { idx, wordTiming ->
      val word = wordTiming.word
      val wordWidth = paint.measureText(word)
      val isActive = idx == state.activeWordIndex
      val isPast = state.activeWordIndex >= 0 && idx < state.activeWordIndex

      val wordPaint = Paint(paint)

      var wordScale = 1.0f
      if (isActive) {
        wordPaint.color = clip.highlightColor.toInt()
        if (isAnimated) {
          wordScale = 1.15f
        }
      } else if (isKaraoke) {
        if (isPast) {
          wordPaint.color = clip.textColor.toInt()
          wordPaint.alpha = (state.opacity * 255).toInt().coerceIn(0, 255)
        } else {
          // Future word dimmed
          wordPaint.color = clip.textColor.toInt()
          wordPaint.alpha = (state.opacity * 130).toInt().coerceIn(0, 255)
        }
      } else if (isHighlight) {
        wordPaint.color = clip.textColor.toInt()
      }

      // Stroke for word
      val effectiveStrokeWidth = max(clip.strokeWidth, 2.5f) * scaleFactor
      val strokePaint = Paint(wordPaint).apply {
        style = Paint.Style.STROKE
        strokeWidth = effectiveStrokeWidth
        color = clip.strokeColor.toInt()
      }

      canvas.save()
      val wordCenterX = currentX + (wordWidth / 2f)
      canvas.translate(wordCenterX, startY)
      if (wordScale != 1.0f) {
        canvas.scale(wordScale, wordScale)
      }

      canvas.drawText(word, -wordWidth / 2f, 0f, strokePaint)
      canvas.drawText(word, -wordWidth / 2f, 0f, wordPaint)
      canvas.restore()

      currentX += wordWidth + spaceWidth
    }
  }

  fun renderToBitmap(
    clip: TextClip,
    currentPosMs: Long,
    width: Int,
    height: Int,
    context: Context
  ): Bitmap {
    val bitmap = Bitmap.createBitmap(max(width, 64), max(height, 64), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    draw(canvas, clip, currentPosMs, width, height, context)
    return bitmap
  }
}
