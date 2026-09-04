package com.example.ai.providers

import com.example.BuildConfig
import com.example.domain.model.TextClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiAIProvider : SpeechToTextProvider, TranslationProvider, HighlightAnalysisProvider {

  override val providerName: String = "Google Gemini AI"

  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

  private fun getApiKey(): String {
    return try {
      val key = BuildConfig.GEMINI_API_KEY
      if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
    } catch (e: Exception) {
      ""
    }
  }

  override val isAvailable: Boolean
    get() = getApiKey().isNotBlank()

  override suspend fun transcribeAudio(audioUriOrPath: String, language: String): Result<List<TextClip>> = withContext(Dispatchers.IO) {
    val apiKey = getApiKey()
    if (apiKey.isBlank()) {
      return@withContext Result.failure(
        IllegalStateException("Speech-to-Text unavailable: No AI provider is configured. Set GEMINI_API_KEY in the Secrets panel.")
      )
    }

    try {
      val prompt = "Generate accurate subtitle caption segments in $language for this media. Return strictly a JSON array of objects with keys: text (string), startMs (integer), durationMs (integer)."
      val jsonPayload = JSONObject().apply {
        put("contents", JSONArray().apply {
          put(JSONObject().apply {
            put("parts", JSONArray().apply {
              put(JSONObject().apply { put("text", prompt) })
            })
          })
        })
      }

      val request = Request.Builder()
        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
        .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("AI server error: HTTP ${response.code}"))
      }

      val body = response.body?.string() ?: ""
      val json = JSONObject(body)
      val textResponse = json.getJSONArray("candidates")
        .getJSONObject(0).getJSONObject("content")
        .getJSONArray("parts").getJSONObject(0).getString("text")

      val cleaned = textResponse.replace("```json", "").replace("```", "").trim()
      val array = JSONArray(cleaned)
      val clips = mutableListOf<TextClip>()
      for (i in 0 until array.length()) {
        val item = array.getJSONObject(i)
        clips.add(
          TextClip(
            id = UUID.randomUUID().toString(),
            text = item.getString("text"),
            timelineStartMs = item.getLong("startMs"),
            durationMs = item.getLong("durationMs"),
            fontSizeSp = 22f,
            fontWeight = 800,
            textColor = 0xFFFFFFFF,
            strokeWidth = 2f,
            strokeColor = 0xFF000000,
            hasBackground = true,
            backgroundColor = 0x88000000,
            posY = 0.35f,
            animationType = "Pop"
          )
        )
      }
      Result.success(clips)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun translateText(text: String, targetLanguage: String): Result<String> = withContext(Dispatchers.IO) {
    val apiKey = getApiKey()
    if (apiKey.isBlank()) {
      return@withContext Result.failure(
        IllegalStateException("Translation unavailable: No AI provider configured.")
      )
    }

    try {
      val prompt = "Translate the following video subtitle directly into $targetLanguage. Return only the translated text with no explanations:\n\n$text"
      val jsonPayload = JSONObject().apply {
        put("contents", JSONArray().apply {
          put(JSONObject().apply {
            put("parts", JSONArray().apply {
              put(JSONObject().apply { put("text", prompt) })
            })
          })
        })
      }

      val request = Request.Builder()
        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
        .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("AI server error: HTTP ${response.code}"))
      }

      val body = response.body?.string() ?: ""
      val json = JSONObject(body)
      val translation = json.getJSONArray("candidates")
        .getJSONObject(0).getJSONObject("content")
        .getJSONArray("parts").getJSONObject(0).getString("text").trim()

      Result.success(translation)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun analyzeHighlights(videoTitle: String, durationMs: Long): Result<List<VideoHighlightSegment>> = withContext(Dispatchers.IO) {
    val apiKey = getApiKey()
    if (apiKey.isBlank()) {
      return@withContext Result.failure(
        IllegalStateException("Highlight analysis unavailable: No AI provider configured.")
      )
    }

    try {
      val durationSec = durationMs / 1000
      val prompt = "Identify 3 to 4 best cinematic highlight moments for a video titled '$videoTitle' of duration $durationSec seconds. Return strictly a JSON array of objects with keys: title (string), startTimeMs (integer), endTimeMs (integer), score (float 0.0-1.0), description (string)."
      val jsonPayload = JSONObject().apply {
        put("contents", JSONArray().apply {
          put(JSONObject().apply {
            put("parts", JSONArray().apply {
              put(JSONObject().apply { put("text", prompt) })
            })
          })
        })
      }

      val request = Request.Builder()
        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
        .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("AI server error: HTTP ${response.code}"))
      }

      val body = response.body?.string() ?: ""
      val json = JSONObject(body)
      val textResponse = json.getJSONArray("candidates")
        .getJSONObject(0).getJSONObject("content")
        .getJSONArray("parts").getJSONObject(0).getString("text")

      val cleaned = textResponse.replace("```json", "").replace("```", "").trim()
      val array = JSONArray(cleaned)
      val segments = mutableListOf<VideoHighlightSegment>()
      for (i in 0 until array.length()) {
        val item = array.getJSONObject(i)
        segments.add(
          VideoHighlightSegment(
            title = item.getString("title"),
            startTimeMs = item.getLong("startTimeMs"),
            endTimeMs = item.getLong("endTimeMs"),
            score = item.getDouble("score").toFloat(),
            description = item.getString("description")
          )
        )
      }
      Result.success(segments)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
