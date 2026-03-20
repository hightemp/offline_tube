package com.hightemp.offline_tube.data.repository

import com.hightemp.offline_tube.data.local.dao.DownloadDao
import com.hightemp.offline_tube.data.local.entity.DownloadEntity
import com.hightemp.offline_tube.data.mapper.DownloadMapper
import com.hightemp.offline_tube.domain.model.DownloadStatus
import com.hightemp.offline_tube.domain.model.DownloadTask
import com.hightemp.offline_tube.domain.model.VideoQuality
import com.hightemp.offline_tube.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
    private val downloadMapper: DownloadMapper
) : DownloadRepository {

    override suspend fun enqueueDownload(
        videoId: String,
        title: String,
        thumbnailUrl: String,
        downloadUrl: String,
        quality: VideoQuality,
        totalBytes: Long
    ): Long = withContext(Dispatchers.IO) {
        Timber.d("DownloadRepositoryImpl: enqueueDownload videoId=%s quality=%s", videoId, quality)
        val entity = DownloadEntity(
            videoId = videoId,
            title = title,
            thumbnailUrl = thumbnailUrl,
            status = DownloadStatus.PENDING.name,
            selectedQuality = quality.label,
            totalBytes = totalBytes,
            downloadUrl = downloadUrl
        )
        downloadDao.insert(entity)
    }

    override fun observeQueue(): Flow<List<DownloadTask>> {
        Timber.d("DownloadRepositoryImpl: observeQueue")
        return downloadDao.observeAll().map { entities ->
            entities.map { downloadMapper.fromEntity(it) }
        }
    }

    override fun observeActiveDownloads(): Flow<List<DownloadTask>> {
        return downloadDao.observeActive().map { entities ->
            entities.map { downloadMapper.fromEntity(it) }
        }
    }

    override suspend fun getDownloadById(id: Long): DownloadTask? = withContext(Dispatchers.IO) {
        downloadDao.getById(id)?.let { downloadMapper.fromEntity(it) }
    }

    override suspend fun getDownloadByVideoId(videoId: String): DownloadTask? = withContext(Dispatchers.IO) {
        downloadDao.getByVideoId(videoId)?.let { downloadMapper.fromEntity(it) }
    }

    override suspend fun updateProgress(id: Long, downloadedBytes: Long, totalBytes: Long, progress: Int) = withContext(Dispatchers.IO) {
        Timber.d("DownloadRepositoryImpl: updateProgress id=%d %d/%d (%d%%)", id, downloadedBytes, totalBytes, progress)
        downloadDao.updateProgress(id, downloadedBytes, totalBytes, progress)
    }

    override suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String?) = withContext(Dispatchers.IO) {
        Timber.d("DownloadRepositoryImpl: updateStatus id=%d status=%s error=%s", id, status, errorMessage)
        downloadDao.updateStatus(id, status.name, errorMessage)
    }

    override suspend fun updateFilePath(id: Long, filePath: String) = withContext(Dispatchers.IO) {
        Timber.d("DownloadRepositoryImpl: updateFilePath id=%d path=%s", id, filePath)
        downloadDao.updateFilePath(id, filePath)
    }

    override suspend fun cancelDownload(id: Long) = withContext(Dispatchers.IO) {
        Timber.d("DownloadRepositoryImpl: cancelDownload id=%d", id)
        downloadDao.updateStatus(id, DownloadStatus.CANCELLED.name, null)
    }

    override suspend fun retryDownload(id: Long) = withContext(Dispatchers.IO) {
        Timber.d("DownloadRepositoryImpl: retryDownload id=%d", id)
        downloadDao.updateStatus(id, DownloadStatus.PENDING.name, null)
    }

    override suspend fun removeDownload(id: Long) = withContext(Dispatchers.IO) {
        Timber.d("DownloadRepositoryImpl: removeDownload id=%d", id)
        downloadDao.delete(id)
    }

    override suspend fun getNextPendingDownload(): DownloadTask? = withContext(Dispatchers.IO) {
        downloadDao.getNextPending()?.let { downloadMapper.fromEntity(it) }
    }

    override suspend fun updateDownloadUrl(id: Long, downloadUrl: String) = withContext(Dispatchers.IO) {
        Timber.d("DownloadRepositoryImpl: updateDownloadUrl id=%d url=%s", id, downloadUrl.take(80))
        downloadDao.updateDownloadUrl(id, downloadUrl)
    }

    override suspend fun resetStuckDownloads() = withContext(Dispatchers.IO) {
        Timber.d("DownloadRepositoryImpl: resetStuckDownloads")
        downloadDao.resetStuckDownloads()
    }
}
