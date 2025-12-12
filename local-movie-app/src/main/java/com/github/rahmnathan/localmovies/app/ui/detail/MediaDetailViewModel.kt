package com.github.rahmnathan.localmovies.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rahmnathan.localmovies.app.media.data.Media
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaDetailUiState(
    val media: Media? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Get media ID from navigation argument
    private val mediaId: String = savedStateHandle.get<String>("mediaId") ?: ""

    private val _uiState = MutableStateFlow(MediaDetailUiState())
    val uiState: StateFlow<MediaDetailUiState> = _uiState.asStateFlow()

    init {
        if (mediaId.isNotBlank()) {
            loadMediaDetails()
        } else {
            _uiState.update {
                it.copy(isLoading = false, error = "Invalid media ID")
            }
        }
    }

    private fun loadMediaDetails() {
        viewModelScope.launch {
            // For now, we'll need to search through cached media to find the one with this ID
            // In a real implementation, you might have a getMediaById API endpoint
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setMedia(media: Media) {
        _uiState.update { it.copy(media = media, isLoading = false) }
    }
}
