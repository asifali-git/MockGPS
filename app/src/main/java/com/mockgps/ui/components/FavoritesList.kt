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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mockgps.data.FavoriteLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesList(
    modifier: Modifier = Modifier,
    favorites: List<FavoriteLocation>,
    onFavoriteClick: (FavoriteLocation) -> Unit,
    onEditClick: (FavoriteLocation) -> Unit,
    onDeleteClick: (FavoriteLocation) -> Unit
) {
    if (favorites.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.Favorite, "No favorites", size = 48.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Text("No saved locations", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tap the star on search results to save locations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    } else {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            androidx.compose.foundation.lazy.items(favorites) { favorite ->
                FavoriteItem(
                    favorite = favorite,
                    onClick = { onFavoriteClick(favorite) },
                    onEdit = { onEditClick(favorite) },
                    onDelete = { onDeleteClick(favorite) }
                )
            }
        }
    }
}

@Composable
fun FavoriteItem(
    favorite: FavoriteLocation,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                
                Column {
                    Text(
                        favorite.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                    if (favorite.address != null && favorite.address!!.isNotBlank()) {
                        Text(
                            favorite.address!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "${LocationUtils.formatDecimalCoordinate(favorite.latitude, true, 5)}, ${LocationUtils.formatDecimalCoordinate(favorite.longitude, false, 5)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddEditFavoriteDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double?, String?, String?) -> Unit,
    initialName: String = "",
    initialLat: Double = 0.0,
    initialLng: Double = 0.0,
    initialAlt: Double? = null,
    initialAddress: String? = null,
    initialCategory: String? = null
) {
    var name by mutableStateOf(initialName)
    var latText by mutableStateOf(LocationUtils.formatDecimalCoordinate(initialLat, true, 6))
    var lngText by mutableStateOf(LocationUtils.formatDecimalCoordinate(initialLng, false, 6))
    var altText by mutableStateOf(initialAlt?.toString() ?: "")
    var address by mutableStateOf(initialAddress ?: "")
    var category by mutableStateOf(initialCategory ?: "Favorite")
    var errorText by mutableStateOf<String?>(null)
    
    val categories = listOf("Home", "Work", "Favorite", "Travel", "Testing", "Custom")

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isBlank()) "Add Favorite" else "Edit Favorite") },
        text = {
            Column(Modifier.padding(16.dp).width(320.dp)) {
                androidx.compose.material3.TextField(
                    value = name,
                    onValueChange = { name = it; errorText = null },
                    label = { Text("Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorText != null
                )
                
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.TextField(
                        value = latText,
                        onValueChange = { latText = it; errorText = null },
                        label = { Text("Latitude") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.foundation.text.KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = errorText != null
                    )
                    androidx.compose.material3.TextField(
                        value = lngText,
                        onValueChange = { lngText = it; errorText = null },
                        label = { Text("Longitude") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.foundation.text.KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = errorText != null
                    )
                }
                
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                
                androidx.compose.material3.TextField(
                    value = altText,
                    onValueChange = { altText = it },
                    label = { Text("Altitude (optional, meters)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.foundation.text.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                
                androidx.compose.material3.TextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.TextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Icon(Icons.Default.Favorite, contentDescription = "") }
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                try {
                    val lat = latText.toDouble()
                    val lng = lngText.toDouble()
                    if (LocationUtils.isValidCoordinate(lat, lng) && name.isNotBlank()) {
                        val alt = altText.toDoubleOrNull()
                        onSave(
                            name, lat, lng, alt,
                            address.takeIf { it.isNotBlank() },
                            category.takeIf { it.isNotBlank() && it != "Category" }
                        )
                        onDismiss()
                    } else {
                        errorText = "Please enter valid name and coordinates"
                    }
                } catch (e: NumberFormatException) {
                    errorText = "Please enter valid numbers for coordinates"
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}