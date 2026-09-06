package com.example.engine.composition.gpu

/**
 * GLSL Shaders for GPU composition:
 * - Vertex transforms (scaling, rotation, translation, crop)
 * - Color adjustments (brightness, contrast, saturation, exposure, temp, tint, highlights, shadows, vignette, grain, sharpness)
 * - Chroma key in fragment shader
 * - GPU Filter presets via color matrix
 * - Multi-effect transitions
 */
object GpuShaders {

  const val VERTEX_SHADER = """
    uniform mat4 uMVPMatrix;
    uniform mat4 uTexMatrix;
    attribute vec4 aPosition;
    attribute vec4 aTextureCoord;
    varying vec2 vTextureCoord;
    
    void main() {
      gl_Position = uMVPMatrix * aPosition;
      vTextureCoord = (uTexMatrix * aTextureCoord).xy;
    }
  """

  fun buildFragmentShader(isOes: Boolean): String {
    val extensionHeader = if (isOes) {
      "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\n"
    } else {
      "precision mediump float;\n"
    }
    val samplerType = if (isOes) "samplerExternalOES" else "sampler2D"

    return """
      $extensionHeader
      varying vec2 vTextureCoord;
      uniform $samplerType uTexture;
      
      // Opacity
      uniform float uOpacity;
      
      // Keyframe Blur & Effect
      uniform float uBlur;
      uniform float uEffectParam;
      
      // Color Adjustments
      uniform float uBrightness;
      uniform float uContrast;
      uniform float uSaturation;
      uniform float uExposure;
      uniform float uTemperature;
      uniform float uTint;
      uniform float uHighlights;
      uniform float uShadows;
      uniform float uVignette;
      uniform float uGrain;
      uniform float uSharpness;
      uniform vec2 uTexelSize;
      
      // Filter Color Matrix (4x4) + Offset (vec4)
      uniform mat4 uColorMatrix;
      uniform vec4 uColorOffset;
      uniform int uUseColorMatrix;
      
      // Chroma Key
      uniform int uChromaEnabled;
      uniform vec3 uKeyColor;
      uniform vec3 uChromaKeyColor;
      uniform float uChromaThreshold;
      uniform float uChromaSimilarity;
      uniform float uChromaSmoothness;
      uniform float uChromaSpill;
      
      // Transitions
      uniform int uTransitionType;
      uniform float uTransitionProgress;
      
      // Blend Mode
      uniform int uBlendMode; // 0=Normal, 1=Screen, 2=Multiply, 3=Overlay, 4=Lighten, 5=Add
      
      // Pseudo random generator for grain
      float rand(vec2 co) {
        return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
      }
      
      void main() {
        // Texture boundaries check
        if (vTextureCoord.x < 0.0 || vTextureCoord.x > 1.0 || vTextureCoord.y < 0.0 || vTextureCoord.y > 1.0) {
          gl_FragColor = vec4(0.0);
          return;
        }
        
        vec4 color;
        if (uBlur > 0.005) {
          float bRad = uBlur * 0.02;
          vec4 bc = vec4(0.0);
          bc += texture2D(uTexture, vTextureCoord + vec2(-bRad, -bRad)) * 0.0625;
          bc += texture2D(uTexture, vTextureCoord + vec2(0.0, -bRad)) * 0.125;
          bc += texture2D(uTexture, vTextureCoord + vec2(bRad, -bRad)) * 0.0625;
          bc += texture2D(uTexture, vTextureCoord + vec2(-bRad, 0.0)) * 0.125;
          bc += texture2D(uTexture, vTextureCoord) * 0.25;
          bc += texture2D(uTexture, vTextureCoord + vec2(bRad, 0.0)) * 0.125;
          bc += texture2D(uTexture, vTextureCoord + vec2(-bRad, bRad)) * 0.0625;
          bc += texture2D(uTexture, vTextureCoord + vec2(0.0, bRad)) * 0.125;
          bc += texture2D(uTexture, vTextureCoord + vec2(bRad, bRad)) * 0.0625;
          color = bc;
        } else if (uSharpness > 0.01) {
          // 4-tap Laplacian sharpening
          vec4 c = texture2D(uTexture, vTextureCoord);
          vec4 up = texture2D(uTexture, vTextureCoord + vec2(0.0, uTexelSize.y));
          vec4 down = texture2D(uTexture, vTextureCoord - vec2(0.0, uTexelSize.y));
          vec4 left = texture2D(uTexture, vTextureCoord - vec2(uTexelSize.x, 0.0));
          vec4 right = texture2D(uTexture, vTextureCoord + vec2(uTexelSize.x, 0.0));
          vec4 laplacian = (up + down + left + right) - 4.0 * c;
          color = c - uSharpness * laplacian;
        } else {
          color = texture2D(uTexture, vTextureCoord);
        }
        
        if (uEffectParam > 0.005) {
          float splitDist = uEffectParam * 0.02;
          float r = texture2D(uTexture, vTextureCoord + vec2(splitDist, 0.0)).r;
          float g = color.g;
          float b = texture2D(uTexture, vTextureCoord - vec2(splitDist, 0.0)).b;
          color.rgb = vec3(r, g, b);
        }
        
        // 1. Chroma Key removal
        if (uChromaEnabled == 1) {
          float dist = distance(color.rgb, uKeyColor);
          if (dist < uChromaThreshold) {
            discard;
          } else if (dist < uChromaThreshold + uChromaSmoothness) {
            float edgeAlpha = (dist - uChromaThreshold) / max(0.001, uChromaSmoothness);
            color.a *= edgeAlpha;
            if (uChromaSpill > 0.0) {
              float maxOther = max(color.r, color.b);
              if (color.g > maxOther) {
                color.g = mix(color.g, maxOther, uChromaSpill);
              }
            }
          }
        }
        
        // 2. Exposure & Brightness
        color.rgb = color.rgb * pow(2.0, uExposure) + vec3(uBrightness);
        
        // 3. Contrast
        color.rgb = (color.rgb - 0.5) * uContrast + 0.5;
        
        // 4. Saturation
        float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
        color.rgb = mix(vec3(luminance), color.rgb, uSaturation);
        
        // 5. Temperature & Tint
        color.r += uTemperature * 0.15;
        color.b -= uTemperature * 0.15;
        color.g -= uTint * 0.12;
        color.r += uTint * 0.08;
        color.b += uTint * 0.08;
        
        // 6. Highlights & Shadows
        if (uHighlights != 0.0 || uShadows != 0.0) {
          float l = dot(color.rgb, vec3(0.299, 0.587, 0.114));
          float shadowMask = 1.0 - smoothstep(0.0, 0.5, l);
          float highlightMask = smoothstep(0.5, 1.0, l);
          color.rgb += uShadows * shadowMask * 0.2;
          color.rgb += uHighlights * highlightMask * 0.2;
        }
        
        // 7. Color Matrix Filter
        if (uUseColorMatrix == 1) {
          color = (uColorMatrix * color) + uColorOffset;
        }
        
        // 8. Vignette
        if (uVignette > 0.01) {
          vec2 coord = (vTextureCoord - 0.5) * 2.0;
          float distFromCenter = length(coord);
          float vig = 1.0 - smoothstep(0.7, 1.4, distFromCenter * (0.8 + uVignette * 0.8));
          color.rgb *= vig;
        }
        
        // 9. Grain
        if (uGrain > 0.01) {
          float noise = (rand(vTextureCoord) - 0.5) * uGrain * 0.3;
          color.rgb += vec3(noise);
        }
        
        // 10. Opacity
        color.rgb = clamp(color.rgb, 0.0, 1.0);
        color.a *= uOpacity;
        
        gl_FragColor = color;
      }
    """
  }

  const val TRANSITION_FRAGMENT_SHADER = """
    precision mediump float;
    varying vec2 vTextureCoord;
    uniform sampler2D uTextureFrom;
    uniform sampler2D uTextureTo;
    uniform float uProgress;
    uniform int uType; // 0=FADE, 1=DISSOLVE, 2=WIPE, 3=SLIDE_LEFT, 4=SLIDE_RIGHT, 5=ZOOM_IN, 6=ZOOM_OUT, 7=SPIN, 8=BLUR, 9=FLASH, 10=GLITCH
    
    float rand(vec2 co) {
      return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
    }
    
    void main() {
      vec2 p = vTextureCoord;
      float pr = clamp(uProgress, 0.0, 1.0);
      
      if (uType == 0) { // FADE
        vec4 from = texture2D(uTextureFrom, p);
        vec4 to = texture2D(uTextureTo, p);
        gl_FragColor = mix(from, to, pr);
      } else if (uType == 1) { // DISSOLVE
        float noise = rand(p);
        if (noise < pr) {
          gl_FragColor = texture2D(uTextureTo, p);
        } else {
          gl_FragColor = texture2D(uTextureFrom, p);
        }
      } else if (uType == 2) { // WIPE
        if (p.x < pr) {
          gl_FragColor = texture2D(uTextureTo, p);
        } else {
          gl_FragColor = texture2D(uTextureFrom, p);
        }
      } else if (uType == 3) { // SLIDE LEFT
        vec2 pTo = p + vec2(1.0 - pr, 0.0);
        vec2 pFrom = p - vec2(pr, 0.0);
        if (p.x < 1.0 - pr) {
          gl_FragColor = texture2D(uTextureFrom, p + vec2(pr, 0.0));
        } else {
          gl_FragColor = texture2D(uTextureTo, p - vec2(1.0 - pr, 0.0));
        }
      } else if (uType == 4) { // SLIDE RIGHT
        if (p.x < pr) {
          gl_FragColor = texture2D(uTextureTo, p + vec2(1.0 - pr, 0.0));
        } else {
          gl_FragColor = texture2D(uTextureFrom, p - vec2(pr, 0.0));
        }
      } else if (uType == 5) { // ZOOM IN
        vec2 center = vec2(0.5, 0.5);
        vec2 pFromZoom = center + (p - center) / (1.0 + pr * 0.8);
        vec2 pToZoom = center + (p - center) * (2.0 - pr);
        vec4 from = texture2D(uTextureFrom, clamp(pFromZoom, 0.0, 1.0));
        vec4 to = texture2D(uTextureTo, clamp(pToZoom, 0.0, 1.0));
        gl_FragColor = mix(from, to, smoothstep(0.3, 0.7, pr));
      } else if (uType == 9) { // FLASH
        vec4 from = texture2D(uTextureFrom, p);
        vec4 to = texture2D(uTextureTo, p);
        float flash = 1.0 - abs(pr - 0.5) * 2.0;
        vec4 blended = mix(from, to, pr);
        gl_FragColor = mix(blended, vec4(1.0), flash * 0.85);
      } else if (uType == 10) { // GLITCH
        float blockY = floor(p.y * 30.0);
        float r = rand(vec2(blockY, pr));
        vec2 offset = vec2(0.0);
        if (r > 0.75) {
          offset.x = (r - 0.75) * 0.15 * sin(pr * 3.14159);
        }
        vec4 from = texture2D(uTextureFrom, clamp(p + offset, 0.0, 1.0));
        vec4 to = texture2D(uTextureTo, clamp(p - offset, 0.0, 1.0));
        gl_FragColor = mix(from, to, pr);
      } else { // Default blend
        vec4 from = texture2D(uTextureFrom, p);
        vec4 to = texture2D(uTextureTo, p);
        gl_FragColor = mix(from, to, pr);
      }
    }
  """
}
