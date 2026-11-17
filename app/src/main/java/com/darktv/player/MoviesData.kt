package com.darktv.player

import androidx.compose.ui.graphics.Color

data class Movie(
    val id: String,
    val title: String,
    val titleEn: String,
    val coverUrl: String,
    val duration: String,
    val quality: String,
    val size: String,
    val genre: String,
    val year: String,
    val description: String,
    val downloadUrl: String,
    val downloadHeaders: Map<String, String> = emptyMap(),
    val rating: Float = 0f,
    val badge: String? = null,
    val color: Color = Color(0xFF6366F1)
)

object MoviesRepository {
    val genres = listOf("همه", "اکشن", "درام", "کمدی", "ترسناک", "عاشقانه", "مستند", "انیمیشن")

    // Sample catalog – replace with a real API when available.
    val movies = listOf(
        Movie("mov_001", "آخرین انتخاب", "The Last Choice", "", "2h 05m", "1080p", "1.8 GB", "درام", "2024",
            "فیلمی درام درباره تصمیم‌های سرنوشت‌ساز در زندگی.", "https://example.com/movies/last_choice.mp4",
            rating = 8.2f, badge = "جدید", color = Color(0xFF7C3AED)),
        Movie("mov_002", "راز شب", "Night Secret", "", "1h 40m", "720p", "1.1 GB", "ترسناک", "2023",
            "وقتی شب فرا می‌رسد، رازها بیدار می‌شوند.", "https://example.com/movies/night_secret.mp4",
            rating = 7.4f, color = Color(0xFF0E7490)),
        Movie("mov_003", "خنده در باران", "Laugh in Rain", "", "1h 30m", "1080p", "1.2 GB", "کمدی", "2024",
            "یک کمدی شیرین برای یک عصر دل‌چسب.", "https://example.com/movies/laugh_rain.mp4",
            rating = 6.9f, color = Color(0xFFEAB308)),
        Movie("mov_004", "برخاست", "Ascension", "", "2h 20m", "1080p", "2.2 GB", "اکشن", "2024",
            "اکشن مهیج با صحنه‌های نفس‌گیر.", "https://example.com/movies/ascension.mp4",
            rating = 8.7f, badge = "جدید", color = Color(0xFFEF4444)),
        Movie("mov_005", "عاشقانه‌های کوچک", "Little Romances", "", "1h 45m", "720p", "980 MB", "عاشقانه", "2023",
            "داستانی گرم و صمیمی از عشق‌های ساده.", "https://example.com/movies/little/romances.mp4",
            rating = 7.1f, color = Color(0xFFEC4899)),
        Movie("mov_006", "مهبانگ", "Big Bang", "", "1h 55m", "1080p", "1.6 GB", "مستند", "2024",
            "مستندی درباره پیدایش جهان.", "https://example.com/movies/bigbang.mp4",
            rating = 8.9f, badge = "مستند", color = Color(0xFF06B6D4)),
        Movie("mov_007", "پیله", "Cocoon", "", "1h 25m", "1080p", "1.0 GB", "انیمیشن", "2024",
            "انیمیشنی زیبا برای همه سنین.", "https://example.com/movies/cocoon.mp4",
            rating = 8.3f, color = Color(0xFF22C55E))
    )

    fun getByGenre(genre: String): List<Movie> =
        if (genre == "همه") movies else movies.filter { it.genre == genre }

    fun search(query: String): List<Movie> =
        if (query.isBlank()) movies else movies.filter {
            it.title.contains(query, ignoreCase = true) || it.titleEn.contains(query, ignoreCase = true)
        }

    fun getTopRated(): List<Movie> = movies.sortedByDescending { it.rating }.take(5)
}
