package com.example.engine

import com.example.domain.model.ClipKeyframe
import com.example.domain.model.VideoClip

data class InterpolatedClipTransform(
  val scale: Float,
  val rotation: Float,
  val posX: Float,
  val posY: Float,
  val opacity: Float
)

object KeyframeInterpolator {

  fun interpolate(clip: VideoClip, relTimeMs: Long): InterpolatedClipTransform {
    val keyframes = clip.keyframes.sortedBy { it.timeMs }
    if (keyframes.isEmpty()) {
      return InterpolatedClipTransform(
        scale = clip.cropScale,
        rotation = clip.rotationDegrees.toFloat(),
        posX = clip.cropOffsetX,
        posY = clip.cropOffsetY,
        opacity = 1.0f
      )
    }

    if (relTimeMs <= keyframes.first().timeMs) {
      val first = keyframes.first()
      return InterpolatedClipTransform(first.scale, first.rotation, first.posX, first.posY, first.opacity)
    }
    if (relTimeMs >= keyframes.last().timeMs) {
      val last = keyframes.last()
      return InterpolatedClipTransform(last.scale, last.rotation, last.posX, last.posY, last.opacity)
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
    var factor = ((relTimeMs - before.timeMs) / range).coerceIn(0f, 1f)

    if (before.interpolation == "SmoothEase") {
      // Cubic ease in-out
      factor = if (factor < 0.5f) {
        4 * factor * factor * factor
      } else {
        (1.0 - Math.pow((-2.0 * factor + 2.0), 3.0) / 2.0).toFloat()
      }
    }

    return InterpolatedClipTransform(
      scale = lerp(before.scale, after.scale, factor),
      rotation = lerp(before.rotation, after.rotation, factor),
      posX = lerp(before.posX, after.posX, factor),
      posY = lerp(before.posY, after.posY, factor),
      opacity = lerp(before.opacity, after.opacity, factor)
    )
  }

  private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
  }
}
