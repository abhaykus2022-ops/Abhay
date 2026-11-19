package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.YouTubeVideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "YouTubeLinkResolver"

class YouTubeLinkResolver(private val context: Context) {

    fun extractVideoId(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        val directIdRegex = Regex("^[A-Za-z0-9_-]{11}$")
        if (directIdRegex.matches(trimmed)) return trimmed

        val parsedUri = runCatching {
            val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "https://$trimmed"
            }
            Uri.parse(normalized)
        }.getOrNull() ?: return null

        val host = parsedUri.host?.lowercase().orEmpty()
        if (!host.contains("youtube.com") && !host.contains("youtu.be") && !host.contains("youtube-nocookie.com")) {
            return null
        }

        if (host.contains("youtu.be")) {
            val candidate = parsedUri.pathSegments.firstOrNull()
            if (candidate != null && directIdRegex.matches(candidate)) return candidate
        }

        val queryV = parsedUri.getQueryParameter("v")
        if (queryV != null && directIdRegex.matches(queryV)) return queryV

        val segments = parsedUri.pathSegments
        for (i in segments.indices) {
            val seg = segments[i].lowercase()
            if ((seg == "shorts" || seg == "live" || seg == "embed" || seg == "v") && i + 1 < segments.size) {
                val id = segments[i + 1]
                if (directIdRegex.matches(id)) return id
            }
        }
        return null
    }

    suspend fun resolveYouTubeVideo(rawUrl: String): YouTubeVideoInfo = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(rawUrl)
            ?: throw IllegalArgumentException("Please paste a valid YouTube video, Shorts, or live link.")

        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        val oEmbedUrl = "https://www.youtube.com/oembed?url=$watchUrl&format=json"

        var title = "YouTube Video ($videoId)"
        var author = "YouTube Creator"

        runCatching {
            val jsonStr = fetchText(oEmbedUrl)
            val json = JSONObject(jsonStr)
            title = json.optString("title", title).ifBlank { title }
            author = json.optString("author_name", author).ifBlank { author }
        }.onFailure {
            Log.w(TAG, "oEmbed lookup fallback for $videoId", it)
        }

        var durationMs = 180_000L
        var descriptionSnippet = ""

        runCatching {
            val html = fetchText(watchUrl)
            val lengthMatch = Regex("\"lengthSeconds\"\\s*:\\s*\"(\\d+)\"").find(html)
            val lengthSec = lengthMatch?.groupValues?.getOrNull(1)?.toLongOrNull()
            if (lengthSec != null && lengthSec > 0L) {
                durationMs = lengthSec * 1000L
            }
            val descMatch = Regex("\"shortDescription\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(html)
            val rawDesc = descMatch?.groupValues?.getOrNull(1).orEmpty()
            descriptionSnippet = rawDesc
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .take(1200)
        }.onFailure {
            Log.w(TAG, "Watch page metadata extraction fallback for $videoId", it)
        }

        val thumbBitmap = downloadBitmap("https://i.ytimg.com/vi/$videoId/hqdefault.jpg")
            ?: downloadBitmap("https://i.ytimg.com/vi/$videoId/0.jpg")

        val thumbPath = thumbBitmap?.let { bmp ->
            val outDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val file = File(outDir, "yt_${videoId}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { stream ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 88, stream)
            }
            bmp.recycle()
            file.absolutePath
        }

        YouTubeVideoInfo(
            videoId = videoId,
            watchUrl = watchUrl,
            title = title,
            channelAuthor = author,
            durationMs = durationMs.coerceAtLeast(5_000L),
            thumbnailPath = thumbPath,
            descriptionSnippet = descriptionSnippet
        )
    }

    suspend fun sampleYouTubeStoryboardFrames(
        videoId: String,
        durationMs: Long
    ): List<SampledFrame> = withContext(Dispatchers.IO) {
        val frameNames = listOf("0.jpg", "1.jpg", "2.jpg", "3.jpg")
        val safeDuration = durationMs.coerceAtLeast(4_000L)
        val sampled = mutableListOf<SampledFrame>()

        frameNames.forEachIndexed { idx, fileName ->
            val url = "https://i.ytimg.com/vi/$videoId/$fileName"
            val bmp = downloadBitmap(url)
            if (bmp != null) {
                val ts = when (idx) {
                    0 -> 0L
                    1 -> (safeDuration * 0.25).toLong()
                    2 -> (safeDuration * 0.55).toLong()
                    else -> (safeDuration * 0.82).toLong()
                }
                sampled.add(SampledFrame(timestampMs = ts, bitmap = bmp))
            }
        }
        sampled
    }

    private fun fetchText(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android 14; Mobile) AppleWebKit/537.36")
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        }
        return try {
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${conn.responseCode} fetching YouTube info")
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadBitmap(urlString: String): Bitmap? {
        return runCatching {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
            }
            try {
                if (conn.responseCode !in 200..299) return null
                conn.inputStream.use { BitmapFactory.decodeStream(it) }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }
}
