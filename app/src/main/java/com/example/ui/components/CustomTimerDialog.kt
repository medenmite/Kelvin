package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmberTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimerDialog(
    initialMinutes: Int,
    onConfirmMinutes: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = EmberTheme.palette
    var minutes by remember { mutableIntStateOf(initialMinutes.coerceIn(1, 120)) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        GlassCard(
            shape = RoundedCornerShape(24.dp),
            containerColor = palette.glassSurface.copy(alpha = 0.96f),
            borderColor = palette.glassBorderOuter,
            contentPadding = 20.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Custom Session Timer",
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.inkPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select duration between 1 and 120 minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkSecondary,
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GlassIconButton(
                        icon = Icons.Filled.Remove,
                        contentDescription = "Decrease minutes",
                        onClick = { minutes = (minutes - 1).coerceAtLeast(1) },
                        tag = "custom_timer_minus",
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.recessedSurface)
                            .border(1.2.dp, palette.glassBorderOuter, RoundedCornerShape(16.dp))
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = "$minutes",
                            style = MaterialTheme.typography.displayMedium,
                            color = palette.inkPrimary,
                        )
                        Text(
                            text = "MINUTES",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.inkSecondary,
                        )
                    }
                    GlassIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "Increase minutes",
                        onClick = { minutes = (minutes + 1).coerceAtMost(120) },
                        tag = "custom_timer_plus",
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(2, 20, 45, 60).forEach { quick ->
                        GlassChipButton(
                            text = "${quick}m",
                            selected = minutes == quick,
                            onClick = { minutes = quick },
                            activeColor = palette.iceCobalt,
                            activeTintColor = palette.iceCobaltTint,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassChipButton(
                        text = "Cancel",
                        selected = false,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    GlassChipButton(
                        text = "Apply",
                        selected = true,
                        onClick = { onConfirmMinutes(minutes) },
                        activeColor = palette.signalCrimson,
                        activeTintColor = palette.signalCrimsonTint,
                        modifier = Modifier.weight(1f),
                        tag = "custom_timer_apply",
                    )
                }
            }
        }
    }
}
