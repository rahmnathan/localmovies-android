package com.github.rahmnathan.localmovies.app.ui.main

import android.util.Log
import com.github.rahmnathan.localmovies.app.cast.GoogleCastUtils
import com.github.rahmnathan.localmovies.app.data.repository.MediaRepository
import com.github.rahmnathan.localmovies.app.data.repository.Result
import com.github.rahmnathan.localmovies.app.media.data.Media
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a playback request
 */
sealed class PlaybackResult {
    data class PlayLocally(
        val streamUrl: String,
        val updatePositionUrl: String,
        val mediaId: String,
        val resumePosition: Long
    ) : PlaybackResult()

    object PlayingOnCast : PlaybackResult()
    data class Error(val message: String) : PlaybackResult()
}

/**
 * Coordinates media playback decisions between local playback and Google Cast.
 * Handles URL signing and Cast session detection.
 */
@Singleton
class PlaybackCoordinator @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val googleCastUtils: GoogleCastUtils
) {
    companion object {
        private const val TAG = "PlaybackCoordinator"
    }

    /**
     * Initiates playback for the given media.
     * Automatically routes to Cast device if a session is active, otherwise returns
     * the signed URLs for local playback.
     *
     * @param media The media to play
     * @param resumePosition Position in milliseconds to resume from
     * @param remainingEpisodes Episodes to queue after this one (for series)
     * @return PlaybackResult indicating how playback should proceed
     */
    suspend fun play(
        media: Media,
        resumePosition: Long = 0,
        remainingEpisodes: List<Media> = emptyList()
    ): PlaybackResult {
        Log.d(TAG, "Playing ${media.title} with resume position: $resumePosition ms")

        // Check if Cast is active
        if (googleCastUtils.isCastSessionActive()) {
            Log.d(TAG, "Cast session active, playing on Cast device")

            val success = googleCastUtils.playOnCast(media, remainingEpisodes, resumePosition)
            if (success) {
                Log.d(TAG, "Successfully sent media to Cast device")
                return PlaybackResult.PlayingOnCast
            } else {
                Log.w(TAG, "Failed to play on Cast, falling back to local playback")
            }
        }

        // Play locally - get signed URLs
        Log.d(TAG, "Playing locally - Getting signed URLs for mediaFileId: ${media.mediaFileId}")

        return when (val result = mediaRepository.getSignedUrls(media.mediaFileId)) {
            is Result.Success -> {
                val signedUrls = result.data
                val streamUrl = signedUrls.stream
                val updatePositionUrl = signedUrls.updatePosition

                if (streamUrl == null) {
                    Log.e(TAG, "Stream URL is null in signed URLs response")
                    return PlaybackResult.Error("Invalid stream URL")
                }

                if (updatePositionUrl == null) {
                    Log.e(TAG, "UpdatePosition URL is null in signed URLs response")
                    return PlaybackResult.Error("Invalid update position URL")
                }

                Log.d(TAG, "Got signed URLs - stream: $streamUrl")
                PlaybackResult.PlayLocally(
                    streamUrl = streamUrl,
                    updatePositionUrl = updatePositionUrl,
                    mediaId = media.mediaFileId,
                    resumePosition = resumePosition
                )
            }
            is Result.Error -> {
                Log.e(TAG, "Failed to get signed URLs: ${result.message}")
                PlaybackResult.Error("Failed to get video URL: ${result.message}")
            }
            else -> {
                PlaybackResult.Error("Unexpected result getting URLs")
            }
        }
    }

    /**
     * Determines remaining episodes to queue for Cast playback.
     * Only queues episodes if the media is a series episode (has episode number).
     *
     * @param media The current media being played
     * @param mediaList The full list of media in the current view
     * @return List of remaining episodes to queue, or empty if not a series
     */
    fun getRemainingEpisodes(media: Media, mediaList: List<Media>): List<Media> {
        // Only queue remaining episodes if this is a series episode (has episode number)
        if (media.number.isNullOrBlank()) {
            return emptyList()
        }

        val currentIndex = mediaList.indexOfFirst { it.mediaFileId == media.mediaFileId }
        return if (currentIndex >= 0 && currentIndex < mediaList.size - 1) {
            mediaList.subList(currentIndex + 1, mediaList.size)
        } else {
            emptyList()
        }
    }
}
