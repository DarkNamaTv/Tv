package com.darktv.player

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class ApiCatalogueTest {
    private fun old(id: Int, name: String, url: String) = Channel(
        id, name, name, "فیلم و سریال", url, "", Color.Transparent, Color.Transparent
    )

    @Test fun mergesByStreamAndKeepsHeadersAndLegacyFallback() {
        val base = listOf(old(6, "GEM Series", "http://46.245.122.7:8080/hls/live1/stream.m3u8"))
        val json = """{"channels":[{"id":89,"name":"جم سریس","nameEn":"Gem Series",
            "image":"https://example.com/logo.png", "categoryEn":"GEM TV Networks",
            "url":"https://example.com/main.m3u8","referer":"https://example.com/","origin":"https://example.com",
            "sources":[{"title":"Iran","url":"http://46.245.122.7:8080/hls/live1/stream.m3u8","referer":"https://mytvsat.tv"},
                       {"title":"Extra","url":"https://example.com/extra.m3u8","referer":"https://other.example/"}]}]}"""
        val merged = ApiCatalogue.merge(base, json)
        assertEquals(1, merged.size)
        val sources = merged[0].streamSources()
        assertEquals(4, sources.size) // three API URLs and the old port fallback
        assertEquals("https://example.com/main.m3u8", sources[0].url)
        assertEquals("https://example.com/", sources[0].headers["Referer"])
        assertEquals("https://other.example/", sources[2].headers["Referer"])
        assertEquals("Extra", sources[2].title)
        assertEquals("https://example.com/logo.png", merged[0].logoUrl)
    }

    @Test fun doesNotConfusePlusWithNormalAndRejectsBadUrls() {
        val base = listOf(old(6, "GEM Series", "https://legacy.example/live.m3u8"))
        val json = """{"channels":[{"id":90,"name":"Plus","nameEn":"GEM Series+",
            "categoryEn":"Music","url":"https://example.com/plus.m3u8",
            "sources":[{"url":"file:///etc/passwd"},{"url":"https://example.com/plus.m3u8"}]}]}"""
        val merged = ApiCatalogue.merge(base, json)
        assertEquals(2, merged.size)
        assertEquals(-90, merged[1].id)
        assertEquals(listOf("music"), merged[1].categories())
        assertEquals(1, merged[1].sources().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyRemoteCatalogue() {
        ApiCatalogue.merge(emptyList(), """{"channels":[]}""")
    }
    @Test fun favoriteOrderKeepsOtherChannelsStable() {
        val channels = listOf(old(1, "One", "https://example.com/one"), old(2, "Two", "https://example.com/two"), old(3, "Three", "https://example.com/three"))
        assertEquals(listOf(2, 1, 3), orderFavorites(channels, setOf(channels[1].favoriteKey)).map { it.id })
        assertEquals(listOf(1, 2, 3), orderFavorites(channels, emptySet()).map { it.id })
    }

}
