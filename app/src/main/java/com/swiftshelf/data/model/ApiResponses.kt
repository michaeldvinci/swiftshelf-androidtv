package com.swiftshelf.data.model

data class LibraryItemsResponse(
    val results: List<LibraryItem>,
    val total: Int?,
    val limit: Int?,
    val page: Int?
)

data class ProgressUpdateRequest(
    val libraryItemId: String,
    val duration: Double,
    val progress: Double,
    val currentTime: Double,
    val isFinished: Boolean
)
