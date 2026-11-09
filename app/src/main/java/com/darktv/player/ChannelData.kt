package com.darktv.player

import androidx.compose.ui.graphics.Color

data class StreamSource(val url: String, val title: String? = null, val headers: Map<String, String> = emptyMap())

data class Channel(
    val id: Int,
    val codeName: String,
    val fullName: String,
    val category: String,
    val url: String,
    val badgeText: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val logoUrl: String? = null,
    val extraUrls: List<String> = emptyList(),
    val categoryKeys: List<String> = emptyList(),
    val preferredSources: List<StreamSource> = emptyList(),
    val favoriteKey: String = "legacy:$id"
) {
    fun categories(): List<String> = if (categoryKeys.isNotEmpty()) categoryKeys else listOf(
        when (category) {
            "موزیک" -> "music"
            "فیلم و سریال" -> "movies"
            "سرگرمی و لایو" -> "entertainment"
            "سبک زندگی و کودک" -> "lifestyle"
            else -> "other"
        }
    )

    fun streamSources(): List<StreamSource> =
        (preferredSources + (listOf(url) + extraUrls + if (url.contains("46.245.122.7")) listOf(getFallbackUrl()) else emptyList())
            .map { StreamSource(it) }).distinctBy { it.url }
    fun sources(): List<String> = streamSources().map { it.url }

    fun getFallbackUrl(): String {
        return if (url.contains(":8080")) {
            url.replace(":8080", "")
        } else {
            url.replace("46.245.122.7", "46.245.122.7:8080")
        }
    }
}

fun orderFavorites(channels: List<Channel>, favorites: Set<String>): List<Channel> =
    channels.sortedBy { it.favoriteKey !in favorites }

object ChannelRepository {
    val categoryOrder = listOf("all", "general", "news", "movies", "music", "entertainment", "kids", "sports", "documentary", "lifestyle", "religious", "education", "other")

    fun normalizeCategory(category: String): String = when (category.lowercase()) {
        "general" -> "general"
        "news", "business" -> "news"
        "movies", "series", "classic" -> "movies"
        "music" -> "music"
        "entertainment", "comedy" -> "entertainment"
        "kids" -> "kids"
        "sports" -> "sports"
        "documentary", "culture" -> "documentary"
        "lifestyle", "family", "cooking" -> "lifestyle"
        "religious" -> "religious"
        "education" -> "education"
        else -> "other"
    }

    fun load(context: android.content.Context): List<Channel> {
        val result = channels.toMutableList()
        try {
            val json = org.json.JSONArray(context.assets.open("channels.json").bufferedReader().use { it.readText() })
            fun key(s: String) = s.lowercase().filter { it.isLetterOrDigit() || it == '+' }
            // Stable upstream IDs for legacy names that differ from the catalogue.
            val aliases = mapOf("GEMFit.tr" to 10, "GEMJunior.tr" to 12,
                "GEMClassic.tr" to 20, "GEMFood.tr" to 22, "GEMRubix.tr" to 23)
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                val name = item.getString("name")
                val urlsJson = item.getJSONArray("urls")
                val urls = (0 until urlsJson.length()).map { urlsJson.getString(it) }
                if (urls.isEmpty()) continue
                val logo = item.optString("logo").takeIf { it.isNotBlank() }
                val rawCategories = item.optJSONArray("categories")
                val categoryKeys = if (rawCategories == null) emptyList() else
                    (0 until rawCategories.length()).map { normalizeCategory(rawCategories.optString(it)) }.distinct()
                val alias = aliases[item.optString("id")]
                val existing = result.indexOfFirst { if (alias != null) it.id == alias else key(it.codeName) == key(name) }
                if (existing >= 0) {
                    val old = result[existing]
                    result[existing] = old.copy(logoUrl = logo ?: old.logoUrl, extraUrls = (old.extraUrls + urls).distinct(), categoryKeys = (old.categories() + categoryKeys).distinct())
                } else {
                    result.add(Channel(1000 + i, name, name, "تلویزیون", urls.first(), name.take(3).uppercase(), Color(0xFF2563EB), Color(0xFF7C3AED), logo, urls.drop(1), categoryKeys, favoriteKey = "iptv:${item.getString("id")}"))
                }
            }
        } catch (e: Exception) { android.util.Log.e("ChannelRepository", "Could not load catalogue", e) }
        return result
    }
    val channels = listOf(
        Channel(1, "Fun Plus", "فان پلاس (Fun Plus)", "سرگرمی و لایو", "http://46.245.122.7:8080/hls/funplus/stream.m3u8", "FUN+", Color(0xFFEC4899), Color(0xFF8B5CF6)),
        Channel(2, "Radio Javan", "رادیو جوان (Radio Javan)", "موزیک", "http://46.245.122.7:8080/hls/rjavan/stream.m3u8", "RJ", Color(0xFFE11D48), Color(0xFF9F1239)),
        Channel(3, "Tunnel Zaman", "تونل زمان (Tunnel Zaman)", "موزیک", "http://46.245.122.7:8080/hls/TunnelZaman/stream.m3u8", "TZ", Color(0xFFD97706), Color(0xFF92400E)),
        Channel(4, "S-Music", "اس موزیک (PMC / S-Music)", "موزیک", "http://46.245.122.7/hls/smusic/stream.m3u8", "S♪", Color(0xFF06B6D4), Color(0xFF2563EB)),
        Channel(5, "Music Video 24", "موزیک ویدیو ۲۴ (MV 24)", "موزیک", "http://46.245.122.7/hls/MusicVideo24/stream.m3u8", "MV24", Color(0xFF8B5CF6), Color(0xFF4F46E5)),
        Channel(6, "GEM Series", "جم سریز (GEM Series)", "فیلم و سریال", "http://46.245.122.7:8080/hls/live1/stream.m3u8", "LV1", Color(0xFFEF4444), Color(0xFFB91C1C)),
        Channel(7, "GEM Bollywood", "جم بالیوود (GEM Bollywood)", "فیلم و سریال", "http://46.245.122.7:8080/hls/live2/stream.m3u8", "LV2", Color(0xFFF97316), Color(0xFFC2410C)),
        Channel(8, "GEM Series+", "جم سریز پلاس (GEM Series+)", "فیلم و سریال", "http://46.245.122.7:8080/hls/live3/stream.m3u8", "LV3", Color(0xFF10B981), Color(0xFF047857)),
        Channel(9, "GEM TV", "جم تی‌وی (GEM TV)", "سرگرمی و لایو", "http://46.245.122.7:8080/hls/live4/stream.m3u8", "LV4", Color(0xFF3B82F6), Color(0xFF1D4ED8)),
        Channel(10, "GM Fit", "جم فیت (GEM Fit)", "سبک زندگی و کودک", "http://46.245.122.7/hls/gmfit/stream.m3u8", "FIT", Color(0xFF22C55E), Color(0xFF15803D)),
        Channel(11, "GNC", "جنرال نتورک (GNC Comedy)", "سرگرمی و لایو", "http://46.245.122.7/hls/gnc/stream.m3u8", "GNC", Color(0xFFEAB308), Color(0xFFA16207)),
        Channel(12, "GM Jun", "جم جونیور (GEM Junior)", "سبک زندگی و کودک", "http://46.245.122.7/hls/gmjun/stream.m3u8", "JR", Color(0xFFF43F5E), Color(0xFFFB923C)),
        Channel(13, "GR CNM", "گرند سینما (Grand Cinema)", "فیلم و سریال", "http://46.245.122.7/hls/grcnm/stream.m3u8", "GC", Color(0xFF6366F1), Color(0xFF312E81)),
        Channel(14, "FX 1", "اف‌ایکس ۱ (FX 1 Movies)", "فیلم و سریال", "http://46.245.122.7/hls/fx1/stream.m3u8", "FX1", Color(0xFF0EA5E9), Color(0xFF0369A1)),
        Channel(15, "FX 2", "اف‌ایکس ۲ (FX 2 Movies)", "فیلم و سریال", "http://46.245.122.7/hls/fx2/stream.m3u8", "FX2", Color(0xFF0284C7), Color(0xFF1E3A8A)),
        Channel(16, "MTC", "ام‌تی‌سی سینما (MTC Cinema)", "فیلم و سریال", "http://46.245.122.7/hls/mtc/stream.m3u8", "MTC", Color(0xFFA855F7), Color(0xFF6B21A8)),
        Channel(17, "Ekran", "اکران فیلم (Ekran TV)", "فیلم و سریال", "http://46.245.122.7/hls/ekranm/stream.m3u8", "EKR", Color(0xFFEC4899), Color(0xFF9D174D)),
        Channel(18, "Oxir", "اکسیر (Oxir TV)", "سرگرمی و لایو", "http://46.245.122.7/hls/oxir/stream.m3u8", "OXR", Color(0xFF14B8A6), Color(0xFF0F766E)),
        Channel(19, "ICC", "آی‌سی‌سی سینما (ICC Iranian Cinema)", "فیلم و سریال", "http://46.245.122.7/hls/icc/stream.m3u8", "ICC", Color(0xFFF59E0B), Color(0xFFB45309)),
        Channel(20, "GM CL", "جم کلاسیک (GEM Classic)", "فیلم و سریال", "http://46.245.122.7/hls/gmcl/stream.m3u8", "CL", Color(0xFF64748B), Color(0xFF334155)),
        Channel(21, "ITN", "شبکه آی‌تی‌ان (ITN Iran TV)", "سرگرمی و لایو", "http://46.245.122.7/hls/itn/stream.m3u8", "ITN", Color(0xFF10B981), Color(0xFF065F46)),
        Channel(22, "GM Food", "جم فود (GEM Food)", "سبک زندگی و کودک", "http://46.245.122.7/hls/gmfood/stream.m3u8", "FOOD", Color(0xFFF97316), Color(0xFF9A3412)),
        Channel(23, "RUB", "تله روبیکس (Rubix+)", "فیلم و سریال", "http://46.245.122.7/hls/RUB/stream.m3u8", "RUB", Color(0xFFDC2626), Color(0xFF7F1D1D)),
        Channel(24, "Y-Time", "یوتایم (Y-Time TV)", "سرگرمی و لایو", "http://46.245.122.7/hls/ytime/stream.m3u8", "YT", Color(0xFF3B82F6), Color(0xFF1E40AF)),
        Channel(25, "MB Persia", "ام‌بی‌سی پرشیا (MBC Persia)", "فیلم و سریال", "http://46.245.122.7/hls/mbpersia/stream.m3u8", "MBC", Color(0xFFD97706), Color(0xFF78350F)),
        Channel(26, "Ravi TV", "راوی تی‌وی (Ravi Documentary)", "سبک زندگی و کودک", "http://46.245.122.7/hls/ravitv/stream.m3u8", "RAVI", Color(0xFF059669), Color(0xFF064E3B)),
        Channel(27, "Khatereh", "شبکه خاطره (Khatereh TV)", "موزیک", "http://46.245.122.7/hls/khatereh/stream.m3u8", "KHT", Color(0xFF9333EA), Color(0xFF581C87))
    )
}
