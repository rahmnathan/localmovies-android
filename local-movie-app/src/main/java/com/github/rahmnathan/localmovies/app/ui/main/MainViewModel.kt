package com.github.rahmnathan.localmovies.app.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rahmnathan.localmovies.app.data.repository.MediaRepository
import com.github.rahmnathan.localmovies.app.data.repository.Result
import com.github.rahmnathan.localmovies.app.media.data.Media
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val mediaList: List<Media> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val currentPath: List<String> = listOf("Movies"),
    val parentIdStack: List<String?> = listOf(null),  // Stack of parent IDs matching currentPath
    val selectedTab: Int = 0, // 0=Movies, 1=Series, 2=Controls, 3=More
    val currentPage: Int = 0,
    val hasMorePages: Boolean = true,
    val totalCount: Long = 0,
    val sortOrder: String = "title", // title, year, rating, added
    val genreFilter: String? = null,
    val typeFilter: String? = null,
    val isOffline: Boolean = false
) {
    /** Current parent ID is the last item in the stack */
    val currentParentId: String? get() = parentIdStack.lastOrNull()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playbackCoordinator: PlaybackCoordinator,
    private val preferencesDataStore: com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore,
    private val networkConnectivityObserver: com.github.rahmnathan.localmovies.app.data.local.NetworkConnectivityObserver
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Search query flow with debouncing
    private val searchQueryFlow = MutableStateFlow("")

    init {
        // Observe network connectivity
        viewModelScope.launch {
            networkConnectivityObserver.isConnected.collect { isConnected ->
                _uiState.update { it.copy(isOffline = !isConnected) }

                // Show offline error message when connection is lost
                if (!isConnected && _uiState.value.error == null) {
                    _uiState.update {
                        it.copy(error = "No internet connection")
                    }
                }
            }
        }

        // Debounce search queries (300ms delay after user stops typing)
        viewModelScope.launch {
            searchQueryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    _uiState.update { it.copy(searchQuery = query, currentPage = 0, hasMorePages = true) }
                    loadMedia(resetList = true)
                }
        }

        // Load initial data (Movies root)
        loadMedia()
    }

    fun onSearchQueryChange(query: String) {
        // Update UI immediately for responsive typing
        _uiState.update { it.copy(searchQuery = query) }
        // Emit to debounced flow for actual search
        searchQueryFlow.value = query
    }

    fun navigateToRoot(rootPath: String) {
        _uiState.update {
            it.copy(
                currentPath = listOf(rootPath),
                parentIdStack = listOf(null),
                typeFilter = when(rootPath) {
                    "Movies" -> "MOVIES"
                    "Series" -> "SERIES"
                    else -> null
                },
                searchQuery = "",
                currentPage = 0,
                hasMorePages = true
            )
        }
        loadMedia(resetList = true)
    }

    fun navigateToDirectory(mediaFileId: String, directoryName: String) {
        _uiState.update { state ->
            state.copy(
                currentPath = state.currentPath + directoryName,
                parentIdStack = state.parentIdStack + mediaFileId,
                typeFilter = null,
                searchQuery = "",
                currentPage = 0,
                hasMorePages = true
            )
        }
        loadMedia(resetList = true)
    }

    fun navigateBack(): Boolean {
        val currentState = _uiState.value
        return if (currentState.currentPath.size > 1 && currentState.typeFilter != "history") {
            val newPath = currentState.currentPath.dropLast(1)
            val newParentIdStack = currentState.parentIdStack.dropLast(1)
            _uiState.update {
                it.copy(
                    currentPath = newPath,
                    parentIdStack = newParentIdStack,
                    typeFilter = when {
                        newPath.size == 1 && newPath[0] == "Movies" -> "MOVIES"
                        newPath.size == 1 && newPath[0] == "Series" -> "SERIES"
                        else -> null
                    },
                    searchQuery = "",
                    currentPage = 0,
                    hasMorePages = true
                )
            }
            loadMedia(resetList = true)
            true
        } else {
            false // At root, let system handle back
        }
    }

    fun onTabSelected(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
        when (tabIndex) {
            0 -> navigateToRoot("Movies")
            1 -> navigateToRoot("Series")
            2 -> loadHistoryTab()
        }
    }

    private fun loadHistoryTab() {
        _uiState.update {
            it.copy(
                currentPath = listOf("History"),
                parentIdStack = listOf(null),
                typeFilter = "history",
                searchQuery = "",
                currentPage = 0,
                hasMorePages = true
            )
        }
        loadMedia(resetList = true)
    }

    fun onSortOrderChange(order: String) {
        _uiState.update { it.copy(sortOrder = order, currentPage = 0, hasMorePages = true) }
        loadMedia(resetList = true)
    }

    fun onGenreFilterChange(genre: String?) {
        _uiState.update { it.copy(genreFilter = genre, currentPage = 0, hasMorePages = true) }
        loadMedia(resetList = true)
    }

    private fun loadMedia(resetList: Boolean = false) {
        viewModelScope.launch {
            val page = if (resetList) 0 else _uiState.value.currentPage
            val searchQuery = _uiState.value.searchQuery.takeIf { it.isNotBlank() }
            val parentId = _uiState.value.currentParentId

            Log.d(TAG, "loadMedia called: resetList=$resetList, page=$page, parentId=$parentId, searchQuery=$searchQuery, sort=${_uiState.value.sortOrder}, genre=${_uiState.value.genreFilter}, type=${_uiState.value.typeFilter}")

            mediaRepository.getMediaList(
                parentId = parentId,
                page = page,
                size = 50,
                order = _uiState.value.sortOrder,
                searchQuery = searchQuery,
                genre = _uiState.value.genreFilter,
                type = _uiState.value.typeFilter
            ).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Result.Success -> {
                        _uiState.update { state ->
                            val newList = if (resetList) {
                                result.data
                            } else {
                                state.mediaList + result.data
                            }

                            // Determine if more pages exist based on total count
                            val totalCount = if (result.totalCount > 0) result.totalCount else state.totalCount
                            val hasMorePages = newList.size < totalCount

                            Log.d(TAG, "Loaded ${newList.size} of $totalCount total items, hasMorePages=$hasMorePages")

                            state.copy(
                                mediaList = newList,
                                isLoading = false,
                                error = null,
                                currentPage = page,
                                totalCount = totalCount,
                                hasMorePages = hasMorePages
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMoreMedia() {
        val currentState = _uiState.value

        // Guard against loading if already loading or no more pages
        if (!currentState.hasMorePages || currentState.isLoading) {
            Log.d(TAG, "Skipping loadMoreMedia: hasMorePages=${currentState.hasMorePages}, isLoading=${currentState.isLoading}")
            return
        }

        val nextPage = currentState.currentPage + 1
        Log.d(TAG, "Loading page $nextPage (current list size: ${currentState.mediaList.size})")

        _uiState.update { it.copy(currentPage = nextPage) }
        loadMedia(resetList = false)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun playMedia(media: Media, resumePosition: Long = 0, onNavigateToPlayer: (url: String, updatePositionUrl: String, mediaId: String, resumePosition: Long) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val remainingEpisodes = playbackCoordinator.getRemainingEpisodes(media, _uiState.value.mediaList)
                Log.d(TAG, "Media is ${if (media.number.isNullOrBlank()) "movie" else "series episode"}, queueing ${remainingEpisodes.size} remaining episodes")

                when (val result = playbackCoordinator.play(media, resumePosition, remainingEpisodes)) {
                    is PlaybackResult.PlayLocally -> {
                        onNavigateToPlayer(
                            result.streamUrl,
                            result.updatePositionUrl,
                            result.mediaId,
                            result.resumePosition
                        )
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is PlaybackResult.PlayingOnCast -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is PlaybackResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception playing media", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to play media: ${e.message}"
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferencesDataStore.clearCredentials()
        }
    }
}
