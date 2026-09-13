package com.example.ui.components.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom Theme Colors for Nav Items matching exact CapCut / Pro Editor design screenshot
 */
data class NavItemColorTheme(
  val bgCircle: Color,
  val iconTint: Color
)

object NavItemThemes {
  val AddText = NavItemColorTheme(Color(0xFF102A45), Color(0xFF00E5FF))
  val Captions = NavItemColorTheme(Color(0xFF321B60), Color(0xFFB388FF))
  val Stickers = NavItemColorTheme(Color(0xFF0B4D3C), Color(0xFF00E676))
  val DrawTheme = NavItemColorTheme(Color(0xFF421C6E), Color(0xFFD500F9))
  val Draw = DrawTheme
  val TextTemplateTheme = NavItemColorTheme(Color(0xFF5A380A), Color(0xFFFFAB40))
  val TextTemplate = TextTemplateTheme
  val Effects = NavItemColorTheme(Color(0xFF5C103C), Color(0xFFFF4081))
  val Filters = NavItemColorTheme(Color(0xFF0F3B66), Color(0xFF40C4FF))
  val Edit = NavItemColorTheme(Color(0xFF521217), Color(0xFFFF5252))
  val Audio = NavItemColorTheme(Color(0xFF094D52), Color(0xFF18FFFF))
  val Speed = NavItemColorTheme(Color(0xFF264D12), Color(0xFFB2FF59))
  val Animations = NavItemColorTheme(Color(0xFF54420A), Color(0xFFFFD740))
  val Overlay = NavItemColorTheme(Color(0xFF1A2350), Color(0xFF8C9EFF))
  val Transitions = NavItemColorTheme(Color(0xFF59220F), Color(0xFFFF6E40))
  val AI = NavItemColorTheme(Color(0xFF381A6E), Color(0xFFE040FB))
  val DefaultSlate = NavItemColorTheme(Color(0xFF1E293B), Color(0xFF94A3B8))
}

data class FuturisticNavItemData(
  val id: String,
  val label: String,
  val icon: ImageVector,
  val theme: NavItemColorTheme,
  val isSelected: Boolean,
  val testTag: String,
  val onClick: () -> Unit
)

/**
 * Futuristic Floating Capsule Bottom Navigation Bar (Matching exact user screenshot)
 */
@Composable
fun FuturisticBottomNavBarContainer(
  onBackClick: () -> Unit,
  items: List<FuturisticNavItemData>,
  modifier: Modifier = Modifier,
  showDividers: Boolean = true
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .navigationBarsPadding(),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .height(68.dp)
        .shadow(
          elevation = 12.dp,
          shape = RoundedCornerShape(26.dp),
          ambientColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
          spotColor = Color(0xFF7C4DFF).copy(alpha = 0.3f)
        )
        .border(
          width = 1.25.dp,
          brush = Brush.horizontalGradient(
            colors = listOf(
              Color(0xFF00E5FF).copy(alpha = 0.45f),
              Color(0xFF7C4DFF).copy(alpha = 0.55f),
              Color(0xFF00E5FF).copy(alpha = 0.45f)
            )
          ),
          shape = RoundedCornerShape(26.dp)
        ),
      shape = RoundedCornerShape(26.dp),
      color = Color(0xFF090D18).copy(alpha = 0.94f)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 7.dp)
          .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // 1. Dedicated Glowing Left Back Arrow Button
        Box(
          modifier = Modifier
            .size(width = 50.dp, height = 54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
              Brush.linearGradient(
                colors = listOf(
                  Color(0xFF16294A),
                  Color(0xFF0E1A33)
                )
              )
            )
            .border(
              BorderStroke(
                width = 1.5.dp,
                color = Color(0xFF00B0FF).copy(alpha = 0.85f)
              ),
              shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onBackClick)
            .testTag("nav_back_arrow_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(2.dp))

        // 2. Navigation Items with Optional Dividers
        items.forEachIndexed { index, item ->
          FuturisticNavItemView(item = item)

          if (showDividers && (index == 3 || index == 5 || index == 6)) {
            Box(
              modifier = Modifier
                .padding(horizontal = 2.dp)
                .width(1.dp)
                .height(30.dp)
                .background(Color(0xFF1E2638))
            )
          }
        }
      }
    }
  }
}

@Composable
fun FuturisticNavItemView(item: FuturisticNavItemData) {
  val interactionSource = remember { MutableInteractionSource() }

  Column(
    modifier = Modifier
      .width(66.dp)
      .height(60.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = item.onClick
      )
      .background(
        if (item.isSelected) Color(0xFF0E1A2E)
        else Color.Transparent
      )
      .border(
        width = if (item.isSelected) 1.75.dp else 0.dp,
        color = if (item.isSelected) Color(0xFF00E5FF) else Color.Transparent,
        shape = RoundedCornerShape(16.dp)
      )
      .padding(vertical = 4.dp, horizontal = 2.dp)
      .testTag(item.testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(CircleShape)
        .background(
          if (item.isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f)
          else item.theme.bgCircle
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = item.icon,
        contentDescription = item.label,
        tint = if (item.isSelected) Color(0xFF00E5FF) else item.theme.iconTint,
        modifier = Modifier.size(19.dp)
      )
    }

    Text(
      text = item.label,
      fontSize = 10.sp,
      fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.Medium,
      color = if (item.isSelected) Color.White else Color(0xFF94A3B8),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )

    // Selection Indicator bar at bottom
    if (item.isSelected) {
      Box(
        modifier = Modifier
          .width(20.dp)
          .height(3.dp)
          .clip(CircleShape)
          .background(Color(0xFF00E5FF))
      )
    } else {
      Spacer(modifier = Modifier.height(3.dp))
    }
  }
}
