package com.github.rahmnathan.localmovies.app

import com.github.rahmnathan.localmovies.app.media.data.MediaEndpoint
import com.github.rahmnathan.localmovies.app.media.data.MediaPath
import java.io.Serializable
import java.util.*

class Client : Serializable {
    private val mainPath = MediaPath()
    val serverUrl = "https://movies.nathanrahm.com"
    var currentPath = mainPath
        private set
    var movieCount: Int? = null
    var lastUpdate: Long? = null
    var userName: String? = null
    var password: String? = null
    var endpoint: MediaEndpoint = MediaEndpoint.MEDIA

    val isViewingEpisodes: Boolean
        get() = currentPath.size == 3

    private val isViewingMovies: Boolean
        get() = currentPath.toString().lowercase(Locale.getDefault()).contains("movies")

    fun resetCurrentPath() {
        currentPath = MediaPath()
    }

    val isViewingVideos: Boolean
        get() = isViewingEpisodes || isViewingMovies

    fun popOneDirectory() {
        currentPath.removeLast()
    }

    fun appendToCurrentPath(directory: String) {
        currentPath.addLast(directory)
    }

}