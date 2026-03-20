package com.hightemp.offline_tube.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hightemp.offline_tube.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE videoId = :videoId")
    suspend fun getByVideoId(videoId: String): DownloadEntity?

    @Query("UPDATE downloads SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, downloadedBytes: Long, totalBytes: Long, progress: Int)

    @Query("UPDATE downloads SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String? = null)

    @Query("UPDATE downloads SET filePath = :filePath WHERE id = :id")
    suspend fun updateFilePath(id: Long, filePath: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM downloads WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT 1")
    suspend fun getNextPending(): DownloadEntity?

    @Query("UPDATE downloads SET downloadUrl = :downloadUrl WHERE id = :id")
    suspend fun updateDownloadUrl(id: Long, downloadUrl: String)

    @Query("UPDATE downloads SET status = 'PENDING', errorMessage = NULL WHERE status = 'DOWNLOADING'")
    suspend fun resetStuckDownloads()
}
