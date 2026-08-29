package com.mockgps.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onVoiceInput: () -> Unit,
    onCoordinateInput: () -> Unit,
    isSearching: Boolean = false,
    hint: String = "Search by coordinates, city, country...",
    showVoiceButton: Boolean = true,
    showCoordinateButton: Boolean = true
) {
    var text by remember { mutableStateOf(query) }
    var showClear by remember { mutableStateOf(query.isNotBlank()) }

    androidx.compose.runtime.LaunchedEffect(query) {
        text = query
        showClear = query.isNotBlank()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                TextField(
                    value = text,
                    onValueChange = { newText ->
                        text = newText
                        showClear = newText.isNotBlank()
                        onQueryChange(newText)
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearch(text) }
                    ),
                    visualTransformation = VisualTransformation.None,
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                if (showClear) {
                    IconButton(onClick = {
                        text = ""
                        onQueryChange("")
                        onClear()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (showVoiceButton) {
            IconButton(onClick = onVoiceInput, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Mic, contentDescription = "Voice search", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (showCoordinateButton) {
            IconButton(onClick = onCoordinateInput, modifier = Modifier.size(48.dp)) {
                Text("XY", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun CoordinateInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit,
    initialLat: Double = 0.0,
    initialLng: Double = 0.0
) {
    var latText by remember { mutableStateOf(String.format("%.6f", initialLat)) }
    var lngText by remember { mutableStateOf(String.format("%.6f", initialLng)) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Coordinates") },
        text = {
            androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
                TextField(
                    value = latText,
                    onValueChange = { latText = it; errorText = null },
                    label = { Text("Latitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorText != null,
                    supportingText = { errorText?.let { Text(it) } }
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                TextField(
                    value = lngText,
                    onValueChange = { lngText = it; errorText = null },
                    label = { Text("Longitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorText != null
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching {
                    val lat = latText.toDouble()
                    val lng = lngText.toDouble()
                    if (lat in -90.0..90.0 && lng in -180.0..180.0) {
                        onConfirm(lat, lng)
                        onDismiss()
                    } else {
                        errorText = "Invalid coordinates"
                    }
                }.onFailure {
                    errorText = "Enter valid numbers"
                }
            }) {
                Text("Set Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}