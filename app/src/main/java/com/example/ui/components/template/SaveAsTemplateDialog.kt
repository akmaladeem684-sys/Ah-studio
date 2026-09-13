package com.example.ui.components.template

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.presets.VideoTemplate
import com.example.domain.StudioAccountManager
import com.example.domain.model.AspectRatio
import com.example.domain.model.Timeline
import com.example.ui.theme.*

@Composable
fun SaveAsTemplateDialog(
  currentTimeline: Timeline,
  currentAspectRatio: AspectRatio,
  initialTitle: String = "",
  onDismiss: () -> Unit,
  onSaved: (VideoTemplate) -> Unit = {}
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf(initialTitle.ifBlank { "My Custom Template" }) }
  var category by remember { mutableStateOf("User-Created") }
  var description by remember { mutableStateOf("Custom video editing template saved from AH Video Studio") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Style, contentDescription = null, tint = CyanAccent)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Save as Reusable Template", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          "This will save your active timeline (clips, layers, text, audio, effects, transitions) as an editable template in Home → Templates.",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
        )
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Template Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text("Category (e.g. Reels, TikTok, Vlogs, Ads)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Template Description") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            val savedTpl = StudioAccountManager.saveProjectAsTemplate(
              title = title,
              category = category,
              description = description,
              timeline = currentTimeline,
              aspectRatio = currentAspectRatio
            )
            Toast.makeText(context, "Template \"${savedTpl.title}\" saved to My Templates! 🎨", Toast.LENGTH_SHORT).show()
            onSaved(savedTpl)
            onDismiss()
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Save Template", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    },
    containerColor = StudioSurface
  )
}
