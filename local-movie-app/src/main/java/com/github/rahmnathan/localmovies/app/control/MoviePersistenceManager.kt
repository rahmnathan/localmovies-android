package com.github.rahmnathan.localmovies.app.control

import android.content.Context
import com.github.rahmnathan.localmovies.app.control.MediaPathUtils.getFilename
import com.github.rahmnathan.localmovies.app.control.MediaPathUtils.getParentPath
import com.github.rahmnathan.localmovies.app.data.Media
import com.github.rahmnathan.localmovies.app.persistence.MovieDAO
import com.github.rahmnathan.localmovies.app.persistence.MovieDatabase
import com.github.rahmnathan.localmovies.app.persistence.MovieEntity
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ExecutorService
import java.util.function.Consumer
import java.util.logging.Logger
import java.util.stream.Collectors

class MoviePersistenceManager(private val movieInfoCache: ConcurrentMap<String, MutableList<Media>>, context: Context, executorService: ExecutorService) {
    private val logger = Logger.getLogger(MoviePersistenceManager::class.java.name)
    private lateinit var movieDAO: MovieDAO

    fun addAll(path: String, media: MutableList<Media>) {
        movieInfoCache.putIfAbsent(path, media)
        logger.info("Adding media to database for path: $path")
        val movieEntities = media.stream().map { movie: Media? -> MovieEntity(path, movie!!) }.collect(Collectors.toList())
        movieDAO.insertAll(movieEntities)
        logger.info("Successfully saved media.")
    }

    fun addOne(path: String, media: Media) {
        val mediaList = movieInfoCache.getOrDefault(path, ArrayList())
        mediaList.add(media)
        movieInfoCache[path] = mediaList
        movieDAO.insert(MovieEntity(path, media))
    }

    fun getMoviesAtPath(path: String): Optional<List<Media>> {
        return Optional.ofNullable(movieInfoCache[path])
    }

    fun deleteMovie(path: String?) {
        val parentPath = getParentPath(path!!)
        val filename = getFilename(path)
        val cachedMedia = movieInfoCache[parentPath]
        cachedMedia?.removeIf { movie: Media -> movie.filename.equals(filename, ignoreCase = true) }
        val movieEntity = movieDAO.getByPathAndFilename(parentPath, filename)
        if (movieEntity != null) movieDAO.delete(movieEntity)
    }

    init {
        CompletableFuture.runAsync(Runnable {
            val db = MovieDatabase.getDatabase(context)
            movieDAO = db.movieDAO()!!
            val movieEntities = movieDAO.all
            movieEntities?.forEach(Consumer { movieEntity: MovieEntity? ->
                logger.info("Loading media into memory for path: " + movieEntity?.directoryPath + " Filename: " + movieEntity!!.media.filename)
                val media = movieInfoCache.getOrDefault(movieEntity.directoryPath, ArrayList())
                media.add(movieEntity.media)
                movieInfoCache.putIfAbsent(movieEntity.directoryPath, media)
            })
        }, executorService)
    }
}