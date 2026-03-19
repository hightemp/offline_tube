package com.hightemp.offline_tube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hightemp.offline_tube.ui.navigation.AppNavigation
import com.hightemp.offline_tube.ui.theme.Offline_tubeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Offline_tubeTheme {
                AppNavigation()
            }
        }
    }
}