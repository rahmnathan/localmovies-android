package com.github.rahmnathan.localmovies.app.control;

import android.content.Context;

import com.github.rahmnathan.localmovies.client.LocalMediaPath;
import com.github.rahmnathan.localmovies.info.provider.data.Movie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MoviePersistenceManager {
    private final Logger logger = Logger.getLogger(MoviePersistenceManager.class.getName());
    private final ConcurrentMap<String, List<Movie>> movieInfoCache;
    private MovieDAO movieDAO;

    public MoviePersistenceManager(ConcurrentMap<String, List<Movie>> movieInfoCache, Context context) {
        this.movieInfoCache = movieInfoCache;

        CompletableFuture.runAsync(() -> {
            MovieDatabase db = MovieDatabase.getDatabase(context);
            movieDAO = db.movieDAO();

            List<MovieListEntity> movieListEntities = movieDAO.getAll();

            movieListEntities.forEach(movieListEntity -> {
                logger.info("Loading MovieListEntities into memory: " + movieListEntity.getPath());
                movieInfoCache.put(movieListEntity.getPath(), movieListEntity.getMovies());
            });
        });
    }

    public boolean contains(String key){
        return movieInfoCache.containsKey(key);
    }

    public void addAll(String path, List<Movie> movies){
        movieInfoCache.putIfAbsent(path, movies);
        logger.info("Adding movielistentities to database: " + path);
        movieDAO.insert(new MovieListEntity(path, movies));
    }

    public List<Movie> getMoviesAtPath(String path){
        return movieInfoCache.getOrDefault(path, new ArrayList<>());
    }

    public void deleteMovie(String path){
        LocalMediaPath mediaPath = new LocalMediaPath();
        mediaPath.addAll(Arrays.asList(path.split("/")));
        mediaPath.remove();
        List<Movie> movies = movieInfoCache.getOrDefault(mediaPath.toString(), new ArrayList<>());
        Movie movie = movies.stream().filter(i -> i.getPath().equalsIgnoreCase(path)).collect(Collectors.toList()).get(0);
        movieDAO.delete(new MovieListEntity(path, movies));
        movies.remove(movie);
    }

    public void addMovie(Movie movie){
        LocalMediaPath mediaPath = new LocalMediaPath();
        mediaPath.addAll(Arrays.asList(movie.getPath().split("/")));
        mediaPath.remove();
        List<Movie> movies = movieInfoCache.getOrDefault(mediaPath.toString(), new ArrayList<>());
        movies.add(movie);
        movieDAO.insert(new MovieListEntity(mediaPath.toString(), movies));
    }
}
