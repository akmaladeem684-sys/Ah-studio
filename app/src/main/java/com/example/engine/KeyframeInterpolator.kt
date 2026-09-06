package com.example.engine

import com.example.domain.model.AudioClip
import com.example.domain.model.ClipKeyframe
import com.example.domain.model.KeyframeInterpolation
import com.example.domain.model.VideoClip
import kotlin.math.abs

data class InterpolatedClipTransform(
  val scaleX: Float,
  val scaleY: Float,
  val rotation: Float,
  val posX: Float,
  val posY: Float,
  val opacity: Float,
  val volume: Float = 1.0f,
  val blur: Float = 0.0f,
  val brightness: Float = 0.0f,
  val contrast: Float = 1.0f,
  val saturation: Float = 1.0f,
  val effectParam: Float = 0.0f
) {
  // Legacy / convenience uniform scale
  val scale: Float get() = (scaleX + scaleY) / 2f

  constructor(
    scale: Float,
    rotation: Float,
    posX: Float,
    posY: Float,
    opacity: Float
  ) : this(
    scaleX = scale,
    scaleY = scale,
    rotation = rotation,
    posX = posX,
    posY = posY,
    opacity = opacity,
    volume = 1.0f,
    blur = 0.0f,
    brightness = 0.0f,
    contrast = 1.0f,
    saturation = 1.0f,
    effectParam = 0.0f
  )
}

object KeyframeInterpolator {

  fun interpolate(clip: VideoClip, relTimeMs: Long): InterpolatedClipTransform {
    val keyframes = clip.keyframes.sortedBy { it.timeMs }
    if (keyframes.isEmpty()) {
      return InterpolatedClipTransform(
        scaleX = clip.cropScale,
        scaleY = clip.cropScale,
        rotation = clip.rotationDegrees.toFloat(),
        posX = clip.cropOffsetX,
        posY = clip.cropOffsetY,
        opacity = clip.opacity,
        volume = clip.volume,
        blur = 0.0f,
        brightness = 0.0f,
        contrast = 1.0f,
        saturation = 1.0f,
        effectParam = 0.0f
      )
    }

    if (relTimeMs <= keyframes.first().timeMs) {
      val first = keyframes.first()
      return keyframeToTransform(first)
    }
    if (relTimeMs >= keyframes.last().timeMs) {
      val last = keyframes.last()
      return keyframeToTransform(last)
    }

    // Find bounding keyframes
    var before = keyframes.first()
    var after = keyframes.last()
    for (i in 0 until keyframes.size - 1) {
      if (relTimeMs >= keyframes[i].timeMs && relTimeMs <= keyframes[i + 1].timeMs) {
        before = keyframes[i]
        after = keyframes[i + 1]
        break
      }
    }

    val range = (after.timeMs - before.timeMs).toFloat().coerceAtLeast(1f)
    val rawT = ((relTimeMs - before.timeMs) / range).coerceIn(0f, 1f)
    val factor = computeFactor(before.interpolation, before.customCurvePoints, rawT)

    return InterpolatedClipTransform(
      scaleX = lerp(before.scaleX, after.scaleX, factor),
      scaleY = lerp(before.scaleY, after.scaleY, factor),
      rotation = lerp(before.rotation, after.rotation, factor),
      posX = lerp(before.posX, after.posX, factor),
      posY = lerp(before.posY, after.posY, factor),
      opacity = lerp(before.opacity, after.opacity, factor).coerceIn(0f, 1f),
      volume = lerp(before.volume, after.volume, factor).coerceAtLeast(0f),
      blur = lerp(before.blur, after.blur, factor).coerceIn(0f, 1f),
      brightness = lerp(before.brightness, after.brightness, factor).coerceIn(-1f, 1f),
      contrast = lerp(before.contrast, after.contrast, factor).coerceAtLeast(0f),
      saturation = lerp(before.saturation, after.saturation, factor).coerceAtLeast(0f),
      effectParam = lerp(before.effectParam, after.effectParam, factor).coerceIn(0f, 1f)
    )
  }

  fun interpolateVolume(keyframes: List<ClipKeyframe>, relTimeMs: Long, defaultVolume: Float = 1.0f): Float {
    if (keyframes.isEmpty()) return defaultVolume
    val sorted = keyframes.sortedBy { it.timeMs }
    if (relTimeMs <= sorted.first().timeMs) return sorted.first().volume
    if (relTimeMs >= sorted.last().timeMs) return sorted.last().volume

    var before = sorted.first()
    var after = sorted.last()
    for (i in 0 until sorted.size - 1) {
      if (relTimeMs >= sorted[i].timeMs && relTimeMs <= sorted[i + 1].timeMs) {
        before = sorted[i]
        after = sorted[i + 1]
        break
      }
    }

    val range = (after.timeMs - before.timeMs).toFloat().coerceAtLeast(1f)
    val rawT = ((relTimeMs - before.timeMs) / range).coerceIn(0f, 1f)
    val factor = computeFactor(before.interpolation, before.customCurvePoints, rawT)
    return lerp(before.volume, after.volume, factor).coerceAtLeast(0f)
  }

  fun interpolateVolume(clip: AudioClip, relTimeMs: Long): Float {
    return interpolateVolume(clip.keyframes, relTimeMs, clip.volume)
  }

  fun computeFactor(
    interpolation: KeyframeInterpolation,
    customCurvePoints: List<Float>?,
    t: Float
  ): Float {
    val clampedT = t.coerceIn(0f, 1f)
    return when (interpolation) {
      KeyframeInterpolation.LINEAR -> clampedT
      KeyframeInterpolation.EASE_IN -> clampedT * clampedT * clampedT
      KeyframeInterpolation.EASE_OUT -> 1f - (1f - clampedT) * (1f - clampedT) * (1f - clampedT)
      KeyframeInterpolation.EASE_IN_OUT -> {
        if (clampedT < 0.5f) {
          4f * clampedT * clampedT * clampedT
        } else {
          (1.0 - Math.pow((-2.0 * clampedT + 2.0), 3.0) / 2.0).toFloat()
        }
      }
      KeyframeInterpolation.CUSTOM_CURVE -> {
        val pts = if (customCurvePoints != null && customCurvePoints.size >= 4) {
          customCurvePoints
        } else {
          listOf(0.42f, 0.0f, 0.58f, 1.0f)
        }
        solveCubicBezier(pts[0], pts[1], pts[2], pts[3], clampedT)
      }
    }
  }

  /**
   * Evaluates cubic bezier Y for a given target X in range [0, 1].
   * P0=(0,0), P1=(p1x, p1y), P2=(p2x, p2y), P3=(1,1).
   */
  fun solveCubicBezier(p1x: Float, p1y: Float, p2x: Float, p2y: Float, targetX: Float): Float {
    if (targetX <= 0f) return 0f
    if (targetX >= 1f) return 1f

    var low = 0f
    var high = 1f
    var t = targetX

    // Binary search / bisection for root x(t) = targetX
    for (i in 0 until 14) {
      val currentX = evaluateBezier1D(p1x, p2x, t)
      if (abs(currentX - targetX) < 0.001f) break
      if (currentX < targetX) {
        low = t
      } else {
        high = t
      }
      t = (low + high) * 0.5f
    }
    return evaluateBezier1D(p1y, p2y, t).coerceIn(0f, 1f)
  }

  private fun evaluateBezier1D(p1: Float, p2: Float, t: Float): Float {
    val oneMinusT = 1f - t
    return 3f * oneMinusT * oneMinusT * t * p1 + 3f * oneMinusT * t * t * p2 + t * t * t
  }

  private fun keyframeToTransform(kf: ClipKeyframe): InterpolatedClipTransform {
    return InterpolatedClipTransform(
      scaleX = kf.scaleX,
      scaleY = kf.scaleY,
      rotation = kf.rotation,
      posX = kf.posX,
      posY = kf.posY,
      opacity = kf.opacity,
      volume = kf.volume,
      blur = kf.blur,
      brightness = kf.brightness,
      contrast = kf.contrast,
      saturation = kf.saturation,
      effectParam = kf.effectParam
    )
  }

  fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
  }
}
