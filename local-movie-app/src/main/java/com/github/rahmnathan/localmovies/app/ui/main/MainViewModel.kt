package com.github.rahmnathan.localmovies.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rahmnathan.localmovies.app.cast.GoogleCastUtils
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
    val currentParentId: String? = null,  // Parent ID for navigating into Series/Seasons
    val selectedTab: Int = 0, // 0=Movies, 1=Series, 2=Controls, 3=More
    val currentPage: Int = 0,
    val hasMorePages: Boolean = true,
    val totalCount: Long = 0,
    val sortOrder: String = "title", // title, year, rating, added
    val genreFilter: String? = null,
    val typeFilter: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val googleCastUtils: GoogleCastUtils,
    private val preferencesDataStore: com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore,
    private val networkConnectivityObserver: com.github.rahmnathan.localmovies.app.data.local.NetworkConnectivityObserver
) : ViewModel() {

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
                currentParentId = null,
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
                currentParentId = mediaFileId,
                typeFilter = null,
                searchQuery = "",
                currentPage = 0,
                hasMorePages = true
            )
        }
        loadMedia(resetList = true)
    }

    fun navigateBack(): Boolean {
        val currentPath = _uiState.value.currentPath
        return if (currentPath.size > 1 && _uiState.value.typeFilter != "history") {
            val newPath = currentPath.dropLast(1)
            _uiState.update {
                it.copy(
                    currentPath = newPath,
                    currentParentId = null,  // Clear parentId when navigating back to root
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
                currentParentId = null,
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

            android.util.Log.d("MainViewModel", "loadMedia called: resetList=$resetList, page=$page, parentId=$parentId, searchQuery=$searchQuery, sort=${_uiState.value.sortOrder}, genre=${_uiState.value.genreFilter}, type=${_uiState.value.typeFilter}")

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

                            android.util.Log.d("MainViewModel", "Loaded ${newList.size} of $totalCount total items, hasMorePages=$hasMorePages")

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
            android.util.Log.d("MainViewModel", "Skipping loadMoreMedia: hasMorePages=${currentState.hasMorePages}, isLoading=${currentState.isLoading}")
            return
        }

        val nextPage = currentState.currentPage + 1
        android.util.Log.d("MainViewModel", "Loading page $nextPage (current list size: ${currentState.mediaList.size})")

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
                android.util.Log.d("MainViewModel", "Playing ${media.title} with resume position: $resumePosition ms")

                // Check if Cast is active
                if (googleCastUtils.isCastSessionActive()) {
                    android.util.Log.d("MainViewModel", "Cast session active, playing on Cast device")

                    // Only queue remaining episodes if this is a series episode (has episode number)
                    val remainingEpisodes = if (!media.number.isNullOrBlank()) {
                        val currentMediaList = _uiState.value.mediaList
                        val currentIndex = currentMediaList.indexOfFirst { it.mediaFileId == media.mediaFileId }
                        if (currentIndex >= 0 && currentIndex < currentMediaList.size - 1) {
                            currentMediaList.subList(currentIndex + 1, currentMediaList.size)
                        } else {
                            emptyList()
                        }
                    } else {
                        // Not a series episode, don't queue anything
                        emptyList()
                    }

                    android.util.Log.d("MainViewModel", "Media is ${if (media.number.isNullOrBlank()) "movie" else "series episode"}, queueing ${remainingEpisodes.size} remaining episodes for cast")

                    val success = googleCastUtils.playOnCast(media, remainingEpisodes, resumePosition)
                    if (success) {
                        android.util.Log.d("MainViewModel", "Successfully sent media to Cast device")
                        _uiState.update { it.copy(isLoading = false) }
                        return@launch
                    } else {
                        android.util.Log.w("MainViewModel", "Failed to play on Cast, falling back to local playback")
                    }
                }

                // Play locally if Cast is not active or failed
                android.util.Log.d("MainViewModel", "Playing locally - Getting signed URLs for mediaFileId: ${media.mediaFileId}")
                when (val result = mediaRepository.getSignedUrls(media.mediaFileId)) {
                    is Result.Success -> {
                        val signedUrls = result.data
                        val streamUrl = signedUrls.stream ?: run {
                            android.util.Log.e("MainViewModel", "Stream URL is null in signed URLs response")
                            _uiState.update { it.copy(isLoading = false, error = "Invalid stream URL") }
                            return@launch
                        }
                        val updatePositionUrl = signedUrls.updatePosition ?: run {
                            android.util.Log.e("MainViewModel", "UpdatePosition URL is null in signed URLs response")
                            _uiState.update { it.copy(isLoading = false, error = "Invalid update position URL") }
                            return@launch
                        }

                        android.util.Log.d("MainViewModel", "Got signed URLs - stream: $streamUrl")
                        android.util.Log.d("MainViewModel", "Got signed URLs - updatePosition: $updatePositionUrl")
                        onNavigateToPlayer(streamUrl, updatePositionUrl, media.mediaFileId, resumePosition)
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is Result.Error -> {
                        android.util.Log.e("MainViewModel", "Failed to get signed URLs: ${result.message}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to get video URL: ${result.message}"
                            )
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Exception playing media", e)
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
