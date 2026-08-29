package com.mockgps.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mockgps.data.MockProfile
import com.mockgps.util.LocationUtils

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val uid: Int = 0
)

@Composable
fun PerAppSpoofingList(
    modifier: Modifier = Modifier,
    profiles: List<MockProfile>,
    installedApps: List<AppInfo>,
    onProfileClick: (MockProfile) -> Unit,
    onProfileToggle: (MockProfile, Boolean) -> Unit,
    onAddApp: () -> Unit,
    onEditProfile: (MockProfile) -> Unit,
    onDeleteProfile: (MockProfile) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Per-App Spoofing", style = MaterialTheme.typography.titleMedium)
        }

        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Apps, "No apps", size = 48.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text("No apps configured for spoofing", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onAddApp) { Text("Add App") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(profiles) { profile ->
                    PerAppProfileItem(
                        profile = profile,
                        appInfo = installedApps.find { it.packageName == profile.packageName },
                        onClick = { onProfileClick(profile) },
                        onToggle = { onProfileToggle(profile, it) },
                        onEdit = { onEditProfile(profile) },
                        onDelete = { onDeleteProfile(profile) }
                    )
                }
            }
        }
    }
}

@Composable
fun PerAppProfileItem(
    profile: MockProfile,
    appInfo: AppInfo?,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Apps, contentDescription = "", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp))
                }

                Column {
                    Text(
                        appInfo?.label ?: profile.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Lat: ${LocationUtils.formatDecimalCoordinate(profile.latitude, true, 4)}, Lng: ${LocationUtils.formatDecimalCoordinate(profile.longitude, false, 4)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(checked = profile.isEnabled, onCheckedChange = onToggle)
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onEdit() })
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { onDelete() })
            }
        }
    }
}

object AppUtils {
    fun getInstalledApps(packageManager: android.content.pm.PackageManager): List<AppInfo> {
        return try {
            packageManager.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map { appInfo ->
                    AppInfo(
                        packageName = appInfo.packageName,
                        label = packageManager.getApplicationLabel(appInfo).toString(),
                        icon = packageManager.getApplicationIcon(appInfo)
                    )
                }
                .sortedBy { it.label }
        } catch (e: Exception) {
            emptyList()
        }
    }
}