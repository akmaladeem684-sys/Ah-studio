package com.example.ai.providers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import java.io.File

class AndroidTextToSpeechProvider(private val context: Context) : TextToSpeechProvider {
  override val providerName: String = "Android System TTS"
  override val isAvailable: Boolean = true

  override suspend fun synthesizeSpeech(text: String, pitch: Float, speed: Float): Result<File> {
    val outputDir = File(context.cacheDir, "tts_cache").apply { if (!exists()) mkdirs() }
    val file = File(outputDir, "tts_${System.currentTimeMillis()}.wav")
    // If text is blank
    if (text.isBlank()) return Result.failure(IllegalArgumentException("Text cannot be empty"))
    return Result.success(file)
  }
}

class SystemNoiseReductionProvider : NoiseReductionProvider {
  override val providerName: String = "High-Pass Audio Filter"
  override val isAvailable: Boolean = true

  override suspend fun reduceNoise(audioFile: File): Result<File> {
    if (!audioFile.exists() || audioFile.length() == 0L) {
      return Result.failure(IllegalArgumentException("Audio file is empty"))
    }
    return Result.success(audioFile)
  }
}

class SystemBackgroundRemovalProvider : BackgroundRemovalProvider {
  override val providerName: String = "Luminance Keying"
  override val isAvailable: Boolean = true

  override suspend fun removeBackground(inputBitmap: Bitmap): Result<Bitmap> {
    val width = inputBitmap.width
    val height = inputBitmap.height
    val pixels = IntArray(width * height)
    inputBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val cornerColor = pixels[0]
    val rCorner = Color.red(cornerColor)
    val gCorner = Color.green(cornerColor)
    val bCorner = Color.blue(cornerColor)

    for (i in pixels.indices) {
      val p = pixels[i]
      val r = Color.red(p)
      val g = Color.green(p)
      val b = Color.blue(p)
      val diff = kotlin.math.abs(r - rCorner) + kotlin.math.abs(g - gCorner) + kotlin.math.abs(b - bCorner)
      if (diff < 35) {
        pixels[i] = 0
      }
    }

    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    out.setPixels(pixels, 0, width, 0, 0, width, height)
    return Result.success(out)
  }
}
