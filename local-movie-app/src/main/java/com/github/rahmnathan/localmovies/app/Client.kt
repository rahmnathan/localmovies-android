package com.github.rahmnathan.localmovies.app

import com.github.rahmnathan.localmovies.app.media.data.MediaEndpoint
import com.github.rahmnathan.localmovies.app.media.data.MediaPath
import java.io.Serializable

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

    fun resetCurrentPath() {
        currentPath = MediaPath()
    }

    fun popOneDirectory() {
        currentPath.removeLast()
    }

    fun appendToCurrentPath(directory: String) {
        currentPath.addLast(directory)
    }

}