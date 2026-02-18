package com.github.rahmnathan.localmovies.app.ui.cast

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class CastControllerViewModel @Inject constructor(
    private val castContext: CastContext?,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {
    // Note: Progress tracking is handled by CastProgressTracker singleton (as fallback),
    // and by the custom Cast receiver (primary) which reports directly to the server.

    private val _uiState = MutableStateFlow(CastControllerUiState())
    val uiState: StateFlow<CastControllerUiState> = _uiState.asStateFlow()

    private var monitoringJob: Job? = null

    private val remoteMediaClient: RemoteMediaClient?
        get() = castContext?.sessionManager?.currentCastSession?.remoteMediaClient

    init {
        // Start monitoring Cast state for UI updates
        startMonitoring()
        // Load saved subtitle offset and send to receiver
        loadAndSendSubtitleOffset()
    }

    companion object {
        private const val TAG = "CastControllerViewModel"
        private const val CAST_NAMESPACE = "urn:x-cast:com.nathanrahm.localmovies"
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

                Log.d(TAG, "Cast session active - title: $title, isPlaying: ${mediaStatus?.playerState == MediaStatus.PLAYER_STATE_PLAYING}, hasNext: $hasNextQueueItem, subtitlesAvailable: $subtitlesAvailable, subtitlesEnabled: $subtitlesEnabled")

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
                Log.d(TAG, "Cast session active but no media info")
                _uiState.update { it.copy(isConnected = true, title = "", imageUrl = "", hasNextQueueItem = false) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating state", e)
            _uiState.update { it.copy(isConnected = false) }
        }
    }

    fun play() {
        viewModelScope.launch {
            try {
                remoteMediaClient?.play()
                _uiState.update { it.copy(isPlaying = true) }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing", e)
            }
        }
    }

    fun pause() {
        viewModelScope.launch {
            try {
                remoteMediaClient?.pause()
                _uiState.update { it.copy(isPlaying = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing", e)
            }
        }
    }

    fun seekTo(position: Long) {
        viewModelScope.launch {
            try {
                remoteMediaClient?.seek(position)
                _uiState.update { it.copy(currentPosition = position) }
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking", e)
            }
        }
    }

    fun stopCasting() {
        viewModelScope.launch {
            try {
                remoteMediaClient?.stop()
                castContext?.sessionManager?.endCurrentSession(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping cast", e)
            }
        }
    }

    fun skipToNext() {
        viewModelScope.launch {
            try {
                val client = remoteMediaClient
                if (client == null) {
                    Log.w(TAG, "Cannot skip: no remote media client")
                    return@launch
                }

                Log.d(TAG, "Skipping to next queue item")
                client.queueNext(null)
            } catch (e: Exception) {
                Log.e(TAG, "Error skipping to next", e)
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

                Log.d(TAG, "Toggling subtitles: enabled=${!currentState}, trackId=${subtitleTrack.id}")

                client.setActiveMediaTracks(newTrackIds)
                _uiState.update { it.copy(subtitlesEnabled = !currentState) }

                // If enabling subtitles, send the current offset to receiver
                if (!currentState) {
                    sendSubtitleOffsetToReceiver(_uiState.value.subtitleOffsetSeconds)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling subtitles", e)
            }
        }
    }

    /**
     * Adjust subtitle offset by the given delta and send to receiver.
     */
    fun adjustSubtitleOffset(deltaSeconds: Float) {
        viewModelScope.launch {
            val newOffset = _uiState.value.subtitleOffsetSeconds + deltaSeconds
            _uiState.update { it.copy(subtitleOffsetSeconds = newOffset) }
            sendSubtitleOffsetToReceiver(newOffset)
            // Persist for future sessions
            userPreferencesDataStore.saveSubtitleOffset(newOffset)
            Log.d(TAG, "Subtitle offset adjusted to: $newOffset")
        }
    }

    /**
     * Reset subtitle offset to zero.
     */
    fun resetSubtitleOffset() {
        viewModelScope.launch {
            _uiState.update { it.copy(subtitleOffsetSeconds = 0f) }
            sendSubtitleOffsetToReceiver(0f)
            userPreferencesDataStore.saveSubtitleOffset(0f)
            Log.d(TAG, "Subtitle offset reset to 0")
        }
    }

    /**
     * Load saved subtitle offset and send to receiver on session start.
     */
    private fun loadAndSendSubtitleOffset() {
        viewModelScope.launch {
            try {
                val savedOffset = userPreferencesDataStore.subtitleOffsetFlow.first()
                _uiState.update { it.copy(subtitleOffsetSeconds = savedOffset) }
                if (savedOffset != 0f) {
                    // Wait a bit for Cast session to be ready
                    delay(1000)
                    sendSubtitleOffsetToReceiver(savedOffset)
                }
                Log.d(TAG, "Loaded saved subtitle offset: $savedOffset")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading subtitle offset", e)
            }
        }
    }

    /**
     * Send subtitle offset to the Cast receiver via custom message channel.
     */
    private fun sendSubtitleOffsetToReceiver(offsetSeconds: Float) {
        try {
            val session = castContext?.sessionManager?.currentCastSession
            if (session == null) {
                Log.w(TAG, "No Cast session available to send subtitle offset")
                return
            }

            val message = JSONObject().apply {
                put("type", "SET_SUBTITLE_OFFSET")
                put("offsetSeconds", offsetSeconds.toDouble())
            }.toString()

            session.sendMessage(CAST_NAMESPACE, message)
                .setResultCallback { status ->
                    if (status.isSuccess) {
                        Log.d(TAG, "Subtitle offset sent successfully: $offsetSeconds")
                    } else {
                        Log.w(TAG, "Failed to send subtitle offset: ${status.statusCode}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending subtitle offset to receiver", e)
        }
    }
}
