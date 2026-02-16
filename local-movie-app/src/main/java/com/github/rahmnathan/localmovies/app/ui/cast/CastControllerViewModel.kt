package com.github.rahmnathan.localmovies.app.ui.cast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CastControllerViewModel @Inject constructor(
    private val castContext: CastContext?
) : ViewModel() {
    // Note: Progress tracking is handled by CastProgressTracker singleton,
    // which runs independently of UI lifecycle to track all queued episodes.

    private val _uiState = MutableStateFlow(CastControllerUiState())
    val uiState: StateFlow<CastControllerUiState> = _uiState.asStateFlow()

    private var monitoringJob: Job? = null

    private val remoteMediaClient: RemoteMediaClient?
        get() = castContext?.sessionManager?.currentCastSession?.remoteMediaClient

    init {
        // Start monitoring Cast state for UI updates
        startMonitoring()
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = viewModelScope.launch {
            while (isActive) {
                updateState()
                delay(2000) // Update UI every 2 seconds
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        monitoringJob?.cancel()
        monitoringJob = null
    }

    fun updatePosition() {
        val client = remoteMediaClient ?: return
        val mediaStatus = client.mediaStatus ?: return

        _uiState.update { state ->
            state.copy(
                currentPosition = client.approximateStreamPosition,
                isPlaying = mediaStatus.playerState == MediaStatus.PLAYER_STATE_PLAYING
            )
        }
    }

    private fun updateState() {
        try {
            val client = remoteMediaClient
            if (client == null) {
                _uiState.update { it.copy(isConnected = false) }
                return
            }

            val mediaInfo = client.mediaInfo
            val mediaStatus = client.mediaStatus

            if (mediaInfo != null) {
                val metadata = mediaInfo.metadata
                val title = metadata?.getString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE) ?: ""
                val imageUrl = metadata?.images?.firstOrNull()?.url?.toString() ?: ""

                // Check if there's a next item in the queue
                val hasNextQueueItem = mediaStatus?.let { status ->
                    val currentItemId = status.currentItemId
                    val queueItems = status.queueItems.orEmpty()
                    if (queueItems.isNotEmpty()) {
                        // Find current item index
                        val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }
                        // Check if there's a next item
                        currentIndex >= 0 && currentIndex < queueItems.size - 1
                    } else {
                        false
                    }
                } ?: false

                // Check if subtitles are available and enabled
                val mediaTracks = mediaInfo.mediaTracks
                val subtitleTrack = mediaTracks?.find { it.type == com.google.android.gms.cast.MediaTrack.TYPE_TEXT }
                val subtitlesAvailable = subtitleTrack != null
                val activeTrackIds = mediaStatus?.activeTrackIds ?: longArrayOf()
                val subtitlesEnabled = subtitleTrack != null && activeTrackIds.contains(subtitleTrack.id)

                android.util.Log.d("CastControllerViewModel", "Cast session active - title: $title, isPlaying: ${mediaStatus?.playerState == MediaStatus.PLAYER_STATE_PLAYING}, hasNext: $hasNextQueueItem, subtitlesAvailable: $subtitlesAvailable, subtitlesEnabled: $subtitlesEnabled")

                _uiState.update { state ->
                    state.copy(
                        isConnected = true,
                        title = title,
                        imageUrl = imageUrl,
                        duration = mediaInfo.streamDuration,
                        currentPosition = client.approximateStreamPosition,
                        isPlaying = mediaStatus?.playerState == MediaStatus.PLAYER_STATE_PLAYING,
                        hasNextQueueItem = hasNextQueueItem,
                        subtitlesAvailable = subtitlesAvailable,
                        subtitlesEnabled = subtitlesEnabled
                    )
                }
            } else {
                android.util.Log.d("CastControllerViewModel", "Cast session active but no media info")
                _uiState.update { it.copy(isConnected = true, title = "", imageUrl = "", hasNextQueueItem = false) }
            }
        } catch (e: Exception) {
            android.util.Log.e("CastControllerViewModel", "Error updating state", e)
            _uiState.update { it.copy(isConnected = false) }
        }
    }

    fun play() {
        viewModelScope.launch {
            try {
                remoteMediaClient?.play()
                _uiState.update { it.copy(isPlaying = true) }
            } catch (e: Exception) {
                android.util.Log.e("CastControllerViewModel", "Error playing", e)
            }
        }
    }

    fun pause() {
        viewModelScope.launch {
            try {
                remoteMediaClient?.pause()
                _uiState.update { it.copy(isPlaying = false) }
            } catch (e: Exception) {
                android.util.Log.e("CastControllerViewModel", "Error pausing", e)
            }
        }
    }

    fun seekTo(position: Long) {
        viewModelScope.launch {
            try {
                remoteMediaClient?.seek(position)
                _uiState.update { it.copy(currentPosition = position) }
            } catch (e: Exception) {
                android.util.Log.e("CastControllerViewModel", "Error seeking", e)
            }
        }
    }

    fun stopCasting() {
        viewModelScope.launch {
            try {
                remoteMediaClient?.stop()
                castContext?.sessionManager?.endCurrentSession(true)
            } catch (e: Exception) {
                android.util.Log.e("CastControllerViewModel", "Error stopping cast", e)
            }
        }
    }

    fun skipToNext() {
        viewModelScope.launch {
            try {
                val client = remoteMediaClient
                if (client == null) {
                    android.util.Log.w("CastControllerViewModel", "Cannot skip: no remote media client")
                    return@launch
                }

                android.util.Log.d("CastControllerViewModel", "Skipping to next queue item")
                client.queueNext(null)
            } catch (e: Exception) {
                android.util.Log.e("CastControllerViewModel", "Error skipping to next", e)
            }
        }
    }

    fun toggleSubtitles() {
        viewModelScope.launch {
            try {
                val client = remoteMediaClient ?: return@launch
                val mediaInfo = client.mediaInfo ?: return@launch
                val mediaTracks = mediaInfo.mediaTracks ?: return@launch

                val subtitleTrack = mediaTracks.find { it.type == com.google.android.gms.cast.MediaTrack.TYPE_TEXT }
                    ?: return@launch

                val currentState = _uiState.value.subtitlesEnabled
                val newTrackIds = if (currentState) {
                    // Disable subtitles - set empty track array
                    longArrayOf()
                } else {
                    // Enable subtitles - set subtitle track ID
                    longArrayOf(subtitleTrack.id)
                }

                android.util.Log.d("CastControllerViewModel", "Toggling subtitles: enabled=${!currentState}, trackId=${subtitleTrack.id}")

                client.setActiveMediaTracks(newTrackIds)
                _uiState.update { it.copy(subtitlesEnabled = !currentState) }
            } catch (e: Exception) {
                android.util.Log.e("CastControllerViewModel", "Error toggling subtitles", e)
            }
        }
    }
}
