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

// Playback Session Models (Canonical ABS API)
data class PlaybackSessionResponse(
    val id: String,                         // Session ID
    val audioTracks: List<PlaybackTrack>,
    val duration: Double?
)

data class PlaybackTrack(
    val contentUrl: String,
    val mimeType: String,
    val duration: Double?
)

data class SessionStartRequest(
    val deviceInfo: DeviceInfo,
    val supportedMimeTypes: List<String>
)

data class DeviceInfo(
    val deviceId: String,
    val clientName: String = "SwiftShelf",
    val clientVersion: String = "1.0.0",
    val platform: String = "Android TV",
    val model: String,
    val deviceName: String
)

data class SessionSyncRequest(
    val currentTime: Double,
    val timeListened: Double,               // Delta since last sync
    val duration: Double
)
