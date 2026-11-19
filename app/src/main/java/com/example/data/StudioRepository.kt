package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Locale

class StudioRepository(private val db: AppDatabase) {
    private val projectDao = db.projectDao()
    private val clipDao = db.highlightClipDao()
    private val exportDao = db.exportRecordDao()
    private val chatDao = db.chatMessageDao()

    val allProjects: Flow<List<VideoProjectEntity>> = projectDao.observeAllProjects()
    val allExports: Flow<List<ExportRecordEntity>> = exportDao.observeAllExports()
    val chatMessages: Flow<List<ChatMessageEntity>> = chatDao.observeAllMessages()

    fun observeProject(projectId: Long): Flow<VideoProjectEntity?> =
        projectDao.observeProject(projectId)

    fun observeClipsForProject(projectId: Long): Flow<List<HighlightClipEntity>> =
        clipDao.observeClipsForProject(projectId)

    suspend fun getProject(projectId: Long): VideoProjectEntity? =
        projectDao.getProject(projectId)

    suspend fun getClipsForProject(projectId: Long): List<HighlightClipEntity> =
        clipDao.getClipsForProject(projectId)

    suspend fun createProjectWithInitialClip(info: VideoMetadataInfo, sourceUri: String): Long {
        val now = System.currentTimeMillis()
        val projectId = projectDao.insertProject(
            VideoProjectEntity(
                title = info.title,
                sourceUri = sourceUri,
                sourceType = ProjectSourceType.LOCAL_VIDEO.id,
                durationMs = info.durationMs,
                width = info.width,
                height = info.height,
                aspectRatioId = AspectRatioPreset.SHORTS_9_16.id,
                thumbnailPath = info.thumbnailPath,
                createdAt = now,
                updatedAt = now
            )
        )
        val initialEnd = info.durationMs.coerceAtMost(30_000L).coerceAtLeast(1_000L)
        clipDao.insertClip(
            HighlightClipEntity(
                projectId = projectId,
                label = "Frosted Glass Cut",
                reason = "Ready for 9:16 YouTube Shorts editing or AI highlight scan",
                category = "Starter Cut",
                viralScore = 82,
                startMs = 0L,
                endMs = initialEnd.coerceAtMost(info.durationMs.coerceAtLeast(1_000L)),
                isAiGenerated = false,
                vfxPresetId = VfxPreset.GLASS_PRISM.id,
                brightness = VfxPreset.GLASS_PRISM.defaultBrightness,
                contrast = VfxPreset.GLASS_PRISM.defaultContrast,
                saturation = VfxPreset.GLASS_PRISM.defaultSaturation,
                zoomIntensity = VfxPreset.GLASS_PRISM.defaultZoom,
                glassIntensity = VfxPreset.GLASS_PRISM.defaultGlass,
                aspectRatioId = AspectRatioPreset.SHORTS_9_16.id
            )
        )
        return projectId
    }

    suspend fun createYouTubeProjectWithInitialClips(
        info: YouTubeVideoInfo,
        aiSummary: String?,
        suggestions: List<HighlightSuggestion>
    ): Long {
        val now = System.currentTimeMillis()
        val projectId = projectDao.insertProject(
            VideoProjectEntity(
                title = info.title,
                sourceUri = "",
                sourceType = ProjectSourceType.YOUTUBE_LINK.id,
                youtubeVideoId = info.videoId,
                youtubeUrl = info.watchUrl,
                channelAuthor = info.channelAuthor,
                descriptionSnippet = info.descriptionSnippet,
                durationMs = info.durationMs,
                width = 1920,
                height = 1080,
                aspectRatioId = AspectRatioPreset.SHORTS_9_16.id,
                thumbnailPath = info.thumbnailPath,
                aiSummary = aiSummary,
                createdAt = now,
                updatedAt = now
            )
        )
        if (suggestions.isNotEmpty()) {
            val entities = suggestions.map { s ->
                HighlightClipEntity(
                    projectId = projectId,
                    label = s.label,
                    reason = s.reason,
                    category = s.category,
                    viralScore = s.viralScore,
                    startMs = s.startMs,
                    endMs = s.endMs,
                    isAiGenerated = true,
                    vfxPresetId = s.recommendedVfx.id,
                    brightness = s.recommendedVfx.defaultBrightness,
                    contrast = s.recommendedVfx.defaultContrast,
                    saturation = s.recommendedVfx.defaultSaturation,
                    zoomIntensity = s.recommendedVfx.defaultZoom,
                    glassIntensity = s.recommendedVfx.defaultGlass,
                    overlayText = s.hookCaption,
                    aspectRatioId = AspectRatioPreset.SHORTS_9_16.id
                )
            }
            clipDao.insertClips(entities)
        } else {
            val initialEnd = info.durationMs.coerceAtMost(30_000L).coerceAtLeast(1_000L)
            clipDao.insertClip(
                HighlightClipEntity(
                    projectId = projectId,
                    label = "YouTube Shorts Hook",
                    reason = "Ready to trim and style with Frosted Glass VFX",
                    category = "YouTube Cut",
                    viralScore = 84,
                    startMs = 0L,
                    endMs = initialEnd,
                    isAiGenerated = false,
                    vfxPresetId = VfxPreset.GLASS_PRISM.id,
                    brightness = VfxPreset.GLASS_PRISM.defaultBrightness,
                    contrast = VfxPreset.GLASS_PRISM.defaultContrast,
                    saturation = VfxPreset.GLASS_PRISM.defaultSaturation,
                    zoomIntensity = VfxPreset.GLASS_PRISM.defaultZoom,
                    glassIntensity = VfxPreset.GLASS_PRISM.defaultGlass,
                    aspectRatioId = AspectRatioPreset.SHORTS_9_16.id
                )
            )
        }
        return projectId
    }

    suspend fun attachLocalVideoToProject(
        project: VideoProjectEntity,
        info: VideoMetadataInfo,
        sourceUri: String
    ) {
        projectDao.updateProject(
            project.copy(
                sourceUri = sourceUri,
                durationMs = info.durationMs.coerceAtLeast(project.durationMs.coerceAtMost(info.durationMs)),
                width = info.width,
                height = info.height,
                thumbnailPath = info.thumbnailPath ?: project.thumbnailPath,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateProject(project: VideoProjectEntity) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(projectId: Long) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun addClip(clip: HighlightClipEntity): Long = clipDao.insertClip(clip)

    suspend fun addClips(clips: List<HighlightClipEntity>): List<Long> = clipDao.insertClips(clips)

    suspend fun updateClip(clip: HighlightClipEntity) = clipDao.updateClip(clip)

    suspend fun deleteClip(clipId: Long) = clipDao.deleteClipById(clipId)

    suspend fun recordExport(record: ExportRecordEntity): Long = exportDao.insertExport(record)

    suspend fun deleteExport(id: Long) = exportDao.deleteExportById(id)

    suspend fun getChatHistory(): List<ChatMessageEntity> = chatDao.getAllMessages()

    suspend fun addChatMessage(message: ChatMessageEntity): Long = chatDao.insertMessage(message)

    suspend fun clearChatHistory() = chatDao.clearAllMessages()
}

fun formatTimecode(ms: Long, includeTenths: Boolean = true): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val tenths = (safeMs % 1000L) / 100L
    return if (includeTenths) {
        String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        val kb = bytes / 1024.0
        String.format(Locale.US, "%.0f KB", kb)
    }
}
