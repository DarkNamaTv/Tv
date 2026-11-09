package com.darktv.player

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream

/** The remote catalogue is additive: the built-in list remains available offline. */
object ApiCatalogue {
    private const val endpoint = "https://api.iptvdark.workers.dev/channels.json"
    private const val cacheName = "iptvdark-channels.json"
    private const val maxBytes = 5 * 1024 * 1024
    private val client = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()

    fun loadCached(context: Context, base: List<Channel>): List<Channel> = try {
        val file = context.getFileStreamPath(cacheName)
        if (!file.exists() || file.length() > maxBytes) base else merge(base, file.readText())
    } catch (e: Exception) {
        Log.w("ApiCatalogue", "Ignoring invalid cached catalogue", e)
        base
    }

    // Call on Dispatchers.IO. Do not replace the last good cache with an invalid response.
    fun refresh(context: Context, base: List<Channel>): List<Channel> {
        val request = Request.Builder().url(endpoint).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Catalogue HTTP ${response.code}" }
            val body = checkNotNull(response.body)
            check(body.contentLength() <= maxBytes) { "Catalogue too large" }
            val bytes = body.byteStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    check(output.size() + count <= maxBytes) { "Catalogue too large" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val text = bytes.toString(Charsets.UTF_8)
            val merged = merge(base, text)
            val temp = context.getFileStreamPath("$cacheName.tmp")
            temp.writeText(text)
            check(temp.renameTo(context.getFileStreamPath(cacheName))) { "Could not save catalogue" }
            return merged
        }
    }

    private fun value(json: JSONObject, key: String): String? =
        if (json.isNull(key)) null else json.optString(key).trim().takeIf { it.isNotEmpty() && '\n' !in it && '\r' !in it }

    private fun source(json: JSONObject, title: String? = null): StreamSource? {
        val url = value(json, "url") ?: return null
        if (url.toHttpUrlOrNull() == null) return null
        val headers = mutableMapOf<String, String>()
        value(json, "referer")?.let { headers["Referer"] = it }
        value(json, "origin")?.let { headers["Origin"] = it }
        value(json, "agent")?.let { headers["User-Agent"] = it }
        return StreamSource(url, title, headers)
    }

    private fun category(value: String): String = when (value.lowercase().trim()) {
        "national", "general", "local", "regional" -> "general"
        "news" -> "news"
        "movies & series", "movies", "series", "gem tv networks" -> "movies"
        "music" -> "music"
        "entertainment" -> "entertainment"
        "kids", "children" -> "kids"
        "sports", "sport" -> "sports"
        "documentary" -> "documentary"
        "lifestyle", "family" -> "lifestyle"
        "religious" -> "religious"
        "education" -> "education"
        else -> "other"
    }

    private fun key(name: String) = name.lowercase().filter { it.isLetterOrDigit() || it == '+' }

    /** Prefer API servers, then retain all existing URLs and metadata. */
    fun merge(base: List<Channel>, text: String): List<Channel> {
        val entries = JSONObject(text).getJSONArray("channels")
        require(entries.length() in 1..5000) { "Empty or oversized catalogue" }
        val result = base.toMutableList()
        var accepted = 0
        for (i in 0 until entries.length()) {
            val item = entries.optJSONObject(i) ?: continue
            val id = item.optInt("id", -1)
            if (id <= 0) continue
            val nameFa = value(item, "name")
            val nameEn = value(item, "nameEn")
            val name = nameEn ?: nameFa ?: continue
            val streams = mutableListOf<StreamSource>()
            source(item)?.let { streams.add(it) }
            val alternatives = item.optJSONArray("sources")
            if (alternatives != null) for (j in 0 until alternatives.length()) {
                val server = alternatives.optJSONObject(j) ?: continue
                source(server, value(server, "title"))?.let { streams.add(it) }
            }
            val distinct = streams.distinctBy { it.url }
            if (distinct.isEmpty()) continue
            accepted++
            val streamUrls = distinct.map { it.url }.toSet()
            // Prefer an exact stream match; only then match a whole normalized name.
            val byUrl = result.indexOfFirst { ch -> ch.sources().any { it in streamUrls } }
            val byName = if (byUrl < 0) result.indexOfFirst { key(it.codeName) == key(name) } else -1
            val existing = if (byUrl >= 0) byUrl else byName
            val logo = value(item, "image")?.takeIf { it.toHttpUrlOrNull() != null }
            val categoryKey = category(value(item, "categoryEn") ?: "")
            if (existing >= 0) {
                val old = result[existing]
                result[existing] = old.copy(
                    logoUrl = logo ?: old.logoUrl,
                    categoryKeys = (old.categories() + categoryKey).distinct(),
                    preferredSources = (old.preferredSources + distinct).distinctBy { it.url }
                )
            } else {
                // API ids are positive; negative ids never collide with packaged channels.
                val apiId = -id
                if (result.any { it.id == apiId }) continue
                result.add(Channel(apiId, name, nameFa ?: name, "تلویزیون", distinct.first().url,
                    "", Color.Transparent, Color.Transparent, logo, emptyList(), listOf(categoryKey), distinct, favoriteKey = "api:$id"))
            }
        }
        require(accepted > 0) { "Catalogue contains no playable entries" }
        return result
    }
}
