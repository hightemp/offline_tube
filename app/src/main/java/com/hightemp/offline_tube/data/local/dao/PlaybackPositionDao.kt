package com.hightemp.offline_tube.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hightemp.offline_tube.data.local.entity.PlaybackPositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackPositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(position: PlaybackPositionEntity)

    @Query("SELECT * FROM playback_positions WHERE videoId = :videoId")
    suspend fun getByVideoId(videoId: String): PlaybackPositionEntity?

    @Query("SELECT * FROM playback_positions WHERE videoId = :videoId")
    fun observeByVideoId(videoId: String): Flow<PlaybackPositionEntity?>

    @Query("DELETE FROM playback_positions WHERE videoId = :videoId")
    suspend fun delete(videoId: String)
}
