package com.hightemp.offline_tube.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hightemp.offline_tube.ui.screens.download.DownloadScreen
import com.hightemp.offline_tube.ui.screens.player.PlayerScreen
import com.hightemp.offline_tube.ui.screens.playlist.PlaylistScreen
import com.hightemp.offline_tube.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Download.route,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(Screen.Download.route) {
                DownloadScreen()
            }

            composable(Screen.Playlist.route) {
                PlaylistScreen(
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("videoId") { type = NavType.StringType }
                )
            ) {
                PlayerScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on Player screen
    val showBottomBar = currentDestination?.route?.startsWith("player") != true

    if (showBottomBar) {
        NavigationBar {
            Screen.bottomNavItems.forEach { screen ->
                NavigationBarItem(
                    icon = {
                        screen.icon?.let { Icon(it, contentDescription = screen.title) }
                    },
                    label = { Text(screen.title) },
                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
