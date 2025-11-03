package com.swiftshelf.data.repository

import com.swiftshelf.data.model.*
import com.swiftshelf.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudiobookRepository {

    private val api = RetrofitClient.getApi()

    suspend fun getLibraries(): Result<List<LibrarySummary>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLibraries()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.libraries)
            } else {
                Result.failure(Exception("Failed to fetch libraries: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLibraryItems(
        libraryId: String,
        limit: Int = 50,
        sort: String = "addedAt",
        descending: Boolean = true
    ): Result<List<LibraryItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLibraryItems(
                libraryId = libraryId,
                limit = limit,
                sort = sort,
                descending = if (descending) 1 else 0
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.results)
            } else {
                Result.failure(Exception("Failed to fetch library items: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getContinueListening(
        libraryId: String,
        limit: Int = 50
    ): Result<List<LibraryItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLibraryItems(
                libraryId = libraryId,
                limit = limit,
                sort = "updatedAt",
                descending = 1
            )
            if (response.isSuccessful && response.body() != null) {
                // Filter items with progress
                val itemsWithProgress = response.body()!!.results.filter {
                    val fraction = it.userMediaProgress?.progressFraction() ?: 0.0
                    fraction > 0.0
                }
                Result.success(itemsWithProgress)
            } else {
                Result.failure(Exception("Failed to fetch continue listening: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchLibrary(
        libraryId: String,
        query: String,
        limit: Int = 5
    ): Result<SearchResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchLibrary(libraryId, query, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to search: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItemDetails(itemId: String): Result<LibraryItem> = withContext(Dispatchers.IO) {
        try {
            val response = api.getItemDetails(itemId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch item details: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProgress(
        libraryItemId: String,
        duration: Double,
        currentTime: Double,
        isFinished: Boolean = false
    ): Result<UserMediaProgress> = withContext(Dispatchers.IO) {
        try {
            val progress = if (duration > 0) currentTime / duration else 0.0
            val request = ProgressUpdateRequest(
                libraryItemId = libraryItemId,
                duration = duration,
                progress = progress,
                currentTime = currentTime,
                isFinished = isFinished
            )
            val response = api.updateProgress(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update progress: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProgress(itemId: String): Result<UserMediaProgress> = withContext(Dispatchers.IO) {
        try {
            val response = api.getProgress(itemId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch progress: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
