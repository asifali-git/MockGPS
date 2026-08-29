package com.mockgps.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mockgps.util.LocationUtils

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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMockLocation)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(Modifier.padding(16.dp)) {
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
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
            }

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