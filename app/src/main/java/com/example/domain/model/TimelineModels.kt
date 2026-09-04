package com.example.domain.model

import java.util.UUID

data class ClipKeyframe(
  val id: String = UUID.randomUUID().toString(),
  val timeMs: Long,
  val scale: Float = 1f,
  val rotation: Float = 0f,
  val posX: Float = 0f,
  val posY: Float = 0f,
  val opacity: Float = 1f,
  val volume: Float = 1f,
  val interpolation: String = "Linear" // "Linear" or "SmoothEase"
)

data class VideoClip(
  val id: String = UUID.randomUUID().toString(),
  val uri: String = "",
  val name: String,
  val isVideo: Boolean = true,
  val timelineStartMs: Long = 0L,
  val durationMs: Long = 3000L,
  val sourceStartMs: Long = 0L,
  val sourceEndMs: Long = 3000L,
  val speed: Float = 1.0f,
  val volume: Float = 1.0f,
  val rotationDegrees: Int = 0,
  val flipHorizontal: Boolean = false,
  val flipVertical: Boolean = false,
  val isMuted: Boolean = false,
  val cropScale: Float = 1.0f,
  val cropOffsetX: Float = 0f,
  val cropOffsetY: Float = 0f,
  val opacity: Float = 1.0f,
  val blendMode: String = "Normal",
  val width: Int = 1920,
  val height: Int = 1080,
  val naturalRotation: Int = 0,
  val frameRate: Float = 30f,
  val mimeType: String = "video/mp4",
  val hasAudio: Boolean = true,
  val isReversed: Boolean = false,
  val freezeFrameAtMs: Long? = null,
  val keyframes: List<ClipKeyframe> = emptyList()
) {
  fun timelineToSourceMs(timelinePosMs: Long): Long {
    val offset = (timelinePosMs - timelineStartMs).coerceIn(0L, durationMs)
    val scaledOffset = (offset * speed).toLong()
    return if (isReversed) {
      (sourceEndMs - scaledOffset).coerceIn(sourceStartMs, sourceEndMs)
    } else {
      (sourceStartMs + scaledOffset).coerceIn(sourceStartMs, sourceEndMs)
    }
  }
}

data class AudioClip(
  val id: String = UUID.randomUUID().toString(),
  val uri: String,
  val title: String,
  val timelineStartMs: Long = 0L,
  val durationMs: Long = 3000L,
  val sourceStartMs: Long = 0L,
  val sourceEndMs: Long = 3000L,
  val volume: Float = 1.0f,
  val speed: Float = 1.0f,
  val fadeInMs: Long = 0L,
  val fadeOutMs: Long = 0L,
  val isMuted: Boolean = false,
  val isVoiceOver: Boolean = false,
  val waveformData: List<Float> = emptyList(),
  val gainDb: Float = 0.0f
)

data class TextClip(
  val id: String = UUID.randomUUID().toString(),
  val text: String = "Tap to edit",
  val timelineStartMs: Long = 0L,
  val durationMs: Long = 3000L,
  val fontFamily: String = "Default",
  val fontSizeSp: Float = 24f,
  val fontWeight: Int = 700,
  val isItalic: Boolean = false,
  val alignment: String = "Center",
  val letterSpacing: Float = 0f,
  val lineSpacing: Float = 0f,
  val textColor: Long = 0xFFFFFFFF,
  val hasGradient: Boolean = false,
  val gradientColorStart: Long = 0xFF00E5FF,
  val gradientColorEnd: Long = 0xFF8B5CF6,
  val strokeWidth: Float = 0f,
  val strokeColor: Long = 0xFF000000,
  val hasShadow: Boolean = false,
  val shadowColor: Long = 0x88000000,
  val shadowBlur: Float = 4f,
  val hasBackground: Boolean = false,
  val backgroundColor: Long = 0xAA000000,
  val opacity: Float = 1.0f,
  val rotation: Float = 0f,
  val posX: Float = 0f, // -1f to 1f normalized
  val posY: Float = 0.3f, // -1f to 1f normalized
  val scale: Float = 1f,
  val animationType: String = "Fade", // "None", "Fade", "Slide", "Zoom", "Bounce", "Typewriter", "Pop", "Shake"
  val animDurationMs: Long = 400L
)

data class StickerClip(
  val id: String = UUID.randomUUID().toString(),
  val emojiOrAsset: String = "🎬",
  val timelineStartMs: Long = 0L,
  val durationMs: Long = 3000L,
  val posX: Float = 0f,
  val posY: Float = 0f,
  val scale: Float = 1f,
  val rotation: Float = 0f,
  val opacity: Float = 1f
)

enum class EffectType(val category: String, val displayName: String) {
  // Basic
  BLUR("Basic", "Blur"),
  SHARPEN("Basic", "Sharpen"),
  GLOW("Basic", "Glow"),
  NOISE("Basic", "Noise"),
  VIGNETTE("Basic", "Vignette"),
  // Motion
  SHAKE("Motion", "Camera Shake"),
  ZOOM("Motion", "Pulsing Zoom"),
  SPIN("Motion", "Slow Spin"),
  CAMERA_MOVEMENT("Motion", "Wander Pan"),
  // Light
  FLASH("Light", "Strobe Flash"),
  LENS_FLARE("Light", "Lens Flare"),
  LIGHT_LEAK("Light", "Warm Light Leak"),
  // Distortion
  RGB_SPLIT("Distortion", "RGB Glitch Split"),
  GLITCH("Distortion", "Digital Distortion"),
  WAVE("Distortion", "Wave Ripple"),
  RIPPLE("Distortion", "Shockwave")
}

data class EffectClip(
  val id: String = UUID.randomUUID().toString(),
  val effectType: EffectType = EffectType.GLOW,
  val timelineStartMs: Long = 0L,
  val durationMs: Long = 3000L,
  val intensity: Float = 0.8f
)

enum class TransitionType(val displayName: String) {
  NONE("None"),
  FADE("Fade"),
  DISSOLVE("Dissolve"),
  SLIDE_LEFT("Slide Left"),
  SLIDE_RIGHT("Slide Right"),
  PUSH_UP("Push Up"),
  ZOOM_IN("Zoom In"),
  ZOOM_OUT("Zoom Out"),
  SPIN("Spin 360"),
  BLUR("Blur Zoom"),
  FLASH("White Flash"),
  GLITCH("Glitch Cut"),
  WIPE("Wipe Curtain")
}

data class Transition(
  val id: String = UUID.randomUUID().toString(),
  val clipIndexBefore: Int = 0,
  val type: TransitionType = TransitionType.FADE,
  val durationMs: Long = 500L
)

data class VideoAdjustments(
  val brightness: Float = 0f,      // -1f to 1f
  val contrast: Float = 1f,        // 0f to 2f
  val saturation: Float = 1f,      // 0f to 2f
  val exposure: Float = 0f,        // -1f to 1f
  val temperature: Float = 0f,     // -1f (cool) to 1f (warm)
  val tint: Float = 0f,            // -1f (green) to 1f (magenta)
  val highlights: Float = 0f,      // -1f to 1f
  val shadows: Float = 0f,         // -1f to 1f
  val sharpness: Float = 0f,       // 0f to 1f
  val fade: Float = 0f,            // 0f to 1f
  val vignette: Float = 0f,        // 0f to 1f
  val grain: Float = 0f            // 0f to 1f
)

enum class FilterType(val displayName: String) {
  NONE("Normal"),
  CINEMATIC("Cinematic Teal & Orange"),
  VINTAGE("Vintage 1970s"),
  WARM("Golden Warm"),
  COOL("Arctic Cool"),
  PORTRAIT("Portrait Soft"),
  BLACK_AND_WHITE("Noir B&W"),
  HDR("High Dynamic Range"),
  FILM("35mm Film Grain"),
  RETRO("80s Retro Synth"),
  NATURE("Vibrant Nature"),
  FOOD("Rich Warm Food"),
  TRAVEL("Mediterranean Travel"),
  SOCIAL_MEDIA("Hyper Vivid")
}

data class FilterSettings(
  val type: FilterType = FilterType.NONE,
  val intensity: Float = 1.0f // 0f to 1f
)

data class ChromaKeySettings(
  val enabled: Boolean = false,
  val targetColor: Long = 0xFF00FF00, // Green Screen
  val intensity: Float = 0.5f,
  val shadow: Float = 0.3f,
  val edgeAdjustment: Float = 0.2f,
  val spillReduction: Float = 0.4f,
  val backgroundType: String = "SolidColor", // "SolidColor", "Image"
  val backgroundColor: Long = 0xFF000000,
  val backgroundUri: String? = null
)

data class Timeline(
  val videoClips: List<VideoClip> = emptyList(),
  val overlayClips: List<VideoClip> = emptyList(),
  val audioClips: List<AudioClip> = emptyList(),
  val textClips: List<TextClip> = emptyList(),
  val stickerClips: List<StickerClip> = emptyList(),
  val effectClips: List<EffectClip> = emptyList(),
  val transitions: List<Transition> = emptyList(),
  val adjustments: VideoAdjustments = VideoAdjustments(),
  val filter: FilterSettings = FilterSettings(),
  val chromaKey: ChromaKeySettings = ChromaKeySettings(),
  val canvasBackgroundColor: Long = 0xFF000000,
  val aspectRatio: AspectRatio = AspectRatio.RATIO_9_16
) {
  val totalDurationMs: Long
    get() {
      val videoDur = videoClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
      val overlayDur = overlayClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
      val audioDur = audioClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
      val textDur = textClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
      val stickerDur = stickerClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
      val effectDur = effectClips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
      return maxOf(videoDur, overlayDur, audioDur, textDur, stickerDur, effectDur).coerceAtLeast(3000L)
    }
}
