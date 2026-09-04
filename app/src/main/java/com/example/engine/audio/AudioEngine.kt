package com.example.engine.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

class AudioEngine(private val context: Context) {

  private var tts: TextToSpeech? = null
  private val _ttsReady = MutableStateFlow(false)
  val ttsReady: StateFlow<Boolean> = _ttsReady.asStateFlow()

  private val _isRecording = MutableStateFlow(false)
  val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

  private val _recordingDurationMs = MutableStateFlow(0L)
  val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

  private val _currentDecibels = MutableStateFlow(0f)
  val currentDecibels: StateFlow<Float> = _currentDecibels.asStateFlow()

  private var toneGenerator: ToneGenerator? = null

  init {
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
      tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
          tts?.language = Locale.US
          _ttsReady.value = true
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun playPreviewSfx(sfxId: String) {
    try {
      val tone = when {
        sfxId.contains("pop") -> ToneGenerator.TONE_PROP_BEEP
        sfxId.contains("whoosh") -> ToneGenerator.TONE_SUP_RINGTONE
        sfxId.contains("camera") -> ToneGenerator.TONE_PROP_ACK
        sfxId.contains("bass") -> ToneGenerator.TONE_CDMA_ABBR_ALERT
        sfxId.contains("ding") -> ToneGenerator.TONE_PROP_PROMPT
        else -> ToneGenerator.TONE_PROP_BEEP2
      }
      toneGenerator?.startTone(tone, 200)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun synthesizeTts(text: String, voicePitch: Float = 1.0f, speechRate: Float = 1.0f) {
    if (_ttsReady.value && text.isNotBlank()) {
      try {
        tts?.setPitch(voicePitch)
        tts?.setSpeechRate(speechRate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AH_TTS_${System.currentTimeMillis()}")
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun startVoiceRecording(onDurationTick: (Long) -> Unit) {
    _isRecording.value = true
    _recordingDurationMs.value = 0L
  }

  fun updateRecordingState(durationMs: Long, amplitude: Float) {
    _recordingDurationMs.value = durationMs
    _currentDecibels.value = amplitude
  }

  fun stopVoiceRecording(): File {
    _isRecording.value = false
    val recordedFile = File(context.cacheDir, "voiceover_${System.currentTimeMillis()}.wav")
    if (!recordedFile.exists()) {
      recordedFile.createNewFile()
    }
    return recordedFile
  }

  fun release() {
    try {
      tts?.stop()
      tts?.shutdown()
      toneGenerator?.release()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
