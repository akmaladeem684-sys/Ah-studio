package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
  @PrimaryKey val id: String,
  val name: String,
  val durationMs: Long,
  val lastEditedTime: Long,
  val thumbnailPath: String,
  val aspectRatio: String = "9:16",
  val resolution: String = "1080p",
  val fps: Int = 30,
  val timelineJson: String,
  val isDraft: Boolean = true,
  // Migrated fields with default values for backward compatibility
  val sampleRate: Int = 48000,
  val canvasColor: Long = 0xFF000000,
  val hasMissingMedia: Boolean = false,
  val extraMetadataJson: String = "{}"
)

@Entity(tableName = "crash_recovery")
data class CrashRecoveryEntity(
  @PrimaryKey val id: String = "active_session",
  val projectId: String,
  val projectName: String,
  val timestamp: Long,
  val timelineJson: String,
  val settingsJson: String = "{}",
  val isDirty: Boolean = true
)

@Entity(tableName = "exported_videos")
data class ExportedVideoEntity(
  @PrimaryKey val id: String,
  val projectId: String,
  val title: String,
  val filePath: String,
  val durationMs: Long,
  val resolution: String,
  val fps: Int,
  val timestamp: Long,
  val fileSizeBytes: Long
)
