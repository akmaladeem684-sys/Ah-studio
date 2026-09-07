package com.example.data.local

import com.example.domain.model.ProjectPackage
import com.example.domain.model.ProjectSettings
import com.example.domain.model.Timeline
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONObject

object TimelineSerializer {
  private val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  private val timelineAdapter = moshi.adapter(Timeline::class.java)
  private val projectPackageAdapter = moshi.adapter(ProjectPackage::class.java)
  private val projectSettingsAdapter = moshi.adapter(ProjectSettings::class.java)

  fun toJson(timeline: Timeline): String {
    return try {
      timelineAdapter.toJson(timeline)
    } catch (e: Exception) {
      "{}"
    }
  }

  fun fromJson(json: String): Timeline {
    if (json.isBlank() || json == "{}") return Timeline()
    return try {
      // Check if this is a v2 ProjectPackage wrapper
      if (json.contains("\"timeline\"") && (json.contains("\"settings\"") || json.contains("\"mediaReferences\""))) {
        val jsonObject = JSONObject(json)
        if (jsonObject.has("timeline")) {
          val timelineSubJson = jsonObject.getJSONObject("timeline").toString()
          timelineAdapter.fromJson(timelineSubJson) ?: Timeline()
        } else {
          timelineAdapter.fromJson(json) ?: Timeline()
        }
      } else {
        timelineAdapter.fromJson(json) ?: Timeline()
      }
    } catch (e: Exception) {
      try {
        timelineAdapter.fromJson(json) ?: Timeline()
      } catch (fallbackEx: Exception) {
        Timeline()
      }
    }
  }

  fun toPackageJson(projectPackage: ProjectPackage): String {
    return try {
      projectPackageAdapter.toJson(projectPackage)
    } catch (e: Exception) {
      // Fallback: minimal JSON serialization
      val timelineJson = toJson(projectPackage.timeline)
      """{"version":2,"projectId":"${projectPackage.projectId}","projectName":"${projectPackage.projectName}","timeline":$timelineJson}"""
    }
  }

  fun fromPackageJson(json: String): ProjectPackage? {
    if (json.isBlank()) return null
    return try {
      projectPackageAdapter.fromJson(json)
    } catch (e: Exception) {
      // Fallback construct package from timeline
      val timeline = fromJson(json)
      ProjectPackage(
        projectId = "",
        projectName = "Restored Project",
        timeline = timeline
      )
    }
  }

  fun settingsToJson(settings: ProjectSettings): String {
    return try {
      projectSettingsAdapter.toJson(settings)
    } catch (e: Exception) {
      "{}"
    }
  }

  fun settingsFromJson(json: String): ProjectSettings {
    if (json.isBlank() || json == "{}") return ProjectSettings()
    return try {
      projectSettingsAdapter.fromJson(json) ?: ProjectSettings()
    } catch (e: Exception) {
      ProjectSettings()
    }
  }
}

