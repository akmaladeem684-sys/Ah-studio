package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlin.math.sin

/**
 * High-craft Startup Loading Screen featuring animated video editing tools:
 * - Animated filmstrip with moving perforation holes and multi-track clips
 * - Animated scissors / razor blade performing precision cut & slice with spark glow
 * - Animated sweeping timeline playhead (CTI) with real-time timecode & frame telemetry
 * - Dynamic pulsating audio waveform equalizer visualizer
 * - Interactive carousel highlighting core editing tools (Cut, Audio, Speed, AI)
 * - Gradient progress bar and telemetry status transitions
 */
@Composable
fun StartupLoadingScreen(
  progress: Float,
  statusText: String,
  onFinished: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "startup_anim")

  // Playhead horizontal sweep across the miniature filmstrip
  val playheadSweep by infiniteTransition.animateFloat(
    initialValue = 0.05f,
    targetValue = 0.95f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "playhead_sweep"
  )

  // Scissors snip angle oscillation (cutting motion)
  val scissorSnipAngle by infiniteTransition.animateFloat(
    initialValue = -18f,
    targetValue = 18f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scissor_snip"
  )

  // Cut spark flash intensity
  val cutSparkAlpha by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 650, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "cut_spark"
  )

  // Filmstrip sprocket scroll offset
  val sprocketOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 28f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "sprocket_scroll"
  )

  // Pulse for glowing rings
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  // SMPTE Frame calculation based on progress
  val currentFrame = remember(progress) {
    (progress * 180).toInt().coerceIn(0, 180)
  }
  val smpteSeconds = currentFrame / 60
  val smpteSubFrames = currentFrame % 60
  val smpteText = String.format("00:00:%02d:%02d", smpteSeconds, smpteSubFrames)

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF070A12),
            Color(0xFF0F172A),
            Color(0xFF060910)
          )
        )
      )
      .testTag("startup_loading_screen")
  ) {
    // Background Ambient Glow Orbs
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(CyanAccent.copy(alpha = 0.15f), Color.Transparent),
          center = Offset(size.width * 0.2f, size.height * 0.25f),
          radius = size.width * 0.6f
        )
      )
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(PurpleAccent.copy(alpha = 0.12f), Color.Transparent),
          center = Offset(size.width * 0.85f, size.height * 0.7f),
          radius = size.width * 0.55f
        )
      )
    }

    // Top Action Bar with Skip affordance
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Tech Spec Badge
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(GreenAccent)
          )
          Text(
            text = "4K 60FPS • ENGINE V2.4",
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color.White.copy(alpha = 0.85f),
              fontSize = 9.5.sp,
              fontWeight = FontWeight.SemiBold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.8.sp
            )
          )
        }
      }

      // Skip Button
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
          .clickable { onFinished() }
          .testTag("skip_startup_btn")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "Skip",
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color.White.copy(alpha = 0.85f),
              fontWeight = FontWeight.Medium,
              fontSize = 11.sp
            )
          )
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }

    // Main Centered Content Column
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // 1. Studio Logo & Brand Title
      Box(
        modifier = Modifier
          .scale(pulseScale)
          .size(76.dp)
          .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = CyanAccent)
          .clip(RoundedCornerShape(20.dp))
          .background(Color(0xFF131B2E))
          .border(
            1.5.dp,
            Brush.linearGradient(listOf(CyanAccent, PurpleAccent, AmberAccent)),
            RoundedCornerShape(20.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_ah_studio_logo_1788535110903),
          contentDescription = "Studio Logo",
          modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "AH VIDEO STUDIO",
          style = MaterialTheme.typography.titleLarge.copy(
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            letterSpacing = 1.2.sp
          )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = CyanAccent,
          modifier = Modifier.padding(top = 1.dp)
        ) {
          Text(
            text = "PRO",
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color.White,
              fontWeight = FontWeight.Black,
              fontSize = 10.sp,
              letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
          )
        }
      }

      Text(
        text = "Professional Timeline & Multi-Track Video Editor",
        style = MaterialTheme.typography.bodySmall.copy(
          color = Color.White.copy(alpha = 0.6f),
          fontSize = 12.sp,
          letterSpacing = 0.3.sp
        ),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(28.dp))

      // 2. THE EDITING TOOLS ANIMATION STAGE
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF101626).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        shadowElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
        ) {
          // Timeline Header Bar with Ruler & SMPTE Timecode
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(15.dp)
              )
              Text(
                text = "TIMELINE STAGE",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = Color.White.copy(alpha = 0.9f),
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.5.sp,
                  letterSpacing = 0.6.sp
                )
              )
            }

            // Realtime SMPTE Timecode Pill
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color.Black.copy(alpha = 0.6f),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, AmberAccent.copy(alpha = 0.6f))
            ) {
              Text(
                text = smpteText,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = AmberAccent,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // 2A. ANIMATED FILMSTRIP & RAZOR CUT TOOL
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(72.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF090D15))
              .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
          ) {
            // Animated Filmstrip Sprocket Perforations (Top & Bottom)
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              // Top Sprockets
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                for (i in 0..11) {
                  Box(
                    modifier = Modifier
                      .size(width = 12.dp, height = 4.dp)
                      .clip(RoundedCornerShape(1.dp))
                      .background(Color.White.copy(alpha = 0.25f))
                  )
                }
              }

              // Bottom Sprockets
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                for (i in 0..11) {
                  Box(
                    modifier = Modifier
                      .size(width = 12.dp, height = 4.dp)
                      .clip(RoundedCornerShape(1.dp))
                      .background(Color.White.copy(alpha = 0.25f))
                  )
                }
              }
            }

            // Video Clip Blocks on Filmstrip
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 9.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Clip A (Cyan/Blue Theme)
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF0284C7).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                  .weight(1.1f)
                  .fillMaxHeight()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "Clip_01.mp4",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = Color.White,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                  )
                  Text(
                    text = "04:30",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = Color.White.copy(alpha = 0.8f),
                      fontSize = 8.sp,
                      fontFamily = FontFamily.Monospace
                    )
                  )
                }
              }

              // Dynamic Cut Point Gap with Spark
              Box(
                modifier = Modifier
                  .width(3.dp)
                  .fillMaxHeight()
                  .background(AmberAccent.copy(alpha = cutSparkAlpha))
              )

              // Clip B (Purple/Violet Theme - Split from Clip A)
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF7C3AED).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA)),
                modifier = Modifier
                  .weight(1.3f)
                  .fillMaxHeight()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "Clip_02_Split",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = Color.White,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                  )
                  Text(
                    text = "05:15",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = Color.White.copy(alpha = 0.8f),
                      fontSize = 8.sp,
                      fontFamily = FontFamily.Monospace
                    )
                  )
                }
              }

              // Clip C (Emerald/Ending)
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF059669).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399)),
                modifier = Modifier
                  .weight(0.9f)
                  .fillMaxHeight()
              ) {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "B-Roll.mov",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = Color.White,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                  )
                }
              }
            }

            // Animated Scissors Cutting Tool Overlay above the cut point
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
              val cutPointX = maxWidth * 0.45f
              Box(
                modifier = Modifier
                  .offset(x = cutPointX - 14.dp, y = 2.dp)
                  .size(28.dp)
                  .rotate(scissorSnipAngle),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.ContentCut,
                  contentDescription = "Split Tool",
                  tint = AmberAccent,
                  modifier = Modifier.size(18.dp)
                )
              }

              // Moving CTI / Playhead Line sweeping across
              val sweepX = maxWidth * playheadSweep
              Box(
                modifier = Modifier
                  .offset(x = sweepX - 1.dp, y = 0.dp)
                  .width(2.dp)
                  .fillMaxHeight()
                  .background(Color.White)
              )
              // Glowing Playhead Needle Top
              Box(
                modifier = Modifier
                  .offset(x = sweepX - 4.dp, y = 0.dp)
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(CyanAccent)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 2B. ANIMATED AUDIO WAVEFORM EQUALIZER TRACK
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF090D15),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B)),
            modifier = Modifier
              .fillMaxWidth()
              .height(38.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = GreenAccent,
                modifier = Modifier.size(15.dp)
              )

              // 18 Animated Equalizer Bars Pulsing Dynamically
              Row(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                val time = (playheadSweep * 100).toInt()
                for (i in 0 until 18) {
                  val baseHeight = 6f + 20f * kotlin.math.abs(sin((time + i * 14) * 0.15f)).toFloat()
                  Box(
                    modifier = Modifier
                      .width(3.dp)
                      .height(baseHeight.dp)
                      .clip(RoundedCornerShape(1.5.dp))
                      .background(
                        if (i % 3 == 0) GreenAccent
                        else if (i % 2 == 0) CyanAccent
                        else PurpleAccent
                      )
                  )
                }
              }

              Text(
                text = "STEREO",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = Color.White.copy(alpha = 0.6f),
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 3. EDITING TOOLS CAROUSEL BADGES (Highlights tools loading)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        EditingToolBadge(
          icon = Icons.Default.ContentCut,
          label = "Smart Split",
          isActive = progress in 0.2f..0.5f || progress >= 0.95f,
          activeColor = AmberAccent
        )
        EditingToolBadge(
          icon = Icons.Default.GraphicEq,
          label = "Audio Beat",
          isActive = progress in 0.4f..0.7f || progress >= 0.95f,
          activeColor = GreenAccent
        )
        EditingToolBadge(
          icon = Icons.Default.Speed,
          label = "Speed Curve",
          isActive = progress in 0.6f..0.85f || progress >= 0.95f,
          activeColor = CyanAccent
        )
        EditingToolBadge(
          icon = Icons.Default.AutoAwesome,
          label = "AI Effects",
          isActive = progress in 0.75f..1.0f,
          activeColor = PurpleAccent
        )
      }

      Spacer(modifier = Modifier.height(26.dp))

      // 4. PROGRESS BAR & STATUS READOUT
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Status Row (Current task + Percentage)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(CyanAccent)
            )
            Text(
              text = statusText,
              style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
              ),
              maxLines = 1
            )
          }

          Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = CyanAccent,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              fontSize = 13.sp
            )
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom Gradient Progress Track
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF1E293B))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(progress.coerceIn(0.02f, 1f))
              .fillMaxHeight()
              .clip(RoundedCornerShape(3.dp))
              .background(
                Brush.horizontalGradient(
                  colors = listOf(
                    CyanAccent,
                    PurpleAccent,
                    AmberAccent
                  )
                )
              )
          )
        }
      }
    }
  }
}

@Composable
private fun EditingToolBadge(
  icon: ImageVector,
  label: String,
  isActive: Boolean,
  activeColor: Color
) {
  val animScale by animateFloatAsState(
    targetValue = if (isActive) 1.06f else 1.0f,
    label = "tool_scale"
  )

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isActive) activeColor.copy(alpha = 0.15f) else Color(0xFF111726),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isActive) activeColor.copy(alpha = 0.6f) else Color(0xFF1E293B)
    ),
    modifier = Modifier
      .scale(animScale)
      .width(72.dp)
      .height(54.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isActive) activeColor else Color.White.copy(alpha = 0.5f),
        modifier = Modifier.size(19.dp)
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          color = if (isActive) Color.White else Color.White.copy(alpha = 0.55f),
          fontSize = 9.sp,
          fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        ),
        maxLines = 1
      )
    }
  }
}
