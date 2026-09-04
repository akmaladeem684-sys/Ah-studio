package com.example.ai

import com.example.ai.providers.GeminiAIProvider
import com.example.domain.model.TextClip
import com.example.domain.model.Timeline
import com.example.domain.model.Transition
import com.example.domain.model.TransitionType
import com.example.domain.model.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

typealias VideoHighlightSegment = com.example.ai.providers.VideoHighlightSegment

class AIToolsService {

  private val geminiProvider = GeminiAIProvider()

  val isAIConfigured: Boolean
    get() = geminiProvider.isAvailable

  /**
   * Generates AI Auto Captions with word/phrase timings using real AI provider.
   * If provider is not configured, returns empty list or throws clear error without fake data.
   */
  suspend fun generateAutoCaptions(
    videoTitle: String,
    durationMs: Long,
    language: String = "English"
  ): List<TextClip> = withContext(Dispatchers.IO) {
    if (!geminiProvider.isAvailable) {
      throw IllegalStateException("AI Provider is not configured. Please add your GEMINI_API_KEY in the Secrets panel to generate real auto-captions.")
    }

    val result = geminiProvider.transcribeAudio(videoTitle, language)
    return@withContext result.getOrThrow()
  }

  /**
   * Translates captions into target language using real AI provider.
   */
  suspend fun translateCaptions(
    captions: List<TextClip>,
    targetLanguage: String
  ): List<TextClip> = withContext(Dispatchers.IO) {
    if (!geminiProvider.isAvailable) {
      throw IllegalStateException("Translation Provider is not configured. Please add your GEMINI_API_KEY in the Secrets panel.")
    }

    val translatedClips = mutableListOf<TextClip>()
    for (clip in captions) {
      val res = geminiProvider.translateText(clip.text, targetLanguage)
      val translatedText = res.getOrDefault(clip.text)
      translatedClips.add(clip.copy(id = UUID.randomUUID().toString(), text = translatedText))
    }
    return@withContext translatedClips
  }

  /**
   * AI Highlight Analyzer: detects exciting moments in project using real AI provider.
   */
  suspend fun analyzeHighlights(timeline: Timeline): List<VideoHighlightSegment> = withContext(Dispatchers.IO) {
    if (!geminiProvider.isAvailable) {
      throw IllegalStateException("AI Highlight Analyzer is not configured. Please set your GEMINI_API_KEY in the Secrets panel.")
    }

    val title = timeline.videoClips.firstOrNull()?.name ?: "Video Project"
    val result = geminiProvider.analyzeHighlights(title, timeline.totalDurationMs)
    return@withContext result.getOrThrow()
  }

  /**
   * AI Auto Edit: creates a dynamic montage from clips with auto cuts and transitions.
   */
  fun autoEditMontage(clips: List<VideoClip>, targetDurationMs: Long = 12000L): Timeline {
    if (clips.isEmpty()) return Timeline()
    val sliceDuration = (targetDurationMs / clips.size).coerceIn(1500L, 4000L)
    val editedClips = clips.mapIndexed { index, clip ->
      clip.copy(
        id = UUID.randomUUID().toString(),
        timelineStartMs = index * sliceDuration,
        durationMs = sliceDuration,
        speed = if (index % 2 == 1) 1.25f else 1.0f
      )
    }

    val transitions = (0 until editedClips.size - 1).map { idx ->
      Transition(
        id = UUID.randomUUID().toString(),
        clipIndexBefore = idx,
        type = if (idx % 2 == 0) TransitionType.FLASH else TransitionType.ZOOM_IN,
        durationMs = 400L
      )
    }

    return Timeline(
      videoClips = editedClips,
      transitions = transitions,
      textClips = listOf(
        TextClip(
          text = "AUTO MONTAGE",
          timelineStartMs = 300L,
          durationMs = 2500L,
          fontSizeSp = 26f,
          fontWeight = 900,
          textColor = 0xFF00E5FF,
          strokeWidth = 2f,
          strokeColor = 0xFF000000,
          animationType = "Pop"
        )
      )
    )
  }

  /**
   * Exports subtitles as standard SRT format.
   */
  fun exportSrt(captions: List<TextClip>): String {
    val sb = StringBuilder()
    captions.sortedBy { it.timelineStartMs }.forEachIndexed { index, clip ->
      sb.append("${index + 1}\n")
      sb.append("${formatSrtTime(clip.timelineStartMs)} --> ${formatSrtTime(clip.timelineStartMs + clip.durationMs)}\n")
      sb.append("${clip.text}\n\n")
    }
    return sb.toString()
  }

  private fun formatSrtTime(ms: Long): String {
    val hours = ms / 3600000
    val minutes = (ms % 3600000) / 60000
    val seconds = (ms % 60000) / 1000
    val millis = ms % 1000
    return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
  }
}
