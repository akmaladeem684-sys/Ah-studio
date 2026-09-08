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
  val thumbnailGradientStart: Long,
  val thumbnailGradientEnd: Long,
  val iconEmoji: String,
  val mediaPlaceholders: List<MediaPlaceholder> = emptyList(),
  val textPlaceholders: List<TextPlaceholder> = emptyList(),
  val audioTitle: String = "Soundtrack",
  val isPro: Boolean = false,
  val createTimeline: (
    mediaReplacements: Map<String, String>,
    textReplacements: Map<String, String>
  ) -> Timeline
) {
  fun createDefaultTimeline(): Timeline = createTimeline(emptyMap(), emptyMap())
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

  val templates = listOf(
    // 1. REELS: Hyper Beat Pulse
    VideoTemplate(
      id = "tpl_reels_beat_pulse",
      title = "Hyper Beat Pulse",
      category = "Reels",
      description = "High-energy fast cuts synced to bass drops with punch zoom keyframes and flash transitions.",
      aspectRatio = AspectRatio.RATIO_9_16,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = 8000L,
      thumbnailGradientStart = 0xFF00E5FF,
      thumbnailGradientEnd = 0xFF7C3AED,
      iconEmoji = "⚡",
      audioTitle = "Bass Pulse Drop (EDM)",
      mediaPlaceholders = listOf(
        MediaPlaceholder("v1", "Hook Opening Scene", PlaceholderType.VIDEO, 2000L, "reels_vid_1"),
        MediaPlaceholder("v2", "Fast Action Cut", PlaceholderType.VIDEO, 1500L, "reels_vid_2"),
        MediaPlaceholder("i1", "Hero Freeze Frame", PlaceholderType.IMAGE, 2000L, "reels_img_1"),
        MediaPlaceholder("v3", "Drop Climax Scene", PlaceholderType.VIDEO, 2500L, "reels_vid_3")
      ),
      textPlaceholders = listOf(
        TextPlaceholder("t1", "Main Hook", "reels_txt_1", "DROP THE BEAT"),
        TextPlaceholder("t2", "Sub Callout", "reels_txt_2", "WATCH TILL THE END 🔥")
      ),
      createTimeline = { media, texts ->
        val kfZoom = listOf(
          ClipKeyframe(id = "kf_r1", timeMs = 0L, scale = 1.0f),
          ClipKeyframe(id = "kf_r2", timeMs = 1800L, scale = 1.35f, rotation = 4f)
        )
        Timeline(
          videoClips = listOf(
            VideoClip(
              id = "reels_vid_1",
              uri = media["v1"] ?: "",
              name = if (media["v1"] != null) "Opening Clip" else "Hook Opening Scene",
              durationMs = 2000L,
              sourceStartMs = 0L,
              sourceEndMs = 2000L,
              speed = 1.0f
            ),
            VideoClip(
              id = "reels_vid_2",
              uri = media["v2"] ?: "",
              name = if (media["v2"] != null) "Action Clip" else "Fast Action Cut",
              timelineStartMs = 2000L,
              durationMs = 1500L,
              sourceStartMs = 0L,
              sourceEndMs = 1500L,
              speed = 1.35f
            ),
            VideoClip(
              id = "reels_img_1",
              uri = media["i1"] ?: "",
              name = if (media["i1"] != null) "Hero Photo" else "Hero Freeze Frame",
              timelineStartMs = 3500L,
              durationMs = 2000L,
              isVideo = false,
              keyframes = kfZoom
            ),
            VideoClip(
              id = "reels_vid_3",
              uri = media["v3"] ?: "",
              name = if (media["v3"] != null) "Climax Clip" else "Drop Climax Scene",
              timelineStartMs = 5500L,
              durationMs = 2500L,
              sourceStartMs = 0L,
              sourceEndMs = 2500L
            )
          ),
          audioClips = listOf(
            AudioClip(
              id = "reels_audio_1",
              uri = "template://audio/reels_beat",
              title = "Bass Pulse Drop (EDM)",
              timelineStartMs = 0L,
              durationMs = 8000L,
              fadeInMs = 200L,
              fadeOutMs = 600L,
              volume = 0.9f
            )
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.FLASH, durationMs = 300L),
            Transition(clipIndexBefore = 1, type = TransitionType.ZOOM_IN, durationMs = 350L),
            Transition(clipIndexBefore = 2, type = TransitionType.GLITCH, durationMs = 300L)
          ),
          effectClips = listOf(
            EffectClip(effectType = EffectType.RGB_SPLIT, timelineStartMs = 2000L, durationMs = 1500L, intensity = 0.85f),
            EffectClip(effectType = EffectType.MOTION_BLUR, timelineStartMs = 5500L, durationMs = 1200L, intensity = 0.75f)
          ),
          textClips = listOf(
            TextClip(
              id = "reels_txt_1",
              text = texts["t1"] ?: "DROP THE BEAT",
              timelineStartMs = 2000L,
              durationMs = 1500L,
              fontSizeSp = 30f,
              textColor = 0xFFFFFFFF,
              hasGradient = true,
              gradientColorEnd = 0xFF00E5FF,
              animationType = "Pop"
            ),
            TextClip(
              id = "reels_txt_2",
              text = texts["t2"] ?: "WATCH TILL THE END 🔥",
              timelineStartMs = 5500L,
              durationMs = 2500L,
              fontSizeSp = 22f,
              textColor = 0xFFFFD700,
              animationType = "Bounce"
            )
          ),
          filter = FilterSettings(FilterType.HDR, 0.9f),
          adjustments = VideoAdjustments(contrast = 1.15f, saturation = 1.2f),
          aspectRatio = AspectRatio.RATIO_9_16
        )
      }
    ),

    // 2. TIKTOK-STYLE SHORT VIDEOS: Viral Hook & Reveal
    VideoTemplate(
      id = "tpl_tiktok_viral_hook",
      title = "Viral Hook & Reveal",
      category = "TikTok-style short videos",
      description = "Fast-paced vertical edit featuring question hook, suspense buildup, and big reveal with strobe.",
      aspectRatio = AspectRatio.RATIO_9_16,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = 9000L,
      thumbnailGradientStart = 0xFFFF0050,
      thumbnailGradientEnd = 0xFF00F2FE,
      iconEmoji = "🎵",
      audioTitle = "Viral Trend Countdown",
      mediaPlaceholders = listOf(
        MediaPlaceholder("v1", "Hook Statement Clip", PlaceholderType.VIDEO, 3000L, "tt_vid_1"),
        MediaPlaceholder("v2", "Buildup Clip", PlaceholderType.VIDEO, 3000L, "tt_vid_2"),
        MediaPlaceholder("i1", "Reaction Sticker / Image", PlaceholderType.IMAGE, 3000L, "tt_img_1", isOverlay = true)
      ),
      textPlaceholders = listOf(
        TextPlaceholder("t1", "Curiosity Question", "tt_txt_1", "Wait for the end... 😱"),
        TextPlaceholder("t2", "Call To Action", "tt_txt_2", "Part 2 in bio! 👉")
      ),
      createTimeline = { media, texts ->
        val kfReaction = listOf(
          ClipKeyframe(id = "kf_tt1", timeMs = 0L, posX = 0.35f, posY = -0.35f, scale = 0.8f),
          ClipKeyframe(id = "kf_tt2", timeMs = 1500L, posX = 0.35f, posY = -0.35f, scale = 1.05f)
        )
        Timeline(
          videoClips = listOf(
            VideoClip(
              id = "tt_vid_1",
              uri = media["v1"] ?: "",
              name = if (media["v1"] != null) "Hook Clip" else "Hook Statement Clip",
              durationMs = 3000L,
              sourceStartMs = 0L,
              sourceEndMs = 3000L
            ),
            VideoClip(
              id = "tt_vid_2",
              uri = media["v2"] ?: "",
              name = if (media["v2"] != null) "Buildup Clip" else "Buildup Clip",
              timelineStartMs = 3000L,
              durationMs = 6000L,
              sourceStartMs = 0L,
              sourceEndMs = 6000L,
              speed = 1.1f
            )
          ),
          overlayClips = listOf(
            VideoClip(
              id = "tt_img_1",
              uri = media["i1"] ?: "",
              name = if (media["i1"] != null) "Reaction Image" else "Reaction Sticker",
              timelineStartMs = 4000L,
              durationMs = 3000L,
              isVideo = false,
              keyframes = kfReaction
            )
          ),
          audioClips = listOf(
            AudioClip(
              id = "tt_audio",
              uri = "template://audio/tiktok_trend",
              title = "Viral Trend Countdown",
              durationMs = 9000L,
              volume = 0.95f
            )
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.ZOOM_IN, durationMs = 300L)
          ),
          effectClips = listOf(
            EffectClip(effectType = EffectType.GLITCH, timelineStartMs = 5800L, durationMs = 1200L, intensity = 0.8f)
          ),
          textClips = listOf(
            TextClip(
              id = "tt_txt_1",
              text = texts["t1"] ?: "Wait for the end... 😱",
              timelineStartMs = 200L,
              durationMs = 3200L,
              fontSizeSp = 24f,
              textColor = 0xFFFFFFFF,
              hasBackground = true,
              backgroundColor = 0xCC000000,
              animationType = "Pop"
            ),
            TextClip(
              id = "tt_txt_2",
              text = texts["t2"] ?: "Part 2 in bio! 👉",
              timelineStartMs = 6500L,
              durationMs = 2500L,
              fontSizeSp = 22f,
              textColor = 0xFF00F2FE,
              animationType = "Slide"
            )
          ),
          filter = FilterSettings(FilterType.HDR, 0.85f),
          aspectRatio = AspectRatio.RATIO_9_16
        )
      }
    ),

    // 3. YOUTUBE: Tech Review & Vlog Intro
    VideoTemplate(
      id = "tpl_youtube_tech_vlog",
      title = "Tech Review & Vlog Intro",
      category = "YouTube",
      description = "Widescreen 16:9 cinematic presentation for reviews, vlogs, and storytelling with spec card overlay.",
      aspectRatio = AspectRatio.RATIO_16_9,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = 14000L,
      thumbnailGradientStart = 0xFFFF0000,
      thumbnailGradientEnd = 0xFF282828,
      iconEmoji = "▶️",
      audioTitle = "Smooth Lo-Fi Chill Beat",
      mediaPlaceholders = listOf(
        MediaPlaceholder("v1", "Talking Head / Host Clip", PlaceholderType.VIDEO, 6000L, "yt_vid_1"),
        MediaPlaceholder("v2", "Product B-Roll Footage", PlaceholderType.VIDEO, 8000L, "yt_vid_2"),
        MediaPlaceholder("i1", "Spec Card / Graph Image", PlaceholderType.IMAGE, 4000L, "yt_img_1", isOverlay = true)
      ),
      textPlaceholders = listOf(
        TextPlaceholder("t1", "Review Title", "yt_txt_1", "THE HONEST TRUTH: Is It Worth It?"),
        TextPlaceholder("t2", "Chapter Topic", "yt_txt_2", "Chapter 01 • Build Quality & Performance"),
        TextPlaceholder("t3", "Subscribe CTA", "yt_txt_3", "Like & Subscribe for Chapter 2!")
      ),
      createTimeline = { media, texts ->
        val kfPan = listOf(
          ClipKeyframe(id = "kf_yt1", timeMs = 0L, posX = -0.1f, posY = 0f, scale = 1.0f),
          ClipKeyframe(id = "kf_yt2", timeMs = 7500L, posX = 0.1f, posY = 0f, scale = 1.08f)
        )
        Timeline(
          videoClips = listOf(
            VideoClip(
              id = "yt_vid_1",
              uri = media["v1"] ?: "",
              name = if (media["v1"] != null) "Host Video" else "Talking Head Host",
              durationMs = 6000L,
              sourceStartMs = 0L,
              sourceEndMs = 6000L
            ),
            VideoClip(
              id = "yt_vid_2",
              uri = media["v2"] ?: "",
              name = if (media["v2"] != null) "B-Roll Footage" else "Product B-Roll",
              timelineStartMs = 6000L,
              durationMs = 8000L,
              sourceStartMs = 0L,
              sourceEndMs = 8000L,
              keyframes = kfPan
            )
          ),
          overlayClips = listOf(
            VideoClip(
              id = "yt_img_1",
              uri = media["i1"] ?: "",
              name = if (media["i1"] != null) "Spec Card" else "Spec Infographic",
              timelineStartMs = 7000L,
              durationMs = 4000L,
              isVideo = false,
              opacity = 0.9f
            )
          ),
          audioClips = listOf(
            AudioClip(
              id = "yt_audio_1",
              uri = "template://audio/youtube_lofi",
              title = "Smooth Lo-Fi Chill Beat",
              timelineStartMs = 0L,
              durationMs = 14000L,
              fadeInMs = 500L,
              fadeOutMs = 1000L,
              volume = 0.7f
            )
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.DISSOLVE, durationMs = 700L)
          ),
          textClips = listOf(
            TextClip(
              id = "yt_txt_1",
              text = texts["t1"] ?: "THE HONEST TRUTH: Is It Worth It?",
              timelineStartMs = 500L,
              durationMs = 5000L,
              fontFamily = "Roboto",
              fontSizeSp = 26f,
              textColor = 0xFFFFFFFF,
              animationType = "Fade"
            ),
            TextClip(
              id = "yt_txt_2",
              text = texts["t2"] ?: "Chapter 01 • Build Quality & Performance",
              timelineStartMs = 6500L,
              durationMs = 4500L,
              fontSizeSp = 20f,
              textColor = 0xFF00E5FF,
              animationType = "Slide"
            ),
            TextClip(
              id = "yt_txt_3",
              text = texts["t3"] ?: "Like & Subscribe for Chapter 2!",
              timelineStartMs = 11000L,
              durationMs = 3000L,
              fontSizeSp = 22f,
              textColor = 0xFFFF4444,
              animationType = "Pop"
            )
          ),
          filter = FilterSettings(FilterType.CINEMATIC, 0.8f),
          adjustments = VideoAdjustments(contrast = 1.1f, vignette = 0.15f),
          aspectRatio = AspectRatio.RATIO_16_9
        )
      }
    ),

    // 4. YOUTUBE SHORTS: Quick Fact Explainer
    VideoTemplate(
      id = "tpl_yt_shorts_explainer",
      title = "Quick Fact Explainer",
      category = "YouTube Shorts",
      description = "Vertical high-retention educational short with bold subtitle highlights and push-in keyframes.",
      aspectRatio = AspectRatio.RATIO_9_16,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = 10000L,
      thumbnailGradientStart = 0xFFFF4B2B,
      thumbnailGradientEnd = 0xFFFF416C,
      iconEmoji = "💡",
      audioTitle = "Urgent Pulse Beat",
      mediaPlaceholders = listOf(
        MediaPlaceholder("v1", "Hook Demonstration", PlaceholderType.VIDEO, 5000L, "yts_vid_1"),
        MediaPlaceholder("v2", "Scientific Fact Reveal", PlaceholderType.VIDEO, 5000L, "yts_vid_2"),
        MediaPlaceholder("i1", "Fact Diagram Photo", PlaceholderType.IMAGE, 3000L, "yts_img_1", isOverlay = true)
      ),
      textPlaceholders = listOf(
        TextPlaceholder("t1", "Main Hook", "yts_txt_1", "3 MIND-BLOWING FACTS 🤯"),
        TextPlaceholder("t2", "Fact One", "yts_txt_2", "1. Honey never spoils even after 3,000 years"),
        TextPlaceholder("t3", "Hashtags CTA", "yts_txt_3", "Subscribe for more facts! #shorts")
      ),
      createTimeline = { media, texts ->
        Timeline(
          videoClips = listOf(
            VideoClip(
              id = "yts_vid_1",
              uri = media["v1"] ?: "",
              name = if (media["v1"] != null) "Hook Clip" else "Hook Demo",
              durationMs = 5000L,
              sourceStartMs = 0L,
              sourceEndMs = 5000L
            ),
            VideoClip(
              id = "yts_vid_2",
              uri = media["v2"] ?: "",
              name = if (media["v2"] != null) "Reveal Clip" else "Fact Reveal",
              timelineStartMs = 5000L,
              durationMs = 5000L,
              sourceStartMs = 0L,
              sourceEndMs = 5000L
            )
          ),
          overlayClips = listOf(
            VideoClip(
              id = "yts_img_1",
              uri = media["i1"] ?: "",
              name = if (media["i1"] != null) "Fact Diagram" else "Diagram Photo",
              timelineStartMs = 3000L,
              durationMs = 3000L,
              isVideo = false,
              opacity = 0.95f
            )
          ),
          audioClips = listOf(
            AudioClip(
              id = "yts_audio",
              uri = "template://audio/yt_shorts_pulse",
              title = "Urgent Pulse Beat",
              durationMs = 10000L,
              volume = 0.85f
            )
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.FLASH, durationMs = 250L)
          ),
          textClips = listOf(
            TextClip(
              id = "yts_txt_1",
              text = texts["t1"] ?: "3 MIND-BLOWING FACTS 🤯",
              timelineStartMs = 0L,
              durationMs = 3500L,
              fontSizeSp = 28f,
              textColor = 0xFFFFFF00,
              strokeWidth = 3f,
              strokeColor = 0xFF000000,
              animationType = "Pop"
            ),
            TextClip(
              id = "yts_txt_2",
              text = texts["t2"] ?: "1. Honey never spoils even after 3,000 years",
              timelineStartMs = 3500L,
              durationMs = 4500L,
              fontSizeSp = 22f,
              textColor = 0xFFFFFFFF,
              animationType = "Typewriter"
            ),
            TextClip(
              id = "yts_txt_3",
              text = texts["t3"] ?: "Subscribe for more facts! #shorts",
              timelineStartMs = 8000L,
              durationMs = 2000L,
              fontSizeSp = 20f,
              textColor = 0xFFFF0055,
              animationType = "Fade"
            )
          ),
          filter = FilterSettings(FilterType.HDR, 0.9f),
          aspectRatio = AspectRatio.RATIO_9_16
        )
      }
    ),

    // 5. INSTAGRAM: Minimalist Aesthetic Story
    VideoTemplate(
      id = "tpl_instagram_aesthetic",
      title = "Minimalist Aesthetic Story",
      category = "Instagram",
      description = "Clean editorial typography with gentle slow pan, soft warm grain, and aesthetic moodboard.",
      aspectRatio = AspectRatio.RATIO_9_16,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = 12000L,
      thumbnailGradientStart = 0xFF833AB4,
      thumbnailGradientEnd = 0xFFFD1D1D,
      iconEmoji = "📸",
      audioTitle = "Ambient Acoustic Guitar",
      mediaPlaceholders = listOf(
        MediaPlaceholder("v1", "Lifestyle Video Scene", PlaceholderType.VIDEO, 6000L, "ig_vid_1"),
        MediaPlaceholder("i1", "Aesthetic Flatlay Photo", PlaceholderType.IMAGE, 6000L, "ig_img_1"),
        MediaPlaceholder("i2", "Moodboard Polaroids", PlaceholderType.IMAGE, 4000L, "ig_img_2", isOverlay = true)
      ),
      textPlaceholders = listOf(
        TextPlaceholder("t1", "Editorial Title", "ig_txt_1", "SUNDAY MORNINGS • VOL 04"),
        TextPlaceholder("t2", "Aesthetic Quote", "ig_txt_2", "Finding beauty in quiet ordinary spaces."),
        TextPlaceholder("t3", "Collection CTA", "ig_txt_3", "NEW COLLECTION OUT NOW • LINK IN BIO")
      ),
      createTimeline = { media, texts ->
        val kfSlowZoom = listOf(
          ClipKeyframe(id = "kf_ig1", timeMs = 0L, scale = 1.0f),
          ClipKeyframe(id = "kf_ig2", timeMs = 6000L, scale = 1.12f)
        )
        Timeline(
          videoClips = listOf(
            VideoClip(
              id = "ig_vid_1",
              uri = media["v1"] ?: "",
              name = if (media["v1"] != null) "Lifestyle Clip" else "Lifestyle Scene",
              durationMs = 6000L,
              sourceStartMs = 0L,
              sourceEndMs = 6000L
            ),
            VideoClip(
              id = "ig_img_1",
              uri = media["i1"] ?: "",
              name = if (media["i1"] != null) "Flatlay Photo" else "Aesthetic Flatlay",
              timelineStartMs = 6000L,
              durationMs = 6000L,
              isVideo = false,
              keyframes = kfSlowZoom
            )
          ),
          overlayClips = listOf(
            VideoClip(
              id = "ig_img_2",
              uri = media["i2"] ?: "",
              name = if (media["i2"] != null) "Moodboard Overlay" else "Polaroid Image",
              timelineStartMs = 2000L,
              durationMs = 4000L,
              isVideo = false,
              opacity = 0.88f
            )
          ),
          audioClips = listOf(
            AudioClip(
              id = "ig_audio_1",
              uri = "template://audio/ig_acoustic",
              title = "Ambient Acoustic Guitar",
              durationMs = 12000L,
              fadeInMs = 800L,
              fadeOutMs = 800L,
              volume = 0.8f
            )
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.DISSOLVE, durationMs = 800L)
          ),
          textClips = listOf(
            TextClip(
              id = "ig_txt_1",
              text = texts["t1"] ?: "SUNDAY MORNINGS • VOL 04",
              timelineStartMs = 500L,
              durationMs = 5000L,
              fontFamily = "Serif",
              fontSizeSp = 22f,
              textColor = 0xFFFFFFFF,
              animationType = "Fade"
            ),
            TextClip(
              id = "ig_txt_2",
              text = texts["t2"] ?: "Finding beauty in quiet ordinary spaces.",
              timelineStartMs = 6500L,
              durationMs = 4500L,
              fontFamily = "Serif",
              fontSizeSp = 18f,
              isItalic = true,
              textColor = 0xFFF0F0F0,
              animationType = "Fade"
            ),
            TextClip(
              id = "ig_txt_3",
              text = texts["t3"] ?: "NEW COLLECTION OUT NOW • LINK IN BIO",
              timelineStartMs = 9000L,
              durationMs = 3000L,
              fontSizeSp = 16f,
              textColor = 0xFFE0E0E0,
              animationType = "Slide"
            )
          ),
          filter = FilterSettings(FilterType.WARM, 0.8f),
          adjustments = VideoAdjustments(grain = 0.2f, contrast = 1.05f),
          aspectRatio = AspectRatio.RATIO_9_16
        )
      }
    ),

    // 6. BUSINESS: Corporate Pitch & Showcase
    VideoTemplate(
      id = "tpl_business_pitch",
      title = "Corporate Pitch & Showcase",
      category = "Business",
      description = "Professional 16:9 presentation template with sleek metric callouts, clean transitions, and logo card.",
      aspectRatio = AspectRatio.RATIO_16_9,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = 15000L,
      thumbnailGradientStart = 0xFF1E3A8A,
      thumbnailGradientEnd = 0xFF0284C7,
      iconEmoji = "💼",
      audioTitle = "Inspiring Tech Corporate",
      mediaPlaceholders = listOf(
        MediaPlaceholder("v1", "Company Workspace Clip", PlaceholderType.VIDEO, 7000L, "biz_vid_1"),
        MediaPlaceholder("v2", "Executive / Team Meeting", PlaceholderType.VIDEO, 8000L, "biz_vid_2"),
        MediaPlaceholder("i1", "Company Logo / Growth Chart", PlaceholderType.IMAGE, 5000L, "biz_img_1", isOverlay = true)
      ),
      textPlaceholders = listOf(
        TextPlaceholder("t1", "Vision Headline", "biz_txt_1", "SCALING ENTERPRISE INNOVATION"),
        TextPlaceholder("t2", "Key Metric", "biz_txt_2", "10x Faster Deployment • 99.9% Uptime"),
        TextPlaceholder("t3", "Meeting Callout", "biz_txt_3", "Transform your workflow today: enterprise.com")
      ),
      createTimeline = { media, texts ->
        Timeline(
          videoClips = listOf(
            VideoClip(
              id = "biz_vid_1",
              uri = media["v1"] ?: "",
              name = if (media["v1"] != null) "Workspace Video" else "Workspace Clip",
              durationMs = 7000L,
              sourceStartMs = 0L,
              sourceEndMs = 7000L
            ),
            VideoClip(
              id = "biz_vid_2",
              uri = media["v2"] ?: "",
              name = if (media["v2"] != null) "Team Video" else "Team Meeting Clip",
              timelineStartMs = 7000L,
              durationMs = 8000L,
              sourceStartMs = 0L,
              sourceEndMs = 8000L
            )
          ),
          overlayClips = listOf(
            VideoClip(
              id = "biz_img_1",
              uri = media["i1"] ?: "",
              name = if (media["i1"] != null) "Growth Chart" else "Company Logo",
              timelineStartMs = 8000L,
              durationMs = 5000L,
              isVideo = false,
              opacity = 0.92f
            )
          ),
          audioClips = listOf(
            AudioClip(
              id = "biz_audio",
              uri = "template://audio/business_tech",
              title = "Inspiring Tech Corporate",
              durationMs = 15000L,
              fadeInMs = 600L,
              fadeOutMs = 1000L,
              volume = 0.75f
            )
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.WIPE, durationMs = 500L)
          ),
          textClips = listOf(
            TextClip(
              id = "biz_txt_1",
              text = texts["t1"] ?: "SCALING ENTERPRISE INNOVATION",
              timelineStartMs = 500L,
              durationMs = 6000L,
              fontSizeSp = 28f,
              textColor = 0xFFFFFFFF,
              animationType = "Fade"
            ),
            TextClip(
              id = "biz_txt_2",
              text = texts["t2"] ?: "10x Faster Deployment • 99.9% Uptime",
              timelineStartMs = 7500L,
              durationMs = 4500L,
              fontSizeSp = 22f,
              textColor = 0xFF38BDF8,
              animationType = "Slide"
            ),
            TextClip(
              id = "biz_txt_3",
              text = texts["t3"] ?: "Transform your workflow today: enterprise.com",
              timelineStartMs = 12000L,
              durationMs = 3000L,
              fontSizeSp = 20f,
              textColor = 0xFFFFFFFF,
              animationType = "Fade"
            )
          ),
          adjustments = VideoAdjustments(contrast = 1.05f),
          aspectRatio = AspectRatio.RATIO_16_9
        )
      }
    ),

    // 7. PRODUCT ADS: Flash Sale & Product Launch
    VideoTemplate(
      id = "tpl_product_ad_launch",
      title = "Flash Sale & Product Launch",
      category = "Product Ads",
      description = "High-converting promo ad with floating cutout photo keyframes, price slash badge, and discount code.",
      aspectRatio = AspectRatio.RATIO_9_16,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = 10000L,
      thumbnailGradientStart = 0xFFF97316,
      thumbnailGradientEnd = 0xFFEA580C,
      iconEmoji = "🛍️",
      audioTitle = "Energetic Commercial Beat",
      mediaPlaceholders = listOf(
        MediaPlaceholder("v1", "Product Unboxing Video", PlaceholderType.VIDEO, 3000L, "ad_vid_1"),
        MediaPlaceholder("i1", "Cutout Hero Product Photo", PlaceholderType.IMAGE, 4000L, "ad_img_1"),
        MediaPlaceholder("v2", "Customer In-Use Demo", PlaceholderType.VIDEO, 3000L, "ad_vid_2")
      ),
      textPlaceholders = listOf(
        TextPlaceholder("t1", "Sale Headline", "ad_txt_1", "LIMITED DROP • 50% OFF"),
        TextPlaceholder("t2", "Promo Offer", "ad_txt_2", "ONLY $29.99 TODAY"),
        TextPlaceholder("t3", "Discount Code CTA", "ad_txt_3", "Use Code: FLASH50 at Checkout")
      ),
      createTimeline = { media, texts ->
        val kfProductBounce = listOf(
          ClipKeyframe(id = "kf_ad1", timeMs = 0L, scale = 0.9f, rotation = -3f),
          ClipKeyframe(id = "kf_ad2", timeMs = 2000L, scale = 1.15f, rotation = 2f),
          ClipKeyframe(id = "kf_ad3", timeMs = 4000L, scale = 1.0f, rotation = 0f)
        )
        Timeline(
          videoClips = listOf(
            VideoClip(
              id = "ad_vid_1",
              uri = media["v1"] ?: "",
              name = if (media["v1"] != null) "Unboxing Video" else "Product Unboxing",
              durationMs = 3000L,
              sourceStartMs = 0L,
              sourceEndMs = 3000L
            ),
            VideoClip(
              id = "ad_img_1",
              uri = media["i1"] ?: "",
              name = if (media["i1"] != null) "Hero Product Photo" else "Cutout Hero Photo",
              timelineStartMs = 3000L,
              durationMs = 4000L,
              isVideo = false,
              keyframes = kfProductBounce
            ),
            VideoClip(
              id = "ad_vid_2",
              uri = media["v2"] ?: "",
              name = if (media["v2"] != null) "Demo Video" else "In-Use Demo",
              timelineStartMs = 7000L,
              durationMs = 3000L,
              sourceStartMs = 0L,
              sourceEndMs = 3000L
            )
          ),
          audioClips = listOf(
            AudioClip(
              id = "ad_audio",
              uri = "template://audio/product_ad_beat",
              title = "Energetic Commercial Beat",
              durationMs = 10000L,
              volume = 0.9f
            )
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.ZOOM_IN, durationMs = 350L),
            Transition(clipIndexBefore = 1, type = TransitionType.FLASH, durationMs = 300L)
          ),
          effectClips = listOf(
            EffectClip(effectType = EffectType.GLOW, timelineStartMs = 3000L, durationMs = 4000L, intensity = 0.75f)
          ),
          textClips = listOf(
            TextClip(
              id = "ad_txt_1",
              text = texts["t1"] ?: "LIMITED DROP • 50% OFF",
              timelineStartMs = 300L,
              durationMs = 2800L,
              fontSizeSp = 26f,
              textColor = 0xFFFFFFFF,
              hasBackground = true,
              backgroundColor = 0xFFE11D48,
              animationType = "Pop"
            ),
            TextClip(
              id = "ad_txt_2",
              text = texts["t2"] ?: "ONLY $29.99 TODAY",
              timelineStartMs = 3200L,
              durationMs = 3600L,
              fontSizeSp = 30f,
              textColor = 0xFFFFD700,
              strokeWidth = 2f,
              strokeColor = 0xFF000000,
              animationType = "Bounce"
            ),
            TextClip(
              id = "ad_txt_3",
              text = texts["t3"] ?: "Use Code: FLASH50 at Checkout",
              timelineStartMs = 7200L,
              durationMs = 2800L,
              fontSizeSp = 22f,
              textColor = 0xFFFFFFFF,
              animationType = "Slide"
            )
          ),
          filter = FilterSettings(FilterType.HDR, 0.95f),
          aspectRatio = AspectRatio.RATIO_9_16
        )
      }
    ),

    // 8. BIRTHDAY: Confetti Birthday Magic
    VideoTemplate(
      id = "tpl_birthday_magic",
      title = "Confetti Birthday Magic",
      category = "Birthday",
      description = "Joyful celebration reel with confetti particles, celebratory stickers, warm festive glow, and bouncy greetings.",
      aspectRatio = AspectRatio.RATIO_9_16,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = 12000L,
      thumbnailGradientStart = 0xFFEC4899,
      thumbnailGradientEnd = 0xFFF59E0B,
      iconEmoji = "🎂",
      audioTitle = "Festive Celebration Anthem",
      mediaPlaceholders = listOf(
        MediaPlaceholder("v1", "Cake Cutting / Cheers Clip", PlaceholderType.VIDEO, 6000L, "bday_vid_1"),
        MediaPlaceholder("i1", "Birthday Star Photo", PlaceholderType.IMAGE, 6000L, "bday_img_1"),
        MediaPlaceholder("i2", "Friends / Party Memory", PlaceholderType.IMAGE, 4000L, "bday_img_2", isOverlay = true)
      ),
      textPlaceholders = listOf(
        TextPlaceholder("t1", "Celebration Title", "bday_txt_1", "HAPPY BIRTHDAY EMILY! 🎂"),
        TextPlaceholder("t2", "Warm Wishes", "bday_txt_2", "Cheers to another fantastic year full of joy & adventure! ✨"),
        TextPlaceholder("t3", "Birthday Age Tag", "bday_txt_3", "Level 25 Unlocked 🚀")
      ),
      createTimeline = { media, texts ->
        val kfPhoto = listOf(
          ClipKeyframe(id = "kf_bday1", timeMs = 0L, scale = 1.0f, rotation = -2f),
          ClipKeyframe(id = "kf_bday2", timeMs = 5000L, scale = 1.15f, rotation = 2f)
        )
        Timeline(
          videoClips = listOf(
            VideoClip(
              id = "bday_vid_1",
              uri = media["v1"] ?: "",
              name = if (media["v1"] != null) "Cake Video" else "Cake Cutting Clip",
              durationMs = 6000L,
              sourceStartMs = 0L,
              sourceEndMs = 6000L
            ),
            VideoClip(
              id = "bday_img_1",
              uri = media["i1"] ?: "",
              name = if (media["i1"] != null) "Birthday Star Photo" else "Star Portrait Photo",
              timelineStartMs = 6000L,
              durationMs = 6000L,
              isVideo = false,
              keyframes = kfPhoto
            )
          ),
          overlayClips = listOf(
            VideoClip(
              id = "bday_img_2",
              uri = media["i2"] ?: "",
              name = if (media["i2"] != null) "Party Photo" else "Memory Photo",
              timelineStartMs = 7000L,
              durationMs = 4000L,
              isVideo = false,
              opacity = 0.9f
            )
          ),
          audioClips = listOf(
            AudioClip(
              id = "bday_audio",
              uri = "template://audio/birthday_anthem",
              title = "Festive Celebration Anthem",
              durationMs = 12000L,
              fadeInMs = 400L,
              fadeOutMs = 800L,
              volume = 0.85f
            )
          ),
          stickerClips = listOf(
            StickerClip(id = "stk_cake", emojiOrAsset = "🎂", timelineStartMs = 500L, durationMs = 5500L, posX = 0.35f, posY = -0.3f, scale = 1.3f),
            StickerClip(id = "stk_party", emojiOrAsset = "🎉", timelineStartMs = 6000L, durationMs = 6000L, posX = -0.35f, posY = -0.3f, scale = 1.4f)
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.FLASH, durationMs = 400L)
          ),
          textClips = listOf(
            TextClip(
              id = "bday_txt_1",
              text = texts["t1"] ?: "HAPPY BIRTHDAY EMILY! 🎂",
              timelineStartMs = 500L,
              durationMs = 5000L,
              fontSizeSp = 28f,
              textColor = 0xFFFFD700,
              strokeWidth = 2f,
              strokeColor = 0xFF000000,
              animationType = "Pop"
            ),
            TextClip(
              id = "bday_txt_2",
              text = texts["t2"] ?: "Cheers to another fantastic year full of joy & adventure! ✨",
              timelineStartMs = 6500L,
              durationMs = 4500L,
              fontSizeSp = 20f,
              textColor = 0xFFFFFFFF,
              animationType = "Fade"
            ),
            TextClip(
              id = "bday_txt_3",
              text = texts["t3"] ?: "Level 25 Unlocked 🚀",
              timelineStartMs = 9500L,
              durationMs = 2500L,
              fontSizeSp = 22f,
              textColor = 0xFF00E5FF,
              animationType = "Bounce"
            )
          ),
          filter = FilterSettings(FilterType.WARM, 0.85f),
          aspectRatio = AspectRatio.RATIO_9_16
        )
      }
    ),

    // 9. WEDDING: Everlasting Love Story
    VideoTemplate(
      id = "tpl_wedding_romance",
      title = "Everlasting Love Story",
      category = "Wedding",
      description = "Emotional 16:9 romance film with slow dissolve transitions, elegant serif titles, and Ken Burns photo push.",
      aspectRatio = AspectRatio.RATIO_16_9,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = 18000L,
      thumbnailGradientStart = 0xFFF472B6,
      thumbnailGradientEnd = 0xFFFDE047,
      iconEmoji = "💍",
      audioTitle = "Emotional Piano & Orchestra",
      mediaPlaceholders = listOf(
        MediaPlaceholder("v1", "Vow Ceremony Clip", PlaceholderType.VIDEO, 9000L, "wed_vid_1"),
        MediaPlaceholder("i1", "Romantic Portrait Photo", PlaceholderType.IMAGE, 9000L, "wed_img_1")
      ),
      textPlaceholders = listOf(
        TextPlaceholder("t1", "Couple Names", "wed_txt_1", "Sarah & David"),
        TextPlaceholder("t2", "Wedding Date", "wed_txt_2", "OCTOBER 14, 2026 • NAPA VALLEY"),
        TextPlaceholder("t3", "Vow Quote", "wed_txt_3", "\"Two souls, one heartbeat, forever and always.\"")
      ),
      createTimeline = { media, texts ->
        val kfKenBurns = listOf(
          ClipKeyframe(id = "kf_wed1", timeMs = 0L, scale = 1.0f, posX = 0f, posY = 0f),
          ClipKeyframe(id = "kf_wed2", timeMs = 9000L, scale = 1.15f, posX = 0.05f, posY = -0.03f)
        )
        Timeline(
          videoClips = listOf(
            VideoClip(
              id = "wed_vid_1",
              uri = media["v1"] ?: "",
              name = if (media["v1"] != null) "Ceremony Video" else "Vow Ceremony Clip",
              durationMs = 9000L,
              sourceStartMs = 0L,
              sourceEndMs = 9000L
            ),
            VideoClip(
              id = "wed_img_1",
              uri = media["i1"] ?: "",
              name = if (media["i1"] != null) "Portrait Photo" else "Romantic Portrait",
              timelineStartMs = 9000L,
              durationMs = 9000L,
              isVideo = false,
              keyframes = kfKenBurns
            )
          ),
          audioClips = listOf(
            AudioClip(
              id = "wed_audio",
              uri = "template://audio/wedding_strings",
              title = "Emotional Piano & Orchestra",
              durationMs = 18000L,
              fadeInMs = 1200L,
              fadeOutMs = 1500L,
              volume = 0.8f
            )
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.DISSOLVE, durationMs = 1200L)
          ),
          effectClips = listOf(
            EffectClip(effectType = EffectType.GLOW, timelineStartMs = 0L, durationMs = 18000L, intensity = 0.4f)
          ),
          textClips = listOf(
            TextClip(
              id = "wed_txt_1",
              text = texts["t1"] ?: "Sarah & David",
              timelineStartMs = 1000L,
              durationMs = 7000L,
              fontFamily = "Serif",
              fontSizeSp = 36f,
              textColor = 0xFFFFFFFF,
              animationType = "Fade"
            ),
            TextClip(
              id = "wed_txt_2",
              text = texts["t2"] ?: "OCTOBER 14, 2026 • NAPA VALLEY",
              timelineStartMs = 2500L,
              durationMs = 5500L,
              fontFamily = "Serif",
              fontSizeSp = 18f,
              textColor = 0xFFF5F5DC,
              animationType = "Fade"
            ),
            TextClip(
              id = "wed_txt_3",
              text = texts["t3"] ?: "\"Two souls, one heartbeat, forever and always.\"",
              timelineStartMs = 10500L,
              durationMs = 6500L,
              fontFamily = "Serif",
              fontSizeSp = 22f,
              isItalic = true,
              textColor = 0xFFFFFFFF,
              animationType = "Fade"
            )
          ),
          filter = FilterSettings(FilterType.VINTAGE, 0.7f),
          adjustments = VideoAdjustments(vignette = 0.2f),
          aspectRatio = AspectRatio.RATIO_16_9
        )
      }
    ),

    // 10. TRAVEL: Wanderlust Adventure Diary
    VideoTemplate(
      id = "tpl_travel_adventure",
      title = "Wanderlust Adventure Diary",
      category = "Travel",
      description = "Epic vertical travel journal with scenic drone clips, destination stamp photo, and GPS coordinate titles.",
      aspectRatio = AspectRatio.RATIO_9_16,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = 15000L,
      thumbnailGradientStart = 0xFF10B981,
      thumbnailGradientEnd = 0xFF06B6D4,
      iconEmoji = "✈️",
      audioTitle = "Uplifting Tropical House",
      mediaPlaceholders = listOf(
        MediaPlaceholder("v1", "Drone Landscape Scene", PlaceholderType.VIDEO, 5000L, "trv_vid_1"),
        MediaPlaceholder("i1", "Passport / Map Photo", PlaceholderType.IMAGE, 4000L, "trv_img_1"),
        MediaPlaceholder("v2", "Adventure Activity Scene", PlaceholderType.VIDEO, 6000L, "trv_vid_2")
      ),
      textPlaceholders = listOf(
        TextPlaceholder("t1", "Destination Title", "trv_txt_1", "EXPLORING BALI, INDONESIA 🌴"),
        TextPlaceholder("t2", "Coordinates", "trv_txt_2", "8.3405° S, 115.0920° E"),
        TextPlaceholder("t3", "Outro Callout", "trv_txt_3", "Never stop wandering ✨")
      ),
      createTimeline = { media, texts ->
        val kfDrone = listOf(
          ClipKeyframe(id = "kf_trv1", timeMs = 0L, scale = 1.0f, posY = 0f),
          ClipKeyframe(id = "kf_trv2", timeMs = 5000L, scale = 1.18f, posY = -0.05f)
        )
        Timeline(
          videoClips = listOf(
            VideoClip(
              id = "trv_vid_1",
              uri = media["v1"] ?: "",
              name = if (media["v1"] != null) "Drone Video" else "Drone Landscape",
              durationMs = 5000L,
              sourceStartMs = 0L,
              sourceEndMs = 5000L,
              keyframes = kfDrone
            ),
            VideoClip(
              id = "trv_img_1",
              uri = media["i1"] ?: "",
              name = if (media["i1"] != null) "Map Photo" else "Passport / Map",
              timelineStartMs = 5000L,
              durationMs = 4000L,
              isVideo = false
            ),
            VideoClip(
              id = "trv_vid_2",
              uri = media["v2"] ?: "",
              name = if (media["v2"] != null) "Activity Video" else "Adventure Activity",
              timelineStartMs = 9000L,
              durationMs = 6000L,
              sourceStartMs = 0L,
              sourceEndMs = 6000L
            )
          ),
          audioClips = listOf(
            AudioClip(
              id = "trv_audio",
              uri = "template://audio/travel_house",
              title = "Uplifting Tropical House",
              durationMs = 15000L,
              fadeInMs = 400L,
              fadeOutMs = 800L,
              volume = 0.85f
            )
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.ZOOM_IN, durationMs = 400L),
            Transition(clipIndexBefore = 1, type = TransitionType.WIPE, durationMs = 400L)
          ),
          textClips = listOf(
            TextClip(
              id = "trv_txt_1",
              text = texts["t1"] ?: "EXPLORING BALI, INDONESIA 🌴",
              timelineStartMs = 500L,
              durationMs = 4500L,
              fontSizeSp = 24f,
              textColor = 0xFFFFFFFF,
              hasBackground = true,
              backgroundColor = 0x88000000,
              animationType = "Pop"
            ),
            TextClip(
              id = "trv_txt_2",
              text = texts["t2"] ?: "8.3405° S, 115.0920° E",
              timelineStartMs = 5200L,
              durationMs = 3500L,
              fontSizeSp = 18f,
              textColor = 0xFF00FFCC,
              animationType = "Typewriter"
            ),
            TextClip(
              id = "trv_txt_3",
              text = texts["t3"] ?: "Never stop wandering ✨",
              timelineStartMs = 10000L,
              durationMs = 4500L,
              fontSizeSp = 22f,
              textColor = 0xFFFFD700,
              animationType = "Fade"
            )
          ),
          filter = FilterSettings(FilterType.WARM, 0.9f),
          aspectRatio = AspectRatio.RATIO_9_16
        )
      }
    ),

    // 11. CINEMATIC: Midnight Neo-Noir Film
    VideoTemplate(
      id = "tpl_cinematic_noir",
      title = "Midnight Neo-Noir Film",
      category = "Cinematic",
      description = "Ultra-widescreen dramatic mood piece with desaturated cyan tones, slow anamorphic zooms, and deep brass.",
      aspectRatio = AspectRatio.RATIO_16_9,
      resolution = Resolution.RES_4K,
      fps = FrameRate.FPS_24,
      durationMs = 16000L,
      thumbnailGradientStart = 0xFF0F172A,
      thumbnailGradientEnd = 0xFF312E81,
      iconEmoji = "🎬",
      audioTitle = "Dramatic Orchestral Inception Braam",
      isPro = true,
      mediaPlaceholders = listOf(
        MediaPlaceholder("v1", "Moody Rainy Street Scene", PlaceholderType.VIDEO, 8000L, "noir_vid_1"),
        MediaPlaceholder("i1", "Theatrical Poster Title Card", PlaceholderType.IMAGE, 8000L, "noir_img_1")
      ),
      textPlaceholders = listOf(
        TextPlaceholder("t1", "Movie Title", "noir_txt_1", "SHADOWS OF THE CITY"),
        TextPlaceholder("t2", "Director Credit", "noir_txt_2", "A Film by AH Studio"),
        TextPlaceholder("t3", "Release Date Tag", "noir_txt_3", "COMING TO THEATERS THIS WINTER")
      ),
      createTimeline = { media, texts ->
        val kfAnamorphic = listOf(
          ClipKeyframe(id = "kf_noir1", timeMs = 0L, scale = 1.0f, posY = 0f),
          ClipKeyframe(id = "kf_noir2", timeMs = 8000L, scale = 1.15f, posY = -0.02f)
        )
        Timeline(
          videoClips = listOf(
            VideoClip(
              id = "noir_vid_1",
              uri = media["v1"] ?: "",
              name = if (media["v1"] != null) "Rain Scene" else "Moody Rainy Street",
              durationMs = 8000L,
              sourceStartMs = 0L,
              sourceEndMs = 8000L,
              keyframes = kfAnamorphic
            ),
            VideoClip(
              id = "noir_img_1",
              uri = media["i1"] ?: "",
              name = if (media["i1"] != null) "Poster Title" else "Theatrical Poster Card",
              timelineStartMs = 8000L,
              durationMs = 8000L,
              isVideo = false
            )
          ),
          audioClips = listOf(
            AudioClip(
              id = "noir_audio",
              uri = "template://audio/noir_braam",
              title = "Dramatic Orchestral Inception Braam",
              durationMs = 16000L,
              fadeInMs = 1000L,
              fadeOutMs = 2000L,
              volume = 0.95f
            )
          ),
          transitions = listOf(
            Transition(clipIndexBefore = 0, type = TransitionType.DISSOLVE, durationMs = 1000L)
          ),
          effectClips = listOf(
            EffectClip(effectType = EffectType.GLITCH, timelineStartMs = 7500L, durationMs = 1000L, intensity = 0.5f)
          ),
          textClips = listOf(
            TextClip(
              id = "noir_txt_1",
              text = texts["t1"] ?: "SHADOWS OF THE CITY",
              timelineStartMs = 1500L,
              durationMs = 6000L,
              fontFamily = "Monospace",
              fontSizeSp = 34f,
              textColor = 0xFFE2E8F0,
              letterSpacing = 4f,
              animationType = "Fade"
            ),
            TextClip(
              id = "noir_txt_2",
              text = texts["t2"] ?: "A Film by AH Studio",
              timelineStartMs = 8500L,
              durationMs = 4000L,
              fontSizeSp = 18f,
              textColor = 0xFF94A3B8,
              animationType = "Fade"
            ),
            TextClip(
              id = "noir_txt_3",
              text = texts["t3"] ?: "COMING TO THEATERS THIS WINTER",
              timelineStartMs = 12500L,
              durationMs = 3500L,
              fontSizeSp = 16f,
              textColor = 0xFF64748B,
              animationType = "Fade"
            )
          ),
          filter = FilterSettings(FilterType.CINEMATIC, 1.0f),
          adjustments = VideoAdjustments(grain = 0.3f, contrast = 1.3f, vignette = 0.35f),
          aspectRatio = AspectRatio.RATIO_16_9
        )
      }
    )
  )
}
