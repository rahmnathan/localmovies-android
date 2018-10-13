package com.github.rahmnathan.localmovies.app.control;

import android.content.Context;

import com.github.rahmnathan.localmovies.app.data.Movie;
import com.github.rahmnathan.localmovies.app.persistence.MovieDAO;
import com.github.rahmnathan.localmovies.app.persistence.MovieDatabase;
import com.github.rahmnathan.localmovies.app.persistence.MovieEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.github.rahmnathan.localmovies.app.control.MediaPathUtils.getFilename;
import static com.github.rahmnathan.localmovies.app.control.MediaPathUtils.getParentPath;

public class MoviePersistenceManager {
    private final Logger logger = Logger.getLogger(MoviePersistenceManager.class.getName());
    private final ConcurrentMap<String, List<Movie>> movieInfoCache;
    private MovieDAO movieDAO;

    public MoviePersistenceManager(ConcurrentMap<String, List<Movie>> movieInfoCache, Context context, ExecutorService executorService) {
        this.movieInfoCache = movieInfoCache;

        CompletableFuture.runAsync(() -> {
            MovieDatabase db = MovieDatabase.getDatabase(context);
            movieDAO = db.movieDAO();

            List<MovieEntity> movieEntities = movieDAO.getAll();

            movieEntities.forEach(movieEntity -> {
                logger.info("Loading MovieEntities into memory - Path: " + movieEntity.getDirectoryPath() + " Filename: " + movieEntity.getMovie().getFilename());
                List<Movie> movies = movieInfoCache.getOrDefault(movieEntity.getDirectoryPath(), new ArrayList<>());
                movies.add(movieEntity.getMovie());
                movieInfoCache.putIfAbsent(movieEntity.getDirectoryPath(), movies);
            });

        }, executorService);
    }

    public void addAll(String path, List<Movie> movies){
        movieInfoCache.putIfAbsent(path, movies);
        logger.info("Adding movielistentities to database: " + path);
        List<MovieEntity> movieEntities = movies.stream().map(movie -> new MovieEntity(path, movie)).collect(Collectors.toList());
        movieDAO.insertAll(movieEntities);
    }

    void addOne(String path, Movie movie){
        List<Movie> movies = movieInfoCache.getOrDefault(path, new ArrayList<>());
        movies.add(movie);
        movieInfoCache.put(path, movies);
        movieDAO.insert(new MovieEntity(path, movie));
    }

    Optional<List<Movie>> getMoviesAtPath(String path){
        return Optional.ofNullable(movieInfoCache.get(path));
    }

    void deleteMovie(String path){
        String parentPath = getParentPath(path);
        String filename = getFilename(path);

        List<Movie> cachedMovies = movieInfoCache.get(parentPath);
        if(cachedMovies != null)
            cachedMovies.removeIf(movie -> movie.getFilename().equalsIgnoreCase(filename));

        MovieEntity movieEntity = movieDAO.getByPathAndFilename(parentPath, filename);
        if(movieEntity != null)
            movieDAO.delete(movieEntity);
    }

}
