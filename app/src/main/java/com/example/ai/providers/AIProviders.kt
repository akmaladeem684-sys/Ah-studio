package com.example.ai.providers

import android.content.Context
import android.graphics.Bitmap
import com.example.domain.model.TextClip
import com.example.domain.model.Timeline
import java.io.File

data class VideoHighlightSegment(
  val title: String,
  val startTimeMs: Long,
  val endTimeMs: Long,
  val score: Float,
  val description: String
)

interface SpeechToTextProvider {
  val isAvailable: Boolean
  val providerName: String
  suspend fun transcribeAudio(audioUriOrPath: String, language: String): Result<List<TextClip>>
}

interface TranslationProvider {
  val isAvailable: Boolean
  val providerName: String
  suspend fun translateText(text: String, targetLanguage: String): Result<String>
}

interface BackgroundRemovalProvider {
  val isAvailable: Boolean
  val providerName: String
  suspend fun removeBackground(inputBitmap: Bitmap): Result<Bitmap>
}

interface NoiseReductionProvider {
  val isAvailable: Boolean
  val providerName: String
  suspend fun reduceNoise(audioFile: File): Result<File>
}

interface TextToSpeechProvider {
  val isAvailable: Boolean
  val providerName: String
  suspend fun synthesizeSpeech(text: String, pitch: Float, speed: Float): Result<File>
}

interface HighlightAnalysisProvider {
  val isAvailable: Boolean
  val providerName: String
  suspend fun analyzeHighlights(videoTitle: String, durationMs: Long): Result<List<VideoHighlightSegment>>
}
