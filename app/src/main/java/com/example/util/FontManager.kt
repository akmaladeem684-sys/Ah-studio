package com.example.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.io.File
import java.io.FileOutputStream

data class FontOption(
  val id: String,
  val name: String,
  val isCustom: Boolean = false,
  val filePath: String? = null
)

object FontManager {
  private const val TAG = "FontManager"
  private const val FONTS_DIR = "custom_fonts"

  val BUILT_IN_FONTS = listOf(
    FontOption("Default", "System Default"),
    FontOption("Sans-Serif", "Modern Sans"),
    FontOption("Serif", "Classic Serif"),
    FontOption("Monospace", "Monospace Code"),
    FontOption("Impact", "Impact Display"),
    FontOption("Bebas", "Bebas Headline"),
    FontOption("Montserrat", "Montserrat Geometric"),
    FontOption("Playfair", "Playfair Editorial"),
    FontOption("Cinematic", "Cinematic Wide"),
    FontOption("Cursive", "Creative Script")
  )

  fun getAvailableFonts(context: Context): List<FontOption> {
    val fonts = mutableListOf<FontOption>()
    fonts.addAll(BUILT_IN_FONTS)

    val dir = File(context.filesDir, FONTS_DIR)
    if (dir.exists() && dir.isDirectory) {
      val files = dir.listFiles { f -> f.extension.equals("ttf", true) || f.extension.equals("otf", true) }
      files?.forEach { f ->
        val displayName = f.nameWithoutExtension.replace('_', ' ')
        fonts.add(
          FontOption(
            id = f.name,
            name = "$displayName (Imported)",
            isCustom = true,
            filePath = f.absolutePath
          )
        )
      }
    }
    return fonts
  }

  fun importFont(context: Context, uri: Uri): FontOption? {
    try {
      var fileName = "imported_font_${System.currentTimeMillis()}.ttf"
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (colIndex != -1) {
            val name = cursor.getString(colIndex)
            if (!name.isNullOrBlank()) fileName = name
          }
        }
      }

      val dir = File(context.filesDir, FONTS_DIR)
      if (!dir.exists()) dir.mkdirs()

      val destination = File(dir, fileName)
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(destination).use { output ->
          input.copyTo(output)
        }
      }

      return FontOption(
        id = destination.name,
        name = destination.nameWithoutExtension.replace('_', ' ') + " (Imported)",
        isCustom = true,
        filePath = destination.absolutePath
      )
    } catch (e: Exception) {
      Log.e(TAG, "Failed to import font: ${e.message}", e)
      return null
    }
  }

  fun loadTypeface(
    context: Context,
    fontFamily: String,
    customFontPath: String?,
    fontWeight: Int = 700,
    isItalic: Boolean = false
  ): Typeface {
    // 1. Try custom font path first if present
    if (!customFontPath.isNullOrBlank()) {
      val file = File(customFontPath)
      if (file.exists()) {
        try {
          val customTypeface = Typeface.createFromFile(file)
          val style = when {
            fontWeight >= 700 && isItalic -> Typeface.BOLD_ITALIC
            fontWeight >= 700 -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
          }
          return Typeface.create(customTypeface, style)
        } catch (e: Exception) {
          Log.w(TAG, "Could not load custom font at $customFontPath: ${e.message}")
        }
      }
    }

    // 2. Try built-in typefaces
    val baseTypeface = when (fontFamily.lowercase()) {
      "sans-serif", "sans", "montserrat" -> Typeface.SANS_SERIF
      "serif", "playfair", "cinematic" -> Typeface.SERIF
      "monospace", "code" -> Typeface.MONOSPACE
      "impact", "bebas" -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      "cursive", "script" -> {
        try {
          Typeface.create("cursive", Typeface.NORMAL)
        } catch (_: Exception) {
          Typeface.SANS_SERIF
        }
      }
      else -> Typeface.DEFAULT
    }

    val style = when {
      fontWeight >= 700 && isItalic -> Typeface.BOLD_ITALIC
      fontWeight >= 700 -> Typeface.BOLD
      isItalic -> Typeface.ITALIC
      else -> Typeface.NORMAL
    }

    return Typeface.create(baseTypeface, style)
  }

  fun getComposeFontFamily(
    fontFamily: String,
    customFontPath: String?
  ): FontFamily {
    if (!customFontPath.isNullOrBlank()) {
      val file = File(customFontPath)
      if (file.exists()) {
        try {
          val tf = Typeface.createFromFile(file)
          return FontFamily(androidx.compose.ui.text.font.Typeface(tf))
        } catch (_: Exception) {}
      }
    }

    return when (fontFamily.lowercase()) {
      "sans-serif", "sans", "montserrat", "impact", "bebas" -> FontFamily.SansSerif
      "serif", "playfair", "cinematic" -> FontFamily.Serif
      "monospace", "code" -> FontFamily.Monospace
      "cursive", "script" -> FontFamily.Cursive
      else -> FontFamily.Default
    }
  }
}
