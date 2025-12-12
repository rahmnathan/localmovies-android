package com.github.rahmnathan.localmovies.app.cast.control

import androidx.core.net.toUri
import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import com.github.rahmnathan.localmovies.app.data.repository.MediaRepository
import com.github.rahmnathan.localmovies.app.data.repository.Result
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.common.images.WebImage
import com.google.common.net.MediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCastUtils @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val castContext: CastContext?
) {

    fun assembleMediaQueue(media: List<Media>): List<MediaQueueItem> {
        return media.map { buildMediaQueueItem(it) }
    }

    /**
     * Check if there's an active Cast session
     */
    fun isCastSessionActive(): Boolean {
        return try {
            castContext?.sessionManager?.currentCastSession != null
        } catch (e: Exception) {
            android.util.Log.e("GoogleCastUtils", "Error checking Cast session", e)
            false
        }
    }

    /**
     * Play media on the Cast device if a session is active
     * Returns true if successfully sent to Cast, false otherwise
     */
    suspend fun playOnCast(media: Media): Boolean {
        return try {
            val castSession = castContext?.sessionManager?.currentCastSession
            if (castSession == null) {
                android.util.Log.w("GoogleCastUtils", "No active Cast session")
                return false
            }

            val signedUrls = when (val result = mediaRepository.getSignedUrls(media.mediaFileId)) {
                is Result.Success -> result.data
                is Result.Error -> {
                    android.util.Log.e("GoogleCastUtils", "Failed to get signed URLs", result.exception)
                    return false
                }
                else -> return false
            }

            val metaData = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
            metaData.putString(MediaMetadata.KEY_TITLE, media.title)
            metaData.putString("media-id", media.mediaFileId)
            metaData.putString("update-position-url", signedUrls.updatePosition)

            // Add poster image if available
            if (signedUrls.poster.isNotBlank()) {
                metaData.addImage(WebImage(signedUrls.poster.toUri()))
            }

            val mediaInfo = MediaInfo.Builder(signedUrls.stream)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(MediaType.ANY_VIDEO_TYPE.toString())
                .setMetadata(metaData)
                .build()

            val request = MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build()

            castSession.remoteMediaClient?.load(request)
            android.util.Log.d("GoogleCastUtils", "Successfully loaded media on Cast device")
            true
        } catch (e: Exception) {
            android.util.Log.e("GoogleCastUtils", "Error playing on Cast", e)
            false
        }
    }

    private fun buildMediaQueueItem(media: Media): MediaQueueItem {
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
        metaData.putString("update-position-url", signedUrls.updatePosition)

        // URLs are now absolute, no need to prepend server URL
        if (signedUrls.poster.isNotBlank()) {
            metaData.addImage(WebImage(signedUrls.poster.toUri()))
        }

        val mediaInfo = MediaInfo.Builder(signedUrls.stream)
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
