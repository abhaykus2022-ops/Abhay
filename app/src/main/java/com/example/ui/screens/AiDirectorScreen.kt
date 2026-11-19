package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ChatMessageEntity
import com.example.ui.components.AmbientGlassBackdrop
import com.example.ui.components.SlateCard
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CrimsonErrorDim
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanDim
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDim
import com.example.viewmodel.AiDirectorViewModel
import com.example.viewmodel.ChatUiState

@Composable
fun AiDirectorScreen(
    viewModel: AiDirectorViewModel,
    projectId: Long?,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(projectId) {
        viewModel.setContextProject(projectId)
    }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val highThinking by viewModel.highThinkingEnabled.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    val chatUiState by viewModel.chatUiState.collectAsStateWithLifecycle()
    val contextSummary by viewModel.activeContextSummary.collectAsStateWithLifecycle()

    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingReply) {
        val total = messages.size + if (streamingReply != null) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    val quickPrompts = listOf(
        "Plan a 30s viral Shorts cut for this clip",
        "Fix slow intro pacing & boost retention",
        "Best Frosted Glass & color grade settings?",
        "Write 3 scroll-stopping YouTube Shorts hooks"
    )

    AmbientGlassBackdrop(modifier = modifier.imePadding()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
            ) {
                Surface(
                    color = SlateSurface.copy(alpha = 0.72f),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), SlateOutline))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = StudioAmberDim.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.55f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = StudioAmber,
                                        modifier = Modifier
                                            .padding(7.dp)
                                            .size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.nav_ai_director),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.ai_director_subtitle),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (messages.isNotEmpty()) {
                                IconButton(
                                    onClick = viewModel::clearChat,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("clear_chat_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteSweep,
                                        contentDescription = "Clear chat history",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MidnightBg.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, SlateOutline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = contextSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (highThinking) StudioAmberDim.copy(alpha = 0.85f) else SlateElevated.copy(alpha = 0.72f))
                                    .border(
                                        width = 1.dp,
                                        color = if (highThinking) StudioAmber else Color.White.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Psychology,
                                    contentDescription = null,
                                    tint = if (highThinking) StudioAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.high_thinking_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (highThinking) StudioAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Switch(
                                    checked = highThinking,
                                    onCheckedChange = viewModel::toggleHighThinking,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MidnightBg,
                                        checkedTrackColor = StudioAmber,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MidnightBg
                                    ),
                                    modifier = Modifier.testTag("high_thinking_toggle")
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (messages.isEmpty() && streamingReply == null) {
                        item {
                            SlateCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Solve any edit, pacing, or Shorts viral challenge",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Ask for custom timing plans, Frosted Glass & color recipes, retention hooks, or troubleshooting. Deep Thinking mode engages high-reasoning analysis for complex multi-step edits.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(message = msg)
                    }

                    if (streamingReply != null) {
                        item(key = "streaming_bubble") {
                            ChatBubble(
                                message = ChatMessageEntity(
                                    role = "model",
                                    content = streamingReply.orEmpty().ifEmpty { "Thinking deeply about your edit..." },
                                    usedHighThinking = highThinking
                                ),
                                isStreaming = true
                            )
                        }
                    }
                }

                if (chatUiState is ChatUiState.Error) {
                    val msg = (chatUiState as ChatUiState.Error).message
                    Surface(
                        color = CrimsonErrorDim.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, CrimsonError),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = viewModel::dismissError) {
                                Text("Dismiss", color = CrimsonError)
                            }
                        }
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickPrompts) { prompt ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = SlateElevated.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .clickable { viewModel.sendMessage(prompt) }
                                .testTag("quick_prompt_chip")
                        ) {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = SlateSurface.copy(alpha = 0.76f),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), SlateOutline))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text("Ask AI Director about cuts, Glass VFX, hooks, or fixes...")
                            },
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = SlateOutline,
                                focusedContainerColor = MidnightBg.copy(alpha = 0.75f),
                                unfocusedContainerColor = MidnightBg.copy(alpha = 0.75f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field")
                        )

                        val isBusy = chatUiState is ChatUiState.Thinking || chatUiState is ChatUiState.Streaming
                        Surface(
                            shape = CircleShape,
                            color = if (inputText.isNotBlank() && !isBusy) ElectricCyan else SlateElevated,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(enabled = inputText.isNotBlank() && !isBusy) {
                                    val textToSend = inputText
                                    inputText = ""
                                    viewModel.sendMessage(textToSend)
                                }
                                .testTag("send_chat_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = ElectricCyan
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send message",
                                        tint = if (inputText.isNotBlank()) MidnightBg else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessageEntity,
    isStreaming: Boolean = false
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 14.dp
            ),
            color = if (isUser) ElectricCyanDim.copy(alpha = 0.78f) else SlateElevated.copy(alpha = 0.74f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.28f),
                        if (isUser) ElectricCyan.copy(alpha = 0.6f) else SlateOutline
                    )
                )
            ),
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "AI Director",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        if (message.usedHighThinking) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = StudioAmberDim,
                                border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "DEEP THINKING",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioAmber,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (isStreaming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.8.dp,
                                color = ElectricCyan
                            )
                        }
                    }
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
