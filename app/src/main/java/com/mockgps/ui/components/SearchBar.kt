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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.keyboard.ImeAction
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mockgps.R
import com.mockgps.util.LocationUtils

@OptIn(ExperimentalMaterial3Api::class)
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
    hint: String = "Search by coordinates, city, country, area code...",
    showVoiceButton: Boolean = true,
    showCoordinateButton: Boolean = true
) {
    var text by mutableStateOf(query)
    var showClear by mutableStateOf(query.isNotBlank())

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
        // Main search field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(24.dp)
                )
        ) {
            TextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    showClear = newText.isNotBlank()
                    onQueryChange(newText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                placeholder = { Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
                    onSearch = { onSearch(text) }
                ),
                visualTransformation = VisualTransformation { it },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )

            // Clear button
            if (showClear) {
                IconButton(
                    onClick = {
                        text = ""
                        onQueryChange("")
                        onClear()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Voice input button
        if (showVoiceButton) {
            IconButton(
                onClick = onVoiceInput,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice search", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Coordinate input button
        if (showCoordinateButton) {
            IconButton(
                onClick = onCoordinateInput,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_coordinates),
                    contentDescription = "Coordinate input",
                    tint = MaterialTheme.colorScheme.primary
                )
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
    var latText by mutableStateOf(LocationUtils.formatDecimalCoordinate(initialLat, true))
    var lngText by mutableStateOf(LocationUtils.formatDecimalCoordinate(initialLng, false))
    var errorText by mutableStateOf<String?>(null)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Coordinates") },
        text = {
            Column(Modifier.padding(16.dp).width(300.dp)) {
                androidx.compose.material3.TextField(
                    value = latText,
                    onValueChange = { latText = it; errorText = null },
                    label = { Text("Latitude") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.foundation.text.KeyboardType.Number,
                        imeAction = androidx.compose.ui.input.keyboard.ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorText != null,
                    supportingText = { if (errorText != null) Text(errorText!!) }
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                androidx.compose.material3.TextField(
                    value = lngText,
                    onValueChange = { lngText = it; errorText = null },
                    label = { Text("Longitude") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.foundation.text.KeyboardType.Number,
                        imeAction = androidx.compose.ui.input.keyboard.ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorText != null
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                try {
                    val lat = latText.toDouble()
                    val lng = lngText.toDouble()
                    if (LocationUtils.isValidCoordinate(lat, lng)) {
                        onConfirm(lat, lng)
                        onDismiss()
                    } else {
                        errorText = "Invalid coordinates. Lat: -90 to 90, Lng: -180 to 180"
                    }
                } catch (e: NumberFormatException) {
                    errorText = "Please enter valid numbers"
                }
            }) {
                Text("Set Location")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}