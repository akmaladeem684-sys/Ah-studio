package com.example.data.local

import com.example.domain.model.Timeline
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object TimelineSerializer {
  private val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  private val adapter = moshi.adapter(Timeline::class.java)

  fun toJson(timeline: Timeline): String {
    return try {
      adapter.toJson(timeline)
    } catch (e: Exception) {
      "{}"
    }
  }

  fun fromJson(json: String): Timeline {
    return try {
      adapter.fromJson(json) ?: Timeline()
    } catch (e: Exception) {
      Timeline()
    }
  }
}
