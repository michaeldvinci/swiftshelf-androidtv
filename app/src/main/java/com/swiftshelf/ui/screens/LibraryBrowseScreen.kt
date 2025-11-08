package com.swiftshelf.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.swiftshelf.data.model.LibraryItem
import com.swiftshelf.data.model.progressFraction
import kotlinx.coroutines.delay

@Composable
fun LibraryBrowseScreen(
    libraryName: String?,
    libraryMediaType: String?,
    firstItemFocusRequester: FocusRequester? = null,
    recentItems: List<LibraryItem>,
    continueListeningItems: List<LibraryItem>,
    hostUrl: String,
    apiToken: String,
    progressBarColor: String = "Yellow",
    onItemClick: (LibraryItem) -> Unit,
    onRequestOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (libraryName == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select a library from the drawer",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val isEbookLibrary = libraryMediaType?.let { type ->
        type.equals("ebooks", ignoreCase = true) ||
        type.equals("books", ignoreCase = true) ||
        type.contains("ebook", ignoreCase = true)
    } == true

    // Track focused item for background and info panel
    var focusedItem by remember { mutableStateOf<LibraryItem?>(recentItems.firstOrNull() ?: continueListeningItems.firstOrNull()) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Full-screen background image
        focusedItem?.let { item ->
            val coverUrl = "$hostUrl/api/items/${item.id}/cover?token=$apiToken"

            Image(
                painter = rememberAsyncImagePainter(coverUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )

            // Scrim gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )
        }

        // Content block on left side
        focusedItem?.let { item ->
            val author = item.media?.metadata?.authors?.firstOrNull()?.name ?: "Unknown Author"
            val year = item.media?.metadata?.publishedYear ?: ""
            val durationHours = item.media?.duration?.let { (it / 3600).toInt() } ?: 0
            val durationMins = item.media?.duration?.let { ((it % 3600) / 60).toInt() } ?: 0
            val durationText = if (durationHours > 0) "${durationHours}h ${durationMins}m" else "${durationMins}m"

            androidx.compose.animation.AnimatedContent(
                targetState = item,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(500)
                    ) togetherWith androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(500)
                    )
                },
                label = "content_animation",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 60.dp, end = 30.dp, bottom = 30.dp)
                    .fillMaxWidth(0.5f)
            ) { animatedItem ->
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    // Metadata line
                    Text(
                        text = buildString {
                            append(author)
                            if (year.isNotEmpty()) append(" • $year")
                            append(" • $durationText")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Title
                    Text(
                        text = animatedItem.media?.metadata?.title ?: "Unknown Title",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    animatedItem.media?.metadata?.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Carousel at bottom-right
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
        ) {
            // Recent Items Carousel
            if (recentItems.isNotEmpty()) {
            val focusGoesToRecent = recentItems.isNotEmpty()

            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .fillMaxWidth()
            ) {
                itemsIndexed(recentItems, key = { _, item -> item.id }) { index, item ->
                    LibraryItemCard(
                        item = item,
                        hostUrl = hostUrl,
                        apiToken = apiToken,
                        isEbook = isEbookLibrary,
                        progressBarColor = progressBarColor,
                        onClick = { onItemClick(item) },
                        isFirst = index == 0,
                        onNavigateLeft = onRequestOpenDrawer,
                        focusRequester = if (focusGoesToRecent && index == 0) firstItemFocusRequester else null,
                        onFocusChanged = { focused ->
                            if (focused) focusedItem = item
                        }
                    )
                }
            }
        }

        // Continue Listening Carousel
        if (continueListeningItems.isNotEmpty()) {
            Text(
                text = "Continue Listening",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            val focusGoesToContinue = recentItems.isEmpty() && continueListeningItems.isNotEmpty()

            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(continueListeningItems, key = { _, item -> item.id }) { index, item ->
                    LibraryItemCard(
                        item = item,
                        hostUrl = hostUrl,
                        apiToken = apiToken,
                        isEbook = isEbookLibrary,
                        progressBarColor = progressBarColor,
                        onClick = { onItemClick(item) },
                        isFirst = index == 0,
                        onNavigateLeft = onRequestOpenDrawer,
                        focusRequester = if (focusGoesToContinue && index == 0) firstItemFocusRequester else null,
                        onFocusChanged = { focused ->
                            if (focused) focusedItem = item
                        }
                    )
                }
            }
        }
        }

        // Empty state
        if (recentItems.isEmpty() && continueListeningItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No items found in this library",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun LibraryItemCard(
    item: LibraryItem,
    hostUrl: String,
    apiToken: String,
    isEbook: Boolean = false,
    progressBarColor: String = "Yellow",
    onClick: () -> Unit,
    isFirst: Boolean = false,
    onNavigateLeft: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    // Scale to 1.1x for immersive list design
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1.0f)
    val coverWidth = 160.dp
    val coverAspectRatio = if (isEbook) 2f / 3f else 1f

    Column(
        modifier = modifier
            .width(coverWidth)
            .scale(scale)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                onFocusChanged?.invoke(focusState.isFocused)
            }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (
                    isFirst &&
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionLeft
                ) {
                    onNavigateLeft?.invoke()
                    true
                } else {
                    false
                }
            }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start
    ) {
        // Cover art
        Box(
            modifier = Modifier
                .width(coverWidth)
                .aspectRatio(coverAspectRatio)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (isFocused) {
                        Modifier.border(
                            BorderStroke(3.dp, Color.White),
                            RoundedCornerShape(8.dp)
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            val coverUrl = item.media?.coverPath?.let {
                "$hostUrl/api/items/${item.id}/cover?token=$apiToken"
            }

            val scaleMode = if (isEbook) ContentScale.Fit else ContentScale.Crop

            Image(
                painter = rememberAsyncImagePainter(coverUrl),
                contentDescription = item.media?.metadata?.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = scaleMode
            )

            val progressFraction = item.userMediaProgress?.progressFraction()?.toFloat() ?: 0f
            if (progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(getProgressBarColor(progressBarColor))
                    )
                }
            }
        }

        // Title - scrolling when focused
        val scrollState = rememberScrollState()
        val title = item.media?.metadata?.title ?: "Unknown"

        LaunchedEffect(isFocused) {
            if (isFocused) {
                delay(2000) // Wait 2 seconds before starting scroll
                while (isFocused) {
                    val maxScroll = scrollState.maxValue
                    if (maxScroll > 0) {
                        scrollState.animateScrollTo(
                            value = maxScroll,
                            animationSpec = tween(durationMillis = (maxScroll * 50), easing = LinearEasing)
                        )
                        delay(1000)
                        scrollState.scrollTo(0)
                        delay(2000)
                    } else {
                        break
                    }
                }
            } else {
                scrollState.scrollTo(0)
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .padding(top = 8.dp)
                .width(coverWidth)
                .horizontalScroll(scrollState, enabled = false),
            color = MaterialTheme.colorScheme.onBackground,
            softWrap = false
        )

        // Author
        item.media?.metadata?.authors?.firstOrNull()?.name?.let { author ->
            Text(
                text = author,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(coverWidth),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Duration
        item.media?.duration?.let { duration ->
            val hours = (duration / 3600).toInt()
            val minutes = ((duration % 3600) / 60).toInt()
            Text(
                text = "${hours}h ${minutes}m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun animateFloatAsState(targetValue: Float): State<Float> {
    return remember { mutableStateOf(targetValue) }.apply {
        value = targetValue
    }
}

private fun getProgressBarColor(colorName: String): Color {
    return when (colorName) {
        "Red" -> Color.Red
        "Green" -> Color.Green
        "Blue" -> Color.Blue
        "Yellow" -> Color.Yellow
        "Purple" -> Color(0xFFBB86FC)
        "Orange" -> Color(0xFFFF9800)
        "Pink" -> Color(0xFFE91E63)
        "Teal" -> Color(0xFF009688)
        else -> Color.Yellow
    }
}
