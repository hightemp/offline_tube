package com.hightemp.offline_tube.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hightemp.offline_tube.domain.model.VideoQuality
import com.hightemp.offline_tube.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val videoQuality: StateFlow<VideoQuality> = settingsRepository
        .observeVideoQuality()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VideoQuality.Q_720P)

    val wifiOnly: StateFlow<Boolean> = settingsRepository
        .observeWifiOnly()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val downloadPath: StateFlow<String> = settingsRepository
        .observeDownloadPath()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val maxConcurrentDownloads: StateFlow<Int> = settingsRepository
        .observeMaxConcurrentDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val autoRotatePlayer: StateFlow<Boolean> = settingsRepository
        .observeAutoRotatePlayer()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setVideoQuality(quality: VideoQuality) {
        viewModelScope.launch {
            Timber.d("SettingsViewModel: setVideoQuality %s", quality.label)
            settingsRepository.setVideoQuality(quality)
        }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWifiOnly(enabled)
        }
    }

    fun setMaxConcurrentDownloads(count: Int) {
        viewModelScope.launch {
            settingsRepository.setMaxConcurrentDownloads(count)
        }
    }

    fun setAutoRotatePlayer(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoRotatePlayer(enabled)
        }
    }
}
