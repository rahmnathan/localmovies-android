package com.github.rahmnathan.localmovies.app.persistence.media.room

import com.github.rahmnathan.localmovies.app.media.provider.control.MediaPathUtils.getFilename
import com.github.rahmnathan.localmovies.app.media.provider.control.MediaPathUtils.getParentPath
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.persistence.media.MediaPersistenceService
import java.util.*
import java.util.logging.Logger
import java.util.stream.Collectors

class MediaPersistenceServiceRoom(private val mediaDAO: MediaDAO) : MediaPersistenceService {
    private val logger = Logger.getLogger(MediaPersistenceServiceRoom::class.java.name)

    override fun addAll(path: String, media: MutableList<Media>) {
        logger.info("Adding media to database for path: $path")
        val movieEntities = media.stream().map { movie: Media? -> MediaEntity(path, movie!!) }.collect(Collectors.toList())
        mediaDAO.insertAll(movieEntities)
        logger.info("Successfully saved media.")
    }

    override fun addOne(path: String, media: Media) {
        mediaDAO.insert(MediaEntity(path, media))
    }

    override fun getMoviesAtPath(path: String):List<Media> {
        val entities = Optional.ofNullable(mediaDAO.getByPath(path))
        return if(entities.isPresent) {
            entities.get().stream()
                    .map { entity -> entity?.media!! }
                    .collect(Collectors.toList())
        } else {
            Collections.emptyList();
        }
    }

    override fun deleteMovie(path: String?) {
        val parentPath = getParentPath(path!!)
        val filename = getFilename(path)
        val movieEntity = mediaDAO.getByPathAndFilename(parentPath, filename)
        if (movieEntity != null) mediaDAO.delete(movieEntity)
    }

    override fun deleteAll() {
        mediaDAO.deleteAll()
    }

    // Suspend implementations for coroutines
    override suspend fun addAllSuspend(path: String, media: List<Media>) {
        logger.info("Adding media to database for path: $path (suspend)")
        val movieEntities = media.map { MediaEntity(path, it) }
        mediaDAO.insertAllSuspend(movieEntities)
        logger.info("Successfully saved media.")
    }

    override suspend fun addOneSuspend(path: String, media: Media) {
        mediaDAO.insertSuspend(MediaEntity(path, media))
    }

    override suspend fun getMoviesAtPathSuspend(path: String): List<Media> {
        val entities = mediaDAO.getByPathSuspend(path)
        return entities.map { it.media }
    }

    override suspend fun deleteMovieSuspend(path: String) {
        val parentPath = getParentPath(path)
        val filename = getFilename(path)
        val movieEntity = mediaDAO.getByPathAndFilename(parentPath, filename)
        if (movieEntity != null) mediaDAO.deleteSuspend(movieEntity)
    }

    override suspend fun deleteAllSuspend() {
        mediaDAO.deleteAllSuspend()
    }
}