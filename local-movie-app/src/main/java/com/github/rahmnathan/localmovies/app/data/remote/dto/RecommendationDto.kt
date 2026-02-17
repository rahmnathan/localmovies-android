package com.github.rahmnathan.localmovies.app.data.remote.dto

import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.media.data.Recommendation
import com.google.gson.annotations.SerializedName

data class RecommendationDto(
    @SerializedName("mediaFile")
    val mediaFile: MediaResponseDto,

    @SerializedName("reason")
    val reason: String?,

    @SerializedName("rank")
    val rank: Int?
) {
    fun toRecommendation(serverUrl: String): Recommendation {
        return Recommendation(
            media = mediaFile.toMedia(serverUrl),
            reason = reason,
            rank = rank ?: 0
        )
    }
}
