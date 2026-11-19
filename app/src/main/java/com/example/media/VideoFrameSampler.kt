package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.VideoMetadataInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class SampledFrame(
    val timestampMs: Long,
    val bitmap: Bitmap
)

class VideoFrameSampler(private val context: Context) {

    suspend fun extractMetadataAndThumbnail(uri: Uri): VideoMetadataInfo = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(1_000L) ?: 10_000L
            val rawW = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 1920
            val rawH = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 1080
            val rotation = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0

            val (width, height) = if (rotation == 90 || rotation == 270) {
                rawH to rawW
            } else {
                rawW to rawH
            }

            val thumbBitmap = retriever.getFrameAtTime(
                (durationMs / 4L) * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: retriever.getFrameAtTime(0L)

            val thumbPath = thumbBitmap?.let { bmp ->
                val scaled = scaleBitmapMaxDim(bmp, 640)
                val outDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
                val file = File(outDir, "thumb_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { stream ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                }
                if (scaled !== bmp) scaled.recycle()
                bmp.recycle()
                file.absolutePath
            }

            val displayName = resolveDisplayName(uri)
                ?: "Studio Cut ${(System.currentTimeMillis() % 1000)}"

            VideoMetadataInfo(
                durationMs = durationMs,
                width = width,
                height = height,
                title = displayName.substringBeforeLast('.').ifBlank { "Untitled Cut" },
                thumbnailPath = thumbPath
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun sampleFramesForAi(
        uri: Uri,
        durationMs: Long,
        frameCount: Int = 8
    ): List<SampledFrame> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<SampledFrame>()
        try {
            retriever.setDataSource(context, uri)
            val safeDuration = durationMs.coerceAtLeast(1_000L)
            val count = frameCount.coerceIn(3, 10)
            for (i in 0 until count) {
                val fraction = (i + 0.5) / count.toDouble()
                val tsMs = (safeDuration * fraction).toLong().coerceIn(0L, safeDuration - 100L)
                val raw = retriever.getFrameAtTime(
                    tsMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (raw != null) {
                    val scaled = scaleBitmapMaxDim(raw, 480)
                    if (scaled !== raw) raw.recycle()
                    frames.add(SampledFrame(timestampMs = tsMs, bitmap = scaled))
                }
            }
        } finally {
            runCatching { retriever.release() }
        }
        frames
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull()
    }

    private fun scaleBitmapMaxDim(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDim && h <= maxDim) return source
        val ratio = if (w >= h) maxDim.toFloat() / w else maxDim.toFloat() / h
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }
}
