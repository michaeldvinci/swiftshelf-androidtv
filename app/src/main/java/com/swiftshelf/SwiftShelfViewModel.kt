package com.swiftshelf

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swiftshelf.audio.GlobalAudioManager
import com.swiftshelf.data.model.*
import com.swiftshelf.data.network.RetrofitClient
import com.swiftshelf.data.repository.AudiobookRepository
import com.swiftshelf.ui.screens.AuthType
import com.swiftshelf.util.SecurePreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SwiftShelfViewModel(application: Application) : AndroidViewModel(application) {

    private val securePrefs = SecurePreferences(application)
    private var repository: AudiobookRepository? = null

    private var audioManager: GlobalAudioManager? = null

    // UI State
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Login state
    private val _hostUrl = MutableStateFlow("")
    val hostUrl: StateFlow<String> = _hostUrl.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _authType = MutableStateFlow(AuthType.API_KEY)
    val authType: StateFlow<AuthType> = _authType.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Libraries
    private val _libraries = MutableStateFlow<List<LibrarySummary>>(emptyList())
    val libraries: StateFlow<List<LibrarySummary>> = _libraries.asStateFlow()

    private val _selectedLibraryIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedLibraryIds: StateFlow<Set<String>> = _selectedLibraryIds.asStateFlow()

    // Current library items
    private val _currentLibraryId = MutableStateFlow<String?>(null)
    val currentLibraryId: StateFlow<String?> = _currentLibraryId.asStateFlow()

    private val _recentItemsByLibrary = MutableStateFlow<Map<String, List<LibraryItem>>>(emptyMap())
    private val _continueListeningItemsByLibrary = MutableStateFlow<Map<String, List<LibraryItem>>>(emptyMap())

    val recentItems: StateFlow<List<LibraryItem>> = combine(
        _currentLibraryId,
        _recentItemsByLibrary
    ) { currentId, itemsMap ->
        currentId?.let { itemsMap[it] } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val continueListeningItems: StateFlow<List<LibraryItem>> = combine(
        _currentLibraryId,
        _continueListeningItemsByLibrary
    ) { currentId, itemsMap ->
        currentId?.let { itemsMap[it] } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<SearchResponse?>(null)
    val searchResults: StateFlow<SearchResponse?> = _searchResults.asStateFlow()

    // Selected item for details dialog
    private val _selectedItem = MutableStateFlow<LibraryItem?>(null)
    val selectedItem: StateFlow<LibraryItem?> = _selectedItem.asStateFlow()

    // Selected series for series dialog
    private val _selectedSeries = MutableStateFlow<SeriesWithBooks?>(null)
    val selectedSeries: StateFlow<SeriesWithBooks?> = _selectedSeries.asStateFlow()

    private val _isLoadingSeries = MutableStateFlow(false)
    val isLoadingSeries: StateFlow<Boolean> = _isLoadingSeries.asStateFlow()

    // Settings
    private val _itemLimit = MutableStateFlow(10)
    val itemLimit: StateFlow<Int> = _itemLimit.asStateFlow()

    private val _progressBarColor = MutableStateFlow("Yellow")
    val progressBarColor: StateFlow<String> = _progressBarColor.asStateFlow()

    private val _preferredPlaybackSpeed = MutableStateFlow(1.0f)
    val preferredPlaybackSpeed: StateFlow<Float> = _preferredPlaybackSpeed.asStateFlow()

    // Current tab (default to Library = 1)
    private val _currentTab = MutableStateFlow(1)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Audio player state (exposed from GlobalAudioManager)
    val currentItem: StateFlow<LibraryItem?>
        get() = audioManager?.currentItem ?: MutableStateFlow(null).asStateFlow()
    val isPlaying: StateFlow<Boolean>
        get() = audioManager?.isPlaying ?: MutableStateFlow(false).asStateFlow()
    val currentTime: StateFlow<Long>
        get() = audioManager?.currentTime ?: MutableStateFlow(0L).asStateFlow()
    val duration: StateFlow<Long>
        get() = audioManager?.duration ?: MutableStateFlow(0L).asStateFlow()
    val playbackSpeed: StateFlow<Float>
        get() = audioManager?.playbackSpeed ?: MutableStateFlow(1.0f).asStateFlow()
    val currentTrackIndex: StateFlow<Int>
        get() = audioManager?.currentTrackIndex ?: MutableStateFlow(0).asStateFlow()
    val currentTrackTitle: StateFlow<String?>
        get() = audioManager?.currentTrackTitle ?: MutableStateFlow(null).asStateFlow()

    private var persistedCurrentLibraryId: String? = securePrefs.getCurrentLibraryId()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        val savedHost = securePrefs.getHostUrl()
        val savedKey = securePrefs.getApiKey()

        if (!savedHost.isNullOrEmpty() && !savedKey.isNullOrEmpty()) {
            _hostUrl.value = savedHost
            _apiKey.value = savedKey
            // Use saved API key directly (it could be an API key or a token from username/password login)
            connectWithApiKey(savedHost, savedKey)
        } else {
            _uiState.value = UiState.Login
        }

        // Load settings
        _itemLimit.value = securePrefs.getItemLimit()
        _progressBarColor.value = securePrefs.getProgressBarColor()
        _preferredPlaybackSpeed.value = securePrefs.getPreferredPlaybackSpeed()
        _selectedLibraryIds.value = securePrefs.getSelectedLibraries()
        persistedCurrentLibraryId?.let { savedId ->
            if (_selectedLibraryIds.value.contains(savedId)) {
                setCurrentLibraryInternal(savedId, persist = false)
            }
        }
        ensureCurrentLibrary()
    }

    fun updateHostUrl(url: String) {
        _hostUrl.value = url
    }

    fun updateApiKey(key: String) {
        _apiKey.value = key
    }

    fun updateUsername(value: String) {
        _username.value = value
    }

    fun updatePassword(value: String) {
        _password.value = value
    }

    fun updateAuthType(type: AuthType) {
        _authType.value = type
    }

    fun connectToServer() {
        when (_authType.value) {
            AuthType.API_KEY -> connectWithApiKey(_hostUrl.value, _apiKey.value)
            AuthType.USERNAME_PASSWORD -> connectWithUsernamePassword(_hostUrl.value, _username.value, _password.value)
        }
    }

    private fun connectWithApiKey(host: String, key: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _errorMessage.value = null

            try {
                // Ensure URL has protocol
                val formattedHost = formatHost(host)

                // Initialize Retrofit
                RetrofitClient.initialize(formattedHost, key)

                // Initialize repository after RetrofitClient
                repository = AudiobookRepository()

                // Fetch libraries to verify connection
                val result = repository!!.getLibraries()
                result.onSuccess { libs ->
                    onConnectionSuccess(libs, formattedHost, key)
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Connection failed"
                    _uiState.value = UiState.Login
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
                _uiState.value = UiState.Login
            }
        }
    }

    private fun connectWithUsernamePassword(host: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _errorMessage.value = null

            try {
                // Ensure URL has protocol
                val formattedHost = formatHost(host)

                // Create unauthenticated API for login
                val unauthApi = RetrofitClient.createUnauthenticatedApi(formattedHost)

                // Attempt login
                val loginRequest = LoginRequest(username = username, password = password)
                val loginResponse = unauthApi.login(loginRequest)

                if (loginResponse.isSuccessful && loginResponse.body() != null) {
                    val token = loginResponse.body()!!.user.token

                    // Now initialize with the token
                    RetrofitClient.initialize(formattedHost, token)
                    repository = AudiobookRepository()

                    // Fetch libraries to verify
                    val result = repository!!.getLibraries()
                    result.onSuccess { libs ->
                        onConnectionSuccess(libs, formattedHost, token)
                    }.onFailure { error ->
                        _errorMessage.value = error.message ?: "Failed to fetch libraries"
                        _uiState.value = UiState.Login
                    }
                } else {
                    val errorBody = loginResponse.errorBody()?.string()
                    _errorMessage.value = when (loginResponse.code()) {
                        401 -> "Invalid username or password"
                        else -> "Login failed: ${loginResponse.code()}"
                    }
                    _uiState.value = UiState.Login
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
                _uiState.value = UiState.Login
            }
        }
    }

    private fun formatHost(host: String): String {
        return if (!host.startsWith("http")) {
            "https://$host"
        } else {
            host
        }
    }

    private fun onConnectionSuccess(libs: List<LibrarySummary>, formattedHost: String, token: String) {
        _libraries.value = libs

        // Save credentials
        securePrefs.saveHostUrl(formattedHost)
        securePrefs.saveApiKey(token)

        // Initialize audio manager with preferred playback speed
        audioManager = GlobalAudioManager(
            context = getApplication(),
            repository = repository!!,
            hostUrl = formattedHost,
            apiToken = token,
            initialPlaybackSpeed = _preferredPlaybackSpeed.value
        )

        if (_selectedLibraryIds.value.isEmpty()) {
            _uiState.value = UiState.LibrarySelection
        } else {
            ensureCurrentLibrary()
            loadLibraryItems()
            _uiState.value = UiState.Main
        }
    }

    fun toggleLibrarySelection(libraryId: String) {
        val current = _selectedLibraryIds.value.toMutableSet()
        val wasSelected = current.contains(libraryId)
        if (wasSelected) {
            current.remove(libraryId)
        } else {
            current.add(libraryId)
        }
        _selectedLibraryIds.value = current
        securePrefs.saveSelectedLibraries(current)

        if (wasSelected) {
            _recentItemsByLibrary.update { it - libraryId }
            _continueListeningItemsByLibrary.update { it - libraryId }
            if (persistedCurrentLibraryId == libraryId) {
                persistedCurrentLibraryId = null
                securePrefs.saveCurrentLibraryId(null)
            }
        }

        if (current.isEmpty()) {
            setCurrentLibraryInternal(null, persist = true)
            _recentItemsByLibrary.value = emptyMap()
            _continueListeningItemsByLibrary.value = emptyMap()
        } else {
            ensureCurrentLibrary()
            if (!wasSelected) {
                loadLibraryItems(libraryId)
            }
        }
    }

    fun confirmLibrarySelection() {
        if (_selectedLibraryIds.value.isNotEmpty()) {
            ensureCurrentLibrary()
            loadLibraryItems()
            _uiState.value = UiState.Main
        }
    }

    private fun loadLibraryItems(targetLibraryId: String? = null) {
        viewModelScope.launch {
            val selectedIds = _selectedLibraryIds.value
            if (selectedIds.isEmpty()) {
                _recentItemsByLibrary.value = emptyMap()
                _continueListeningItemsByLibrary.value = emptyMap()
                return@launch
            }

            if (targetLibraryId != null && !selectedIds.contains(targetLibraryId)) {
                return@launch
            }

            val librariesToLoad: Collection<String> = targetLibraryId?.let { listOf(it) } ?: selectedIds

            librariesToLoad.forEach { libraryId ->
                repository?.getLibraryItems(
                    libraryId = libraryId,
                    limit = _itemLimit.value,
                    sort = "addedAt",
                    descending = true
                )?.onSuccess { items ->
                    _recentItemsByLibrary.update { current ->
                        current + (libraryId to items.sortedByDescending { it.addedAt })
                    }
                }

                repository?.getContinueListening(
                    libraryId = libraryId,
                    limit = _itemLimit.value
                )?.onSuccess { items ->
                    _continueListeningItemsByLibrary.update { current ->
                        current + (libraryId to items.sortedByDescending { it.updatedAt })
                    }
                }
            }
        }
    }

    fun refreshLibraryItems() {
        loadLibraryItems()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun performSearch() {
        viewModelScope.launch {
            val query = _searchQuery.value
            Log.d("SwiftShelf", "performSearch called with query: '$query'")
            if (query.isBlank()) {
                Log.d("SwiftShelf", "Query is blank, returning")
                return@launch
            }

            if (repository == null) {
                Log.e("SwiftShelf", "Repository is null!")
                return@launch
            }

            val libraryId = _currentLibraryId.value ?: _selectedLibraryIds.value.firstOrNull()
            Log.d("SwiftShelf", "currentLibraryId=${_currentLibraryId.value}, selectedLibraryIds=${_selectedLibraryIds.value}")
            if (libraryId == null) {
                Log.e("SwiftShelf", "No library ID available")
                return@launch
            }

            Log.d("SwiftShelf", "Searching library $libraryId for: $query")
            repository?.searchLibrary(libraryId, query, limit = 10)?.onSuccess { results ->
                Log.d("SwiftShelf", "Search results: books=${results.book?.size}, series=${results.series?.size}, narrators=${results.narrators?.size}")
                _searchResults.value = results
            }?.onFailure { error ->
                Log.e("SwiftShelf", "Search failed: ${error.message}")
            }
        }
    }

    fun selectItem(item: LibraryItem) {
        // Fetch full item details (including chapters) before showing dialog
        viewModelScope.launch {
            _selectedItem.value = item // Show dialog immediately with basic info

            // Fetch full details with chapters in background
            repository?.getItemDetails(item.id)?.onSuccess { fullItem ->
                _selectedItem.value = fullItem
            }
        }
    }

    fun selectItemById(itemId: String) {
        // Fetch item details by ID and show dialog
        viewModelScope.launch {
            repository?.getItemDetails(itemId)?.onSuccess { item ->
                _selectedItem.value = item
            }?.onFailure { error ->
                Log.e("SwiftShelf", "Failed to load item: ${error.message}")
                _errorMessage.value = "Failed to load item: ${error.message}"
            }
        }
    }

    fun dismissItemDetails() {
        _selectedItem.value = null
    }

    fun selectSeries(seriesId: String) {
        viewModelScope.launch {
            _isLoadingSeries.value = true
            Log.d("SwiftShelf", "Fetching series details for: $seriesId")

            val libraryId = _currentLibraryId.value ?: _selectedLibraryIds.value.firstOrNull()
            if (libraryId == null) {
                Log.e("SwiftShelf", "No library selected")
                _errorMessage.value = "No library selected"
                _isLoadingSeries.value = false
                return@launch
            }

            repository?.getSeriesWithBooks(libraryId, seriesId)?.onSuccess { series ->
                Log.d("SwiftShelf", "Series: name=${series.name}, books count=${series.books?.size}")
                _selectedSeries.value = series
            }?.onFailure { error ->
                Log.e("SwiftShelf", "Failed to load series: ${error.message}")
                _errorMessage.value = "Failed to load series: ${error.message}"
            }

            _isLoadingSeries.value = false
        }
    }

    fun dismissSeriesDetails() {
        _selectedSeries.value = null
    }

    fun playItem(item: LibraryItem) {
        audioManager?.loadItem(item)
    }

    fun playItemFromTime(item: LibraryItem, startTimeSeconds: Double) {
        audioManager?.loadItem(item, startTimeSeconds = startTimeSeconds)
    }

    fun setCurrentTab(index: Int) {
        _currentTab.value = index
    }

    // Audio controls
    fun playPause() {
        audioManager?.let {
            if (it.isPlaying.value) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun skipForward() {
        audioManager?.skipForward()
    }

    fun skipBackward() {
        audioManager?.skipBackward()
    }

    fun nextTrack() {
        audioManager?.nextTrack()
    }

    fun previousTrack() {
        audioManager?.previousTrack()
    }

    fun seekTo(position: Long) {
        audioManager?.seekTo(position)
    }

    fun setPlaybackSpeed(speed: Float) {
        audioManager?.setPlaybackSpeed(speed)
    }

    fun increaseSpeed() {
        updatePreferredPlaybackSpeed((_preferredPlaybackSpeed.value + 0.1f).coerceAtMost(3.0f))
    }

    fun decreaseSpeed() {
        updatePreferredPlaybackSpeed((_preferredPlaybackSpeed.value - 0.1f).coerceAtLeast(0.5f))
    }

    fun setCurrentLibrary(libraryId: String) {
        if (!_selectedLibraryIds.value.contains(libraryId)) return
        if (_currentLibraryId.value == libraryId) return
        setCurrentLibraryInternal(libraryId, persist = true)
        val hasRecent = _recentItemsByLibrary.value.containsKey(libraryId)
        val hasContinue = _continueListeningItemsByLibrary.value.containsKey(libraryId)
        if (!hasRecent || !hasContinue) {
            loadLibraryItems(libraryId)
        }
    }

    // Settings
    fun updateItemLimit(limit: Int) {
        _itemLimit.value = limit
        securePrefs.saveItemLimit(limit)
        refreshLibraryItems()
    }

    fun updateProgressBarColor(color: String) {
        _progressBarColor.value = color
        securePrefs.saveProgressBarColor(color)
    }

    fun updatePreferredPlaybackSpeed(speed: Float) {
        _preferredPlaybackSpeed.value = speed
        securePrefs.savePreferredPlaybackSpeed(speed)
        // Also update the audio manager so it takes effect on next play
        audioManager?.setPlaybackSpeed(speed)
    }

    fun logout() {
        audioManager?.release()
        audioManager = null
        repository = null  // Clear repository so it gets recreated with new token on next login
        securePrefs.clear()
        _uiState.value = UiState.Login
        _selectedLibraryIds.value = emptySet()
        _libraries.value = emptyList()
        _recentItemsByLibrary.value = emptyMap()
        _continueListeningItemsByLibrary.value = emptyMap()
        setCurrentLibraryInternal(null, persist = true)
    }

    fun getAudioManager() = audioManager

    override fun onCleared() {
        super.onCleared()
        audioManager?.release()
    }

    private fun ensureCurrentLibrary() {
        val selected = _selectedLibraryIds.value
        if (selected.isEmpty()) {
            setCurrentLibraryInternal(null, persist = true)
            return
        }

        val current = _currentLibraryId.value
        if (current != null && selected.contains(current)) {
            return
        }

        persistedCurrentLibraryId?.let { savedId ->
            if (selected.contains(savedId)) {
                setCurrentLibraryInternal(savedId, persist = false)
                if (!_recentItemsByLibrary.value.containsKey(savedId) ||
                    !_continueListeningItemsByLibrary.value.containsKey(savedId)
                ) {
                    loadLibraryItems(savedId)
                }
                return
            }
        }

        val availableLibraries = _libraries.value
        if (availableLibraries.isEmpty()) {
            return
        }

        val firstFromList = availableLibraries.firstOrNull { selected.contains(it.id) }
        val fallbackId = firstFromList?.id ?: selected.first()
        setCurrentLibraryInternal(fallbackId, persist = true)
        if (!_recentItemsByLibrary.value.containsKey(fallbackId) ||
            !_continueListeningItemsByLibrary.value.containsKey(fallbackId)
        ) {
            loadLibraryItems(fallbackId)
        }
    }

    private fun setCurrentLibraryInternal(libraryId: String?, persist: Boolean) {
        _currentLibraryId.value = libraryId
        if (persist) {
            persistedCurrentLibraryId = libraryId
            securePrefs.saveCurrentLibraryId(libraryId)
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object Login : UiState()
        object LibrarySelection : UiState()
        object Main : UiState()
    }
}
