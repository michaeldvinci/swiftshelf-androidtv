package com.swiftshelf

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swiftshelf.audio.GlobalAudioManager
import com.swiftshelf.data.model.*
import com.swiftshelf.data.network.RetrofitClient
import com.swiftshelf.data.repository.AudiobookRepository
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
            connectToServer(savedHost, savedKey)
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

    fun connectToServer(host: String = _hostUrl.value, key: String = _apiKey.value) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _errorMessage.value = null

            try {
                // Ensure URL has protocol
                val formattedHost = if (!host.startsWith("http")) {
                    "https://$host"
                } else {
                    host
                }

                // Initialize Retrofit
                RetrofitClient.initialize(formattedHost, key)

                // Initialize repository after RetrofitClient
                if (repository == null) {
                    repository = AudiobookRepository()
                }

                // Fetch libraries
                val result = repository!!.getLibraries()
                result.onSuccess { libs ->
                    _libraries.value = libs

                    // Save credentials
                    securePrefs.saveHostUrl(formattedHost)
                    securePrefs.saveApiKey(key)

                    // Initialize audio manager
                    audioManager = GlobalAudioManager(
                        context = getApplication(),
                        repository = repository!!,
                        hostUrl = formattedHost,
                        apiToken = key
                    )

                    if (_selectedLibraryIds.value.isEmpty()) {
                        _uiState.value = UiState.LibrarySelection
                    } else {
                        ensureCurrentLibrary()
                        loadLibraryItems()
                        _uiState.value = UiState.Main
                    }
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
            if (query.isBlank()) return@launch

            val libraryId = _currentLibraryId.value ?: _selectedLibraryIds.value.firstOrNull() ?: return@launch
            repository?.searchLibrary(libraryId, query, limit = 5)?.onSuccess { results ->
                _searchResults.value = results
            }
        }
    }

    fun selectItem(item: LibraryItem) {
        _selectedItem.value = item
    }

    fun dismissItemDetails() {
        _selectedItem.value = null
    }

    fun playItem(item: LibraryItem) {
        audioManager?.loadItem(item)
        dismissItemDetails()
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
    }

    fun logout() {
        audioManager?.release()
        audioManager = null
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
