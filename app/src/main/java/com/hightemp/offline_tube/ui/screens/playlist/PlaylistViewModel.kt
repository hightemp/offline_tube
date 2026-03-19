package com.hightemp.offline_tube.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hightemp.offline_tube.domain.model.PlaybackPosition
import com.hightemp.offline_tube.domain.model.Video
import com.hightemp.offline_tube.domain.repository.VideoRepository
import com.hightemp.offline_tube.domain.usecase.GetPlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val getPlaylistUseCase: GetPlaylistUseCase,
    private val videoRepository: VideoRepository
) : ViewModel() {

    val videos: StateFlow<List<Video>> = getPlaylistUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _playbackPositions = MutableStateFlow<Map<String, PlaybackPosition>>(emptyMap())
    val playbackPositions: StateFlow<Map<String, PlaybackPosition>> = _playbackPositions.asStateFlow()

    init {
        loadPlaybackPositions()
    }

    private fun loadPlaybackPositions() {
        viewModelScope.launch {
            // Refresh positions whenever video list changes
            videos.collect { videoList ->
                val positions = mutableMapOf<String, PlaybackPosition>()
                videoList.forEach { video ->
                    videoRepository.getPlaybackPosition(video.videoId)?.let { pos ->
                        positions[video.videoId] = pos
                    }
                }
                _playbackPositions.value = positions
            }
        }
    }

    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            Timber.d("PlaylistViewModel: deleting video %s", videoId)
            videoRepository.deleteVideo(videoId)
        }
    }
}
