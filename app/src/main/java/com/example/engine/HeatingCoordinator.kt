package com.example.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.SafetyPrefsRepository
import com.example.data.TelemetryRepository
import com.example.model.ComputeTarget
import com.example.model.HeatingStatus
import com.example.model.HeatingStrength
import com.example.model.RunMode
import com.example.model.SafetySettings
import com.example.model.TelemetrySnapshot
import com.example.model.ThermalLevel
import com.example.model.WarmerUiState
import com.example.model.formatTemperature
import com.example.service.WarmerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HeatingCoordinator private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val telemetryRepository = TelemetryRepository(appContext)
    val safetyPrefsRepository = SafetyPrefsRepository(appContext)
    private val cpuEngine = CpuStressEngine()
    private val offscreenGpuRunner = OffscreenGpuRunner()
    private val hapticHelper = HapticFeedbackHelper(appContext)

    private val _uiState = MutableStateFlow(WarmerUiState())
    val uiState: StateFlow<WarmerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var appInForeground: Boolean = true
    private var latestSettings: SafetySettings = SafetySettings()

    init {
        scope.launch {
            telemetryRepository.telemetry.collect { snapshot ->
                _uiState.update { it.copy(telemetry = snapshot) }
                evaluateSafetyGuardrails(snapshot, latestSettings)
            }
        }
        scope.launch {
            safetyPrefsRepository.settingsFlow.collect { settings ->
                latestSettings = settings
                _uiState.update { current ->
                    current.copy(
                        safetySettings = settings,
                        runMode = if (!current.isHeating) settings.preferredRunMode else current.runMode,
                    )
                }
                if (_uiState.value.isHeating) {
                    evaluateSafetyGuardrails(_uiState.value.telemetry, settings)
                }
            }
        }
    }

    fun setAppForegroundState(inForeground: Boolean) {
        appInForeground = inForeground
        val state = _uiState.value
        if (!state.isHeating) return

        val needsGpu = state.selectedTarget == ComputeTarget.GPU || state.selectedTarget == ComputeTarget.CPU_AND_GPU
        if (!inForeground && state.runMode == RunMode.BACKGROUND_SERVICE && needsGpu) {
            offscreenGpuRunner.start(
                strength = state.effectiveStrength,
                onFpsMeasured = { fps -> _uiState.update { it.copy(gpuFps = fps) } },
                onUnavailable = {
                    scope.launch {
                        _uiState.update { it.copy(backgroundGpuFallbackToCpu = true) }
                        reconfigureWorkers()
                    }
                },
            )
        } else if (inForeground) {
            offscreenGpuRunner.stop()
            if (state.backgroundGpuFallbackToCpu) {
                _uiState.update { it.copy(backgroundGpuFallbackToCpu = false) }
                scope.launch { reconfigureWorkers() }
            }
        }
    }

    fun selectTarget(target: ComputeTarget) {
        _uiState.update { it.copy(selectedTarget = target) }
        if (_uiState.value.isHeating) {
            scope.launch { reconfigureWorkers() }
        }
    }

    fun selectStrength(strength: HeatingStrength) {
        hapticHelper.vibrateIntensityToggle(strength)
        _uiState.update {
            it.copy(
                selectedStrength = strength,
                effectiveStrength = strength,
                status = if (it.status == HeatingStatus.THROTTLED) HeatingStatus.WARMING else it.status,
            )
        }
        if (_uiState.value.isHeating) {
            scope.launch { reconfigureWorkers() }
        }
    }

    fun selectDuration(durationSeconds: Int?) {
        _uiState.update { state ->
            state.copy(
                selectedDurationSeconds = durationSeconds,
                remainingSeconds = durationSeconds,
            )
        }
    }

    fun setRunMode(runMode: RunMode) {
        _uiState.update { it.copy(runMode = runMode) }
        scope.launch { safetyPrefsRepository.setPreferredRunMode(runMode) }
        if (_uiState.value.isHeating) {
            if (runMode == RunMode.BACKGROUND_SERVICE) {
                startForegroundServiceIfNeeded()
            } else {
                stopForegroundService()
            }
        }
    }

    fun startWarming() {
        val state = _uiState.value
        val telemetry = state.telemetry
        val settings = state.safetySettings

        if (telemetry.batteryTempC >= settings.maxTempC) {
            hapticHelper.vibrateThermalSafetyCutoff()
            _uiState.update {
                it.copy(
                    status = HeatingStatus.COOLING_SAFETY_TRIP,
                    safetyTripReason = "Battery is already at ${formatTemperature(telemetry.batteryTempC, settings.useFahrenheit)} (cutoff ${formatTemperature(settings.maxTempC, settings.useFahrenheit)}).",
                )
            }
            return
        }
        if (!telemetry.isCharging && telemetry.batteryLevel <= settings.minBatteryPercent) {
            hapticHelper.vibrateThermalSafetyCutoff()
            _uiState.update {
                it.copy(
                    status = HeatingStatus.COOLING_SAFETY_TRIP,
                    safetyTripReason = "Battery (${telemetry.batteryLevel}%) is at or below your ${settings.minBatteryPercent}% reserve limit.",
                )
            }
            return
        }
        if (telemetry.thermalLevel.severityRank >= ThermalLevel.SEVERE.severityRank) {
            hapticHelper.vibrateThermalSafetyCutoff()
            _uiState.update {
                it.copy(
                    status = HeatingStatus.COOLING_SAFETY_TRIP,
                    safetyTripReason = "System thermal status is ${telemetry.thermalLevel.label}. Allow device to cool before warming.",
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                status = HeatingStatus.WARMING,
                effectiveStrength = it.selectedStrength,
                remainingSeconds = it.selectedDurationSeconds,
                elapsedSeconds = 0,
                safetyTripReason = null,
                backgroundGpuFallbackToCpu = false,
            )
        }

        scope.launch { reconfigureWorkers() }
        if (_uiState.value.runMode == RunMode.BACKGROUND_SERVICE) {
            startForegroundServiceIfNeeded()
        }
        startSessionTicker()
    }

    fun stopWarming(reasonStatus: HeatingStatus = HeatingStatus.IDLE, tripReason: String? = null) {
        if (reasonStatus == HeatingStatus.COOLING_SAFETY_TRIP) {
            hapticHelper.vibrateThermalSafetyCutoff()
        }
        timerJob?.cancel()
        timerJob = null
        offscreenGpuRunner.stop()
        stopForegroundService()
        scope.launch {
            cpuEngine.stop()
            _uiState.update {
                it.copy(
                    status = reasonStatus,
                    safetyTripReason = tripReason,
                    activeCpuThreads = 0,
                    gpuFps = 0,
                    isPocketDimmerActive = false,
                    backgroundGpuFallbackToCpu = false,
                )
            }
        }
    }

    fun onForegroundGpuFps(fps: Int) {
        _uiState.update { it.copy(gpuFps = fps) }
    }

    fun setPocketDimmer(active: Boolean) {
        _uiState.update { it.copy(isPocketDimmerActive = active && it.isHeating) }
    }

    fun setSafetySheetOpen(open: Boolean) {
        _uiState.update { it.copy(isSafetySheetOpen = open) }
    }

    fun setCustomTimerDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isCustomTimerDialogOpen = open) }
    }

    fun clearStatusBanner() {
        _uiState.update {
            if (!it.isHeating) it.copy(status = HeatingStatus.IDLE, safetyTripReason = null)
            else it.copy(safetyTripReason = null)
        }
    }

    private suspend fun reconfigureWorkers() {
        val state = _uiState.value
        if (!state.isHeating) {
            cpuEngine.stop()
            offscreenGpuRunner.stop()
            _uiState.update { it.copy(activeCpuThreads = 0, gpuFps = 0) }
            return
        }

        val needsCpu = state.selectedTarget == ComputeTarget.CPU ||
            state.selectedTarget == ComputeTarget.CPU_AND_GPU ||
            state.backgroundGpuFallbackToCpu
        val needsGpu = state.selectedTarget == ComputeTarget.GPU ||
            state.selectedTarget == ComputeTarget.CPU_AND_GPU

        val threads = if (needsCpu) {
            cpuEngine.startOrUpdate(
                strength = state.effectiveStrength,
                boostedForGpuFallback = state.backgroundGpuFallbackToCpu,
            )
        } else {
            cpuEngine.stop()
            0
        }

        if (needsGpu && !appInForeground && state.runMode == RunMode.BACKGROUND_SERVICE && !state.backgroundGpuFallbackToCpu) {
            offscreenGpuRunner.updateStrength(state.effectiveStrength)
        } else if (!needsGpu) {
            offscreenGpuRunner.stop()
        }

        _uiState.update {
            it.copy(
                activeCpuThreads = threads,
                gpuFps = if (needsGpu) it.gpuFps else 0,
            )
        }
    }

    private fun startSessionTicker() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _uiState.value.isHeating) {
                delay(1000L)
                var expired = false
                _uiState.update { current ->
                    val nextElapsed = current.elapsedSeconds + 1
                    val nextRemaining = current.remainingSeconds?.let { rem ->
                        if (rem <= 1) {
                            expired = true
                            0
                        } else {
                            rem - 1
                        }
                    }
                    current.copy(
                        elapsedSeconds = nextElapsed,
                        remainingSeconds = nextRemaining,
                    )
                }
                if (expired) {
                    stopWarming(
                        reasonStatus = HeatingStatus.COMPLETED,
                        tripReason = "Session timer finished safely.",
                    )
                    break
                }
            }
        }
    }

    private fun evaluateSafetyGuardrails(telemetry: TelemetrySnapshot, settings: SafetySettings) {
        val state = _uiState.value
        if (!state.isHeating) return

        // 1. Hard battery temperature cutoff
        if (telemetry.batteryTempC >= settings.maxTempC) {
            stopWarming(
                reasonStatus = HeatingStatus.COOLING_SAFETY_TRIP,
                tripReason = "Cutoff reached: battery hit ${formatTemperature(telemetry.batteryTempC, settings.useFahrenheit)} (limit ${formatTemperature(settings.maxTempC, settings.useFahrenheit)}).",
            )
            return
        }

        // 2. Hard OS thermal status severe/critical protection
        if (telemetry.thermalLevel.severityRank >= ThermalLevel.SEVERE.severityRank) {
            stopWarming(
                reasonStatus = HeatingStatus.COOLING_SAFETY_TRIP,
                tripReason = "Android thermal protection triggered (${telemetry.thermalLevel.label} thermal state).",
            )
            return
        }

        // 3. Minimum battery reserve protection (when not plugged in)
        if (!telemetry.isCharging && telemetry.batteryLevel <= settings.minBatteryPercent) {
            stopWarming(
                reasonStatus = HeatingStatus.COOLING_SAFETY_TRIP,
                tripReason = "Battery reached your ${settings.minBatteryPercent}% reserve floor.",
            )
            return
        }

        // 4. Optional auto step-down within 1.5°C of cutoff or MODERATE OS thermal throttling
        if (settings.autoStepDown) {
            val nearTempLimit = telemetry.batteryTempC >= (settings.maxTempC - 1.5f)
            val moderateThermal = telemetry.thermalLevel == ThermalLevel.MODERATE
            if ((nearTempLimit || moderateThermal) && state.effectiveStrength != HeatingStrength.LOW) {
                val stepped = when (state.effectiveStrength) {
                    HeatingStrength.HIGH -> HeatingStrength.MEDIUM
                    HeatingStrength.MEDIUM -> HeatingStrength.LOW
                    HeatingStrength.LOW -> HeatingStrength.LOW
                }
                if (stepped != state.effectiveStrength) {
                    _uiState.update {
                        it.copy(
                            effectiveStrength = stepped,
                            status = HeatingStatus.THROTTLED,
                            safetyTripReason = "Auto-stepped down to ${stepped.label} near thermal limit.",
                        )
                    }
                    scope.launch { reconfigureWorkers() }
                }
            }
        }
    }

    private fun startForegroundServiceIfNeeded() {
        val intent = Intent(appContext, WarmerForegroundService::class.java).apply {
            action = WarmerForegroundService.ACTION_START
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        }
    }

    private fun stopForegroundService() {
        runCatching {
            appContext.stopService(Intent(appContext, WarmerForegroundService::class.java))
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: HeatingCoordinator? = null

        fun getInstance(context: Context): HeatingCoordinator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HeatingCoordinator(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
