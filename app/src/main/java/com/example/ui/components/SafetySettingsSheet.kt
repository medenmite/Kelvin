package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.SafetySettings
import com.example.model.formatTemperature
import com.example.ui.theme.EmberTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetySettingsSheet(
    settings: SafetySettings,
    onMaxTempChange: (Float) -> Unit,
    onMinBatteryChange: (Int) -> Unit,
    onAutoStepDownChange: (Boolean) -> Unit,
    onUseFahrenheitChange: (Boolean) -> Unit,
    onUseOledBlackChange: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = EmberTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.canvasBackground,
        contentColor = palette.inkPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.safety_sheet_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.inkPrimary,
                    )
                    Text(
                        text = "Automatic protections shut down heating if any threshold trips.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkSecondary,
                    )
                }
                GlassIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Close safety settings",
                    onClick = onDismiss,
                    tag = "close_safety_sheet",
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Theme Switcher (Darkish White vs True Deep Black OLED)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Display Theme",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.inkPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "True Deep Black turns off OLED pixels to maximize battery efficiency.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GlassChipButton(
                            text = "Pearl Mist",
                            subtitle = "Darkish White",
                            icon = Icons.Filled.LightMode,
                            selected = !settings.useOledBlackTheme,
                            onClick = { onUseOledBlackChange(false) },
                            activeColor = palette.iceCobalt,
                            activeTintColor = palette.iceCobaltTint,
                            modifier = Modifier.weight(1f),
                            tag = "theme_pearl_button",
                        )
                        GlassChipButton(
                            text = "Deep Black",
                            subtitle = "OLED Save",
                            icon = Icons.Filled.DarkMode,
                            selected = settings.useOledBlackTheme,
                            onClick = { onUseOledBlackChange(true) },
                            activeColor = palette.signalCrimson,
                            activeTintColor = palette.signalCrimsonTint,
                            modifier = Modifier.weight(1f),
                            tag = "theme_oled_button",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Max Battery Temp Cutoff Slider
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.safety_max_temp_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.inkPrimary,
                        )
                        Text(
                            text = formatTemperature(settings.maxTempC, settings.useFahrenheit),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.signalCrimson,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recommended safe warmth range: 39°C – 44°C (102°F – 111°F).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkSecondary,
                    )
                    Slider(
                        value = settings.maxTempC,
                        onValueChange = { raw ->
                            val snapped = (raw * 2f).roundToInt() / 2f
                            onMaxTempChange(snapped)
                        },
                        valueRange = 37.0f..48.0f,
                        steps = 21,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("max_temp_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = palette.signalCrimson,
                            activeTrackColor = palette.signalCrimson,
                            inactiveTrackColor = palette.recessedSurface,
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatTemperature(37.0f, settings.useFahrenheit),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.inkSecondary,
                        )
                        Text(
                            text = "Safe Default (${formatTemperature(43.0f, settings.useFahrenheit)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.iceCobalt,
                        )
                        Text(
                            text = formatTemperature(48.0f, settings.useFahrenheit),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.inkSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Min Battery Reserve Slider
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.safety_min_batt_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.inkPrimary,
                        )
                        Text(
                            text = "${settings.minBatteryPercent}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.iceCobalt,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Stops warming automatically if unplugged battery reaches this level.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkSecondary,
                    )
                    Slider(
                        value = settings.minBatteryPercent.toFloat(),
                        onValueChange = { raw ->
                            val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(5, 50)
                            onMinBatteryChange(snapped)
                        },
                        valueRange = 5f..50f,
                        steps = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("min_battery_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = palette.iceCobalt,
                            activeTrackColor = palette.iceCobalt,
                            inactiveTrackColor = palette.recessedSurface,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto Step-Down & Unit Toggle
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = stringResource(R.string.safety_auto_step_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.inkPrimary,
                            )
                            Text(
                                text = stringResource(R.string.safety_auto_step_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.inkSecondary,
                            )
                        }
                        Switch(
                            checked = settings.autoStepDown,
                            onCheckedChange = onAutoStepDownChange,
                            modifier = Modifier.testTag("auto_step_down_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = palette.signalCrimson,
                                uncheckedThumbColor = palette.inkSecondary,
                                uncheckedTrackColor = palette.recessedSurface,
                            ),
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = palette.glassBorderOuter,
                    )

                    Text(
                        text = stringResource(R.string.safety_unit_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.inkPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GlassChipButton(
                            text = "Celsius (°C)",
                            selected = !settings.useFahrenheit,
                            onClick = { onUseFahrenheitChange(false) },
                            activeColor = palette.iceCobalt,
                            activeTintColor = palette.iceCobaltTint,
                            modifier = Modifier.weight(1f),
                            tag = "unit_celsius_button",
                        )
                        GlassChipButton(
                            text = "Fahrenheit (°F)",
                            selected = settings.useFahrenheit,
                            onClick = { onUseFahrenheitChange(true) },
                            activeColor = palette.iceCobalt,
                            activeTintColor = palette.iceCobaltTint,
                            modifier = Modifier.weight(1f),
                            tag = "unit_fahrenheit_button",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassChipButton(
                text = "Reset Safe Defaults (43.0°C / 15%)",
                selected = false,
                icon = Icons.Filled.RestartAlt,
                onClick = onResetDefaults,
                activeColor = palette.signalCrimson,
                activeTintColor = palette.signalCrimsonTint,
                modifier = Modifier.fillMaxWidth(),
                tag = "reset_safety_defaults_button",
            )
        }
    }
}
