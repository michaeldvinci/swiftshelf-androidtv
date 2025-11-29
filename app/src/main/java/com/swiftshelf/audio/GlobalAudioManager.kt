package com.swiftshelf.audio

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.swiftshelf.data.model.LibraryItem
import com.swiftshelf.data.repository.AudiobookRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GlobalAudioManager(
    private val context: Context,
    private val repository: AudiobookRepository,
    private val hostUrl: String,
    private val apiToken: String,
    initialPlaybackSpeed: Float = 1.0f
) {

    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressSyncJob: Job? = null
    private var sessionSyncJob: Job? = null

    // Session management (canonical ABS flow)
    private var currentSessionId: String? = null
    private var lastSessionSyncTime = 0L
    private var lastProgressSyncTime = 0L
    private val SESSION_SYNC_INTERVAL_MS = 15000L // 15 seconds (match tvOS)
    private val PROGRESS_SYNC_INTERVAL_MS = 90000L // 90 seconds

    // Multi-file audiobook support (match tvOS pattern)
    private var tracks: List<com.swiftshelf.data.model.Track> = emptyList()
    private var currentTrackStartOffset: Double = 0.0 // Canonical absolute timestamp
    private var pendingResumeSeconds: Double? = null // Resume position to apply on first play

    // State flows
    private val _currentItem = MutableStateFlow<LibraryItem?>(null)
    val currentItem: StateFlow<LibraryItem?> = _currentItem.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTime = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(initialPlaybackSpeed)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow<String?>(null)
    val currentTrackTitle: StateFlow<String?> = _currentTrackTitle.asStateFlow()

    private val _coverArt = MutableStateFlow<Bitmap?>(null)
    val coverArt: StateFlow<Bitmap?> = _coverArt.asStateFlow()

    private val _loadingStatus = MutableStateFlow<LoadingStatus>(LoadingStatus.Idle)
    val loadingStatus: StateFlow<LoadingStatus> = _loadingStatus.asStateFlow()

    init {
        initializePlayer()
        // Apply initial playback speed
        player?.setPlaybackSpeed(initialPlaybackSpeed)
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> {
                            _duration.value = duration
                            _loadingStatus.value = LoadingStatus.Ready
                        }
                        Player.STATE_BUFFERING -> {
                            _loadingStatus.value = LoadingStatus.Buffering
                        }
                        Player.STATE_ENDED -> {
                            handleTrackEnded()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    val wasPlaying = _isPlaying.value
                    _isPlaying.value = isPlayingNow

                    if (isPlayingNow && !wasPlaying) {
                        // Reset sync times to prevent inflated delta on resume
                        lastSessionSyncTime = System.currentTimeMillis()
                        lastProgressSyncTime = System.currentTimeMillis()
                        startProgressTracking()

                        // Start session if needed
                        if (currentSessionId == null) {
                            startPlaybackSession()
                        }
                    } else if (!isPlayingNow && wasPlaying) {
                        stopProgressTracking()
                        // Close session on pause
                        scope.launch {
                            closeCurrentSession()
                        }
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateCurrentTrackInfo()
                }
            })
        }
    }

    fun loadItem(item: LibraryItem, autoPlay: Boolean = true, startTimeSeconds: Double? = null) {
        scope.launch {
            try {
                _loadingStatus.value = LoadingStatus.Loading

                // Stop current playback and close session
                stop()

                // Load full item details with tracks
                val detailsResult = repository.getItemDetails(item.id)
                if (detailsResult.isFailure) {
                    _loadingStatus.value = LoadingStatus.Error("Failed to load item details")
                    return@launch
                }

                val fullItem = detailsResult.getOrNull() ?: return@launch
                _currentItem.value = fullItem

                // Store tracks for multi-file audiobook support
                tracks = fullItem.media?.tracks?.sortedBy { it.index ?: 0 } ?: emptyList()

                // Build media items from tracks
                val mediaItems = buildMediaItems(fullItem)
                if (mediaItems.isEmpty()) {
                    _loadingStatus.value = LoadingStatus.Error("No playable tracks found")
                    return@launch
                }

                // Set media items
                player?.setMediaItems(mediaItems)
                player?.prepare()

                // Determine resume position
                if (startTimeSeconds != null) {
                    // Explicit start time provided (e.g., from chapter selection)
                    pendingResumeSeconds = startTimeSeconds
                } else {
                    // Load progress and set pending resume position (like tvOS)
                    val progressResult = repository.getProgress(fullItem.id)
                    val savedProgress = progressResult.getOrNull()
                    val savedCurrentTime = savedProgress?.currentTime ?: 0.0

                    if (savedCurrentTime > 5.0) {
                        // Set pending resume position (will be applied on play())
                        pendingResumeSeconds = (savedCurrentTime - 5.0).coerceAtLeast(0.0)
                    } else {
                        pendingResumeSeconds = null
                    }
                }

                updateCurrentTrackInfo()
                _loadingStatus.value = LoadingStatus.Ready

                // Auto-play if requested (like tvOS)
                if (autoPlay) {
                    play()
                }

            } catch (e: Exception) {
                _loadingStatus.value = LoadingStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun buildMediaItems(item: LibraryItem): List<MediaItem> {
        val tracks = item.media?.tracks
        return if (!tracks.isNullOrEmpty()) {
            // Use tracks with contentUrl
            tracks.mapNotNull { track ->
                track.contentUrl?.let { url ->
                    val fullUrl = "$hostUrl$url?token=$apiToken"
                    MediaItem.Builder()
                        .setUri(Uri.parse(fullUrl))
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(track.title ?: "Track ${track.index ?: 0}")
                                .build()
                        )
                        .build()
                }
            }
        } else {
            // Fallback to audio files
            val audioFiles = item.media?.audioFiles
            audioFiles?.mapNotNull { audioFile ->
                audioFile.ino?.let { ino ->
                    val url = "$hostUrl/api/items/${item.id}/file/$ino?token=$apiToken"
                    MediaItem.Builder()
                        .setUri(Uri.parse(url))
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(audioFile.fileName ?: "Unknown")
                                .build()
                        )
                        .build()
                }
            } ?: emptyList()
        }
    }

    private fun updateCurrentTrackInfo() {
        player?.let { p ->
            val index = p.currentMediaItemIndex
            _currentTrackIndex.value = index
            _currentTrackTitle.value = p.currentMediaItem?.mediaMetadata?.title?.toString()

            // Update startOffset for multi-file audiobooks (critical for tvOS compatibility)
            if (tracks.isNotEmpty() && index >= 0 && index < tracks.size) {
                currentTrackStartOffset = tracks[index].startOffset ?: 0.0
                android.util.Log.d("GlobalAudioManager", "Track changed to index $index, startOffset: ${currentTrackStartOffset}s")
            } else {
                currentTrackStartOffset = 0.0
            }
        }
    }

    fun play() {
        // Apply pending resume position on first play (like tvOS)
        pendingResumeSeconds?.let { resumeSeconds ->
            seekToAbsoluteTime(resumeSeconds)
            pendingResumeSeconds = null // Clear so it doesn't repeat
        }

        // Apply current playback speed setting
        player?.setPlaybackSpeed(_playbackSpeed.value)
        player?.play()
    }

    fun pause() {
        player?.pause()
        syncProgressNow()
    }

    fun stop() {
        player?.stop()
        _currentItem.value = null
        _isPlaying.value = false
        stopProgressTracking()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    /**
     * Seek to an absolute time position in seconds for multi-file audiobooks.
     * This matches the tvOS implementation for proper chapter/track seeking.
     */
    private fun seekToAbsoluteTime(absoluteSeconds: Double) {
        val p = player ?: return

        if (tracks.isEmpty()) {
            // Single file audiobook - direct seek
            p.seekTo((absoluteSeconds * 1000).toLong())
            return
        }

        // Multi-file audiobook - find which track contains this absolute time
        var targetTrackIndex = 0
        var timeWithinTrack = absoluteSeconds

        for ((index, track) in tracks.withIndex()) {
            val trackStart = track.startOffset ?: 0.0
            val trackDuration = track.duration ?: 0.0
            val trackEnd = trackStart + trackDuration

            if (absoluteSeconds >= trackStart && absoluteSeconds < trackEnd) {
                targetTrackIndex = index
                timeWithinTrack = absoluteSeconds - trackStart
                break
            }
        }

        // Seek to the target track and position
        if (targetTrackIndex != p.currentMediaItemIndex) {
            android.util.Log.d("GlobalAudioManager", "Seeking to track $targetTrackIndex at ${timeWithinTrack}s (absolute: ${absoluteSeconds}s)")
            p.seekTo(targetTrackIndex, (timeWithinTrack * 1000).toLong())
        } else {
            p.seekTo((timeWithinTrack * 1000).toLong())
        }

        updateCurrentTrackInfo()
    }

    fun skipForward(seconds: Int = 15) {
        player?.let {
            val newPosition = (it.currentPosition + seconds * 1000).coerceAtMost(it.duration)
            it.seekTo(newPosition)
        }
    }

    fun skipBackward(seconds: Int = 15) {
        player?.let {
            val newPosition = (it.currentPosition - seconds * 1000).coerceAtLeast(0)
            it.seekTo(newPosition)
        }
    }

    fun nextTrack() {
        player?.seekToNext()
    }

    fun previousTrack() {
        player?.seekToPrevious()
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player?.setPlaybackSpeed(speed)
    }

    private fun startProgressTracking() {
        progressSyncJob?.cancel()
        sessionSyncJob?.cancel()

        // Main loop: Update UI state with proper absolute time calculation
        progressSyncJob = scope.launch {
            while (isActive) {
                player?.let { p ->
                    // For multi-file audiobooks, calculate absolute time using startOffset
                    val trackPositionMs = p.currentPosition
                    val absoluteTimeMs = if (tracks.isNotEmpty()) {
                        // Add the current track's startOffset to get absolute position
                        ((currentTrackStartOffset * 1000) + trackPositionMs).toLong()
                    } else {
                        // Single file - use position directly
                        trackPositionMs
                    }

                    _currentTime.value = absoluteTimeMs

                    // Duration should be total duration from item metadata for multi-file
                    val totalDuration = _currentItem.value?.media?.duration
                    _duration.value = if (totalDuration != null && totalDuration > 0) {
                        (totalDuration * 1000).toLong()
                    } else {
                        p.duration
                    }
                }
                delay(100)
            }
        }

        // Session sync timer: 15 seconds (match tvOS)
        sessionSyncJob = scope.launch {
            while (isActive) {
                delay(SESSION_SYNC_INTERVAL_MS)
                syncSessionNow()
            }
        }

        // Progress sync timer: 90 seconds
        scope.launch {
            while (isActive) {
                delay(PROGRESS_SYNC_INTERVAL_MS)
                syncProgressNow()
            }
        }
    }

    private fun stopProgressTracking() {
        progressSyncJob?.cancel()
        progressSyncJob = null
        sessionSyncJob?.cancel()
        sessionSyncJob = null
    }

    private fun syncProgressNow() {
        val item = _currentItem.value ?: return
        val currentTimeSeconds = (_currentTime.value / 1000.0)
        var durationSeconds = (_duration.value / 1000.0)

        // Duration fallback: use item duration if player duration is invalid
        if (durationSeconds <= 0.0) {
            durationSeconds = item.media?.duration ?: 0.0
        }

        if (durationSeconds <= 0.0) {
            android.util.Log.w("GlobalAudioManager", "Cannot sync progress: duration is 0")
            return
        }

        lastProgressSyncTime = System.currentTimeMillis()

        scope.launch {
            repository.updateProgress(
                libraryItemId = item.id,
                duration = durationSeconds,
                currentTime = currentTimeSeconds,
                isFinished = false
            )
        }
    }

    private fun handleTrackEnded() {
        if (player?.hasNextMediaItem() == true) {
            player?.seekToNext()
        } else {
            // Mark as finished
            val item = _currentItem.value ?: return
            val durationSeconds = (_duration.value / 1000.0)
            scope.launch {
                repository.updateProgress(
                    libraryItemId = item.id,
                    duration = durationSeconds,
                    currentTime = durationSeconds,
                    isFinished = true
                )
            }
        }
    }

    // Session Management (Canonical ABS API)
    private fun startPlaybackSession() {
        val item = _currentItem.value ?: return

        android.util.Log.d("GlobalAudioManager", "🚀 Starting playback session for: ${item.media?.metadata?.title}")

        scope.launch {
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )

            val result = repository.startPlaybackSession(
                itemId = item.id,
                deviceId = deviceId,
                model = android.os.Build.MODEL,
                deviceName = android.os.Build.DEVICE
            )

            result.onSuccess { response ->
                currentSessionId = response.id
                lastSessionSyncTime = System.currentTimeMillis()
                android.util.Log.d("GlobalAudioManager", "✅ Session started: ${response.id}")
            }.onFailure { error ->
                android.util.Log.e("GlobalAudioManager", "❌ Failed to start session: ${error.message}")
            }
        }
    }

    private fun syncSessionNow() {
        val sessionId = currentSessionId ?: return
        val item = _currentItem.value ?: return

        val currentTimeSeconds = (_currentTime.value / 1000.0)
        var durationSeconds = (_duration.value / 1000.0)

        // Duration fallback
        if (durationSeconds <= 0.0) {
            durationSeconds = item.media?.duration ?: 0.0
        }

        if (durationSeconds <= 0.0) {
            android.util.Log.w("GlobalAudioManager", "Cannot sync session: duration is 0")
            return
        }

        // Calculate delta time listened since last sync
        val now = System.currentTimeMillis()
        val deltaSeconds = ((now - lastSessionSyncTime) / 1000.0).coerceAtLeast(0.0)
        lastSessionSyncTime = now

        scope.launch {
            repository.syncSession(
                sessionId = sessionId,
                currentTime = currentTimeSeconds,
                timeListened = deltaSeconds,
                duration = durationSeconds
            ).onFailure { error ->
                android.util.Log.e("GlobalAudioManager", "❌ Session sync failed: ${error.message}")
            }
        }
    }

    private suspend fun closeCurrentSession() {
        val sessionId = currentSessionId ?: return
        val item = _currentItem.value ?: return

        android.util.Log.d("GlobalAudioManager", "🛑 Closing session: $sessionId")

        val currentTimeSeconds = (_currentTime.value / 1000.0)
        var durationSeconds = (_duration.value / 1000.0)

        if (durationSeconds <= 0.0) {
            durationSeconds = item.media?.duration ?: 0.0
        }

        // Calculate final delta
        val now = System.currentTimeMillis()
        val deltaSeconds = ((now - lastSessionSyncTime) / 1000.0).coerceAtLeast(0.0)

        repository.closeSession(
            sessionId = sessionId,
            currentTime = if (durationSeconds > 0) currentTimeSeconds else null,
            timeListened = if (durationSeconds > 0) deltaSeconds else null,
            duration = if (durationSeconds > 0) durationSeconds else null
        ).onSuccess {
            android.util.Log.d("GlobalAudioManager", "✅ Session closed")
            currentSessionId = null
        }.onFailure { error ->
            android.util.Log.e("GlobalAudioManager", "❌ Failed to close session: ${error.message}")
            currentSessionId = null
        }
    }

    fun release() {
        scope.launch {
            closeCurrentSession()
        }
        syncProgressNow()
        stopProgressTracking()
        player?.release()
        player = null
        scope.cancel()
    }

    sealed class LoadingStatus {
        object Idle : LoadingStatus()
        object Loading : LoadingStatus()
        object Buffering : LoadingStatus()
        object Ready : LoadingStatus()
        data class Error(val message: String) : LoadingStatus()
    }
}
