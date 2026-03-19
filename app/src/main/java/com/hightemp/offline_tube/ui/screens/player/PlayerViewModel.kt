package com.hightemp.offline_tube.ui.screens.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hightemp.offline_tube.domain.model.Video
import com.hightemp.offline_tube.domain.repository.VideoRepository
import com.hightemp.offline_tube.domain.usecase.SavePlaybackPositionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class PlayerUiState(
    val video: Video? = null,
    val filePath: String? = null,
    val initialPositionMs: Long = 0L,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val videoRepository: VideoRepository,
    private val savePlaybackPositionUseCase: SavePlaybackPositionUseCase
) : ViewModel() {

    private val videoId: String = savedStateHandle.get<String>("videoId") ?: ""

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        loadVideo()
    }

    private fun loadVideo() {
        viewModelScope.launch {
            try {
                val video = videoRepository.getVideoById(videoId)
                if (video == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Видео не найдено"
                    )
                    return@launch
                }

                val playbackPosition = videoRepository.getPlaybackPosition(videoId)
                val initialPosition = playbackPosition?.positionMs ?: 0L

                Timber.d("PlayerViewModel: loaded video %s, resuming from %dms", video.title, initialPosition)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    video = video,
                    filePath = video.filePath,
                    initialPositionMs = initialPosition
                )
            } catch (e: Exception) {
                Timber.e(e, "PlayerViewModel: error loading video %s", videoId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Ошибка загрузки видео"
                )
            }
        }
    }

    fun savePosition(positionMs: Long, durationMs: Long) {
        if (videoId.isBlank() || positionMs < 0) return

        viewModelScope.launch {
            try {
                savePlaybackPositionUseCase(
                    videoId = videoId,
                    positionMs = positionMs,
                    durationMs = durationMs
                )
            } catch (e: Exception) {
                Timber.e(e, "PlayerViewModel: error saving position")
            }
        }
    }
}
