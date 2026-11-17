package com.example.model

enum class ComputeTarget(
    val label: String,
    val shortLabel: String,
    val subtitle: String,
) {
    CPU("CPU", "CPU", "Multi-core math"),
    GPU("GPU", "GPU", "Shader pipeline"),
    CPU_AND_GPU("CPU + GPU", "DUAL", "Combined output"),
}

enum class HeatingStrength(
    val label: String,
    val shaderIterations: Int,
    val fboSize: Int,
) {
    LOW("Low", shaderIterations = 36, fboSize = 512),
    MEDIUM("Medium", shaderIterations = 96, fboSize = 896),
    HIGH("High", shaderIterations = 192, fboSize = 1280),
}

enum class RunMode(val label: String) {
    SCREEN_ON("Screen-On"),
    BACKGROUND_SERVICE("Background"),
}

enum class HeatingStatus {
    IDLE,
    WARMING,
    THROTTLED,
    COOLING_SAFETY_TRIP,
    COMPLETED,
}

enum class ThermalLevel(val label: String, val severityRank: Int) {
    NONE("Cool", 0),
    LIGHT("Warm", 1),
    MODERATE("Hot", 2),
    SEVERE("Severe", 3),
    CRITICAL("Critical", 4),
}

data class TelemetrySnapshot(
    val batteryTempC: Float = 28.0f,
    val batteryLevel: Int = 80,
    val voltageMv: Int = 3850,
    val currentMa: Int? = null,
    val isCharging: Boolean = false,
    val thermalLevel: ThermalLevel = ThermalLevel.NONE,
    val thermalHeadroom: Float? = null,
    val tempHistoryC: List<Float> = emptyList(),
)

data class SafetySettings(
    val maxTempC: Float = 43.0f,
    val minBatteryPercent: Int = 15,
    val autoStepDown: Boolean = true,
    val useFahrenheit: Boolean = false,
    val useOledBlackTheme: Boolean = false,
    val preferredRunMode: RunMode = RunMode.SCREEN_ON,
    val disclaimerDismissed: Boolean = false,
)

data class WarmerUiState(
    val selectedTarget: ComputeTarget = ComputeTarget.CPU_AND_GPU,
    val selectedStrength: HeatingStrength = HeatingStrength.MEDIUM,
    val effectiveStrength: HeatingStrength = HeatingStrength.MEDIUM,
    val runMode: RunMode = RunMode.SCREEN_ON,
    val selectedDurationSeconds: Int? = 600, // 10 min default; null = continuous
    val remainingSeconds: Int? = 600,
    val elapsedSeconds: Int = 0,
    val status: HeatingStatus = HeatingStatus.IDLE,
    val safetyTripReason: String? = null,
    val activeCpuThreads: Int = 0,
    val gpuFps: Int = 0,
    val backgroundGpuFallbackToCpu: Boolean = false,
    val telemetry: TelemetrySnapshot = TelemetrySnapshot(),
    val safetySettings: SafetySettings = SafetySettings(),
    val isPocketDimmerActive: Boolean = false,
    val isSafetySheetOpen: Boolean = false,
    val isCustomTimerDialogOpen: Boolean = false,
) {
    val isHeating: Boolean
        get() = status == HeatingStatus.WARMING || status == HeatingStatus.THROTTLED

    val shouldRunForegroundGpu: Boolean
        get() = isHeating && (selectedTarget == ComputeTarget.GPU || selectedTarget == ComputeTarget.CPU_AND_GPU)
}

fun formatTemperature(tempC: Float, useFahrenheit: Boolean): String {
    return if (useFahrenheit) {
        val f = tempC * 9f / 5f + 32f
        String.format(java.util.Locale.US, "%.1f°F", f)
    } else {
        String.format(java.util.Locale.US, "%.1f°C", tempC)
    }
}

fun formatTemperatureValue(tempC: Float, useFahrenheit: Boolean): String {
    return if (useFahrenheit) {
        val f = tempC * 9f / 5f + 32f
        String.format(java.util.Locale.US, "%.1f", f)
    } else {
        String.format(java.util.Locale.US, "%.1f", tempC)
    }
}

fun formatTimerSeconds(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}
