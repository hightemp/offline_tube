package com.hightemp.offline_tube.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hightemp.offline_tube.data.local.dao.DownloadDao
import com.hightemp.offline_tube.data.local.dao.PlaybackPositionDao
import com.hightemp.offline_tube.data.local.dao.VideoDao
import com.hightemp.offline_tube.data.local.entity.DownloadEntity
import com.hightemp.offline_tube.data.local.entity.PlaybackPositionEntity
import com.hightemp.offline_tube.data.local.entity.VideoEntity

@Database(
    entities = [
        VideoEntity::class,
        DownloadEntity::class,
        PlaybackPositionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun downloadDao(): DownloadDao
    abstract fun playbackPositionDao(): PlaybackPositionDao
}
