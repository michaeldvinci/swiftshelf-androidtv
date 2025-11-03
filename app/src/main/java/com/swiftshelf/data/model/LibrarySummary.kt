package com.swiftshelf.data.model

data class LibrarySummary(
    val id: String,
    val name: String,
    val mediaType: String? = null
)

data class LibraryResponse(
    val libraries: List<LibrarySummary>
)
