package com.example.domain.model

enum class AspectRatio(val label: String, val ratio: Float, val iconDesc: String) {
  RATIO_9_16("9:16", 9f / 16f, "TikTok / Reels / Shorts"),
  RATIO_16_9("16:9", 16f / 9f, "YouTube / Landscape"),
  RATIO_1_1("1:1", 1f, "Instagram Square"),
  RATIO_4_5("4:5", 4f / 5f, "Instagram Portrait"),
  RATIO_3_4("3:4", 3f / 4f, "Classic Portrait"),
  CUSTOM("Custom", 1f, "Freeform")
}

enum class Resolution(val label: String, val width: Int, val height: Int) {
  RES_480P("480p", 480, 854),
  RES_720P("720p", 720, 1280),
  RES_1080P("1080p", 1080, 1920),
  RES_2K("2K", 1440, 2560),
  RES_4K("4K", 2160, 3840)
}

enum class FrameRate(val fps: Int) {
  FPS_24(24),
  FPS_25(25),
  FPS_30(30),
  FPS_50(50),
  FPS_60(60)
}

enum class ExportQuality(val label: String, val bitrateMultiplier: Float) {
  LOW("Low (Fast)", 0.6f),
  MEDIUM("Medium (Balanced)", 1.0f),
  HIGH("High (Pristine)", 1.5f),
  CUSTOM("Custom (Pro Bitrate)", 2.0f)
}
