package com.mockgps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mockgps.network.NominatimResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsList(
    modifier: Modifier = Modifier,
    results: List<NominatimResult>,
    onResultClick: (NominatimResult) -> Unit,
    onFavoriteToggle: (NominatimResult, Boolean) -> Unit,
    isFavorite: (NominatimResult) -> Boolean,
    emptyMessage: String = "No results found"
) {
    if (results.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.LocationOn, "No results", size = 48.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            items(results) { result ->
                SearchResultItem(
                    result = result,
                    onClick = { onResultClick(result) },
                    onFavoriteClick = { onFavoriteToggle(result, !isFavorite(result)) },
                    isFavorite = isFavorite(result)
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(
    result: NominatimResult,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    isFavorite: Boolean
) {
    val address = result.address
    val displayName = result.display_name
    val cityName = address?.getCityName() ?: ""
    val countryName = address?.country ?: ""
    val subLocalities = address?.getSubLocalities() ?: emptyList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = buildAnnotatedString {
                            append(displayName.split(",").firstOrNull() ?: displayName, SpanStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp))
                            if (cityName.isNotBlank() || countryName.isNotBlank()) {
                                append("\n")
                                val parts = mutableListOf<String>()
                                cityName.let { if (it.isNotBlank()) parts.add(it) }
                                countryName.let { if (it.isNotBlank()) parts.add(it) }
                                append(parts.joinToString(", "), SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp))
                            }
                        }
                    )
                    
                    if (subLocalities.isNotEmpty()) {
                        androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                        Text(
                            "Areas: ${subLocalities.take(3).joinToString(", ")}${if (subLocalities.size > 3) "..." else ""}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    
                    androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                    Text(
                        "Lat: ${LocationUtils.formatDecimalCoordinate(result.lat.toDouble(), true)}, Lng: ${LocationUtils.formatDecimalCoordinate(result.lon.toDouble(), false)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SearchHistoryList(
    modifier: Modifier = Modifier,
    history: List<com.mockgps.data.SearchHistory>,
    onItemClick: (com.mockgps.data.SearchHistory) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    if (history.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No search history", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Recent Searches", style = MaterialTheme.typography.titleMedium)
                androidx.compose.material3.TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }
            Divider()
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(history) { item ->
                    SearchHistoryItem(
                        item = item,
                        onClick = { onItemClick(item) },
                        onDelete = { onDeleteClick(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchHistoryItem(
    item: com.mockgps.data.SearchHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.displayName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}