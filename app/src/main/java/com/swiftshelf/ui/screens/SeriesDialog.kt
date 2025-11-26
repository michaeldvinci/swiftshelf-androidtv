package com.swiftshelf.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.swiftshelf.data.model.SeriesBookItem
import com.swiftshelf.data.model.SeriesWithBooks

@Composable
fun SeriesDialog(
    series: SeriesWithBooks,
    hostUrl: String,
    apiToken: String,
    onDismiss: () -> Unit,
    onBookClick: (String) -> Unit  // Pass book ID for fetching full details
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Blurred background using first book's cover
            val firstBookCover = series.books?.firstOrNull()?.let { book ->
                "$hostUrl/api/items/${book.id}/cover?token=$apiToken"
            }

            if (firstBookCover != null) {
                Image(
                    painter = rememberAsyncImagePainter(firstBookCover),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f
                )
            }

            // Content card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxSize()
                ) {
                    // Series title
                    Text(
                        text = series.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Book count and duration
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        val count = series.books?.size ?: 0
                        if (count > 0) {
                            Text(
                                text = "$count books",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        series.totalDuration?.let { duration ->
                            val hours = (duration / 3600).toInt()
                            val minutes = ((duration % 3600) / 60).toInt()
                            Text(
                                text = "Total: ${hours}h ${minutes}m",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Description
                    series.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Books list
                    Text(
                        text = "Books in Series",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Sort by series sequence
                        val sortedBooks = series.books
                            ?.sortedBy { it.media?.metadata?.series?.sequence?.toDoubleOrNull() ?: Double.MAX_VALUE }
                            ?: emptyList()

                        items(sortedBooks) { book ->
                            SeriesBookCard(
                                book = book,
                                hostUrl = hostUrl,
                                apiToken = apiToken,
                                onClick = { onBookClick(book.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesBookCard(
    book: SeriesBookItem,
    hostUrl: String,
    apiToken: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverUrl = "$hostUrl/api/items/${book.id}/cover?token=$apiToken"

    // Get sequence number from series metadata
    val sequence = book.media?.metadata?.series?.sequence

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sequence number
            sequence?.let {
                Text(
                    text = "#$it",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(40.dp)
                )
            }

            // Cover
            Image(
                painter = rememberAsyncImagePainter(coverUrl),
                contentDescription = book.media?.metadata?.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentScale = ContentScale.Crop
            )

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.media?.metadata?.title ?: "Unknown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )

                book.media?.duration?.let { duration ->
                    val hours = (duration / 3600).toInt()
                    val minutes = ((duration % 3600) / 60).toInt()
                    Text(
                        text = "${hours}h ${minutes}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
