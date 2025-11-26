package com.swiftshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.tv.material3.DrawerValue as TvDrawerValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.rememberDrawerState as rememberTvDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.swiftshelf.ui.screens.*
import com.swiftshelf.ui.theme.SwiftShelfTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SwiftShelfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwiftShelfTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SwiftShelfApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun SwiftShelfApp(viewModel: SwiftShelfViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val audioManager = viewModel.getAudioManager()

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is SwiftShelfViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is SwiftShelfViewModel.UiState.Login -> {
                val hostUrl by viewModel.hostUrl.collectAsState()
                val apiKey by viewModel.apiKey.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()

                LoginScreen(
                    hostUrl = hostUrl,
                    apiKey = apiKey,
                    isLoading = false,
                    errorMessage = errorMessage,
                    onHostUrlChange = viewModel::updateHostUrl,
                    onApiKeyChange = viewModel::updateApiKey,
                    onConnectClick = { viewModel.connectToServer() }
                )
            }

            is SwiftShelfViewModel.UiState.LibrarySelection -> {
                val libraries by viewModel.libraries.collectAsState()
                val selectedLibraryIds by viewModel.selectedLibraryIds.collectAsState()

                LibrarySelectionScreen(
                    libraries = libraries,
                    selectedLibraryIds = selectedLibraryIds,
                    onLibraryToggle = viewModel::toggleLibrarySelection,
                    onContinue = viewModel::confirmLibrarySelection
                )
            }

            is SwiftShelfViewModel.UiState.Main -> {
                MainAppContent(viewModel, audioManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTvMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: SwiftShelfViewModel,
    audioManager: com.swiftshelf.audio.GlobalAudioManager?
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val selectedSeries by viewModel.selectedSeries.collectAsState()
    val hostUrl by viewModel.hostUrl.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val libraries by viewModel.libraries.collectAsState()
    val selectedLibraryIds by viewModel.selectedLibraryIds.collectAsState()
    val currentLibraryId by viewModel.currentLibraryId.collectAsState()
    val currentLibrary = libraries.firstOrNull { it.id == currentLibraryId }

    // Audio state
    val currentItem by (audioManager?.currentItem ?: MutableStateFlow(null)).collectAsState()

    val drawerState = rememberTvDrawerState(initialValue = TvDrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val firstCarouselFocusRequester = remember { FocusRequester() }

    // Player visibility state
    var showPlayer by remember { mutableStateOf(false) }
    var playerItem by remember { mutableStateOf<com.swiftshelf.data.model.LibraryItem?>(null) }

    // EPUB reader state
    var showEpubReader by remember { mutableStateOf(false) }
    var ebookItem by remember { mutableStateOf<com.swiftshelf.data.model.LibraryItem?>(null) }
    var ebookFile by remember { mutableStateOf<com.swiftshelf.data.model.LibraryFile?>(null) }

    // Update playerItem when audioManager's currentItem changes
    LaunchedEffect(currentItem) {
        if (currentItem != null) {
            playerItem = currentItem
        }
    }

    val openDrawer: () -> Unit = {
        scope.launch {
            drawerState.setValue(TvDrawerValue.Open)
        }
    }
    val closeDrawer: () -> Unit = {
        scope.launch {
            drawerState.setValue(TvDrawerValue.Closed)
            firstCarouselFocusRequester.requestFocus()
        }
    }

    // Auto-focus first carousel item when library tab loads
    val recentItems by viewModel.recentItems.collectAsState()
    val continueListeningItems by viewModel.continueListeningItems.collectAsState()

    LaunchedEffect(currentTab, recentItems, continueListeningItems) {
        // Only auto-focus on library tab (1) and when drawer is closed
        if (currentTab == 1 && drawerState.currentValue == TvDrawerValue.Closed) {
            val hasItems = recentItems.isNotEmpty() || continueListeningItems.isNotEmpty()
            if (hasItems) {
                kotlinx.coroutines.delay(100) // Small delay to ensure UI is rendered
                firstCarouselFocusRequester.requestFocus()
            }
        }
    }

    // Top-level Box to layer NavigationDrawer with fullscreen overlays
    Box(modifier = Modifier.fillMaxSize()) {
        NavigationDrawer(
            drawerState = drawerState,
            drawerContent = { drawerValue ->
                TvDrawerContent(
                    drawerValue = drawerValue,
                    libraries = libraries,
                    selectedLibraryIds = selectedLibraryIds,
                    currentLibraryId = currentLibraryId,
                    currentTab = currentTab,
                    currentItem = currentItem,
                    onLibraryClick = { libraryId ->
                        viewModel.setCurrentLibrary(libraryId)
                        viewModel.setCurrentTab(1)
                        closeDrawer()
                    },
                    onSearchClick = {
                        viewModel.setCurrentTab(0)
                        closeDrawer()
                    },
                    onUserClick = {
                        // Reserved for future user profile
                    },
                    onNowPlayingClick = {
                        // Set playerItem from currentItem when opening via Now Playing icon
                        if (currentItem != null) {
                            playerItem = currentItem
                        }
                        showPlayer = true
                        closeDrawer()
                    },
                    onSettingsClick = {
                        viewModel.setCurrentTab(3)
                        closeDrawer()
                    }
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            Scaffold { paddingValues ->
                Box(
                    modifier = Modifier.padding(paddingValues)
                ) {
                    when (currentTab) {
                        0 -> {
                            val searchQuery by viewModel.searchQuery.collectAsState()
                            val searchResults by viewModel.searchResults.collectAsState()

                            SearchScreen(
                                searchQuery = searchQuery,
                                searchResults = searchResults,
                                hostUrl = hostUrl,
                                apiToken = apiKey,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSearch = viewModel::performSearch,
                                onItemClick = viewModel::selectItem,
                                onSeriesClick = viewModel::selectSeries
                            )
                        }

                        1 -> {
                            val progressBarColor by viewModel.progressBarColor.collectAsState()

                            LibraryBrowseScreen(
                                libraryName = currentLibrary?.name,
                                libraryMediaType = currentLibrary?.mediaType,
                                firstItemFocusRequester = firstCarouselFocusRequester,
                                recentItems = recentItems,
                                continueListeningItems = continueListeningItems,
                                hostUrl = hostUrl,
                                apiToken = apiKey,
                                progressBarColor = progressBarColor,
                                onItemClick = viewModel::selectItem,
                                onRequestOpenDrawer = openDrawer
                            )
                        }

                        3 -> {
                            val itemLimit by viewModel.itemLimit.collectAsState()
                            val progressBarColor by viewModel.progressBarColor.collectAsState()
                            val preferredPlaybackSpeed by viewModel.preferredPlaybackSpeed.collectAsState()

                            SettingsScreen(
                                itemLimit = itemLimit,
                                progressBarColor = progressBarColor,
                                preferredPlaybackSpeed = preferredPlaybackSpeed,
                                onItemLimitChange = viewModel::updateItemLimit,
                                onProgressBarColorChange = viewModel::updateProgressBarColor,
                                onPlaybackSpeedChange = viewModel::updatePreferredPlaybackSpeed,
                                onLogout = viewModel::logout
                            )
                        }
                    }
                }
            }

            selectedItem?.let { item ->
                BookDetailsDialog(
                    item = item,
                    hostUrl = hostUrl,
                    apiToken = apiKey,
                    onDismiss = viewModel::dismissItemDetails,
                    onPlayClick = {
                        playerItem = item // Set immediately so player has item to display
                        viewModel.playItem(item)
                        viewModel.dismissItemDetails()
                        showPlayer = true
                    },
                    onPlayFromChapter = { startTimeSeconds ->
                        playerItem = item // Set immediately so player has item to display
                        viewModel.playItemFromTime(item, startTimeSeconds)
                        viewModel.dismissItemDetails()
                        showPlayer = true
                    },
                    onReadClick = if (item.hasEbook) {
                        {
                            item.ebookFile?.let { file ->
                                ebookItem = item
                                ebookFile = file
                                showEpubReader = true
                                viewModel.dismissItemDetails()
                            }
                        }
                    } else null
                )
            }

            selectedSeries?.let { series ->
                SeriesDialog(
                    series = series,
                    hostUrl = hostUrl,
                    apiToken = apiKey,
                    onDismiss = viewModel::dismissSeriesDetails,
                    onBookClick = { bookId ->
                        viewModel.dismissSeriesDetails()
                        viewModel.selectItemById(bookId)
                    }
                )
            }
            } // End of inner Box
        } // End of NavigationDrawer content

        // Fullscreen Media Player - OUTSIDE NavigationDrawer for true fullscreen
        if (showPlayer && playerItem != null) {
            val isPlaying by viewModel.isPlaying.collectAsState()
            val currentTimeMs by viewModel.currentTime.collectAsState()
            val durationMs by viewModel.duration.collectAsState()
            val playbackSpeed by viewModel.playbackSpeed.collectAsState()
            val currentTrackTitle by viewModel.currentTrackTitle.collectAsState()

            MediaPlayerScreen(
                item = playerItem,
                isPlaying = isPlaying,
                currentTimeMs = currentTimeMs,
                durationMs = durationMs,
                playbackSpeed = playbackSpeed,
                currentTrackTitle = currentTrackTitle,
                hostUrl = hostUrl,
                apiToken = apiKey,
                onDismiss = { showPlayer = false },
                onPlayPause = viewModel::playPause,
                onSkipBackward = viewModel::skipBackward,
                onSkipForward = viewModel::skipForward,
                onPreviousTrack = viewModel::previousTrack,
                onNextTrack = viewModel::nextTrack,
                onSpeedDecrease = viewModel::decreaseSpeed,
                onSpeedIncrease = viewModel::increaseSpeed,
                onSeek = viewModel::seekTo
            )
        }

        // EPUB Reader - OUTSIDE NavigationDrawer for true fullscreen
        if (showEpubReader && ebookItem != null && ebookFile != null) {
            EpubReaderScreen(
                item = ebookItem!!,
                ebookFile = ebookFile!!,
                hostUrl = hostUrl,
                apiToken = apiKey,
                onDismiss = {
                    showEpubReader = false
                    ebookItem = null
                    ebookFile = null
                }
            )
        }
    } // End of top-level Box
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvDrawerContent(
    drawerValue: TvDrawerValue,
    libraries: List<com.swiftshelf.data.model.LibrarySummary>,
    selectedLibraryIds: Set<String>,
    currentLibraryId: String?,
    currentTab: Int,
    currentItem: com.swiftshelf.data.model.LibraryItem?,
    onLibraryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onUserClick: () -> Unit,
    onNowPlayingClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val selectedLibraries = libraries.filter { selectedLibraryIds.contains(it.id) }

    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User initial at top
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onUserClick() }
                .focusable(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "M",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))

        // Search icon
        IconButton(
            onClick = onSearchClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(28.dp),
                tint = if (currentTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))

        // Library initials (first letter of each library name)
        selectedLibraries.forEach { library ->
            val isActive = library.id == currentLibraryId
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onLibraryClick(library.id) }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = library.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Now Playing icon (only show if something is playing)
        if (currentItem != null) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            IconButton(
                onClick = onNowPlayingClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = "Now Playing",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Settings icon at bottom
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(28.dp),
                tint = if (currentTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
