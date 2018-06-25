package com.github.rahmnathan.localmovies.app.control;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.github.rahmnathan.localmovies.info.provider.data.Movie;

import java.util.List;

@Entity(tableName = "movie_list")
public class MovieListEntity {
    @PrimaryKey
    @NonNull
    private String path;
    @TypeConverters(MovieTypeConverters.class)
    private List<Movie> movies;

    public MovieListEntity(String path, List<Movie> movies) {
        this.path = path;
        this.movies = movies;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @TypeConverters(MovieTypeConverters.class)
    public List<Movie> getMovies() {
        return movies;
    }

    @TypeConverters(MovieTypeConverters.class)
    public void setMovies(List<Movie> movies) {
        this.movies = movies;
    }
}
