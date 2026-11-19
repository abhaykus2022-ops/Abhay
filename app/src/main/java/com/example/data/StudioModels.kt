package com.example.data

enum class ProjectSourceType(val id: String) {
    LOCAL_VIDEO("LOCAL_VIDEO"),
    YOUTUBE_LINK("YOUTUBE_LINK");

    companion object {
        fun fromId(id: String?): ProjectSourceType =
            entries.firstOrNull { it.id == id } ?: LOCAL_VIDEO
    }
}

enum class AspectRatioPreset(
    val id: String,
    val label: String,
    val ratioFloat: Float,
    val targetWidth: Int,
    val targetHeight: Int,
    val badge: String
) {
    SHORTS_9_16("SHORTS_9_16", "9:16 Shorts", 9f / 16f, 1080, 1920, "Shorts"),
    LANDSCAPE_16_9("LANDSCAPE_16_9", "16:9 HD", 16f / 9f, 1920, 1080, "YouTube"),
    SQUARE_1_1("SQUARE_1_1", "1:1 Square", 1f, 1080, 1080, "Feed");

    companion object {
        fun fromId(id: String?): AspectRatioPreset =
            entries.firstOrNull { it.id == id } ?: SHORTS_9_16
    }
}

enum class VfxPreset(
    val id: String,
    val displayName: String,
    val tagline: String,
    val defaultBrightness: Float,
    val defaultContrast: Float,
    val defaultSaturation: Float,
    val defaultZoom: Float,
    val defaultGlass: Float,
    val redScale: Float,
    val greenScale: Float,
    val blueScale: Float,
    val hueShift: Float
) {
    NATURAL(
        id = "NATURAL",
        displayName = "Original",
        tagline = "Clean source color",
        defaultBrightness = 0f,
        defaultContrast = 0f,
        defaultSaturation = 0f,
        defaultZoom = 0f,
        defaultGlass = 0f,
        redScale = 1f,
        greenScale = 1f,
        blueScale = 1f,
        hueShift = 0f
    ),
    GLASS_PRISM(
        id = "GLASS_PRISM",
        displayName = "Frosted Glass",
        tagline = "Specular glass frame & glow",
        defaultBrightness = 0.04f,
        defaultContrast = 0.18f,
        defaultSaturation = 14f,
        defaultZoom = 0.06f,
        defaultGlass = 0.78f,
        redScale = 0.98f,
        greenScale = 1.03f,
        blueScale = 1.12f,
        hueShift = -2f
    ),
    CINEMA_TEAL(
        id = "CINEMA_TEAL",
        displayName = "Cinema Teal",
        tagline = "Teal & amber filmic grade",
        defaultBrightness = 0.02f,
        defaultContrast = 0.22f,
        defaultSaturation = 16f,
        defaultZoom = 0.08f,
        defaultGlass = 0.20f,
        redScale = 1.05f,
        greenScale = 0.99f,
        blueScale = 1.12f,
        hueShift = -4f
    ),
    MIDNIGHT_PRO(
        id = "MIDNIGHT_PRO",
        displayName = "Midnight Pro",
        tagline = "Deep crisp studio contrast",
        defaultBrightness = -0.04f,
        defaultContrast = 0.28f,
        defaultSaturation = 8f,
        defaultZoom = 0.06f,
        defaultGlass = 0.30f,
        redScale = 0.96f,
        greenScale = 1.0f,
        blueScale = 1.10f,
        hueShift = 0f
    ),
    WARM_FILM(
        id = "WARM_FILM",
        displayName = "Warm Reel",
        tagline = "Golden highlight warmth",
        defaultBrightness = 0.04f,
        defaultContrast = 0.14f,
        defaultSaturation = 12f,
        defaultZoom = 0.05f,
        defaultGlass = 0.15f,
        redScale = 1.12f,
        greenScale = 1.03f,
        blueScale = 0.88f,
        hueShift = 6f
    ),
    VIVID_HDR(
        id = "VIVID_HDR",
        displayName = "Vivid Pulse",
        tagline = "High-pop gaming & action",
        defaultBrightness = 0.05f,
        defaultContrast = 0.25f,
        defaultSaturation = 34f,
        defaultZoom = 0.10f,
        defaultGlass = 0.25f,
        redScale = 1.06f,
        greenScale = 1.04f,
        blueScale = 1.08f,
        hueShift = 0f
    ),
    NOIR(
        id = "NOIR",
        displayName = "Silver Noir",
        tagline = "Dramatic monochrome",
        defaultBrightness = 0.0f,
        defaultContrast = 0.35f,
        defaultSaturation = -100f,
        defaultZoom = 0.05f,
        defaultGlass = 0.35f,
        redScale = 1f,
        greenScale = 1f,
        blueScale = 1f,
        hueShift = 0f
    );

    companion object {
        fun fromId(id: String?): VfxPreset =
            entries.firstOrNull { it.id == id } ?: GLASS_PRISM
    }
}

enum class ExportResolution(
    val id: String,
    val label: String,
    val shortDimensionPx: Int,
    val description: String
) {
    HD_1080P("HD_1080P", "1080p Full HD", 1080, "Crisp YouTube Shorts & 1080p master"),
    HD_720P("HD_720P", "720p Fast HD", 720, "Quick render & smaller file size"),
    SOURCE("SOURCE", "Source Quality", 1080, "Match original sharpness");

    companion object {
        fun fromId(id: String?): ExportResolution =
            entries.firstOrNull { it.id == id } ?: HD_1080P
    }
}

data class VideoMetadataInfo(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val title: String,
    val thumbnailPath: String?
)

data class YouTubeVideoInfo(
    val videoId: String,
    val watchUrl: String,
    val title: String,
    val channelAuthor: String,
    val durationMs: Long,
    val thumbnailPath: String?,
    val descriptionSnippet: String
)

data class HighlightSuggestion(
    val label: String,
    val reason: String,
    val category: String,
    val viralScore: Int,
    val startMs: Long,
    val endMs: Long,
    val recommendedVfx: VfxPreset,
    val hookCaption: String
)
