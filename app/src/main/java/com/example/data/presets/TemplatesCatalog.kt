package com.example.data.presets

import com.example.domain.model.*

enum class PlaceholderType {
  VIDEO,
  IMAGE
}

data class MediaPlaceholder(
  val slotId: String,
  val label: String,
  val placeholderType: PlaceholderType,
  val requiredDurationMs: Long,
  val targetClipId: String,
  val defaultName: String = "Placeholder Media",
  val isOverlay: Boolean = false
)

data class TextPlaceholder(
  val slotId: String,
  val label: String,
  val targetClipId: String,
  val defaultText: String
)

data class VideoTemplate(
  val id: String,
  val title: String,
  val category: String,
  val description: String,
  val aspectRatio: AspectRatio,
  val resolution: Resolution = Resolution.RES_1080P,
  val fps: FrameRate = FrameRate.FPS_30,
  val durationMs: Long,
  val thumbnailGradientStart: Long = 0xFF1E293B,
  val thumbnailGradientEnd: Long = 0xFF0F172A,
  val iconEmoji: String = "🎬",
  val mediaPlaceholders: List<MediaPlaceholder> = emptyList(),
  val textPlaceholders: List<TextPlaceholder> = emptyList(),
  val audioTitle: String = "Soundtrack",
  val isPro: Boolean = false,
  val savedTimeline: Timeline? = null,
  val creatorId: String = "",
  val creatorName: String = "Template Creator",
  val creatorHandle: String = "@creator",
  val creatorAvatarUrl: String? = null,
  val previewVideoUrl: String? = null,
  val previewThumbnailUrl: String? = null,
  val viewsCount: Long = 0L,
  val usesCount: Long = 0L,
  val createdAt: Long = System.currentTimeMillis(),
  val createTimeline: (
    mediaReplacements: Map<String, String>,
    textReplacements: Map<String, String>
  ) -> Timeline = { _, _ -> savedTimeline ?: Timeline() }
) {
  fun createDefaultTimeline(): Timeline = savedTimeline ?: createTimeline(emptyMap(), emptyMap())
}

object TemplatesCatalog {
  val categories = listOf(
    "All",
    "Reels",
    "TikTok-style short videos",
    "YouTube",
    "YouTube Shorts",
    "Instagram",
    "Business",
    "Product Ads",
    "Birthday",
    "Wedding",
    "Travel",
    "Cinematic"
  )

  /**
   * Only real Firebase templates are shown in the application.
   * Fake, dummy, and hardcoded templates have been removed.
   */
  val templates: List<VideoTemplate> = emptyList()
}
