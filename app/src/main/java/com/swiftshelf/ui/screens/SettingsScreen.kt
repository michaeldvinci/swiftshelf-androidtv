package com.swiftshelf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swiftshelf.R
import com.swiftshelf.data.model.LibrarySummary

@Composable
fun SettingsScreen(
    itemLimit: Int,
    progressBarColor: String,
    preferredPlaybackSpeed: Float,
    libraries: List<LibrarySummary>,
    selectedLibraryIds: Set<String>,
    onItemLimitChange: (Int) -> Unit,
    onProgressBarColorChange: (String) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onLibraryToggle: (String) -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstFocusRequester = remember { FocusRequester() }
    var showColorDialog by remember { mutableStateOf(false) }
    var showLibraryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        firstFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Libraries Selection
                SettingDropdownRow(
                    label = "Libraries",
                    value = "${selectedLibraryIds.size} selected",
                    onClick = { showLibraryDialog = true },
                    focusRequester = firstFocusRequester
                )

                // Items per Library
                SettingRow(
                    label = "Items per Library",
                    value = itemLimit.toString(),
                    onDecrease = { if (itemLimit > 5) onItemLimitChange(itemLimit - 5) },
                    onIncrease = { if (itemLimit < 50) onItemLimitChange(itemLimit + 5) }
                )

                // Playback Speed (+/- 0.1)
                SettingRow(
                    label = "Playback Speed",
                    value = "${String.format("%.1f", preferredPlaybackSpeed)}×",
                    onDecrease = {
                        onPlaybackSpeedChange((preferredPlaybackSpeed - 0.1f).coerceAtLeast(0.5f))
                    },
                    onIncrease = {
                        onPlaybackSpeedChange((preferredPlaybackSpeed + 0.1f).coerceAtMost(3.0f))
                    }
                )

                // Progress Bar Color (dropdown)
                SettingColorRow(
                    label = "Progress Bar Color",
                    value = progressBarColor,
                    color = getColorForName(progressBarColor),
                    onClick = { showColorDialog = true }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLogout,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.logout))
            }
        }
    )

    // Color selection dialog
    if (showColorDialog) {
        val colorOptions = listOf("Yellow", "Red", "Green", "Blue", "Purple", "Orange", "Pink", "Teal")
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Progress Bar Color") },
            text = {
                // 2-column grid layout
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.chunked(2).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowColors.forEach { colorName ->
                                var itemFocused by remember { mutableStateOf(false) }
                                Surface(
                                    onClick = {
                                        onProgressBarColorChange(colorName)
                                        showColorDialog = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .onFocusChanged { itemFocused = it.isFocused },
                                    color = when {
                                        itemFocused -> MaterialTheme.colorScheme.primaryContainer
                                        colorName == progressBarColor -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(getColorForName(colorName))
                                        )
                                        Text(
                                            text = colorName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (colorName == progressBarColor) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // Add empty spacer if odd number of items in last row
                            if (rowColors.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Library selection dialog
    if (showLibraryDialog) {
        AlertDialog(
            onDismissRequest = { showLibraryDialog = false },
            title = { Text("Select Libraries") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    if (libraries.isEmpty()) {
                        Text(
                            text = "No libraries available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        libraries.forEach { library ->
                            val isSelected = selectedLibraryIds.contains(library.id)
                            var itemFocused by remember { mutableStateOf(false) }
                            Surface(
                                onClick = { onLibraryToggle(library.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { itemFocused = it.isFocused },
                                color = when {
                                    itemFocused -> MaterialTheme.colorScheme.primaryContainer
                                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { onLibraryToggle(library.id) }
                                    )
                                    Text(
                                        text = library.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (library != libraries.last()) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLibraryDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onDecrease,
                modifier = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.widthIn(min = 50.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            IconButton(onClick = onIncrease) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }
}

@Composable
private fun SettingDropdownRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { isFocused = it.isFocused },
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Select"
                )
            }
        }
    }
}

@Composable
private fun SettingColorRow(
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Select"
                )
            }
        }
    }
}

private fun getColorForName(name: String): Color {
    return when (name) {
        "Yellow" -> Color.Yellow
        "Red" -> Color.Red
        "Green" -> Color.Green
        "Blue" -> Color.Blue
        "Purple" -> Color(0xFFBB86FC)
        "Orange" -> Color(0xFFFF9800)
        "Pink" -> Color(0xFFE91E63)
        "Teal" -> Color(0xFF009688)
        else -> Color.Yellow
    }
}
