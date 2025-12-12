package com.github.rahmnathan.localmovies.app.persistence.media

import com.github.rahmnathan.localmovies.app.media.data.Media
import java.util.*

interface MediaPersistenceService {
    fun addAll(path: String, media: MutableList<Media>)

    fun addOne(path: String, media: Media)

    fun getMoviesAtPath(path: String): List<Media>

    fun deleteMovie(path: String?)

    fun deleteAll()

    // Suspend functions for coroutines
    suspend fun addAllSuspend(path: String, media: List<Media>)

    suspend fun addOneSuspend(path: String, media: Media)

    suspend fun getMoviesAtPathSuspend(path: String): List<Media>

    suspend fun deleteMovieSuspend(path: String)

    suspend fun deleteAllSuspend()
}