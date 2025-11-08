package com.swiftshelf.data.network

import com.swiftshelf.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface AudiobookshelfApi {

    @GET("api/libraries")
    suspend fun getLibraries(): Response<LibraryResponse>

    @GET("api/libraries/{id}/items")
    suspend fun getLibraryItems(
        @Path("id") libraryId: String,
        @Query("limit") limit: Int = 50,
        @Query("sort") sort: String = "addedAt",
        @Query("desc") descending: Int = 1
    ): Response<LibraryItemsResponse>

    @GET("api/libraries/{id}/search")
    suspend fun searchLibrary(
        @Path("id") libraryId: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 5
    ): Response<SearchResponse>

    @GET("api/items/{id}")
    suspend fun getItemDetails(
        @Path("id") itemId: String
    ): Response<LibraryItem>

    @GET("api/items/{id}/cover")
    suspend fun getItemCover(
        @Path("id") itemId: String
    ): Response<ByteArray>

    @PATCH("api/me/progress")
    suspend fun updateProgress(
        @Body request: ProgressUpdateRequest
    ): Response<UserMediaProgress>

    @GET("api/me/progress/{id}")
    suspend fun getProgress(
        @Path("id") itemId: String
    ): Response<UserMediaProgress>

    // Playback Session Endpoints (Canonical ABS API)
    @POST("api/items/{id}/play")
    suspend fun startPlaybackSession(
        @Path("id") itemId: String,
        @Body request: SessionStartRequest
    ): Response<PlaybackSessionResponse>

    @POST("api/session/{sessionId}/sync")
    suspend fun syncSession(
        @Path("sessionId") sessionId: String,
        @Body request: SessionSyncRequest
    ): Response<Unit>

    @POST("api/session/{sessionId}/close")
    suspend fun closeSession(
        @Path("sessionId") sessionId: String,
        @Body request: SessionSyncRequest?
    ): Response<Unit>
}
