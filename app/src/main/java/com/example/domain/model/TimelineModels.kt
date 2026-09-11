package com.example.domain.model

import java.util.UUID

enum class KeyframeInterpolation(val displayName: String) {
  LINEAR("Linear"),
  EASE_IN("Ease In"),
  EASE_OUT("Ease Out"),
  EASE_IN_OUT("Ease In-Out"),
  CUSTOM_CURVE("Custom Curve");

  companion object {
    fun fromString(str: String): KeyframeInterpolation {
      return when (str.trim().lowercase()) {
        "linear" -> LINEAR
        "ease in", "easein", "ease_in" -> EASE_IN
        "ease out", "easeout", "ease_out" -> EASE_OUT
        "ease in-out", "easeinout", "ease_in_out", "smoothease" -> EASE_IN_OUT
        "custom curve", "custom", "bezier" -> CUSTOM_CURVE
        else -> LINEAR
      }
    }
  }
}

data class ClipKeyframe(
  val id: String = UUID.randomUUID().toString(),
  val timeMs: Long,
  val posX: Float = 0f,
  val posY: Float = 0f,
  val scaleX: Float = 1f,
  val scaleY: Float = 1f,
  val rotation: Float = 0f,
  val opacity: Float = 1f,
  val volume: Float = 1f,
  val blur: Float = 0f,
  val brightness: Float = 0f,
  val contrast: Float = 1f,
  val saturation: Float = 1f,
  val effectParam: Float = 0f,
  val interpolation: KeyframeInterpolation = KeyframeInterpolation.LINEAR,
  val customCurvePoints: List<Float> = listOf(0.42f, 0.0f, 0.58f, 1.0f) // P1x, P1y, P2x, P2y
) {
  val scale: Float get() = (scaleX + scaleY) / 2f

  constructor(
    id: String = UUID.randomUUID().toString(),
    timeMs: Long,
    scale: Float,
    rotation: Float = 0f,
    posX: Float = 0f,
    posY: Float = 0f,
    opacity: Float = 1f,
    volume: Float = 1f,
    interpolation: String = "Linear"
  ) : this(
    id = id,
    timeMs = timeMs,
    posX = posX,
    posY = posY,
    scaleX = scale,
    scaleY = scale,
    rotation = rotation,
    opacity = opacity,
    volume = volume,
    blur = 0f,
    brightness = 0f,
    contrast = 1f,
    saturation = 1f,
    effectParam = 0f,
    interpolation = KeyframeInterpolation.fromString(interpolation)
  )
}

enum class InAnimationType(val displayName: String, val category: String = "Popular") {
  NONE("None", "Basic"),
  FADE_IN("Fade In", "Fade & Zoom"),
  ZOOM_IN("Zoom In", "Fade & Zoom"),
  ZOOM_OUT("Zoom Out", "Fade & Zoom"),
  SLIDE_UP("Slide Up", "Slide"),
  SLIDE_DOWN("Slide Down", "Slide"),
  SLIDE_LEFT("Slide Left", "Slide"),
  SLIDE_RIGHT("Slide Right", "Slide"),
  SPIN_IN("Spin In", "Motion"),
  BOUNCE_IN("Bounce In", "Motion"),
  POP_IN("Pop In", "Motion"),
  FLIP_X("Flip Horizontal", "3D"),
  FLIP_Y("Flip Vertical", "3D"),
  SWING_IN("Swing In", "Motion"),
  ELASTIC_IN("Elastic In", "Dynamic"),
  GLITCH_IN("Glitch In", "Distortion"),
  WIPE_IN("Wipe In", "Basic"),
  BLUR_IN("Blur In", "Blur")
}

enum class OutAnimationType(val displayName: String, val category: String = "Popular") {
  NONE("None", "Basic"),
  FADE_OUT("Fade Out", "Fade & Zoom"),
  ZOOM_OUT("Zoom Out", "Fade & Zoom"),
  ZOOM_IN_OUT("Zoom In Disappear", "Fade & Zoom"),
  SLIDE_UP_OUT("Slide Up", "Slide"),
  SLIDE_DOWN_OUT("Slide Down", "Slide"),
  SLIDE_LEFT_OUT("Slide Left", "Slide"),
  SLIDE_RIGHT_OUT("Slide Right", "Slide"),
  SPIN_OUT("Spin Out", "Motion"),
  BOUNCE_OUT("Bounce Out", "Motion"),
  POP_OUT("Pop Out", "Motion"),
  FLIP_X_OUT("Flip Out", "3D"),
  SWING_OUT("Swing Out", "Motion"),
  GLITCH_OUT("Glitch Out", "Distortion"),
  WIPE_OUT("Wipe Out", "Basic"),
  BLUR_OUT("Blur Out", "Blur")
}

enum class ComboAnimationType(val displayName: String, val category: String = "Loop & Rhythm") {
  NONE("None", "Basic"),
  PULSE("Pulse Beat", "Rhythm"),
  HEARTBEAT("Heartbeat", "Rhythm"),
  PENDULUM("Pendulum Swing", "Motion"),
  FLOAT("Floating Drift", "Motion"),
  SHAKE("Camera Shake", "Distortion"),
  JITTER("Glitch Jitter", "Distortion"),
  FLASH_PULSE("Flash Strobe", "Lighting"),
  WAVE("Wave Wobble", "Motion"),
  SPIN_360("Spin 360 Loop", "Motion"),
  BREATHE("Breathe Flow", "Rhythm"),
  ZOOM_PULSE("Zoom Rhythm", "Rhythm")
}

enum class AnimationEasing(val displayName: String) {
  EASE_OUT("Ease Out (Smooth)"),
  EASE_IN("Ease In (Accelerate)"),
  EASE_IN_OUT("Ease In-Out"),
  LINEAR("Linear (Constant)"),
  OVERSHOOT("Overshoot (Spring)"),
  BOUNCE("Bounce"),
  ELASTIC("Elastic")
}

data class ClipAnimationSettings(
  val inType: InAnimationType = InAnimationType.NONE,
  val inDurationMs: Long = 500L,
  val outType: OutAnimationType = OutAnimationType.NONE,
  val outDurationMs: Long = 500L,
  val comboType: ComboAnimationType = ComboAnimationType.NONE,
  val intensity: Float = 1.0f,
  val easing: AnimationEasing = AnimationEasing.EASE_OUT,
  val speed: Float = 1.0f
) {
  val hasAnimation: Boolean
    get() = inType != InAnimationType.NONE || outType != OutAnimationType.NONE || comboType != ComboAnimationType.NONE
}

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
  val sourceTotalDurationMs: Long = 0L,
  val keyframes: List<ClipKeyframe> = emptyList(),
  val filter: FilterSettings? = null,
  val animation: ClipAnimationSettings = ClipAnimationSettings()
) {
  val totalMediaDurationMs: Long
    get() = if (sourceTotalDurationMs > 0L) sourceTotalDurationMs else maxOf(sourceEndMs, durationMs)

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
  val gainDb: Float = 0.0f,
  val isReversed: Boolean = false,
  val keyframes: List<ClipKeyframe> = emptyList()
)

data class WordTiming(
  val word: String,
  val startMs: Long,
  val durationMs: Long
)

data class TextClip(
  val id: String = UUID.randomUUID().toString(),
  val text: String = "Tap to edit",
  val timelineStartMs: Long = 0L,
  val durationMs: Long = 3000L,
  val fontFamily: String = "Default",
  val customFontPath: String? = null,
  val fontSizeSp: Float = 24f,
  val fontWeight: Int = 700,
  val isItalic: Boolean = false,
  val isUnderline: Boolean = false,
  val isAllCaps: Boolean = false,
  val alignment: String = "Center",
  val letterSpacing: Float = 0f,
  val lineSpacing: Float = 1.0f,
  val textColor: Long = 0xFFFFFFFF,
  val hasGradient: Boolean = false,
  val gradientColorStart: Long = 0xFF00E5FF,
  val gradientColorEnd: Long = 0xFF8B5CF6,
  val gradientDirection: String = "Horizontal",
  val strokeWidth: Float = 0f,
  val strokeColor: Long = 0xFF000000,
  val hasShadow: Boolean = false,
  val shadowColor: Long = 0x88000000,
  val shadowBlur: Float = 4f,
  val shadowOffsetX: Float = 2f,
  val shadowOffsetY: Float = 2f,
  val hasBackground: Boolean = false,
  val backgroundColor: Long = 0xAA000000,
  val cornerRadius: Float = 12f,
  val bgPadding: Float = 16f,
  val opacity: Float = 1.0f,
  val rotation: Float = 0f,
  val posX: Float = 0f, // -1f to 1f normalized
  val posY: Float = 0.35f, // -1f to 1f normalized
  val scale: Float = 1f,
  val animationType: String = "Fade", // "None", "Fade", "Slide", "Zoom", "Bounce", "Typewriter", "Pop", "Shake"
  val animDurationMs: Long = 400L,
  val subtitleStyle: String = "Classic", // "Classic", "Bold", "HighlightWord", "Karaoke", "Animated"
  val highlightColor: Long = 0xFFFFEB3B,
  val words: List<WordTiming> = emptyList()
)

enum class StickerAnimationType(val displayName: String) {
  NONE("Static"),
  PULSE("Pulse"),
  HEARTBEAT("Heartbeat"),
  BOUNCE("Bounce"),
  SPIN("Spin 360°"),
  SHAKE("Shake"),
  FLOAT("Float"),
  SWING("Swing"),
  GLOW_PULSE("Glow Pulse"),
  POP_IN("Pop In")
}

enum class BadgeType(
  val displayName: String,
  val subtitle: String,
  val primaryColor: Long,
  val secondaryColor: Long,
  val icon: String
) {
  NEW("NEW", "Fresh Arrival", 0xFF06B6D4, 0xFF0284C7, "✨"),
  SALE("SALE", "Special Discount", 0xFFEF4444, 0xFFB91C1C, "🏷️"),
  HOT("HOT", "Trending Now", 0xFFF97316, 0xFFDC2626, "🔥"),
  TRENDING("TRENDING", "Viral Hit", 0xFF8B5CF6, 0xFF6366F1, "📈"),
  BEST_SELLER("BEST SELLER", "#1 Top Choice", 0xFFF59E0B, 0xFFD97706, "👑"),
  LIMITED_EDITION("LIMITED EDITION", "Exclusive Drop", 0xFF334155, 0xFFF59E0B, "⏳"),
  PREMIUM("PREMIUM", "VIP Quality", 0xFF7C3AED, 0xFF4F46E5, "💎"),
  SPECIAL_OFFER("SPECIAL OFFER", "Save Big", 0xFFEAB308, 0xFFCA8A04, "🎁"),
  DISCOUNT("DISCOUNT", "Price Drop", 0xFF10B981, 0xFF059669, "💸"),
  RECOMMENDED("RECOMMENDED", "Staff Pick", 0xFF0284C7, 0xFF0369A1, "👍"),
  FEATURED("FEATURED", "Spotlight", 0xFF6366F1, 0xFF4338CA, "🌟"),
  EXCLUSIVE("EXCLUSIVE", "Members Only", 0xFFBE185D, 0xFF831843, "🔒"),
  VERIFIED("VERIFIED", "Official Badge", 0xFF0EA5E9, 0xFF0284C7, "✔️"),
  TOP_RATED("TOP RATED", "5-Star Quality", 0xFFFBBF24, 0xFFF59E0B, "⭐"),
  FREE("FREE", "No Cost", 0xFF22C55E, 0xFF16A34A, "🆓"),
  COMING_SOON("COMING SOON", "Stay Tuned", 0xFFFB923C, 0xFFEA580C, "🚀")
}

data class StickerClip(
  val id: String = UUID.randomUUID().toString(),
  val emojiOrAsset: String = "🎬",
  val timelineStartMs: Long = 0L,
  val durationMs: Long = 3000L,
  val posX: Float = 0f,
  val posY: Float = 0f,
  val scale: Float = 1f,
  val rotation: Float = 0f,
  val opacity: Float = 1f,
  val animationType: StickerAnimationType = StickerAnimationType.NONE,
  val badgeType: BadgeType? = null,
  val category: String = "Emoji & Emotions"
)

enum class EffectType(val category: String, val displayName: String) {
  // Blur & Glow
  BLUR("Basic", "Blur"),
  GLOW("Basic", "Glow"),
  MOTION_BLUR("Motion", "Motion Blur"),
  // Motion
  SHAKE("Motion", "Shake"),
  ZOOM("Motion", "Zoom"),
  SPIN("Motion", "Spin"),
  // Light
  FLASH("Light", "Flash"),
  LENS_FLARE("Light", "Lens Flare"),
  LIGHT_LEAK("Light", "Light Leak"),
  // Distortion & Glitch
  GLITCH("Distortion", "Glitch"),
  RGB_SPLIT("Distortion", "RGB Split"),
  DISTORTION("Distortion", "Distortion"),
  // Additional presets
  SHARPEN("Basic", "Sharpen"),
  NOISE("Basic", "Noise"),
  VIGNETTE("Basic", "Vignette"),
  CAMERA_MOVEMENT("Motion", "Wander Pan"),
  WAVE("Distortion", "Wave Ripple"),
  RIPPLE("Distortion", "Shockwave")
}

data class EffectClip(
  val id: String = UUID.randomUUID().toString(),
  val effectType: EffectType = EffectType.GLOW,
  val timelineStartMs: Long = 0L,
  val durationMs: Long = 3000L,
  val intensity: Float = 0.8f,
  val keyframes: List<ClipKeyframe> = emptyList()
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

enum class FilterType(val displayName: String, val category: String = "Pro Enhancements") {
  NONE("Original", "All"),
  FOUR_K("4K", "Pro Enhancements"),
  BLACKLIGHT_FIX("Blacklight Fix", "Pro Enhancements"),
  ENHANCE("Enhance", "Pro Enhancements"),
  HDR("HDR", "Pro Enhancements"),
  GLOW("Glow", "Pro Enhancements"),
  FOCUS("Focus", "Pro Enhancements"),
  QUALITY_RESTORATION("Quality Restoration", "Pro Enhancements"),
  GOLDEN_AUTUMN("Golden Autumn", "Cinematic & Nature"),
  OCEANIC_VIEW("Oceanic View", "Cinematic & Nature"),
  ALMOND("Almond", "Aesthetic Looks"),
  SUNLIGHT_ORANGE_BLUE("Sunlight Orange Blue", "Cinematic & Nature"),
  
  // Classic / Creative presets
  CINEMATIC("Cinematic Teal & Orange", "Cinematic & Nature"),
  WARM("Golden Warm", "Aesthetic Looks"),
  COOL("Arctic Cool", "Aesthetic Looks"),
  PORTRAIT("Portrait Soft", "Aesthetic Looks"),
  BLACK_AND_WHITE("Black & White", "Aesthetic Looks"),
  VINTAGE("Vintage 1970s", "Aesthetic Looks"),
  SATURATION("Saturation Boost", "Pro Enhancements"),
  FILM("35mm Film Grain", "Cinematic & Nature"),
  RETRO("80s Retro Synth", "Aesthetic Looks"),
  NATURE("Vibrant Nature", "Cinematic & Nature"),
  FOOD("Rich Warm Food", "Aesthetic Looks"),
  TRAVEL("Mediterranean Travel", "Cinematic & Nature"),
  SOCIAL_MEDIA("Hyper Vivid", "Pro Enhancements")
}

data class FilterSettings(
  val type: FilterType = FilterType.NONE,
  val intensity: Float = 1.0f // 0f to 1f
)

data class ChromaKeySettings(
  val enabled: Boolean = false,
  val targetColor: Long = 0xFF00FF00, // Green Screen default
  val similarity: Float = 0.4f,       // Similarity / distance threshold (0.0 to 1.0)
  val smoothness: Float = 0.15f,      // Smoothness / feathering (0.0 to 1.0)
  val spillSuppression: Float = 0.5f, // Spill suppression (0.0 to 1.0)
  val edgeControl: Float = 0.0f,      // Edge control: choke/expand (-1.0 to 1.0)
  val backgroundType: String = "SolidColor", // "SolidColor", "Image", "Video", "Transparent"
  val backgroundColor: Long = 0xFF000000,
  val backgroundUri: String? = null,
  val intensity: Float = similarity,
  val shadow: Float = 0.3f,
  val edgeAdjustment: Float = smoothness,
  val spillReduction: Float = spillSuppression
)

enum class TrackType {
  MAIN_VIDEO,
  OVERLAY,
  TEXT,
  AUDIO,
  STICKER,
  EFFECT
}

enum class TrackHeight(val label: String, val heightDp: Int) {
  COMPACT("Compact", 40),
  NORMAL("Normal", 56),
  EXPANDED("Expanded", 78)
}

data class TrackSettings(
  val type: TrackType,
  val isLocked: Boolean = false,
  val isHidden: Boolean = false,
  val isMuted: Boolean = false,
  val isSolo: Boolean = false,
  val height: TrackHeight = TrackHeight.NORMAL
)

fun defaultTrackSettings(): Map<TrackType, TrackSettings> {
  return TrackType.values().associateWith { TrackSettings(it) }
}

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
  val aspectRatio: AspectRatio = AspectRatio.RATIO_9_16,
  val trackSettings: Map<TrackType, TrackSettings> = defaultTrackSettings()
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
