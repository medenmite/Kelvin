package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.engine.GpuStressSurface
import com.example.model.ComputeTarget
import com.example.model.HeatingStatus
import com.example.model.HeatingStrength
import com.example.model.RunMode
import com.example.model.WarmerUiState
import com.example.model.formatTemperature
import com.example.model.formatTimerSeconds
import com.example.ui.components.CustomTimerDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChipButton
import com.example.ui.components.GlassIconButton
import com.example.ui.components.HeroThermalDashboardCard
import com.example.ui.components.ParticleBackground
import com.example.ui.components.PocketDimmerOverlay
import com.example.ui.components.PrimaryGlassWarmButton
import com.example.ui.components.SafetySettingsSheet
import com.example.ui.components.SegmentedOption
import com.example.ui.components.SlidingGlassSegmentedControl
import com.example.ui.components.TemperatureSparklineCard
import com.example.ui.theme.EmberTheme

@Composable
fun WarmerScreen(
    uiState: WarmerUiState,
    viewModel: WarmerViewModel,
) {
    val palette = EmberTheme.palette
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.setRunMode(RunMode.BACKGROUND_SERVICE)
        } else {
            viewModel.setRunMode(RunMode.SCREEN_ON)
        }
    }

    val onRequestBackgroundMode: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.setRunMode(RunMode.BACKGROUND_SERVICE)
            } else {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.setRunMode(RunMode.BACKGROUND_SERVICE)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ParticleBackground(
            status = uiState.status,
            strength = uiState.effectiveStrength,
            modifier = Modifier.fillMaxSize(),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val isWideLayout = maxWidth >= 700.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = if (isWideLayout) 1040.dp else 600.dp),
                ) {
                    TopStatusBar(
                        uiState = uiState,
                        onToggleTheme = { viewModel.updateUseOledBlackTheme(!uiState.safetySettings.useOledBlackTheme) },
                        onToggleUnit = { viewModel.updateUseFahrenheit(!uiState.safetySettings.useFahrenheit) },
                        onOpenSafety = { viewModel.setSafetySheetOpen(true) },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedVisibility(
                        visible = !uiState.safetySettings.disclaimerDismissed,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            SafetyNoticeCard(onDismiss = { viewModel.dismissDisclaimer() })
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.safetyTripReason != null,
                        enter = fadeIn(tween(240)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                        exit = fadeOut(tween(200)) + shrinkVertically(),
                    ) {
                        uiState.safetyTripReason?.let { reason ->
                            Column {
                                StatusAlertBanner(
                                    status = uiState.status,
                                    message = reason,
                                    onDismiss = { viewModel.clearStatusBanner() },
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    if (isWideLayout) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                TelemetrySection(
                                    uiState = uiState,
                                    onGpuFps = { viewModel.onForegroundGpuFps(it) },
                                    onOpenSafety = { viewModel.setSafetySheetOpen(true) },
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                ControlsSection(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onRequestBackgroundMode = onRequestBackgroundMode,
                                )
                            }
                        }
                    } else {
                        TelemetrySection(
                            uiState = uiState,
                            onGpuFps = { viewModel.onForegroundGpuFps(it) },
                            onOpenSafety = { viewModel.setSafetySheetOpen(true) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ControlsSection(
                            uiState = uiState,
                            viewModel = viewModel,
                            onRequestBackgroundMode = onRequestBackgroundMode,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.isPocketDimmerActive && uiState.isHeating,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(220)),
        ) {
            PocketDimmerOverlay(
                uiState = uiState,
                onUnlock = { viewModel.setPocketDimmer(false) },
                onStopWarming = { viewModel.stopWarming() },
            )
        }
    }

    if (uiState.isSafetySheetOpen) {
        SafetySettingsSheet(
            settings = uiState.safetySettings,
            onMaxTempChange = { viewModel.updateMaxTempC(it) },
            onMinBatteryChange = { viewModel.updateMinBatteryPercent(it) },
            onAutoStepDownChange = { viewModel.updateAutoStepDown(it) },
            onUseFahrenheitChange = { viewModel.updateUseFahrenheit(it) },
            onUseOledBlackChange = { viewModel.updateUseOledBlackTheme(it) },
            onResetDefaults = { viewModel.resetSafetyDefaults() },
            onDismiss = { viewModel.setSafetySheetOpen(false) },
        )
    }

    if (uiState.isCustomTimerDialogOpen) {
        CustomTimerDialog(
            initialMinutes = (uiState.selectedDurationSeconds ?: 600) / 60,
            onConfirmMinutes = { mins ->
                viewModel.selectDuration(mins * 60)
                viewModel.setCustomTimerDialogOpen(false)
            },
            onDismiss = { viewModel.setCustomTimerDialogOpen(false) },
        )
    }
}

@Composable
private fun TopStatusBar(
    uiState: WarmerUiState,
    onToggleTheme: () -> Unit,
    onToggleUnit: () -> Unit,
    onOpenSafety: () -> Unit,
) {
    val palette = EmberTheme.palette
    val infiniteTransition = rememberInfiniteTransition(label = "statusDot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotAlpha",
    )

    val statusColor by animateColorAsState(
        targetValue = when (uiState.status) {
            HeatingStatus.IDLE -> palette.iceCobalt
            HeatingStatus.WARMING -> palette.signalCrimson
            HeatingStatus.THROTTLED -> palette.coralThermal
            HeatingStatus.COOLING_SAFETY_TRIP -> palette.urgentRed
            HeatingStatus.COMPLETED -> palette.safeEmerald
        },
        animationSpec = tween(250),
        label = "statusColor",
    )

    val statusText = when (uiState.status) {
        HeatingStatus.IDLE -> stringResource(R.string.status_idle)
        HeatingStatus.WARMING -> stringResource(R.string.status_warming)
        HeatingStatus.THROTTLED -> stringResource(R.string.status_throttled)
        HeatingStatus.COOLING_SAFETY_TRIP -> stringResource(R.string.status_safety_trip)
        HeatingStatus.COMPLETED -> stringResource(R.string.status_completed)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(palette.glassSurface.copy(alpha = if (palette.isOledBlack) 0.88f else 0.82f))
                    .border(
                        1.3.dp,
                        if (uiState.isHeating) palette.signalCrimson.copy(alpha = 0.65f) else palette.glassBorderOuter,
                        RoundedCornerShape(15.dp),
                    )
                    .padding(1.dp)
                    .border(
                        1.dp,
                        palette.glassBorderInner.copy(alpha = if (palette.isOledBlack) 0.45f else 1f),
                        RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = palette.signalCrimson,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (uiState.isHeating) palette.signalCrimson else palette.iceCobalt, CircleShape),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.inkPrimary,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.14f))
                        .border(1.dp, statusColor.copy(alpha = 0.34f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .alpha(if (uiState.isHeating) dotAlpha else 1f)
                                .clip(CircleShape)
                                .background(statusColor),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                        )
                        if (uiState.isHeating && uiState.elapsedSeconds > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "· ${formatTimerSeconds(uiState.elapsedSeconds)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.inkSecondary,
                            )
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = if (palette.isOledBlack) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = if (palette.isOledBlack) "Switch to Pearl Mist theme" else "Switch to Deep Black OLED theme",
                onClick = onToggleTheme,
                tint = if (palette.isOledBlack) palette.signalCrimson else palette.inkPrimary,
                tag = "theme_quick_toggle",
            )

            Box(
                modifier = Modifier
                    .testTag("unit_quick_toggle")
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(palette.glassSurface.copy(alpha = if (palette.isOledBlack) 0.85f else 0.78f))
                    .border(1.2.dp, palette.glassBorderOuter, RoundedCornerShape(15.dp))
                    .padding(1.dp)
                    .border(1.dp, palette.glassBorderInner.copy(alpha = if (palette.isOledBlack) 0.45f else 1f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onToggleUnit),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (uiState.safetySettings.useFahrenheit) "°F" else "°C",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.iceCobalt,
                )
            }

            GlassIconButton(
                icon = Icons.Filled.Tune,
                contentDescription = stringResource(R.string.cd_safety_settings),
                onClick = onOpenSafety,
                tint = palette.inkPrimary,
                tag = "safety_settings_button",
            )
        }
    }
}

@Composable
private fun TelemetrySection(
    uiState: WarmerUiState,
    onGpuFps: (Int) -> Unit,
    onOpenSafety: () -> Unit,
) {
    val palette = EmberTheme.palette
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeroThermalDashboardCard(
            telemetry = uiState.telemetry,
            maxTempCutoffC = uiState.safetySettings.maxTempC,
            minBatteryReserve = uiState.safetySettings.minBatteryPercent,
            useFahrenheit = uiState.safetySettings.useFahrenheit,
            isHeating = uiState.isHeating,
            activeCpuThreads = uiState.activeCpuThreads,
            gpuFps = uiState.gpuFps,
            onOpenSafetySettings = onOpenSafety,
        )

        TemperatureSparklineCard(
            historyC = uiState.telemetry.tempHistoryC,
            maxTempCutoffC = uiState.safetySettings.maxTempC,
            useFahrenheit = uiState.safetySettings.useFahrenheit,
            isHeating = uiState.isHeating,
        )

        AnimatedVisibility(
            visible = uiState.shouldRunForegroundGpu,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = palette.signalCrimson.copy(alpha = 0.45f),
                contentPadding = 12.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .border(1.4.dp, palette.signalCrimson.copy(alpha = 0.55f), RoundedCornerShape(13.dp)),
                        ) {
                            GpuStressSurface(
                                active = uiState.shouldRunForegroundGpu,
                                strength = uiState.effectiveStrength,
                                onFpsMeasured = onGpuFps,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "GPU SHADER MATRIX",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.signalCrimson,
                            )
                            Text(
                                text = "${uiState.effectiveStrength.fboSize}×${uiState.effectiveStrength.fboSize} FBO · ${uiState.effectiveStrength.shaderIterations} ALU passes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.inkPrimary,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(palette.signalCrimsonTint)
                            .border(1.dp, palette.signalCrimson.copy(alpha = 0.38f), RoundedCornerShape(9.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${uiState.gpuFps} FPS",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.signalCrimson,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlsSection(
    uiState: WarmerUiState,
    viewModel: WarmerViewModel,
    onRequestBackgroundMode: () -> Unit,
) {
    val palette = EmberTheme.palette
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 14.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Text(
                        text = stringResource(R.string.section_workload),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.inkSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SlidingGlassSegmentedControl(
                        options = listOf(
                            SegmentedOption(
                                value = ComputeTarget.CPU,
                                label = "CPU",
                                subtitle = "Math cores",
                                icon = Icons.Filled.Memory,
                                tag = "target_cpu",
                            ),
                            SegmentedOption(
                                value = ComputeTarget.GPU,
                                label = "GPU",
                                subtitle = "GL shader",
                                icon = Icons.Filled.GraphicEq,
                                tag = "target_gpu",
                            ),
                            SegmentedOption(
                                value = ComputeTarget.CPU_AND_GPU,
                                label = "DUAL",
                                subtitle = "CPU + GPU",
                                icon = Icons.Filled.LocalFireDepartment,
                                tag = "target_cpu_and_gpu",
                            ),
                        ),
                        selectedValue = uiState.selectedTarget,
                        onSelect = { viewModel.selectTarget(it) },
                        activeColor = palette.signalCrimson,
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.section_strength),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.inkSecondary,
                        )
                        if (uiState.status == HeatingStatus.THROTTLED) {
                            Text(
                                text = "STEPPED TO ${uiState.effectiveStrength.label.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.coralThermal,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SlidingGlassSegmentedControl(
                        options = listOf(
                            SegmentedOption(
                                value = HeatingStrength.LOW,
                                label = "Low",
                                subtitle = "Gentle",
                                tag = "strength_low",
                                barsCount = 1,
                            ),
                            SegmentedOption(
                                value = HeatingStrength.MEDIUM,
                                label = "Medium",
                                subtitle = "Steady",
                                tag = "strength_medium",
                                barsCount = 2,
                            ),
                            SegmentedOption(
                                value = HeatingStrength.HIGH,
                                label = "High",
                                subtitle = "Max heat",
                                tag = "strength_high",
                                barsCount = 3,
                            ),
                        ),
                        selectedValue = uiState.selectedStrength,
                        onSelect = { viewModel.selectStrength(it) },
                        activeColor = if (uiState.selectedStrength == HeatingStrength.LOW) palette.iceCobalt else palette.signalCrimson,
                    )
                }
            }
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 14.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = if (uiState.isHeating) palette.signalCrimson else palette.iceCobalt,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.section_timer),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.inkSecondary,
                        )
                    }
                    AnimatedContent(
                        targetState = uiState.remainingSeconds?.let { formatTimerSeconds(it) } ?: "CONTINUOUS",
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                        label = "timerReadout",
                    ) { timerText ->
                        Text(
                            text = timerText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (uiState.isHeating) palette.signalCrimson else palette.inkPrimary,
                            modifier = Modifier.testTag("timer_display"),
                        )
                    }
                }

                val totalSec = uiState.selectedDurationSeconds
                val remSec = uiState.remainingSeconds
                if (totalSec != null && totalSec > 0 && remSec != null) {
                    val targetFraction = (remSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue = targetFraction,
                        animationSpec = tween(durationMillis = 350),
                        label = "timerProgress",
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(palette.recessedSurface)
                            .border(0.8.dp, palette.glassBorderOuter, RoundedCornerShape(3.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (uiState.isHeating) palette.signalCrimson else palette.iceCobalt),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    val presets = listOf(300 to "5m", 600 to "10m", 900 to "15m", 1800 to "30m")
                    presets.forEach { (secs, label) ->
                        GlassChipButton(
                            text = label,
                            selected = uiState.selectedDurationSeconds == secs,
                            onClick = { viewModel.selectDuration(secs) },
                            activeColor = palette.iceCobalt,
                            activeTintColor = palette.iceCobaltTint,
                            horizontalPadding = 4.dp,
                            modifier = Modifier.weight(1f),
                            tag = "timer_$label",
                        )
                    }
                    GlassChipButton(
                        text = "∞",
                        selected = uiState.selectedDurationSeconds == null,
                        onClick = { viewModel.selectDuration(null) },
                        activeColor = palette.iceCobalt,
                        activeTintColor = palette.iceCobaltTint,
                        horizontalPadding = 4.dp,
                        modifier = Modifier.weight(0.85f),
                        tag = "timer_infinite",
                    )
                    val isCustomSelected = uiState.selectedDurationSeconds != null &&
                        uiState.selectedDurationSeconds !in listOf(300, 600, 900, 1800)
                    val customChipLabel = if (isCustomSelected && uiState.selectedDurationSeconds != null) {
                        "${(uiState.selectedDurationSeconds / 60).coerceAtLeast(1)}m"
                    } else {
                        "+"
                    }
                    GlassChipButton(
                        text = customChipLabel,
                        selected = isCustomSelected,
                        onClick = { viewModel.setCustomTimerDialogOpen(true) },
                        activeColor = palette.iceCobalt,
                        activeTintColor = palette.iceCobaltTint,
                        horizontalPadding = 4.dp,
                        modifier = Modifier.weight(0.95f),
                        tag = "timer_custom",
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.section_execution),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.inkSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GlassChipButton(
                            text = RunMode.SCREEN_ON.label,
                            icon = Icons.Filled.PhoneAndroid,
                            selected = uiState.runMode == RunMode.SCREEN_ON,
                            onClick = { viewModel.setRunMode(RunMode.SCREEN_ON) },
                            activeColor = palette.iceCobalt,
                            activeTintColor = palette.iceCobaltTint,
                            modifier = Modifier.weight(1f),
                            tag = "mode_screen_on",
                        )
                        GlassChipButton(
                            text = RunMode.BACKGROUND_SERVICE.label,
                            icon = Icons.Filled.NotificationsActive,
                            selected = uiState.runMode == RunMode.BACKGROUND_SERVICE,
                            onClick = onRequestBackgroundMode,
                            activeColor = palette.iceCobalt,
                            activeTintColor = palette.iceCobaltTint,
                            modifier = Modifier.weight(1f),
                            tag = "mode_background",
                        )
                    }
                }
            }
        }

        val ctaSubtitle = if (uiState.isHeating) {
            "Cutoff ${formatTemperature(uiState.safetySettings.maxTempC, uiState.safetySettings.useFahrenheit)} · Min ${uiState.safetySettings.minBatteryPercent}% batt"
        } else {
            "${uiState.selectedTarget.label} · ${uiState.selectedStrength.label} · ${uiState.runMode.label}"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryGlassWarmButton(
                isHeating = uiState.isHeating,
                text = if (uiState.isHeating) stringResource(R.string.action_stop_warming) else stringResource(R.string.action_start_warming),
                subtitle = ctaSubtitle,
                icon = if (uiState.isHeating) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                onClick = { viewModel.toggleWarming() },
                modifier = Modifier.weight(1f),
            )

            AnimatedVisibility(
                visible = uiState.isHeating,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                GlassChipButton(
                    text = "Dim",
                    subtitle = "Lock",
                    icon = Icons.Filled.Bedtime,
                    selected = false,
                    onClick = { viewModel.setPocketDimmer(true) },
                    activeColor = palette.inkPrimary,
                    activeTintColor = palette.recessedSurface,
                    modifier = Modifier.width(86.dp),
                    tag = "pocket_dimmer_button",
                )
            }
        }
    }
}

@Composable
private fun SafetyNoticeCard(onDismiss: () -> Unit) {
    val palette = EmberTheme.palette
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = palette.iceCobaltTint.copy(alpha = 0.78f),
        borderColor = palette.iceCobalt.copy(alpha = 0.45f),
        contentPadding = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = palette.iceCobalt,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.safety_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.inkPrimary,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_disclaimer_button"),
            ) {
                Text(
                    text = stringResource(R.string.action_dismiss),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.iceCobalt,
                )
            }
        }
    }
}

@Composable
private fun StatusAlertBanner(
    status: HeatingStatus,
    message: String,
    onDismiss: () -> Unit,
) {
    val palette = EmberTheme.palette
    val isTrip = status == HeatingStatus.COOLING_SAFETY_TRIP
    val isComplete = status == HeatingStatus.COMPLETED
    val accent = when {
        isTrip -> palette.urgentRed
        isComplete -> palette.safeEmerald
        else -> palette.signalCrimson
    }
    val tint = when {
        isTrip -> palette.urgentRedTint
        isComplete -> palette.safeEmeraldTint
        else -> palette.signalCrimsonTint
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth().testTag("status_alert_banner"),
        containerColor = tint.copy(alpha = 0.90f),
        borderColor = accent,
        contentPadding = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isComplete) Icons.Filled.AllInclusive else Icons.Filled.Warning,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = palette.inkPrimary,
                modifier = Modifier.weight(1f),
            )
            GlassIconButton(
                icon = Icons.Filled.Close,
                contentDescription = "Dismiss alert",
                onClick = onDismiss,
                tint = accent,
                containerColor = palette.glassSurface.copy(alpha = 0.72f),
                borderColor = accent.copy(alpha = 0.35f),
            )
        }
    }
}
