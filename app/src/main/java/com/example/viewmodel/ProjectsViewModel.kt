package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StudioContainer
import com.example.data.ExportRecordEntity
import com.example.data.VideoProjectEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "ProjectsViewModel"

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data class Importing(val statusText: String) : ImportUiState
    data class Error(val message: String) : ImportUiState
}

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = StudioContainer.get(application)
    private val repository = container.repository
    private val frameSampler = container.frameSampler
    private val youtubeResolver = container.youtubeResolver
    private val geminiService = container.geminiService

    val projects: StateFlow<List<VideoProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val exports: StateFlow<List<ExportRecordEntity>> = repository.allExports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val _importState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    fun importVideo(uri: Uri, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            _importState.value = ImportUiState.Importing("Reading local video...")
            try {
                runCatching {
                    getApplication<Application>().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val metadata = frameSampler.extractMetadataAndThumbnail(uri)
                val projectId = repository.createProjectWithInitialClip(metadata, uri.toString())
                _importState.value = ImportUiState.Idle
                onCreated(projectId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import selected video", e)
                _importState.value = ImportUiState.Error(
                    e.localizedMessage ?: "Could not read this video file. Try another MP4 or MOV."
                )
            }
        }
    }

    fun importYouTubeLink(
        rawUrl: String,
        focusPrompt: String = "",
        onCreated: (Long) -> Unit
    ) {
        if (rawUrl.isBlank()) {
            _importState.value = ImportUiState.Error("Paste a YouTube video or Shorts link first.")
            return
        }
        viewModelScope.launch {
            _importState.value = ImportUiState.Importing("Fetching YouTube video & frames...")
            try {
                val ytInfo = youtubeResolver.resolveYouTubeVideo(rawUrl)
                val frames = youtubeResolver.sampleYouTubeStoryboardFrames(
                    videoId = ytInfo.videoId,
                    durationMs = ytInfo.durationMs
                )

                _importState.value = ImportUiState.Importing("Gemini generating viral clips...")
                var aiSummary: String? = null
                val suggestions = try {
                    val analysis = geminiService.analyzeYouTubeVideoForHighlights(
                        info = ytInfo,
                        frames = frames,
                        existingClipCount = 0,
                        userFocusPrompt = focusPrompt
                    )
                    aiSummary = analysis.summary
                    analysis.suggestions
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "YouTube AI highlight scan timed out, creating starter clip", e)
                    emptyList()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "YouTube AI highlight scan failed, creating starter clip", e)
                    aiSummary = "Ready for manual & AI highlight clipping (${e.localizedMessage ?: "AI retry available in Studio"})"
                    emptyList()
                } finally {
                    frames.forEach { it.bitmap.recycle() }
                }

                val projectId = repository.createYouTubeProjectWithInitialClips(
                    info = ytInfo,
                    aiSummary = aiSummary,
                    suggestions = suggestions
                )
                _importState.value = ImportUiState.Idle
                onCreated(projectId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import YouTube link", e)
                _importState.value = ImportUiState.Error(
                    e.localizedMessage ?: "Unable to resolve this YouTube link."
                )
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteProject(projectId) }
                .onFailure { Log.e(TAG, "Failed to delete project $projectId", it) }
        }
    }

    fun deleteExport(exportId: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteExport(exportId) }
                .onFailure { Log.e(TAG, "Failed to delete export $exportId", it) }
        }
    }

    fun dismissImportError() {
        _importState.value = ImportUiState.Idle
    }
}
