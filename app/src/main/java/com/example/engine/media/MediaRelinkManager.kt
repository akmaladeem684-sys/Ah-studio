package com.example.engine.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.model.AudioClip
import com.example.domain.model.MissingMediaItem
import com.example.domain.model.Timeline
import com.example.domain.model.VideoClip
import java.io.File

object MediaRelinkManager {

  /**
   * Verifies whether a given URI or file path is currently accessible on the device.
   */
  fun isMediaAccessible(context: Context, uriString: String?): Boolean {
    if (uriString.isNullOrBlank()) return false

    // Sample / synthetic demo assets are always accessible
    if (uriString.startsWith("demo://") ||
      uriString.startsWith("sample://") ||
      uriString.startsWith("asset://") ||
      uriString.startsWith("internal://") ||
      uriString.startsWith("android.resource://")
    ) {
      return true
    }

    return try {
      val parsedUri = Uri.parse(uriString)
      when (parsedUri.scheme) {
        "content" -> {
          // Check if ContentResolver can open or query the content URI
          val fd = context.contentResolver.openFileDescriptor(parsedUri, "r")
          if (fd != null) {
            fd.close()
            true
          } else {
            false
          }
        }
        "file" -> {
          val path = parsedUri.path ?: ""
          File(path).exists()
        }
        null -> {
          File(uriString).exists()
        }
        else -> {
          // For http / https or unknown schemes, check basic structure
          uriString.isNotBlank()
        }
      }
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Resolves a human-readable display filename from a file path or content URI.
   */
  fun resolveFileName(context: Context, uriString: String?): String {
    if (uriString.isNullOrBlank()) return "Untitled Media"
    return try {
      val uri = Uri.parse(uriString)
      if (uri.scheme == "content") {
        var name: String? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) name = cursor.getString(idx)
          }
        }
        name ?: uri.lastPathSegment ?: "Media File"
      } else {
        val f = File(uri.path ?: uriString)
        f.name.ifBlank { "Media File" }
      }
    } catch (e: Exception) {
      uriString.substringAfterLast("/").ifBlank { "Media File" }
    }
  }

  /**
   * Scans the timeline for any media clips whose referenced files are no longer accessible.
   */
  fun detectMissingMedia(context: Context, timeline: Timeline): List<MissingMediaItem> {
    val missingList = mutableListOf<MissingMediaItem>()

    // Check Main Video Clips
    timeline.videoClips.forEach { clip ->
      if (!isMediaAccessible(context, clip.uri)) {
        missingList.add(
          MissingMediaItem(
            clipId = clip.id,
            clipName = clip.name,
            currentUri = clip.uri,
            originalFilename = resolveFileName(context, clip.uri),
            mediaType = if (clip.isVideo) "VIDEO" else "IMAGE",
            trackType = "Main Video"
          )
        )
      }
    }

    // Check Overlay Clips
    timeline.overlayClips.forEach { clip ->
      if (!isMediaAccessible(context, clip.uri)) {
        missingList.add(
          MissingMediaItem(
            clipId = clip.id,
            clipName = clip.name,
            currentUri = clip.uri,
            originalFilename = resolveFileName(context, clip.uri),
            mediaType = if (clip.isVideo) "VIDEO" else "IMAGE",
            trackType = "Overlay"
          )
        )
      }
    }

    // Check Audio Clips
    timeline.audioClips.forEach { clip ->
      if (!isMediaAccessible(context, clip.uri)) {
        missingList.add(
          MissingMediaItem(
            clipId = clip.id,
            clipName = clip.title,
            currentUri = clip.uri,
            originalFilename = resolveFileName(context, clip.uri),
            mediaType = "AUDIO",
            trackType = "Audio"
          )
        )
      }
    }

    // Check Chroma Key background image/video if set
    val bgUri = timeline.chromaKey.backgroundUri
    if (!bgUri.isNullOrBlank() && (timeline.chromaKey.backgroundType == "Image" || timeline.chromaKey.backgroundType == "Video")) {
      if (!isMediaAccessible(context, bgUri)) {
        missingList.add(
          MissingMediaItem(
            clipId = "chroma_background",
            clipName = "Chroma Key Background",
            currentUri = bgUri,
            originalFilename = resolveFileName(context, bgUri),
            mediaType = "IMAGE",
            trackType = "Chroma Background"
          )
        )
      }
    }

    return missingList
  }

  /**
   * Relinks a specific missing media item to a newly provided URI and updates metadata.
   */
  fun relinkClip(context: Context, timeline: Timeline, clipId: String, newUri: String): Timeline {
    val meta = MediaMetadataHelper.extractMetadata(context, newUri)
    val newFileName = resolveFileName(context, newUri)

    var updated = timeline

    // Check main video clips
    val newVideoClips = timeline.videoClips.map { clip ->
      if (clip.id == clipId) {
        clip.copy(
          uri = newUri,
          name = if (clip.name.startsWith("Clip ") || clip.name == "Media File") newFileName else clip.name,
          width = meta.width,
          height = meta.height,
          frameRate = meta.frameRate,
          mimeType = meta.mimeType,
          hasAudio = meta.hasAudio,
          sourceEndMs = if (meta.durationMs > 0) minOf(clip.sourceEndMs, meta.durationMs) else clip.sourceEndMs
        )
      } else clip
    }
    updated = updated.copy(videoClips = newVideoClips)

    // Check overlay clips
    val newOverlayClips = timeline.overlayClips.map { clip ->
      if (clip.id == clipId) {
        clip.copy(
          uri = newUri,
          name = if (clip.name.startsWith("Clip ") || clip.name == "Media File") newFileName else clip.name,
          width = meta.width,
          height = meta.height,
          frameRate = meta.frameRate,
          mimeType = meta.mimeType,
          hasAudio = meta.hasAudio
        )
      } else clip
    }
    updated = updated.copy(overlayClips = newOverlayClips)

    // Check audio clips
    val newAudioClips = timeline.audioClips.map { clip ->
      if (clip.id == clipId) {
        clip.copy(
          uri = newUri,
          title = if (clip.title.startsWith("Audio ") || clip.title == "Media File") newFileName else clip.title,
          sourceEndMs = if (meta.durationMs > 0) minOf(clip.sourceEndMs, meta.durationMs) else clip.sourceEndMs
        )
      } else clip
    }
    updated = updated.copy(audioClips = newAudioClips)

    // Check chroma background
    if (clipId == "chroma_background") {
      updated = updated.copy(
        chromaKey = updated.chromaKey.copy(backgroundUri = newUri)
      )
    }

    return updated
  }
}
