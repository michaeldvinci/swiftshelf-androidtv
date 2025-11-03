package com.swiftshelf.data.model

data class SearchResponse(
    val book: List<LibraryItem>?,
    val narrators: List<NarratorResult>?,
    val series: List<SeriesResult>?
)

data class NarratorResult(
    val name: String?,
    val numBooks: Int?
)

data class SeriesResult(
    val id: String?,
    val name: String?,
    val sequence: String?
)
