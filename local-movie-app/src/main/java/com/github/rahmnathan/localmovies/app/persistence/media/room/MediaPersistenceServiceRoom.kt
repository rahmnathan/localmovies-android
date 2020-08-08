package com.github.rahmnathan.localmovies.app.persistence.media.room

import com.github.rahmnathan.localmovies.app.media.provider.control.MediaPathUtils.getFilename
import com.github.rahmnathan.localmovies.app.media.provider.control.MediaPathUtils.getParentPath
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.persistence.media.MediaPersistenceService
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ExecutorService
import java.util.function.Consumer
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
}