package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ExportedVideoEntity
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.data.local.TimelineSerializer
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProjectRepository(private val database: AppDatabase) {
  private val projectDao: ProjectDao = database.projectDao()
  private val exportedVideoDao = database.exportedVideoDao()

  val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
  val drafts: Flow<List<ProjectEntity>> = projectDao.getDrafts()
  val exportedVideos: Flow<List<ExportedVideoEntity>> = exportedVideoDao.getAllExportedVideos()

  suspend fun getProjectById(id: String): ProjectEntity? {
    return projectDao.getProjectById(id)
  }

  suspend fun saveProject(
    id: String,
    name: String,
    durationMs: Long,
    thumbnailPath: String,
    aspectRatio: String,
    resolution: String,
    fps: Int,
    timeline: Timeline,
    isDraft: Boolean = true
  ): ProjectEntity {
    val entity = ProjectEntity(
      id = id,
      name = name,
      durationMs = durationMs,
      lastEditedTime = System.currentTimeMillis(),
      thumbnailPath = thumbnailPath,
      aspectRatio = aspectRatio,
      resolution = resolution,
      fps = fps,
      timelineJson = TimelineSerializer.toJson(timeline),
      isDraft = isDraft
    )
    projectDao.insertProject(entity)
    return entity
  }

  suspend fun duplicateProject(id: String): ProjectEntity? {
    val original = projectDao.getProjectById(id) ?: return null
    val duplicated = original.copy(
      id = UUID.randomUUID().toString(),
      name = "${original.name} (Copy)",
      lastEditedTime = System.currentTimeMillis()
    )
    projectDao.insertProject(duplicated)
    return duplicated
  }

  suspend fun renameProject(id: String, newName: String) {
    projectDao.renameProject(id, newName)
  }

  suspend fun deleteProject(id: String) {
    projectDao.deleteProjectById(id)
  }

  suspend fun recordExport(
    projectId: String,
    title: String,
    filePath: String,
    durationMs: Long,
    resolution: String,
    fps: Int,
    fileSizeBytes: Long
  ) {
    val entity = ExportedVideoEntity(
      id = UUID.randomUUID().toString(),
      projectId = projectId,
      title = title,
      filePath = filePath,
      durationMs = durationMs,
      resolution = resolution,
      fps = fps,
      timestamp = System.currentTimeMillis(),
      fileSizeBytes = fileSizeBytes
    )
    exportedVideoDao.insertExportedVideo(entity)
  }

  suspend fun deleteExportedVideo(id: String) {
    exportedVideoDao.deleteExportedVideo(id)
  }

  suspend fun createSampleProjectIfEmpty() {
    // Seed initial demo project with sample clips and tracks if database has no projects
    val defaultTimeline = Timeline(
      videoClips = listOf(
        VideoClip(
          id = "sample_clip_1",
          uri = "asset://nature_stream.mp4",
          name = "Cinematic Mountain Stream",
          isVideo = true,
          timelineStartMs = 0L,
          durationMs = 4500L,
          sourceStartMs = 0L,
          sourceEndMs = 4500L,
          speed = 1.0f,
          volume = 1.0f
        ),
        VideoClip(
          id = "sample_clip_2",
          uri = "asset://urban_sunset.mp4",
          name = "Golden Hour Skyline",
          isVideo = true,
          timelineStartMs = 4500L,
          durationMs = 5500L,
          sourceStartMs = 0L,
          sourceEndMs = 5500L,
          speed = 1.25f,
          volume = 0.8f
        )
      ),
      audioClips = listOf(
        AudioClip(
          id = "sample_audio_1",
          uri = "internal://lofi_chill_beat",
          title = "Chill Lofi Dreams (Original Mix)",
          timelineStartMs = 0L,
          durationMs = 10000L,
          volume = 0.85f,
          fadeInMs = 800L,
          fadeOutMs = 1200L,
          waveformData = listOf(0.2f, 0.4f, 0.6f, 0.9f, 0.7f, 0.5f, 0.8f, 1.0f, 0.6f, 0.4f, 0.7f, 0.8f, 0.5f, 0.3f, 0.6f, 0.7f, 0.4f, 0.2f)
        )
      ),
      textClips = listOf(
        TextClip(
          id = "sample_text_1",
          text = "CINEMATIC JOURNEY",
          timelineStartMs = 500L,
          durationMs = 3500L,
          fontFamily = "Sans-Serif",
          fontSizeSp = 28f,
          fontWeight = 900,
          textColor = 0xFFFFFFFF,
          hasGradient = true,
          gradientColorStart = 0xFF00E5FF,
          gradientColorEnd = 0xFF8B5CF6,
          strokeWidth = 2f,
          strokeColor = 0xAA000000,
          posY = -0.2f,
          animationType = "Pop"
        ),
        TextClip(
          id = "sample_text_2",
          text = "Shot on AH Video Studio Pro",
          timelineStartMs = 4600L,
          durationMs = 4000L,
          fontFamily = "Default",
          fontSizeSp = 18f,
          textColor = 0xFFE2E8F0,
          posY = 0.35f,
          animationType = "Fade"
        )
      ),
      stickerClips = listOf(
        StickerClip(
          id = "sample_sticker_1",
          emojiOrAsset = "✨",
          timelineStartMs = 1000L,
          durationMs = 3000L,
          posX = 0.35f,
          posY = -0.3f,
          scale = 1.2f
        )
      ),
      effectClips = listOf(
        EffectClip(
          id = "sample_effect_1",
          effectType = EffectType.GLOW,
          timelineStartMs = 4000L,
          durationMs = 2000L,
          intensity = 0.75f
        )
      ),
      transitions = listOf(
        Transition(
          id = "sample_trans_1",
          clipIndexBefore = 0,
          type = TransitionType.DISSOLVE,
          durationMs = 600L
        )
      ),
      adjustments = VideoAdjustments(
        brightness = 0.05f,
        contrast = 1.1f,
        saturation = 1.15f,
        vignette = 0.15f
      ),
      filter = FilterSettings(
        type = FilterType.CINEMATIC,
        intensity = 0.85f
      )
    )

    saveProject(
      id = "demo_project_cinema",
      name = "Cinematic Travel Reel",
      durationMs = 10000L,
      thumbnailPath = "",
      aspectRatio = "9:16",
      resolution = "1080p",
      fps = 30,
      timeline = defaultTimeline,
      isDraft = false
    )
  }
}
