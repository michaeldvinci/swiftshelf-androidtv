package com.swiftshelf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swiftshelf.R

@Composable
fun SettingsScreen(
    itemLimit: Int,
    progressBarColor: String,
    preferredPlaybackSpeed: Float,
    onItemLimitChange: (Int) -> Unit,
    onProgressBarColorChange: (String) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(48.dp)
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Library Settings Section
        Text(
            text = "Library Settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                var sliderValue by remember(itemLimit) { mutableFloatStateOf(itemLimit.toFloat()) }

                Text(
                    text = "Items per library: ${sliderValue.toInt()}",
                    style = MaterialTheme.typography.titleMedium
                )

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onItemLimitChange(sliderValue.toInt()) },
                    valueRange = 5f..50f,
                    steps = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Text(
                    text = "Number of items to load per library",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Appearance Section
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Progress Bar Color",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val colors = listOf(
                    "Yellow", "Red", "Green", "Blue", "Purple", "Orange", "Pink", "Teal", "Rainbow"
                )

                colors.forEach { color ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = progressBarColor == color,
                            onClick = { onProgressBarColorChange(color) }
                        )
                        Text(
                            text = color,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // Playback Section
        Text(
            text = "Playback",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                var sliderSpeed by remember(preferredPlaybackSpeed) { mutableFloatStateOf(preferredPlaybackSpeed) }

                Text(
                    text = "Preferred Playback Speed: ${String.format("%.1f", sliderSpeed)}×",
                    style = MaterialTheme.typography.titleMedium
                )

                Slider(
                    value = sliderSpeed,
                    onValueChange = { sliderSpeed = it },
                    onValueChangeFinished = { onPlaybackSpeedChange(sliderSpeed) },
                    valueRange = 0.5f..3.0f,
                    steps = 24,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Text(
                    text = "Default playback speed for new audiobooks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // User Account Section
        Text(
            text = "User Account",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.logout))
                }
            }
        }
    }
}
