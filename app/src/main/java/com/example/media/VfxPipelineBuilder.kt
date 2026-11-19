package com.example.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import com.example.data.AspectRatioPreset
import com.example.data.ExportResolution
import com.example.data.HighlightClipEntity
import com.example.data.VfxPreset
import kotlin.math.abs

object VfxPipelineBuilder {

    @OptIn(UnstableApi::class)
    fun buildPreviewEffects(clip: HighlightClipEntity): List<Effect> {
        val aspect = AspectRatioPreset.fromId(clip.aspectRatioId)
        val effects = buildColorAndMotionEffects(clip).toMutableList()
        effects.add(
            Presentation.createForAspectRatio(
                aspect.ratioFloat,
                Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            )
        )
        buildGlassAndTextOverlayEffect(clip, aspect)?.let { effects.add(it) }
        return effects
    }

    @OptIn(UnstableApi::class)
    fun buildExportEffects(
        clip: HighlightClipEntity,
        resolution: ExportResolution,
        sourceWidth: Int,
        sourceHeight: Int
    ): List<Effect> {
        val aspect = AspectRatioPreset.fromId(clip.aspectRatioId)
        val effects = buildColorAndMotionEffects(clip).toMutableList()

        val (targetW, targetH) = computeExportDimensions(
            aspect = aspect,
            resolution = resolution,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight
        )
        effects.add(
            Presentation.createForWidthAndHeight(
                targetW,
                targetH,
                Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            )
        )
        buildGlassAndTextOverlayEffect(clip, aspect)?.let { effects.add(it) }
        return effects
    }

    @OptIn(UnstableApi::class)
    private fun buildColorAndMotionEffects(clip: HighlightClipEntity): List<Effect> {
        val effects = mutableListOf<Effect>()
        val preset = VfxPreset.fromId(clip.vfxPresetId)

        if (abs(clip.brightness) > 0.01f) {
            effects.add(Brightness(clip.brightness.coerceIn(-0.8f, 0.8f)))
        }
        if (abs(clip.contrast) > 0.01f) {
            effects.add(Contrast(clip.contrast.coerceIn(-0.8f, 0.9f)))
        }
        if (abs(clip.saturation) > 0.5f || abs(preset.hueShift) > 0.5f) {
            effects.add(
                HslAdjustment.Builder()
                    .adjustHue(preset.hueShift)
                    .adjustSaturation(clip.saturation.coerceIn(-100f, 100f))
                    .build()
            )
        }
        val glassCoolBoost = if (clip.glassIntensity > 0.05f) 0.04f * clip.glassIntensity else 0f
        if (preset.redScale != 1f || preset.greenScale != 1f || preset.blueScale != 1f || glassCoolBoost > 0f) {
            effects.add(
                RgbAdjustment.Builder()
                    .setRedScale(preset.redScale)
                    .setGreenScale(preset.greenScale + glassCoolBoost * 0.4f)
                    .setBlueScale(preset.blueScale + glassCoolBoost)
                    .build()
            )
        }
        if (clip.zoomIntensity > 0.01f) {
            val maxExtraScale = clip.zoomIntensity.coerceIn(0f, 0.35f)
            effects.add(
                MatrixTransformation { presentationTimeUs ->
                    val cycleUs = 6_000_000f
                    val progress = ((presentationTimeUs % cycleUs) / cycleUs).coerceIn(0f, 1f)
                    val scale = 1f + (maxExtraScale * progress)
                    Matrix().apply { setScale(scale, scale) }
                }
            )
        }
        return effects
    }

    @OptIn(UnstableApi::class)
    private fun buildGlassAndTextOverlayEffect(
        clip: HighlightClipEntity,
        aspect: AspectRatioPreset
    ): OverlayEffect? {
        val overlays = mutableListOf<androidx.media3.effect.TextureOverlay>()

        if (clip.glassIntensity > 0.03f) {
            val glassBitmap = createGlassFrameBitmap(aspect, clip.glassIntensity.coerceIn(0.05f, 1f))
            val glassSettings = StaticOverlaySettings.Builder()
                .setBackgroundFrameAnchor(0f, 0f)
                .setOverlayFrameAnchor(0f, 0f)
                .setAlphaScale(1f)
                .build()
            overlays.add(BitmapOverlay.createStaticBitmapOverlay(glassBitmap, glassSettings))
        }

        val clean = clip.overlayText.trim()
        if (clean.isNotEmpty()) {
            val padded = "  $clean  "
            val spannable = SpannableString(padded).apply {
                setSpan(
                    ForegroundColorSpan(Color.WHITE),
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    BackgroundColorSpan(Color.argb(175, 14, 22, 36)),
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    AbsoluteSizeSpan(24, true),
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            val settings = StaticOverlaySettings.Builder()
                .setOverlayFrameAnchor(0f, 0f)
                .setBackgroundFrameAnchor(0f, 0.68f)
                .setAlphaScale(0.96f)
                .build()
            overlays.add(TextOverlay.createStaticTextOverlay(spannable, settings))
        }

        return if (overlays.isEmpty()) null else OverlayEffect(overlays)
    }

    private fun createGlassFrameBitmap(aspect: AspectRatioPreset, intensity: Float): Bitmap {
        val (w, h) = when (aspect) {
            AspectRatioPreset.SHORTS_9_16 -> 540 to 960
            AspectRatioPreset.LANDSCAPE_16_9 -> 960 to 540
            AspectRatioPreset.SQUARE_1_1 -> 640 to 640
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Subtle frosted edge vignette & smoked glass rim
        val margin = (minOf(w, h) * 0.042f)
        val radius = minOf(w, h) * 0.055f
        val innerRect = RectF(margin, margin, w - margin, h - margin)

        val outerPath = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(0f, 0f, w.toFloat(), h.toFloat(), Path.Direction.CW)
            addRoundRect(innerRect, radius, radius, Path.Direction.CW)
        }
        val frostedRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                w.toFloat(),
                h.toFloat(),
                intArrayOf(
                    Color.argb((125 * intensity).toInt(), 56, 189, 248),
                    Color.argb((145 * intensity).toInt(), 11, 14, 20),
                    Color.argb((110 * intensity).toInt(), 245, 158, 11)
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(outerPath, frostedRimPaint)

        // 2. Specular glass inner bezel stroke
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            shader = LinearGradient(
                margin,
                margin,
                w - margin,
                h - margin,
                intArrayOf(
                    Color.argb((215 * intensity).toInt(), 255, 255, 255),
                    Color.argb((170 * intensity).toInt(), 56, 189, 248),
                    Color.argb((130 * intensity).toInt(), 255, 255, 255)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(innerRect, radius, radius, borderPaint)

        // 3. Diagonal studio glass sheen reflection across top-left
        val sheenPath = Path().apply {
            moveTo(margin, margin + h * 0.24f)
            lineTo(margin + w * 0.42f, margin)
            lineTo(margin + w * 0.62f, margin)
            lineTo(margin, margin + h * 0.38f)
            close()
        }
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                margin,
                margin,
                margin + w * 0.45f,
                margin + h * 0.35f,
                Color.argb((78 * intensity).toInt(), 224, 242, 254),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(innerRect, radius, radius, Path.Direction.CW) })
        canvas.drawPath(sheenPath, sheenPaint)
        canvas.restore()

        return bitmap
    }

    fun computeExportDimensions(
        aspect: AspectRatioPreset,
        resolution: ExportResolution,
        sourceWidth: Int,
        sourceHeight: Int
    ): Pair<Int, Int> {
        val baseShort = when (resolution) {
            ExportResolution.HD_1080P -> 1080
            ExportResolution.HD_720P -> 720
            ExportResolution.SOURCE -> minOf(sourceWidth, sourceHeight).coerceIn(720, 1080)
        }
        val rawPair = when (aspect) {
            AspectRatioPreset.SHORTS_9_16 -> baseShort to (baseShort * 16 / 9)
            AspectRatioPreset.LANDSCAPE_16_9 -> (baseShort * 16 / 9) to baseShort
            AspectRatioPreset.SQUARE_1_1 -> baseShort to baseShort
        }
        val evenW = (rawPair.first / 2) * 2
        val evenH = (rawPair.second / 2) * 2
        return evenW to evenH
    }
}
