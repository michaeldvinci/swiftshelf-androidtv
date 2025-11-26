package com.swiftshelf.data.model

data class SearchResponse(
    val book: List<BookSearchResult>?,
    val narrators: List<NarratorResult>?,
    val series: List<SeriesSearchResult>?
)

/** Book search results are wrapped in a libraryItem field */
data class BookSearchResult(
    val libraryItem: LibraryItem
)

data class NarratorResult(
    val name: String?,
    val numBooks: Int?
)

/** Series search results are wrapped in a series field */
data class SeriesSearchResult(
    val series: SearchSeriesInfo
)

data class SearchSeriesInfo(
    val id: String?,
    val name: String?
)

/** Response from GET /api/libraries/{id}/series */
data class LibrarySeriesResponse(
    val results: List<SeriesWithBooks>,
    val total: Int?,
    val limit: Int?,
    val page: Int?
)

/** A series with its books from the library series endpoint */
data class SeriesWithBooks(
    val id: String,
    val name: String,
    val description: String?,
    val books: List<SeriesBookItem>?,
    val addedAt: Long?,
    val totalDuration: Double?
)

/** A book within a series - simplified structure from series endpoint */
data class SeriesBookItem(
    val id: String,
    val libraryId: String?,
    val mediaType: String?,
    val media: SeriesBookMedia?
)

data class SeriesBookMedia(
    val metadata: SeriesBookMetadata?,
    val duration: Double?
)

data class SeriesBookMetadata(
    val title: String?,
    val authorName: String?,
    val series: SeriesSequenceInfo?
)

data class SeriesSequenceInfo(
    val id: String?,
    val name: String?,
    val sequence: String?
)
