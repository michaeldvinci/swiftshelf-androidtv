package com.swiftshelf.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.swiftshelf.data.model.Chapter
import com.swiftshelf.data.model.LibraryItem

@Composable
fun BookDetailsDialog(
    item: LibraryItem,
    hostUrl: String,
    apiToken: String,
    onDismiss: () -> Unit,
    onPlayClick: () -> Unit,
    onPlayFromChapter: ((Double) -> Unit)? = null,
    onReadClick: (() -> Unit)? = null
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
                    .width(1100.dp)
                    .heightIn(max = 750.dp),
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
                            .width(380.dp)
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(48.dp))

                    // Details column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Scrollable metadata section
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Title
                            Text(
                                text = item.media?.metadata?.title ?: "Unknown",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Series
                            item.media?.metadata?.series?.firstOrNull()?.let { series ->
                                Text(
                                    text = buildString {
                                        append(series.name ?: "")
                                        series.sequence?.let { append(" #$it") }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
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
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Narrated by: ${narrators.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Description
                            item.media?.metadata?.description?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 6,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Chapters section
                            val chapters = item.media?.chapters
                            if (!chapters.isNullOrEmpty()) {
                                ChaptersSection(
                                    chapters = chapters,
                                    onChapterClick = { chapter ->
                                        chapter.start?.let { startTime ->
                                            onPlayFromChapter?.invoke(startTime)
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Play button
                            Button(
                                onClick = onPlayClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Play",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            // Read button (only show if ebook exists)
                            // TODO: Check for ebook presence when LibraryItem model supports it
                            if (onReadClick != null) {
                                OutlinedButton(
                                    onClick = onReadClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Book,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Read",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChaptersSection(
    chapters: List<Chapter>,
    onChapterClick: (Chapter) -> Unit
) {
    Column {
        Text(
            text = "Chapters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Show chapters in a fixed-height scrollable list
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 200.dp)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(chapters) { index, chapter ->
                ChapterRow(
                    index = index + 1,
                    chapter = chapter,
                    onClick = { onChapterClick(chapter) }
                )
            }
        }
    }
}

@Composable
private fun ChapterRow(
    index: Int,
    chapter: Chapter,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )

            Text(
                text = chapter.title ?: "Chapter $index",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration
        val start = chapter.start ?: 0.0
        val end = chapter.end ?: 0.0
        val durationSeconds = (end - start).toLong()
        if (durationSeconds > 0) {
            Text(
                text = formatDuration(durationSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}
