package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioColorScheme = lightColorScheme(
  primary = CyanAccent,
  onPrimary = Color.White,
  primaryContainer = SkyBlueContainer,
  onPrimaryContainer = CyanAccentDark,
  secondary = PurpleAccent,
  onSecondary = Color.White,
  secondaryContainer = StudioSurfaceVariant,
  onSecondaryContainer = PurpleAccent,
  tertiary = PinkAccent,
  onTertiary = Color.White,
  background = StudioDarkBg,
  onBackground = TextPrimary,
  surface = StudioSurface,
  onSurface = TextPrimary,
  surfaceVariant = StudioSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = StudioBorder,
  error = RedAccent,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = StudioColorScheme,
    typography = Typography,
    content = content
  )
}

