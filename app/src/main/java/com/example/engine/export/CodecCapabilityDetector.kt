package com.example.engine.export

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import com.example.domain.model.Resolution

data class DeviceCodecCapabilities(
  val supportsH264Hardware: Boolean = true,
  val supportsH265Hardware: Boolean = false,
  val maxSupportedWidth: Int = 1920,
  val maxSupportedHeight: Int = 1080,
  val maxSupportedFps: Int = 60,
  val maxSupportedBitrateBps: Int = 20_000_000,
  val isLowEndDevice: Boolean = false,
  val recommendedResolution: Resolution = Resolution.RES_1080P
)

object CodecCapabilityDetector {
  private const val TAG = "CodecCapabilityDetector"

  fun detectCapabilities(): DeviceCodecCapabilities {
    var h264Hw = false
    var h265Hw = false
    var maxWidth = 1920
    var maxHeight = 1080
    var maxFps = 30
    var maxBitrate = 15_000_000

    try {
      val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
      val codecInfos = codecList.codecInfos

      for (info in codecInfos) {
        if (!info.isEncoder) continue

        val types = info.supportedTypes
        for (type in types) {
          if (type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true)) {
            val isHw = isHardwareCodec(info)
            if (isHw) h264Hw = true

            try {
              val caps = info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
              val videoCaps = caps.videoCapabilities
              if (videoCaps != null) {
                maxWidth = maxOf(maxWidth, videoCaps.supportedWidths.upper)
                maxHeight = maxOf(maxHeight, videoCaps.supportedHeights.upper)
                maxFps = maxOf(maxFps, videoCaps.supportedFrameRates.upper)
                maxBitrate = maxOf(maxBitrate, 25_000_000)
              }
            } catch (ignored: Exception) {}
          } else if (type.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true)) {
            val isHw = isHardwareCodec(info)
            if (isHw) h265Hw = true
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error detecting hardware codec capabilities", e)
    }

    val isLowEnd = maxWidth < 1920 || !h264Hw
    val recommendedRes = when {
      maxWidth >= 3840 -> Resolution.RES_4K
      maxWidth >= 2560 -> Resolution.RES_2K
      else -> Resolution.RES_1080P
    }

    Log.d(
      TAG,
      "Codec capability scan complete: H264_HW=$h264Hw, H265_HW=$h265Hw, MaxRes=${maxWidth}x${maxHeight}, MaxFps=$maxFps, LowEnd=$isLowEnd"
    )

    return DeviceCodecCapabilities(
      supportsH264Hardware = h264Hw,
      supportsH265Hardware = h265Hw,
      maxSupportedWidth = maxWidth,
      maxSupportedHeight = maxHeight,
      maxSupportedFps = maxFps,
      maxSupportedBitrateBps = maxBitrate,
      isLowEndDevice = isLowEnd,
      recommendedResolution = recommendedRes
    )
  }

  private fun isHardwareCodec(info: MediaCodecInfo): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      return info.isHardwareAccelerated
    }
    val name = info.name.lowercase()
    return !name.startsWith("omx.google.") &&
      !name.startsWith("c2.android.") &&
      !name.contains("sw")
  }
}
