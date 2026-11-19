package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StudioContainer
import com.example.data.AspectRatioPreset
import com.example.data.ChatMessageEntity
import com.example.data.VfxPreset
import com.example.data.formatTimecode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiDirectorViewModel"

sealed interface ChatUiState {
    data object Idle : ChatUiState
    data object Thinking : ChatUiState
    data object Streaming : ChatUiState
    data class Error(val message: String) : ChatUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AiDirectorViewModel(application: Application) : AndroidViewModel(application) {
    private val container = StudioContainer.get(application)
    private val repository = container.repository
    private val geminiService = container.geminiService

    private val _contextProjectId = MutableStateFlow<Long?>(null)
    private val _highThinkingEnabled = MutableStateFlow(true)
    val highThinkingEnabled: StateFlow<Boolean> = _highThinkingEnabled.asStateFlow()

    private val _streamingReply = MutableStateFlow<String?>(null)
    val streamingReply: StateFlow<String?> = _streamingReply.asStateFlow()

    private val _chatUiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val chatUiState: StateFlow<ChatUiState> = _chatUiState.asStateFlow()

    val messages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val activeContextSummary: StateFlow<String> = combine(
        _contextProjectId,
        repository.allProjects
    ) { targetId, projects ->
        projects.firstOrNull { it.id == targetId } ?: projects.firstOrNull()
    }.flatMapLatest { project ->
        if (project == null) {
            flowOf("No active video project loaded yet.")
        } else {
            repository.observeClipsForProject(project.id).flatMapLatest { clips ->
                val topClip = clips.firstOrNull()
                val ratio = AspectRatioPreset.fromId(topClip?.aspectRatioId ?: project.aspectRatioId)
                val vfx = VfxPreset.fromId(topClip?.vfxPresetId)
                val summary = buildString {
                    append("Project '${project.title}' (${formatTimecode(project.durationMs, false)}, ${project.width}x${project.height})")
                    if (topClip != null) {
                        append(", Clip '${topClip.label}' [${formatTimecode(topClip.startMs)}–${formatTimecode(topClip.endMs)}]")
                        append(", Aspect: ${ratio.label}, Look: ${vfx.displayName}, Speed: ${topClip.speedMultiplier}x")
                    }
                    if (!project.aiSummary.isNullOrBlank()) {
                        append(". Scene summary: ${project.aiSummary}")
                    }
                }
                flowOf(summary)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), "Studio ready")

    fun setContextProject(projectId: Long?) {
        _contextProjectId.value = projectId
    }

    fun toggleHighThinking(enabled: Boolean) {
        _highThinkingEnabled.value = enabled
    }

    fun sendMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) return
        if (_chatUiState.value is ChatUiState.Thinking || _chatUiState.value is ChatUiState.Streaming) return

        val useHighThinking = _highThinkingEnabled.value
        viewModelScope.launch {
            _chatUiState.value = ChatUiState.Thinking
            _streamingReply.value = ""
            try {
                val priorHistory = repository.getChatHistory()
                repository.addChatMessage(
                    ChatMessageEntity(
                        projectId = _contextProjectId.value,
                        role = "user",
                        content = prompt,
                        usedHighThinking = useHighThinking
                    )
                )

                val accumulated = StringBuilder()
                withTimeout(60_000L) {
                    geminiService.streamDirectorReply(
                        history = priorHistory,
                        userPrompt = prompt,
                        projectContextSummary = activeContextSummary.value,
                        useHighThinking = useHighThinking
                    ).collect { chunk ->
                        if (chunk.isNotEmpty()) {
                            _chatUiState.value = ChatUiState.Streaming
                            accumulated.append(chunk)
                            _streamingReply.value = accumulated.toString()
                        }
                    }
                }

                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotEmpty()) {
                    repository.addChatMessage(
                        ChatMessageEntity(
                            projectId = _contextProjectId.value,
                            role = "model",
                            content = finalReply,
                            usedHighThinking = useHighThinking
                        )
                    )
                    _chatUiState.value = ChatUiState.Idle
                } else {
                    _chatUiState.value = ChatUiState.Error("AI Director returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Director chat timed out", e)
                _chatUiState.value = ChatUiState.Error(
                    "Response timed out after 60 seconds. Try a shorter question or disable Deep Thinking for faster replies."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Director chat failed", e)
                _chatUiState.value = ChatUiState.Error(
                    e.localizedMessage ?: "Unable to reach AI Director."
                )
            } finally {
                _streamingReply.value = null
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            _chatUiState.value = ChatUiState.Idle
            _streamingReply.value = null
        }
    }

    fun dismissError() {
        _chatUiState.value = ChatUiState.Idle
    }
}
