package com.github.rahmnathan.localmovies.app.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rahmnathan.localmovies.app.data.repository.MediaRepository
import com.github.rahmnathan.localmovies.app.data.repository.Result
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.media.data.Recommendation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    val selectedTab: Int = 0, // 0=Movies, 1=Series, 2=Favorites, 3=History, 4=For You
    val currentPage: Int = 0,
    val hasMorePages: Boolean = true,
    val totalCount: Long = 0,
    val sortOrder: String = "title", // title, year, rating, added
    val genreFilter: String? = null,
    val typeFilter: String? = "MOVIES",  // Default to Movies tab filter
    val isOffline: Boolean = false,
    val recommendations: List<Recommendation> = emptyList(),
    val isLoadingRecommendations: Boolean = false,
    val continueWatching: List<Media> = emptyList(),
    val isLoadingContinueWatching: Boolean = false
) {
    /** Current parent ID is the last item in the stack */
    val currentParentId: String? get() = parentIdStack.lastOrNull()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playbackCoordinator: PlaybackCoordinator,
    private val preferencesDataStore: com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore,
    private val networkConnectivityObserver: com.github.rahmnathan.localmovies.app.data.local.NetworkConnectivityObserver,
    private val episodeQueueManager: com.github.rahmnathan.localmovies.app.ui.player.EpisodeQueueManager
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
        private const val PAGE_SIZE = 50
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Search query flow with debouncing
    private val searchQueryFlow = MutableStateFlow("")

    // Track current loading job to cancel on filter/tab changes
    private var currentLoadJob: Job? = null
    private var dismissedRecommendationIds: Set<String> = emptySet()
    private val mediaDetailsCache: MutableMap<String, Media> = mutableMapOf()

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

        viewModelScope.launch {
            preferencesDataStore.dismissedRecommendationIdsFlow.collect { ids ->
                dismissedRecommendationIds = ids
                _uiState.update { state ->
                    state.copy(
                        recommendations = state.recommendations.filterNot { it.media.mediaFileId in ids }
                    )
                }
            }
        }

        loadContinueWatching()

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

    fun navigateToDirectory(mediaFileId: String, directoryName: String, mediaType: String) {
        // Determine the type filter based on what we're navigating into
        // Similar to webapp logic: SERIES -> SEASONS, SEASON -> EPISODES
        val childTypeFilter = when (mediaType.uppercase()) {
            "SERIES" -> "SEASONS"
            "SEASON" -> "EPISODES"
            "MOVIE_FOLDER" -> "MOVIES"
            "EPISODE_FOLDER" -> "EPISODES"
            else -> null
        }

        _uiState.update { state ->
            state.copy(
                currentPath = state.currentPath + directoryName,
                parentIdStack = state.parentIdStack + mediaFileId,
                typeFilter = childTypeFilter,
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

        if (tabIndex != 3 && tabIndex != 4) {
            loadContinueWatching()
        }

        when (tabIndex) {
            0 -> navigateToRoot("Movies")
            1 -> navigateToRoot("Series")
            2 -> loadFavoritesTab()
            3 -> loadHistoryTab()
            4 -> loadRecommendationsTab()
        }
    }

    private fun loadFavoritesTab() {
        _uiState.update {
            it.copy(
                currentPath = listOf("Favorites"),
                parentIdStack = listOf(null),
                typeFilter = "favorites",
                searchQuery = "",
                currentPage = 0,
                hasMorePages = true
            )
        }
        loadMedia(resetList = true)
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

    private fun loadRecommendationsTab() {
        _uiState.update {
            it.copy(
                currentPath = listOf("For You"),
                parentIdStack = listOf(null),
                typeFilter = "recommendations",
                searchQuery = "",
                currentPage = 0,
                hasMorePages = false,
                isLoadingRecommendations = true
            )
        }
        loadRecommendations()
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            try {
                val recommendations = mediaRepository.getRecommendations()
                    .filterNot { it.media.mediaFileId in dismissedRecommendationIds }
                _uiState.update {
                    it.copy(
                        recommendations = recommendations,
                        isLoadingRecommendations = false
                    )
                }
                Log.d(TAG, "Loaded ${recommendations.size} recommendations")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load recommendations", e)
                _uiState.update {
                    it.copy(
                        isLoadingRecommendations = false,
                        error = "Failed to load recommendations"
                    )
                }
            }
        }
    }

    private fun loadContinueWatching() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingContinueWatching = true) }

            mediaRepository.getMediaList(
                page = 0,
                size = 20,
                order = "added",
                includeDetails = false,
                type = "history"
            ).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val continueWatchingItems = result.data.filter { media ->
                            val progress = media.getWatchProgress()
                            media.streamable && progress != null && progress > 0.01f && progress < 0.98f
                        }
                        _uiState.update {
                            it.copy(
                                continueWatching = continueWatchingItems,
                                isLoadingContinueWatching = false
                            )
                        }
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Failed to load continue watching", result.exception)
                        _uiState.update { it.copy(isLoadingContinueWatching = false) }
                    }
                    is Result.Loading -> Unit
                }
            }
        }
    }

    fun onSortOrderChange(order: String) {
        _uiState.update { it.copy(sortOrder = order, currentPage = 0, hasMorePages = true) }
        loadMedia(resetList = true)
    }

    fun onGenreFilterChange(genre: String?) {
        _uiState.update { it.copy(genreFilter = genre, currentPage = 0, hasMorePages = true) }
        loadMedia(resetList = true)
    }

    fun onRecommendationNotInterested(media: Media) {
        _uiState.update { state ->
            state.copy(
                recommendations = state.recommendations.filterNot { it.media.mediaFileId == media.mediaFileId }
            )
        }

        viewModelScope.launch {
            preferencesDataStore.dismissRecommendation(media.mediaFileId)
        }
    }

    fun onRecommendationMoreLikeThis(media: Media) {
        val targetTab = if (isSeriesType(media.type)) 1 else 0
        onTabSelected(targetTab)

        val mappedGenre = mapGenreForFilter(media.genre)
        onGenreFilterChange(mappedGenre)
    }

    private fun isSeriesType(type: String): Boolean {
        val normalized = type.uppercase()
        return normalized == "SERIES" || normalized == "SEASON" || normalized == "EPISODE" || normalized == "EPISODES"
    }

    private fun mapGenreForFilter(rawGenre: String?): String? {
        val firstGenre = rawGenre
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.lowercase()
            ?: return null

        return when {
            "action" in firstGenre -> "action"
            "comedy" in firstGenre -> "comedy"
            "fantasy" in firstGenre -> "fantasy"
            "horror" in firstGenre -> "horror"
            "sci" in firstGenre -> "sci-fi"
            "thriller" in firstGenre -> "thriller"
            "war" in firstGenre -> "war"
            else -> null
        }
    }

    private fun loadMedia(resetList: Boolean = false) {
        // Cancel any in-flight request when starting a new one (especially on tab/filter change)
        if (resetList) {
            currentLoadJob?.cancel()
        }

        // Capture state snapshot BEFORE launching coroutine to avoid race conditions
        val stateSnapshot = _uiState.value
        val page = if (resetList) 0 else stateSnapshot.currentPage
        val searchQuery = stateSnapshot.searchQuery.takeIf { it.isNotBlank() }
        val parentId = stateSnapshot.currentParentId
        val typeFilter = stateSnapshot.typeFilter
        val selectedTab = stateSnapshot.selectedTab
        val sortOrder = stateSnapshot.sortOrder
        val genreFilter = stateSnapshot.genreFilter

        Log.d(TAG, "loadMedia called: resetList=$resetList, page=$page, parentId=$parentId, searchQuery=$searchQuery, sort=$sortOrder, genre=$genreFilter, type=$typeFilter")

        currentLoadJob = viewModelScope.launch {

            mediaRepository.getMediaList(
                parentId = parentId,
                page = page,
                size = PAGE_SIZE,
                order = sortOrder,
                includeDetails = false,
                searchQuery = searchQuery,
                genre = genreFilter,
                type = typeFilter
            ).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Result.Success -> {
                        _uiState.update { state ->
                            // Validate that the response is still relevant (user hasn't switched tabs)
                            if (state.selectedTab != selectedTab || state.typeFilter != typeFilter) {
                                Log.d(TAG, "Discarding stale response: expected tab=$selectedTab/type=$typeFilter, current tab=${state.selectedTab}/type=${state.typeFilter}")
                                return@update state.copy(isLoading = false)
                            }

                            val newList = if (resetList) {
                                result.data
                            } else {
                                // Deduplicate by mediaFileId to prevent LazyColumn key conflicts
                                val existingIds = state.mediaList.map { it.mediaFileId }.toSet()
                                val newItems = result.data.filter { it.mediaFileId !in existingIds }
                                if (newItems.size != result.data.size) {
                                    Log.w(TAG, "loadMedia: Filtered ${result.data.size - newItems.size} duplicate items")
                                }
                                state.mediaList + newItems
                            }

                            val totalCount = if (result.totalCount > 0) result.totalCount else state.totalCount
                            val hasMorePages = if (totalCount > 0) {
                                newList.size < totalCount
                            } else {
                                // Fallback when server does not provide Count header
                                result.data.size >= PAGE_SIZE
                            }

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
        // Atomically check and set isLoading to prevent concurrent page loads
        var shouldProceed = false
        var nextPage = 0

        _uiState.update { state ->
            if (!state.hasMorePages || state.isLoading) {
                Log.d(TAG, "Skipping loadMoreMedia: hasMorePages=${state.hasMorePages}, isLoading=${state.isLoading}")
                state // No change - already loading or no more pages
            } else {
                nextPage = state.currentPage + 1
                shouldProceed = true
                Log.d(TAG, "Loading page $nextPage (current list size: ${state.mediaList.size})")
                state.copy(currentPage = nextPage, isLoading = true)
            }
        }

        if (shouldProceed) {
            loadMediaPage(page = nextPage)
        }
    }

    private fun loadMediaPage(page: Int) {
        // Capture state snapshot BEFORE launching coroutine to avoid race conditions
        val stateSnapshot = _uiState.value
        val searchQuery = stateSnapshot.searchQuery.takeIf { it.isNotBlank() }
        val parentId = stateSnapshot.currentParentId
        val typeFilter = stateSnapshot.typeFilter
        val selectedTab = stateSnapshot.selectedTab
        val sortOrder = stateSnapshot.sortOrder
        val genreFilter = stateSnapshot.genreFilter

        Log.d(TAG, "loadMediaPage: page=$page, parentId=$parentId, searchQuery=$searchQuery, type=$typeFilter")

        currentLoadJob = viewModelScope.launch {
            mediaRepository.getMediaList(
                parentId = parentId,
                page = page,
                size = PAGE_SIZE,
                order = sortOrder,
                includeDetails = false,
                searchQuery = searchQuery,
                genre = genreFilter,
                type = typeFilter
            ).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        // isLoading already set in loadMoreMedia
                    }
                    is Result.Success -> {
                        _uiState.update { state ->
                            // Validate that the response is still relevant (user hasn't switched tabs)
                            if (state.selectedTab != selectedTab || state.typeFilter != typeFilter) {
                                Log.d(TAG, "Discarding stale page response: expected tab=$selectedTab/type=$typeFilter, current tab=${state.selectedTab}/type=${state.typeFilter}")
                                return@update state.copy(isLoading = false)
                            }

                            // Also validate we're appending to the right page - prevent duplicate appends
                            if (state.currentPage != page) {
                                Log.d(TAG, "Discarding duplicate page response: expected page=$page, current page=${state.currentPage}")
                                return@update state
                            }

                            // Deduplicate by mediaFileId to prevent LazyColumn key conflicts
                            val existingIds = state.mediaList.map { it.mediaFileId }.toSet()
                            val newItems = result.data.filter { it.mediaFileId !in existingIds }
                            val newList = state.mediaList + newItems

                            if (newItems.size != result.data.size) {
                                Log.w(TAG, "Filtered ${result.data.size - newItems.size} duplicate items from page $page")
                            }

                            val totalCount = if (result.totalCount > 0) result.totalCount else state.totalCount
                            val hasMorePages = if (totalCount > 0) {
                                newList.size < totalCount
                            } else {
                                // Fallback when server does not provide Count header
                                result.data.size >= PAGE_SIZE
                            }

                            Log.d(TAG, "Loaded ${newList.size} of $totalCount total items, hasMorePages=$hasMorePages")

                            state.copy(
                                mediaList = newList,
                                isLoading = false,
                                error = null,
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun retryLoading() {
        _uiState.update { it.copy(error = null, currentPage = 0, hasMorePages = true) }
        loadMedia(resetList = true)
    }

    fun playMedia(media: Media, resumePosition: Long = 0, onNavigateToPlayer: (url: String, updatePositionUrl: String, mediaId: String, resumePosition: Long) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val remainingEpisodes = playbackCoordinator.getRemainingEpisodes(media, _uiState.value.mediaList)
                Log.d(TAG, "Media is ${if (media.number.isNullOrBlank()) "movie" else "series episode"}, queueing ${remainingEpisodes.size} remaining episodes")

                when (val result = playbackCoordinator.play(media, resumePosition, remainingEpisodes)) {
                    is PlaybackResult.PlayLocally -> {
                        // Set up episode queue for auto-play
                        episodeQueueManager.setQueue(media, remainingEpisodes)

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

    fun toggleFavorite(media: Media) {
        viewModelScope.launch {
            val success = mediaRepository.toggleFavorite(media.mediaFileId, media.favorite)
            if (success) {
                // Update the media item in the list with the new favorite state
                _uiState.update { state ->
                    val updatedList = state.mediaList.map { item ->
                        if (item.mediaFileId == media.mediaFileId) {
                            item.copy(favorite = !item.favorite)
                        } else {
                            item
                        }
                    }
                    state.copy(mediaList = updatedList)
                }
            } else {
                Log.e(TAG, "Failed to toggle favorite for ${media.mediaFileId}")
            }
        }
    }

    fun removeFromHistory(media: Media) {
        viewModelScope.launch {
            val success = mediaRepository.removeFromHistory(media.mediaFileId)
            if (success) {
                // Remove the media item from the list and continue-watching rail
                _uiState.update { state ->
                    val updatedList = state.mediaList.filter { it.mediaFileId != media.mediaFileId }
                    val updatedContinueWatching = state.continueWatching.filter { it.mediaFileId != media.mediaFileId }
                    state.copy(
                        mediaList = updatedList,
                        continueWatching = updatedContinueWatching
                    )
                }
            } else {
                Log.e(TAG, "Failed to remove from history: ${media.mediaFileId}")
            }
        }
    }

    fun loadMediaDetails(media: Media, onLoaded: (Media?) -> Unit) {
        if (!media.plot.isNullOrBlank() || !media.actors.isNullOrBlank()) {
            onLoaded(media)
            return
        }

        mediaDetailsCache[media.mediaFileId]?.let {
            onLoaded(it)
            return
        }

        viewModelScope.launch {
            when (val result = mediaRepository.getMediaDetails(media.mediaFileId)) {
                is Result.Success -> {
                    mediaDetailsCache[media.mediaFileId] = result.data
                    onLoaded(result.data)
                }
                is Result.Error -> {
                    Log.w(TAG, "Failed to load details for ${media.mediaFileId}", result.exception)
                    onLoaded(null)
                }
                is Result.Loading -> onLoaded(null)
            }
        }
    }
}
