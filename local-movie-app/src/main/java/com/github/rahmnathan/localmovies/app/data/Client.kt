package com.github.rahmnathan.localmovies.app.data

import java.io.Serializable
import java.util.*

class Client : Serializable {
    private val mainPath = LocalMediaPath()
    val serverUrl = "https://movies.nathanrahm.com"
    var currentPath = mainPath
        private set
    var movieCount: Int? = null
    var lastUpdate: Long? = null
    var userName: String? = null
    var password: String? = null

    val isViewingEpisodes: Boolean
        get() = currentPath.size == 3

    private val isViewingMovies: Boolean
        get() = currentPath.toString().toLowerCase(Locale.getDefault()).contains("movies")

    fun resetCurrentPath() {
        currentPath = LocalMediaPath()
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