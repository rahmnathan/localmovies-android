package com.github.rahmnathan.localmovies.app.ui.player

import com.github.rahmnathan.localmovies.app.data.repository.MediaRepository
import com.github.rahmnathan.localmovies.app.data.repository.Result
import com.github.rahmnathan.localmovies.app.media.data.Media
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the episode queue for auto-play functionality.
 * Stores remaining episodes after the current one finishes.
 */
@Singleton
class EpisodeQueueManager @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    private var episodeQueue: List<Media> = emptyList()
    private var currentMediaId: String? = null

    /**
     * Sets up the episode queue for auto-play.
     * @param currentMedia The media currently being played
     * @param remainingEpisodes List of episodes after the current one
     */
    fun setQueue(currentMedia: Media, remainingEpisodes: List<Media>) {
        currentMediaId = currentMedia.mediaFileId
        episodeQueue = remainingEpisodes
    }

    /**
     * Clears the episode queue.
     */
    fun clearQueue() {
        currentMediaId = null
        episodeQueue = emptyList()
    }

    /**
     * Gets the next episode in the queue, if any.
     */
    fun getNextEpisode(): Media? {
        return episodeQueue.firstOrNull()
    }

    /**
     * Advances to the next episode, returning the signed URLs if available.
     * Also updates the queue to point to the subsequent episode.
     */
    suspend fun advanceToNextEpisode(): NextEpisodePlayback? {
        val nextEpisode = episodeQueue.firstOrNull() ?: return null

        // Update queue to remaining episodes
        episodeQueue = episodeQueue.drop(1)
        currentMediaId = nextEpisode.mediaFileId

        // Get signed URLs for the next episode
        return when (val result = mediaRepository.getSignedUrls(nextEpisode.mediaFileId)) {
            is Result.Success -> {
                val signedUrls = result.data
                NextEpisodePlayback(
                    streamUrl = signedUrls.stream ?: return null,
                    updatePositionUrl = signedUrls.updatePosition ?: return null,
                    mediaId = nextEpisode.mediaFileId,
                    title = nextEpisode.title,
                    episodeNumber = nextEpisode.number
                )
            }
            else -> null
        }
    }

    /**
     * Checks if there are more episodes in the queue.
     */
    fun hasNextEpisode(): Boolean = episodeQueue.isNotEmpty()
}

data class NextEpisodePlayback(
    val streamUrl: String,
    val updatePositionUrl: String,
    val mediaId: String,
    val title: String,
    val episodeNumber: String?
)
