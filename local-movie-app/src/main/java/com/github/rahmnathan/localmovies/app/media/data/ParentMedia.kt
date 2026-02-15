package com.github.rahmnathan.localmovies.app.media.data

import java.io.Serializable

/**
 * Lightweight parent reference for episode context.
 * For episodes: parent = season, season.parent = series
 */
data class ParentMedia(
    val mediaFileId: String,
    val mediaFileType: String,
    val title: String?,
    val number: Int?,
    val parent: ParentMedia? = null
) : Serializable {

    /**
     * Get the series info by traversing up the parent chain.
     * For an episode: parent (season) -> parent (series)
     */
    fun getSeries(): ParentMedia? {
        return when (mediaFileType) {
            "SERIES" -> this
            "SEASON" -> parent?.takeIf { it.mediaFileType == "SERIES" }
            else -> parent?.getSeries()
        }
    }

    /**
     * Get the season info.
     * For an episode, this is the direct parent.
     */
    fun getSeason(): ParentMedia? {
        return when (mediaFileType) {
            "SEASON" -> this
            else -> parent?.getSeason()
        }
    }
}
