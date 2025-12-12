package com.github.rahmnathan.localmovies.app.media.provider.control

object MediaPathUtils {
    fun getParentPath(path: String): String {
        return path.substringBeforeLast('/', "")
    }

    fun getFilename(path: String): String {
        return path.substringAfterLast('/')
    }
}
