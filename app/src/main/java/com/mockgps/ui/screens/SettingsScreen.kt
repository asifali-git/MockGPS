package com.mockgps.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mockgps.R
import com.mockgps.receiver.BootReceiver
import com.mockgps.shizuku.ShizukuHelper
import com.mockgps.util.PermissionUtils
import com.mockgps.util.VersionCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPress: () -> Unit
) {
    val context = LocalContext.current
    var autoStartEnabled by mutableStateOf(BootReceiver.isAutoStartEnabled(context))
    var perAppEnabled by mutableStateOf(BootReceiver.isPerAppEnabled(context))
    var shizukuAvailable by mutableStateOf(ShizukuHelper.isShizukuAvailable(context))
    var shizukuPermissionGranted by mutableStateOf(ShizukuHelper.checkSelfPermission(context) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    var mockLocationPermissionGranted by mutableStateOf(VersionCompat.hasMockLocationPermission(context))
    var batteryOptimizationIgnored by mutableStateOf(VersionCompat.isIgnoringBatteryOptimizations(context))

    // Update states
    androidx.compose.runtime.LaunchedEffect(Unit) {
        autoStartEnabled = BootReceiver.isAutoStartEnabled(context)
        perAppEnabled = BootReceiver.isPerAppEnabled(context)
        shizukuAvailable = ShizukuHelper.isShizukuAvailable(context)
        shizukuPermissionGranted = ShizukuHelper.checkSelfPermission(context) == android.content.pm.PackageManager.PERMISSION_GRANTED
        mockLocationPermissionGranted = VersionCompat.hasMockLocationPermission(context)
        batteryOptimizationIgnored = VersionCompat.isIgnoringBatteryOptimizations(context)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Auto-Start Section
        SettingsSection(title = "Auto-Start", icon = "autorenew") {
            SettingsRow(
                title = "Start on Boot",
                subtitle = "Automatically start mock location services when device boots",
                icon = "power",
                trailing = {
                    Switch(
                        checked = autoStartEnabled,
                        onCheckedChange = { enabled ->
                            autoStartEnabled = enabled
                            BootReceiver.setAutoStartEnabled(context, enabled)
                            if (enabled) {
                                com.mockgps.MockGPSApplication.startServices(context)
                            } else {
                                com.mockgps.MockGPSApplication.stopServices(context)
                            }
                        }
                    )
                }
            )
            
            Divider()
            
            SettingsRow(
                title = "Per-App Spoofing on Boot",
                subtitle = "Start per-app location spoofing automatically (requires Shizuku)",
                icon = "apps",
                enabled = shizukuAvailable && shizukuPermissionGranted,
                trailing = {
                    Switch(
                        checked = perAppEnabled,
                        onCheckedChange = { enabled ->
                            perAppEnabled = enabled
                            BootReceiver.setPerAppEnabled(context, enabled)
                            if (enabled) {
                                val intent = Intent(context, com.mockgps.service.PerAppLocationService::class.java)
                                intent.action = com.mockgps.service.PerAppLocationService.ACTION_START_PER_APP
                                VersionCompat.startForegroundServiceCompat(context, intent)
                            } else {
                                context.stopService(Intent(context, com.mockgps.service.PerAppLocationService::class.java))
                            }
                        },
                        enabled = shizukuAvailable && shizukuPermissionGranted
                    )
                }
            )
        }

        // Shizuku Section
        SettingsSection(title = "Shizuku Integration", icon = "security") {
            SettingsRow(
                title = "Shizuku Available",
                subtitle = if (shizukuAvailable) "Shizuku is installed and ready" else "Shizuku not installed - per-app spoofing limited",
                icon = shizukuAvailable ? "check_circle" : "error",
                iconColor = if (shizukuAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            
            if (shizukuAvailable) {
                Divider()
                SettingsRow(
                    title = "Shizuku Permission",
                    subtitle = if (shizukuPermissionGranted) "Granted - per-app spoofing enabled" else "Not granted - tap to request",
                    icon = shizukuPermissionGranted ? "lock_open" : "lock",
                    iconColor = if (shizukuPermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    onClick = {
                        if (!shizukuPermissionGranted) {
                            requestShizukuPermission(context) { granted ->
                                shizukuPermissionGranted = granted
                            }
                        }
                    }
                )
            } else {
                Divider()
                SettingsRow(
                    title = "Install Shizuku",
                    subtitle = "Required for per-app location spoofing without root",
                    icon = "download",
                    onClick = {
                        openShizukuInstall(context)
                    }
                )
            }
        }

        // Permissions Section
        SettingsSection(title = "Permissions", icon = "lock") {
            SettingsRow(
                title = "Mock Location Permission",
                subtitle = if (mockLocationPermissionGranted) "Granted" else "Not granted - required for mock location",
                icon = mockLocationPermissionGranted ? "check_circle" : "error",
                iconColor = if (mockLocationPermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                onClick = {
                    if (!mockLocationPermissionGranted) {
                        VersionCompat.requestMockLocationPermission(context)
                    }
                }
            )
            
            Divider()
            
            SettingsRow(
                title = "Battery Optimization",
                subtitle = if (batteryOptimizationIgnored) "Ignored - services will run reliably" else "Not ignored - system may kill services",
                icon = batteryOptimizationIgnored ? "battery_full" : "battery_alert",
                iconColor = if (batteryOptimizationIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                onClick = {
                    if (!batteryOptimizationIgnored) {
                        VersionCompat.requestIgnoreBatteryOptimizations(context)
                    }
                }
            )
            
            Divider()
            
            SettingsRow(
                title = "Location Permissions",
                subtitle = "Fine and coarse location access",
                icon = "location_on",
                onClick = {
                    PermissionUtils.openAppSettings(context)
                }
            )
        }

        // Advanced Section
        SettingsSection(title = "Advanced", icon = "settings") {
            SettingsRow(
                title = "Developer Options",
                subtitle = "Open to enable 'Select mock location app'",
                icon = "developer_mode",
                onClick = {
                    PermissionUtils.openDeveloperOptions(context)
                }
            )
            
            Divider()
            
            SettingsRow(
                title = "Reset All Settings",
                subtitle = "Clear all saved locations, profiles, and preferences",
                icon = "restore",
                iconColor = MaterialTheme.colorScheme.error,
                onClick = {
                    // Show confirmation dialog
                }
            )
        }

        // Info Section
        SettingsSection(title = "About", icon = "info") {
            SettingsRow(
                title = "Version",
                subtitle = "1.0.0",
                icon = "tag"
            )
            
            Divider()
            
            SettingsRow(
                title = "Open Source",
                subtitle = "View on GitHub",
                icon = "code",
                onClick = {
                    openGitHub(context)
                }
            )
            
            Divider()
            
            SettingsRow(
                title = "Shizuku Project",
                subtitle = "Learn more about Shizuku",
                icon = "link",
                onClick = {
                    openShizukuGitHub(context)
                }
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Section header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                androidx.compose.material3.Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
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
    icon: String,
    iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val clickable = onClick != null || trailing != null
    
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(if (clickable) androidx.compose.foundation.clickable(onClick = onClick ?: {}) else Modifier),
        enabled = enabled,
        headlineContent = {
            androidx.compose.material3.Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        },
        supportingContent = {
            androidx.compose.material3.Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        },
        leadingContent = {
            Icon(
                imageVector = getIcon(icon),
                contentDescription = "",
                tint = if (enabled) iconColor else iconColor.copy(alpha = 0.38f)
            )
        },
        trailingContent = trailing
    )
}

fun getIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name) {
        "autorenew" -> androidx.compose.material.icons.Icons.Default.Autorenew
        "power" -> androidx.compose.material.icons.Icons.Default.PowerSettingsNew
        "apps" -> androidx.compose.material.icons.Icons.Default.Apps
        "security" -> androidx.compose.material.icons.Icons.Default.Security
        "check_circle" -> androidx.compose.material.icons.Icons.Default.CheckCircle
        "error" -> androidx.compose.material.icons.Icons.Default.Error
        "lock_open" -> androidx.compose.material.icons.Icons.Default.LockOpen
        "lock" -> androidx.compose.material.icons.Icons.Default.Lock
        "download" -> androidx.compose.material.icons.Icons.Default.Download
        "location_on" -> androidx.compose.material.icons.Icons.Default.LocationOn
        "battery_full" -> androidx.compose.material.icons.Icons.Default.BatteryFull
        "battery_alert" -> androidx.compose.material.icons.Icons.Default.BatteryAlert
        "developer_mode" -> androidx.compose.material.icons.Icons.Default.DeveloperMode
        "restore" -> androidx.compose.material.icons.Icons.Default.Restore
        "info" -> androidx.compose.material.icons.Icons.Default.Info
        "tag" -> androidx.compose.material.icons.Icons.Default.LocalOffer
        "code" -> androidx.compose.material.icons.Icons.Default.Code
        "link" -> androidx.compose.material.icons.Icons.Default.Link
        "settings" -> androidx.compose.material.icons.Icons.Default.Settings
        else -> androidx.compose.material.icons.Icons.Default.Help
    }
}

private fun requestShizukuPermission(context: Context, callback: (Boolean) -> Unit) {
    ShizukuHelper.requestPermission(context, object : ShizukuHelper.ShizukuCallback {
        override fun onShizukuReady() {
            callback(true)
        }

        override fun onShizukuError(error: String) {
            callback(false)
        }

        override fun onPermissionGranted() {
            callback(true)
        }

        override fun onPermissionDenied() {
            callback(false)
        }
    })
}

private fun openShizukuInstall(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openGitHub(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/yourusername/MockGPS"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openShizukuGitHub(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}