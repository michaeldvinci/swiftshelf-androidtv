package com.swiftshelf.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.swiftshelf.data.model.LibraryItem

@Composable
fun CompactPlayer(
    item: LibraryItem?,
    isPlaying: Boolean,
    currentTime: Long,
    duration: Long,
    playbackSpeed: Float,
    hostUrl: String,
    apiToken: String,
    onPlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onPreviousTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onSpeedDecrease: () -> Unit,
    onSpeedIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column {
            // Progress bar
            val progress = if (duration > 0) currentTime.toFloat() / duration.toFloat() else 0f
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color.Yellow
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cover art and metadata
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val coverUrl = item.media?.coverPath?.let {
                        "$hostUrl/api/items/${item.id}/cover?token=$apiToken"
                    }

                    Image(
                        painter = rememberAsyncImagePainter(coverUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.media?.metadata?.title ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        item.media?.metadata?.authors?.firstOrNull()?.name?.let { author ->
                            Text(
                                text = author,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "${formatTime(currentTime)} / ${formatTime(duration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPreviousTrack) {
                            Icon(
                                Icons.Filled.SkipPrevious,
                                contentDescription = "Previous Track"
                            )
                        }

                        IconButton(onClick = onSkipBackward) {
                            Icon(
                                Icons.Filled.Replay10,
                                contentDescription = "Skip Backward"
                            )
                        }

                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(onClick = onSkipForward) {
                            Icon(
                                Icons.Filled.Forward30,
                                contentDescription = "Skip Forward"
                            )
                        }

                        IconButton(onClick = onNextTrack) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "Next Track"
                            )
                        }
                    }

                    // Playback speed
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = onSpeedDecrease) {
                            Text("−", style = MaterialTheme.typography.titleLarge)
                        }

                        Text(
                            text = "${String.format("%.1f", playbackSpeed)}×",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.widthIn(min = 50.dp)
                        )

                        TextButton(onClick = onSpeedIncrease) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
