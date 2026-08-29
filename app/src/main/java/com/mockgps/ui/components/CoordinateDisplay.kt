package com.mockgps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mockgps.R
import com.mockgps.util.LocationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoordinateDisplay(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    altitude: Double? = null,
    speed: Float = 0f,
    heading: Float = 0f,
    accuracy: Float = 0f,
    isMockLocation: Boolean = false,
    onCopyClick: () -> Unit,
    onNavigateClick: () -> Unit,
    showDetails: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isMockLocation) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            // Main coordinates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = buildAnnotatedString {
                            append("Lat: ", SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp))
                            append(LocationUtils.formatDecimalCoordinate(latitude, true, 8), SpanStyle(fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, fontSize = 16.sp))
                        }
                    )
                    Text(
                        text = buildAnnotatedString {
                            append("Lng: ", SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp))
                            append(LocationUtils.formatDecimalCoordinate(longitude, false, 8), SpanStyle(fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, fontSize = 16.sp))
                        }
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onCopyClick) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy coordinates", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateClick) {
                        Icon(Icons.Default.Navigation, contentDescription = "Navigate", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (showDetails) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                Divider()
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))

                // Details grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailItem(
                        icon = Icons.Default.MyLocation,
                        label = "Accuracy",
                        value = "${accuracy.toInt()}m"
                    )
                    if (altitude != null) {
                        DetailItem(
                            icon = Icons.Default.Navigation,
                            label = "Altitude",
                            value = "${altitude.toInt()}m"
                        )
                    }
                    if (speed > 0) {
                        DetailItem(
                            icon = Icons.Default.Navigation,
                            label = "Speed",
                            value = String.format("%.1f km/h", speed * 3.6)
                        )
                    }
                    if (heading > 0) {
                        DetailItem(
                            icon = Icons.Default.Navigation,
                            label = "Heading",
                            value = "${heading.toInt()}°"
                        )
                    }
                }
            }

            // DMS format
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "DMS: ${LocationUtils.formatCoordinate(latitude, true)}, ${LocationUtils.formatCoordinate(longitude, false)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (isMockLocation) {
                    Text(
                        "MOCK LOCATION ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    icon: androidx.compose.material.icons.filled.Icon,
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = "", size = 20.dp, tint = MaterialTheme.colorScheme.primary)
        androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}