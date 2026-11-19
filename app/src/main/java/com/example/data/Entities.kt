package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "video_projects")
data class VideoProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUri: String,
    val sourceType: String = ProjectSourceType.LOCAL_VIDEO.id,
    val youtubeVideoId: String? = null,
    val youtubeUrl: String? = null,
    val channelAuthor: String? = null,
    val descriptionSnippet: String? = null,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val aspectRatioId: String = AspectRatioPreset.SHORTS_9_16.id,
    val thumbnailPath: String? = null,
    val aiSummary: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "highlight_clips",
    foreignKeys = [
        ForeignKey(
            entity = VideoProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class HighlightClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val label: String,
    val reason: String,
    val category: String = "Highlight",
    val viralScore: Int = 85,
    val startMs: Long,
    val endMs: Long,
    val isAiGenerated: Boolean = false,
    val vfxPresetId: String = VfxPreset.GLASS_PRISM.id,
    val brightness: Float = VfxPreset.GLASS_PRISM.defaultBrightness,
    val contrast: Float = VfxPreset.GLASS_PRISM.defaultContrast,
    val saturation: Float = VfxPreset.GLASS_PRISM.defaultSaturation,
    val zoomIntensity: Float = VfxPreset.GLASS_PRISM.defaultZoom,
    val glassIntensity: Float = VfxPreset.GLASS_PRISM.defaultGlass,
    val speedMultiplier: Float = 1.0f,
    val overlayText: String = "",
    val aspectRatioId: String = AspectRatioPreset.SHORTS_9_16.id,
    val createdAt: Long = System.currentTimeMillis()
) {
    val clipDurationMs: Long
        get() = (endMs - startMs).coerceAtLeast(500L)
}

@Entity(tableName = "export_records")
data class ExportRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val projectTitle: String,
    val clipLabel: String,
    val outputUri: String,
    val resolutionLabel: String,
    val aspectRatioLabel: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val exportedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long? = null,
    val role: String,
    val content: String,
    val usedHighThinking: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
