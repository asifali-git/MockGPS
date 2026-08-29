package com.mockgps.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mockgps.receiver.BootReceiver
import com.mockgps.shizuku.ShizukuHelper
import com.mockgps.util.VersionCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPress: () -> Unit
) {
    val context = LocalContext.current
    var autoStartEnabled by remember { mutableStateOf(BootReceiver.isAutoStartEnabled(context)) }
    var perAppEnabled by remember { mutableStateOf(BootReceiver.isPerAppEnabled(context)) }
    var shizukuAvailable by remember { mutableStateOf(ShizukuHelper.isShizukuAvailable(context)) }
    var shizukuPermissionGranted by remember { mutableStateOf(false) }
    var mockLocationPermissionGranted by remember { mutableStateOf(VersionCompat.hasMockLocationPermission(context)) }
    var batteryOptimizationIgnored by remember { mutableStateOf(VersionCompat.isIgnoringBatteryOptimizations(context)) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsSection(title = "Auto-Start") {
            SettingsRow(
                title = "Start on Boot",
                subtitle = "Auto-start mock location on device boot",
                icon = Icons.Default.PowerSettingsNew,
                trailing = {
                    Switch(checked = autoStartEnabled, onCheckedChange = { enabled ->
                        autoStartEnabled = enabled
                        BootReceiver.setAutoStartEnabled(context, enabled)
                        if (enabled) com.mockgps.MockGPSApplication.startServices(context)
                        else com.mockgps.MockGPSApplication.stopServices(context)
                    })
                }
            )
            Divider()
            SettingsRow(
                title = "Per-App Spoofing on Boot",
                subtitle = "Start per-app spoofing automatically (requires Shizuku)",
                icon = Icons.Default.Apps,
                enabled = shizukuAvailable,
                trailing = {
                    Switch(checked = perAppEnabled, onCheckedChange = { enabled ->
                        perAppEnabled = enabled
                        BootReceiver.setPerAppEnabled(context, enabled)
                        if (enabled) {
                            val intent = Intent(context, com.mockgps.service.PerAppLocationService::class.java)
                            intent.action = com.mockgps.service.PerAppLocationService.ACTION_START_PER_APP
                            VersionCompat.startForegroundServiceCompat(context, intent)
                        } else {
                            context.stopService(Intent(context, com.mockgps.service.PerAppLocationService::class.java))
                        }
                    }, enabled = shizukuAvailable)
                }
            )
        }

        SettingsSection(title = "Shizuku Integration") {
            SettingsRow(
                title = "Shizuku Available",
                subtitle = if (shizukuAvailable) "Ready" else "Not installed - per-app limited",
                icon = if (shizukuAvailable) Icons.Default.CheckCircle else Icons.Default.Error,
                iconColor = if (shizukuAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            if (shizukuAvailable) {
                Divider()
                SettingsRow(
                    title = "Shizuku Permission",
                    subtitle = if (shizukuPermissionGranted) "Granted" else "Tap to request",
                    icon = if (shizukuPermissionGranted) Icons.Default.LockOpen else Icons.Default.Lock,
                    iconColor = if (shizukuPermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    onClick = { if (!shizukuPermissionGranted) ShizukuHelper.requestPermission(context, object : ShizukuHelper.ShizukuCallback {
                        override fun onShizukuReady() { shizukuPermissionGranted = true }
                        override fun onShizukuError(e: String) { shizukuPermissionGranted = false }
                        override fun onPermissionGranted() { shizukuPermissionGranted = true }
                        override fun onPermissionDenied() { shizukuPermissionGranted = false }
                    }) }
                )
            } else {
                Divider()
                SettingsRow(
                    title = "Install Shizuku",
                    subtitle = "Required for per-app spoofing",
                    icon = Icons.Default.Download,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))) }
                )
            }
        }

        SettingsSection(title = "Permissions") {
            SettingsRow(
                title = "Mock Location",
                subtitle = if (mockLocationPermissionGranted) "Granted" else "Tap to enable",
                icon = if (mockLocationPermissionGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                iconColor = if (mockLocationPermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                onClick = { VersionCompat.requestMockLocationPermission(context) }
            )
            Divider()
            SettingsRow(
                title = "Battery Optimization",
                subtitle = if (batteryOptimizationIgnored) "Ignored" else "Tap to ignore",
                icon = if (batteryOptimizationIgnored) Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
                iconColor = if (batteryOptimizationIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                onClick = { VersionCompat.requestIgnoreBatteryOptimizations(context) }
            )
            Divider()
            SettingsRow(
                title = "Developer Options",
                subtitle = "Enable 'Select mock location app'",
                icon = Icons.Default.DeveloperMode,
                onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
            )
        }

        SettingsSection(title = "About") {
            SettingsRow(title = "Version", subtitle = "1.0.0", icon = Icons.Default.Info)
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            Divider()
            content()
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    ListItem(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)) },
        leadingContent = { Icon(icon, contentDescription = "", tint = if (enabled) iconColor else iconColor.copy(alpha = 0.38f)) },
        trailingContent = trailing
    )
}