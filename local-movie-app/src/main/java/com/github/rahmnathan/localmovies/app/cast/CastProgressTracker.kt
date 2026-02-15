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
 * Singleton service that tracks cast playback progress and reports it to the server.
 * Uses RemoteMediaClient.Callback for status updates (pushed from Cast device) and
 * ProgressListener for periodic position updates.
 *
 * This runs independently of UI lifecycle to ensure all queued episodes are tracked.
 */
@Singleton
class CastProgressTracker @Inject constructor(
    private val castContext: CastContext?,
    private val mediaRepository: MediaRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastProgressSaveTime = 0L
    private var lastMediaId: String? = null
    private var isInitialized = false

    private val progressListener = RemoteMediaClient.ProgressListener { progressMs, _ ->
        val currentTime = System.currentTimeMillis()
        // Save progress every 5 seconds
        if (currentTime - lastProgressSaveTime >= SAVE_INTERVAL_MS) {
            saveCurrentProgress(progressMs)
            lastProgressSaveTime = currentTime
        }
    }

    private val mediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            Log.d(TAG, "onStatusUpdated called")
            checkForMediaChange()
        }

        override fun onQueueStatusUpdated() {
            Log.d(TAG, "onQueueStatusUpdated called - queue item may have changed")
            // Reset save time to ensure we capture progress quickly for new item
            lastProgressSaveTime = 0L
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
        client.addProgressListener(progressListener, PROGRESS_LISTENER_INTERVAL_MS)
        Log.d(TAG, "Registered media client listeners")
    }

    private fun unregisterMediaClientListeners(session: CastSession) {
        session.remoteMediaClient?.let { client ->
            client.unregisterCallback(mediaClientCallback)
            client.removeProgressListener(progressListener)
            Log.d(TAG, "Unregistered media client listeners")
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
            // Save progress immediately for the new item
            saveCurrentProgress(client.approximateStreamPosition)
        }
    }

    private fun saveCurrentProgress(positionMs: Long) {
        scope.launch {
            try {
                val client = getRemoteMediaClient() ?: return@launch
                val mediaStatus = client.mediaStatus ?: return@launch

                // Only save when playing
                if (mediaStatus.playerState != MediaStatus.PLAYER_STATE_PLAYING) {
                    return@launch
                }

                val mediaInfo = client.mediaInfo ?: return@launch
                val metadata = mediaInfo.metadata ?: return@launch

                val updatePositionUrl = metadata.getString("update-position-url")
                if (updatePositionUrl.isNullOrBlank()) {
                    Log.w(TAG, "No update-position-url in metadata")
                    return@launch
                }

                val mediaId = metadata.getString("media-id")
                Log.d(TAG, "Saving progress: mediaId=$mediaId, position=$positionMs")

                mediaRepository.saveProgress(
                    updatePositionUrl = updatePositionUrl,
                    position = positionMs
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving cast progress", e)
            }
        }
    }

    private fun getRemoteMediaClient(): RemoteMediaClient? {
        return castContext?.sessionManager?.currentCastSession?.remoteMediaClient
    }

    companion object {
        private const val TAG = "CastProgressTracker"
        private const val PROGRESS_LISTENER_INTERVAL_MS = 1000L
        private const val SAVE_INTERVAL_MS = 5000L
    }
}
