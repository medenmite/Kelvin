package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetrySnapshot
import com.example.model.ThermalLevel
import com.example.model.formatTemperature
import com.example.model.formatTemperatureValue
import com.example.ui.theme.EmberTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HeroThermalDashboardCard(
    telemetry: TelemetrySnapshot,
    maxTempCutoffC: Float,
    minBatteryReserve: Int,
    useFahrenheit: Boolean,
    isHeating: Boolean,
    activeCpuThreads: Int,
    gpuFps: Int,
    onOpenSafetySettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = EmberTheme.palette
    val minScaleC = 18f
    val maxScaleC = 52f
    val normalizedTemp = ((telemetry.batteryTempC - minScaleC) / (maxScaleC - minScaleC)).coerceIn(0f, 1f)
    val normalizedCutoff = ((maxTempCutoffC - minScaleC) / (maxScaleC - minScaleC)).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = normalizedTemp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "heroTempProgress",
    )

    val animatedCutoff by animateFloatAsState(
        targetValue = normalizedCutoff,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "heroCutoffProgress",
    )

    val animatedBattery by animateFloatAsState(
        targetValue = telemetry.batteryLevel / 100f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "heroBattProgress",
    )

    val activeZoneColor by animateColorAsState(
        targetValue = when {
            telemetry.batteryTempC >= maxTempCutoffC -> palette.urgentRed
            telemetry.batteryTempC >= maxTempCutoffC - 2.0f -> palette.signalCrimson
            telemetry.batteryTempC >= 36.0f -> palette.coralThermal
            telemetry.batteryTempC >= 30.0f -> palette.skyFrost
            else -> palette.iceCobalt
        },
        animationSpec = tween(320),
        label = "heroZoneColor",
    )

    val batteryColor by animateColorAsState(
        targetValue = when {
            telemetry.isCharging -> palette.safeEmerald
            telemetry.batteryLevel <= minBatteryReserve -> palette.urgentRed
            telemetry.batteryLevel <= minBatteryReserve + 10 -> palette.signalCrimson
            else -> palette.iceCobalt
        },
        animationSpec = tween(300),
        label = "heroBatteryColor",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "tempPulse")
    val activePulse by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "activePulse",
    )

    val tempString = formatTemperatureValue(telemetry.batteryTempC, useFahrenheit)
    val dotIndex = tempString.indexOf('.')
    val wholeDigits = if (dotIndex >= 0) tempString.substring(0, dotIndex) else tempString
    val decimalPart = if (dotIndex >= 0) tempString.substring(dotIndex) else ".0"

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = if (isHeating) palette.signalCrimson.copy(alpha = 0.55f) else palette.glassBorderOuter,
        contentPadding = 16.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Top Precision Readout Deck (Unobstructed Split Typography + Status & Cutoff Stack)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(activeZoneColor.copy(alpha = 0.14f))
                                .border(1.dp, activeZoneColor.copy(alpha = 0.36f), RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Thermostat,
                                contentDescription = null,
                                tint = activeZoneColor,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "CHASSIS THERMISTOR",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.inkSecondary,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Split Digital Readout: Large Whole Number + Superscript Unit + Tabular Tenths
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = wholeDigits,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 54.sp,
                                lineHeight = 54.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1.8).sp,
                            ),
                            color = palette.inkPrimary,
                        )
                        Column(
                            modifier = Modifier.padding(start = 2.dp, bottom = 5.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = if (useFahrenheit) "°F" else "°C",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = activeZoneColor,
                            )
                            Text(
                                text = decimalPart,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 22.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = palette.inkSecondary,
                            )
                        }
                    }
                }

                // Right Column: Thermal Zone Badge + Interactive Cutoff Gate Pill + Margin
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(activeZoneColor.copy(alpha = 0.14f))
                            .border(1.dp, activeZoneColor.copy(alpha = 0.38f), RoundedCornerShape(9.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(activeZoneColor),
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${telemetry.thermalLevel.label.uppercase()} ZONE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = activeZoneColor,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.recessedSurface.copy(alpha = if (palette.isOledBlack) 0.92f else 0.78f))
                            .border(1.dp, palette.urgentRed.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                            .then(
                                if (onOpenSafetySettings != null) {
                                    Modifier.clickable(onClick = onOpenSafetySettings)
                                } else {
                                    Modifier
                                },
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(palette.urgentRed),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIMIT ${formatTemperature(maxTempCutoffC, useFahrenheit)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.inkPrimary,
                            )
                        }
                    }

                    val marginC = maxTempCutoffC - telemetry.batteryTempC
                    val marginText = if (marginC <= 0f) {
                        "AT CUTOFF LIMIT"
                    } else {
                        val formatted = if (useFahrenheit) {
                            String.format(java.util.Locale.US, "%.1f°F", marginC * 9f / 5f)
                        } else {
                            String.format(java.util.Locale.US, "%.1f°C", marginC)
                        }
                        "$formatted below cutoff"
                    }
                    Text(
                        text = marginText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (marginC <= 1.5f) palette.signalCrimson else palette.inkMuted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Full-Width Calibrated Linear Thermal Spectrum Bar (Zero Overlap, Instrument Grade)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.recessedSurface.copy(alpha = if (palette.isOledBlack) 0.92f else 0.68f))
                    .border(1.dp, palette.glassBorderOuter, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                ) {
                    val totalCells = 34
                    val gap = 3.dp.toPx()
                    val cellW = (size.width - gap * (totalCells - 1)) / totalCells
                    val activeIdx = (animatedProgress * (totalCells - 1)).toInt().coerceIn(0, totalCells - 1)
                    val cutoffX = (animatedCutoff * size.width).coerceIn(4.dp.toPx(), size.width - 4.dp.toPx())

                    val unlitColor = if (palette.isOledBlack) {
                        palette.glassBorderOuter.copy(alpha = 0.42f)
                    } else {
                        Color.White.copy(alpha = 0.72f)
                    }

                    for (i in 0 until totalCells) {
                        val frac = i.toFloat() / (totalCells - 1)
                        val isMajor = i % 6 == 0
                        val barH = if (isMajor) 18.dp.toPx() else 13.dp.toPx()
                        val topY = (size.height - barH) / 2f
                        val x = i * (cellW + gap)

                        val isLit = i <= activeIdx
                        val cellColor = if (isLit) {
                            val base = when {
                                frac >= animatedCutoff -> palette.urgentRed
                                frac >= animatedCutoff - 0.08f -> palette.signalCrimson
                                frac >= 0.52f -> palette.coralThermal
                                frac >= 0.32f -> palette.skyFrost
                                else -> palette.iceCobalt
                            }
                            if (i == activeIdx && isHeating) base.copy(alpha = activePulse) else base
                        } else {
                            unlitColor
                        }

                        drawRoundRect(
                            color = cellColor,
                            topLeft = Offset(x, topY),
                            size = Size(cellW, barH),
                            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
                        )
                    }

                    // Active temperature position notch indicator
                    val currentX = (animatedProgress * size.width).coerceIn(4.dp.toPx(), size.width - 4.dp.toPx())
                    drawCircle(
                        color = activeZoneColor,
                        radius = 3.2.dp.toPx(),
                        center = Offset(currentX, 2.5.dp.toPx()),
                    )

                    // Sharp Cutoff Gate Line + Top & Bottom Bracket Caps
                    drawLine(
                        color = palette.urgentRed,
                        start = Offset(cutoffX, 0f),
                        end = Offset(cutoffX, size.height),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = palette.urgentRed,
                        start = Offset(cutoffX - 4.dp.toPx(), 1.dp.toPx()),
                        end = Offset(cutoffX + 4.dp.toPx(), 1.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = palette.urgentRed,
                        start = Offset(cutoffX - 4.dp.toPx(), size.height - 1.dp.toPx()),
                        end = Offset(cutoffX + 4.dp.toPx(), size.height - 1.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Calibrated Temperature Scale Ruler Marks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val scaleMarksC = listOf(18f, 26f, 35f, 43f, 52f)
                    scaleMarksC.forEach { markC ->
                        val markText = if (useFahrenheit) {
                            "${(markC * 9f / 5f + 32f).toInt()}°"
                        } else {
                            "${markC.toInt()}°"
                        }
                        Text(
                            text = markText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (kotlin.math.abs(markC - maxTempCutoffC) < 2.2f) {
                                palette.urgentRed
                            } else {
                                palette.inkMuted
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Unified 3-Column Frosted Telemetry Strip (Battery | Power Rail | Compute/Headroom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Tile 1: Battery Level + Mini Segmented Bar
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.recessedSurface.copy(alpha = if (palette.isOledBlack) 0.90f else 0.65f))
                        .border(1.dp, palette.glassBorderOuter, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (telemetry.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
                            contentDescription = null,
                            tint = batteryColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = if (telemetry.isCharging) "CHG" else "MIN $minBatteryReserve%",
                            style = MaterialTheme.typography.labelSmall,
                            color = batteryColor,
                            maxLines = 1,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${telemetry.batteryLevel}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = palette.inkPrimary,
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                    ) {
                        val segments = 6
                        val gap = 2.5.dp.toPx()
                        val segW = (size.width - gap * (segments - 1)) / segments
                        val filled = (animatedBattery * segments).toInt().coerceIn(0, segments)
                        val emptyColor = if (palette.isOledBlack) palette.glassBorderOuter.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.72f)
                        for (i in 0 until segments) {
                            drawRoundRect(
                                color = if (i < filled) batteryColor else emptyColor,
                                topLeft = Offset(i * (segW + gap), 0f),
                                size = Size(segW, size.height),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                            )
                        }
                    }
                }

                // Tile 2: Voltage & Current Draw
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.recessedSurface.copy(alpha = if (palette.isOledBlack) 0.90f else 0.65f))
                        .border(1.dp, palette.glassBorderOuter, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = palette.iceCobalt,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "POWER",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.inkSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${telemetry.voltageMv}mV",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = palette.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val drawText = telemetry.currentMa?.let { ma ->
                        val sign = if (ma > 0 && telemetry.isCharging) "+" else ""
                        "$sign$ma mA"
                    } ?: if (telemetry.isCharging) "USB / AC" else "Battery DC"
                    Text(
                        text = drawText,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Tile 3: Active Compute & OS Headroom
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.recessedSurface.copy(alpha = if (palette.isOledBlack) 0.90f else 0.65f))
                        .border(1.dp, palette.glassBorderOuter, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = null,
                            tint = if (isHeating) palette.signalCrimson else palette.inkSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        val headroomText = telemetry.thermalHeadroom?.let {
                            "${((1f - it).coerceIn(0f, 1f) * 100).toInt()}% HR"
                        } ?: "LOAD"
                        Text(
                            text = headroomText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isHeating) palette.signalCrimson else palette.inkSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val loadHeadline = when {
                        activeCpuThreads > 0 && gpuFps > 0 -> "${activeCpuThreads}T+${gpuFps}"
                        activeCpuThreads > 0 -> "${activeCpuThreads} Cores"
                        gpuFps > 0 -> "$gpuFps FPS"
                        else -> "Standby"
                    }
                    Text(
                        text = loadHeadline,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (isHeating) palette.signalCrimson else palette.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isHeating) "Output Active" else "Zero Load",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun TemperatureArcGaugeCard(
    tempC: Float,
    maxTempCutoffC: Float,
    useFahrenheit: Boolean,
    thermalLevel: ThermalLevel,
    modifier: Modifier = Modifier,
) {
    val palette = EmberTheme.palette
    val minScaleC = 18f
    val maxScaleC = 52f
    val normalizedTemp = ((tempC - minScaleC) / (maxScaleC - minScaleC)).coerceIn(0f, 1f)
    val normalizedCutoff = ((maxTempCutoffC - minScaleC) / (maxScaleC - minScaleC)).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = normalizedTemp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tempArcProgress",
    )

    val activeZoneColor by animateColorAsState(
        targetValue = when {
            tempC >= maxTempCutoffC -> palette.urgentRed
            tempC >= maxTempCutoffC - 2.0f -> palette.signalCrimson
            tempC >= 36.0f -> palette.coralThermal
            tempC >= 30.0f -> palette.skyFrost
            else -> palette.iceCobalt
        },
        animationSpec = tween(350),
        label = "tempZoneColor",
    )

    GlassCard(
        modifier = modifier,
        contentPadding = 14.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Thermostat,
                        contentDescription = null,
                        tint = activeZoneColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TEMP",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.inkSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(activeZoneColor.copy(alpha = 0.14f))
                        .border(1.dp, activeZoneColor.copy(alpha = 0.34f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = thermalLevel.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = activeZoneColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatTemperatureValue(tempC, useFahrenheit),
                style = MaterialTheme.typography.displayMedium,
                color = palette.inkPrimary,
            )
            Text(
                text = if (useFahrenheit) "°F" else "°C",
                style = MaterialTheme.typography.labelMedium,
                color = activeZoneColor,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
            ) {
                val totalTicks = 20
                val gap = 3.dp.toPx()
                val segW = (size.width - gap * (totalTicks - 1)) / totalTicks
                val activeTicks = (animatedProgress * (totalTicks - 1)).toInt().coerceIn(0, totalTicks - 1)
                for (i in 0 until totalTicks) {
                    val frac = i.toFloat() / (totalTicks - 1)
                    val tickColor = if (i <= activeTicks) {
                        when {
                            frac >= normalizedCutoff -> palette.urgentRed
                            frac >= normalizedCutoff - 0.08f -> palette.signalCrimson
                            frac >= 0.55f -> palette.coralThermal
                            else -> palette.iceCobalt
                        }
                    } else {
                        palette.recessedSurface
                    }
                    drawRoundRect(
                        color = tickColor,
                        topLeft = Offset(i * (segW + gap), 2.dp.toPx()),
                        size = Size(segW, size.height - 4.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Cutoff ${formatTemperature(maxTempCutoffC, useFahrenheit)}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkMuted,
            )
        }
    }
}

@Composable
fun TemperatureSparklineCard(
    historyC: List<Float>,
    maxTempCutoffC: Float,
    useFahrenheit: Boolean,
    isHeating: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = EmberTheme.palette
    val infiniteTransition = rememberInfiniteTransition(label = "sparkDot")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sparkHaloScale",
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sparkHaloAlpha",
    )

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 14.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "THERMAL CURVE (60S)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.inkSecondary,
                    )
                    val lowTemp = historyC.minOrNull()
                    val highTemp = historyC.maxOrNull()
                    val rangeLabel = if (lowTemp != null && highTemp != null) {
                        "Min ${formatTemperature(lowTemp, useFahrenheit)} · Peak ${formatTemperature(highTemp, useFahrenheit)}"
                    } else {
                        "Sampling thermal baseline…"
                    }
                    Text(
                        text = rangeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkMuted,
                    )
                }

                val firstTemp = historyC.firstOrNull()
                val lastTemp = historyC.lastOrNull()
                if (firstTemp != null && lastTemp != null && historyC.size >= 3) {
                    val delta = lastTemp - firstTemp
                    val deltaStr = if (useFahrenheit) {
                        String.format(java.util.Locale.US, "%+.1f°F", delta * 9f / 5f)
                    } else {
                        String.format(java.util.Locale.US, "%+.1f°C", delta)
                    }
                    val badgeColor = if (delta >= 0.2f) palette.signalCrimson else palette.iceCobalt
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.14f))
                            .border(1.dp, badgeColor.copy(alpha = 0.34f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "Δ $deltaStr",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.recessedSurface.copy(alpha = if (palette.isOledBlack) 0.90f else 0.62f))
                    .border(1.dp, palette.glassBorderOuter, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val samples = if (historyC.size >= 2) historyC else listOf(28f, 28f)
                    val minVal = (samples.minOrNull() ?: 25f).coerceAtMost(maxTempCutoffC - 6f) - 1.5f
                    val maxVal = (samples.maxOrNull() ?: 40f).coerceAtLeast(maxTempCutoffC) + 1.5f
                    val span = (maxVal - minVal).coerceAtLeast(4f)

                    // Clean vertical & horizontal architectural grid
                    for (r in 1..2) {
                        val gy = size.height * (r / 3f)
                        drawLine(
                            color = palette.inkMuted.copy(alpha = 0.18f),
                            start = Offset(0f, gy),
                            end = Offset(size.width, gy),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    for (c in 1..3) {
                        val gx = size.width * (c / 4f)
                        drawLine(
                            color = palette.inkMuted.copy(alpha = 0.12f),
                            start = Offset(gx, 0f),
                            end = Offset(gx, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    // Cutoff dashed line
                    val cutoffNorm = ((maxTempCutoffC - minVal) / span).coerceIn(0f, 1f)
                    val cutoffY = size.height - cutoffNorm * size.height
                    drawLine(
                        color = palette.urgentRed.copy(alpha = 0.72f),
                        start = Offset(0f, cutoffY),
                        end = Offset(size.width, cutoffY),
                        strokeWidth = 1.3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                    )

                    val path = Path()
                    val count = samples.size
                    val stepX = if (count > 1) size.width / (count - 1).coerceAtLeast(1) else size.width
                    samples.forEachIndexed { idx, value ->
                        val x = idx * stepX
                        val norm = ((value - minVal) / span).coerceIn(0f, 1f)
                        val y = size.height - norm * size.height
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    val latest = samples.last()
                    val lineColor = if (latest >= maxTempCutoffC - 2f || isHeating) palette.signalCrimson else palette.iceCobalt
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(
                            width = 2.6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )

                    val lastNorm = ((latest - minVal) / span).coerceIn(0f, 1f)
                    val lastPt = Offset(size.width, size.height - lastNorm * size.height)
                    if (isHeating) {
                        drawCircle(
                            color = lineColor.copy(alpha = haloAlpha),
                            radius = 4.dp.toPx() * haloScale,
                            center = lastPt,
                        )
                    }
                    drawCircle(color = if (palette.isOledBlack) Color.Black else Color.White, radius = 4.5.dp.toPx(), center = lastPt)
                    drawCircle(color = lineColor, radius = 3.dp.toPx(), center = lastPt)
                }
            }
        }
    }
}
