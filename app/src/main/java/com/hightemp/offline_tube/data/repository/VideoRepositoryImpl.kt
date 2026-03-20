package com.hightemp.offline_tube.data.repository

import com.hightemp.offline_tube.data.local.dao.PlaybackPositionDao
import com.hightemp.offline_tube.data.local.dao.VideoDao
import com.hightemp.offline_tube.data.mapper.DownloadMapper
import com.hightemp.offline_tube.data.mapper.VideoMapper
import com.hightemp.offline_tube.data.remote.InnerTubeApi
import com.hightemp.offline_tube.data.remote.InnerTubeConfig
import com.hightemp.offline_tube.domain.model.PlaybackPosition
import com.hightemp.offline_tube.domain.model.Video
import com.hightemp.offline_tube.domain.model.YouTubeError
import com.hightemp.offline_tube.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val innerTubeApi: InnerTubeApi,
    private val videoDao: VideoDao,
    private val playbackPositionDao: PlaybackPositionDao,
    private val videoMapper: VideoMapper,
    private val downloadMapper: DownloadMapper
) : VideoRepository {

    override suspend fun extractVideoInfo(videoId: String): Result<Video> = withContext(Dispatchers.IO) {
        Timber.d("VideoRepositoryImpl: extractVideoInfo videoId=%s", videoId)
        try {
            val response = innerTubeApi.getPlayerResponse(videoId)

            val status = response.playabilityStatus?.status
            if (status !in InnerTubeConfig.ACCEPTABLE_STATUSES) {
                val reason = response.playabilityStatus?.reason ?: "Unknown error"
                Timber.w("VideoRepositoryImpl: video unavailable status=%s reason=%s", status, reason)
                return@withContext Result.failure(YouTubeError.VideoUnavailable(reason))
            }

            val video = videoMapper.fromResponse(response)
            if (video.formats.isEmpty()) {
                Timber.w("VideoRepositoryImpl: no formats for videoId=%s", videoId)
                return@withContext Result.failure(YouTubeError.NoFormatsAvailable(videoId))
            }

            // Cache metadata in DB
            videoDao.insert(videoMapper.toEntity(video))
            Timber.d("VideoRepositoryImpl: cached video metadata videoId=%s formats=%d", videoId, video.formats.size)

            Result.success(video)
        } catch (e: IOException) {
            Timber.e(e, "VideoRepositoryImpl: network error for videoId=%s", videoId)
            Result.failure(YouTubeError.NetworkError(e))
        } catch (e: Exception) {
            Timber.e(e, "VideoRepositoryImpl: parsing error for videoId=%s", videoId)
            Result.failure(YouTubeError.ParsingError)
        }
    }

    override fun observeDownloadedVideos(): Flow<List<Video>> {
        Timber.d("VideoRepositoryImpl: observeDownloadedVideos")
        return videoDao.observeDownloaded().map { entities ->
            entities.map { videoMapper.fromEntity(it) }
        }
    }

    override suspend fun getVideoById(videoId: String): Video? = withContext(Dispatchers.IO) {
        videoDao.getById(videoId)?.let { videoMapper.fromEntity(it) }
    }

    override suspend fun deleteVideo(videoId: String) = withContext(Dispatchers.IO) {
        Timber.d("VideoRepositoryImpl: deleteVideo videoId=%s", videoId)
        val entity = videoDao.getById(videoId)
        entity?.filePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                Timber.d("VideoRepositoryImpl: deleting file path=%s", path)
                file.delete()
            }
        }
        playbackPositionDao.delete(videoId)
        videoDao.delete(videoId)
    }

    override suspend fun savePlaybackPosition(position: PlaybackPosition) = withContext(Dispatchers.IO) {
        Timber.d("VideoRepositoryImpl: savePlaybackPosition videoId=%s pos=%d", position.videoId, position.positionMs)
        playbackPositionDao.insert(downloadMapper.toPositionEntity(position))
    }

    override suspend fun getPlaybackPosition(videoId: String): PlaybackPosition? = withContext(Dispatchers.IO) {
        playbackPositionDao.getByVideoId(videoId)?.let { downloadMapper.fromPositionEntity(it) }
    }

    override fun observePlaybackPosition(videoId: String): Flow<PlaybackPosition?> {
        return playbackPositionDao.observeByVideoId(videoId).map { entity ->
            entity?.let { downloadMapper.fromPositionEntity(it) }
        }
    }
}
