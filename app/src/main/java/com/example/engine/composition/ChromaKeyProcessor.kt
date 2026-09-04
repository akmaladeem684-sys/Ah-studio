package com.example.engine.composition

import android.graphics.Bitmap
import android.graphics.Color
import com.example.domain.model.ChromaKeySettings
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object ChromaKeyProcessor {

  /**
   * Applies chroma key (green screen / custom color keying) to a Bitmap in-place or returns keyed copy.
   */
  fun applyChromaKey(source: Bitmap, settings: ChromaKeySettings): Bitmap {
    if (!settings.enabled) return source

    val targetColor = settings.targetColor.toInt()
    val targetR = Color.red(targetColor)
    val targetG = Color.green(targetColor)
    val targetB = Color.blue(targetColor)

    // Convert target color to normalized UV/chrominance distance or Euclidean RGB
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    val threshold = settings.intensity * 255f * 1.5f
    val smoothing = max(1f, settings.edgeAdjustment * 100f)
    val spill = settings.spillReduction.coerceIn(0f, 1f)

    for (i in pixels.indices) {
      val pixel = pixels[i]
      val a = Color.alpha(pixel)
      if (a == 0) continue

      var r = Color.red(pixel)
      var g = Color.green(pixel)
      var b = Color.blue(pixel)

      // Distance from key color in RGB space
      val dist = sqrt(
        ((r - targetR) * (r - targetR) +
         (g - targetG) * (g - targetG) +
         (b - targetB) * (b - targetB)).toDouble()
      ).toFloat()

      if (dist < threshold) {
        // Full transparent
        pixels[i] = 0
      } else if (dist < threshold + smoothing) {
        // Feather edge
        val alphaFactor = (dist - threshold) / smoothing
        val newAlpha = (a * alphaFactor).toInt().coerceIn(0, 255)

        // Spill suppression: if green screen, clamp green to average of red & blue
        if (spill > 0f && targetG > targetR && targetG > targetB) {
          val maxOther = max(r, b)
          if (g > maxOther) {
            g = (g * (1f - spill) + maxOther * spill).toInt().coerceIn(0, 255)
          }
        }
        pixels[i] = Color.argb(newAlpha, r, g, b)
      } else {
        // Spill suppression on edges
        if (spill > 0f && targetG > targetR && targetG > targetB && g > max(r, b)) {
          val maxOther = max(r, b)
          val despilledG = (g * (1f - spill * 0.5f) + maxOther * (spill * 0.5f)).toInt().coerceIn(0, 255)
          pixels[i] = Color.argb(a, r, despilledG, b)
        }
      }
    }

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    output.setPixels(pixels, 0, width, 0, 0, width, height)
    return output
  }
}
