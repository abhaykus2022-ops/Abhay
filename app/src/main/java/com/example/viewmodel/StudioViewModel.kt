package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StudioContainer
import com.example.data.AspectRatioPreset
import com.example.data.ExportRecordEntity
import com.example.data.ExportResolution
import com.example.data.HighlightClipEntity
import com.example.data.ProjectSourceType
import com.example.data.VideoProjectEntity
import com.example.data.VfxPreset
import com.example.data.YouTubeVideoInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "StudioViewModel"

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object SamplingFrames : AnalysisUiState
    data object AnalyzingWithAi : AnalysisUiState
    data class Success(val generatedCount: Int, val summary: String) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Exporting(val progress: Float) : ExportUiState
    data class Success(val record: ExportRecordEntity) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

sealed interface HookUiState {
    data object Idle : HookUiState
    data object Generating : HookUiState
    data class Error(val message: String) : HookUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val container = StudioContainer.get(application)
    private val repository = container.repository
    private val frameSampler = container.frameSampler
    private val youtubeResolver = container.youtubeResolver
    private val exportManager = container.exportManager
    private val geminiService = container.geminiService

    private val _activeProjectId = MutableStateFlow<Long?>(null)
    private val _selectedClipId = MutableStateFlow<Long?>(null)

    val project: StateFlow<VideoProjectEntity?> = _activeProjectId
        .flatMapLatest { id ->
            if (id != null && id > 0L) repository.observeProject(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    val clips: StateFlow<List<HighlightClipEntity>> = _activeProjectId
        .flatMapLatest { id ->
            if (id != null && id > 0L) repository.observeClipsForProject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val selectedClip: StateFlow<HighlightClipEntity?> = combine(clips, _selectedClipId) { list, selectedId ->
        list.firstOrNull { it.id == selectedId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _hookState = MutableStateFlow<HookUiState>(HookUiState.Idle)
    val hookState: StateFlow<HookUiState> = _hookState.asStateFlow()

    fun openProject(projectId: Long?) {
        if (projectId != null && projectId > 0L) {
            if (_activeProjectId.value != projectId) {
                _activeProjectId.value = projectId
                _selectedClipId.value = null
                _analysisState.value = AnalysisUiState.Idle
                _exportState.value = ExportUiState.Idle
            }
        } else if (_activeProjectId.value == null) {
            viewModelScope.launch {
                val latest = repository.allProjects.firstOrNull()?.firstOrNull()
                if (latest != null) {
                    _activeProjectId.value = latest.id
                }
            }
        }
    }

    fun selectClip(clipId: Long) {
        _selectedClipId.value = clipId
    }

    fun attachLocalVideoToCurrentProject(uri: Uri) {
        val currentProject = project.value ?: return
        viewModelScope.launch {
            try {
                runCatching {
                    getApplication<Application>().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val metadata = frameSampler.extractMetadataAndThumbnail(uri)
                repository.attachLocalVideoToProject(currentProject, metadata, uri.toString())
                _exportState.value = ExportUiState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to attach local video", e)
                _exportState.value = ExportUiState.Error(
                    e.localizedMessage ?: "Unable to read the selected local video file."
                )
            }
        }
    }

    fun analyzeWithAi(focusPrompt: String = "") {
        val currentProject = project.value ?: return
        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.SamplingFrames
            try {
                val isYouTubeOnly = currentProject.sourceType == ProjectSourceType.YOUTUBE_LINK.id &&
                    currentProject.sourceUri.isBlank()

                val result = if (isYouTubeOnly && !currentProject.youtubeVideoId.isNullOrBlank()) {
                    val frames = youtubeResolver.sampleYouTubeStoryboardFrames(
                        videoId = currentProject.youtubeVideoId,
                        durationMs = currentProject.durationMs
                    )
                    _analysisState.value = AnalysisUiState.AnalyzingWithAi
                    val ytInfo = YouTubeVideoInfo(
                        videoId = currentProject.youtubeVideoId,
                        watchUrl = currentProject.youtubeUrl
                            ?: "https://www.youtube.com/watch?v=${currentProject.youtubeVideoId}",
                        title = currentProject.title,
                        channelAuthor = currentProject.channelAuthor ?: "YouTube Creator",
                        durationMs = currentProject.durationMs,
                        thumbnailPath = currentProject.thumbnailPath,
                        descriptionSnippet = currentProject.descriptionSnippet.orEmpty()
                    )
                    try {
                        geminiService.analyzeYouTubeVideoForHighlights(
                            info = ytInfo,
                            frames = frames,
                            existingClipCount = clips.value.size,
                            userFocusPrompt = focusPrompt
                        )
                    } finally {
                        frames.forEach { it.bitmap.recycle() }
                    }
                } else {
                    val uri = Uri.parse(currentProject.sourceUri)
                    val frames = frameSampler.sampleFramesForAi(
                        uri = uri,
                        durationMs = currentProject.durationMs,
                        frameCount = 8
                    )
                    if (frames.isEmpty()) {
                        _analysisState.value = AnalysisUiState.Error(
                            "Unable to extract video frames for analysis. Ensure the video file is still accessible."
                        )
                        return@launch
                    }
                    _analysisState.value = AnalysisUiState.AnalyzingWithAi
                    try {
                        geminiService.analyzeVideoForHighlights(
                            frames = frames,
                            durationMs = currentProject.durationMs,
                            existingClipCount = clips.value.size,
                            userFocusPrompt = focusPrompt
                        )
                    } finally {
                        frames.forEach { it.bitmap.recycle() }
                    }
                }

                val currentAspect = selectedClip.value?.aspectRatioId ?: currentProject.aspectRatioId
                val newEntities = result.suggestions.map { suggestion ->
                    HighlightClipEntity(
                        projectId = currentProject.id,
                        label = suggestion.label,
                        reason = suggestion.reason,
                        category = suggestion.category,
                        viralScore = suggestion.viralScore,
                        startMs = suggestion.startMs,
                        endMs = suggestion.endMs,
                        isAiGenerated = true,
                        vfxPresetId = suggestion.recommendedVfx.id,
                        brightness = suggestion.recommendedVfx.defaultBrightness,
                        contrast = suggestion.recommendedVfx.defaultContrast,
                        saturation = suggestion.recommendedVfx.defaultSaturation,
                        zoomIntensity = suggestion.recommendedVfx.defaultZoom,
                        glassIntensity = suggestion.recommendedVfx.defaultGlass,
                        speedMultiplier = 1.0f,
                        overlayText = suggestion.hookCaption,
                        aspectRatioId = currentAspect
                    )
                }

                val insertedIds = repository.addClips(newEntities)
                repository.updateProject(currentProject.copy(aiSummary = result.summary))
                insertedIds.firstOrNull()?.let { _selectedClipId.value = it }
                _analysisState.value = AnalysisUiState.Success(
                    generatedCount = insertedIds.size,
                    summary = result.summary
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI semantic video analysis timed out", e)
                _analysisState.value = AnalysisUiState.Error(
                    "AI analysis timed out after 60 seconds. Check your connection and retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI semantic video analysis failed", e)
                _analysisState.value = AnalysisUiState.Error(
                    e.localizedMessage ?: "AI video analysis failed."
                )
            }
        }
    }

    fun createCustomClip(targetDurationMs: Long? = null) {
        val currentProject = project.value ?: return
        val base = selectedClip.value
        viewModelScope.launch {
            val totalDuration = currentProject.durationMs.coerceAtLeast(1_500L)
            val start = (base?.startMs ?: 0L).coerceIn(0L, (totalDuration - 1_500L).coerceAtLeast(0L))
            val end = if (targetDurationMs != null) {
                (start + targetDurationMs).coerceIn((start + 1_000L).coerceAtMost(totalDuration), totalDuration)
            } else {
                totalDuration
            }
            val clipCount = clips.value.size + 1
            val newClip = HighlightClipEntity(
                projectId = currentProject.id,
                label = if (targetDurationMs == null) "Full Edit #$clipCount" else "Custom Cut #$clipCount",
                reason = "Custom creator timing cut",
                category = "Custom Cut",
                viralScore = 85,
                startMs = start,
                endMs = end,
                isAiGenerated = false,
                vfxPresetId = base?.vfxPresetId ?: VfxPreset.GLASS_PRISM.id,
                brightness = base?.brightness ?: VfxPreset.GLASS_PRISM.defaultBrightness,
                contrast = base?.contrast ?: VfxPreset.GLASS_PRISM.defaultContrast,
                saturation = base?.saturation ?: VfxPreset.GLASS_PRISM.defaultSaturation,
                zoomIntensity = base?.zoomIntensity ?: VfxPreset.GLASS_PRISM.defaultZoom,
                glassIntensity = base?.glassIntensity ?: VfxPreset.GLASS_PRISM.defaultGlass,
                speedMultiplier = base?.speedMultiplier ?: 1.0f,
                overlayText = base?.overlayText ?: "",
                aspectRatioId = base?.aspectRatioId ?: currentProject.aspectRatioId
            )
            val id = repository.addClip(newClip)
            _selectedClipId.value = id
        }
    }

    fun deleteClip(clipId: Long) {
        if (clips.value.size <= 1) return
        viewModelScope.launch {
            repository.deleteClip(clipId)
            if (_selectedClipId.value == clipId) {
                _selectedClipId.value = null
            }
        }
    }

    fun updateTrimRange(startMs: Long, endMs: Long) {
        val currentProject = project.value ?: return
        val clip = selectedClip.value ?: return
        val maxDuration = currentProject.durationMs.coerceAtLeast(1_000L)
        val safeStart = startMs.coerceIn(0L, (maxDuration - 500L).coerceAtLeast(0L))
        val safeEnd = endMs.coerceIn((safeStart + 500L).coerceAtMost(maxDuration), maxDuration)
        updateClip(clip.copy(startMs = safeStart, endMs = safeEnd))
    }

    fun applyDurationPreset(durationMs: Long?) {
        val currentProject = project.value ?: return
        val clip = selectedClip.value ?: return
        val maxDuration = currentProject.durationMs.coerceAtLeast(1_000L)
        if (durationMs == null) {
            updateClip(clip.copy(startMs = 0L, endMs = maxDuration))
        } else {
            val safeStart = clip.startMs.coerceIn(0L, (maxDuration - 1_000L).coerceAtLeast(0L))
            val safeEnd = (safeStart + durationMs).coerceAtMost(maxDuration)
            updateClip(clip.copy(startMs = safeStart, endMs = safeEnd))
        }
    }

    fun applyVfxPreset(preset: VfxPreset) {
        val clip = selectedClip.value ?: return
        updateClip(
            clip.copy(
                vfxPresetId = preset.id,
                brightness = preset.defaultBrightness,
                contrast = preset.defaultContrast,
                saturation = preset.defaultSaturation,
                zoomIntensity = preset.defaultZoom,
                glassIntensity = preset.defaultGlass
            )
        )
    }

    fun updateColorAndMotion(
        brightness: Float = selectedClip.value?.brightness ?: 0f,
        contrast: Float = selectedClip.value?.contrast ?: 0f,
        saturation: Float = selectedClip.value?.saturation ?: 0f,
        zoomIntensity: Float = selectedClip.value?.zoomIntensity ?: 0f,
        glassIntensity: Float = selectedClip.value?.glassIntensity ?: 0f,
        speedMultiplier: Float = selectedClip.value?.speedMultiplier ?: 1f
    ) {
        val clip = selectedClip.value ?: return
        updateClip(
            clip.copy(
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                zoomIntensity = zoomIntensity,
                glassIntensity = glassIntensity,
                speedMultiplier = speedMultiplier
            )
        )
    }

    fun updateAspectRatio(preset: AspectRatioPreset) {
        val clip = selectedClip.value ?: return
        val currentProject = project.value
        updateClip(clip.copy(aspectRatioId = preset.id))
        if (currentProject != null) {
            viewModelScope.launch {
                repository.updateProject(currentProject.copy(aspectRatioId = preset.id))
            }
        }
    }

    fun updateOverlayText(text: String) {
        val clip = selectedClip.value ?: return
        updateClip(clip.copy(overlayText = text.take(64)))
    }

    fun generateAiHookCaption() {
        val clip = selectedClip.value ?: return
        val aspect = AspectRatioPreset.fromId(clip.aspectRatioId)
        viewModelScope.launch {
            _hookState.value = HookUiState.Generating
            try {
                val caption = geminiService.generateQuickHookCaption(
                    clipLabel = clip.label,
                    clipReason = clip.reason,
                    aspectRatioLabel = aspect.label
                )
                updateClip(clip.copy(overlayText = caption.take(64)))
                _hookState.value = HookUiState.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Hook caption generation timed out", e)
                _hookState.value = HookUiState.Error("Hook generation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Hook caption generation failed", e)
                _hookState.value = HookUiState.Error(
                    e.localizedMessage ?: "Failed to generate AI hook."
                )
            }
        }
    }

    fun exportCurrentClip(resolution: ExportResolution) {
        val currentProject = project.value ?: return
        val clip = selectedClip.value ?: return
        if (currentProject.sourceUri.isBlank()) {
            _exportState.value = ExportUiState.Error(
                "Link your local video file first to render an offline HD MP4, or share the YouTube Clip Kit below."
            )
            return
        }
        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting(0f)
            try {
                val completed = exportManager.exportClipToHdMp4(
                    sourceUri = Uri.parse(currentProject.sourceUri),
                    sourceWidth = currentProject.width,
                    sourceHeight = currentProject.height,
                    clip = clip,
                    resolution = resolution,
                    projectTitle = currentProject.title,
                    onProgress = { progress ->
                        _exportState.value = ExportUiState.Exporting(progress)
                    }
                )
                val aspect = AspectRatioPreset.fromId(clip.aspectRatioId)
                val record = ExportRecordEntity(
                    projectId = currentProject.id,
                    projectTitle = currentProject.title,
                    clipLabel = clip.label,
                    outputUri = completed.outputUri.toString(),
                    resolutionLabel = resolution.label,
                    aspectRatioLabel = aspect.label,
                    durationMs = (clip.clipDurationMs / clip.speedMultiplier).toLong(),
                    fileSizeBytes = completed.fileSizeBytes
                )
                val id = repository.recordExport(record)
                _exportState.value = ExportUiState.Success(record.copy(id = id))
            } catch (e: CancellationException) {
                _exportState.value = ExportUiState.Idle
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "HD export failed", e)
                _exportState.value = ExportUiState.Error(
                    e.localizedMessage ?: "HD video export failed on this device."
                )
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportUiState.Idle
    }

    fun dismissAnalysisBanner() {
        _analysisState.value = AnalysisUiState.Idle
    }

    private fun updateClip(updated: HighlightClipEntity) {
        viewModelScope.launch {
            repository.updateClip(updated)
        }
    }
}
