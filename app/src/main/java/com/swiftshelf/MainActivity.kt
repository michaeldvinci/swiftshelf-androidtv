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
import androidx.compose.material3.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: SwiftShelfViewModel,
    audioManager: com.swiftshelf.audio.GlobalAudioManager?
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val hostUrl by viewModel.hostUrl.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val libraries by viewModel.libraries.collectAsState()
    val selectedLibraryIds by viewModel.selectedLibraryIds.collectAsState()
    val currentLibraryId by viewModel.currentLibraryId.collectAsState()
    val currentLibrary = libraries.firstOrNull { it.id == currentLibraryId }

    // Audio state
    val currentItem by (audioManager?.currentItem ?: MutableStateFlow(null)).collectAsState()
    val isPlaying by (audioManager?.isPlaying ?: MutableStateFlow(false)).collectAsState()
    val currentTime by (audioManager?.currentTime ?: MutableStateFlow(0L)).collectAsState()
    val duration by (audioManager?.duration ?: MutableStateFlow(0L)).collectAsState()
    val playbackSpeed by (audioManager?.playbackSpeed ?: MutableStateFlow(1.0f)).collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val drawerSearchFocusRequester = remember { FocusRequester() }
    val firstCarouselFocusRequester = remember { FocusRequester() }

    val openDrawer: () -> Unit = {
        scope.launch {
            drawerState.open()
            drawerSearchFocusRequester.requestFocus()
        }
    }
    val closeDrawer: () -> Unit = {
        scope.launch {
            drawerState.close()
            firstCarouselFocusRequester.requestFocus()
        }
    }

    // Handle back button to close drawer
    androidx.activity.compose.BackHandler(enabled = drawerState.isOpen) {
        closeDrawer()
    }

    // Auto-focus first carousel item when library tab loads
    val recentItems by viewModel.recentItems.collectAsState()
    val continueListeningItems by viewModel.continueListeningItems.collectAsState()

    LaunchedEffect(currentTab, recentItems, continueListeningItems) {
        // Only auto-focus on library tab (1) and when drawer is closed
        if (currentTab == 1 && !drawerState.isOpen) {
            val hasItems = recentItems.isNotEmpty() || continueListeningItems.isNotEmpty()
            if (hasItems) {
                kotlinx.coroutines.delay(100) // Small delay to ensure UI is rendered
                firstCarouselFocusRequester.requestFocus()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            DrawerContent(
                libraries = libraries,
                selectedLibraryIds = selectedLibraryIds,
                currentLibraryId = currentLibraryId,
                searchFocusRequester = drawerSearchFocusRequester,
                onNavigateRight = closeDrawer,
                onLibraryClick = { libraryId ->
                    viewModel.setCurrentLibrary(libraryId)
                    viewModel.setCurrentTab(1)
                    closeDrawer()
                },
                onSearchClick = {
                    viewModel.setCurrentTab(0)
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
            Scaffold(
                bottomBar = {
                    Column {
                        if (currentItem != null && duration > 0) {
                            val progress = currentTime.toFloat() / duration.toFloat()
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (currentItem != null) {
                            CompactPlayer(
                                item = currentItem,
                                isPlaying = isPlaying,
                                currentTime = currentTime,
                                duration = duration,
                                playbackSpeed = playbackSpeed,
                                hostUrl = hostUrl,
                                apiToken = apiKey,
                                onPlayPause = viewModel::playPause,
                                onSkipBackward = { viewModel.skipBackward() },
                                onSkipForward = { viewModel.skipForward() },
                                onPreviousTrack = { viewModel.previousTrack() },
                                onNextTrack = { viewModel.nextTrack() },
                                onSpeedDecrease = { viewModel.setPlaybackSpeed((playbackSpeed - 0.1f).coerceAtLeast(0.5f)) },
                                onSpeedIncrease = { viewModel.setPlaybackSpeed((playbackSpeed + 0.1f).coerceAtMost(3.0f)) }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                val bottomInset = if (currentItem != null) 120.dp else 0.dp
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(bottom = bottomInset)
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
                                onItemClick = viewModel::selectItem
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
                    onPlayClick = { viewModel.playItem(item) }
                )
            }
        }
    }
}

@Composable
fun DrawerContent(
    libraries: List<com.swiftshelf.data.model.LibrarySummary>,
    selectedLibraryIds: Set<String>,
    currentLibraryId: String?,
    searchFocusRequester: FocusRequester,
    onNavigateRight: () -> Unit,
    onLibraryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Filter to show only selected libraries
    val selectedLibraries = libraries.filter { selectedLibraryIds.contains(it.id) }

    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp)
            .onPreviewKeyEvent { event ->
                // Handle right D-pad press to close drawer
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                    onNavigateRight()
                    true
                } else {
                    false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with search icon and user initials
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .focusRequester(searchFocusRequester)
                        .focusable()
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // User initials circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ME",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Show selected libraries as clickable items
            if (selectedLibraries.isNotEmpty()) {
                Text(
                    text = "Libraries",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                )

                selectedLibraries.forEach { library ->
                    val isActive = library.id == currentLibraryId
                    Text(
                        text = library.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isActive) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable { onLibraryClick(library.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Settings
            NavigationDrawerItem(
                label = { Text("Settings") },
                selected = false,
                onClick = onSettingsClick,
                icon = {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
