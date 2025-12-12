package com.github.rahmnathan.localmovies.app.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rahmnathan.localmovies.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

data class PlayerUiState(
    val videoUrl: String = "",
    val updatePositionUrl: String = "",
    val currentPosition: Long = 0,
    val isPlaying: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressTrackingJob: Job? = null

    init {
        val encodedVideoUrl = savedStateHandle.get<String>("url") ?: ""
        val encodedUpdatePositionUrl = savedStateHandle.get<String>("updatePositionUrl") ?: ""

        // Decode URLs from navigation parameters
        val videoUrl = URLDecoder.decode(encodedVideoUrl, StandardCharsets.UTF_8.toString())
        val updatePositionUrl = URLDecoder.decode(encodedUpdatePositionUrl, StandardCharsets.UTF_8.toString())

        _uiState.update {
            it.copy(
                videoUrl = videoUrl,
                updatePositionUrl = updatePositionUrl
            )
        }
    }

    fun onPlaybackStarted() {
        _uiState.update { it.copy(isPlaying = true) }
        startProgressTracking()
    }

    fun onPlaybackPaused() {
        _uiState.update { it.copy(isPlaying = false) }
        stopProgressTracking()
    }

    fun onPositionChanged(position: Long) {
        _uiState.update { it.copy(currentPosition = position) }
    }

    /**
     * CRITICAL FIX: This replaces the infinite while(true) loop from PlayerActivity.
     * The coroutine is tied to viewModelScope and will be automatically cancelled
     * when the ViewModel is cleared (activity destroyed).
     */
    private fun startProgressTracking() {
        // Cancel existing job if any
        stopProgressTracking()

        // Start new job tied to viewModelScope (auto-canceled when ViewModel is cleared)
        progressTrackingJob = viewModelScope.launch {
            while (true) {
                delay(5000) // Every 5 seconds

                val currentState = _uiState.value
                if (currentState.isPlaying && currentState.updatePositionUrl.isNotBlank()) {
                    try {
                        mediaRepository.saveProgress(
                            updatePositionUrl = currentState.updatePositionUrl,
                            position = currentState.currentPosition
                        )
                    } catch (e: Exception) {
                        // Log but don't crash - position tracking is best effort
                        _uiState.update { it.copy(error = "Failed to save progress") }
                    }
                }
            }
        }
    }

    private fun stopProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracking() // Ensures cleanup
    }
}
