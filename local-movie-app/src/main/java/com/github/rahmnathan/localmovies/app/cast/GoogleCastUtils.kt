package com.github.rahmnathan.localmovies.app.cast

import android.util.Log
import androidx.core.net.toUri
import com.github.rahmnathan.localmovies.app.data.repository.MediaRepository
import com.github.rahmnathan.localmovies.app.data.repository.Result
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.images.WebImage
import com.google.common.net.MediaType
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCastUtils @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val castContext: CastContext?
) {

    /**
     * Check if there's an active Cast session
     */
    fun isCastSessionActive(): Boolean {
        return try {
            castContext?.sessionManager?.currentCastSession != null
        } catch (e: Exception) {
            Log.e("GoogleCastUtils", "Error checking Cast session", e)
            false
        }
    }

    /**
     * Play media on the Cast device if a session is active
     * Queues up all remaining episodes in the list
     * Returns true if successfully sent to Cast, false otherwise
     */
    suspend fun playOnCast(media: Media, remainingEpisodes: List<Media> = emptyList(), resumePosition: Long = 0): Boolean {
        return try {
            val castSession = castContext?.sessionManager?.currentCastSession
            if (castSession == null) {
                Log.w("GoogleCastUtils", "No active Cast session")
                return false
            }

            // Build queue items from current media and remaining episodes
            // Only the first item gets the resume position
            val allEpisodes = listOf(media) + remainingEpisodes
            val queueItems = allEpisodes.mapIndexed { index, episode ->
                if (index == 0) {
                    buildMediaQueueItem(episode, resumePosition)
                } else {
                    buildMediaQueueItem(episode, 0)
                }
            }

            Log.d("GoogleCastUtils", "Loading cast queue with ${queueItems.size} items")

            // Load the queue (or single item if only one)
            if (queueItems.size > 1) {
                // For queue, we need to use a different approach to set start time
                castSession.remoteMediaClient?.queueLoad(
                    queueItems.toTypedArray(),
                    0, // Start at first item
                    MediaStatus.REPEAT_MODE_REPEAT_OFF,
                    resumePosition, // Start time in milliseconds for first item
                    null
                )
            } else {
                // Single item - use regular load
                val signedUrls = when (val result = mediaRepository.getSignedUrls(media.mediaFileId)) {
                    is Result.Success -> result.data
                    is Result.Error -> {
                        Log.e("GoogleCastUtils", "Failed to get signed URLs", result.exception)
                        return false
                    }
                    else -> return false
                }

                val metaData = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
                metaData.putString(MediaMetadata.KEY_TITLE, media.title)
                metaData.putString("media-id", media.mediaFileId)
                signedUrls?.updatePosition?.let { metaData.putString("update-position-url", it) }

                signedUrls?.poster?.takeIf { it.isNotBlank() }?.let { poster ->
                    metaData.addImage(WebImage(poster.toUri()))
                }

                val streamUrl = signedUrls?.stream ?: throw IllegalStateException("Stream URL is required for casting")
                val mediaInfo = MediaInfo.Builder(streamUrl)
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setContentType(MediaType.ANY_VIDEO_TYPE.toString())
                    .setMetadata(metaData)
                    .build()

                val request = MediaLoadRequestData.Builder()
                    .setMediaInfo(mediaInfo)
                    .setAutoplay(true)
                    .setCurrentTime(resumePosition)
                    .build()

                castSession.remoteMediaClient?.load(request)
            }

            Log.d("GoogleCastUtils", "Successfully loaded media on Cast device")
            true
        } catch (e: Exception) {
            Log.e("GoogleCastUtils", "Error playing on Cast", e)
            false
        }
    }

    private fun buildMediaQueueItem(media: Media, resumePosition: Long = 0): MediaQueueItem {
        // Use runBlocking since this is called from non-coroutine context (Google Cast SDK)
        val signedUrls = runBlocking {
            when (val result = mediaRepository.getSignedUrls(media.mediaFileId)) {
                is Result.Success -> result.data
                is Result.Error -> throw result.exception
                else -> throw IllegalStateException("Unexpected result")
            }
        }

        val metaData = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        metaData.putString(MediaMetadata.KEY_TITLE, media.title)
        metaData.putString("media-id", media.mediaFileId)
        signedUrls.updatePosition?.let { metaData.putString("update-position-url", it) }

        // URLs are now absolute, no need to prepend server URL
        signedUrls.poster?.takeIf { it.isNotBlank() }?.let { poster ->
            metaData.addImage(WebImage(poster.toUri()))
        }

        val streamUrl = signedUrls.stream ?: throw IllegalStateException("Stream URL is required for casting")
        val mediaInfo = MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MediaType.ANY_VIDEO_TYPE.toString())
            .setMetadata(metaData)
            .build()

        return MediaQueueItem.Builder(mediaInfo)
            .setAutoplay(true)
            .setPreloadTime(30.0)
            .build()
    }
}