package com.hightemp.offline_tube.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.hightemp.offline_tube.R
import com.hightemp.offline_tube.data.local.dao.DownloadDao
import com.hightemp.offline_tube.data.local.dao.VideoDao
import com.hightemp.offline_tube.domain.model.DownloadStatus
import com.hightemp.offline_tube.domain.repository.DownloadRepository
import com.hightemp.offline_tube.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadRepository: DownloadRepository,
    private val settingsRepository: SettingsRepository,
    private val videoDao: VideoDao,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "download_worker"
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001

        private const val PROGRESS_UPDATE_INTERVAL_BYTES = 64 * 1024L // Update every 64KB
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Timber.d("DownloadWorker: doWork started")

        try {
            createNotificationChannel()

            val task = downloadRepository.getNextPendingDownload()
            if (task == null) {
                Timber.d("DownloadWorker: no pending downloads")
                return@withContext Result.success()
            }

            Timber.d("DownloadWorker: processing download id=%d videoId=%s", task.id, task.videoId)

            val downloadUrl = task.downloadUrl
            if (downloadUrl.isNullOrBlank()) {
                Timber.w("DownloadWorker: no download URL for task id=%d", task.id)
                downloadRepository.updateStatus(task.id, DownloadStatus.FAILED, "Download URL missing")
                return@withContext Result.failure()
            }

            // Mark as downloading
            downloadRepository.updateStatus(task.id, DownloadStatus.DOWNLOADING)

            // Set foreground notification
            setForeground(createForegroundInfo(task.title, 0))

            // Prepare file path
            val downloadPath = settingsRepository.getDownloadPath()
            val dir = File(downloadPath)
            if (!dir.exists()) dir.mkdirs()

            val sanitizedTitle = task.title.replace(Regex("[^a-zA-Z0-9а-яА-ЯёЁ._\\- ]"), "_")
                .take(100)
            val fileName = "${task.videoId}_${sanitizedTitle}.mp4"
            val outputFile = File(dir, fileName)

            Timber.d("DownloadWorker: downloading to %s", outputFile.absolutePath)

            // Download the file
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12.1; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/97.0.4692.56)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.e("DownloadWorker: HTTP error %d for task id=%d", response.code, task.id)
                downloadRepository.updateStatus(task.id, DownloadStatus.FAILED, "HTTP error ${response.code}")
                response.close()
                return@withContext Result.retry()
            }

            val body = response.body
            if (body == null) {
                Timber.e("DownloadWorker: empty response body for task id=%d", task.id)
                downloadRepository.updateStatus(task.id, DownloadStatus.FAILED, "Empty response")
                response.close()
                return@withContext Result.failure()
            }

            val totalBytes = body.contentLength().let {
                if (it > 0) it else task.totalBytes
            }

            var downloadedBytes = 0L
            var lastProgressUpdate = 0L

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            Timber.d("DownloadWorker: cancelled for task id=%d", task.id)
                            downloadRepository.updateStatus(task.id, DownloadStatus.CANCELLED)
                            outputFile.delete()
                            return@withContext Result.failure()
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // Throttle progress updates
                        if (downloadedBytes - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL_BYTES) {
                            lastProgressUpdate = downloadedBytes
                            val progress = if (totalBytes > 0) {
                                ((downloadedBytes.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
                            } else 0
                            downloadRepository.updateProgress(task.id, downloadedBytes, totalBytes, progress)
                            setForeground(createForegroundInfo(task.title, progress))
                        }
                    }
                }
            }

            response.close()

            // Mark as completed
            downloadRepository.updateFilePath(task.id, outputFile.absolutePath)
            downloadRepository.updateProgress(task.id, downloadedBytes, totalBytes, 100)
            downloadRepository.updateStatus(task.id, DownloadStatus.COMPLETED)

            // Mark video as downloaded in video table
            videoDao.markDownloaded(task.videoId, outputFile.absolutePath)

            Timber.d("DownloadWorker: completed download id=%d size=%d path=%s", task.id, downloadedBytes, outputFile.absolutePath)

            // Check if more pending downloads
            val nextTask = downloadRepository.getNextPendingDownload()
            if (nextTask != null) {
                Timber.d("DownloadWorker: more pending downloads, returning retry")
                return@withContext Result.retry()
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "DownloadWorker: error during download")
            val task = downloadRepository.getNextPendingDownload()
            // The task was already marked DOWNLOADING, so find it
            Result.retry()
        }
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Загрузка видео")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Загрузки видео",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о загрузке видео"
            }

            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
