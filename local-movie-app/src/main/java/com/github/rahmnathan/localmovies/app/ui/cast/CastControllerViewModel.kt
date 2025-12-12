package com.github.rahmnathan.localmovies.app.ui.cast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val castContext: CastContext?
) : ViewModel() {

    private val _uiState = MutableStateFlow(CastControllerUiState())
    val uiState: StateFlow<CastControllerUiState> = _uiState.asStateFlow()

    private val remoteMediaClient: RemoteMediaClient?
        get() = castContext?.sessionManager?.currentCastSession?.remoteMediaClient

    init {
        // Start monitoring Cast state
        startMonitoring()
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                updateState()
                kotlinx.coroutines.delay(1000) // Update every second
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

                    android.util.Log.d("CastControllerViewModel", "Cast session active - title: $title, isPlaying: ${mediaStatus?.playerState == MediaStatus.PLAYER_STATE_PLAYING}")

                    _uiState.update { state ->
                        state.copy(
                            isConnected = true,
                            title = title,
                            imageUrl = imageUrl,
                            duration = mediaInfo.streamDuration,
                            currentPosition = client.approximateStreamPosition,
                            isPlaying = mediaStatus?.playerState == MediaStatus.PLAYER_STATE_PLAYING
                        )
                    }
                } else {
                    android.util.Log.d("CastControllerViewModel", "Cast session active but no media info")
                    _uiState.update { it.copy(isConnected = true, title = "", imageUrl = "") }
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
}
