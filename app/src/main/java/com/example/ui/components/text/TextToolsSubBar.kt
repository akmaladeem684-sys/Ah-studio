package com.example.ui.components.text

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class TextSubTool(val label: String, val icon: ImageVector, val tag: String) {
  ADD_TEXT("Add Text", Icons.Default.TextFields, "subtool_add_text"),
  AUTO_CAPTIONS("Auto Captions", Icons.Default.Subtitles, "subtool_auto_captions"),
  STICKERS("Stickers", Icons.Default.EmojiEmotions, "subtool_stickers"),
  DRAW("Draw", Icons.Default.Brush, "subtool_draw"),
  TEXT_TEMPLATES("Text Templates", Icons.Default.AutoAwesome, "subtool_text_templates"),
  TEXT_TO_AUDIO("Text to Audio", Icons.Default.RecordVoiceOver, "subtool_text_to_audio"),
  AUTO_LYRICS("Auto Lyrics", Icons.Default.MusicNote, "subtool_auto_lyrics")
}

@Composable
fun TextToolsSubBar(
  activeSubTool: TextSubTool?,
  onSelectSubTool: (TextSubTool) -> Unit,
  onBackToMainMenu: () -> Unit,
  modifier: Modifier = Modifier
) {
  BoxWithConstraints(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFF0C0E15))
      .drawBehind {
        drawLine(
          color = Color(0xFF1E2230),
          start = Offset(0f, 0f),
          end = Offset(size.width, 0f),
          strokeWidth = 1.dp.toPx()
        )
      }
  ) {
    val horizontalPadding = 8.dp
    val itemSpacing = 6.dp

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = horizontalPadding, vertical = 8.dp)
        .navigationBarsPadding()
        .testTag("text_subtools_scroll"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
      // 1. Back Button to Return to Main Navigation
      Surface(
        onClick = onBackToMainMenu,
        shape = RoundedCornerShape(8.dp),
        color = StudioSurfaceVariant,
        modifier = Modifier
          .height(52.dp)
          .width(42.dp)
          .testTag("text_back_to_main_nav")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            Icons.Default.ArrowBack,
            contentDescription = "Back to Main Navigation",
            tint = CyanAccent,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // 2. The 7 Text Sub-Tools
      TextSubTool.values().forEach { subTool ->
        val isSelected = activeSubTool == subTool
        TextSubToolItem(
          icon = subTool.icon,
          label = subTool.label,
          isSelected = isSelected,
          testTag = subTool.tag,
          onClick = { onSelectSubTool(subTool) }
        )
      }
    }
  }
}

@Composable
private fun TextSubToolItem(
  icon: ImageVector,
  label: String,
  isSelected: Boolean,
  testTag: String,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }

  Column(
    modifier = Modifier
      .width(62.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .background(
        if (isSelected) CyanAccent.copy(alpha = 0.15f)
        else Color.Transparent
      )
      .padding(vertical = 4.dp, horizontal = 2.dp)
      .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(
          if (isSelected) CyanAccent
          else StudioSurfaceVariant
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isSelected) Color.Black else TextSecondary,
        modifier = Modifier.size(16.dp)
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) CyanAccent else TextSecondary,
        textAlign = TextAlign.Center
      ),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
