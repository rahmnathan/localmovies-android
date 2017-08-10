package com.github.rahmnathan.localmovies.info.provider.boundary;

import com.github.rahmnathan.localmovies.client.Client;
import com.github.rahmnathan.localmovies.info.provider.control.MovieInfoProvider;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovieInfoFacade {
    private final MovieInfoProvider movieInfoProvider = new MovieInfoProvider();
    private final Logger logger = Logger.getLogger(MovieInfoFacade.class.getName());

    public List<MovieInfo> getMovieInfo(Client myClient, int page, int resultsPerPage) {
        logger.log(Level.INFO, "Requesting movies");
        return movieInfoProvider.getMovieInfo(myClient, page, resultsPerPage);
    }
}