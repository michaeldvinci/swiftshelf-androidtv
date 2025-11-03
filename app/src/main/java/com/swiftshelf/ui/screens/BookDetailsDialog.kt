package com.swiftshelf.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.swiftshelf.R
import com.swiftshelf.data.model.LibraryItem

@Composable
fun BookDetailsDialog(
    item: LibraryItem,
    hostUrl: String,
    apiToken: String,
    onDismiss: () -> Unit,
    onPlayClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Blurred background
            val coverUrl = item.media?.coverPath?.let {
                "$hostUrl/api/items/${item.id}/cover?token=$apiToken"
            }

            Image(
                painter = rememberAsyncImagePainter(coverUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.3f
            )

            // Content card
            Card(
                modifier = Modifier
                    .width(900.dp)
                    .heightIn(max = 700.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(48.dp)
                        .fillMaxWidth()
                ) {
                    // Cover art
                    Image(
                        painter = rememberAsyncImagePainter(coverUrl),
                        contentDescription = item.media?.metadata?.title,
                        modifier = Modifier
                            .size(380.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(48.dp))

                    // Details
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Title
                        Text(
                            text = item.media?.metadata?.title ?: "Unknown",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Series
                        item.media?.metadata?.series?.firstOrNull()?.let { series ->
                            Text(
                                text = series.name ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Author
                        item.media?.metadata?.authors?.firstOrNull()?.name?.let { author ->
                            Text(
                                text = "by $author",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Duration
                        item.media?.duration?.let { duration ->
                            val hours = (duration / 3600).toInt()
                            val minutes = ((duration % 3600) / 60).toInt()
                            Text(
                                text = "Duration: ${hours}h ${minutes}m",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        // Narrators
                        item.media?.metadata?.narrators?.let { narrators ->
                            if (narrators.isNotEmpty()) {
                                Text(
                                    text = "Narrated by: ${narrators.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Description
                        item.media?.metadata?.description?.let { description ->
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Play button
                        Button(
                            onClick = onPlayClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.play),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
