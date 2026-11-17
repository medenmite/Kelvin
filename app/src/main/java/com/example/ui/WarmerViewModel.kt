package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.HeatingCoordinator
import com.example.model.ComputeTarget
import com.example.model.HeatingStatus
import com.example.model.HeatingStrength
import com.example.model.RunMode
import com.example.model.WarmerUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WarmerViewModel(application: Application) : AndroidViewModel(application) {

    private val coordinator = HeatingCoordinator.getInstance(application)
    val uiState: StateFlow<WarmerUiState> = coordinator.uiState

    fun onForegroundChanged(inForeground: Boolean) {
        coordinator.setAppForegroundState(inForeground)
    }

    fun selectTarget(target: ComputeTarget) {
        coordinator.selectTarget(target)
    }

    fun selectStrength(strength: HeatingStrength) {
        coordinator.selectStrength(strength)
    }

    fun selectDuration(durationSeconds: Int?) {
        coordinator.selectDuration(durationSeconds)
    }

    fun setRunMode(runMode: RunMode) {
        coordinator.setRunMode(runMode)
    }

    fun toggleWarming() {
        if (uiState.value.isHeating) {
            coordinator.stopWarming(HeatingStatus.IDLE)
        } else {
            coordinator.startWarming()
        }
    }

    fun stopWarming() {
        coordinator.stopWarming(HeatingStatus.IDLE)
    }

    fun onForegroundGpuFps(fps: Int) {
        coordinator.onForegroundGpuFps(fps)
    }

    fun setPocketDimmer(active: Boolean) {
        coordinator.setPocketDimmer(active)
    }

    fun setSafetySheetOpen(open: Boolean) {
        coordinator.setSafetySheetOpen(open)
    }

    fun setCustomTimerDialogOpen(open: Boolean) {
        coordinator.setCustomTimerDialogOpen(open)
    }

    fun clearStatusBanner() {
        coordinator.clearStatusBanner()
    }

    fun updateMaxTempC(tempC: Float) {
        viewModelScope.launch { coordinator.safetyPrefsRepository.setMaxTempC(tempC) }
    }

    fun updateMinBatteryPercent(percent: Int) {
        viewModelScope.launch { coordinator.safetyPrefsRepository.setMinBatteryPercent(percent) }
    }

    fun updateAutoStepDown(enabled: Boolean) {
        viewModelScope.launch { coordinator.safetyPrefsRepository.setAutoStepDown(enabled) }
    }

    fun updateUseFahrenheit(useFahrenheit: Boolean) {
        viewModelScope.launch { coordinator.safetyPrefsRepository.setUseFahrenheit(useFahrenheit) }
    }

    fun updateUseOledBlackTheme(enabled: Boolean) {
        viewModelScope.launch { coordinator.safetyPrefsRepository.setUseOledBlackTheme(enabled) }
    }

    fun dismissDisclaimer() {
        viewModelScope.launch { coordinator.safetyPrefsRepository.setDisclaimerDismissed(true) }
    }

    fun resetSafetyDefaults() {
        viewModelScope.launch {
            coordinator.safetyPrefsRepository.setMaxTempC(43.0f)
            coordinator.safetyPrefsRepository.setMinBatteryPercent(15)
            coordinator.safetyPrefsRepository.setAutoStepDown(true)
        }
    }
}
