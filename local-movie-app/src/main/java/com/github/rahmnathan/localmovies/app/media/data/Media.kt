package com.github.rahmnathan.localmovies.app.media.data

import java.io.Serializable

data class Media(
    val title: String,
    val imdbRating: String?,
    val metaRating: String?,
    val image: String?,
    val releaseYear: String?,
    val created: Long?,
    val genre: String?,
    val filename: String,
    val actors: String?,
    val plot: String?,
    val path: String,
    val number: String?,
    val type: String,
    val mediaFileId: String,
    val streamable: Boolean,
    val mediaViews: List<MediaView>? = null,
    val signedUrls: SignedUrls? = null,
    val parent: ParentMedia? = null,
    val favorite: Boolean = false
) : Serializable {

    /**
     * Get the most recent valid resume position (in milliseconds)
     * Returns null if no valid resume position exists
     */
    fun getResumePosition(): Long? {
        val recentView = mediaViews
            ?.filter { it.isRecent() }
            ?.maxByOrNull { it.updated }

        return if (recentView != null && recentView.position > 0) {
            recentView.getPositionMillis()
        } else {
            null
        }
    }

    /**
     * Get the series title for this media (if it's an episode).
     * Traverses: episode -> season -> series
     */
    fun getSeriesTitle(): String? {
        return parent?.getSeries()?.title
    }

    /**
     * Get the season number for this media (if it's an episode).
     */
    fun getSeasonNumber(): Int? {
        return parent?.getSeason()?.number
    }

    /**
     * Get the series mediaFileId (for grouping episodes by series).
     */
    fun getSeriesId(): String? {
        return parent?.getSeries()?.mediaFileId
    }

    /**
     * Get watch progress as a fraction (0.0 to 1.0) for displaying progress bars.
     * Returns null if no progress or duration data is available.
     */
    fun getWatchProgress(): Float? {
        val recentView = mediaViews
            ?.filter { it.isRecent() }
            ?.maxByOrNull { it.updated }
            ?: return null

        return recentView.getProgressFraction()
    }

    /**
     * Get the media duration in seconds (from mediaViews).
     * Duration is stored in milliseconds, so we convert to seconds.
     * Returns null if no duration data is available.
     */
    fun getDuration(): Long? {
        val recentView = mediaViews?.maxByOrNull { it.updated }
        val durationMs = recentView?.duration?.toLong() ?: return null
        return durationMs / 1000
    }
}

/**
 * Comparator for sorting Media by title
 */
val MediaTitleComparator: Comparator<Media> = Comparator { m1, m2 ->
    m1.title.compareTo(m2.title)
}
