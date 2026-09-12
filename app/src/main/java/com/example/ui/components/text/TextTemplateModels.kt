package com.example.ui.components.text

import androidx.compose.runtime.Immutable

@Immutable
data class TextTemplateItem(
  val id: String,
  val name: String,
  val category: String,
  val sampleText: String,
  val fontFamily: String,
  val fontSizeSp: Float = 28f,
  val fontWeight: Int = 800,
  val isItalic: Boolean = false,
  val isUnderline: Boolean = false,
  val isAllCaps: Boolean = false,
  val textColor: Long = 0xFFFFFFFF,
  val hasGradient: Boolean = false,
  val gradientColorStart: Long = 0xFF00E5FF,
  val gradientColorEnd: Long = 0xFF8B5CF6,
  val gradientDirection: String = "Horizontal",
  val strokeWidth: Float = 0f,
  val strokeColor: Long = 0xFF000000,
  val hasShadow: Boolean = true,
  val shadowColor: Long = 0x88000000,
  val shadowBlur: Float = 4f,
  val shadowOffsetX: Float = 2f,
  val shadowOffsetY: Float = 2f,
  val hasBackground: Boolean = false,
  val backgroundColor: Long = 0xCC000000,
  val cornerRadius: Float = 12f,
  val bgPadding: Float = 16f,
  val opacity: Float = 1.0f,
  val animationType: String = "Pop",
  val badgeEmoji: String = "✨",
  val effectStyle: String = "None"
)

data class TemplateCategoryInfo(
  val name: String,
  val iconEmoji: String,
  val description: String = ""
)

object TemplateCategories {
  val ALL_CATEGORIES = listOf(
    TemplateCategoryInfo("Trending", "🔥", "Most popular viral styles"),
    TemplateCategoryInfo("Whimsical", "🦄", "Playful, magical & cute"),
    TemplateCategoryInfo("KATSEYE", "⭐", "K-Pop idol & glam chic"),
    TemplateCategoryInfo("Pixel Bead", "👾", "Retro 8-bit & arcade pixel"),
    TemplateCategoryInfo("Classic", "🏛️", "Timeless, elegant & clean"),
    TemplateCategoryInfo("New", "✨", "Freshly dropped designs"),
    TemplateCategoryInfo("Hits", "🎵", "Top charting video titles"),
    TemplateCategoryInfo("Free Fire", "🎮", "Action gaming & battle royale"),
    TemplateCategoryInfo("Nailing", "💅", "Beauty, salon & nail art"),
    TemplateCategoryInfo("Icon", "🏷️", "Emblem & badge graphics"),
    TemplateCategoryInfo("Title", "🎬", "Big bold headline openers"),
    TemplateCategoryInfo("Retro", "📼", "90s VHS & vintage synthwave"),
    TemplateCategoryInfo("Advertisement", "📢", "Commercial promo & sales"),
    TemplateCategoryInfo("Social Media", "📱", "TikTok, Reels & Shorts stickers"),
    TemplateCategoryInfo("Vlog", "📹", "Daily lifestyle & creators"),
    TemplateCategoryInfo("Tag", "🏷️", "Pricing & product tags"),
    TemplateCategoryInfo("Chapter", "📑", "Video section dividers"),
    TemplateCategoryInfo("Daily", "☀️", "Everyday mood & routine"),
    TemplateCategoryInfo("Travel", "✈️", "Vacation & wanderlust"),
    TemplateCategoryInfo("Autumn", "🍂", "Warm cozy seasonal vibes"),
    TemplateCategoryInfo("Life", "🌿", "Mindfulness & calm stories"),
    TemplateCategoryInfo("Makeup", "💄", "Cosmetics & glam tutorial"),
    TemplateCategoryInfo("Spark", "⚡", "Energy, neon & flash"),
    TemplateCategoryInfo("Music", "🎧", "Beats, audio visual & lyrics"),
    TemplateCategoryInfo("Sports", "⚽", "Fitness, gym & athletics"),
    TemplateCategoryInfo("Festivals", "🎉", "Celebrations & party"),
    TemplateCategoryInfo("Captions", "💬", "High-visibility speech text"),
    TemplateCategoryInfo("Messages", "✉️", "Chat bubbles & DM style"),
    TemplateCategoryInfo("Game", "🕹️", "Stream overlays & esports"),
    TemplateCategoryInfo("News", "📰", "Breaking news & live ticker"),
    TemplateCategoryInfo("Time", "⏰", "Countdown, clock & dates"),
    TemplateCategoryInfo("Technology", "💻", "Cyber, coding & futuristic"),
    TemplateCategoryInfo("Food", "🍔", "Recipes, cafe & restaurant"),
    TemplateCategoryInfo("Summer", "🏖️", "Beach, sunny & tropical"),
    TemplateCategoryInfo("3D", "🧊", "Dimensional extruded text"),
    TemplateCategoryInfo("Kaomoji", "ʕ•ᴥ•ʔ", "Cute text emotes & faces"),
    TemplateCategoryInfo("Campus", "🎓", "College, study & student life"),
    TemplateCategoryInfo("Pet", "🐾", "Cats, dogs & animal fun"),
    TemplateCategoryInfo("Outro", "🏁", "Ending cards & subscribe"),
    TemplateCategoryInfo("Spider", "🕸️", "Dark edgy comic superhero"),
    TemplateCategoryInfo("Minions", "🍌", "Yellow cartoon mischief")
  )
}

val ALL_TEXT_TEMPLATES: List<TextTemplateItem> = listOf(
  // 1. Trending 🔥
  TextTemplateItem(
    id = "trend_glow_pop",
    name = "Trending Neon Pop",
    category = "Trending",
    sampleText = "VIRAL HIT 🔥",
    fontFamily = "Impact",
    fontSizeSp = 32f,
    fontWeight = 900,
    textColor = 0xFFFF007F,
    hasGradient = true,
    gradientColorStart = 0xFFFF007F,
    gradientColorEnd = 0xFF00F0FF,
    strokeWidth = 3f,
    strokeColor = 0xFF000000,
    hasShadow = true,
    shadowColor = 0xFFFF007F,
    animationType = "Pop",
    badgeEmoji = "🔥"
  ),
  TextTemplateItem(
    id = "trend_glow_pill",
    name = "Trending Glass Capsule",
    category = "Trending",
    sampleText = "MUST WATCH 👀",
    fontFamily = "Montserrat",
    fontSizeSp = 26f,
    fontWeight = 800,
    textColor = 0xFFFFFFFF,
    hasBackground = true,
    backgroundColor = 0xEE1E293B,
    cornerRadius = 30f,
    bgPadding = 18f,
    strokeWidth = 1.5f,
    strokeColor = 0xFF00E5FF,
    animationType = "Zoom",
    badgeEmoji = "🚀"
  ),

  // 2. Whimsical 🦄
  TextTemplateItem(
    id = "whimsical_fairy",
    name = "Fairy Dream Sparkle",
    category = "Whimsical",
    sampleText = "✨ Magic Moment ✨",
    fontFamily = "Cursive",
    fontSizeSp = 30f,
    fontWeight = 700,
    textColor = 0xFFFFD1DC,
    hasGradient = true,
    gradientColorStart = 0xFFFFD1DC,
    gradientColorEnd = 0xFFE0BBE4,
    hasShadow = true,
    shadowColor = 0x99957DAD,
    animationType = "Bounce",
    badgeEmoji = "🦄"
  ),
  TextTemplateItem(
    id = "whimsical_candy",
    name = "Cotton Candy Cloud",
    category = "Whimsical",
    sampleText = "Sweet Dreams 🍭",
    fontFamily = "Sans-Serif",
    fontSizeSp = 28f,
    fontWeight = 800,
    textColor = 0xFFFFF0F5,
    hasBackground = true,
    backgroundColor = 0xCCFF69B4,
    cornerRadius = 24f,
    bgPadding = 16f,
    animationType = "Pop",
    badgeEmoji = "🍬"
  ),

  // 3. KATSEYE ⭐
  TextTemplateItem(
    id = "katseye_idol_pink",
    name = "KATSEYE Debut Glam",
    category = "KATSEYE",
    sampleText = "TOUCH THE CROWN 👑",
    fontFamily = "Montserrat",
    fontSizeSp = 30f,
    fontWeight = 900,
    textColor = 0xFFFF1493,
    hasGradient = true,
    gradientColorStart = 0xFFFF1493,
    gradientColorEnd = 0xFFFF69B4,
    strokeWidth = 2f,
    strokeColor = 0xFFFFFFFF,
    hasShadow = true,
    shadowColor = 0xAAFF1493,
    animationType = "Pop",
    badgeEmoji = "⭐"
  ),
  TextTemplateItem(
    id = "katseye_cyber_y2k",
    name = "Y2K Pop Star",
    category = "KATSEYE",
    sampleText = "GIRL GROUP VIBES ✨",
    fontFamily = "Futuristic",
    fontSizeSp = 28f,
    fontWeight = 800,
    textColor = 0xFF00FFFF,
    strokeWidth = 2.5f,
    strokeColor = 0xFFFF007F,
    animationType = "Slide",
    badgeEmoji = "💎"
  ),

  // 4. Pixel Bead 👾
  TextTemplateItem(
    id = "pixel_arcade",
    name = "8-Bit Pixel Bead",
    category = "Pixel Bead",
    sampleText = "LEVEL UP! 🎮",
    fontFamily = "Monospace",
    fontSizeSp = 28f,
    fontWeight = 900,
    textColor = 0xFF00FF66,
    hasBackground = true,
    backgroundColor = 0xFF111827,
    strokeWidth = 2f,
    strokeColor = 0xFF00FF66,
    cornerRadius = 4f,
    animationType = "Typewriter",
    badgeEmoji = "👾"
  ),

  // 5. Classic 🏛️
  TextTemplateItem(
    id = "classic_serif_gold",
    name = "Classic Editorial Gold",
    category = "Classic",
    sampleText = "THE ART OF CINEMA",
    fontFamily = "Playfair",
    fontSizeSp = 30f,
    fontWeight = 700,
    textColor = 0xFFFFD700,
    strokeWidth = 1f,
    strokeColor = 0xFFB8860B,
    hasShadow = true,
    shadowColor = 0xAA000000,
    animationType = "Fade",
    badgeEmoji = "🏛️"
  ),
  TextTemplateItem(
    id = "classic_clean_lower_third",
    name = "Minimalist Lower Third",
    category = "Classic",
    sampleText = "ALEXANDER HAMILTON",
    fontFamily = "Serif",
    fontSizeSp = 24f,
    fontWeight = 600,
    textColor = 0xFFF8FAFC,
    hasBackground = true,
    backgroundColor = 0xEE1E293B,
    cornerRadius = 6f,
    bgPadding = 14f,
    animationType = "Slide",
    badgeEmoji = "📜"
  ),

  // 6. New ✨
  TextTemplateItem(
    id = "new_aurora_glow",
    name = "Aurora Hologram",
    category = "New",
    sampleText = "NEW RELEASE ✨",
    fontFamily = "Montserrat",
    fontSizeSp = 30f,
    fontWeight = 800,
    textColor = 0xFFE0F2FE,
    hasGradient = true,
    gradientColorStart = 0xFF38BDF8,
    gradientColorEnd = 0xFFA855F7,
    hasShadow = true,
    shadowColor = 0xFF38BDF8,
    animationType = "Pop",
    badgeEmoji = "✨"
  ),

  // 7. Hits 🎵
  TextTemplateItem(
    id = "hits_billboard_bold",
    name = "Top Billboard Hit",
    category = "Hits",
    sampleText = "#1 GLOBAL TRACK 🏆",
    fontFamily = "Impact",
    fontSizeSp = 32f,
    fontWeight = 900,
    textColor = 0xFFFFEB3B,
    strokeWidth = 3f,
    strokeColor = 0xFF000000,
    hasShadow = true,
    shadowColor = 0xFF000000,
    animationType = "Pop",
    badgeEmoji = "🎵"
  ),

  // 8. Free Fire 🎮
  TextTemplateItem(
    id = "ff_booyah",
    name = "BOOYAH Victory Banner",
    category = "Free Fire",
    sampleText = "BOOYAH! #1 🏆",
    fontFamily = "Impact",
    fontSizeSp = 34f,
    fontWeight = 900,
    textColor = 0xFFFF9900,
    hasGradient = true,
    gradientColorStart = 0xFFFF9900,
    gradientColorEnd = 0xFFFF0000,
    strokeWidth = 3.5f,
    strokeColor = 0xFF000000,
    hasShadow = true,
    shadowColor = 0xFFFF4500,
    animationType = "Pop",
    badgeEmoji = "🔥"
  ),
  TextTemplateItem(
    id = "ff_headshot",
    name = "Free Fire Headshot",
    category = "Free Fire",
    sampleText = "HEADSHOT 🎯 999+",
    fontFamily = "Bebas",
    fontSizeSp = 32f,
    fontWeight = 900,
    textColor = 0xFFFF0033,
    hasBackground = true,
    backgroundColor = 0xEE1A1A1A,
    strokeWidth = 2f,
    strokeColor = 0xFFFF0033,
    cornerRadius = 6f,
    animationType = "Shake",
    badgeEmoji = "🎯"
  ),

  // 9. Nailing 💅
  TextTemplateItem(
    id = "nailing_glam",
    name = "Nail Art Studio",
    category = "Nailing",
    sampleText = "Nails On Fleek 💅",
    fontFamily = "Playfair",
    fontSizeSp = 28f,
    fontWeight = 800,
    textColor = 0xFFFFB6C1,
    hasBackground = true,
    backgroundColor = 0xEE4A154B,
    cornerRadius = 20f,
    bgPadding = 16f,
    animationType = "Fade",
    badgeEmoji = "💅"
  ),

  // 10. Icon 🏷️
  TextTemplateItem(
    id = "icon_verified_badge",
    name = "Official Verified Badge",
    category = "Icon",
    sampleText = "VERIFIED OFFICIAL ✔️",
    fontFamily = "Montserrat",
    fontSizeSp = 24f,
    fontWeight = 800,
    textColor = 0xFF38BDF8,
    hasBackground = true,
    backgroundColor = 0xEE0369A1,
    cornerRadius = 16f,
    bgPadding = 14f,
    animationType = "Pop",
    badgeEmoji = "🏷️"
  ),

  // 11. Title 🎬
  TextTemplateItem(
    id = "title_blockbuster",
    name = "Cinematic Movie Title",
    category = "Title",
    sampleText = "THE FINAL CHRONICLE",
    fontFamily = "Cinematic",
    fontSizeSp = 32f,
    fontWeight = 800,
    textColor = 0xFFF1F5F9,
    hasShadow = true,
    shadowColor = 0xDD000000,
    shadowBlur = 8f,
    animationType = "Fade",
    badgeEmoji = "🎬"
  ),
  TextTemplateItem(
    id = "title_split_modern",
    name = "Modern Split Header",
    category = "Title",
    sampleText = "EPISODE 01 // BEGINNING",
    fontFamily = "Bebas",
    fontSizeSp = 30f,
    fontWeight = 800,
    textColor = 0xFF00E5FF,
    hasBackground = true,
    backgroundColor = 0xDD0F172A,
    cornerRadius = 8f,
    bgPadding = 16f,
    animationType = "Slide",
    badgeEmoji = "🎥"
  ),

  // 12. Retro 📼
  TextTemplateItem(
    id = "retro_vhs_glitch",
    name = "90s VHS Synthwave",
    category = "Retro",
    sampleText = "PLAY ▶ 1994 VHS",
    fontFamily = "Monospace",
    fontSizeSp = 28f,
    fontWeight = 900,
    textColor = 0xFF00FFCC,
    strokeWidth = 2f,
    strokeColor = 0xFFFF0099,
    hasShadow = true,
    shadowColor = 0xFFFF0099,
    animationType = "Typewriter",
    badgeEmoji = "📼"
  ),

  // 13. Advertisement 📢
  TextTemplateItem(
    id = "ad_flash_sale",
    name = "Flash Sale 50% OFF",
    category = "Advertisement",
    sampleText = "50% OFF FLASH SALE 🔥",
    fontFamily = "Impact",
    fontSizeSp = 30f,
    fontWeight = 900,
    textColor = 0xFFFFFFFF,
    hasBackground = true,
    backgroundColor = 0xFFEF4444,
    cornerRadius = 10f,
    bgPadding = 18f,
    animationType = "Bounce",
    badgeEmoji = "📢"
  ),

  // 14. Social Media 📱
  TextTemplateItem(
    id = "social_subscribe_pill",
    name = "Subscribe & Like Pill",
    category = "Social Media",
    sampleText = "SUBSCRIBE & BELL 🔔",
    fontFamily = "Montserrat",
    fontSizeSp = 26f,
    fontWeight = 900,
    textColor = 0xFFFFFFFF,
    hasBackground = true,
    backgroundColor = 0xFFDC2626,
    cornerRadius = 30f,
    bgPadding = 20f,
    animationType = "Pop",
    badgeEmoji = "📱"
  ),
  TextTemplateItem(
    id = "social_tiktok_glow",
    name = "TikTok Viral Header",
    category = "Social Media",
    sampleText = "PART 2 IS UP! 💥",
    fontFamily = "Impact",
    fontSizeSp = 28f,
    fontWeight = 900,
    textColor = 0xFF00F0FF,
    strokeWidth = 2.5f,
    strokeColor = 0xFFFF0055,
    animationType = "Pop",
    badgeEmoji = "🎵"
  ),

  // 15. Vlog 📹
  TextTemplateItem(
    id = "vlog_cozy_morning",
    name = "Day in my Life Vlog",
    category = "Vlog",
    sampleText = "morning routine ☕ vlog",
    fontFamily = "Playfair",
    fontSizeSp = 26f,
    fontWeight = 700,
    isItalic = true,
    textColor = 0xFFFFFBEB,
    hasBackground = true,
    backgroundColor = 0xAA78350F,
    cornerRadius = 18f,
    bgPadding = 16f,
    animationType = "Fade",
    badgeEmoji = "📹"
  ),

  // 16. Tag 🏷️
  TextTemplateItem(
    id = "tag_price_pill",
    name = "Only $9.99 Tag",
    category = "Tag",
    sampleText = "ONLY $19.99 🏷️",
    fontFamily = "Montserrat",
    fontSizeSp = 24f,
    fontWeight = 900,
    textColor = 0xFF000000,
    hasBackground = true,
    backgroundColor = 0xFFFFEB3B,
    cornerRadius = 8f,
    bgPadding = 14f,
    animationType = "Pop",
    badgeEmoji = "🏷️"
  ),

  // 17. Chapter 📑
  TextTemplateItem(
    id = "chapter_divider",
    name = "Chapter Title Stamp",
    category = "Chapter",
    sampleText = "CHAPTER 02 • THE CLIMAX",
    fontFamily = "Bebas",
    fontSizeSp = 28f,
    fontWeight = 800,
    textColor = 0xFF38BDF8,
    hasBackground = true,
    backgroundColor = 0xEE0F172A,
    cornerRadius = 6f,
    bgPadding = 16f,
    animationType = "Slide",
    badgeEmoji = "📑"
  ),

  // 18. Daily ☀️
  TextTemplateItem(
    id = "daily_sunny_mood",
    name = "Good Morning Sunshine",
    category = "Daily",
    sampleText = "Today's Agenda ☀️",
    fontFamily = "Sans-Serif",
    fontSizeSp = 26f,
    fontWeight = 800,
    textColor = 0xFFFEF08A,
    hasBackground = true,
    backgroundColor = 0xCC854D0E,
    cornerRadius = 16f,
    bgPadding = 14f,
    animationType = "Fade",
    badgeEmoji = "☀️"
  ),

  // 19. Travel ✈️
  TextTemplateItem(
    id = "travel_wanderlust",
    name = "Tokyo Travel Diary",
    category = "Travel",
    sampleText = "TOKYO, JAPAN 🇯🇵 2026",
    fontFamily = "Cinematic",
    fontSizeSp = 28f,
    fontWeight = 800,
    textColor = 0xFFFFFFFF,
    hasBackground = true,
    backgroundColor = 0xAA047857,
    cornerRadius = 12f,
    bgPadding = 18f,
    animationType = "Slide",
    badgeEmoji = "✈️"
  ),

  // 20. Autumn 🍂
  TextTemplateItem(
    id = "autumn_warm_leaves",
    name = "Autumn Leaves Vibes",
    category = "Autumn",
    sampleText = "Cozy Autumn Nights 🍂",
    fontFamily = "Playfair",
    fontSizeSp = 28f,
    fontWeight = 700,
    textColor = 0xFFFFEDD5,
    hasBackground = true,
    backgroundColor = 0xDD9A3412,
    cornerRadius = 16f,
    bgPadding = 16f,
    animationType = "Fade",
    badgeEmoji = "🍂"
  ),

  // 21. Life 🌿
  TextTemplateItem(
    id = "life_zen_mindset",
    name = "Zen Mindful Quote",
    category = "Life",
    sampleText = "Peace In The Chaos 🌿",
    fontFamily = "Serif",
    fontSizeSp = 28f,
    fontWeight = 700,
    textColor = 0xFFECFDF5,
    hasBackground = true,
    backgroundColor = 0xAA065F46,
    cornerRadius = 20f,
    bgPadding = 16f,
    animationType = "Fade",
    badgeEmoji = "🌿"
  ),

  // 22. Makeup 💄
  TextTemplateItem(
    id = "makeup_glow_tutorial",
    name = "Glam Beauty Tutorial",
    category = "Makeup",
    sampleText = "Soft Glam Look 💄✨",
    fontFamily = "Cursive",
    fontSizeSp = 30f,
    fontWeight = 700,
    textColor = 0xFFFFE4E6,
    hasGradient = true,
    gradientColorStart = 0xFFFFE4E6,
    gradientColorEnd = 0xFFFDA4AF,
    hasShadow = true,
    shadowColor = 0x99BE185D,
    animationType = "Fade",
    badgeEmoji = "💄"
  ),

  // 23. Spark ⚡
  TextTemplateItem(
    id = "spark_electric_thunder",
    name = "Thunder Spark Surge",
    category = "Spark",
    sampleText = "ELECTRIC ENERGY ⚡",
    fontFamily = "Futuristic",
    fontSizeSp = 30f,
    fontWeight = 900,
    textColor = 0xFFFACC15,
    strokeWidth = 2.5f,
    strokeColor = 0xFF7C3AED,
    hasShadow = true,
    shadowColor = 0xFFFACC15,
    animationType = "Shake",
    badgeEmoji = "⚡"
  ),

  // 24. Music 🎧
  TextTemplateItem(
    id = "music_now_playing",
    name = "Now Playing Audio Card",
    category = "Music",
    sampleText = "NOW PLAYING 🎧 02:45",
    fontFamily = "Montserrat",
    fontSizeSp = 24f,
    fontWeight = 800,
    textColor = 0xFF22D3EE,
    hasBackground = true,
    backgroundColor = 0xEE111827,
    cornerRadius = 16f,
    bgPadding = 16f,
    animationType = "Slide",
    badgeEmoji = "🎧"
  ),

  // 25. Sports ⚽
  TextTemplateItem(
    id = "sports_gym_beast",
    name = "Gym Beast Motivation",
    category = "Sports",
    sampleText = "NO EXCUSES // GRIND 🏋️",
    fontFamily = "Impact",
    fontSizeSp = 32f,
    fontWeight = 900,
    textColor = 0xFFFFFFFF,
    hasBackground = true,
    backgroundColor = 0xFFEA580C,
    cornerRadius = 8f,
    bgPadding = 18f,
    animationType = "Pop",
    badgeEmoji = "⚽"
  ),

  // 26. Festivals 🎉
  TextTemplateItem(
    id = "festival_celebration_fireworks",
    name = "Festival Celebration",
    category = "Festivals",
    sampleText = "HAPPY CELEBRATION 🎉",
    fontFamily = "Impact",
    fontSizeSp = 32f,
    fontWeight = 900,
    textColor = 0xFFFFD700,
    hasGradient = true,
    gradientColorStart = 0xFFFFD700,
    gradientColorEnd = 0xFFFF1493,
    strokeWidth = 2f,
    strokeColor = 0xFF000000,
    hasShadow = true,
    shadowColor = 0xFFFF007F,
    animationType = "Bounce",
    badgeEmoji = "🎉"
  ),

  // 27. Captions 💬
  TextTemplateItem(
    id = "captions_bold_yellow",
    name = "High Visibility Subtitle",
    category = "Captions",
    sampleText = "THIS CHANGED EVERYTHING!",
    fontFamily = "Impact",
    fontSizeSp = 30f,
    fontWeight = 900,
    textColor = 0xFFFFEA00,
    strokeWidth = 4.5f,
    strokeColor = 0xFF000000,
    hasShadow = true,
    shadowColor = 0xFF000000,
    animationType = "Pop",
    badgeEmoji = "💬"
  ),

  // 28. Messages ✉️
  TextTemplateItem(
    id = "msg_chat_bubble_blue",
    name = "iMessage Chat Bubble",
    category = "Messages",
    sampleText = "Wait, are you serious? 😱",
    fontFamily = "Sans-Serif",
    fontSizeSp = 24f,
    fontWeight = 600,
    textColor = 0xFFFFFFFF,
    hasBackground = true,
    backgroundColor = 0xFF007AFF,
    cornerRadius = 18f,
    bgPadding = 16f,
    animationType = "Pop",
    badgeEmoji = "✉️"
  ),

  // 29. Game 🕹️
  TextTemplateItem(
    id = "game_victory_royale",
    name = "Victory Royale Esports",
    category = "Game",
    sampleText = "VICTORY ROYALE 👑",
    fontFamily = "Impact",
    fontSizeSp = 32f,
    fontWeight = 900,
    textColor = 0xFF38BDF8,
    strokeWidth = 3f,
    strokeColor = 0xFF1E293B,
    hasBackground = true,
    backgroundColor = 0xDD0284C7,
    cornerRadius = 8f,
    bgPadding = 18f,
    animationType = "Pop",
    badgeEmoji = "🕹️"
  ),

  // 30. News 📰
  TextTemplateItem(
    id = "news_breaking_alert",
    name = "Breaking News Alert",
    category = "News",
    sampleText = "BREAKING NEWS • LIVE",
    fontFamily = "Impact",
    fontSizeSp = 28f,
    fontWeight = 900,
    textColor = 0xFFFFFFFF,
    hasBackground = true,
    backgroundColor = 0xFFDC2626,
    cornerRadius = 4f,
    bgPadding = 16f,
    animationType = "Slide",
    badgeEmoji = "📰"
  ),

  // 31. Time ⏰
  TextTemplateItem(
    id = "time_countdown_clock",
    name = "Digital Time Stamp",
    category = "Time",
    sampleText = "23:59:59 // COUNTDOWN",
    fontFamily = "Monospace",
    fontSizeSp = 28f,
    fontWeight = 900,
    textColor = 0xFF00F0FF,
    hasBackground = true,
    backgroundColor = 0xEE030712,
    strokeWidth = 1f,
    strokeColor = 0xFF00F0FF,
    cornerRadius = 6f,
    animationType = "Typewriter",
    badgeEmoji = "⏰"
  ),

  // 32. Technology 💻
  TextTemplateItem(
    id = "tech_cyber_code",
    name = "Cyber Matrix Terminal",
    category = "Technology",
    sampleText = "<AI_INIT PROTOCOL_2088>",
    fontFamily = "Futuristic",
    fontSizeSp = 26f,
    fontWeight = 800,
    textColor = 0xFF22C55E,
    hasBackground = true,
    backgroundColor = 0xEE022C22,
    strokeWidth = 1.5f,
    strokeColor = 0xFF22C55E,
    cornerRadius = 8f,
    bgPadding = 14f,
    animationType = "Typewriter",
    badgeEmoji = "💻"
  ),

  // 33. Food 🍔
  TextTemplateItem(
    id = "food_tasty_recipe",
    name = "Delicious Burger Recipe",
    category = "Food",
    sampleText = "SECRET SAUCE RECIPE 🍔",
    fontFamily = "Impact",
    fontSizeSp = 28f,
    fontWeight = 900,
    textColor = 0xFFFFFFFF,
    hasBackground = true,
    backgroundColor = 0xFFD97706,
    cornerRadius = 12f,
    bgPadding = 16f,
    animationType = "Bounce",
    badgeEmoji = "🍔"
  ),

  // 34. Summer 🏖️
  TextTemplateItem(
    id = "summer_tropical_beach",
    name = "Tropical Summer Vibes",
    category = "Summer",
    sampleText = "SUMMER VACATION 🏖️",
    fontFamily = "Bebas",
    fontSizeSp = 32f,
    fontWeight = 900,
    textColor = 0xFF06B6D4,
    hasGradient = true,
    gradientColorStart = 0xFF06B6D4,
    gradientColorEnd = 0xFFF59E0B,
    strokeWidth = 2f,
    strokeColor = 0xFFFFFFFF,
    animationType = "Pop",
    badgeEmoji = "🏖️"
  ),

  // 35. 3D 🧊
  TextTemplateItem(
    id = "three_d_extrusion",
    name = "3D Extruded Block",
    category = "3D",
    sampleText = "3D DIMENSION 🧊",
    fontFamily = "Impact",
    fontSizeSp = 34f,
    fontWeight = 900,
    textColor = 0xFFFFFFFF,
    strokeWidth = 3f,
    strokeColor = 0xFF7C3AED,
    hasShadow = true,
    shadowColor = 0xFF4C1D95,
    shadowBlur = 12f,
    shadowOffsetX = 6f,
    shadowOffsetY = 6f,
    animationType = "Pop",
    badgeEmoji = "🧊"
  ),

  // 36. Kaomoji ʕ•ᴥ•ʔ
  TextTemplateItem(
    id = "kaomoji_cute_bear",
    name = "Cute Kaomoji Mood",
    category = "Kaomoji",
    sampleText = "(づ｡◕‿‿◕｡)づ yay!",
    fontFamily = "Sans-Serif",
    fontSizeSp = 26f,
    fontWeight = 800,
    textColor = 0xFFFF80BF,
    hasBackground = true,
    backgroundColor = 0xDD2D0A22,
    cornerRadius = 20f,
    bgPadding = 14f,
    animationType = "Bounce",
    badgeEmoji = "ʕ•ᴥ•ʔ"
  ),

  // 37. Campus 🎓
  TextTemplateItem(
    id = "campus_student_study",
    name = "Study With Me Campus",
    category = "Campus",
    sampleText = "EXAM STUDY WITH ME 📚",
    fontFamily = "Montserrat",
    fontSizeSp = 26f,
    fontWeight = 800,
    textColor = 0xFFFFFFFF,
    hasBackground = true,
    backgroundColor = 0xFF1D4ED8,
    cornerRadius = 10f,
    bgPadding = 16f,
    animationType = "Slide",
    badgeEmoji = "🎓"
  ),

  // 38. Pet 🐾
  TextTemplateItem(
    id = "pet_dog_cat_fun",
    name = "Puppy & Kitty Moments",
    category = "Pet",
    sampleText = "My Dog Did WHAT?! 🐶",
    fontFamily = "Sans-Serif",
    fontSizeSp = 28f,
    fontWeight = 800,
    textColor = 0xFFFEF3C7,
    hasBackground = true,
    backgroundColor = 0xFFB45309,
    cornerRadius = 20f,
    bgPadding = 16f,
    animationType = "Bounce",
    badgeEmoji = "🐾"
  ),

  // 39. Outro 🏁
  TextTemplateItem(
    id = "outro_end_screen",
    name = "Thanks For Watching",
    category = "Outro",
    sampleText = "THANKS FOR WATCHING! ❤️",
    fontFamily = "Montserrat",
    fontSizeSp = 28f,
    fontWeight = 900,
    textColor = 0xFFFFFFFF,
    hasBackground = true,
    backgroundColor = 0xEE09090B,
    strokeWidth = 1.5f,
    strokeColor = 0xFFE11D48,
    cornerRadius = 14f,
    bgPadding = 18f,
    animationType = "Fade",
    badgeEmoji = "🏁"
  ),

  // 40. Spider 🕸️
  TextTemplateItem(
    id = "spider_hero_comic",
    name = "Spider Comic Hero",
    category = "Spider",
    sampleText = "WITH GREAT POWER 🕷️",
    fontFamily = "Impact",
    fontSizeSp = 30f,
    fontWeight = 900,
    textColor = 0xFFFF1E27,
    strokeWidth = 3f,
    strokeColor = 0xFF003366,
    hasShadow = true,
    shadowColor = 0xFF000000,
    animationType = "Pop",
    badgeEmoji = "🕸️"
  ),

  // 41. Minions 🍌
  TextTemplateItem(
    id = "minions_banana_yellow",
    name = "Bello Banana Mischief",
    category = "Minions",
    sampleText = "BELLO! BANANA! 🍌",
    fontFamily = "Impact",
    fontSizeSp = 32f,
    fontWeight = 900,
    textColor = 0xFFFFD700,
    hasBackground = true,
    backgroundColor = 0xFF0284C7,
    strokeWidth = 2.5f,
    strokeColor = 0xFF000000,
    cornerRadius = 24f,
    bgPadding = 18f,
    animationType = "Bounce",
    badgeEmoji = "🍌"
  )
)
