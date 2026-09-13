package com.example.domain

import com.example.data.presets.MediaPlaceholder
import com.example.data.presets.PlaceholderType
import com.example.data.presets.TextPlaceholder
import com.example.data.presets.VideoTemplate
import com.example.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class UserProfile(
  val name: String = "Akmal Adeem",
  val handle: String = "@akmal_creator_pro",
  val email: String = "akmaladeem684@gmail.com",
  val bio: String = "4K Cinematic Video Director & Motion Graphics Specialist 🎬",
  val planName: String = "Pro VIP Creator Pass",
  val isPro: Boolean = true,
  val avatarColor: Long = 0xFF00E5FF
)

data class SocialAccountConnection(
  val platformName: String,
  val platformIcon: String,
  val handle: String,
  val isConnected: Boolean
)

data class UserAccountProfile(
  val id: String,
  val name: String,
  val email: String,
  val avatarColor: Long,
  val isCurrent: Boolean
)

object StudioAccountManager {
  private val _profile = MutableStateFlow(UserProfile())
  val profile: StateFlow<UserProfile> = _profile.asStateFlow()

  private val _savedTemplateIds = MutableStateFlow<Set<String>>(
    setOf("tpl_reels_beat_pulse", "tpl_yt_tech_review")
  )
  val savedTemplateIds: StateFlow<Set<String>> = _savedTemplateIds.asStateFlow()

  private val _customTemplates = MutableStateFlow<List<VideoTemplate>>(
    listOf(createInitialCustomTemplate())
  )
  val customTemplates: StateFlow<List<VideoTemplate>> = _customTemplates.asStateFlow()

  private val _socialConnections = MutableStateFlow<List<SocialAccountConnection>>(
    listOf(
      SocialAccountConnection("YouTube", "🔴", "@AkmalStudioPro", true),
      SocialAccountConnection("TikTok", "🎵", "@akmal_edits", true),
      SocialAccountConnection("Instagram", "📸", "@akmal.reels", true),
      SocialAccountConnection("X / Twitter", "🐦", "@akmal_dev", false)
    )
  )
  val socialConnections: StateFlow<List<SocialAccountConnection>> = _socialConnections.asStateFlow()

  private val _userAccounts = MutableStateFlow<List<UserAccountProfile>>(
    listOf(
      UserAccountProfile("acc_1", "Akmal Adeem", "akmaladeem684@gmail.com", 0xFF00E5FF, true),
      UserAccountProfile("acc_2", "AH Studio Official", "business@ahvideostudio.com", 0xFF9333EA, false),
      UserAccountProfile("acc_3", "Personal Channel", "personal@creator.io", 0xFF10B981, false)
    )
  )
  val userAccounts: StateFlow<List<UserAccountProfile>> = _userAccounts.asStateFlow()

  fun updateProfile(name: String, handle: String, bio: String, email: String) {
    _profile.value = _profile.value.copy(
      name = name.ifBlank { "Creator" },
      handle = if (handle.startsWith("@")) handle else "@$handle",
      bio = bio,
      email = email
    )
  }

  fun toggleSavedTemplate(templateId: String) {
    val current = _savedTemplateIds.value.toMutableSet()
    if (current.contains(templateId)) {
      current.remove(templateId)
    } else {
      current.add(templateId)
    }
    _savedTemplateIds.value = current
  }

  fun saveProjectAsTemplate(
    title: String,
    category: String,
    description: String,
    timeline: Timeline,
    aspectRatio: AspectRatio
  ): VideoTemplate {
    val newId = "cust_tpl_${UUID.randomUUID().toString().take(8)}"
    val dur = timeline.totalDurationMs.coerceAtLeast(3000L)
    val template = VideoTemplate(
      id = newId,
      title = title.ifBlank { "Untitled Template" },
      category = if (category.isBlank()) "User-Created" else category,
      description = description.ifBlank { "Custom template created in AH Video Studio" },
      aspectRatio = aspectRatio,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = dur,
      thumbnailGradientStart = 0xFF3B82F6,
      thumbnailGradientEnd = 0xFF8B5CF6,
      iconEmoji = "🎬",
      audioTitle = "Project Soundtrack",
      savedTimeline = timeline.copy(),
      createTimeline = { _, _ -> timeline.copy() }
    )

    _customTemplates.value = listOf(template) + _customTemplates.value
    val currentSaved = _savedTemplateIds.value.toMutableSet()
    currentSaved.add(newId)
    _savedTemplateIds.value = currentSaved
    return template
  }

  fun duplicateCustomTemplate(templateId: String): VideoTemplate? {
    val target = _customTemplates.value.find { it.id == templateId }
      ?: com.example.data.presets.TemplatesCatalog.templates.find { it.id == templateId }
      ?: return null

    val copyId = "cust_tpl_${UUID.randomUUID().toString().take(8)}"
    val duplicated = target.copy(
      id = copyId,
      title = "${target.title} (Copy)",
      category = if (target.category.isBlank()) "User-Created" else target.category,
      savedTimeline = target.savedTimeline ?: target.createDefaultTimeline()
    )

    _customTemplates.value = listOf(duplicated) + _customTemplates.value
    val currentSaved = _savedTemplateIds.value.toMutableSet()
    currentSaved.add(copyId)
    _savedTemplateIds.value = currentSaved
    return duplicated
  }

  fun deleteCustomTemplate(templateId: String) {
    _customTemplates.value = _customTemplates.value.filterNot { it.id == templateId }
    val currentSaved = _savedTemplateIds.value.toMutableSet()
    currentSaved.remove(templateId)
    _savedTemplateIds.value = currentSaved
  }

  private fun createInitialCustomTemplate(): VideoTemplate {
    val sampleTimeline = Timeline(
      videoClips = listOf(
        VideoClip(
          id = "cust_v1",
          name = "Cinematic Travel Hook.mp4",
          uri = "sample_travel_hook",
          timelineStartMs = 0L,
          durationMs = 3000L,
          sourceStartMs = 0L,
          sourceEndMs = 3000L,
          speed = 1.0f,
          volume = 0.9f
        ),
        VideoClip(
          id = "cust_v2",
          name = "Sunset Cityscape.mp4",
          uri = "sample_sunset_city",
          timelineStartMs = 3000L,
          durationMs = 3000L,
          sourceStartMs = 0L,
          sourceEndMs = 3000L,
          speed = 1.1f,
          volume = 0.9f
        )
      ),
      textClips = listOf(
        TextClip(
          id = "cust_t1",
          text = "DAILY VLOG OPENER",
          timelineStartMs = 500L,
          durationMs = 3500L,
          fontFamily = "Montserrat-Bold",
          fontSizeSp = 40f,
          textColor = 0xFF00E5FF
        )
      ),
      audioClips = listOf(
        AudioClip(
          id = "cust_a1",
          title = "Ambient Morning Chill.mp3",
          uri = "sample_ambient_chill",
          timelineStartMs = 0L,
          durationMs = 6000L,
          volume = 0.8f
        )
      ),
      filter = FilterSettings(FilterType.CINEMATIC, 0.85f)
    )

    return VideoTemplate(
      id = "cust_tpl_vlog_intro",
      title = "My Daily Vlog Opener",
      category = "User-Created",
      description = "Personalized cinematic travel opener with atmospheric text animations and custom color grade.",
      aspectRatio = AspectRatio.RATIO_16_9,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = 6000L,
      thumbnailGradientStart = 0xFF10B981,
      thumbnailGradientEnd = 0xFF3B82F6,
      iconEmoji = "✨",
      audioTitle = "Ambient Morning Chill",
      savedTimeline = sampleTimeline,
      createTimeline = { _, _ -> sampleTimeline }
    )
  }

  fun toggleSocialConnection(platformName: String) {
    _socialConnections.value = _socialConnections.value.map { conn ->
      if (conn.platformName == platformName) {
        conn.copy(isConnected = !conn.isConnected)
      } else {
        conn
      }
    }
  }

  fun switchActiveAccount(accountId: String) {
    _userAccounts.value = _userAccounts.value.map { acc ->
      acc.copy(isCurrent = acc.id == accountId)
    }
    _userAccounts.value.find { it.id == accountId }?.let { active ->
      _profile.value = _profile.value.copy(
        name = active.name,
        email = active.email,
        avatarColor = active.avatarColor
      )
    }
  }

  fun clearAppCache(): Long {
    return 48_500_000L // 48.5 MB cleared
  }
}
