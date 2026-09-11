package com.example.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.io.FileOutputStream

data class FontOption(
  val id: String,
  val name: String,
  val category: String = "English", // "Urdu", "English", "Custom"
  val nativeSample: String = "",
  val isCustom: Boolean = false,
  val filePath: String? = null
)

object FontManager {
  private const val TAG = "FontManager"
  private const val FONTS_DIR = "custom_fonts"

  val URDU_FONTS = listOf(
    FontOption("jameel_nastaliq", "Jameel Noori Nastaliq", "Urdu", "جمیل نوری نستعلیق"),
    FontOption("nastaleeq", "Urdu Calligraphy Nastaleeq", "Urdu", "اردو خطاطی نستعلیق"),
    FontOption("alvi_nastaliq", "Alvi Nastaleeq", "Urdu", "علوی نستعلیق"),
    FontOption("urdu_naskh", "Urdu Naskh Modern", "Urdu", "نسخ اردو"),
    FontOption("gulzar", "Gulzar Urdu", "Urdu", "گلزار اردو"),
    FontOption("lateef", "Lateef Urdu Script", "Urdu", "لطیف اردو"),
    FontOption("scheherazade", "Scheherazade Urdu", "Urdu", "شہربانو اردو"),
    FontOption("kasheeda", "Kasheeda Calligraphy", "Urdu", "کشیدہ اردو")
  )

  val ENGLISH_FONTS = listOf(
    FontOption("Sans-Serif", "Modern Sans", "English", "Modern Sans"),
    FontOption("Serif", "Classic Serif", "English", "Classic Serif"),
    FontOption("Monospace", "Monospace Code", "English", "Monospace"),
    FontOption("Impact", "Impact Display", "English", "IMPACT BOLD"),
    FontOption("Bebas", "Bebas Headline", "English", "BEBAS HEADER"),
    FontOption("Montserrat", "Montserrat Geometric", "English", "Montserrat"),
    FontOption("Playfair", "Playfair Editorial", "English", "Playfair"),
    FontOption("Cinematic", "Cinematic Wide", "English", "CINEMATIC"),
    FontOption("Cursive", "Creative Script", "English", "Creative Cursive"),
    FontOption("Futuristic", "Cyber Tech", "English", "CYBERPUNK")
  )

  val BUILT_IN_FONTS = URDU_FONTS + ENGLISH_FONTS

  fun getAvailableFonts(context: Context): List<FontOption> {
    val fonts = mutableListOf<FontOption>()
    fonts.addAll(BUILT_IN_FONTS)

    // 1. Add Custom Fonts from Custom Fonts Directory
    val dir = File(context.filesDir, FONTS_DIR)
    if (dir.exists() && dir.isDirectory) {
      val files = dir.listFiles { f -> f.extension.equals("ttf", true) || f.extension.equals("otf", true) }
      files?.forEach { f ->
        val displayName = f.nameWithoutExtension.replace('_', ' ')
        fonts.add(
          FontOption(
            id = f.name,
            name = "$displayName (Imported)",
            category = "Custom",
            nativeSample = "Custom Font",
            isCustom = true,
            filePath = f.absolutePath
          )
        )
      }
    }

    // 2. Add Plugin Fonts from Installed Plugins
    val pluginFonts = com.example.engine.plugin.PluginManager.getEnabledItemsForCategory(
      com.example.domain.plugin.PluginCategory.FONT
    )
    for ((plugin, fontItem) in pluginFonts) {
      val fontFile = plugin.getItemFile(fontItem)
      val paramLang = (fontItem.parameters["language"] as? String)
        ?: (fontItem.parameters["category"] as? String)
        ?: ""
      val isUrdu = paramLang.equals("Urdu", ignoreCase = true) ||
        fontItem.name.contains("urdu", ignoreCase = true) ||
        fontItem.name.contains("nastaliq", ignoreCase = true) ||
        fontItem.name.contains("nastaleeq", ignoreCase = true) ||
        fontItem.name.contains("alvi", ignoreCase = true) ||
        fontItem.name.contains("jameel", ignoreCase = true)
      
      val cat = when {
        isUrdu -> "Urdu"
        paramLang.equals("English", ignoreCase = true) -> "English"
        else -> "Custom"
      }

      val sample = (fontItem.parameters["sample"] as? String)
        ?: if (isUrdu) "جمیل نوری نستعلیق خطاطی" else fontItem.name

      fonts.add(
        FontOption(
          id = fontItem.id,
          name = "${fontItem.emoji} ${fontItem.name}",
          category = cat,
          nativeSample = sample,
          isCustom = true,
          filePath = fontFile?.absolutePath
        )
      )
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
        category = "Custom",
        nativeSample = "Imported",
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

    // 2. Map Urdu / English built-in typefaces
    val lower = fontFamily.lowercase()
    val baseTypeface = when {
      // Urdu / Arabic font mappings
      lower.contains("nastaliq") || lower.contains("nastaleeq") || lower.contains("jameel") || lower.contains("alvi") || lower.contains("kasheeda") -> {
        try {
          Typeface.create("sans-serif-arabic", Typeface.BOLD)
        } catch (_: Exception) {
          Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
      }
      lower.contains("naskh") || lower.contains("scheherazade") || lower.contains("lateef") || lower.contains("gulzar") -> {
        try {
          Typeface.create("sans-serif-arabic", Typeface.NORMAL)
        } catch (_: Exception) {
          Typeface.SERIF
        }
      }
      // English font mappings
      lower.contains("sans-serif") || lower.contains("sans") || lower.contains("montserrat") -> Typeface.SANS_SERIF
      lower.contains("serif") || lower.contains("playfair") || lower.contains("cinematic") -> Typeface.SERIF
      lower.contains("monospace") || lower.contains("code") -> Typeface.MONOSPACE
      lower.contains("impact") || lower.contains("bebas") || lower.contains("futuristic") -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      lower.contains("cursive") || lower.contains("script") -> {
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

    val lower = fontFamily.lowercase()
    return when {
      lower.contains("nastaliq") || lower.contains("nastaleeq") || lower.contains("jameel") || lower.contains("serif") || lower.contains("playfair") -> FontFamily.Serif
      lower.contains("monospace") || lower.contains("code") -> FontFamily.Monospace
      lower.contains("cursive") || lower.contains("script") -> FontFamily.Cursive
      else -> FontFamily.SansSerif
    }
  }
}
