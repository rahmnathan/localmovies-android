package com.github.rahmnathan.localmovies.info.provider.data;

public class MovieEvent {
    private final String relativePath;
    private final String event;
    private final Movie movie;

    public MovieEvent(String event, Movie movie, String relativePath) {
        this.relativePath = relativePath;
        this.event = event;
        this.movie = movie;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getEvent() {
        return event;
    }

    public Movie getMovie() {
        return movie;
    }
}
