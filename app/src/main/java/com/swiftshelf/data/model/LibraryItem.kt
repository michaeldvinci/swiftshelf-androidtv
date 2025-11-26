package com.swiftshelf.data.model

import com.google.gson.annotations.SerializedName

data class LibraryItem(
    val id: String,
    val media: Media?,
    @SerializedName("userMediaProgress", alternate = ["mediaProgress"])
    val userMediaProgress: UserMediaProgress?,
    val libraryFiles: List<LibraryFile>?,
    val addedAt: Double?,
    val updatedAt: Double?
) {
    /** Get the first ebook file if present */
    val ebookFile: LibraryFile?
        get() = libraryFiles?.firstOrNull { it.fileType == "ebook" }

    /** Check if this item has an ebook */
    val hasEbook: Boolean
        get() = ebookFile != null
}

data class LibraryFile(
    val ino: String?,
    val fileType: String?,
    val metadata: LibraryFileMetadata?,
    val addedAt: Double?,
    val updatedAt: Double?
)

data class LibraryFileMetadata(
    val filename: String?,
    val ext: String?,
    val path: String?,
    val relPath: String?,
    val size: Long?
)

data class Media(
    val duration: Double?,
    val coverPath: String?,
    val metadata: Metadata?,
    val audioFiles: List<AudioFile>?,
    val chapters: List<Chapter>?,
    val tracks: List<Track>?
)

data class Metadata(
    val title: String?,
    val subtitle: String?,
    val authors: List<Author>?,
    val narrators: List<String>?,
    val series: List<SeriesInfo>?,
    val publishedYear: String?,
    val description: String?
)

data class Author(
    val id: String?,
    val name: String?
)

data class SeriesInfo(
    val id: String?,
    val name: String?,
    val sequence: String?
)

data class AudioFile(
    val index: Int?,
    val ino: String?,
    @SerializedName("filename")
    val fileName: String?,
    val duration: Double?,
    val metadata: AudioFileMetadata?
)

data class AudioFileMetadata(
    val filename: String?,
    val ext: String?,
    val path: String?,
    val relPath: String?,
    val size: Long?
)

data class Chapter(
    val id: Int?,
    val start: Double?,
    val end: Double?,
    val title: String?
)

data class Track(
    val index: Int?,
    val startOffset: Double?,
    val duration: Double?,
    val title: String?,
    val contentUrl: String?,
    val mimeType: String?
)

data class UserMediaProgress(
    val id: String?,
    val libraryItemId: String?,
    val duration: Double?,
    val progress: Double?,
    val currentTime: Double?,
    val isFinished: Boolean?,
    val hideFromContinueListening: Boolean?,
    val lastUpdate: Long?,
    val startedAt: Long?,
    val finishedAt: Long?
)

fun UserMediaProgress.progressFraction(): Double {
    val stored = progress
    if (stored != null && stored > 0.0) {
        return stored.coerceIn(0.0, 1.0)
    }

    val current = currentTime
    val total = duration
    if (current != null && total != null && total > 0) {
        val computed = (current / total).coerceIn(0.0, 1.0)
        if (computed > 0.0) {
            return computed
        }
    }

    return if (isFinished == true) 1.0 else 0.0
}
