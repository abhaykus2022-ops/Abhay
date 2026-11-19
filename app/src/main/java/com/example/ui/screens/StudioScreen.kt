package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.AspectRatioPreset
import com.example.data.ExportResolution
import com.example.data.HighlightClipEntity
import com.example.data.VideoProjectEntity
import com.example.data.VfxPreset
import com.example.data.formatFileSize
import com.example.data.formatTimecode
import com.example.ui.components.AmbientGlassBackdrop
import com.example.ui.components.AspectRatioSelectorRow
import com.example.ui.components.LiveVideoPreviewPlayer
import com.example.ui.components.SlateCard
import com.example.ui.components.TimecodeBadge
import com.example.ui.components.YouTubeClipPreviewPlayer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CrimsonErrorDim
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanDim
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MintReady
import com.example.ui.theme.MintReadyDim
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDim
import com.example.ui.theme.TimecodeTextStyle
import com.example.viewmodel.AnalysisUiState
import com.example.viewmodel.ExportUiState
import com.example.viewmodel.HookUiState
import com.example.viewmodel.StudioViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    projectId: Long?,
    onNavigateToProjects: () -> Unit,
    onNavigateToAiDirector: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(projectId) {
        viewModel.openProject(projectId)
    }

    val project by viewModel.project.collectAsStateWithLifecycle()
    val clips by viewModel.clips.collectAsStateWithLifecycle()
    val selectedClip by viewModel.selectedClip.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val hookState by viewModel.hookState.collectAsStateWithLifecycle()

    var showExportSheet by rememberSaveable { mutableStateOf(false) }

    val linkLocalPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachLocalVideoToCurrentProject(uri)
        }
    }

    val currentProject = project
    val activeClip = selectedClip

    if (currentProject == null || activeClip == null) {
        AmbientGlassBackdrop(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                SlateCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MovieFilter,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = stringResource(R.string.empty_studio_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.empty_studio_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onNavigateToProjects,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = MidnightBg
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("go_to_projects_button")
                        ) {
                            Text("Open Projects Hub", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    AmbientGlassBackdrop(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            StudioTopBar(
                project = currentProject,
                onOpenDirector = { onNavigateToAiDirector(currentProject.id) },
                onExportClick = { showExportSheet = true }
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth >= 720.dp
                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.48f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProjectPreviewCanvas(
                                project = currentProject,
                                clip = activeClip
                            )
                            AspectRatioSelectorRow(
                                selected = AspectRatioPreset.fromId(activeClip.aspectRatioId),
                                onSelect = viewModel::updateAspectRatio
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.52f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            StudioControlsDeck(
                                project = currentProject,
                                clips = clips,
                                activeClip = activeClip,
                                analysisState = analysisState,
                                hookState = hookState,
                                viewModel = viewModel
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ProjectPreviewCanvas(
                            project = currentProject,
                            clip = activeClip
                        )
                        StudioControlsDeck(
                            project = currentProject,
                            clips = clips,
                            activeClip = activeClip,
                            analysisState = analysisState,
                            hookState = hookState,
                            viewModel = viewModel
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        if (showExportSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = {
                    if (exportState !is ExportUiState.Exporting) {
                        showExportSheet = false
                        viewModel.resetExportState()
                    }
                },
                sheetState = sheetState,
                containerColor = SlateSurface.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                ExportBottomSheetContent(
                    project = currentProject,
                    clip = activeClip,
                    exportState = exportState,
                    onStartExport = viewModel::exportCurrentClip,
                    onLinkLocalVideo = {
                        linkLocalPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onClose = {
                        showExportSheet = false
                        viewModel.resetExportState()
                    }
                )
            }
        }
    }
}

@Composable
private fun ProjectPreviewCanvas(
    project: VideoProjectEntity,
    clip: HighlightClipEntity
) {
    if (project.sourceUri.isNotBlank()) {
        LiveVideoPreviewPlayer(
            sourceUri = project.sourceUri,
            clip = clip
        )
    } else if (!project.youtubeVideoId.isNullOrBlank()) {
        YouTubeClipPreviewPlayer(
            videoId = project.youtubeVideoId,
            clip = clip
        )
    }
}

@Composable
private fun StudioTopBar(
    project: VideoProjectEntity,
    onOpenDirector: () -> Unit,
    onExportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSurface.copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), SlateOutline)),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (project.sourceUri.isBlank() && !project.youtubeVideoId.isNullOrBlank()) {
                    "YouTube • ${formatTimecode(project.durationMs, false)} • ${project.channelAuthor ?: "Creator"}"
                } else {
                    "Source ${formatTimecode(project.durationMs, false)} • ${project.width}×${project.height}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onOpenDirector,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("studio_open_director_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Ask AI Director",
                    tint = StudioAmber
                )
            }

            Button(
                onClick = onExportClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = MidnightBg
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(42.dp)
                    .testTag("export_hd_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (project.sourceUri.isBlank()) "Clip Kit / MP4" else stringResource(R.string.cta_export_hd),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StudioControlsDeck(
    project: VideoProjectEntity,
    clips: List<HighlightClipEntity>,
    activeClip: HighlightClipEntity,
    analysisState: AnalysisUiState,
    hookState: HookUiState,
    viewModel: StudioViewModel
) {
    var focusPrompt by rememberSaveable { mutableStateOf("") }
    var selectedToolTab by rememberSaveable { mutableIntStateOf(0) }

    SlateCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = StudioAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "AI Highlight Clips (${clips.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = { viewModel.createCustomClip(30_000L) },
                    modifier = Modifier.testTag("add_custom_clip_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom Clip", color = ElectricCyan)
                }
            }

            OutlinedTextField(
                value = focusPrompt,
                onValueChange = { focusPrompt = it },
                placeholder = {
                    Text(
                        text = "Optional focus (e.g. gameplay clutches, funny moments, 45s story)...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = SlateOutline,
                    focusedContainerColor = MidnightBg.copy(alpha = 0.72f),
                    unfocusedContainerColor = MidnightBg.copy(alpha = 0.72f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_focus_prompt_input")
            )

            val isAnalyzing = analysisState is AnalysisUiState.SamplingFrames ||
                analysisState is AnalysisUiState.AnalyzingWithAi

            Button(
                onClick = { viewModel.analyzeWithAi(focusPrompt) },
                enabled = !isAnalyzing,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioAmber,
                    contentColor = MidnightBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("analyze_highlights_button")
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MidnightBg
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (analysisState is AnalysisUiState.SamplingFrames) {
                            "Sampling video keyframes..."
                        } else {
                            "Gemini analyzing best moments..."
                        },
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (clips.any { it.isAiGenerated }) {
                            stringResource(R.string.cta_generate_more)
                        } else {
                            stringResource(R.string.cta_analyze_ai)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (analysisState is AnalysisUiState.Error) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrimsonErrorDim.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, CrimsonError)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = analysisState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::dismissAnalysisBanner) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss error",
                                tint = CrimsonError
                            )
                        }
                    }
                }
            } else if (analysisState is AnalysisUiState.Success) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MintReadyDim.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MintReady.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "Generated ${analysisState.generatedCount} highlights: ${analysisState.summary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MintReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clips, key = { it.id }) { clip ->
                    val isSelected = clip.id == activeClip.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ElectricCyanDim.copy(alpha = 0.85f) else SlateElevated.copy(alpha = 0.72f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier
                            .width(210.dp)
                            .clickable { viewModel.selectClip(clip.id) }
                            .testTag("clip_card_${clip.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = if (clip.isAiGenerated) StudioAmberDim else SlateSurface
                                ) {
                                    Text(
                                        text = if (clip.isAiGenerated) "AI ${clip.viralScore}%" else clip.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (clip.isAiGenerated) StudioAmber else ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (clips.size > 1) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Delete clip",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.deleteClip(clip.id) }
                                    )
                                }
                            }
                            Text(
                                text = clip.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatTimecode(clip.startMs)} – ${formatTimecode(clip.endMs)}",
                                style = TimecodeTextStyle,
                                color = ElectricCyan
                            )
                        }
                    }
                }
            }
        }
    }

    // Editor Tool Deck Tabs
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateSurface.copy(alpha = 0.7f))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), SlateOutline)),
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val tabs = listOf(
            Triple(0, "Trim & Time", Icons.Filled.ContentCut),
            Triple(1, "Glass & VFX", Icons.Filled.Tune),
            Triple(2, "Frame & Hook", Icons.Outlined.AspectRatio)
        )
        tabs.forEach { (idx, label, icon) ->
            val selected = selectedToolTab == idx
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) ElectricCyanDim.copy(alpha = 0.85f) else Color.Transparent)
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) ElectricCyan else Color.Transparent,
                        shape = RoundedCornerShape(9.dp)
                    )
                    .clickable { selectedToolTab = idx }
                    .testTag("editor_tab_$idx")
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    when (selectedToolTab) {
        0 -> TrimTimingPanel(
            project = project,
            clip = activeClip,
            onUpdateRange = viewModel::updateTrimRange,
            onApplyDuration = viewModel::applyDurationPreset
        )

        1 -> CinematicVfxPanel(
            clip = activeClip,
            onApplyPreset = viewModel::applyVfxPreset,
            onUpdateSliders = viewModel::updateColorAndMotion
        )

        else -> FramingHookPanel(
            clip = activeClip,
            hookState = hookState,
            onSelectAspectRatio = viewModel::updateAspectRatio,
            onUpdateOverlayText = viewModel::updateOverlayText,
            onGenerateAiHook = viewModel::generateAiHookCaption
        )
    }
}

@Composable
private fun TrimTimingPanel(
    project: VideoProjectEntity,
    clip: HighlightClipEntity,
    onUpdateRange: (Long, Long) -> Unit,
    onApplyDuration: (Long?) -> Unit
) {
    val maxMs = project.durationMs.coerceAtLeast(1_500L).toFloat()
    var sliderRange by remember(clip.id, clip.startMs, clip.endMs) {
        mutableStateOf(clip.startMs.toFloat()..clip.endMs.toFloat())
    }

    SlateCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Clip Timing",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TimecodeBadge(
                    text = "Len ${formatTimecode(clip.clipDurationMs)}",
                    accentColor = MintReady
                )
            }

            RangeSlider(
                value = sliderRange,
                onValueChange = { range ->
                    val minGap = 500f
                    val start = range.start.coerceIn(0f, (maxMs - minGap).coerceAtLeast(0f))
                    val end = range.endInclusive.coerceIn((start + minGap).coerceAtMost(maxMs), maxMs)
                    sliderRange = start..end
                },
                onValueChangeFinished = {
                    onUpdateRange(sliderRange.start.toLong(), sliderRange.endInclusive.toLong())
                },
                valueRange = 0f..maxMs,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricCyan,
                    activeTrackColor = ElectricCyan,
                    inactiveTrackColor = SlateElevated
                ),
                modifier = Modifier.testTag("trim_range_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimingStepperBox(
                    title = "Start",
                    timecode = formatTimecode(clip.startMs),
                    onMinus = { onUpdateRange(clip.startMs - 500L, clip.endMs) },
                    onPlus = { onUpdateRange(clip.startMs + 500L, clip.endMs) },
                    modifier = Modifier.weight(1f)
                )
                TimingStepperBox(
                    title = "End",
                    timecode = formatTimecode(clip.endMs),
                    onMinus = { onUpdateRange(clip.startMs, clip.endMs - 500L) },
                    onPlus = { onUpdateRange(clip.startMs, clip.endMs + 500L) },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Quick Length Presets (Shorts or Long-Form)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(
                    "15s Short" to 15_000L,
                    "30s Short" to 30_000L,
                    "60s Max Short" to 60_000L,
                    "Full / Long" to null
                )
                items(presets) { (label, ms) ->
                    FilterChip(
                        selected = false,
                        onClick = { onApplyDuration(ms) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SlateElevated.copy(alpha = 0.72f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("duration_preset_${ms ?: "full"}")
                    )
                }
            }
        }
    }
}

@Composable
private fun TimingStepperBox(
    title: String,
    timecode: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MidnightBg.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, SlateOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onMinus,
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier.size(width = 40.dp, height = 40.dp)
            ) {
                Text("-0.5", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timecode,
                    style = TimecodeTextStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            TextButton(
                onClick = onPlus,
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier.size(width = 40.dp, height = 40.dp)
            ) {
                Text("+0.5", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            }
        }
    }
}

@Composable
private fun CinematicVfxPanel(
    clip: HighlightClipEntity,
    onApplyPreset: (VfxPreset) -> Unit,
    onUpdateSliders: (Float, Float, Float, Float, Float, Float) -> Unit
) {
    val activePreset = VfxPreset.fromId(clip.vfxPresetId)

    SlateCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Glass & Cinematic Look Presets",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { onApplyPreset(VfxPreset.NATURAL) }) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(VfxPreset.entries) { preset ->
                    val selected = preset == activePreset
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) ElectricCyanDim.copy(alpha = 0.85f) else SlateElevated.copy(alpha = 0.72f),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) ElectricCyan else Color.White.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier
                            .clickable { onApplyPreset(preset) }
                            .testTag("vfx_preset_${preset.id}")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) ElectricCyan else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = preset.tagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BlurOn,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Frosted Glass Frame & Specular Sheen",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
            }
            VfxSliderRow(
                label = "Glass Effect Intensity",
                valueText = "${(clip.glassIntensity * 100).roundToInt()}%",
                value = clip.glassIntensity,
                range = 0f..1.0f,
                onChange = { g ->
                    onUpdateSliders(
                        clip.brightness,
                        clip.contrast,
                        clip.saturation,
                        clip.zoomIntensity,
                        g,
                        clip.speedMultiplier
                    )
                }
            )

            VfxSliderRow(
                label = "Contrast",
                valueText = "${(clip.contrast * 100).roundToInt()}%",
                value = clip.contrast,
                range = -0.35f..0.65f,
                onChange = { c ->
                    onUpdateSliders(
                        clip.brightness,
                        c,
                        clip.saturation,
                        clip.zoomIntensity,
                        clip.glassIntensity,
                        clip.speedMultiplier
                    )
                }
            )
            VfxSliderRow(
                label = "Saturation",
                valueText = "${clip.saturation.roundToInt()}",
                value = clip.saturation,
                range = -100f..60f,
                onChange = { s ->
                    onUpdateSliders(
                        clip.brightness,
                        clip.contrast,
                        s,
                        clip.zoomIntensity,
                        clip.glassIntensity,
                        clip.speedMultiplier
                    )
                }
            )
            VfxSliderRow(
                label = "Brightness",
                valueText = "${(clip.brightness * 100).roundToInt()}%",
                value = clip.brightness,
                range = -0.4f..0.4f,
                onChange = { b ->
                    onUpdateSliders(
                        b,
                        clip.contrast,
                        clip.saturation,
                        clip.zoomIntensity,
                        clip.glassIntensity,
                        clip.speedMultiplier
                    )
                }
            )
            VfxSliderRow(
                label = "Dynamic Punch-In Zoom",
                valueText = "${(clip.zoomIntensity * 100).roundToInt()}%",
                value = clip.zoomIntensity,
                range = 0f..0.30f,
                onChange = { z ->
                    onUpdateSliders(
                        clip.brightness,
                        clip.contrast,
                        clip.saturation,
                        z,
                        clip.glassIntensity,
                        clip.speedMultiplier
                    )
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Speed,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Playback Speed",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                items(speeds) { speed ->
                    val selected = kotlin.math.abs(clip.speedMultiplier - speed) < 0.05f
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onUpdateSliders(
                                clip.brightness,
                                clip.contrast,
                                clip.saturation,
                                clip.zoomIntensity,
                                clip.glassIntensity,
                                speed
                            )
                        },
                        label = { Text("${speed}x", style = TimecodeTextStyle) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyanDim,
                            selectedLabelColor = ElectricCyan,
                            containerColor = SlateElevated.copy(alpha = 0.72f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun VfxSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = TimecodeTextStyle,
                color = ElectricCyan
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = SlateElevated
            )
        )
    }
}

@Composable
private fun FramingHookPanel(
    clip: HighlightClipEntity,
    hookState: HookUiState,
    onSelectAspectRatio: (AspectRatioPreset) -> Unit,
    onUpdateOverlayText: (String) -> Unit,
    onGenerateAiHook: () -> Unit
) {
    SlateCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Output Format",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            AspectRatioSelectorRow(
                selected = AspectRatioPreset.fromId(clip.aspectRatioId),
                onSelect = onSelectAspectRatio
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "On-Screen Hook Caption",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedButton(
                    onClick = onGenerateAiHook,
                    enabled = hookState !is HookUiState.Generating,
                    border = BorderStroke(1.dp, StudioAmber),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("generate_ai_hook_button")
                ) {
                    if (hookState is HookUiState.Generating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = StudioAmber
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = StudioAmber,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Hook", color = StudioAmber)
                }
            }

            OutlinedTextField(
                value = clip.overlayText,
                onValueChange = onUpdateOverlayText,
                placeholder = { Text("Add a bold Shorts title overlay (optional)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = SlateOutline,
                    focusedContainerColor = MidnightBg.copy(alpha = 0.72f),
                    unfocusedContainerColor = MidnightBg.copy(alpha = 0.72f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overlay_text_input")
            )

            if (hookState is HookUiState.Error) {
                Text(
                    text = hookState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = CrimsonError
                )
            }
        }
    }
}

@Composable
private fun ExportBottomSheetContent(
    project: VideoProjectEntity,
    clip: HighlightClipEntity,
    exportState: ExportUiState,
    onStartExport: (ExportResolution) -> Unit,
    onLinkLocalVideo: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedResolution by rememberSaveable { mutableStateOf(ExportResolution.HD_1080P) }
    val aspect = AspectRatioPreset.fromId(clip.aspectRatioId)
    val preset = VfxPreset.fromId(clip.vfxPresetId)
    val hasLocalFile = project.sourceUri.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (hasLocalFile) "Export HD Video" else "YouTube Clip Kit & MP4 Export",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${clip.label} • ${aspect.label} • ${formatTimecode(clip.startMs)}–${formatTimecode(clip.endMs)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!hasLocalFile) {
            val ytTimestampUrl = "https://youtu.be/${project.youtubeVideoId}?t=${clip.startMs / 1000L}"
            val clipKitSummary = buildString {
                append("SlateCut AI Clip Plan: ${clip.label}\n")
                append("Source: $ytTimestampUrl\n")
                append("Cut Window: ${formatTimecode(clip.startMs)} – ${formatTimecode(clip.endMs)} (${formatTimecode(clip.clipDurationMs)})\n")
                append("Format: ${aspect.label} | Look: ${preset.displayName} | Glass: ${(clip.glassIntensity * 100).roundToInt()}%\n")
                if (clip.overlayText.isNotBlank()) {
                    append("Hook Caption: \"${clip.overlayText}\"\n")
                }
                append("Why It Works: ${clip.reason}")
            }

            SlateCard(containerColor = SlateElevated.copy(alpha = 0.78f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Instant Clip Timestamp & VFX Kit",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan
                    )
                    Text(
                        text = clipKitSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(clipKitSummary)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_clip_kit_button")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Kit")
                }
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, clipKitSummary)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(shareIntent, "Share Clip Kit"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = MidnightBg
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_clip_kit_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Kit", fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onLinkLocalVideo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioAmber,
                    contentColor = MidnightBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("link_local_video_button")
            ) {
                Icon(Icons.Filled.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Link Local Video File for 1080p MP4 Render", fontWeight = FontWeight.Bold)
            }
            return@Column
        }

        when (exportState) {
            is ExportUiState.Idle, is ExportUiState.Error -> {
                if (exportState is ExportUiState.Error) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CrimsonErrorDim,
                        border = BorderStroke(1.dp, CrimsonError)
                    ) {
                        Text(
                            text = exportState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }

                ExportResolution.entries.forEach { res ->
                    val selected = res == selectedResolution
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) ElectricCyanDim else SlateElevated.copy(alpha = 0.78f),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) ElectricCyan else SlateOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedResolution = res }
                            .testTag("export_res_${res.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = res.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selected) ElectricCyan else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = res.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = ElectricCyan
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { onStartExport(selectedResolution) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = MidnightBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_export_button")
                ) {
                    Text("Render & Save MP4", fontWeight = FontWeight.Bold)
                }
            }

            is ExportUiState.Exporting -> {
                val pct = (exportState.progress * 100).roundToInt()
                SlateCard(containerColor = SlateElevated.copy(alpha = 0.8f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Rendering HD MP4 ($pct%)",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricCyan
                        )
                        LinearProgressIndicator(
                            progress = { exportState.progress.coerceIn(0.02f, 1f) },
                            color = ElectricCyan,
                            trackColor = MidnightBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = "Baking frosted glass frame, color grade, and hardware H.264 encode...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is ExportUiState.Success -> {
                val record = exportState.record
                SlateCard(
                    containerColor = MintReadyDim.copy(alpha = 0.85f),
                    borderColor = MintReady
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MintReady
                            )
                            Text(
                                text = "Saved to Movies/SlateCutAI",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${record.resolutionLabel} • ${record.aspectRatioLabel} • ${formatFileSize(record.fileSizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MintReady
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(record.outputUri), "video/mp4")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(viewIntent) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play")
                    }

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "video/mp4"
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(record.outputUri))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share ${record.clipLabel}")
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = MidnightBg
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sheet_share_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Done", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
