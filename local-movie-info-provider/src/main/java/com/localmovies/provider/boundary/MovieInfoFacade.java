package com.localmovies.provider.boundary;

import com.localmovies.client.Client;
import com.localmovies.provider.control.MovieInfoProvider;
import com.localmovies.provider.data.MovieInfo;

import java.util.List;

public class MovieInfoFacade {

    private final MovieInfoProvider movieInfoProvider = new MovieInfoProvider();

    public List<MovieInfo> getMovieInfo(Client myClient, int page, int resultsPerPage) {
        return movieInfoProvider.getMovieInfo(myClient, page, resultsPerPage);
    }
}
