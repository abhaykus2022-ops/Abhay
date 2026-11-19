package com.example.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.data.ExportResolution
import com.example.data.HighlightClipEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ExportCompletedFile(
    val outputUri: Uri,
    val fileSizeBytes: Long
)

class VideoExportManager(private val context: Context) {

    @OptIn(UnstableApi::class)
    suspend fun exportClipToHdMp4(
        sourceUri: Uri,
        sourceWidth: Int,
        sourceHeight: Int,
        clip: HighlightClipEntity,
        resolution: ExportResolution,
        projectTitle: String,
        onProgress: (Float) -> Unit
    ): ExportCompletedFile = withContext(Dispatchers.Main) {
        val tempDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = (projectTitle + "_" + clip.label)
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .take(40)
            .ifBlank { "SlateCut" }
        val tempFile = File(tempDir, "${safeName}_${System.currentTimeMillis()}.mp4")

        val videoEffects = VfxPipelineBuilder.buildExportEffects(
            clip = clip,
            resolution = resolution,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight
        )

        val clippedMediaItem = MediaItem.Builder()
            .setUri(sourceUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.startMs.coerceAtLeast(0L))
                    .setEndPositionMs(clip.endMs.coerceAtLeast(clip.startMs + 500L))
                    .build()
            )
            .build()

        val editedBuilder = EditedMediaItem.Builder(clippedMediaItem)
            .setEffects(Effects(emptyList(), videoEffects))

        if (kotlin.math.abs(clip.speedMultiplier - 1.0f) > 0.02f) {
            val targetSpeed = clip.speedMultiplier.coerceIn(0.5f, 2.0f)
            editedBuilder.setSpeed(object : SpeedProvider {
                override fun getSpeed(presentationTimeUs: Long): Float = targetSpeed
                override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
            })
        }

        val editedMediaItem = editedBuilder.build()

        runTransformer(editedMediaItem, tempFile.absolutePath, onProgress)

        val published = withContext(Dispatchers.IO) {
            publishToGalleryOrStorage(tempFile, "${safeName}_${System.currentTimeMillis()}.mp4")
        }
        runCatching { tempFile.delete() }
        published
    }

    @OptIn(UnstableApi::class)
    private suspend fun runTransformer(
        editedMediaItem: EditedMediaItem,
        outputPath: String,
        onProgress: (Float) -> Unit
    ) {
        var activeTransformer: Transformer? = null
        try {
            suspendCancellableCoroutine<Unit> { cont ->
                val transformer = Transformer.Builder(context)
                    .setLooper(Looper.getMainLooper())
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(
                            composition: Composition,
                            exportResult: ExportResult
                        ) {
                            onProgress(1f)
                            if (cont.isActive) cont.resume(Unit)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            if (cont.isActive) {
                                cont.resumeWithException(exportException)
                            }
                        }
                    })
                    .build()

                activeTransformer = transformer
                transformer.start(editedMediaItem, outputPath)

                val handler = Handler(Looper.getMainLooper())
                val progressHolder = ProgressHolder()
                val poller = object : Runnable {
                    override fun run() {
                        if (!cont.isActive) return
                        val state = transformer.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            onProgress((progressHolder.progress / 100f).coerceIn(0f, 0.99f))
                        }
                        handler.postDelayed(this, 180L)
                    }
                }
                handler.post(poller)

                cont.invokeOnCancellation {
                    handler.removeCallbacks(poller)
                    transformer.cancel()
                }
            }
        } catch (t: Throwable) {
            activeTransformer?.cancel()
            throw t
        }
    }

    private fun publishToGalleryOrStorage(tempFile: File, displayName: String): ExportCompletedFile {
        val fileSize = tempFile.length()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/SlateCutAI")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Unable to create MediaStore entry for export.")
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(tempFile).use { input ->
                    input.copyTo(out)
                }
            } ?: throw IllegalStateException("Unable to open MediaStore output stream.")
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return ExportCompletedFile(outputUri = uri, fileSizeBytes = fileSize)
        } else {
            val moviesDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "SlateCutAI"
            ).apply { mkdirs() }
            val destFile = File(moviesDir, displayName)
            tempFile.copyTo(destFile, overwrite = true)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )
            return ExportCompletedFile(outputUri = uri, fileSizeBytes = destFile.length())
        }
    }
}
