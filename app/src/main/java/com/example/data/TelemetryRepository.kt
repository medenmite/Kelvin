package com.example.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.example.model.TelemetrySnapshot
import com.example.model.ThermalLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TelemetryRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _telemetry = MutableStateFlow(readSnapshot(emptyList()))
    val telemetry: StateFlow<TelemetrySnapshot> = _telemetry.asStateFlow()

    private val historyBuffer = ArrayDeque<Float>(60)
    private var lastHeadroomPollMs = 0L
    private var cachedHeadroom: Float? = null
    private var headroomSupported: Boolean? = null

    init {
        scope.launch {
            while (isActive) {
                val snapshot = readSnapshot(historyBuffer.toList())
                if (historyBuffer.size >= 60) {
                    historyBuffer.removeFirst()
                }
                historyBuffer.addLast(snapshot.batteryTempC)
                _telemetry.value = snapshot.copy(tempHistoryC = historyBuffer.toList())
                delay(1000L)
            }
        }
    }

    private fun readSnapshot(existingHistory: List<Float>): TelemetrySnapshot {
        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val tempC = (rawTemp / 10f).coerceIn(-10f, 80f)

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 80) ?: 80
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
        val batteryPct = ((level * 100f) / scale).toInt().coerceIn(0, 100)

        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 3850) ?: 3850
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val rawCurrentUa = runCatching {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        }.getOrNull()
        val currentMa = if (rawCurrentUa != null &&
            rawCurrentUa != Int.MIN_VALUE &&
            rawCurrentUa != Int.MAX_VALUE &&
            rawCurrentUa != 0
        ) {
            val ma = rawCurrentUa / 1000
            if (kotlin.math.abs(ma) in 1..15000) ma else null
        } else {
            null
        }

        val thermalHeadroom = pollThermalHeadroomIfDue()
        val thermalLevel = resolveThermalLevel(thermalHeadroom)

        return TelemetrySnapshot(
            batteryTempC = tempC,
            batteryLevel = batteryPct,
            voltageMv = voltageMv,
            currentMa = currentMa,
            isCharging = isCharging,
            thermalLevel = thermalLevel,
            thermalHeadroom = thermalHeadroom,
            tempHistoryC = existingHistory,
        )
    }

    private fun pollThermalHeadroomIfDue(): Float? {
        val pm = powerManager ?: return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        if (headroomSupported == false) return null

        val now = System.currentTimeMillis()
        // ADPF guideline: do not poll getThermalHeadroom more than once every 10 seconds
        if (now - lastHeadroomPollMs < 11_000L && lastHeadroomPollMs != 0L) {
            return cachedHeadroom
        }
        lastHeadroomPollMs = now

        val headroom = runCatching { pm.getThermalHeadroom(30) }.getOrNull()
        if (headroom == null || headroom.isNaN() || headroom <= 0f) {
            if (headroomSupported == null) {
                headroomSupported = false
            }
            return cachedHeadroom
        }
        headroomSupported = true
        cachedHeadroom = headroom
        return headroom
    }

    private fun resolveThermalLevel(headroom: Float?): ThermalLevel {
        val pm = powerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pm != null) {
            val osStatus = runCatching { pm.currentThermalStatus }.getOrDefault(PowerManager.THERMAL_STATUS_NONE)
            when {
                osStatus >= PowerManager.THERMAL_STATUS_CRITICAL -> return ThermalLevel.CRITICAL
                osStatus == PowerManager.THERMAL_STATUS_SEVERE -> return ThermalLevel.SEVERE
                osStatus == PowerManager.THERMAL_STATUS_MODERATE -> return ThermalLevel.MODERATE
                osStatus == PowerManager.THERMAL_STATUS_LIGHT -> return ThermalLevel.LIGHT
            }
        }
        // ADPF heuristic fallback when currentThermalStatus reports NONE on older/OEM devices
        if (headroom != null && !headroom.isNaN()) {
            return when {
                headroom > 1.0f -> ThermalLevel.SEVERE
                headroom > 0.95f -> ThermalLevel.MODERATE
                headroom > 0.85f -> ThermalLevel.LIGHT
                else -> ThermalLevel.NONE
            }
        }
        return ThermalLevel.NONE
    }
}
