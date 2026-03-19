package com.hightemp.offline_tube.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    data object Download : Screen(
        route = "download",
        title = "Загрузка",
        icon = Icons.Default.Download
    )

    data object Playlist : Screen(
        route = "playlist",
        title = "Плейлист",
        icon = Icons.AutoMirrored.Filled.PlaylistPlay
    )

    data object Settings : Screen(
        route = "settings",
        title = "Настройки",
        icon = Icons.Default.Settings
    )

    data object Player : Screen(
        route = "player/{videoId}",
        title = "Плеер"
    ) {
        fun createRoute(videoId: String) = "player/$videoId"
    }

    companion object {
        val bottomNavItems = listOf(Download, Playlist, Settings)
    }
}
