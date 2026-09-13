package com.example.engine.composition

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.Log
import com.example.domain.model.EffectClip
import com.example.domain.model.EffectType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Unified Video Effect Rendering Engine for CapCut Studio.
 * Renders live on Compose Preview canvas and composition export canvas.
 */
object VideoEffectRenderer {

  private const val TAG = "VideoEffectRenderer"

  data class EffectMotionTransform(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val alpha: Float = 1f
  )

  /**
   * Calculates the combined motion/transform properties from all active motion effects.
   */
  fun calculateMotionTransform(
    activeEffects: List<EffectClip>,
    currentPosMs: Long
  ): EffectMotionTransform {
    var sX = 1f
    var sY = 1f
    var rot = 0f
    var tX = 0f
    var tY = 0f
    var alp = 1f

    for (effect in activeEffects) {
      val relTime = (currentPosMs - effect.timelineStartMs).coerceAtLeast(0L)
      val intensity = effect.intensity.coerceIn(0f, 1f)
      if (intensity <= 0.01f) continue

      when (effect.effectType) {
        EffectType.SHAKE -> {
          val phase = (relTime % 1000L).toFloat()
          val jx = (sin(phase * 0.05f) * 0.035f + sin(phase * 0.13f) * 0.02f) * intensity
          val jy = (cos(phase * 0.07f) * 0.035f + sin(phase * 0.17f) * 0.02f) * intensity
          val r = sin(phase * 0.04f) * 4f * intensity
          tX += jx
          tY += jy
          rot += r
        }
        EffectType.ZOOM -> {
          val pulse = 1.05f + (sin(relTime * 0.006f) * 0.15f * intensity)
          sX *= pulse
          sY *= pulse
        }
        EffectType.SKATER_ZOOM -> {
          val punch = 1f + (0.3f + sin(relTime * 0.008f) * 0.15f) * intensity
          sX *= punch
          sY *= punch
        }
        EffectType.VERTIGO_DOLLY -> {
          val progress = ((relTime % 2500L).toFloat() / 2500f)
          val dolly = 1f + (progress * 0.35f * intensity)
          sX *= dolly
          sY *= dolly
        }
        EffectType.SPIN -> {
          val spinAngle = ((relTime % 3000L).toFloat() / 3000f) * 360f * intensity
          rot += spinAngle
        }
        EffectType.CAMERA_MOVEMENT -> {
          tX += sin(relTime * 0.002f) * 0.045f * intensity
          tY += cos(relTime * 0.003f) * 0.035f * intensity
        }
        EffectType.MIRROR -> {
          sX *= -1f
        }
        else -> {}
      }
    }

    return EffectMotionTransform(
      scaleX = sX,
      scaleY = sY,
      rotation = rot,
      translationX = tX,
      translationY = tY,
      alpha = alp.coerceIn(0f, 1f)
    )
  }

  /**
   * Calculates the combined ColorMatrix from all active color-altering effects.
   */
  fun calculateEffectColorMatrix(
    activeEffects: List<EffectClip>,
    currentPosMs: Long
  ): ColorMatrix? {
    var combinedMatrix: ColorMatrix? = null

    for (effect in activeEffects) {
      val relTime = (currentPosMs - effect.timelineStartMs).coerceAtLeast(0L)
      val intensity = effect.intensity.coerceIn(0f, 1f)
      if (intensity <= 0.01f) continue

      val mat: ColorMatrix? = when (effect.effectType) {
        EffectType.THERMAL_CAMERA -> {
          ColorMatrix(floatArrayOf(
            -1.2f * intensity + (1f - intensity), 2.0f * intensity, 0.2f * intensity, 0f, 20f * intensity,
            -0.5f * intensity, -1.0f * intensity + (1f - intensity), 2.5f * intensity, 0f, 40f * intensity,
            2.5f * intensity, -0.8f * intensity, -0.5f * intensity + (1f - intensity), 0f, 30f * intensity,
            0f, 0f, 0f, 1f, 0f
          ))
        }
        EffectType.BLUEPRINT_CAD -> {
          ColorMatrix(floatArrayOf(
            0.1f * (1f - intensity), 0.1f, 0.2f, 0f, 10f * intensity,
            0.3f, 0.5f * intensity + 0.3f, 0.4f, 0f, 60f * intensity,
            0.6f, 0.8f * intensity + 0.5f, 1.2f * intensity + 0.8f, 0f, 120f * intensity,
            0f, 0f, 0f, 1f, 0f
          ))
        }
        EffectType.ACID_TRIP -> {
          val huePhase = (relTime % 2400L).toFloat() / 2400f * 360f
          val cm = ColorMatrix()
          cm.setRotate(0, huePhase)
          cm.setRotate(1, huePhase * 0.7f)
          cm.setRotate(2, huePhase * 1.3f)
          cm
        }
        EffectType.POP_ART_POSTER -> {
          ColorMatrix(floatArrayOf(
            1.8f * intensity + 1f, 0f, 0f, 0f, 25f * intensity,
            0f, 1.5f * intensity + 1f, 0f, 0f, 10f * intensity,
            0f, 0f, 2.0f * intensity + 1f, 0f, -10f * intensity,
            0f, 0f, 0f, 1f, 0f
          ))
        }
        EffectType.CHARCOAL_DRAW -> {
          val inv = 1f - intensity
          ColorMatrix(floatArrayOf(
            0.33f * intensity + inv, 0.33f * intensity, 0.33f * intensity, 0f, -20f * intensity,
            0.33f * intensity, 0.33f * intensity + inv, 0.33f * intensity, 0f, -20f * intensity,
            0.33f * intensity, 0.33f * intensity, 0.33f * intensity + inv, 0f, -20f * intensity,
            0f, 0f, 0f, 1f, 0f
          ))
        }
        EffectType.PASTEL_DREAM -> {
          ColorMatrix(floatArrayOf(
            0.8f, 0.1f, 0.1f, 0f, 40f * intensity,
            0.1f, 0.75f, 0.15f, 0f, 25f * intensity,
            0.15f, 0.1f, 0.85f, 0f, 45f * intensity,
            0f, 0f, 0f, 1f, 0f
          ))
        }
        EffectType.COLOR_POP_SPLASH -> {
          val cm = ColorMatrix()
          cm.setSaturation(1f + 1.2f * intensity)
          cm
        }
        EffectType.GOLDEN_HOUR -> {
          ColorMatrix(floatArrayOf(
            1f + 0.35f * intensity, 0f, 0f, 0f, 30f * intensity,
            0f, 1f + 0.18f * intensity, 0f, 0f, 15f * intensity,
            0f, 0f, 1f - 0.25f * intensity, 0f, -10f * intensity,
            0f, 0f, 0f, 1f, 0f
          ))
        }
        EffectType.SKELETON_XRAY -> {
          ColorMatrix(floatArrayOf(
            -1.0f * intensity + (1f - intensity), 0f, 0f, 0f, 255f * intensity,
            0f, -0.6f * intensity + (1f - intensity), 0.4f * intensity, 0f, 230f * intensity,
            0f, 0.2f * intensity, -0.4f * intensity + (1f - intensity), 0f, 255f * intensity,
            0f, 0f, 0f, 1f, 0f
          ))
        }
        EffectType.FACE_BEAUTY -> {
          ColorMatrix(floatArrayOf(
            1.08f, 0.02f, 0.02f, 0f, 12f * intensity,
            0.02f, 1.05f, 0.02f, 0f, 8f * intensity,
            0.02f, 0.02f, 1.02f, 0f, 5f * intensity,
            0f, 0f, 0f, 1f, 0f
          ))
        }
        EffectType.GLOW -> {
          ColorMatrix(floatArrayOf(
            1.15f * intensity + (1f - intensity), 0f, 0f, 0f, 25f * intensity,
            0f, 1.15f * intensity + (1f - intensity), 0f, 0f, 25f * intensity,
            0f, 0f, 1.15f * intensity + (1f - intensity), 0f, 25f * intensity,
            0f, 0f, 0f, 1f, 0f
          ))
        }
        else -> null
      }

      if (mat != null) {
        if (combinedMatrix == null) {
          combinedMatrix = ColorMatrix(mat)
        } else {
          combinedMatrix.postConcat(mat)
        }
      }
    }

    return combinedMatrix
  }

  /**
   * Main entry point to render procedural visual effects onto an Android Canvas.
   */
  fun renderEffectsOnCanvas(
    canvas: Canvas,
    activeEffects: List<EffectClip>,
    currentPosMs: Long,
    width: Int,
    height: Int
  ) {
    if (activeEffects.isEmpty() || width <= 0 || height <= 0) return

    for (effect in activeEffects) {
      val relTime = (currentPosMs - effect.timelineStartMs).coerceAtLeast(0L)
      val intensity = effect.intensity.coerceIn(0f, 1f)
      if (intensity <= 0.01f) continue

      try {
        renderSingleEffect(
          canvas = canvas,
          effect = effect,
          intensity = intensity,
          relTime = relTime,
          width = width,
          height = height
        )
      } catch (e: Exception) {
        Log.e(TAG, "Failed rendering effect ${effect.effectType.name}: ${e.message}", e)
      }
    }
  }

  /**
   * Renders an individual visual effect onto the canvas with high fidelity and proper blending.
   */
  fun renderSingleEffect(
    canvas: Canvas,
    effect: EffectClip,
    intensity: Float,
    relTime: Long,
    width: Int,
    height: Int
  ) {
    val w = width.toFloat()
    val h = height.toFloat()
    val cx = w / 2f
    val cy = h / 2f

    when (effect.effectType) {
      // 1. LIGHTING & FLASHES
      EffectType.FLASH -> {
        val cycle = relTime % 400L
        val flashAlpha = if (cycle < 180L) {
          (1f - (cycle / 180f)) * intensity
        } else 0f
        if (flashAlpha > 0.01f) {
          val paint = Paint().apply {
            color = Color.WHITE
            alpha = (flashAlpha * 255).toInt().coerceIn(0, 255)
          }
          canvas.drawRect(0f, 0f, w, h, paint)
        }
      }

      EffectType.STROBE -> {
        val isStrobeOn = (relTime % 160L) < 80L
        if (isStrobeOn) {
          val paint = Paint().apply {
            color = if ((relTime % 320L) < 160L) Color.WHITE else Color.CYAN
            alpha = (intensity * 230).toInt().coerceIn(0, 255)
          }
          canvas.drawRect(0f, 0f, w, h, paint)
        }
      }

      EffectType.LENS_FLARE, EffectType.SOLAR_FLARE -> {
        val flareX = cx + cos(relTime * 0.002f) * (w * 0.35f)
        val flareY = cy * 0.6f + sin(relTime * 0.0025f) * (h * 0.2f)

        // Horizontal Anamorphic Streak
        val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader = LinearGradient(
            0f, flareY, w, flareY,
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#8000C2FF"), Color.WHITE, Color.parseColor("#8000C2FF"), Color.TRANSPARENT),
            floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f),
            Shader.TileMode.CLAMP
          )
          strokeWidth = 6f * intensity
          style = Paint.Style.STROKE
        }
        canvas.drawLine(0f, flareY, w, flareY, streakPaint)

        // Central Radiant Flare Core
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader = RadialGradient(
            flareX, flareY, 120f * intensity,
            intArrayOf(Color.WHITE, Color.parseColor("#AAFFD700"), Color.TRANSPARENT),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
          )
        }
        canvas.drawCircle(flareX, flareY, 120f * intensity, corePaint)

        // Flare Ghost Hexagons
        val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#6600E5FF")
          style = Paint.Style.FILL
        }
        val dx = (cx - flareX) * 0.6f
        val dy = (cy - flareY) * 0.6f
        canvas.drawCircle(cx + dx, cy + dy, 24f * intensity, ghostPaint)
        canvas.drawCircle(cx - dx * 0.5f, cy - dy * 0.5f, 16f * intensity, ghostPaint)
      }

      EffectType.LIGHT_LEAK, EffectType.GOLDEN_HOUR -> {
        val leakX = w * (0.8f + sin(relTime * 0.0015f) * 0.2f)
        val leakY = h * (0.15f + cos(relTime * 0.002f) * 0.15f)
        val leakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader = RadialGradient(
            leakX, leakY, w * 0.85f,
            intArrayOf(
              Color.parseColor("#B3FF7A00"),
              Color.parseColor("#80FF0077"),
              Color.parseColor("#40FFD700"),
              Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.35f, 0.65f, 1f),
            Shader.TileMode.CLAMP
          )
          alpha = (intensity * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawRect(0f, 0f, w, h, leakPaint)
      }

      EffectType.BOKEH -> {
        val bokehPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
        }
        val rng = Random(42)
        for (i in 0 until 18) {
          val speed = 0.0003f + (i * 0.00008f)
          val orbY = (h - ((relTime * speed * h + (i * 70f)) % h))
          val orbX = (w * rng.nextFloat() + sin((relTime + i * 200) * 0.003f) * 30f).coerceIn(0f, w)
          val radius = (18f + rng.nextFloat() * 32f) * intensity
          bokehPaint.color = if (i % 2 == 0) Color.parseColor("#55F59E0B") else Color.parseColor("#55E879F9")
          canvas.drawCircle(orbX, orbY, radius, bokehPaint)
        }
      }

      EffectType.FIRE_SPARK -> {
        val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
        }
        val rng = Random(101)
        for (i in 0 until 35) {
          val speed = 0.0008f + (i * 0.0001f)
          val sy = h - ((relTime * speed * h + (i * 45f)) % h)
          val sx = (w * rng.nextFloat() + sin((relTime + i * 300) * 0.008f) * 40f).coerceIn(0f, w)
          val sRadius = (2.5f + rng.nextFloat() * 4f) * intensity
          sparkPaint.color = if (i % 3 == 0) Color.parseColor("#FFEE4400") else Color.parseColor("#FFFFDD00")
          canvas.drawCircle(sx, sy, sRadius, sparkPaint)
        }
      }

      EffectType.LASER_GRID -> {
        val gridHorizonY = h * 0.65f
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#CC00C2FF")
          strokeWidth = 2f
        }
        // Horizontal perspective lines
        val scroll = (relTime * 0.1f) % 40f
        var y = gridHorizonY
        var step = 8f
        while (y < h) {
          val lineY = (y + scroll).coerceAtMost(h)
          canvas.drawLine(0f, lineY, w, lineY, gridPaint)
          step *= 1.35f
          y += step
        }
        // Vanishing perspective rays
        for (i in -8..8) {
          val bottomX = cx + (i * (w / 7f))
          canvas.drawLine(cx, gridHorizonY, bottomX, h, gridPaint)
        }
      }

      // 2. RETRO, VHS, CRT & GLITCH
      EffectType.GLITCH, EffectType.AI_GLITCH_REALITY -> {
        val rng = Random((relTime / 70L).toInt())
        val glitchCount = (6 * intensity).toInt().coerceAtLeast(2)
        val glitchPaint = Paint().apply {
          style = Paint.Style.FILL
        }

        for (i in 0 until glitchCount) {
          val barY = rng.nextFloat() * h
          val barH = 6f + rng.nextFloat() * 25f
          val offsetX = (rng.nextFloat() - 0.5f) * 60f * intensity

          // Red split strip
          glitchPaint.color = Color.parseColor("#80FF0033")
          canvas.drawRect(offsetX, barY, w + offsetX, barY + barH, glitchPaint)

          // Cyan split strip
          glitchPaint.color = Color.parseColor("#8000FFEA")
          canvas.drawRect(-offsetX, barY + 3f, w - offsetX, barY + barH + 3f, glitchPaint)
        }
      }

      EffectType.RGB_SPLIT -> {
        val splitDist = 12f * intensity * sin(relTime * 0.01f).coerceIn(-1f, 1f)
        val splitPaint = Paint().apply {
          style = Paint.Style.STROKE
          strokeWidth = 4f
        }
        splitPaint.color = Color.parseColor("#66FF0000")
        canvas.drawRect(splitDist, 0f, w + splitDist, h, splitPaint)
        splitPaint.color = Color.parseColor("#6600FFFF")
        canvas.drawRect(-splitDist, 0f, w - splitDist, h, splitPaint)
      }

      EffectType.VHS_VINTAGE -> {
        // VHS Tracking Line
        val trackY = (relTime * 0.15f) % h
        val trackPaint = Paint().apply {
          shader = LinearGradient(
            0f, trackY, 0f, trackY + 18f,
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#80FFFFFF"), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
          )
        }
        canvas.drawRect(0f, trackY, w, trackY + 18f, trackPaint)

        // VHS Tape Noise scanlines
        val scanPaint = Paint().apply {
          color = Color.parseColor("#25FFFFFF")
          strokeWidth = 1f
        }
        var sy = 0f
        while (sy < h) {
          canvas.drawLine(0f, sy, w, sy, scanPaint)
          sy += 6f
        }

        // Retro OSD Timestamp Text
        val osdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          textSize = (h * 0.038f).coerceIn(16f, 32f)
          isFakeBoldText = true
          setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
        val isBlink = (relTime % 1000L) < 600L
        if (isBlink) {
          canvas.drawText("PLAY  ►", w * 0.08f, h * 0.12f, osdPaint)
        }
        val sec = (relTime / 1000L) % 60L
        val min = (relTime / 60000L) % 60L
        val hour = (relTime / 3600000L)
        val timeStr = String.format("SP  %02d:%02d:%02d", hour, min, sec)
        canvas.drawText(timeStr, w * 0.08f, h * 0.92f, osdPaint)
      }

      EffectType.CRT_TV -> {
        val crtPaint = Paint().apply {
          color = Color.parseColor("#44000000")
          strokeWidth = 2f
        }
        var y = 0f
        while (y < h) {
          canvas.drawLine(0f, y, w, y, crtPaint)
          y += 5f
        }
        // Vignette tube border
        val vigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader = RadialGradient(
            cx, cy, w * 0.72f,
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#E6000000")),
            floatArrayOf(0.7f, 1f),
            Shader.TileMode.CLAMP
          )
        }
        canvas.drawRect(0f, 0f, w, h, vigPaint)
      }

      EffectType.VIGNETTE -> {
        val vigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader = RadialGradient(
            cx, cy, (w * 0.7f).coerceAtLeast(10f),
            intArrayOf(Color.TRANSPARENT, Color.BLACK),
            floatArrayOf(0.45f, 1f),
            Shader.TileMode.CLAMP
          )
          alpha = (intensity * 240).toInt().coerceIn(0, 255)
        }
        canvas.drawRect(0f, 0f, w, h, vigPaint)
      }

      EffectType.NOISE -> {
        val noisePaint = Paint().apply {
          color = Color.WHITE
          strokeWidth = 2f
        }
        val rng = Random(relTime.toInt())
        val count = (width * height * 0.00035f * intensity).toInt().coerceIn(50, 600)
        for (i in 0 until count) {
          noisePaint.alpha = rng.nextInt(60, 200)
          val nx = rng.nextFloat() * w
          val ny = rng.nextFloat() * h
          canvas.drawPoint(nx, ny, noisePaint)
        }
      }

      // 3. MOTION, SPEED & SHOCKWAVES
      EffectType.WARP_SPEED -> {
        val warpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          strokeWidth = 3f * intensity
        }
        val rng = Random(77)
        for (i in 0 until 40) {
          val angle = rng.nextFloat() * 6.283185f
          val length = (100f + rng.nextFloat() * 300f) * intensity
          val speedProgress = ((relTime * 0.001f + i * 0.05f) % 1f)
          val r1 = speedProgress * (w * 0.6f)
          val r2 = r1 + length

          val x1 = cx + cos(angle) * r1
          val y1 = cy + sin(angle) * r1
          val x2 = cx + cos(angle) * r2
          val y2 = cy + sin(angle) * r2

          warpPaint.color = if (i % 2 == 0) Color.CYAN else Color.WHITE
          warpPaint.alpha = ((1f - speedProgress) * 220).toInt().coerceIn(0, 255)
          canvas.drawLine(x1, y1, x2, y2, warpPaint)
        }
      }

      EffectType.MANGA_LINE -> {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.BLACK
          strokeWidth = 4f * intensity
        }
        val count = 48
        val rng = Random((relTime / 80L).toInt())
        for (i in 0 until count) {
          val angle = (i.toFloat() / count) * 6.283185f + (rng.nextFloat() - 0.5f) * 0.08f
          val innerR = (w * 0.28f + (rng.nextFloat() * 50f))
          val outerR = w * 0.8f

          val x1 = cx + cos(angle) * innerR
          val y1 = cy + sin(angle) * innerR
          val x2 = cx + cos(angle) * outerR
          val y2 = cy + sin(angle) * outerR

          linePaint.strokeWidth = (2f + rng.nextFloat() * 5f) * intensity
          canvas.drawLine(x1, y1, x2, y2, linePaint)
        }
      }

      EffectType.RIPPLE, EffectType.WAVE -> {
        val rippleRadius = ((relTime % 1400L).toFloat() / 1400f) * (w * 0.7f)
        val alphaNorm = (1f - (rippleRadius / (w * 0.7f))).coerceIn(0f, 1f)
        val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 8f * intensity
          color = Color.parseColor("#00E5FF")
          alpha = (alphaNorm * intensity * 240).toInt().coerceIn(0, 255)
        }
        canvas.drawCircle(cx, cy, rippleRadius, ripplePaint)
        canvas.drawCircle(cx, cy, (rippleRadius * 0.65f), ripplePaint.apply { strokeWidth = 5f * intensity })
      }

      // 4. BODY & FIGURE EFFECTS
      EffectType.BODY_AURA, EffectType.FIRE_AURA -> {
        val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 14f * intensity
          shader = RadialGradient(
            cx, cy, w * 0.45f,
            intArrayOf(
              Color.parseColor("#FFD700"),
              Color.parseColor("#FF4500"),
              Color.TRANSPARENT
            ),
            floatArrayOf(0.4f, 0.75f, 1f),
            Shader.TileMode.CLAMP
          )
        }
        val auraPulse = 1f + sin(relTime * 0.008f) * 0.06f
        canvas.save()
        canvas.scale(auraPulse, auraPulse, cx, cy)
        canvas.drawOval(RectF(cx - w * 0.28f, cy - h * 0.38f, cx + w * 0.28f, cy + h * 0.38f), auraPaint)
        canvas.restore()
      }

      EffectType.NEON_OUTLINE -> {
        val neonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 6f * intensity
          color = Color.parseColor("#00E5FF")
          setShadowLayer(16f * intensity, 0f, 0f, Color.parseColor("#00C2FF"))
        }
        canvas.drawRoundRect(RectF(cx - w * 0.3f, cy - h * 0.38f, cx + w * 0.3f, cy + h * 0.38f), 40f, 40f, neonPaint)
      }

      EffectType.GLOW_EYES -> {
        val eyeSpacing = w * 0.09f
        val eyeY = cy - h * 0.08f
        val leftEyeX = cx - eyeSpacing
        val rightEyeX = cx + eyeSpacing

        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#FF0033")
          setShadowLayer(25f * intensity, 0f, 0f, Color.RED)
        }
        canvas.drawCircle(leftEyeX, eyeY, 12f * intensity, eyePaint)
        canvas.drawCircle(rightEyeX, eyeY, 12f * intensity, eyePaint)

        // Laser Streaks
        val laserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          strokeWidth = 4f * intensity
        }
        canvas.drawLine(leftEyeX - 120f * intensity, eyeY, leftEyeX + 30f, eyeY, laserPaint)
        canvas.drawLine(rightEyeX - 30f, eyeY, rightEyeX + 120f * intensity, eyeY, laserPaint)
      }

      EffectType.ANGEL_WINGS, EffectType.CYBER_WINGS -> {
        val wingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 8f * intensity
          color = if (effect.effectType == EffectType.ANGEL_WINGS) Color.parseColor("#FFEAA7") else Color.parseColor("#9D4EDD")
          setShadowLayer(20f, 0f, 0f, Color.WHITE)
        }

        val wingFlap = sin(relTime * 0.005f) * 15f
        // Left Wing Path
        val leftPath = Path().apply {
          moveTo(cx - 30f, cy)
          cubicTo(cx - w * 0.25f, cy - h * 0.25f + wingFlap, cx - w * 0.45f, cy - h * 0.15f + wingFlap, cx - w * 0.48f, cy + h * 0.1f)
          cubicTo(cx - w * 0.35f, cy + h * 0.05f, cx - w * 0.2f, cy + h * 0.08f, cx - 20f, cy + 20f)
        }
        // Right Wing Path
        val rightPath = Path().apply {
          moveTo(cx + 30f, cy)
          cubicTo(cx + w * 0.25f, cy - h * 0.25f + wingFlap, cx + w * 0.45f, cy - h * 0.15f + wingFlap, cx + w * 0.48f, cy + h * 0.1f)
          cubicTo(cx + w * 0.35f, cy + h * 0.05f, cx + w * 0.2f, cy + h * 0.08f, cx + 20f, cy + 20f)
        }

        canvas.drawPath(leftPath, wingPaint)
        canvas.drawPath(rightPath, wingPaint)
      }

      EffectType.LIGHTNING_BODY, EffectType.AI_SPEED_FORCE -> {
        val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = if (effect.effectType == EffectType.AI_SPEED_FORCE) Color.parseColor("#FFCC00") else Color.parseColor("#00E5FF")
          strokeWidth = 5f * intensity
          setShadowLayer(14f, 0f, 0f, Color.WHITE)
        }

        val rng = Random((relTime / 60L).toInt())
        var startX = cx + (rng.nextFloat() - 0.5f) * (w * 0.6f)
        var startY = 0f
        while (startY < h) {
          val nextX = startX + (rng.nextFloat() - 0.5f) * 60f
          val nextY = startY + 30f + rng.nextFloat() * 40f
          canvas.drawLine(startX, startY, nextX, nextY, boltPaint)
          startX = nextX
          startY = nextY
        }
      }

      EffectType.HEART_TRAIL -> {
        val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#FF3366")
          style = Paint.Style.FILL
        }
        val rng = Random(99)
        for (i in 0 until 14) {
          val speed = 0.0004f + (i * 0.00008f)
          val hy = h - ((relTime * speed * h + (i * 80f)) % h)
          val hx = (w * rng.nextFloat() + sin((relTime + i * 200) * 0.004f) * 35f).coerceIn(0f, w)
          val heartSize = (14f + rng.nextFloat() * 16f) * intensity
          drawHeart(canvas, hx, hy, heartSize, heartPaint)
        }
      }

      EffectType.CYBER_FACE -> {
        val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#00E5FF")
          style = Paint.Style.STROKE
          strokeWidth = 3f * intensity
        }
        // Target Box
        val boxR = w * 0.22f
        canvas.drawCircle(cx, cy, boxR, hudPaint)
        canvas.drawLine(cx - boxR - 20f, cy, cx + boxR + 20f, cy, hudPaint)
        canvas.drawLine(cx, cy - boxR - 20f, cx, cy + boxR + 20f, hudPaint)

        // Coordinates Text
        val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#00E5FF")
          textSize = 14f
          isFakeBoldText = true
        }
        canvas.drawText("TARGET LOCK: 98.4%", cx - boxR, cy - boxR - 12f, txtPaint)
        canvas.drawText("RADAR: ACTIVE", cx - boxR, cy + boxR + 24f, txtPaint)
      }

      // 5. AI EFFECTS
      EffectType.AI_CYBERPUNK_CITY -> {
        // Neon rain streaks
        val rainPaint = Paint().apply {
          color = Color.parseColor("#9900E5FF")
          strokeWidth = 2f
        }
        val rng = Random(33)
        for (i in 0 until 40) {
          val rx = rng.nextFloat() * w
          val ry = (relTime * (0.8f + rng.nextFloat() * 0.5f) + i * 40f) % h
          canvas.drawLine(rx, ry, rx - 10f, ry + 25f, rainPaint)
        }
        // Cyber magenta ground reflection
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader = LinearGradient(
            0f, h * 0.75f, 0f, h,
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#66FF007F")),
            null,
            Shader.TileMode.CLAMP
          )
        }
        canvas.drawRect(0f, h * 0.75f, w, h, glowPaint)
      }

      EffectType.AI_PARTICLE_DISPERSE -> {
        val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#EEF59E0B")
          style = Paint.Style.FILL
        }
        val rng = Random(55)
        for (i in 0 until 60) {
          val progress = ((relTime * 0.0004f + i * 0.02f) % 1f)
          val px = cx + (progress * w * 0.5f) + (rng.nextFloat() - 0.5f) * 80f
          val py = cy + (rng.nextFloat() - 0.5f) * (h * 0.6f) - (progress * 100f)
          particlePaint.alpha = ((1f - progress) * 240).toInt().coerceIn(0, 255)
          canvas.drawCircle(px, py, (2f + rng.nextFloat() * 5f) * intensity, particlePaint)
        }
      }

      EffectType.AI_NEON_TRAIL -> {
        val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 10f * intensity
          color = Color.parseColor("#00F0FF")
          setShadowLayer(20f, 0f, 0f, Color.parseColor("#7000FF"))
        }
        val path = Path()
        var started = false
        for (step in 0..16) {
          val t = relTime * 0.003f + step * 0.35f
          val px = cx + sin(t) * (w * 0.35f)
          val py = cy + cos(t * 0.8f) * (h * 0.35f)
          if (!started) {
            path.moveTo(px, py)
            started = true
          } else {
            path.lineTo(px, py)
          }
        }
        canvas.drawPath(path, ribbonPaint)
      }

      EffectType.AI_SCI_FI_PORTAL -> {
        val portalRadius = (w * 0.28f) * (1f + sin(relTime * 0.006f) * 0.08f)
        val portalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 12f * intensity
          shader = RadialGradient(
            cx, cy, portalRadius,
            intArrayOf(Color.parseColor("#00E5FF"), Color.parseColor("#7B2CBF"), Color.TRANSPARENT),
            floatArrayOf(0.4f, 0.85f, 1f),
            Shader.TileMode.CLAMP
          )
        }
        canvas.drawCircle(cx, cy, portalRadius, portalPaint)
      }

      EffectType.AI_FREEZE_TIME -> {
        // Crystalline ice frame frost
        val frostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#80B8E2F2")
          strokeWidth = 3f
        }
        val count = 28
        for (i in 0 until count) {
          val x = (i.toFloat() / count) * w
          canvas.drawLine(x, 0f, x + 15f, 40f * intensity, frostPaint)
          canvas.drawLine(x, h, x - 15f, h - 40f * intensity, frostPaint)
        }
      }

      EffectType.AI_LIQUID_GOLD, EffectType.AI_GOLDEN_GOD -> {
        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          strokeWidth = 6f * intensity
          color = Color.parseColor("#CCFFD700")
        }
        val count = 24
        val rotOffset = (relTime * 0.0015f)
        for (i in 0 until count) {
          val angle = (i.toFloat() / count) * 6.283185f + rotOffset
          val endX = cx + cos(angle) * (w * 0.8f)
          val endY = cy + sin(angle) * (w * 0.8f)
          canvas.drawLine(cx, cy, endX, endY, rayPaint)
        }
      }

      else -> {
        // Generic fallback luminous ambient overlay for any unlisted effect
        val ambientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader = RadialGradient(
            cx, cy, w * 0.6f,
            intArrayOf(Color.parseColor("#4D00C2FF"), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
          )
          alpha = (intensity * 180).toInt().coerceIn(0, 255)
        }
        canvas.drawRect(0f, 0f, w, h, ambientPaint)
      }
    }
  }

  private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
    val path = Path()
    path.moveTo(cx, cy - size * 0.2f)
    path.cubicTo(cx - size * 0.5f, cy - size * 0.8f, cx - size, cy - size * 0.2f, cx, cy + size * 0.6f)
    path.cubicTo(cx + size, cy - size * 0.2f, cx + size * 0.5f, cy - size * 0.8f, cx, cy - size * 0.2f)
    path.close()
    canvas.drawPath(path, paint)
  }
}
