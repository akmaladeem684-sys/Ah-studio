package com.example.data.presets

import com.example.domain.model.*
import java.util.UUID

data class VideoTemplate(
  val id: String,
  val title: String,
  val category: String,
  val description: String,
  val aspectRatio: AspectRatio,
  val durationMs: Long,
  val thumbnailGradientStart: Long,
  val thumbnailGradientEnd: Long,
  val iconEmoji: String,
  val isPro: Boolean = false,
  val createTimeline: () -> Timeline
)

object TemplatesCatalog {
  val templates = listOf(
    VideoTemplate(
      id = "tpl_beat_sync",
      title = "Dynamic Beat Sync",
      category = "Trending Reels",
      description = "High-energy fast cuts synchronized to pulse drops with punch zoom transitions.",
      aspectRatio = AspectRatio.RATIO_9_16,
      durationMs = 8000L,
      thumbnailGradientStart = 0xFF00E5FF,
      thumbnailGradientEnd = 0xFF7C3AED,
      iconEmoji = "⚡",
      isPro = false,
      createTimeline = {
        Timeline(
          videoClips = listOf(
            VideoClip(name = "Beat Scene 1", durationMs = 2000L),
            VideoClip(name = "Beat Scene 2", timelineStartMs = 2000L, durationMs = 1500L, speed = 1.25f),
            VideoClip(name = "Beat Scene 3", timelineStartMs = 3500L, durationMs = 2000L),
            VideoClip(name = "Beat Scene 4", timelineStartMs = 5500L, durationMs = 2500L)
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.FLASH, durationMs = 300L),
            Transition(clipIndexBefore = 1, type = TransitionType.ZOOM_IN, durationMs = 350L),
            Transition(clipIndexBefore = 2, type = TransitionType.GLITCH, durationMs = 300L)
          ),
          textClips = listOf(
            TextClip(text = "DROP THE BEAT", timelineStartMs = 2000L, durationMs = 1500L, fontSizeSp = 30f, animationType = "Pop")
          ),
          filter = FilterSettings(FilterType.HDR, 0.9f)
        )
      }
    ),
    VideoTemplate(
      id = "tpl_cinema_vlog",
      title = "Warm Cinematic Vlog",
      category = "Travel & Vlog",
      description = "Warm golden hues, gentle slow zooms, and ambient atmospheric text overlays.",
      aspectRatio = AspectRatio.RATIO_9_16,
      durationMs = 12000L,
      thumbnailGradientStart = 0xFFF59E0B,
      thumbnailGradientEnd = 0xFFEF4444,
      iconEmoji = "🌅",
      isPro = false,
      createTimeline = {
        Timeline(
          videoClips = listOf(
            VideoClip(name = "Morning Sun", durationMs = 6000L),
            VideoClip(name = "Coastal Walk", timelineStartMs = 6000L, durationMs = 6000L)
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.DISSOLVE, durationMs = 800L)
          ),
          textClips = listOf(
            TextClip(text = "Golden Hour Memories", timelineStartMs = 1000L, durationMs = 4000L, fontSizeSp = 22f, animationType = "Fade"),
            TextClip(text = "Chapter One • The Journey", timelineStartMs = 6500L, durationMs = 4500L, fontSizeSp = 20f, animationType = "Slide")
          ),
          filter = FilterSettings(FilterType.WARM, 0.85f),
          adjustments = VideoAdjustments(vignette = 0.2f, contrast = 1.05f)
        )
      }
    ),
    VideoTemplate(
      id = "tpl_cyberpunk",
      title = "Cyberpunk Neo Glitch",
      category = "Effects Heavy",
      description = "Futuristic RGB distortion, glitch stutter cuts, and neon glowing typography.",
      aspectRatio = AspectRatio.RATIO_9_16,
      durationMs = 9000L,
      thumbnailGradientStart = 0xFFEC4899,
      thumbnailGradientEnd = 0xFF3B82F6,
      iconEmoji = "👾",
      isPro = true,
      createTimeline = {
        Timeline(
          videoClips = listOf(
            VideoClip(name = "Night Lights", durationMs = 4500L),
            VideoClip(name = "Neon Alley", timelineStartMs = 4500L, durationMs = 4500L)
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.GLITCH, durationMs = 450L)
          ),
          effectClips = listOf(
            EffectClip(effectType = EffectType.RGB_SPLIT, timelineStartMs = 1000L, durationMs = 3000L, intensity = 0.8f),
            EffectClip(effectType = EffectType.GLOW, timelineStartMs = 4500L, durationMs = 4500L, intensity = 0.9f)
          ),
          textClips = listOf(
            TextClip(text = "NEO TOKYO 2099", timelineStartMs = 500L, durationMs = 3500L, textColor = 0xFF00E5FF, hasGradient = true, gradientColorEnd = 0xFFEC4899, animationType = "Typewriter")
          ),
          filter = FilterSettings(FilterType.RETRO, 0.95f)
        )
      }
    ),
    VideoTemplate(
      id = "tpl_minimal_quote",
      title = "Editorial Minimalist",
      category = "Aesthetic Quotes",
      description = "Clean B&W aesthetic, elegant typography, and subtle film grain overlay.",
      aspectRatio = AspectRatio.RATIO_4_5,
      durationMs = 7000L,
      thumbnailGradientStart = 0xFF334155,
      thumbnailGradientEnd = 0xFF0F172A,
      iconEmoji = "✒️",
      isPro = false,
      createTimeline = {
        Timeline(
          videoClips = listOf(
            VideoClip(name = "Serene Landscape", durationMs = 7000L)
          ),
          textClips = listOf(
            TextClip(text = "\"Simplicity is the ultimate sophistication.\"", timelineStartMs = 1000L, durationMs = 5500L, fontFamily = "Serif", fontSizeSp = 24f, isItalic = true, animationType = "Fade")
          ),
          filter = FilterSettings(FilterType.BLACK_AND_WHITE, 1.0f),
          adjustments = VideoAdjustments(grain = 0.25f, contrast = 1.2f)
        )
      }
    )
  )
}
