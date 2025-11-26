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

    suspend fun getSeriesWithBooks(
        libraryId: String,
        seriesId: String
    ): Result<SeriesWithBooks> = withContext(Dispatchers.IO) {
        try {
            // Filter by series ID using the id. prefix
            val filter = "id.${seriesId}"
            val response = api.getLibrarySeries(libraryId, filter)
            if (response.isSuccessful && response.body() != null) {
                val series = response.body()!!.results.firstOrNull()
                if (series != null) {
                    Result.success(series)
                } else {
                    Result.failure(Exception("Series not found"))
                }
            } else {
                Result.failure(Exception("Failed to fetch series: ${response.code()}"))
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

    // Playback Session Methods (Canonical ABS API)
    suspend fun startPlaybackSession(
        itemId: String,
        deviceId: String,
        model: String,
        deviceName: String
    ): Result<PlaybackSessionResponse> = withContext(Dispatchers.IO) {
        try {
            val request = SessionStartRequest(
                deviceInfo = DeviceInfo(
                    deviceId = deviceId,
                    model = model,
                    deviceName = deviceName
                ),
                supportedMimeTypes = listOf("audio/mpeg", "audio/mp4", "audio/flac", "audio/x-m4a", "audio/aac")
            )
            val response = api.startPlaybackSession(itemId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to start session: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncSession(
        sessionId: String,
        currentTime: Double,
        timeListened: Double,
        duration: Double
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = SessionSyncRequest(
                currentTime = currentTime,
                timeListened = timeListened,
                duration = duration
            )
            val response = api.syncSession(sessionId, request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to sync session: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun closeSession(
        sessionId: String,
        currentTime: Double? = null,
        timeListened: Double? = null,
        duration: Double? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = if (currentTime != null && duration != null) {
                SessionSyncRequest(
                    currentTime = currentTime,
                    timeListened = timeListened ?: 0.0,
                    duration = duration
                )
            } else null

            val response = api.closeSession(sessionId, request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to close session: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
