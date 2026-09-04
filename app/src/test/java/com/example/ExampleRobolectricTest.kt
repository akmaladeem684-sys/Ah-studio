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
}

