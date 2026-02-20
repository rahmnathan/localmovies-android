package com.github.rahmnathan.localmovies.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.app.MediaRouteButton
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.ui.cast.CastMiniController
import com.google.android.gms.cast.framework.CastButtonFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToPlayer: (url: String, updatePositionUrl: String, mediaId: String, resumePosition: Long) -> Unit = { _, _, _, _ -> },
    onNavigateToCastController: () -> Unit = {},
    onNavigateToSetup: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedMediaForDetails by remember { mutableStateOf<Media?>(null) }
    var isLoadingSelectedMediaDetails by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.currentPath.size > 1) {
        viewModel.navigateBack()
    }

    LaunchedEffect(selectedMediaForDetails?.mediaFileId) {
        val media = selectedMediaForDetails ?: return@LaunchedEffect
        if (!media.plot.isNullOrBlank() || !media.actors.isNullOrBlank()) {
            isLoadingSelectedMediaDetails = false
            return@LaunchedEffect
        }

        val requestedMediaId = media.mediaFileId
        isLoadingSelectedMediaDetails = true
        viewModel.loadMediaDetails(media) { loaded ->
            if (selectedMediaForDetails?.mediaFileId != requestedMediaId) {
                return@loadMediaDetails
            }

            if (loaded != null) {
                selectedMediaForDetails = loaded.copy(
                    favorite = selectedMediaForDetails?.favorite ?: loaded.favorite
                )
            }
            isLoadingSelectedMediaDetails = false
        }
    }

    // Media details dialog
    selectedMediaForDetails?.let { media ->
        // Track favorite state locally for immediate UI feedback
        var isFavorite by remember(media.mediaFileId) { mutableStateOf(media.favorite) }

        MediaDetailsDialog(
            media = media,
            isFavorite = isFavorite,
            isLoadingDetails = isLoadingSelectedMediaDetails,
            onDismiss = {
                selectedMediaForDetails = null
                isLoadingSelectedMediaDetails = false
            },
            onPlay = { resumePosition ->
                selectedMediaForDetails = null
                if (media.streamable) {
                    viewModel.playMedia(media, resumePosition, onNavigateToPlayer)
                } else {
                    viewModel.navigateToDirectory(media.mediaFileId, media.filename, media.type)
                }
            },
            onFavoriteClick = {
                isFavorite = !isFavorite
                viewModel.toggleFavorite(media)
            }
        )
    }

    // Settings dialog
    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false },
            onNavigateToSetup = onNavigateToSetup
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchExpanded) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Search...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                    } else {
                        Text("LocalMovies")
                    }
                },
                actions = {
                    if (isSearchExpanded) {
                        IconButton(onClick = {
                            isSearchExpanded = false
                            viewModel.onSearchQueryChange("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    } else {
                        IconButton(onClick = { isSearchExpanded = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }

                        // Sort button with dropdown
                        SortDropdownMenu(
                            expanded = showSortMenu,
                            onExpandedChange = { showSortMenu = it },
                            onSortOrderChange = { order ->
                                viewModel.onSortOrderChange(order)
                                showSortMenu = false
                            }
                        )

                        // Filter button with dropdown
                        FilterDropdownMenu(
                            expanded = showFilterMenu,
                            onExpandedChange = { showFilterMenu = it },
                            onGenreFilterChange = { genre ->
                                viewModel.onGenreFilterChange(genre)
                                showFilterMenu = false
                            }
                        )
                    }

                    // Cast button using AndroidView
                    AndroidView(
                        factory = { context ->
                            MediaRouteButton(context).apply {
                                try {
                                    CastButtonFactory.setUpMediaRouteButton(context, this)
                                } catch (e: Exception) {
                                    android.util.Log.e("MainScreen", "Failed to setup Cast button", e)
                                    visibility = android.view.View.GONE
                                }
                            }
                        },
                        modifier = Modifier
                            .width(48.dp)
                            .height(48.dp)
                            .padding(end = 8.dp)
                    )

                    // Settings button
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Cast mini controller (shows when casting)
                CastMiniController(
                    onClick = onNavigateToCastController
                )

                // Navigation bar
                NavigationBar {
                    NavigationBarItem(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.onTabSelected(0) },
                        icon = { Icon(Icons.Filled.Movie, contentDescription = "Movies") },
                        label = { Text("Movies") }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.onTabSelected(1) },
                        icon = { Icon(Icons.Filled.Tv, contentDescription = "Series") },
                        label = { Text("Series") }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 2,
                        onClick = { viewModel.onTabSelected(2) },
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
                        label = { Text("Favorites") }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 3,
                        onClick = { viewModel.onTabSelected(3) },
                        icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                        label = { Text("History") }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 4,
                        onClick = { viewModel.onTabSelected(4) },
                        icon = { Icon(Icons.Filled.Stars, contentDescription = "For You") },
                        label = { Text("For You") }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Breadcrumb navigation hint (e.g., "Breaking Bad → Season 1")
            if (uiState.currentPath.size > 1 && uiState.typeFilter == null) {
                BreadcrumbBar(
                    path = uiState.currentPath,
                    onNavigateBack = { viewModel.navigateBack() }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                // Offline indicator banner
                if (uiState.isOffline) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                when {
                    uiState.isLoading && uiState.mediaList.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.error != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.retryLoading() }) {
                                Text("Retry")
                            }
                        }
                    }
                    uiState.typeFilter == "recommendations" && uiState.recommendations.isEmpty() && !uiState.isLoadingRecommendations -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stars,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No recommendations yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Watch some movies or shows to get personalized suggestions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    uiState.mediaList.isEmpty() && uiState.typeFilter != "recommendations" -> {
                        EmptyState(
                            searchQuery = uiState.searchQuery,
                            currentPath = uiState.currentPath,
                            typeFilter = uiState.typeFilter,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.typeFilter == "history" -> {
                        // Group history items by series for "Continue Watching" experience
                        HistoryList(
                            mediaList = uiState.mediaList,
                            isLoading = uiState.isLoading,
                            onPlayMedia = { media, resumePosition ->
                                viewModel.playMedia(media, resumePosition, onNavigateToPlayer)
                            },
                            onShowDetails = { media ->
                                selectedMediaForDetails = media
                            },
                            onRemoveFromHistory = { media ->
                                viewModel.removeFromHistory(media)
                            }
                        )
                    }
                    uiState.typeFilter == "recommendations" -> {
                        RecommendationsList(
                            recommendations = uiState.recommendations,
                            isLoading = uiState.isLoadingRecommendations,
                            onMediaClick = { media ->
                                selectedMediaForDetails = media
                            },
                            onPlayMedia = { media ->
                                val resumePosition = media.getResumePosition() ?: 0L
                                viewModel.playMedia(media, resumePosition, onNavigateToPlayer)
                            },
                            onMoreLikeThis = { media ->
                                viewModel.onRecommendationMoreLikeThis(media)
                            },
                            onNotInterested = { media ->
                                viewModel.onRecommendationNotInterested(media)
                            }
                        )
                    }
                    else -> {
                        MediaGrid(
                            mediaList = uiState.mediaList,
                            isLoading = uiState.isLoading,
                            hasMorePages = uiState.hasMorePages,
                            selectedTab = uiState.selectedTab,
                            searchQuery = uiState.searchQuery,
                            sortOrder = uiState.sortOrder,
                            genreFilter = uiState.genreFilter,
                            onLoadMore = { viewModel.loadMoreMedia() },
                            onMediaClick = { media ->
                                if (media.streamable) {
                                    // Show details dialog for playable content (movies, episodes)
                                    selectedMediaForDetails = media
                                } else {
                                    // Navigate directly into series/seasons
                                    viewModel.navigateToDirectory(media.mediaFileId, media.filename, media.type)
                                }
                            },
                            onMediaLongClick = { media ->
                                // Long press always shows details (for series info, etc.)
                                selectedMediaForDetails = media
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortDropdownMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSortOrderChange: (String) -> Unit
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Default.Sort, contentDescription = "Sort")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Title") },
                onClick = { onSortOrderChange("title") }
            )
            DropdownMenuItem(
                text = { Text("Year") },
                onClick = { onSortOrderChange("year") }
            )
            DropdownMenuItem(
                text = { Text("Rating") },
                onClick = { onSortOrderChange("rating") }
            )
            DropdownMenuItem(
                text = { Text("Date Added") },
                onClick = { onSortOrderChange("added") }
            )
        }
    }
}

@Composable
private fun FilterDropdownMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onGenreFilterChange: (String?) -> Unit
) {
    val genres = listOf(
        null to "All",
        "action" to "Action",
        "comedy" to "Comedy",
        "fantasy" to "Fantasy",
        "horror" to "Horror",
        "sci-fi" to "Sci-Fi",
        "thriller" to "Thriller",
        "war" to "War"
    )

    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Default.FilterList, contentDescription = "Filter")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            genres.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onGenreFilterChange(value) }
                )
            }
        }
    }
}

@Composable
private fun MediaGrid(
    mediaList: List<Media>,
    isLoading: Boolean,
    hasMorePages: Boolean,
    selectedTab: Int,
    searchQuery: String,
    sortOrder: String,
    genreFilter: String?,
    onLoadMore: () -> Unit,
    onMediaClick: (Media) -> Unit,
    onMediaLongClick: (Media) -> Unit
) {
    val listState = rememberLazyGridState()

    // Scroll to top when tab, search, sort, or filter changes
    LaunchedEffect(selectedTab, searchQuery, sortOrder, genreFilter) {
        listState.scrollToItem(0)
    }

    // Detect when we're near the bottom and load more
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= (totalItems - 10)
        }
    }

    LaunchedEffect(shouldLoadMore, hasMorePages, isLoading) {
        if (shouldLoadMore && hasMorePages && !isLoading) {
            onLoadMore()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        state = listState,
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = mediaList,
            key = { it.mediaFileId },
            contentType = { "media_card" }
        ) { media ->
            MediaCard(
                media = media,
                onClick = { onMediaClick(media) },
                onLongClick = { onMediaLongClick(media) }
            )
        }

        // Show loading indicator at the bottom when loading more
        if (isLoading && mediaList.isNotEmpty()) {
            item(
                key = "loading_indicator",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "loading"
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateToSetup: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Show current user info if available
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "You can logout to switch accounts or change server URL",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.logout()
                    onDismiss()
                    onNavigateToSetup()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptyState(
    searchQuery: String,
    currentPath: List<String>,
    typeFilter: String?,
    modifier: Modifier = Modifier
) {
    val (title, message, icon) = when {
        searchQuery.isNotBlank() -> {
            Triple("No Results", "No media found matching \"$searchQuery\"", Icons.Default.Search)
        }
        typeFilter == "favorites" -> {
            Triple("No Favorites", "Tap the heart icon on any media to add it to your favorites", Icons.Default.Favorite)
        }
        typeFilter == "history" -> {
            Triple("No History", "Your viewing history will appear here", Icons.Default.History)
        }
        currentPath.size > 1 -> {
            Triple("Empty Folder", "This folder doesn't contain any media", Icons.Default.Movie)
        }
        else -> {
            Triple("No Media", "No ${currentPath.firstOrNull() ?: "content"} available", Icons.Default.Movie)
        }
    }

    Column(
        modifier = modifier
            .padding(48.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Icon with glow effect background
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Glow circle background
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Em)
        )
    }
}
