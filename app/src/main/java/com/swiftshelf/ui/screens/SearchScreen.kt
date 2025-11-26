package com.swiftshelf.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.swiftshelf.data.model.LibraryItem
import com.swiftshelf.data.model.SearchResponse

@Composable
fun SearchScreen(
    searchQuery: String,
    searchResults: SearchResponse?,
    hostUrl: String,
    apiToken: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onItemClick: (LibraryItem) -> Unit,
    onSeriesClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search") },
            placeholder = { Text("Search for books, narrators, or series...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )

        // Search results
        searchResults?.let { results ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Books
                if (!results.book.isNullOrEmpty()) {
                    item {
                        Text(
                            text = "Books",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(results.book) { bookResult ->
                        BookSearchResultCard(
                            item = bookResult.libraryItem,
                            hostUrl = hostUrl,
                            apiToken = apiToken,
                            onClick = { onItemClick(bookResult.libraryItem) }
                        )
                    }
                }

                // Narrators
                if (!results.narrators.isNullOrEmpty()) {
                    item {
                        Text(
                            text = "Narrators",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(results.narrators) { narrator ->
                        NarratorResultCard(
                            name = narrator.name ?: "Unknown",
                            numBooks = narrator.numBooks
                        )
                    }
                }

                // Series
                if (!results.series.isNullOrEmpty()) {
                    item {
                        Text(
                            text = "Series",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(results.series) { seriesResult ->
                        SeriesResultCard(
                            name = seriesResult.series.name ?: "Unknown",
                            onClick = { seriesResult.series.id?.let { onSeriesClick(it) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NarratorResultCard(
    name: String,
    numBooks: Int?
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = { /* Narrator search not implemented yet */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            numBooks?.let {
                Text(
                    text = "$it books",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SeriesResultCard(
    name: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun BookSearchResultCard(
    item: LibraryItem,
    hostUrl: String,
    apiToken: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverUrl = item.media?.coverPath?.let {
        "$hostUrl/api/items/${item.id}/cover?token=$apiToken"
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover image
            Image(
                painter = rememberAsyncImagePainter(coverUrl),
                contentDescription = item.media?.metadata?.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.media?.metadata?.title ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )

                item.media?.metadata?.authors?.firstOrNull()?.name?.let { author ->
                    Text(
                        text = "by $author",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item.media?.duration?.let { duration ->
                    val hours = (duration / 3600).toInt()
                    val minutes = ((duration % 3600) / 60).toInt()
                    Text(
                        text = "Duration: ${hours}h ${minutes}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
