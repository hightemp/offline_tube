package com.hightemp.offline_tube.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hightemp.offline_tube.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity)

    @Query("SELECT * FROM videos WHERE videoId = :videoId")
    suspend fun getById(videoId: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE isDownloaded = 1 ORDER BY createdAt DESC")
    fun observeDownloaded(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDownloaded = 1 ORDER BY createdAt DESC")
    suspend fun getDownloaded(): List<VideoEntity>

    @Query("UPDATE videos SET isDownloaded = 1, filePath = :filePath WHERE videoId = :videoId")
    suspend fun markDownloaded(videoId: String, filePath: String)

    @Query("DELETE FROM videos WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("UPDATE videos SET filePath = null, isDownloaded = 0 WHERE videoId = :videoId")
    suspend fun markNotDownloaded(videoId: String)
}
