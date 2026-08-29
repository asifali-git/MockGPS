package com.mockgps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mockgps.data.FavoriteLocation
import com.mockgps.util.LocationUtils

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
            modifier = modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.Favorite, "No favorites", size = 48.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Text("No saved locations", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(favorites) { favorite ->
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
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp))
                }

                Column {
                    Text(favorite.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    favorite.address?.let { addr ->
                        if (addr.isNotBlank()) {
                            Text(addr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                    Text(
                        "${LocationUtils.formatDecimalCoordinate(favorite.latitude, true, 5)}, ${LocationUtils.formatDecimalCoordinate(favorite.longitude, false, 5)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onEdit() }.padding(8.dp))
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { onDelete() }.padding(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var name by remember { mutableStateOf(initialName) }
    var latText by remember { mutableStateOf(String.format("%.6f", initialLat)) }
    var lngText by remember { mutableStateOf(String.format("%.6f", initialLng)) }
    var altText by remember { mutableStateOf(initialAlt?.toString() ?: "") }
    var address by remember { mutableStateOf(initialAddress ?: "") }
    var category by remember { mutableStateOf(initialCategory ?: "Favorite") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val categories = listOf("Home", "Work", "Favorite", "Travel", "Testing")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isBlank()) "Add Favorite" else "Edit Favorite") },
        text = {
            Column(Modifier.padding(16.dp)) {
                TextField(value = name, onValueChange = { name = it; errorText = null }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = errorText != null)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(value = latText, onValueChange = { latText = it }, label = { Text("Latitude") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    TextField(value = lngText, onValueChange = { lngText = it }, label = { Text("Longitude") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                TextField(value = address, onValueChange = { address = it }, label = { Text("Address (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = false, onExpandedChange = {}, modifier = Modifier.fillMaxWidth()) {
                    TextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, readOnly = true, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching {
                    val lat = latText.toDouble()
                    val lng = lngText.toDouble()
                    if (lat in -90.0..90.0 && lng in -180.0..180.0 && name.isNotBlank()) {
                        onSave(name, lat, lng, altText.toDoubleOrNull(), address.takeIf { it.isNotBlank() }, category)
                        onDismiss()
                    } else {
                        errorText = "Enter valid name and coordinates"
                    }
                }.onFailure { errorText = "Enter valid numbers" }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}