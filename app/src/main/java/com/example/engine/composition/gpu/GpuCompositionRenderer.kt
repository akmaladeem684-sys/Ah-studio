package com.example.engine.composition.gpu

import android.content.Context
import android.graphics.*
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.example.domain.model.*
import com.example.engine.KeyframeInterpolator
import com.example.engine.composition.ComposedFrame
import com.example.engine.composition.ComposedOverlay
import com.example.engine.composition.ComposedSticker
import com.example.engine.composition.ComposedText
import com.example.engine.text.TextLayerRenderer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max

/**
 * GPU-accelerated composition pipeline.
 * Performs real-time hardware texture rendering, GPU scaling, rotation, cropping,
 * opacity, blending, filters, color adjustments, transitions, text layers,
 * image overlays, stickers, keyframes, and chroma key via OpenGL ES.
 */
class GpuCompositionRenderer(private val context: Context) {
  companion object {
    private const val TAG = "GpuCompositionRenderer"
    private const val FLOAT_SIZE_BYTES = 4
    private const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 4 * FLOAT_SIZE_BYTES
    private const val POSITION_DATA_OFFSET = 0
    private const val TEXTURE_DATA_OFFSET = 2
  }

  // Full-screen quad geometry: (x, y, u, v)
  private val quadVertices = floatArrayOf(
    -1.0f, -1.0f,  0.0f, 0.0f,
     1.0f, -1.0f,  1.0f, 0.0f,
    -1.0f,  1.0f,  0.0f, 1.0f,
     1.0f,  1.0f,  1.0f, 1.0f
  )

  private val vertexBuffer: FloatBuffer = ByteBuffer
    .allocateDirect(quadVertices.size * FLOAT_SIZE_BYTES)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
      put(quadVertices)
      position(0)
    }

  // OpenGL Programs
  private var program2D = 0
  private var programOes = 0
  private var programTransition = 0

  // Cached Text & Sticker textures: Key -> GlTextureInfo(textureId, width, height, hash)
  private data class CachedTexture(val texId: Int, val width: Int, val height: Int, val hash: Int)
  private val textTextureCache = mutableMapOf<String, CachedTexture>()
  private val stickerTextureCache = mutableMapOf<String, CachedTexture>()
  private val imageTextureCache = mutableMapOf<String, CachedTexture>()

  // Framebuffers for multi-pass / transition rendering
  private val fboA = GlFramebuffer()
  private val fboB = GlFramebuffer()

  // Matrix buffers
  private val mvpMatrix = FloatArray(16)
  private val texMatrix = FloatArray(16)
  private val identityMatrix = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }

  private var isInitialized = false

  fun initGl() {
    if (isInitialized) return

    program2D = GlShaderUtil.createProgram(GpuShaders.VERTEX_SHADER, GpuShaders.buildFragmentShader(isOes = false))
    programOes = GlShaderUtil.createProgram(GpuShaders.VERTEX_SHADER, GpuShaders.buildFragmentShader(isOes = true))
    programTransition = GlShaderUtil.createProgram(GpuShaders.VERTEX_SHADER, GpuShaders.TRANSITION_FRAGMENT_SHADER)

    isInitialized = true
  }

  /**
   * Main GPU rendering method:
   * Composites main clip, PIP overlays, text layers, stickers, transitions, color adjustments,
   * and chroma key directly into the currently bound OpenGL framebuffer / window surface.
   */
  fun render(
    frame: ComposedFrame,
    mainTextureId: Int,
    isMainOes: Boolean,
    mainTexMatrix: FloatArray? = null,
    overlayTextures: Map<String, Int> = emptyMap(),
    viewportWidth: Int,
    viewportHeight: Int,
    timelineAdjustments: VideoAdjustments = VideoAdjustments(),
    timelineFilter: FilterSettings = FilterSettings(),
    chromaKey: ChromaKeySettings = ChromaKeySettings()
  ) {
    if (!isInitialized) {
      initGl()
    }

    GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

    // Enable standard alpha blending
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

    // 1. Render Main Video Clip
    if (mainTextureId > 0) {
      renderMainClip(
        frame = frame,
        textureId = mainTextureId,
        isOes = isMainOes,
        customTexMatrix = mainTexMatrix,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        adjustments = timelineAdjustments,
        filter = timelineFilter
      )
    }

    // 2. Render PIP Overlays with Keyframes & Chroma Key
    for (overlay in frame.activeOverlays) {
      val overlayTexId = overlayTextures[overlay.clip.id]
      if (overlayTexId != null && overlayTexId > 0) {
        renderOverlayClip(
          overlay = overlay,
          textureId = overlayTexId,
          viewportWidth = viewportWidth,
          viewportHeight = viewportHeight,
          chromaKey = chromaKey
        )
      }
    }

    // 3. Render GPU Text Layers
    for (text in frame.activeTexts) {
      renderTextClip(text, viewportWidth, viewportHeight)
    }

    // 4. Render GPU Stickers
    for (sticker in frame.activeStickers) {
      renderStickerClip(sticker, viewportWidth, viewportHeight)
    }
  }

  private fun renderMainClip(
    frame: ComposedFrame,
    textureId: Int,
    isOes: Boolean,
    customTexMatrix: FloatArray?,
    viewportWidth: Int,
    viewportHeight: Int,
    adjustments: VideoAdjustments,
    filter: FilterSettings
  ) {
    val program = if (isOes) programOes else program2D
    GLES20.glUseProgram(program)

    // Compute MVP transform
    Matrix.setIdentityM(mvpMatrix, 0)
    val clip = frame.activeClip
    var keyframeBlur = 0f
    var keyframeEffectParam = 0f
    var finalAdjustments = adjustments
    var finalOpacity = 1.0f

    if (clip != null) {
      val kf = frame.activeClipTransform ?: KeyframeInterpolator.interpolate(clip, frame.timelinePosMs - clip.timelineStartMs)

      // Crop & aspect scale with keyframe scaleX & scaleY
      val scaleX = (if (clip.flipHorizontal) -clip.cropScale else clip.cropScale) * kf.scaleX
      val scaleY = (if (clip.flipVertical) -clip.cropScale else clip.cropScale) * kf.scaleY

      Matrix.translateM(mvpMatrix, 0, clip.cropOffsetX + kf.posX, clip.cropOffsetY + kf.posY, 0f)
      Matrix.rotateM(mvpMatrix, 0, (clip.rotationDegrees.toFloat() + kf.rotation) % 360f, 0f, 0f, 1f)
      Matrix.scaleM(mvpMatrix, 0, scaleX, scaleY, 1f)

      finalOpacity *= kf.opacity
      keyframeBlur = kf.blur
      keyframeEffectParam = kf.effectParam
      finalAdjustments = adjustments.copy(
        brightness = (adjustments.brightness + kf.brightness).coerceIn(-1f, 1f),
        contrast = (adjustments.contrast * kf.contrast).coerceAtLeast(0f),
        saturation = (adjustments.saturation * kf.saturation).coerceAtLeast(0f)
      )
    }

    // Texture Matrix (handling surface texture orientation or cropping)
    if (customTexMatrix != null) {
      System.arraycopy(customTexMatrix, 0, texMatrix, 0, 16)
    } else {
      Matrix.setIdentityM(texMatrix, 0)
    }

    // Transition effect if active on main clip
    if (frame.activeTransition != null) {
      val tr = frame.activeTransition
      when (tr.type) {
        TransitionType.FADE -> {
          finalOpacity = (finalOpacity * (1.0f - tr.progress)).coerceIn(0f, 1f)
        }
        TransitionType.SLIDE_LEFT -> {
          Matrix.translateM(mvpMatrix, 0, -tr.progress * 2.0f, 0f, 0f)
        }
        TransitionType.ZOOM_IN -> {
          val zoom = 1.0f + tr.progress * 0.5f
          Matrix.scaleM(mvpMatrix, 0, zoom, zoom, 1f)
        }
        else -> {}
      }
    }

    // Bind uniforms
    bindCommonUniforms(
      program = program,
      textureId = textureId,
      isOes = isOes,
      opacity = finalOpacity,
      adjustments = finalAdjustments,
      filter = filter,
      chromaKey = ChromaKeySettings(enabled = false),
      viewportWidth = viewportWidth,
      viewportHeight = viewportHeight,
      blur = keyframeBlur,
      effectParam = keyframeEffectParam
    )

    // Draw Quad
    drawQuad(program)
  }

  private fun renderOverlayClip(
    overlay: ComposedOverlay,
    textureId: Int,
    viewportWidth: Int,
    viewportHeight: Int,
    chromaKey: ChromaKeySettings
  ) {
    val program = program2D
    GLES20.glUseProgram(program)

    Matrix.setIdentityM(mvpMatrix, 0)
    // Map overlay position (-1..1), scaleX & scaleY, and rotation
    Matrix.translateM(mvpMatrix, 0, overlay.posX, overlay.posY, 0f)
    Matrix.rotateM(mvpMatrix, 0, overlay.rotation, 0f, 0f, 1f)
    Matrix.scaleM(mvpMatrix, 0, overlay.scaleX * 0.5f, overlay.scaleY * 0.5f, 1f)

    Matrix.setIdentityM(texMatrix, 0)

    // Apply Blend Mode
    when (overlay.blendMode.lowercase()) {
      "screen" -> GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_COLOR)
      "multiply" -> GLES20.glBlendFunc(GLES20.GL_DST_COLOR, GLES20.GL_ONE_MINUS_SRC_ALPHA)
      "add" -> GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
      else -> GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    val overlayAdj = VideoAdjustments(
      brightness = overlay.brightness,
      contrast = overlay.contrast,
      saturation = overlay.saturation
    )

    bindCommonUniforms(
      program = program,
      textureId = textureId,
      isOes = false,
      opacity = overlay.opacity,
      adjustments = overlayAdj,
      filter = FilterSettings(),
      chromaKey = chromaKey,
      viewportWidth = viewportWidth,
      viewportHeight = viewportHeight,
      blur = overlay.blur,
      effectParam = overlay.effectParam
    )

    drawQuad(program)

    // Restore standard blend func
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
  }

  private fun renderTextClip(text: ComposedText, viewportWidth: Int, viewportHeight: Int) {
    val bitmap = TextLayerRenderer.renderToBitmap(
      clip = text.clip,
      currentPosMs = text.currentPosMs,
      width = viewportWidth,
      height = viewportHeight,
      context = context
    )
    val texId = GlShaderUtil.uploadBitmapToTexture(bitmap, 0)
    bitmap.recycle()
    if (texId == 0) return

    val program = program2D
    GLES20.glUseProgram(program)

    Matrix.setIdentityM(mvpMatrix, 0)
    Matrix.setIdentityM(texMatrix, 0)

    bindCommonUniforms(
      program = program,
      textureId = texId,
      isOes = false,
      opacity = 1.0f,
      adjustments = VideoAdjustments(),
      filter = FilterSettings(),
      chromaKey = ChromaKeySettings(enabled = false),
      viewportWidth = viewportWidth,
      viewportHeight = viewportHeight
    )

    drawQuad(program)
    GLES20.glDeleteTextures(1, intArrayOf(texId), 0)
  }

  private fun renderStickerClip(sticker: ComposedSticker, viewportWidth: Int, viewportHeight: Int) {
    val cached = getOrCreateStickerTexture(sticker.clip, viewportWidth)
    if (cached == null || cached.texId == 0) return

    val program = program2D
    GLES20.glUseProgram(program)

    Matrix.setIdentityM(mvpMatrix, 0)
    Matrix.translateM(mvpMatrix, 0, sticker.posX, -sticker.posY, 0f)
    Matrix.rotateM(mvpMatrix, 0, -sticker.rotation, 0f, 0f, 1f)

    val aspect = viewportWidth.toFloat() / max(1, viewportHeight)
    val stickerAspect = cached.width.toFloat() / max(1, cached.height)
    val scaleY = (cached.height.toFloat() / viewportHeight) * 2f * sticker.scale
    val scaleX = scaleY * stickerAspect / aspect

    Matrix.scaleM(mvpMatrix, 0, scaleX, scaleY, 1f)
    Matrix.setIdentityM(texMatrix, 0)

    bindCommonUniforms(
      program = program,
      textureId = cached.texId,
      isOes = false,
      opacity = sticker.opacity,
      adjustments = VideoAdjustments(),
      filter = FilterSettings(),
      chromaKey = ChromaKeySettings(enabled = false),
      viewportWidth = viewportWidth,
      viewportHeight = viewportHeight
    )

    drawQuad(program)
  }

  private fun bindCommonUniforms(
    program: Int,
    textureId: Int,
    isOes: Boolean,
    opacity: Float,
    adjustments: VideoAdjustments,
    filter: FilterSettings,
    chromaKey: ChromaKeySettings,
    viewportWidth: Int,
    viewportHeight: Int,
    blur: Float = 0f,
    effectParam: Float = 0f
  ) {
    val uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
    val uTexMatrixHandle = GLES20.glGetUniformLocation(program, "uTexMatrix")
    val uTextureHandle = GLES20.glGetUniformLocation(program, "uTexture")
    val uOpacityHandle = GLES20.glGetUniformLocation(program, "uOpacity")
    val uBlurHandle = GLES20.glGetUniformLocation(program, "uBlur")
    val uEffectParamHandle = GLES20.glGetUniformLocation(program, "uEffectParam")

    GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(uTexMatrixHandle, 1, false, texMatrix, 0)
    GLES20.glUniform1f(uOpacityHandle, opacity)
    if (uBlurHandle >= 0) GLES20.glUniform1f(uBlurHandle, blur)
    if (uEffectParamHandle >= 0) GLES20.glUniform1f(uEffectParamHandle, effectParam)

    // Bind texture
    val target = if (isOes) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(target, textureId)
    GLES20.glUniform1i(uTextureHandle, 0)

    // Color adjustments
    val uBrightnessHandle = GLES20.glGetUniformLocation(program, "uBrightness")
    val uContrastHandle = GLES20.glGetUniformLocation(program, "uContrast")
    val uSaturationHandle = GLES20.glGetUniformLocation(program, "uSaturation")
    val uExposureHandle = GLES20.glGetUniformLocation(program, "uExposure")
    val uTemperatureHandle = GLES20.glGetUniformLocation(program, "uTemperature")
    val uTintHandle = GLES20.glGetUniformLocation(program, "uTint")
    val uHighlightsHandle = GLES20.glGetUniformLocation(program, "uHighlights")
    val uShadowsHandle = GLES20.glGetUniformLocation(program, "uShadows")
    val uVignetteHandle = GLES20.glGetUniformLocation(program, "uVignette")
    val uGrainHandle = GLES20.glGetUniformLocation(program, "uGrain")
    val uSharpnessHandle = GLES20.glGetUniformLocation(program, "uSharpness")
    val uTexelSizeHandle = GLES20.glGetUniformLocation(program, "uTexelSize")

    if (uBrightnessHandle >= 0) GLES20.glUniform1f(uBrightnessHandle, adjustments.brightness)
    if (uContrastHandle >= 0) GLES20.glUniform1f(uContrastHandle, adjustments.contrast)
    if (uSaturationHandle >= 0) GLES20.glUniform1f(uSaturationHandle, adjustments.saturation)
    if (uExposureHandle >= 0) GLES20.glUniform1f(uExposureHandle, adjustments.exposure)
    if (uTemperatureHandle >= 0) GLES20.glUniform1f(uTemperatureHandle, adjustments.temperature)
    if (uTintHandle >= 0) GLES20.glUniform1f(uTintHandle, adjustments.tint)
    if (uHighlightsHandle >= 0) GLES20.glUniform1f(uHighlightsHandle, adjustments.highlights)
    if (uShadowsHandle >= 0) GLES20.glUniform1f(uShadowsHandle, adjustments.shadows)
    if (uVignetteHandle >= 0) GLES20.glUniform1f(uVignetteHandle, adjustments.vignette)
    if (uGrainHandle >= 0) GLES20.glUniform1f(uGrainHandle, adjustments.grain)
    if (uSharpnessHandle >= 0) GLES20.glUniform1f(uSharpnessHandle, adjustments.sharpness)
    if (uTexelSizeHandle >= 0) GLES20.glUniform2f(uTexelSizeHandle, 1.0f / max(1, viewportWidth), 1.0f / max(1, viewportHeight))

    // Chroma key uniforms
    val uChromaEnabledHandle = GLES20.glGetUniformLocation(program, "uChromaEnabled")
    if (uChromaEnabledHandle >= 0) {
      if (chromaKey.enabled) {
        val color = chromaKey.targetColor.toInt()
        val r = android.graphics.Color.red(color) / 255f
        val g = android.graphics.Color.green(color) / 255f
        val b = android.graphics.Color.blue(color) / 255f

        GLES20.glUniform1i(uChromaEnabledHandle, 1)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uKeyColor"), r, g, b)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uChromaThreshold"), chromaKey.intensity * 0.8f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uChromaSmoothness"), max(0.01f, chromaKey.edgeAdjustment * 0.4f))
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uChromaSpill"), chromaKey.spillReduction)
      } else {
        GLES20.glUniform1i(uChromaEnabledHandle, 0)
      }
    }
  }

  private fun drawQuad(program: Int) {
    val aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
    val aTextureCoordHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")

    vertexBuffer.position(POSITION_DATA_OFFSET)
    GLES20.glVertexAttribPointer(
      aPositionHandle, 2, GLES20.GL_FLOAT, false,
      TRIANGLE_VERTICES_DATA_STRIDE_BYTES, vertexBuffer
    )
    GLES20.glEnableVertexAttribArray(aPositionHandle)

    vertexBuffer.position(TEXTURE_DATA_OFFSET)
    GLES20.glVertexAttribPointer(
      aTextureCoordHandle, 2, GLES20.GL_FLOAT, false,
      TRIANGLE_VERTICES_DATA_STRIDE_BYTES, vertexBuffer
    )
    GLES20.glEnableVertexAttribArray(aTextureCoordHandle)

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

    GLES20.glDisableVertexAttribArray(aPositionHandle)
    GLES20.glDisableVertexAttribArray(aTextureCoordHandle)
  }

  private fun getOrCreateTextTexture(clip: TextClip, viewportWidth: Int): CachedTexture? {
    val hash = clip.hashCode() xor viewportWidth
    val cached = textTextureCache[clip.id]
    if (cached != null && cached.hash == hash) {
      return cached
    }

    // Rasterize text to high-res Bitmap once, then upload to GPU texture
    val scaleFactor = viewportWidth.toFloat() / 600f
    val fontSize = (clip.fontSizeSp * scaleFactor).coerceAtLeast(14f)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.textSize = fontSize
      this.isFakeBoldText = clip.fontWeight >= 700
      this.color = clip.textColor.toInt()
      this.textAlign = Paint.Align.LEFT
    }

    val bounds = Rect()
    paint.getTextBounds(clip.text, 0, clip.text.length, bounds)

    val padX = 32
    val padY = 24
    val bmpWidth = max(bounds.width() + padX * 2, 64)
    val bmpHeight = max(bounds.height() + padY * 2, 48)

    val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background rect
    if (clip.hasBackground) {
      val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = clip.backgroundColor.toInt()
      }
      canvas.drawRoundRect(RectF(0f, 0f, bmpWidth.toFloat(), bmpHeight.toFloat()), 16f, 16f, bgPaint)
    }

    // Text stroke
    val textX = padX.toFloat() - bounds.left
    val textY = padY.toFloat() - bounds.top
    if (clip.strokeWidth > 0f) {
      val strokePaint = Paint(paint).apply {
        style = Paint.Style.STROKE
        strokeWidth = clip.strokeWidth * scaleFactor
        color = clip.strokeColor.toInt()
      }
      canvas.drawText(clip.text, textX, textY, strokePaint)
    }

    // Gradient
    if (clip.hasGradient) {
      paint.shader = LinearGradient(
        0f, 0f, bmpWidth.toFloat(), 0f,
        clip.gradientColorStart.toInt(), clip.gradientColorEnd.toInt(),
        Shader.TileMode.CLAMP
      )
    }

    // Text fill
    canvas.drawText(clip.text, textX, textY, paint)

    // Upload to OpenGL texture
    val oldTexId = cached?.texId ?: 0
    val texId = GlShaderUtil.uploadBitmapToTexture(bitmap, oldTexId)
    bitmap.recycle()

    val entry = CachedTexture(texId, bmpWidth, bmpHeight, hash)
    textTextureCache[clip.id] = entry
    return entry
  }

  private fun getOrCreateStickerTexture(clip: StickerClip, viewportWidth: Int): CachedTexture? {
    val hash = clip.hashCode() xor viewportWidth
    val cached = stickerTextureCache[clip.id]
    if (cached != null && cached.hash == hash) {
      return cached
    }

    val size = (80f * (viewportWidth.toFloat() / 600f)).toInt().coerceIn(64, 256)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = size * 0.75f
      textAlign = Paint.Align.CENTER
    }
    val yPos = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(clip.emojiOrAsset, size / 2f, yPos, paint)

    val oldTexId = cached?.texId ?: 0
    val texId = GlShaderUtil.uploadBitmapToTexture(bitmap, oldTexId)
    bitmap.recycle()

    val entry = CachedTexture(texId, size, size, hash)
    stickerTextureCache[clip.id] = entry
    return entry
  }

  fun uploadImageTexture(id: String, bitmap: Bitmap): Int {
    val existing = imageTextureCache[id]
    val texId = GlShaderUtil.uploadBitmapToTexture(bitmap, existing?.texId ?: 0)
    imageTextureCache[id] = CachedTexture(texId, bitmap.width, bitmap.height, bitmap.generationId)
    return texId
  }

  fun release() {
    // Release framebuffers
    fboA.release()
    fboB.release()

    // Delete textures
    val texturesToDelete = mutableListOf<Int>()
    for (t in textTextureCache.values) texturesToDelete.add(t.texId)
    for (s in stickerTextureCache.values) texturesToDelete.add(s.texId)
    for (i in imageTextureCache.values) texturesToDelete.add(i.texId)

    if (texturesToDelete.isNotEmpty()) {
      GLES20.glDeleteTextures(texturesToDelete.size, texturesToDelete.toIntArray(), 0)
    }
    textTextureCache.clear()
    stickerTextureCache.clear()
    imageTextureCache.clear()

    // Delete programs
    if (program2D != 0) {
      GLES20.glDeleteProgram(program2D)
      program2D = 0
    }
    if (programOes != 0) {
      GLES20.glDeleteProgram(programOes)
      programOes = 0
    }
    if (programTransition != 0) {
      GLES20.glDeleteProgram(programTransition)
      programTransition = 0
    }

    isInitialized = false
    Log.d(TAG, "GpuCompositionRenderer resources cleanly released")
  }
}
