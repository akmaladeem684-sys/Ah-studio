package com.example.engine.composition

import android.graphics.ColorMatrix
import com.example.domain.model.FilterSettings
import com.example.domain.model.FilterType
import com.example.domain.model.VideoAdjustments

object ColorFilterGenerator {

  /**
   * Generates a combined Android ColorMatrix representing user adjustments
   * (brightness, contrast, saturation, exposure, temperature, tint, etc.)
   * and artistic filter presets (with optional per-clip override).
   */
  fun createCombinedMatrix(
    adjustments: VideoAdjustments,
    filterSettings: FilterSettings,
    clipFilter: FilterSettings? = null
  ): ColorMatrix {
    val result = ColorMatrix()

    // 1. Saturation
    val satMatrix = ColorMatrix()
    satMatrix.setSaturation(adjustments.saturation.coerceIn(0f, 3f))
    result.postConcat(satMatrix)

    // 2. Brightness & Exposure
    val totalBright = (adjustments.brightness + adjustments.exposure) * 100f
    val brightArray = floatArrayOf(
      1f, 0f, 0f, 0f, totalBright,
      0f, 1f, 0f, 0f, totalBright,
      0f, 0f, 1f, 0f, totalBright,
      0f, 0f, 0f, 1f, 0f
    )
    result.postConcat(ColorMatrix(brightArray))

    // 3. Contrast
    val contrast = adjustments.contrast.coerceIn(0.1f, 3f)
    val contrastOffset = (1f - contrast) * 128f
    val contrastArray = floatArrayOf(
      contrast, 0f, 0f, 0f, contrastOffset,
      0f, contrast, 0f, 0f, contrastOffset,
      0f, 0f, contrast, 0f, contrastOffset,
      0f, 0f, 0f, 1f, 0f
    )
    result.postConcat(ColorMatrix(contrastArray))

    // 4. Temperature (Warm vs Cool)
    if (adjustments.temperature != 0f) {
      val temp = adjustments.temperature.coerceIn(-1f, 1f)
      val rOffset = if (temp > 0) temp * 40f else 0f
      val bOffset = if (temp < 0) -temp * 40f else 0f
      val tempArray = floatArrayOf(
        1f, 0f, 0f, 0f, rOffset,
        0f, 1f, 0f, 0f, rOffset * 0.5f,
        0f, 0f, 1f, 0f, bOffset,
        0f, 0f, 0f, 1f, 0f
      )
      result.postConcat(ColorMatrix(tempArray))
    }

    // 5. Tint (Green vs Magenta)
    if (adjustments.tint != 0f) {
      val tint = adjustments.tint.coerceIn(-1f, 1f)
      val gOffset = if (tint < 0) -tint * 35f else 0f
      val rbOffset = if (tint > 0) tint * 25f else 0f
      val tintArray = floatArrayOf(
        1f, 0f, 0f, 0f, rbOffset,
        0f, 1f, 0f, 0f, gOffset,
        0f, 0f, 1f, 0f, rbOffset,
        0f, 0f, 0f, 1f, 0f
      )
      result.postConcat(ColorMatrix(tintArray))
    }

    // 6. Active Filter Preset (Clip override if set, otherwise timeline filter)
    val activeFilter = if (clipFilter != null && clipFilter.type != FilterType.NONE) {
      clipFilter
    } else {
      filterSettings
    }

    val filterMatrix = getFilterMatrix(activeFilter.type, activeFilter.intensity)
    if (filterMatrix != null) {
      result.postConcat(filterMatrix)
    }

    return result
  }

  /**
   * Returns a ColorMatrix for a specific filter type and intensity (0.0 to 1.0).
   */
  fun getFilterMatrix(type: FilterType, intensity: Float = 1.0f): ColorMatrix? {
    if (type == FilterType.NONE || intensity <= 0f) return null

    val baseMatrix = when (type) {
      // --- Professional Enhancement Filters ---
      FilterType.FOUR_K -> {
        // 4K Ultra HD: High micro-contrast, crisp midtones, boosted sharpness and pure luminous dynamic range
        ColorMatrix(
          floatArrayOf(
            1.22f, 0.00f, 0.00f, 0f, -8f,
            0.00f, 1.22f, 0.00f, 0f, -8f,
            0.00f, 0.00f, 1.22f, 0f, -8f,
            0.00f, 0.00f, 0.00f, 1f, 0f
          )
        )
      }
      FilterType.BLACKLIGHT_FIX -> {
        // Backlight / Blacklight Fix: Shadow lift (+32 offset), controlled highlights, skin-tone warming
        ColorMatrix(
          floatArrayOf(
            1.08f, 0.00f, 0.00f, 0f, 28f,
            0.00f, 1.05f, 0.00f, 0f, 24f,
            0.00f, 0.00f, 1.02f, 0f, 20f,
            0.00f, 0.00f, 0.00f, 1f, 0f
          )
        )
      }
      FilterType.ENHANCE -> {
        // AI Auto Enhance: Vibrant color pop, enhanced midtone contrast, balanced dynamic clarity
        ColorMatrix(
          floatArrayOf(
            1.16f, 0.02f, 0.00f, 0f, 8f,
            0.01f, 1.18f, 0.01f, 0f, 8f,
            0.00f, 0.02f, 1.15f, 0f, 10f,
            0.00f, 0.00f, 0.00f, 1f, 0f
          )
        )
      }
      FilterType.HDR -> {
        // High Dynamic Range: Deep rich blacks, bright luminous highlights, punchy vivid color spectrum
        ColorMatrix(
          floatArrayOf(
            1.28f, 0.00f, 0.00f, 0f, -14f,
            0.00f, 1.28f, 0.00f, 0f, -14f,
            0.00f, 0.00f, 1.30f, 0f, -12f,
            0.00f, 0.00f, 0.00f, 1f, 0f
          )
        )
      }
      FilterType.GLOW -> {
        // Dreamy Glow & Bloom: Raised black floor, diffused highlight luminance, ethereal pastel warmth
        ColorMatrix(
          floatArrayOf(
            1.08f, 0.05f, 0.02f, 0f, 18f,
            0.02f, 1.06f, 0.02f, 0f, 16f,
            0.02f, 0.04f, 1.10f, 0f, 20f,
            0.00f, 0.00f, 0.00f, 1f, 0f
          )
        )
      }
      FilterType.FOCUS -> {
        // Focus: Micro-contrast punch, edge separation, crisp subject isolation
        ColorMatrix(
          floatArrayOf(
            1.25f, -0.05f, -0.05f, 0f, -6f,
            -0.05f, 1.25f, -0.05f, 0f, -6f,
            -0.05f, -0.05f, 1.25f, 0f, -6f,
            0.00f, 0.00f, 0.00f, 1f, 0f
          )
        )
      }
      FilterType.QUALITY_RESTORATION -> {
        // Quality Restoration: Balanced exposure recovery, noise-smoothing curve, clean midtone fidelity
        ColorMatrix(
          floatArrayOf(
            1.10f, 0.01f, 0.01f, 0f, 14f,
            0.01f, 1.10f, 0.01f, 0f, 14f,
            0.01f, 0.01f, 1.10f, 0f, 14f,
            0.00f, 0.00f, 0.00f, 1f, 0f
          )
        )
      }
      FilterType.GOLDEN_AUTUMN -> {
        // Golden Autumn: Rich seasonal amber, warm golden honey, auburn and foliage glow
        ColorMatrix(
          floatArrayOf(
            1.26f, 0.08f, 0.00f, 0f, 26f,
            0.04f, 1.12f, 0.00f, 0f, 12f,
            0.00f, 0.00f, 0.76f, 0f, -16f,
            0.00f, 0.00f, 0.00f, 1f, 0f
          )
        )
      }
      FilterType.OCEANIC_VIEW -> {
        // Oceanic View: Deep azure and aquamarine sea tones, vibrant sky blue, clean sea breeze atmosphere
        ColorMatrix(
          floatArrayOf(
            0.82f, 0.00f, 0.00f, 0f, -8f,
            0.00f, 1.16f, 0.08f, 0f, 14f,
            0.04f, 0.12f, 1.34f, 0f, 28f,
            0.00f, 0.00f, 0.00f, 1f, 0f
          )
        )
      }
      FilterType.ALMOND -> {
        // Almond: Creamy warm matte neutral aesthetic, soft vintage pastel desaturation, cozy editorial look
        ColorMatrix(
          floatArrayOf(
            1.14f, 0.06f, 0.02f, 0f, 18f,
            0.04f, 1.08f, 0.02f, 0f, 14f,
            0.02f, 0.04f, 0.94f, 0f, 8f,
            0.00f, 0.00f, 0.00f, 1f, 0f
          )
        )
      }
      FilterType.SUNLIGHT_ORANGE_BLUE -> {
        // Sunlight Orange Blue: Dual-tone cinematic grading (warm golden sunlight highlights vs deep oceanic teal shadows)
        ColorMatrix(
          floatArrayOf(
            1.24f, 0.00f, 0.00f, 0f, 20f,
            0.00f, 1.04f, 0.00f, 0f, 0f,
            -0.08f, 0.10f, 1.28f, 0f, 22f,
            0.00f, 0.00f, 0.00f, 1f, 0f
          )
        )
      }

      // --- Classic Presets ---
      FilterType.BLACK_AND_WHITE -> {
        ColorMatrix().apply { setSaturation(0f) }
      }
      FilterType.SATURATION -> {
        ColorMatrix().apply { setSaturation(1.85f) }
      }
      FilterType.CINEMATIC -> {
        ColorMatrix(
          floatArrayOf(
            1.2f, 0f, 0f, 0f, 10f,
            0f, 1.05f, 0f, 0f, 5f,
            0f, 0.1f, 1.15f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      }
      FilterType.VINTAGE -> {
        ColorMatrix(
          floatArrayOf(
            0.9f, 0.1f, 0.1f, 0f, 25f,
            0.1f, 0.8f, 0.1f, 0f, 15f,
            0.1f, 0.1f, 0.6f, 0f, 5f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      }
      FilterType.WARM -> {
        ColorMatrix(
          floatArrayOf(
            1.15f, 0f, 0f, 0f, 20f,
            0f, 1.05f, 0f, 0f, 10f,
            0f, 0f, 0.85f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      }
      FilterType.COOL -> {
        ColorMatrix(
          floatArrayOf(
            0.85f, 0f, 0f, 0f, -10f,
            0f, 1.0f, 0f, 0f, 0f,
            0f, 0f, 1.2f, 0f, 25f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      }
      FilterType.PORTRAIT -> {
        ColorMatrix(
          floatArrayOf(
            1.05f, 0f, 0f, 0f, 12f,
            0f, 1.02f, 0f, 0f, 8f,
            0f, 0f, 0.98f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      }
      FilterType.FILM -> {
        ColorMatrix(
          floatArrayOf(
            1.0f, 0.05f, 0.05f, 0f, 10f,
            0.05f, 0.95f, 0.05f, 0f, 10f,
            0.05f, 0.05f, 0.85f, 0f, 15f,
            0.0f, 0.0f, 0.0f, 1f, 0f
          )
        )
      }
      FilterType.RETRO -> {
        ColorMatrix(
          floatArrayOf(
            1.1f, 0.1f, 0.2f, 0f, 15f,
            0.1f, 0.9f, 0.1f, 0f, 5f,
            0.2f, 0.1f, 1.2f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      }
      FilterType.NATURE -> {
        ColorMatrix(
          floatArrayOf(
            1.0f, 0f, 0f, 0f, 0f,
            0f, 1.2f, 0f, 0f, 15f,
            0f, 0f, 1.05f, 0f, 5f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      }
      FilterType.FOOD -> {
        ColorMatrix(
          floatArrayOf(
            1.2f, 0f, 0f, 0f, 20f,
            0f, 1.1f, 0f, 0f, 10f,
            0f, 0f, 0.9f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      }
      FilterType.TRAVEL -> {
        ColorMatrix(
          floatArrayOf(
            1.1f, 0f, 0.05f, 0f, 10f,
            0f, 1.15f, 0f, 0f, 10f,
            0.05f, 0f, 1.2f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      }
      FilterType.SOCIAL_MEDIA -> {
        ColorMatrix(
          floatArrayOf(
            1.2f, 0f, 0f, 0f, 5f,
            0f, 1.2f, 0f, 0f, 5f,
            0f, 0f, 1.2f, 0f, 5f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      }
      else -> null
    } ?: return null

    if (intensity >= 0.99f) return baseMatrix

    // Blend baseMatrix with Identity according to intensity
    val identity = ColorMatrix()
    val blended = ColorMatrix()
    val baseArr = baseMatrix.array
    val idArr = identity.array
    val outArr = FloatArray(20)
    for (i in 0 until 20) {
      outArr[i] = idArr[i] + (baseArr[i] - idArr[i]) * intensity
    }
    blended.set(outArr)
    return blended
  }

  /**
   * Helper returning FloatArray (20 floats) for Compose ColorFilter / ColorMatrix.
   */
  fun getFilterMatrixArray(type: FilterType, intensity: Float = 1.0f): FloatArray {
    val matrix = getFilterMatrix(type, intensity) ?: ColorMatrix()
    return matrix.array
  }
}
