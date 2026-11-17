package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.model.HeatingStatus
import com.example.model.HeatingStrength
import com.example.ui.theme.EmberTheme
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class ParticleSeed(
    var xNorm: Float,
    var yNorm: Float,
    val radiusDp: Float,
    val speedFactor: Float,
    val driftAmplitude: Float,
    val driftFreq: Float,
    val phase: Float,
    val baseAlpha: Float,
    val shapeType: Int,
)

@Composable
fun ParticleBackground(
    status: HeatingStatus,
    strength: HeatingStrength,
    modifier: Modifier = Modifier,
) {
    val palette = EmberTheme.palette
    val isHeating = status == HeatingStatus.WARMING || status == HeatingStatus.THROTTLED
    val isSafetyTrip = status == HeatingStatus.COOLING_SAFETY_TRIP

    val canvasBg by animateColorAsState(
        targetValue = palette.canvasBackground,
        animationSpec = tween(durationMillis = 320),
        label = "canvasBg",
    )

    val targetPrimaryColor = when {
        isSafetyTrip -> palette.signalCrimson
        isHeating -> palette.signalCrimson
        else -> palette.iceCobalt
    }
    val targetSecondaryColor = when {
        isSafetyTrip -> palette.coralThermal
        isHeating -> palette.coralThermal
        else -> palette.inkMuted
    }

    val primaryColor by animateColorAsState(
        targetValue = targetPrimaryColor,
        animationSpec = tween(durationMillis = 650),
        label = "particlePrimaryColor",
    )
    val secondaryColor by animateColorAsState(
        targetValue = targetSecondaryColor,
        animationSpec = tween(durationMillis = 650),
        label = "particleSecondaryColor",
    )

    val targetSpeedMultiplier = when {
        !isHeating -> 0.20f
        strength == HeatingStrength.LOW -> 0.55f
        strength == HeatingStrength.MEDIUM -> 0.95f
        else -> 1.48f
    }
    val speedMultiplier by animateFloatAsState(
        targetValue = targetSpeedMultiplier,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "particleSpeed",
    )

    val particles = remember {
        val rng = Random(4250)
        List(64) { idx ->
            val isBackdrop = idx < 16
            ParticleSeed(
                xNorm = rng.nextFloat(),
                yNorm = rng.nextFloat(),
                radiusDp = if (isBackdrop) (12f + rng.nextFloat() * 18f) else (2.0f + rng.nextFloat() * 4.2f),
                speedFactor = if (isBackdrop) (0.015f + rng.nextFloat() * 0.02f) else (0.036f + rng.nextFloat() * 0.058f),
                driftAmplitude = if (isBackdrop) (0.01f + rng.nextFloat() * 0.015f) else (0.014f + rng.nextFloat() * 0.028f),
                driftFreq = 0.5f + rng.nextFloat() * 1.4f,
                phase = rng.nextFloat() * (2f * PI.toFloat()),
                baseAlpha = if (isBackdrop) (0.06f + rng.nextFloat() * 0.06f) else (0.18f + rng.nextFloat() * 0.30f),
                shapeType = if (isBackdrop) 3 else (idx % 3),
            )
        }
    }

    var frameClockNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    val mult = speedMultiplier
                    for (p in particles) {
                        p.yNorm -= p.speedFactor * mult * dt
                        if (p.yNorm < -0.10f) {
                            p.yNorm = 1.10f
                        }
                    }
                }
                lastNanos = now
                frameClockNanos = now
            }
        }
    }

    Canvas(
        modifier = modifier.background(canvasBg),
    ) {
        val timeSec = (frameClockNanos / 1_000_000_000f)
        val w = size.width
        val h = size.height

        // Crisp architectural micro-grid dots (kept ultra-minimal on OLED black to preserve true black power savings)
        val gridSpacing = 26.dp.toPx()
        val dotColor = palette.gridDot
        var gx = gridSpacing * 0.5f
        while (gx < w) {
            var gy = gridSpacing * 0.5f
            while (gy < h) {
                drawCircle(
                    color = dotColor,
                    radius = 0.85.dp.toPx(),
                    center = Offset(gx, gy),
                )
                gy += gridSpacing
            }
            gx += gridSpacing
        }

        val alphaScale = if (palette.isOledBlack) 0.85f else 1f
        for (i in particles.indices) {
            val p = particles[i]
            val sway = sin(timeSec * p.driftFreq + p.phase) * p.driftAmplitude
            val cx = ((p.xNorm + sway).coerceIn(-0.08f, 1.08f)) * w
            val cy = p.yNorm * h
            val rPx = p.radiusDp.dp.toPx()
            val color = (if (i % 3 == 0) secondaryColor else primaryColor).copy(alpha = p.baseAlpha * alphaScale)

            when (p.shapeType) {
                0 -> drawCircle(
                    color = color,
                    radius = rPx,
                    center = Offset(cx, cy),
                )
                1 -> drawCircle(
                    color = color,
                    radius = rPx * 1.2f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.4.dp.toPx()),
                )
                2 -> rotate(degrees = 45f + timeSec * 12f, pivot = Offset(cx, cy)) {
                    val side = rPx * 1.55f
                    drawRect(
                        color = color,
                        topLeft = Offset(cx - side / 2f, cy - side / 2f),
                        size = Size(side, side),
                    )
                }
                else -> {
                    drawCircle(
                        color = color,
                        radius = rPx,
                        center = Offset(cx, cy),
                    )
                    drawCircle(
                        color = color.copy(alpha = p.baseAlpha * 1.4f * alphaScale),
                        radius = rPx,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
        }
    }
}
