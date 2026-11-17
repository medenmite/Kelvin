package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.RunMode
import com.example.ui.WarmerScreen
import com.example.ui.WarmerViewModel
import com.example.ui.theme.EmberCoreTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WarmerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val useOledBlack = uiState.safetySettings.useOledBlackTheme

            LaunchedEffect(useOledBlack) {
                val style = if (useOledBlack) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            LaunchedEffect(uiState.isHeating, uiState.runMode) {
                if (uiState.isHeating && uiState.runMode == RunMode.SCREEN_ON) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            EmberCoreTheme(useOledBlack = useOledBlack) {
                WarmerScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onForegroundChanged(true)
    }

    override fun onStop() {
        super.onStop()
        viewModel.onForegroundChanged(false)
    }
}
