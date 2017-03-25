package com.localmovies.provider.boundary;

import com.localmovies.client.Client;
import com.localmovies.provider.control.MovieInfoProvider;
import com.localmovies.provider.data.MovieInfo;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovieInfoFacade {

    private final MovieInfoProvider movieInfoProvider = new MovieInfoProvider();
    private final Logger logger = Logger.getLogger("MovieInfoFacade");

    public List<MovieInfo> getMovieInfo(Client myClient, int page, int resultsPerPage) {
        logger.log(Level.INFO, "Requesting movies");
        return movieInfoProvider.getMovieInfo(myClient, page, resultsPerPage);
    }
}
