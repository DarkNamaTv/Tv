package com.darktv.player

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val movieGradients = listOf(
    listOf(Color(0xFF7C3AED), Color(0xFF4F46E5)),
    listOf(Color(0xFF0E7490), Color(0xFF06B6D4)),
    listOf(Color(0xFFEF4444), Color(0xFF7F1D1D)),
    listOf(Color(0xFFEAB308), Color(0xFFD97706)),
    listOf(Color(0xFFEC4899), Color(0xFF9D174D)),
    listOf(Color(0xFF22C55E), Color(0xFF047857)),
    listOf(Color(0xFF06B6D4), Color(0xFF1E3A8A)),
    listOf(Color(0xFF64748B), Color(0xFF1E293B))
)

private fun movieGradient(id: String): List<Color> {
    val idx = id.hashCode() and 0x7fffffff
    return movieGradients[idx % movieGradients.size]
}

@Composable
fun MoviesScreen(
    english: Boolean,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val palette = LocalPalette.current
    val themeAccent = LocalAccent.current

    val genresFa = listOf("همه", "اکشن", "درام", "کمدی", "ترسناک", "عاشقانه", "مستند", "انیمیشن")
    val genresEn = listOf("All", "Action", "Drama", "Comedy", "Horror", "Romance", "Documentary", "Animation")

    var selectedGenre by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selectedMovie by remember { mutableStateOf<Movie?>(null) }

    fun tr(fa: String, en: String) = if (english) en else fa

    val allMovies = MoviesRepository.movies
    val filteredMovies = remember(selectedGenre, query) {
        val genreName = genresFa[selectedGenre]
        val base = if (selectedGenre == 0) allMovies
            else allMovies.filter { it.genre == genreName }
        if (query.isBlank()) base
        else base.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.titleEn.contains(query, ignoreCase = true)
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ── Header ──────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = tr("بازگشت","Back"), tint = palette.text)
            }
            Column {
                Text(tr("دانلود فیلم","Download Movies"), style = MaterialTheme.typography.titleMedium,
                    color = palette.text, fontWeight = FontWeight.Bold)
                Text(tr("فیلم‌های جدید با یک لمس","Fresh movies with one tap"),
                    color = palette.muted, fontSize = 11.sp)
            }
        }

        // ── Search bar ────────────────────────────────────────
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null, tint = palette.muted) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                Icon(Icons.Default.Close, null, tint = palette.muted) } },
            label = { Text(tr("جستجوی فیلم","Search movies")) },
            modifier = Modifier.fillMaxWidth().heightIn(max = 56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeAccent, unfocusedBorderColor = palette.glassBorder.copy(alpha = 0.4f),
                focusedContainerColor = palette.card.copy(alpha = 0.5f), unfocusedContainerColor = palette.card.copy(alpha = 0.3f)
            )
        )

        Spacer(Modifier.height(10.dp))

        // ── Genre chips ────────────────────────────────────────
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(genresFa.size) { idx ->
                val active = selectedGenre == idx
                Surface(
                    onClick = { selectedGenre = idx; query = "" },
                    shape = RoundedCornerShape(50),
                    color = if (active) themeAccent else palette.card,
                    border = BorderStroke(1.dp, if (active) themeAccent else palette.glassBorder.copy(alpha = 0.35f))
                ) {
                    Text(
                        if (english) genresEn[idx] else genresFa[idx],
                        color = if (active) Color.White else palette.text,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Movies list ───────────────────────────────────────
        if (filteredMovies.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Movie, null, tint = palette.muted.copy(alpha = 0.4f), modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(tr("فیلمی پیدا نشد","No movies found"), color = palette.muted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredMovies, key = { it.id }) { movie ->
                    MovieCard(
                        movie = movie,
                        english = english,
                        palette = palette,
                        themeAccent = themeAccent,
                        onClick = { selectedMovie = movie },
                        onDownload = { DownloadsManager.startDownload(ctx, movie, english) }
                    )
                }
            }
        }
    }

    // ── Detail Dialog ──────────────────────────────────────
    selectedMovie?.let { movie ->
        Dialog(onDismissRequest = { selectedMovie = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            MovieDetailSheet(
                movie = movie, palette = palette, themeAccent = themeAccent, english = english,
                onDownload = { DownloadsManager.startDownload(ctx, movie, english); selectedMovie = null },
                onClose = { selectedMovie = null }
            )
        }
    }
}

@Composable
private fun MovieCard(
    movie: Movie, english: Boolean, palette: AppPalette, themeAccent: Color,
    onClick: () -> Unit, onDownload: () -> Unit
) {
    val grad = movieGradient(movie.id)
    val textColor = if (grad[0].luminance() > 0.35f) Color.Black else Color.White

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = palette.card,
        modifier = Modifier.fillMaxWidth().animateItem()
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            // Poster
            Box(
                Modifier.size(90.dp, 128.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(grad)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Movie, null, tint = textColor.copy(alpha = 0.8f), modifier = Modifier.size(32.dp))

                if (movie.badge != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
                    ) {
                        Text(movie.badge, color = Color(0xFFFFD54F), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp)
                ) {
                    Row(Modifier.padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(String.format("%.1f", movie.rating), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(movie.title, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(movie.titleEn, color = palette.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(movie.year, movie.duration, movie.quality, movie.size).forEach { tag ->
                        SmallTag(tag, palette)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(movie.description, color = palette.muted, fontSize = 11.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                onClick = onDownload, shape = RoundedCornerShape(50),
                color = themeAccent, modifier = Modifier.size(42.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.FileDownload, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SmallTag(text: String, palette: AppPalette) {
    Surface(color = palette.glassTop.copy(alpha = 0.35f), shape = RoundedCornerShape(5.dp)) {
        Text(text, color = palette.text.copy(alpha = 0.85f), fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
    }
}

@Composable
private fun MovieDetailSheet(
    movie: Movie, palette: AppPalette, themeAccent: Color, english: Boolean,
    onDownload: () -> Unit, onClose: () -> Unit
) {
    val grad = movieGradient(movie.id)
    val textColor = if (grad[0].luminance() > 0.35f) Color.Black else Color.White
    fun tr(fa: String, en: String) = if (english) en else fa

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.card,
        border = BorderStroke(1.dp, palette.glassBorder.copy(alpha = 0.5f)),
        modifier = Modifier.widthIn(max = 400.dp).padding(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(65.dp, 95.dp).clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(grad)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Movie, null, tint = textColor.copy(alpha = 0.85f), modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(movie.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(movie.titleEn, color = palette.muted, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(movie.year, movie.quality, movie.size).forEach { SmallTag(it, palette) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(String.format("%.1f", movie.rating), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        Text(movie.genre, color = themeAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = palette.muted) }
            }
            Spacer(Modifier.height(14.dp))
            Text(movie.description, color = palette.muted, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                tr("حجم: ","Size: ") + movie.size + "  ·  " + tr("مدت: ","Duration: ") + movie.duration,
                color = palette.muted, fontSize = 12.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onDownload,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tr("دانلود فیلم","Download"), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(tr("بستن","Close")) }
            }
        }
    }
}
