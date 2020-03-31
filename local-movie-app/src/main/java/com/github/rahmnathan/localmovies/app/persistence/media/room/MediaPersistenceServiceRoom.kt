package com.github.rahmnathan.localmovies.app.persistence.media.room

import com.github.rahmnathan.localmovies.app.control.MediaPathUtils.getFilename
import com.github.rahmnathan.localmovies.app.control.MediaPathUtils.getParentPath
import com.github.rahmnathan.localmovies.app.data.Media
import com.github.rahmnathan.localmovies.app.persistence.media.MediaPersistenceService
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ExecutorService
import java.util.function.Consumer
import java.util.logging.Logger
import java.util.stream.Collectors

class MediaPersistenceServiceRoom(private val mediaDAO: MediaDAO,
                                  executorService: ExecutorService) : MediaPersistenceService {
    private val logger = Logger.getLogger(MediaPersistenceServiceRoom::class.java.name)
    private val mediaCache: ConcurrentMap<String, MutableList<Media>> = ConcurrentHashMap()

    override fun addAll(path: String, media: MutableList<Media>) {
        mediaCache.putIfAbsent(path, media)
        logger.info("Adding media to database for path: $path")
        val movieEntities = media.stream().map { movie: Media? -> MediaEntity(path, movie!!) }.collect(Collectors.toList())
        mediaDAO.insertAll(movieEntities)
        logger.info("Successfully saved media.")
    }

    override fun addOne(path: String, media: Media) {
        val mediaList = mediaCache.getOrDefault(path, ArrayList())
        mediaList.add(media)
        mediaCache[path] = mediaList
        mediaDAO.insert(MediaEntity(path, media))
    }

    override fun getMoviesAtPath(path: String): Optional<List<Media>> {
        return Optional.ofNullable(mediaCache[path])
    }

    override fun deleteMovie(path: String?) {
        val parentPath = getParentPath(path!!)
        val filename = getFilename(path)
        val cachedMedia = mediaCache[parentPath]
        cachedMedia?.removeIf { movie: Media -> movie.filename.equals(filename, ignoreCase = true) }
        val movieEntity = mediaDAO.getByPathAndFilename(parentPath, filename)
        if (movieEntity != null) mediaDAO.delete(movieEntity)
    }

    init {
        CompletableFuture.runAsync(Runnable {
            val movieEntities = mediaDAO.all
            movieEntities?.forEach(Consumer { mediaEntity: MediaEntity? ->
                val media = mediaCache.getOrDefault(mediaEntity!!.directoryPath, ArrayList())
                media.add(mediaEntity.media)
                mediaCache.putIfAbsent(mediaEntity.directoryPath, media)
            })
        }, executorService)
    }
}