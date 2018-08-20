package com.github.rahmnathan.localmovies.app.persistence;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import com.github.rahmnathan.localmovies.app.data.Movie;

@Entity
public class MovieEntity {
    @PrimaryKey(autoGenerate = true)
    private Long id;
    private String directoryPath;
    @Embedded
    private Movie movie;

    public MovieEntity(String directoryPath, Movie movie) {
        this.directoryPath = directoryPath;
        this.movie = movie;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String path) {
        this.directoryPath = path;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }
}
