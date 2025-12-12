package com.github.rahmnathan.localmovies.app.ui.cast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rahmnathan.localmovies.app.data.repository.MediaRepository
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CastControllerViewModel @Inject constructor(
    private val castContext: CastContext?,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CastControllerUiState())
    val uiState: StateFlow<CastControllerUiState> = _uiState.asStateFlow()

    private val remoteMediaClient: RemoteMediaClient?
        get() = castContext?.sessionManager?.currentCastSession?.remoteMediaClient

    init {
        // Start monitoring Cast state
        startMonitoring()
    }

    private var lastProgressSaveTime = 0L

    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                updateState()

                // Save progress every 5 seconds when playing
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastProgressSaveTime >= 5000) {
                    saveProgress()
                    lastProgressSaveTime = currentTime
                }

                kotlinx.coroutines.delay(2000) // Update every 2 seconds
            }
        }
    }

    private fun saveProgress() {
        viewModelScope.launch {
            try {
                val client = remoteMediaClient ?: return@launch
                val mediaStatus = client.mediaStatus ?: return@launch

                // Only save when playing
                if (mediaStatus.playerState != MediaStatus.PLAYER_STATE_PLAYING) {
                    return@launch
                }

                val mediaInfo = client.mediaInfo ?: return@launch
                val metadata = mediaInfo.metadata ?: return@launch

                // Get the update position URL from metadata
                val updatePositionUrl = metadata.getString("update-position-url")
                if (updatePositionUrl.isNullOrBlank()) {
                    android.util.Log.w("CastControllerViewModel", "No update-position-url in metadata")
                    return@launch
                }

                val currentPosition = client.approximateStreamPosition

                android.util.Log.d("CastControllerViewModel", "Saving cast progress: position=$currentPosition, url=$updatePositionUrl")

                mediaRepository.saveProgress(
                    updatePositionUrl = updatePositionUrl,
                    position = currentPosition
                )
            } catch (e: Exception) {
                android.util.Log.e("CastControllerViewModel", "Error saving cast progress", e)
            }
        }
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
        viewModelScope.launch {
            try {
                val client = remoteMediaClient
                if (client == null) {
                    _uiState.update { it.copy(isConnected = false) }
                    return@launch
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
                        val queueItems = status.queueItems
                        if (queueItems != null && queueItems.isNotEmpty()) {
                            // Find current item index
                            val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }
                            // Check if there's a next item
                            currentIndex >= 0 && currentIndex < queueItems.size - 1
                        } else {
                            false
                        }
                    } ?: false

                    android.util.Log.d("CastControllerViewModel", "Cast session active - title: $title, isPlaying: ${mediaStatus?.playerState == MediaStatus.PLAYER_STATE_PLAYING}, hasNext: $hasNextQueueItem")

                    _uiState.update { state ->
                        state.copy(
                            isConnected = true,
                            title = title,
                            imageUrl = imageUrl,
                            duration = mediaInfo.streamDuration,
                            currentPosition = client.approximateStreamPosition,
                            isPlaying = mediaStatus?.playerState == MediaStatus.PLAYER_STATE_PLAYING,
                            hasNextQueueItem = hasNextQueueItem
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
}
