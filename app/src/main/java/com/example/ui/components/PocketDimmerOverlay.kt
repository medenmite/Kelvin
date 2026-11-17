package com.example.ui.components

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.WarmerUiState
import com.example.model.formatTemperature
import com.example.model.formatTimerSeconds
import com.example.ui.theme.CoralThermal
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.SignalCrimson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PocketDimmerOverlay(
    uiState: WarmerUiState,
    onUnlock: () -> Unit,
    onStopWarming: () -> Unit,
) {
    val context = LocalContext.current
    BackHandler {
        // Consume back button so accidental pocket touches cannot dismiss the dimmer
    }

    DisposableEffect(context) {
        val activity = context as? Activity
        val window = activity?.window
        val previousBrightness = window?.attributes?.screenBrightness ?: -1f
        if (window != null) {
            val attrs = window.attributes
            attrs.screenBrightness = 0.02f
            window.attributes = attrs
        }
        onDispose {
            if (window != null) {
                val attrs = window.attributes
                attrs.screenBrightness = previousBrightness
                window.attributes = attrs
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .testTag("pocket_dimmer_overlay"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "POCKET DIMMER ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSteel.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${uiState.selectedTarget.label} · ${uiState.effectiveStrength.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CoralThermal.copy(alpha = 0.75f),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTemperature(
                        uiState.telemetry.batteryTempC,
                        uiState.safetySettings.useFahrenheit,
                    ),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White.copy(alpha = 0.65f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.remainingSeconds?.let { formatTimerSeconds(it) } ?: "CONTINUOUS",
                    style = MaterialTheme.typography.displayMedium,
                    color = CoralThermal.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Battery ${uiState.telemetry.batteryLevel}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSteel.copy(alpha = 0.7f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HoldToConfirmButton(
                    icon = Icons.Filled.LockOpen,
                    label = stringResource(R.string.pocket_dimmer_hint),
                    ringColor = Color.White.copy(alpha = 0.75f),
                    onConfirmed = onUnlock,
                    tag = "pocket_unlock_button",
                )
                HoldToConfirmButton(
                    icon = Icons.Filled.PowerSettingsNew,
                    label = stringResource(R.string.pocket_dimmer_stop),
                    ringColor = SignalCrimson,
                    onConfirmed = onStopWarming,
                    tag = "pocket_stop_button",
                )
            }
        }
    }
}

@Composable
private fun HoldToConfirmButton(
    icon: ImageVector,
    label: String,
    ringColor: Color,
    onConfirmed: () -> Unit,
    tag: String,
) {
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(140.dp),
    ) {
        Box(
            modifier = Modifier
                .testTag(tag)
                .size(84.dp)
                .clip(CircleShape)
                .background(Color(0xFF14161B))
                .border(1.2.dp, Color(0xFF2B303A), CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            var holdJob: Job? = null
                            holdJob = scope.launch {
                                val start = System.currentTimeMillis()
                                val durationMs = 1200L
                                while (true) {
                                    val elapsed = System.currentTimeMillis() - start
                                    progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                                    if (progress >= 1f) {
                                        onConfirmed()
                                        break
                                    }
                                    delay(16L)
                                }
                            }
                            try {
                                tryAwaitRelease()
                            } finally {
                                holdJob.cancel()
                                progress = 0f
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                if (progress > 0f) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ringColor,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedSteel.copy(alpha = 0.85f),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp),
        )
    }
}
