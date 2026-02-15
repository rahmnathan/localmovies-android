package com.github.rahmnathan.localmovies.app.ui.player

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.github.rahmnathan.localmovies.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

data class NextEpisodeInfo(
    val streamUrl: String,
    val updatePositionUrl: String,
    val mediaId: String,
    val title: String,
    val episodeNumber: String?
)

data class PlayerUiState(
    val videoUrl: String = "",
    val updatePositionUrl: String = "",
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentMediaId: String = "",
    val resumePosition: Long = 0,
    val nextEpisode: NextEpisodeInfo? = null,
    val showNextEpisodeCountdown: Boolean = false,
    val countdownSeconds: Int = 10
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val application: Application,
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val episodeQueueManager: EpisodeQueueManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressTrackingJob: Job? = null
    private var countdownJob: Job? = null

    // Callback for navigating to next episode
    private var onPlayNextEpisode: ((NextEpisodeInfo) -> Unit)? = null

    // ExoPlayer instance
    val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        // Add player listener for state changes
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    Player.STATE_READY -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                duration = player.duration.coerceAtLeast(0)
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        onPlaybackEnded()
                    }
                    Player.STATE_IDLE -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracking()
                } else {
                    stopProgressTracking()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("PlayerViewModel", "Playback error: ${error.message}", error)
                _uiState.update {
                    it.copy(
                        error = "Playback error: ${error.errorCodeName}",
                        isLoading = false
                    )
                }
            }
        })

        // Set play when ready
        playWhenReady = true
    }

    init {
        val encodedVideoUrl = savedStateHandle.get<String>("url") ?: ""
        val encodedUpdatePositionUrl = savedStateHandle.get<String>("updatePositionUrl") ?: ""
        val encodedMediaId = savedStateHandle.get<String>("mediaId") ?: ""
        val resumePosition = savedStateHandle.get<Long>("resumePosition") ?: 0L

        // Decode URLs from navigation parameters
        val videoUrl = URLDecoder.decode(encodedVideoUrl, StandardCharsets.UTF_8.toString())
        val updatePositionUrl = URLDecoder.decode(encodedUpdatePositionUrl, StandardCharsets.UTF_8.toString())
        val mediaId = URLDecoder.decode(encodedMediaId, StandardCharsets.UTF_8.toString())

        android.util.Log.d("PlayerViewModel", "Initializing player with video URL: $videoUrl")
        android.util.Log.d("PlayerViewModel", "Resume position: $resumePosition ms")

        _uiState.update {
            it.copy(
                videoUrl = videoUrl,
                updatePositionUrl = updatePositionUrl,
                currentMediaId = mediaId,
                resumePosition = resumePosition,
                isLoading = true
            )
        }

        // Prepare player with media
        if (videoUrl.isNotBlank()) {
            val mediaItem = MediaItem.fromUri(videoUrl)
            player.setMediaItem(mediaItem)
            player.prepare()

            // Seek to resume position if provided
            if (resumePosition > 0) {
                player.seekTo(resumePosition)
            }
        }

        // Check if there's a next episode in the queue
        loadNextEpisodeInfo()
    }

    private fun loadNextEpisodeInfo() {
        val nextMedia = episodeQueueManager.getNextEpisode()
        if (nextMedia != null) {
            _uiState.update {
                it.copy(
                    nextEpisode = NextEpisodeInfo(
                        streamUrl = "", // Will be fetched when needed
                        updatePositionUrl = "",
                        mediaId = nextMedia.mediaFileId,
                        title = nextMedia.title,
                        episodeNumber = nextMedia.number
                    )
                )
            }
        }
    }

    fun onPlaybackPaused() {
        player.pause()
    }

    private fun onPlaybackEnded() {
        player.pause()

        // If there's a next episode, start the countdown
        val nextEpisode = _uiState.value.nextEpisode
        if (nextEpisode != null) {
            startNextEpisodeCountdown()
        }
    }

    private fun startNextEpisodeCountdown() {
        countdownJob?.cancel()
        _uiState.update { it.copy(showNextEpisodeCountdown = true, countdownSeconds = 10) }

        countdownJob = viewModelScope.launch {
            for (seconds in 10 downTo 1) {
                _uiState.update { it.copy(countdownSeconds = seconds) }
                delay(1000)
            }
            // Countdown finished - auto-play next episode
            playNextEpisode()
        }
    }

    fun cancelNextEpisodeCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _uiState.update { it.copy(showNextEpisodeCountdown = false) }
    }

    fun playNextEpisode() {
        countdownJob?.cancel()
        countdownJob = null
        _uiState.update { it.copy(showNextEpisodeCountdown = false, isLoading = true) }

        viewModelScope.launch {
            val nextPlayback = episodeQueueManager.advanceToNextEpisode()
            if (nextPlayback != null) {
                val nextEpisodeInfo = NextEpisodeInfo(
                    streamUrl = nextPlayback.streamUrl,
                    updatePositionUrl = nextPlayback.updatePositionUrl,
                    mediaId = nextPlayback.mediaId,
                    title = nextPlayback.title,
                    episodeNumber = nextPlayback.episodeNumber
                )
                onPlayNextEpisode?.invoke(nextEpisodeInfo)
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load next episode") }
            }
        }
    }

    fun setNextEpisodeCallback(callback: (NextEpisodeInfo) -> Unit) {
        onPlayNextEpisode = callback
    }

    fun setNextEpisode(nextEpisode: NextEpisodeInfo?) {
        _uiState.update { it.copy(nextEpisode = nextEpisode) }
    }

    /**
     * Progress tracking that updates both UI state and saves to backend.
     * The coroutine is tied to viewModelScope and will be automatically cancelled
     * when the ViewModel is cleared (activity destroyed).
     */
    private fun startProgressTracking() {
        // Cancel existing job if any
        stopProgressTracking()

        // Start new job tied to viewModelScope (auto-canceled when ViewModel is cleared)
        progressTrackingJob = viewModelScope.launch {
            while (true) {
                delay(1000) // Update UI every second

                // Update current position from player
                val currentPosition = player.currentPosition.coerceAtLeast(0)
                _uiState.update { it.copy(currentPosition = currentPosition) }

                // Save to backend every 5 seconds
                if (currentPosition % 5000 < 1000) {
                    val currentState = _uiState.value
                    if (currentState.updatePositionUrl.isNotBlank()) {
                        try {
                            val duration = player.duration.takeIf { it > 0 }
                            mediaRepository.saveProgress(
                                updatePositionUrl = currentState.updatePositionUrl,
                                position = currentPosition,
                                duration = duration
                            )
                        } catch (e: Exception) {
                            // Log but don't crash - position tracking is best effort
                            android.util.Log.w("PlayerViewModel", "Failed to save progress", e)
                        }
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
        stopProgressTracking()
        countdownJob?.cancel()
        player.release() // Release ExoPlayer resources
    }
}
