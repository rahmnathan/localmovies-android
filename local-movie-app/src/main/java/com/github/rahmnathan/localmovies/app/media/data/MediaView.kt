package com.github.rahmnathan.localmovies.app.media.data

import java.io.Serializable

data class MediaUser(
    val id: Long,
    val userId: String,
    val created: Long,
    val updated: Long
) : Serializable

data class MediaView(
    val id: Long,
    val position: Double,
    val mediaUser: MediaUser,
    val created: Long,
    val updated: Long
) : Serializable {

    /**
     * Check if this view is recent (within last 30 days)
     */
    fun isRecent(): Boolean {
        val thirtyDaysAgo = System.currentTimeMillis() / 1000 - (30 * 24 * 60 * 60)
        return updated > thirtyDaysAgo
    }

    /**
     * Get position in milliseconds for video player
     * Position from API is already in milliseconds (as Double)
     */
    fun getPositionMillis(): Long {
        return position.toLong()
    }
}
