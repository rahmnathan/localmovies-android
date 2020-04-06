package com.github.rahmnathan.localmovies.app.persistence.media

import com.github.rahmnathan.localmovies.app.media.data.Media
import java.util.*

interface MediaPersistenceService {
    fun addAll(path: String, media: MutableList<Media>)

    fun addOne(path: String, media: Media)

    fun getMoviesAtPath(path: String): Optional<List<Media>>

    fun deleteMovie(path: String?)
}