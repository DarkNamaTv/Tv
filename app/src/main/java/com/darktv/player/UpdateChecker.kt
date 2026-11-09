package com.darktv.player

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class UpdateStatus {
    object Checking : UpdateStatus()
    object NoRelease : UpdateStatus()
    object UpToDate : UpdateStatus()
    object Unavailable : UpdateStatus()
    data class Available(val version: String, val url: String) : UpdateStatus()
}

object UpdateChecker {
    const val REPOSITORY_URL = "https://github.com/DarkNamaTv/Tv"
    private const val API_URL = "https://api.github.com/repos/DarkNamaTv/Tv/releases/latest"
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()

    /** Only compare stable numeric version tags; unknown tag formats are not assumed newer. */
    fun isNewer(tag: String, current: String): Boolean {
        fun parse(value: String): List<Int>? {
            val clean = value.trim().removePrefix("v").removePrefix("V")
            if (!clean.matches(Regex("\\d+(\\.\\d+){0,3}"))) return null
            return clean.split('.').map { it.toIntOrNull() ?: return null }
        }
        val latest = parse(tag) ?: return false
        val installed = parse(current) ?: return false
        for (i in 0 until maxOf(latest.size, installed.size)) {
            val l = latest.getOrElse(i) { 0 }; val r = installed.getOrElse(i) { 0 }
            if (l != r) return l > r
        }
        return false
    }

    fun check(currentVersion: String): UpdateStatus = try {
        val request = Request.Builder().url(API_URL).header("Accept", "application/vnd.github+json")
            .header("User-Agent", "DarkTV-Android").build()
        client.newCall(request).execute().use { response ->
            when {
                response.code == 404 -> UpdateStatus.NoRelease
                !response.isSuccessful -> UpdateStatus.Unavailable
                else -> {
                    val json = JSONObject(response.body?.string() ?: "")
                    val version = json.optString("tag_name")
                    val html = json.optString("html_url")
                    if (json.optBoolean("draft") || json.optBoolean("prerelease")) UpdateStatus.Unavailable
                    else if (isNewer(version, currentVersion)) {
                        val safeUrl = if (html.startsWith("$REPOSITORY_URL/releases/tag/")) html else "$REPOSITORY_URL/releases"
                        UpdateStatus.Available(version, safeUrl)
                    } else if (!version.trim().removePrefix("v").removePrefix("V").matches(Regex("\\d+(\\.\\d+){0,3}"))) UpdateStatus.Unavailable
                    else UpdateStatus.UpToDate
                }
            }
        }
    } catch (_: Exception) { UpdateStatus.Unavailable }
}
