package com.example

import android.content.Context
import com.example.ai.GeminiStudioService
import com.example.data.AppDatabase
import com.example.data.StudioRepository
import com.example.media.VideoExportManager
import com.example.media.VideoFrameSampler
import com.example.media.YouTubeLinkResolver

class StudioContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    val repository: StudioRepository by lazy { StudioRepository(database) }
    val frameSampler: VideoFrameSampler by lazy { VideoFrameSampler(appContext) }
    val youtubeResolver: YouTubeLinkResolver by lazy { YouTubeLinkResolver(appContext) }
    val exportManager: VideoExportManager by lazy { VideoExportManager(appContext) }
    val geminiService: GeminiStudioService by lazy { GeminiStudioService() }

    companion object {
        @Volatile
        private var INSTANCE: StudioContainer? = null

        fun get(context: Context): StudioContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StudioContainer(context).also { INSTANCE = it }
            }
        }
    }
}
