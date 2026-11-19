package com.example.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.AspectRatioPreset
import com.example.data.HighlightClipEntity
import com.example.data.VfxPreset
import com.example.data.formatTimecode
import com.example.media.VfxPipelineBuilder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanDim
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.TimecodeTextStyle
import kotlinx.coroutines.delay

@Composable
fun AmbientGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBg)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricCyan.copy(alpha = 0.16f),
                            ElectricCyan.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.12f),
                        radius = size.maxDimension * 0.45f
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.12f),
                    radius = size.maxDimension * 0.45f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            StudioAmber.copy(alpha = 0.11f),
                            StudioAmber.copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.58f),
                        radius = size.maxDimension * 0.40f
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.58f),
                    radius = size.maxDimension * 0.40f
                )
            },
        content = content
    )
}

@Composable
fun SlateCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SlateSurface.copy(alpha = 0.72f),
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    val borderBrush = if (borderColor != null) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                borderColor.copy(alpha = 0.85f),
                borderColor.copy(alpha = 0.45f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                ElectricCyan.copy(alpha = 0.20f),
                SlateOutline.copy(alpha = 0.65f)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        containerColor.copy(alpha = (containerColor.alpha + 0.08f).coerceAtMost(0.95f)),
                        containerColor
                    )
                )
            )
            .border(width = 1.dp, brush = borderBrush, shape = shape)
    ) {
        Column(content = content)
    }
}

@Composable
fun TimecodeBadge(
    text: String,
    accentColor: Color = ElectricCyan,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MidnightBg.copy(alpha = 0.74f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.25f), SlateOutline)
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Text(
                text = text,
                style = TimecodeTextStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AspectRatioSelectorRow(
    selected: AspectRatioPreset,
    onSelect: (AspectRatioPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateSurface.copy(alpha = 0.7f))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), SlateOutline)),
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AspectRatioPreset.entries.forEach { preset ->
            val isSelected = preset == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) ElectricCyanDim.copy(alpha = 0.85f) else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) ElectricCyan else Color.Transparent,
                        shape = RoundedCornerShape(9.dp)
                    )
                    .clickable { onSelect(preset) }
                    .testTag("aspect_ratio_${preset.id}")
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = preset.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun LiveVideoPreviewPlayer(
    sourceUri: String,
    clip: HighlightClipEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val aspect = AspectRatioPreset.fromId(clip.aspectRatioId)
    val preset = VfxPreset.fromId(clip.vfxPresetId)

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    val pipelineKey = remember(
        sourceUri,
        clip.id,
        clip.startMs,
        clip.endMs,
        clip.vfxPresetId,
        clip.brightness,
        clip.contrast,
        clip.saturation,
        clip.zoomIntensity,
        clip.glassIntensity,
        clip.overlayText,
        clip.aspectRatioId
    ) {
        listOf(
            sourceUri,
            clip.id,
            clip.startMs,
            clip.endMs,
            clip.vfxPresetId,
            (clip.brightness * 100).toInt(),
            (clip.contrast * 100).toInt(),
            clip.saturation.toInt(),
            (clip.zoomIntensity * 100).toInt(),
            (clip.glassIntensity * 100).toInt(),
            clip.overlayText,
            clip.aspectRatioId
        ).joinToString("|")
    }

    LaunchedEffect(pipelineKey) {
        delay(90L)
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(sourceUri))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.startMs.coerceAtLeast(0L))
                    .setEndPositionMs(clip.endMs.coerceAtLeast(clip.startMs + 500L))
                    .build()
            )
            .build()
        val effects = VfxPipelineBuilder.buildPreviewEffects(clip)
        exoPlayer.setVideoEffects(effects)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    LaunchedEffect(clip.speedMultiplier) {
        exoPlayer.setPlaybackSpeed(clip.speedMultiplier.coerceIn(0.5f, 2.0f))
    }

    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(150L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A101A).copy(alpha = 0.85f), MidnightBg.copy(alpha = 0.92f))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.24f), ElectricCyan.copy(alpha = 0.25f), SlateOutline)
                ),
                RoundedCornerShape(18.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .animateContentSize()
                .then(
                    when (aspect) {
                        AspectRatioPreset.SHORTS_9_16 -> Modifier.height(300.dp).aspectRatio(9f / 16f)
                        AspectRatioPreset.LANDSCAPE_16_9 -> Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        AspectRatioPreset.SQUARE_1_1 -> Modifier.height(260.dp).aspectRatio(1f)
                    }
                )
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .border(1.5.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }
                    .testTag("preview_player_surface")
            )

            PreviewBadgesOverlay(
                aspect = aspect,
                preset = preset,
                glassIntensity = clip.glassIntensity,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, MidnightBg.copy(alpha = 0.86f))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("preview_play_pause_button")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SlateElevated.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(20.dp)
                        )
                    }
                }

                TimecodeBadge(
                    text = "${formatTimecode(currentPosMs)} / ${formatTimecode(clip.clipDurationMs)}",
                    accentColor = if (isPlaying) ElectricCyan else StudioAmber
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeClipPreviewPlayer(
    videoId: String,
    clip: HighlightClipEntity,
    modifier: Modifier = Modifier
) {
    val aspect = AspectRatioPreset.fromId(clip.aspectRatioId)
    val preset = VfxPreset.fromId(clip.vfxPresetId)
    val startSec = (clip.startMs / 1000L).coerceAtLeast(0L)
    val endSec = (clip.endMs / 1000L).coerceAtLeast(startSec + 2L)

    val embedHtml = remember(videoId, startSec, endSec) {
        """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
        <style>
          html, body { margin:0; padding:0; background:#000; width:100%; height:100%; overflow:hidden; }
          iframe { width:100%; height:100%; border:0; }
        </style>
        </head>
        <body>
          <iframe
            src="https://www.youtube.com/embed/$videoId?start=$startSec&end=$endSec&autoplay=1&playsinline=1&rel=0&modestbranding=1"
            allow="autoplay; encrypted-media; picture-in-picture"
            allowfullscreen>
          </iframe>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A101A).copy(alpha = 0.85f), MidnightBg.copy(alpha = 0.92f))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.24f), ElectricCyan.copy(alpha = 0.25f), SlateOutline)
                ),
                RoundedCornerShape(18.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .animateContentSize()
                .then(
                    when (aspect) {
                        AspectRatioPreset.SHORTS_9_16 -> Modifier.height(300.dp).aspectRatio(9f / 16f)
                        AspectRatioPreset.LANDSCAPE_16_9 -> Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        AspectRatioPreset.SQUARE_1_1 -> Modifier.height(260.dp).aspectRatio(1f)
                    }
                )
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .border(1.5.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    if (webView.tag != "$videoId|$startSec|$endSec") {
                        webView.tag = "$videoId|$startSec|$endSec"
                        webView.loadDataWithBaseURL(
                            "https://www.youtube.com",
                            embedHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("youtube_preview_webview")
            )

            // Live Frosted Glass & Specular Sheen Visual Overlay Preview
            if (clip.glassIntensity > 0.03f) {
                val intensity = clip.glassIntensity.coerceIn(0f, 1f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val margin = size.minDimension * 0.042f
                    val corner = size.minDimension * 0.055f
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.8f * intensity),
                                ElectricCyan.copy(alpha = 0.65f * intensity),
                                Color.White.copy(alpha = 0.4f * intensity)
                            )
                        ),
                        topLeft = Offset(margin, margin),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - margin * 2,
                            size.height - margin * 2
                        ),
                        cornerRadius = CornerRadius(corner, corner),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    val sheen = Path().apply {
                        moveTo(margin, margin + size.height * 0.24f)
                        lineTo(margin + size.width * 0.42f, margin)
                        lineTo(margin + size.width * 0.62f, margin)
                        lineTo(margin, margin + size.height * 0.38f)
                        close()
                    }
                    drawPath(
                        path = sheen,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFFE0F2FE).copy(alpha = 0.26f * intensity),
                                Color.Transparent
                            )
                        )
                    )
                }
            }

            PreviewBadgesOverlay(
                aspect = aspect,
                preset = preset,
                glassIntensity = clip.glassIntensity,
                extraTag = "YT CLIP",
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )

            if (clip.overlayText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MidnightBg.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 42.dp)
                ) {
                    Text(
                        text = clip.overlayText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            TimecodeBadge(
                text = "YT ${formatTimecode(clip.startMs, false)} – ${formatTimecode(clip.endMs, false)}",
                accentColor = StudioAmber,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun PreviewBadgesOverlay(
    aspect: AspectRatioPreset,
    preset: VfxPreset,
    glassIntensity: Float,
    extraTag: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MidnightBg.copy(alpha = 0.76f),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
            ) {
                Text(
                    text = aspect.badge.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            if (extraTag != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF450A0A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Text(
                        text = extraTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (glassIntensity > 0.05f) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MidnightBg.copy(alpha = 0.76f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BlurOn,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Glass",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
            if (preset != VfxPreset.NATURAL) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MidnightBg.copy(alpha = 0.76f),
                    border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = StudioAmber,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioAmber
                        )
                    }
                }
            }
        }
    }
}
