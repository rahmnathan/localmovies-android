package com.github.rahmnathan.localmovies.app.adapter.external.localmovie;

import com.github.rahmnathan.localmovies.app.data.Client;
import com.github.rahmnathan.localmovies.app.data.Movie;
import com.github.rahmnathan.localmovies.app.data.MovieEvent;
import com.github.rahmnathan.localmovies.app.data.MovieRequest;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovieFacade {
    private final MovieProvider movieProvider = new MovieProvider();
    private final Logger logger = Logger.getLogger(MovieFacade.class.getName());

    public List<Movie> getMovieInfo(Client myClient, MovieRequest movieRequest) {
        logger.log(Level.INFO, "Requesting movies");
        return movieProvider.getMovieInfo(myClient, movieRequest);
    }

    public List<MovieEvent> getMovieEvents(Client myClient) {
        logger.log(Level.INFO, "Requesting movie events");
        List<MovieEvent> events = movieProvider.getMovieEvents(myClient);
        logger.info("Found MovieEvents (count): " + events.size());
        return events;
    }
}
