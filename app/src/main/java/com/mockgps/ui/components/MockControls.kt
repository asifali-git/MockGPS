package com.mockgps.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockControls(
    modifier: Modifier = Modifier,
    isMocking: Boolean,
    onStartMock: () -> Unit,
    onStopMock: () -> Unit,
    speedMultiplier: Float = 1f,
    onSpeedChange: (Float) -> Unit,
    updateInterval: Long = 1000,
    onIntervalChange: (Long) -> Unit,
    gpsAccuracy: Float = 10f,
    onAccuracyChange: (Float) -> Unit,
    onAdvancedSettings: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (isMocking) "Mock Location Active" else "Mock Location Inactive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isMocking) "Tap to stop spoofing location" else "Tap to start spoofing location",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = if (isMocking) onStopMock else onStartMock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMocking) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isMocking) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isMocking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isMocking) "Stop" else "Start"
                        )
                        Text(if (isMocking) "STOP" else "START", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Advanced settings", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))

                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = "", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Speed: ${String.format("%.1fx", speedMultiplier)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Slider(
                        value = speedMultiplier,
                        onValueChange = onSpeedChange,
                        valueRange = 0.1f..10f,
                        steps = 99,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isMocking
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Update Interval: ${updateInterval}ms", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = updateInterval.toFloat(),
                        onValueChange = { onIntervalChange(it.toLong()) },
                        valueRange = 100f..10000f,
                        steps = 99,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isMocking
                    )
                }
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GPS Accuracy: ${gpsAccuracy.toInt()}m", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = gpsAccuracy,
                        onValueChange = onAccuracyChange,
                        valueRange = 1f..100f,
                        steps = 99,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isMocking
                    )
                }
            }
        }
    }
}

@Composable
fun AdvancedSettingsDialog(
    onDismiss: () -> Unit,
    altitudeMode: Int,
    onAltitudeModeChange: (Int) -> Unit,
    manualAltitude: Double,
    onManualAltitudeChange: (Double) -> Unit,
    useFixedAltitude: Boolean,
    onFixedAltitudeChange: (Boolean) -> Unit,
    enableRouteMode: Boolean,
    onRouteModeChange: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advanced Settings") },
        text = {
            Column(Modifier.padding(16.dp).width(300.dp)) {
                Text("Altitude Mode", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Column {
                    listOf("GPS Altitude" to 0, "Manual Altitude" to 1).forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { onAltitudeModeChange(value) }
                        ) {
                            RadioButton(
                                selected = altitudeMode == value,
                                onClick = { onAltitudeModeChange(value) }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }

                if (altitudeMode == 1) {
                    Spacer(Modifier.height(12.dp))
                    Text("Manual Altitude (meters)", style = MaterialTheme.typography.bodySmall)
                    TextField(
                        value = manualAltitude.toString(),
                        onValueChange = { v -> runCatching { onManualAltitudeChange(v.toDouble()) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Use Fixed Altitude", style = MaterialTheme.typography.bodyMedium)
                        Text("Ignore GPS altitude, use manual value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = useFixedAltitude, onCheckedChange = onFixedAltitudeChange)
                }

                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Route Simulation", style = MaterialTheme.typography.bodyMedium)
                        Text("Follow saved route waypoints", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = enableRouteMode, onCheckedChange = onRouteModeChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}