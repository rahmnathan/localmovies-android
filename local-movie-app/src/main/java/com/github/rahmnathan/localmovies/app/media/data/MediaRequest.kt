package com.github.rahmnathan.localmovies.app.media.data

data class MediaRequest(
    val page: Int,
    val pageSize: Int,
    val path: String,
    val order: String,
    val client: String = "ANDROID",
    val q: String? = null,      // Search query parameter
    val genre: String? = null,  // Genre filter
    val type: String? = null    // Type filter (MOVIE, SERIES, etc.)
)
