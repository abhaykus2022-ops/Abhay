package com.example.ai

import android.util.Log
import com.example.data.ChatMessageEntity
import com.example.data.HighlightSuggestion
import com.example.data.VfxPreset
import com.example.data.YouTubeVideoInfo
import com.example.media.SampledFrame
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiStudioService"

class GeminiStudioService {

    private val highlightSchema = Schema.obj(
        properties = mapOf(
            "videoSummary" to Schema.string(
                description = "Concise 1-sentence semantic summary of the video's mood, action, and pacing."
            ),
            "highlights" to Schema.array(
                description = "Ranked viral highlight moments extracted from the video frames and metadata.",
                items = Schema.obj(
                    properties = mapOf(
                        "label" to Schema.string(description = "Short punchy clip title (max 5 words)."),
                        "reason" to Schema.string(description = "Why this moment maximizes retention or viral potential."),
                        "category" to Schema.enumeration(
                            values = listOf("Viral Hook", "Peak Action", "Cinematic Beat", "Story Climax", "Gameplay Clutch"),
                            description = "Best matching highlight category."
                        ),
                        "viralScore" to Schema.integer(description = "Estimated engagement score from 70 to 99."),
                        "startSec" to Schema.double(description = "Recommended clip start timestamp in seconds."),
                        "endSec" to Schema.double(description = "Recommended clip end timestamp in seconds."),
                        "recommendedVfx" to Schema.enumeration(
                            values = listOf("GLASS_PRISM", "CINEMA_TEAL", "MIDNIGHT_PRO", "WARM_FILM", "VIVID_HDR", "NOIR", "NATURAL"),
                            description = "Cinematic color/glass grade preset matching the scene."
                        ),
                        "hookCaption" to Schema.string(description = "Short on-screen hook text (max 6 words) for Shorts.")
                    )
                )
            )
        )
    )

    private val analyzerModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-4-hydrogen",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = highlightSchema
            }
        )
    }

    private val quickHookModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.1-flash-lite"
        )
    }

    private val chatStandardModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-4-hydrogen",
            systemInstruction = content {
                text(DIRECTOR_SYSTEM_INSTRUCTION)
            }
        )
    }

    private val chatHighThinkingModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-4-carbon",
            generationConfig = generationConfig {
                thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            },
            systemInstruction = content {
                text(DIRECTOR_SYSTEM_INSTRUCTION)
            }
        )
    }

    data class SemanticAnalysisResult(
        val summary: String,
        val suggestions: List<HighlightSuggestion>
    )

    suspend fun analyzeVideoForHighlights(
        frames: List<SampledFrame>,
        durationMs: Long,
        existingClipCount: Int,
        userFocusPrompt: String = ""
    ): SemanticAnalysisResult {
        val durationSec = (durationMs / 1000.0).coerceAtLeast(1.0)
        val extraInstruction = if (userFocusPrompt.isNotBlank()) {
            "Creator focus request: $userFocusPrompt."
        } else {
            "Identify the most engaging, high-retention moments suitable for YouTube Shorts (15-55s) or longer narrative cuts if the moment warrants it."
        }

        val response = withTimeout(60_000L) {
            analyzerModel.generateContent(
                content {
                    text(
                        """
                        You are an elite viral video editor and semantic content analyst.
                        Total video duration: ${String.format("%.1f", durationSec)} seconds.
                        Already generated clips: $existingClipCount (propose fresh, distinct highlight windows when possible).
                        $extraInstruction
                        Analyze the sampled frames below (each labeled with its exact timestamp in seconds) and produce 3 to 5 ranked highlight clips strictly within [0.0, ${String.format("%.1f", durationSec)}] seconds, where endSec > startSec + 2.0.
                        """.trimIndent()
                    )
                    frames.forEach { frame ->
                        text("Frame at ${String.format("%.1f", frame.timestampMs / 1000.0)}s:")
                        image(frame.bitmap)
                    }
                }
            )
        }
        return parseAnalysisResponse(response.text, durationMs)
    }

    suspend fun analyzeYouTubeVideoForHighlights(
        info: YouTubeVideoInfo,
        frames: List<SampledFrame>,
        existingClipCount: Int,
        userFocusPrompt: String = ""
    ): SemanticAnalysisResult {
        val durationSec = (info.durationMs / 1000.0).coerceAtLeast(5.0)
        val extraInstruction = if (userFocusPrompt.isNotBlank()) {
            "Creator focus request: $userFocusPrompt."
        } else {
            "Generate 4 high-retention YouTube Shorts or highlight clip segments (15-55s each, or longer if narrative requires)."
        }

        val response = withTimeout(60_000L) {
            analyzerModel.generateContent(
                content {
                    text(
                        """
                        You are an elite viral video editor and YouTube Shorts clip strategist.
                        YouTube Video Title: ${info.title}
                        Creator/Channel: ${info.channelAuthor}
                        Video URL: ${info.watchUrl}
                        Total video duration: ${String.format("%.1f", durationSec)} seconds.
                        Already generated clips: $existingClipCount (propose fresh, distinct highlight windows when possible).
                        Description / Chapters context:
                        ${info.descriptionSnippet.ifBlank { "No chapters provided; infer dynamic hooks, peak moments, and climax beats from visual frames and duration." }}
                        $extraInstruction
                        Analyze the metadata and storyboard frames below and produce 3 to 5 ranked highlight clips strictly within [0.0, ${String.format("%.1f", durationSec)}] seconds, where endSec > startSec + 2.0. Recommend GLASS_PRISM when a modern frosted glass studio border fits well.
                        """.trimIndent()
                    )
                    frames.forEach { frame ->
                        text("Storyboard frame near ${String.format("%.1f", frame.timestampMs / 1000.0)}s:")
                        image(frame.bitmap)
                    }
                }
            )
        }
        return parseAnalysisResponse(response.text, info.durationMs)
    }

    private fun parseAnalysisResponse(rawText: String?, durationMs: Long): SemanticAnalysisResult {
        val jsonText = rawText
            ?: throw IllegalStateException("Gemini returned an empty analysis response.")

        val json = JSONObject(jsonText)
        val summary = json.optString("videoSummary", "Semantic highlight scan completed.")
        val array = json.getJSONArray("highlights")
        val suggestions = mutableListOf<HighlightSuggestion>()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val rawStartMs = (item.optDouble("startSec", 0.0) * 1000.0).toLong()
            val rawEndMs = (item.optDouble("endSec", rawStartMs / 1000.0 + 15.0) * 1000.0).toLong()
            val clampedStart = rawStartMs.coerceIn(0L, (durationMs - 1_500L).coerceAtLeast(0L))
            val clampedEnd = rawEndMs.coerceIn(
                (clampedStart + 1_500L).coerceAtMost(durationMs),
                durationMs.coerceAtLeast(1_500L)
            )
            suggestions.add(
                HighlightSuggestion(
                    label = item.optString("label", "Highlight #${i + 1}").take(42),
                    reason = item.optString("reason", "High visual & pacing energy detected."),
                    category = item.optString("category", "Viral Hook"),
                    viralScore = item.optInt("viralScore", 88).coerceIn(60, 99),
                    startMs = clampedStart,
                    endMs = clampedEnd,
                    recommendedVfx = VfxPreset.fromId(item.optString("recommendedVfx")),
                    hookCaption = item.optString("hookCaption", "")
                )
            )
        }

        if (suggestions.isEmpty()) {
            throw IllegalStateException("No highlight segments were returned.")
        }
        return SemanticAnalysisResult(summary = summary, suggestions = suggestions)
    }

    suspend fun generateQuickHookCaption(
        clipLabel: String,
        clipReason: String,
        aspectRatioLabel: String
    ): String {
        val response = withTimeout(30_000L) {
            quickHookModel.generateContent(
                content {
                    text(
                        "Write ONE punchy 3-to-6 word on-screen hook caption for a $aspectRatioLabel video clip titled '$clipLabel'. Context: $clipReason. Return ONLY the caption text without quotes."
                    )
                }
            )
        }
        return response.text?.trim()?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Could not generate a hook caption.")
    }

    fun streamDirectorReply(
        history: List<ChatMessageEntity>,
        userPrompt: String,
        projectContextSummary: String,
        useHighThinking: Boolean
    ): Flow<String> {
        val model = if (useHighThinking) chatHighThinkingModel else chatStandardModel

        val chatHistory = history.takeLast(14).map { msg ->
            content(role = if (msg.role == "user") "user" else "model") {
                text(msg.content)
            }
        }
        val chatSession = model.startChat(history = chatHistory)
        val fullMessage = buildString {
            if (projectContextSummary.isNotBlank()) {
                append("[Active Studio Context: $projectContextSummary]\n\n")
            }
            append(userPrompt)
        }

        return chatSession.sendMessageStream(fullMessage)
            .mapNotNull { chunk -> chunk.text }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "Director stream failed (highThinking=$useHighThinking)", error)
                throw error
            }
    }

    companion object {
        private const val DIRECTOR_SYSTEM_INSTRUCTION = """
            You are SlateCut AI Director, an expert YouTube Shorts & long-form video editor, colorist, and viral retention strategist.
            Help creators solve editing problems cleanly and practically:
            - Suggest exact cut timings, pacing fixes, visual hook lines, and aspect ratio framing (9:16 Shorts vs 16:9 HD).
            - Recommend color grading & glass effects (Frosted Glass, Cinema Teal, Midnight Pro, Warm Reel, Vivid Pulse, Silver Noir), dynamic zoom, and playback speed curves.
            - Keep answers structured, concise, actionable, and tailored to the active project context when provided.
        """
    }
}
