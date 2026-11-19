package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM video_projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<VideoProjectEntity>>

    @Query("SELECT * FROM video_projects WHERE id = :projectId")
    fun observeProject(projectId: Long): Flow<VideoProjectEntity?>

    @Query("SELECT * FROM video_projects WHERE id = :projectId")
    suspend fun getProject(projectId: Long): VideoProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProjectEntity): Long

    @Update
    suspend fun updateProject(project: VideoProjectEntity)

    @Query("DELETE FROM video_projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)
}

@Dao
interface HighlightClipDao {
    @Query("SELECT * FROM highlight_clips WHERE projectId = :projectId ORDER BY createdAt DESC, viralScore DESC")
    fun observeClipsForProject(projectId: Long): Flow<List<HighlightClipEntity>>

    @Query("SELECT * FROM highlight_clips WHERE projectId = :projectId ORDER BY startMs ASC")
    suspend fun getClipsForProject(projectId: Long): List<HighlightClipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: HighlightClipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<HighlightClipEntity>): List<Long>

    @Update
    suspend fun updateClip(clip: HighlightClipEntity)

    @Query("DELETE FROM highlight_clips WHERE id = :clipId")
    suspend fun deleteClipById(clipId: Long)
}

@Dao
interface ExportRecordDao {
    @Query("SELECT * FROM export_records ORDER BY exportedAt DESC")
    fun observeAllExports(): Flow<List<ExportRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(record: ExportRecordEntity): Long

    @Query("DELETE FROM export_records WHERE id = :id")
    suspend fun deleteExportById(id: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun observeAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    suspend fun getAllMessages(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}
