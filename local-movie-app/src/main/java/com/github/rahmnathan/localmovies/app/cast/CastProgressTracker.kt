package com.github.rahmnathan.localmovies.app.cast

import android.util.Log
import com.github.rahmnathan.localmovies.app.data.repository.MediaRepository
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton service that acts as a fallback for cast progress tracking.
 *
 * Primary progress tracking is handled by the custom Cast receiver (receiver.js),
 * which reports directly to the server every 10 seconds during playback.
 *
 * This tracker serves as a backup, saving progress only when:
 * - Playback transitions from PLAYING to IDLE/PAUSED (session end)
 * - The Cast session ends unexpectedly
 *
 * This runs independently of UI lifecycle to ensure progress is captured even if
 * the app is backgrounded or the receiver fails to report.
 */
@Singleton
class CastProgressTracker @Inject constructor(
    private val castContext: CastContext?,
    private val mediaRepository: MediaRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastMediaId: String? = null
    private var lastPlayerState: Int? = null
    private var isInitialized = false

    private val mediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            Log.d(TAG, "onStatusUpdated called")
            checkForMediaChange()
            checkForPlaybackEnded()
        }

        override fun onQueueStatusUpdated() {
            Log.d(TAG, "onQueueStatusUpdated called - queue item may have changed")
            checkForMediaChange()
        }

        override fun onMetadataUpdated() {
            Log.d(TAG, "onMetadataUpdated called")
        }
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            Log.d(TAG, "Cast session started: $sessionId")
            registerMediaClientListeners(session)
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            Log.d(TAG, "Cast session resumed, wasSuspended: $wasSuspended")
            registerMediaClientListeners(session)
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            Log.d(TAG, "Cast session ended, error: $error")
            unregisterMediaClientListeners(session)
            lastMediaId = null
            lastPlayerState = null
        }

        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {
            Log.d(TAG, "Cast session suspended, reason: $reason")
        }
    }

    /**
     * Initialize the tracker. Should be called once when the app starts.
     */
    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return
        }

        val sessionManager = castContext?.sessionManager ?: run {
            Log.w(TAG, "CastContext not available, progress tracking disabled")
            return
        }

        sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)

        // If there's already an active session, register listeners
        sessionManager.currentCastSession?.let { session ->
            Log.d(TAG, "Active cast session found on init, registering listeners")
            registerMediaClientListeners(session)
        }

        isInitialized = true
        Log.i(TAG, "CastProgressTracker initialized")
    }

    private fun registerMediaClientListeners(session: CastSession) {
        val client = session.remoteMediaClient ?: run {
            Log.w(TAG, "No RemoteMediaClient available")
            return
        }

        client.registerCallback(mediaClientCallback)
        Log.d(TAG, "Registered media client callback (fallback progress tracking)")
    }

    private fun unregisterMediaClientListeners(session: CastSession) {
        session.remoteMediaClient?.let { client ->
            client.unregisterCallback(mediaClientCallback)
            Log.d(TAG, "Unregistered media client callback")
        }
    }

    private fun checkForMediaChange() {
        val client = getRemoteMediaClient() ?: return
        val mediaInfo = client.mediaInfo ?: return
        val metadata = mediaInfo.metadata ?: return

        val currentMediaId = metadata.getString("media-id")
        val title = metadata.getString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE)

        if (currentMediaId != lastMediaId) {
            Log.i(TAG, "New media detected: $currentMediaId (title: $title)")
            lastMediaId = currentMediaId
            // Note: Progress tracking for new items is handled by the Cast receiver
        }
    }

    /**
     * Detect when playback transitions from PLAYING to IDLE/PAUSED and save final progress.
     * This ensures history is recorded even when the user stops casting or the media ends.
     */
    private fun checkForPlaybackEnded() {
        val client = getRemoteMediaClient() ?: return
        val mediaStatus = client.mediaStatus ?: return
        val currentState = mediaStatus.playerState

        val previousState = lastPlayerState
        lastPlayerState = currentState

        // If we transitioned from PLAYING to IDLE or PAUSED, save the final progress
        if (previousState == MediaStatus.PLAYER_STATE_PLAYING &&
            (currentState == MediaStatus.PLAYER_STATE_IDLE || currentState == MediaStatus.PLAYER_STATE_PAUSED)) {
            Log.i(TAG, "Playback ended (state: $previousState -> $currentState), saving final progress")
            saveFinalProgress(client.approximateStreamPosition)
        }
    }

    /**
     * Save final progress when playback ends, regardless of player state.
     * This is the fallback mechanism - primary tracking is done by the Cast receiver.
     */
    private fun saveFinalProgress(positionMs: Long) {
        scope.launch {
            try {
                val client = getRemoteMediaClient() ?: return@launch
                saveProgressInternal(client, positionMs)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving final cast progress", e)
            }
        }
    }

    private suspend fun saveProgressInternal(client: RemoteMediaClient, positionMs: Long) {
        val mediaInfo = client.mediaInfo ?: return
        val metadata = mediaInfo.metadata ?: return

        val updatePositionUrl = metadata.getString("update-position-url")
        if (updatePositionUrl.isNullOrBlank()) {
            Log.w(TAG, "No update-position-url in metadata")
            return
        }

        val mediaId = metadata.getString("media-id")
        val duration = mediaInfo.streamDuration.takeIf { it > 0 }
        Log.d(TAG, "Saving progress: mediaId=$mediaId, position=$positionMs, duration=$duration")

        mediaRepository.saveProgress(
            updatePositionUrl = updatePositionUrl,
            position = positionMs,
            duration = duration
        )
    }

    private fun getRemoteMediaClient(): RemoteMediaClient? {
        return castContext?.sessionManager?.currentCastSession?.remoteMediaClient
    }

    companion object {
        private const val TAG = "CastProgressTracker"
    }
}
