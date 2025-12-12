package com.github.rahmnathan.localmovies.app.ui.main

import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.app.MediaRouteButton
import coil3.compose.AsyncImage
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.ui.cast.CastMiniController
import com.google.android.gms.cast.framework.CastButtonFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToPlayer: (url: String, updatePositionUrl: String) -> Unit = { _, _ -> },
    onNavigateToCastController: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.currentPath.size > 1) {
        viewModel.navigateBack()
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
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Title") },
                                    onClick = {
                                        viewModel.onSortOrderChange("title")
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Year") },
                                    onClick = {
                                        viewModel.onSortOrderChange("year")
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rating") },
                                    onClick = {
                                        viewModel.onSortOrderChange("rating")
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date Added") },
                                    onClick = {
                                        viewModel.onSortOrderChange("added")
                                        showSortMenu = false
                                    }
                                )
                            }
                        }

                        // Filter button with dropdown
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All") },
                                    onClick = {
                                        viewModel.onGenreFilterChange(null)
                                        showFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Action") },
                                    onClick = {
                                        viewModel.onGenreFilterChange("action")
                                        showFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Comedy") },
                                    onClick = {
                                        viewModel.onGenreFilterChange("comedy")
                                        showFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Fantasy") },
                                    onClick = {
                                        viewModel.onGenreFilterChange("fantasy")
                                        showFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Horror") },
                                    onClick = {
                                        viewModel.onGenreFilterChange("horror")
                                        showFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sci-Fi") },
                                    onClick = {
                                        viewModel.onGenreFilterChange("sci-fi")
                                        showFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Thriller") },
                                    onClick = {
                                        viewModel.onGenreFilterChange("thriller")
                                        showFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("War") },
                                    onClick = {
                                        viewModel.onGenreFilterChange("war")
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Cast button using AndroidView
                    AndroidView(
                        factory = { context ->
                            MediaRouteButton(context).apply {
                                try {
                                    CastButtonFactory.setUpMediaRouteButton(context, this)
                                } catch (e: Exception) {
                                    android.util.Log.e("MainScreen", "Failed to setup Cast button", e)
                                    // Hide the button if Cast is not available
                                    visibility = android.view.View.GONE
                                }
                            }
                        },
                        modifier = Modifier
                            .width(48.dp)
                            .height(48.dp)
                            .padding(end = 8.dp)
                    )
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
                        icon = {},
                        label = { Text("Movies") }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.onTabSelected(1) },
                        icon = {},
                        label = { Text("Series") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
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
                        Button(onClick = { viewModel.clearError() }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.mediaList.isEmpty() -> {
                    Text(
                        text = "No media found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    val listState = rememberLazyGridState()

                    // Detect when we're near the bottom and load more
                    LaunchedEffect(listState) {
                        snapshotFlow {
                            val layoutInfo = listState.layoutInfo
                            val totalItems = layoutInfo.totalItemsCount
                            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

                            // Load more when we're within 10 items of the end
                            lastVisibleItemIndex >= (totalItems - 10)
                        }
                        .collect { shouldLoadMore ->
                            if (shouldLoadMore && uiState.hasMorePages && !uiState.isLoading) {
                                viewModel.loadMoreMedia()
                            }
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(90.dp),
                        state = listState,
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = uiState.mediaList,
                            key = { it.mediaFileId }
                        ) { media ->
                            MediaCard(
                                media = media,
                                onClick = {
                                    if (media.streamable) {
                                        viewModel.playMedia(media, onNavigateToPlayer)
                                    } else {
                                        viewModel.navigateToDirectory(media.filename)
                                    }
                                }
                            )
                        }

                        // Show loading indicator at the bottom when loading more
                        if (uiState.isLoading && uiState.mediaList.isNotEmpty()) {
                            item {
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
            }
        }
    }
}

@Composable
fun MediaCard(
    media: Media,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!media.image.isNullOrBlank()) {
                // Decode base64 image
                val imageBytes = try {
                    Base64.decode(media.image, Base64.DEFAULT)
                } catch (e: Exception) {
                    null
                }

                if (imageBytes != null) {
                    AsyncImage(
                        model = imageBytes,
                        contentDescription = media.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback if base64 decode fails
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    )
                }
            } else {
                // No image available
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
