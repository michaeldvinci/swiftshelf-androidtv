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
