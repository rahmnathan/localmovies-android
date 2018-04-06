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
import com.github.rahmnathan.localmovies.info.provider.boundary.MovieInfoFacade;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfoRequest;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovieInfoLoader implements Runnable {
    private final Client client;
    private final ProgressBar progressBar;
    private final MovieListAdapter movieListAdapter;
    private final ConcurrentMap<String, List<MovieInfo>> movieInfoCache;
    private final MovieInfoFacade movieInfoFacade = new MovieInfoFacade();
    private final Logger logger = Logger.getLogger(MovieInfoLoader.class.getName());
    private final String deviceId;
    private final Handler UIHandler = new Handler(Looper.getMainLooper());
    private final Context context;

    public MovieInfoLoader(ProgressBar progressBar, MovieListAdapter movieListAdapter, Client myClient,
                    ConcurrentMap<String, List<MovieInfo>> movieInfoCache, Context context) {
        deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
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
        List<MovieInfo> movieInfoList = new ArrayList<>();
        int i = 0;
        String token = FirebaseInstanceId.getInstance().getToken();
        do {
            MovieInfoRequest movieInfoRequest = MovieInfoRequest.Builder.newInstance()
                    .setDeviceId(deviceId)
                    .setPage(i)
                    .setPath(client.getCurrentPath().toString())
                    .setPushToken(token)
                    .setResultsPerPage(itemsPerPage)
                    .build();

            List<MovieInfo> infoList = movieInfoFacade.getMovieInfo(client, movieInfoRequest);
            movieListAdapter.updateList(infoList);
            movieInfoList.addAll(infoList);
            UIHandler.post(movieListAdapter::notifyDataSetChanged);
            i++;
            if (!movieListAdapter.getChars().equals("")) {
                UIHandler.post(() -> movieListAdapter.getFilter().filter(movieListAdapter.getChars()));
            }
        } while (i <= (client.getMovieCount() / itemsPerPage));

        UIHandler.post(() -> progressBar.setVisibility(View.GONE));
        movieInfoCache.putIfAbsent(client.getCurrentPath().toString(), movieInfoList);
    }
}