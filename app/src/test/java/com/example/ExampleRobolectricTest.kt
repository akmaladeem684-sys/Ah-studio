package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.SelectedTrackElement
import com.example.engine.TimelineEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AH Video Studio", appName)
  }

  @Test
  fun `timeline engine supports adding video and overlay clips`() {
    val engine = TimelineEngine()

    // Add main video clip
    engine.addVideoClip(
      uri = "content://media/video_1",
      name = "Main Video",
      isVideo = true,
      durationMs = 5000L
    )

    assertEquals(1, engine.timeline.value.videoClips.size)
    assertEquals(5000L, engine.timeline.value.totalDurationMs)

    // Add overlay clip
    engine.addOverlayClip(
      uri = "content://media/overlay_img",
      name = "PIP Logo",
      isVideo = false,
      durationMs = 3000L,
      scale = 0.5f,
      posX = 0.3f,
      posY = -0.3f,
      opacity = 0.85f
    )

    assertEquals(1, engine.timeline.value.overlayClips.size)
    val overlay = engine.timeline.value.overlayClips.first()
    assertEquals("PIP Logo", overlay.name)
    assertEquals(0.5f, overlay.cropScale, 0.01f)
    assertEquals(0.85f, overlay.opacity, 0.01f)

    // Update overlay properties
    engine.setOverlayScale(overlay.id, 0.75f)
    engine.setOverlayOpacity(overlay.id, 0.9f)
    engine.setOverlayBlendMode(overlay.id, "Screen")

    val updatedOverlay = engine.timeline.value.overlayClips.first()
    assertEquals(0.75f, updatedOverlay.cropScale, 0.01f)
    assertEquals(0.9f, updatedOverlay.opacity, 0.01f)
    assertEquals("Screen", updatedOverlay.blendMode)

    // Delete overlay
    engine.selectElement(SelectedTrackElement.Overlay(overlay.id))
    engine.deleteSelected()
    assertEquals(0, engine.timeline.value.overlayClips.size)
  }

  @Test
  fun `gpu composition shader sources contain required uniforms and algorithms`() {
    val vertexShader = com.example.engine.composition.gpu.GpuShaders.VERTEX_SHADER
    assertTrue(vertexShader.contains("uMVPMatrix"))
    assertTrue(vertexShader.contains("uTexMatrix"))
    assertTrue(vertexShader.contains("aPosition"))
    assertTrue(vertexShader.contains("aTextureCoord"))

    val fragmentShader = com.example.engine.composition.gpu.GpuShaders.buildFragmentShader(isOes = true)
    assertTrue(fragmentShader.contains("GL_OES_EGL_image_external"))
    assertTrue(fragmentShader.contains("uChromaEnabled"))
    assertTrue(fragmentShader.contains("uChromaKeyColor"))
    assertTrue(fragmentShader.contains("uChromaSimilarity"))
    assertTrue(fragmentShader.contains("uChromaSmoothness"))
    assertTrue(fragmentShader.contains("uBrightness"))
    assertTrue(fragmentShader.contains("uContrast"))
    assertTrue(fragmentShader.contains("uSaturation"))
    assertTrue(fragmentShader.contains("uExposure"))
    assertTrue(fragmentShader.contains("uTemperature"))
    assertTrue(fragmentShader.contains("uVignette"))
    assertTrue(fragmentShader.contains("uTransitionType"))
    assertTrue(fragmentShader.contains("uTransitionProgress"))
    assertTrue(fragmentShader.contains("uBlendMode"))
    assertTrue(fragmentShader.contains("uOpacity"))
  }

  @Test
  fun `video composition engine evaluates frame properties and transforms`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engine = com.example.engine.composition.VideoCompositionEngine(context)

    val timeline = com.example.domain.model.Timeline(
      videoClips = listOf(
        com.example.domain.model.VideoClip(
          id = "clip_1",
          name = "Clip 1",
          uri = "content://media/clip1",
          isVideo = true,
          timelineStartMs = 0L,
          durationMs = 4000L
        )
      ),
      overlayClips = listOf(
        com.example.domain.model.VideoClip(
          id = "pip_1",
          name = "PIP 1",
          uri = "content://media/pip1",
          isVideo = false,
          timelineStartMs = 1000L,
          durationMs = 2000L,
          cropScale = 0.5f,
          cropOffsetX = 0.2f,
          cropOffsetY = -0.2f,
          opacity = 0.9f
        )
      ),
      textClips = listOf(
        com.example.domain.model.TextClip(
          id = "text_1",
          text = "Title Text",
          timelineStartMs = 500L,
          durationMs = 2000L,
          animationType = "Pop",
          animDurationMs = 500L
        )
      )
    )

    // Evaluate frame at 1500ms
    val frame = engine.evaluateFrame(timeline, 1500L)
    assertNotNull(frame.activeClip)
    assertEquals("clip_1", frame.activeClip?.id)
    assertEquals(1, frame.activeOverlays.size)
    assertEquals("pip_1", frame.activeOverlays.first().clip.id)
    assertEquals(0.5f, frame.activeOverlays.first().scale, 0.01f)
    assertEquals(0.9f, frame.activeOverlays.first().opacity, 0.01f)
    assertEquals(1, frame.activeTexts.size)
    assertEquals("Title Text", frame.activeTexts.first().clip.text)

    // Verify GPU renderer lifecycle release
    engine.releaseGpu()
  }
}

