package com.github.rahmnathan.localmovies.app.media.data

import java.io.Serializable
import java.util.Comparator

class Media(
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
) : Serializable, Comparator<Media> {

    override fun toString(): String {
        return title
    }

    override fun compare(info1: Media, info2: Media): Int {
        return info1.title.compareTo(info2.title)
    }

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
}