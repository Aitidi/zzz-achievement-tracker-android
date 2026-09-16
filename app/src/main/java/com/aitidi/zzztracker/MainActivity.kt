package com.aitidi.zzztracker

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.aitidi.zzztracker.ui.TrackerApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEdgeToEdge()
        setContent {
            TrackerApp()
        }
    }

    private fun configureEdgeToEdge() {
        // Activity.enableEdgeToEdge() embeds deprecated SHORT_EDGES in its API 28/29 path.
        // The theme supplies transparent system bars on Android 14 and earlier.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        // Android 9/10 retain the default cutout mode rather than deprecated SHORT_EDGES.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}
