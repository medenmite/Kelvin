package com.example.engine

import com.example.model.HeatingStrength
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln1p
import kotlin.math.sin
import kotlin.math.sqrt

class CpuStressEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var workerJobs: List<Job> = emptyList()

    @Volatile
    var sink: Double = 0.0
        private set

    fun startOrUpdate(strength: HeatingStrength, boostedForGpuFallback: Boolean = false): Int {
        stopInternal()

        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val targetThreads = when {
            boostedForGpuFallback -> cores
            strength == HeatingStrength.LOW -> (cores * 0.40f).toInt().coerceAtLeast(1)
            strength == HeatingStrength.MEDIUM -> (cores * 0.75f).toInt().coerceAtLeast(2).coerceAtMost(cores)
            else -> cores
        }

        val activeSliceMs = when {
            boostedForGpuFallback -> 16L
            strength == HeatingStrength.LOW -> 6L
            strength == HeatingStrength.MEDIUM -> 12L
            else -> 24L
        }

        val pauseSliceMs = when {
            boostedForGpuFallback -> 0L
            strength == HeatingStrength.LOW -> 6L
            strength == HeatingStrength.MEDIUM -> 2L
            else -> 0L
        }

        workerJobs = List(targetThreads) { threadIndex ->
            scope.launch {
                var acc = 1.0001 + threadIndex * 0.013
                while (isActive) {
                    val burstEndNs = System.nanoTime() + activeSliceMs * 1_000_000L
                    while (isActive && System.nanoTime() < burstEndNs) {
                        for (i in 0 until 256) {
                            val x = acc + i * 0.00037
                            acc = sin(x) * cos(x) + sqrt(x * x + 1.0) + atan2(x, 1.41421) * 0.1
                            if (acc > 1e6 || acc.isNaN()) {
                                acc = 1.0 + (i % 13) * 0.07
                            }
                        }
                        sink = acc + ln1p(kotlin.math.abs(acc))
                    }
                    if (pauseSliceMs > 0L) {
                        delay(pauseSliceMs)
                    }
                }
            }
        }
        return targetThreads
    }

    suspend fun stop() {
        val jobs = workerJobs
        workerJobs = emptyList()
        jobs.forEach { it.cancelAndJoin() }
    }

    private fun stopInternal() {
        val jobs = workerJobs
        workerJobs = emptyList()
        jobs.forEach { it.cancel() }
    }
}
