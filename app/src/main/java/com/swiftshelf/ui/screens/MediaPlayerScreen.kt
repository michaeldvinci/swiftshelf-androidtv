package com.swiftshelf.ui.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.swiftshelf.data.model.Chapter
import com.swiftshelf.data.model.LibraryItem

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MediaPlayerScreen(
    item: LibraryItem?,
    isPlaying: Boolean,
    currentTimeMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    hostUrl: String,
    apiToken: String,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onPreviousTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onSpeedDecrease: () -> Unit,
    onSpeedIncrease: () -> Unit,
    onSeek: (Long) -> Unit
) {
    // Show loading if item is null (shouldn't happen with new flow)
    if (item == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val playPauseFocusRequester = remember { FocusRequester() }
    var showChapterMenu by remember { mutableStateOf(false) }

    // Handle back button properly on Android TV
    BackHandler(enabled = true) {
        if (showChapterMenu) {
            showChapterMenu = false
        } else {
            onDismiss()
        }
    }

    // Request focus on play/pause button when screen opens
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        playPauseFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background gradient overlay on cover art
        val coverUrl = "$hostUrl/api/items/${item.id}/cover?token=$apiToken"

        Box(modifier = Modifier.fillMaxSize()) {
            // Background blurred cover (dimmed)
            Image(
                painter = rememberAsyncImagePainter(coverUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentScale = ContentScale.Crop,
                alpha = 0.15f
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.9f),
                                Color.Black
                            )
                        )
                    )
            )
        }

        // Main content - centered player
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            val maxHeight = maxHeight
            val maxWidth = maxWidth

            // Dynamic cover size: use smaller of width/height, capped at reasonable size
            val coverSize = minOf(
                maxHeight * 0.35f, // 35% of height
                maxWidth * 0.25f,  // 25% of width
                200.dp             // Max 200dp
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Cover Art - dynamically sized
                Image(
                    painter = rememberAsyncImagePainter(coverUrl),
                    contentDescription = "Cover Art",
                    modifier = Modifier
                        .size(coverSize)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = item.media?.metadata?.title ?: "Unknown",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                // Author
                item.media?.metadata?.authors?.firstOrNull()?.name?.let { author ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Current chapter name
                val currentChapter = findCurrentChapter(
                    chapters = item.media?.chapters,
                    currentTimeMs = currentTimeMs
                )
                currentChapter?.title?.let { chapterTitle ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress slider
                ProgressSlider(
                    currentTimeMs = currentTimeMs,
                    durationMs = durationMs
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Transport controls
                TransportControls(
                    isPlaying = isPlaying,
                    playbackSpeed = playbackSpeed,
                    playPauseFocusRequester = playPauseFocusRequester,
                    onPlayPause = onPlayPause,
                    onSkipBackward = onSkipBackward,
                    onSkipForward = onSkipForward,
                    onPreviousTrack = onPreviousTrack,
                    onNextTrack = onNextTrack,
                    onSpeedDecrease = onSpeedDecrease,
                    onSpeedIncrease = onSpeedIncrease
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Chapters button
                val chapters = item.media?.chapters
                if (!chapters.isNullOrEmpty()) {
                    OutlinedButton(
                        onClick = { showChapterMenu = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chapters (${chapters.size})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Close button in top-right corner
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Chapter menu overlay
        AnimatedVisibility(
            visible = showChapterMenu,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            ChapterMenuOverlay(
                chapters = item.media?.chapters ?: emptyList(),
                currentTimeMs = currentTimeMs,
                onChapterClick = { chapter ->
                    chapter.start?.let { startTime ->
                        onSeek((startTime * 1000).toLong())
                    }
                    showChapterMenu = false
                },
                onDismiss = { showChapterMenu = false }
            )
        }
    }
}

@Composable
private fun ProgressSlider(
    currentTimeMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) currentTimeMs.toFloat() / durationMs.toFloat() else 0f

    Column(
        modifier = modifier.fillMaxWidth(0.7f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentTimeMs),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = formatTime(durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    playbackSpeed: Float,
    playPauseFocusRequester: FocusRequester,
    onPlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onPreviousTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onSpeedDecrease: () -> Unit,
    onSpeedIncrease: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous track
        TransportButton(
            onClick = onPreviousTrack,
            icon = Icons.Default.SkipPrevious,
            contentDescription = "Previous Chapter"
        )

        // Skip backward
        TransportButton(
            onClick = onSkipBackward,
            icon = Icons.Default.Replay10,
            contentDescription = "Skip Back 15 seconds"
        )

        // Play/Pause (larger)
        var playPauseFocused by remember { mutableStateOf(false) }
        Surface(
            onClick = onPlayPause,
            modifier = Modifier
                .size(56.dp)
                .focusRequester(playPauseFocusRequester)
                .onFocusChanged { playPauseFocused = it.isFocused },
            shape = CircleShape,
            color = if (playPauseFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            border = if (playPauseFocused) androidx.compose.foundation.BorderStroke(3.dp, Color.White) else null
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
        }

        // Skip forward
        TransportButton(
            onClick = onSkipForward,
            icon = Icons.Default.Forward30,
            contentDescription = "Skip Forward 30 seconds"
        )

        // Next track
        TransportButton(
            onClick = onNextTrack,
            icon = Icons.Default.SkipNext,
            contentDescription = "Next Chapter"
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Playback speed controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TransportButton(
                onClick = onSpeedDecrease,
                text = "−",
                contentDescription = "Decrease Speed"
            )

            Text(
                text = String.format("%.1fx", playbackSpeed),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.Center
            )

            TransportButton(
                onClick = onSpeedIncrease,
                text = "+",
                contentDescription = "Increase Speed"
            )
        }
    }
}

@Composable
private fun TransportButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    text: String? = null,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White.copy(alpha = 0.2f)
    }

    // Use Surface for better focus handling on TV
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = CircleShape,
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
            } else if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ChapterMenuOverlay(
    chapters: List<Chapter>,
    currentTimeMs: Long,
    onChapterClick: (Chapter) -> Unit,
    onDismiss: () -> Unit
) {
    val currentTimeSeconds = currentTimeMs / 1000.0
    val firstChapterFocusRequester = remember { FocusRequester() }

    // Request focus on first chapter when menu opens
    LaunchedEffect(Unit) {
        if (chapters.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            try {
                firstChapterFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Focus request might fail if not yet attached
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 600.dp)
                .clickable(enabled = false) { }, // Prevent click-through
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chapters",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = Color.White.copy(alpha = 0.2f))

                Spacer(modifier = Modifier.height(16.dp))

                // Chapter list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(chapters) { index, chapter ->
                        val chapterStart = chapter.start ?: 0.0
                        val chapterEnd = chapter.end ?: 0.0
                        val isCurrentChapter = currentTimeSeconds >= chapterStart && currentTimeSeconds < chapterEnd

                        ChapterMenuItem(
                            chapter = chapter,
                            index = index + 1,
                            isCurrentChapter = isCurrentChapter,
                            firstFocusRequester = if (index == 0) firstChapterFocusRequester else null,
                            onClick = { onChapterClick(chapter) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterMenuItem(
    chapter: Chapter,
    index: Int,
    isCurrentChapter: Boolean,
    firstFocusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isFocused -> MaterialTheme.colorScheme.primary
        isCurrentChapter -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .then(if (firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier)
            .focusable()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrentChapter) Color.White else Color.Gray,
                modifier = Modifier.width(32.dp)
            )

            Column {
                Text(
                    text = chapter.title ?: "Chapter $index",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (isCurrentChapter) {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        chapter.start?.let { startTime ->
            Text(
                text = formatTimeSeconds(startTime),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
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

private fun formatTimeSeconds(seconds: Double): String {
    val totalSeconds = seconds.toLong()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

private fun findCurrentChapter(chapters: List<Chapter>?, currentTimeMs: Long): Chapter? {
    if (chapters.isNullOrEmpty()) return null
    val currentTimeSeconds = currentTimeMs / 1000.0
    return chapters.find { chapter ->
        val start = chapter.start ?: 0.0
        val end = chapter.end ?: Double.MAX_VALUE
        currentTimeSeconds >= start && currentTimeSeconds < end
    }
}
