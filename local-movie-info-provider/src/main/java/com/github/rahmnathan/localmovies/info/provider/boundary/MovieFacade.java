package com.github.rahmnathan.localmovies.info.provider.boundary;

import com.github.rahmnathan.localmovies.client.Client;
import com.github.rahmnathan.localmovies.info.provider.control.MovieProvider;
import com.github.rahmnathan.localmovies.info.provider.data.Movie;
import com.github.rahmnathan.localmovies.info.provider.data.MovieRequest;

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
}
