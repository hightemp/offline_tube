package com.hightemp.offline_tube.ui.screens.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hightemp.offline_tube.data.worker.DownloadWorker
import com.hightemp.offline_tube.domain.model.DownloadStatus
import com.hightemp.offline_tube.domain.model.DownloadTask
import com.hightemp.offline_tube.domain.model.Video
import com.hightemp.offline_tube.domain.model.VideoQuality
import com.hightemp.offline_tube.domain.repository.DownloadRepository
import com.hightemp.offline_tube.domain.repository.SettingsRepository
import com.hightemp.offline_tube.domain.usecase.DownloadVideoUseCase
import com.hightemp.offline_tube.domain.usecase.ExtractVideoInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class DownloadUiState(
    val url: String = "",
    val isLoading: Boolean = false,
    val videoInfo: Video? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val extractVideoInfoUseCase: ExtractVideoInfoUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val downloadRepository: DownloadRepository,
    private val settingsRepository: SettingsRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    val downloadQueue: StateFlow<List<DownloadTask>> = downloadRepository
        .observeQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedQuality: StateFlow<VideoQuality> = settingsRepository
        .observeVideoQuality()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VideoQuality.Q_720P)

    fun onUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(url = url, errorMessage = null)
    }

    fun extractVideoInfo() {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Введите ссылку на видео")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, videoInfo = null, successMessage = null)

            try {
                val result = extractVideoInfoUseCase(url)
                result.fold(
                    onSuccess = { video ->
                        Timber.d("DownloadViewModel: extracted info for %s", video.title)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            videoInfo = video
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "DownloadViewModel: extraction failed")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Не удалось получить информацию о видео"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "DownloadViewModel: unexpected error")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Произошла ошибка"
                )
            }
        }
    }

    fun startDownload(video: Video? = null) {
        val videoToDownload = video ?: _uiState.value.videoInfo
        if (videoToDownload == null) {
            Timber.w("DownloadViewModel: startDownload called but no video info available")
            _uiState.value = _uiState.value.copy(errorMessage = "Сначала получите информацию о видео")
            return
        }

        Timber.d("DownloadViewModel: startDownload videoId=%s title=%s formats=%d",
            videoToDownload.videoId, videoToDownload.title, videoToDownload.formats.size)
        videoToDownload.formats.forEachIndexed { i, fmt ->
            Timber.d("DownloadViewModel: format[%d] itag=%d %s %dx%d hasAudio=%b hasVideo=%b url=%s",
                i, fmt.itag, fmt.mimeType, fmt.width ?: 0, fmt.height ?: 0,
                fmt.hasAudio, fmt.hasVideo, fmt.url.take(80))
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val result = downloadVideoUseCase(videoToDownload)
                result.fold(
                    onSuccess = {
                        Timber.d("DownloadViewModel: download enqueued id=%d", it)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            videoInfo = null,
                            url = "",
                            successMessage = "Видео добавлено в очередь загрузки"
                        )
                        enqueueDownloadWork()
                    },
                    onFailure = { error ->
                        Timber.e(error, "DownloadViewModel: download enqueue failed")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Не удалось добавить загрузку"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "DownloadViewModel: unexpected error during download start")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Произошла ошибка"
                )
            }
        }
    }

    fun cancelDownload(taskId: Long) {
        viewModelScope.launch {
            downloadRepository.cancelDownload(taskId)
        }
    }

    fun retryDownload(taskId: Long) {
        viewModelScope.launch {
            downloadRepository.retryDownload(taskId)
            enqueueDownloadWork()
        }
    }

    fun removeDownload(taskId: Long) {
        viewModelScope.launch {
            downloadRepository.removeDownload(taskId)
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    private fun enqueueDownloadWork() {
        Timber.d("DownloadViewModel: enqueueDownloadWork called")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            DownloadWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
        Timber.d("DownloadViewModel: enqueued download work with REPLACE policy")
    }
}
