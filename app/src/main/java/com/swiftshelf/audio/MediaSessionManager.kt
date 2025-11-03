package com.swiftshelf.audio

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media3.session.MediaSession
import com.swiftshelf.data.model.LibraryItem

class MediaSessionManager(context: Context) {

    private val mediaSession: MediaSessionCompat = MediaSessionCompat(context, "SwiftShelfSession")

    init {
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        mediaSession.isActive = true
    }

    fun updateMetadata(item: LibraryItem?, coverArtBitmap: android.graphics.Bitmap?) {
        if (item == null) return

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.media?.metadata?.title)
            .putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                item.media?.metadata?.authors?.firstOrNull()?.name
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM,
                item.media?.metadata?.series?.firstOrNull()?.name
            )
            .putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                (item.media?.duration?.toLong() ?: 0L) * 1000
            )
            .apply {
                if (coverArtBitmap != null) {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, coverArtBitmap)
                }
            }
            .build()

        mediaSession.setMetadata(metadata)
    }

    fun updatePlaybackState(
        isPlaying: Boolean,
        position: Long,
        playbackSpeed: Float
    ) {
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, position, playbackSpeed)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_REWIND or
                PlaybackStateCompat.ACTION_SEEK_TO
            )
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    fun setCallback(callback: MediaSessionCompat.Callback) {
        mediaSession.setCallback(callback)
    }

    fun release() {
        mediaSession.isActive = false
        mediaSession.release()
    }
}
