package com.github.rahmnathan.localmovies.app.media.data

data class Recommendation(
    val media: Media,
    val reason: String?,
    val rank: Int
)
