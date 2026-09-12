package com.example.domain

import com.example.data.presets.MediaPlaceholder
import com.example.data.presets.PlaceholderType
import com.example.data.presets.TextPlaceholder
import com.example.data.presets.VideoTemplate
import com.example.domain.model.AspectRatio
import com.example.domain.model.FrameRate
import com.example.domain.model.Resolution
import com.example.domain.model.Timeline
import com.example.domain.model.VideoClip
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
    listOf(
      VideoTemplate(
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
        mediaPlaceholders = listOf(
          MediaPlaceholder("v1", "Travel Scene 1", PlaceholderType.VIDEO, 3000L, "cust_v1"),
          MediaPlaceholder("v2", "Travel Scene 2", PlaceholderType.VIDEO, 3000L, "cust_v2")
        ),
        textPlaceholders = listOf(
          TextPlaceholder("t1", "Vlog Title", "cust_t1", "DAILY ESCAPE")
        ),
        createTimeline = { _, _ -> Timeline() }
      )
    )
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

  fun createAndSaveCustomTemplate(
    title: String,
    category: String,
    description: String,
    durationSec: Int,
    aspectRatio: AspectRatio
  ): VideoTemplate {
    val newId = "cust_tpl_${UUID.randomUUID().toString().take(8)}"
    val newTemplate = VideoTemplate(
      id = newId,
      title = title,
      category = if (category.isBlank()) "User-Created" else category,
      description = description.ifBlank { "Custom user template created in AH Video Studio" },
      aspectRatio = aspectRatio,
      resolution = Resolution.RES_1080P,
      fps = FrameRate.FPS_30,
      durationMs = durationSec * 1000L,
      thumbnailGradientStart = 0xFF8B5CF6,
      thumbnailGradientEnd = 0xFFEC4899,
      iconEmoji = "🎨",
      audioTitle = "Custom Audio Track",
      mediaPlaceholders = listOf(
        MediaPlaceholder("slot1", "Main Video Clip", PlaceholderType.VIDEO, (durationSec * 1000L) / 2, "slot1_clip"),
        MediaPlaceholder("slot2", "Secondary Clip", PlaceholderType.VIDEO, (durationSec * 1000L) / 2, "slot2_clip")
      ),
      textPlaceholders = listOf(
        TextPlaceholder("txt1", "Main Header", "txt1_clip", title.uppercase())
      ),
      createTimeline = { _, _ -> Timeline() }
    )

    _customTemplates.value = listOf(newTemplate) + _customTemplates.value
    // Auto save template to saved IDs
    val currentSaved = _savedTemplateIds.value.toMutableSet()
    currentSaved.add(newId)
    _savedTemplateIds.value = currentSaved

    return newTemplate
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
