package com.example.engine.composition

import android.graphics.ColorMatrix
import com.example.domain.model.FilterSettings
import com.example.domain.model.FilterType
import com.example.domain.model.VideoAdjustments
import kotlin.math.cos
import kotlin.math.sin

object ColorFilterGenerator {

  /**
   * Generates a combined Android ColorMatrix representing both user adjustments
   * (brightness, contrast, saturation, exposure, temperature, tint, etc.)
   * and artistic filter presets.
   */
  fun createCombinedMatrix(
    adjustments: VideoAdjustments,
    filterSettings: FilterSettings
  ): ColorMatrix {
    val result = ColorMatrix()

    // 1. Saturation
    val satMatrix = ColorMatrix()
    satMatrix.setSaturation(adjustments.saturation.coerceIn(0f, 3f))
    result.postConcat(satMatrix)

    // 2. Brightness & Exposure
    // Brightness offset (-255 to 255) and Exposure scale
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

    // 4. Temperature (Warm vs Cool: increases Red/Yellow vs increases Blue)
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

    // 6. Filter Preset
    val filterMatrix = getFilterMatrix(filterSettings.type, filterSettings.intensity)
    if (filterMatrix != null) {
      result.postConcat(filterMatrix)
    }

    return result
  }

  private fun getFilterMatrix(type: FilterType, intensity: Float): ColorMatrix? {
    if (type == FilterType.NONE || intensity <= 0f) return null

    val baseMatrix = when (type) {
      FilterType.BLACK_AND_WHITE -> {
        ColorMatrix().apply { setSaturation(0f) }
      }
      FilterType.CINEMATIC -> {
        // Teal and orange look
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
      FilterType.HDR -> {
        ColorMatrix(
          floatArrayOf(
            1.25f, 0f, 0f, 0f, -15f,
            0f, 1.25f, 0f, 0f, -15f,
            0f, 0f, 1.25f, 0f, -15f,
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
            0f, 0f, 0f, 1f, 0f
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
}
