package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.engine.HeatingCoordinator
import com.example.model.HeatingStatus
import com.example.model.formatTemperature
import com.example.model.formatTimerSeconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WarmerForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            HeatingCoordinator.getInstance(applicationContext).stopWarming(HeatingStatus.IDLE)
            stopSelfSafely()
            return START_NOT_STICKY
        }

        acquireWakeLock()
        val initialNotification = buildNotification(
            tempText = "--.-°C",
            batteryText = "--%",
            timerText = "Active",
            modeText = "Warming",
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        observerJob?.cancel()
        observerJob = serviceScope.launch {
            val coordinator = HeatingCoordinator.getInstance(applicationContext)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            coordinator.uiState.collect { state ->
                if (!state.isHeating) {
                    stopSelfSafely()
                    return@collect
                }
                val tempText = formatTemperature(
                    state.telemetry.batteryTempC,
                    state.safetySettings.useFahrenheit,
                )
                val batteryText = "${state.telemetry.batteryLevel}%"
                val timerText = state.remainingSeconds?.let { formatTimerSeconds(it) } ?: "Continuous"
                val modeText = "${state.selectedTarget.label} · ${state.effectiveStrength.label}"
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(tempText, batteryText, timerText, modeText),
                )
            }
        }

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Kelvin::WarmingWakeLock").apply {
            setReferenceCounted(false)
            // Bounded to 2 hours max as a safety guard
            acquire(2 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
        wakeLock = null
    }

    private fun stopSelfSafely() {
        observerJob?.cancel()
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        observerJob?.cancel()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        tempText: String,
        batteryText: String,
        timerText: String,
        modeText: String,
    ): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, WarmerForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Kelvin · $tempText · $batteryText")
            .setContentText("$modeText · $timerText")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notification_stop_action), stopIntent)
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.action.START_WARMING"
        const val ACTION_STOP = "com.example.action.STOP_WARMING"
        private const val CHANNEL_ID = "embercore_warming_channel"
        private const val NOTIFICATION_ID = 4201
    }
}
