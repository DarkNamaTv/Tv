package com.darktv.player

import android.content.Context
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.content.Intent
import android.widget.Toast

/**
 * Delegates downloads to Android's DownloadManager so the movie is saved
 * reliably (even if the app closes) and appears in the Downloads folder.
 */
object DownloadsManager {
    fun startDownload(context: Context, movie: Movie, english: Boolean) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return
        val request = DownloadManager.Request(Uri.parse(movie.downloadUrl))
            .setTitle(movie.title)
            .setDescription(movie.titleEn)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "XioTv/${movie.title}.mp4")
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
        movie.downloadHeaders.forEach { (k, v) -> request.addRequestHeader(k, v) }
        try {
            dm.enqueue(request)
            val msg = if (english) "Download started" else "دانلود شروع شد"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context,
                if (english) "Storage permission needed" else "نیاز به مجوز ذخیره‌سازی",
                Toast.LENGTH_LONG).show()
        }
    }
}
