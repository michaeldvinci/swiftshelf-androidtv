package com.swiftshelf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.swiftshelf.data.model.LibraryItem
import com.swiftshelf.data.model.SearchResponse

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun SearchScreen(
    searchQuery: String,
    searchResults: SearchResponse?,
    hostUrl: String,
    apiToken: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onItemClick: (LibraryItem) -> Unit,
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = narrator.name ?: "Unknown",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                narrator.numBooks?.let {
                                    Text(
                                        text = "$it books",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = seriesResult.series.name ?: "Unknown",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookSearchResultCard(
    item: LibraryItem,
    hostUrl: String,
    apiToken: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LibraryItemCard(
                item = item,
                hostUrl = hostUrl,
                apiToken = apiToken,
                onClick = onClick,
                modifier = Modifier.width(150.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.media?.metadata?.title ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium
                )

                item.media?.metadata?.authors?.firstOrNull()?.name?.let { author ->
                    Text(
                        text = "by $author",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item.media?.duration?.let { duration ->
                    val hours = (duration / 3600).toInt()
                    val minutes = ((duration % 3600) / 60).toInt()
                    Text(
                        text = "Duration: ${hours}h ${minutes}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
