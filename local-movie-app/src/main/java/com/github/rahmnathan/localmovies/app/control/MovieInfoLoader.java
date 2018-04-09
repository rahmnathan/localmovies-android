package com.github.rahmnathan.localmovies.app.control;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.rahmnathan.localmovies.app.adapter.MovieListAdapter;
import com.github.rahmnathan.localmovies.client.Client;
import com.github.rahmnathan.localmovies.info.provider.boundary.MovieFacade;
import com.github.rahmnathan.localmovies.info.provider.data.Movie;
import com.github.rahmnathan.localmovies.info.provider.data.MovieRequest;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovieInfoLoader implements Runnable {
    private final Logger logger = Logger.getLogger(MovieInfoLoader.class.getName());
    private final MovieFacade movieFacade = new MovieFacade();
    private final Handler UIHandler = new Handler(Looper.getMainLooper());
    private final ConcurrentMap<String, List<Movie>> movieInfoCache;
    private final MovieListAdapter movieListAdapter;
    private final ProgressBar progressBar;
    private final String deviceId;
    private final Context context;
    private final Client client;

    MovieInfoLoader(ProgressBar progressBar, MovieListAdapter movieListAdapter, Client myClient,
                           ConcurrentMap<String, List<Movie>> movieInfoCache, Context context) {
        this.deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        this.movieInfoCache = movieInfoCache;
        this.client = myClient;
        this.movieListAdapter = movieListAdapter;
        this.progressBar = progressBar;
        this.context = context;
    }

    public void run() {
        logger.log(Level.INFO, "Dynamically loading titles");
        if (client.getAccessToken() == null) {
            UIHandler.post(() -> Toast.makeText(context, "Login failed - Check credentials", Toast.LENGTH_LONG).show());
            return;
        }
        int itemsPerPage = 30;
        UIHandler.post(() -> progressBar.setVisibility(View.VISIBLE));
        movieListAdapter.clearLists();
        List<Movie> movieList = new ArrayList<>();
        int i = 0;
        String token = FirebaseInstanceId.getInstance().getToken();
        do {
            MovieRequest movieRequest = MovieRequest.Builder.newInstance()
                    .setDeviceId(deviceId)
                    .setPage(i)
                    .setPath(client.getCurrentPath().toString())
                    .setPushToken(token)
                    .setResultsPerPage(itemsPerPage)
                    .build();

            List<Movie> infoList = movieFacade.getMovieInfo(client, movieRequest);
            movieListAdapter.updateList(infoList);
            movieList.addAll(infoList);
            UIHandler.post(movieListAdapter::notifyDataSetChanged);
            i++;
            if (!movieListAdapter.getChars().equals("")) {
                UIHandler.post(() -> movieListAdapter.getFilter().filter(movieListAdapter.getChars()));
            }
        } while (i <= (client.getMovieCount() / itemsPerPage));

        UIHandler.post(() -> progressBar.setVisibility(View.GONE));
        movieInfoCache.putIfAbsent(client.getCurrentPath().toString(), movieList);
    }
}