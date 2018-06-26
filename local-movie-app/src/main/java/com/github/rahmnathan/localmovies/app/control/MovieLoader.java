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
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovieLoader implements Runnable {
    private final Logger logger = Logger.getLogger(MovieLoader.class.getName());
    private final Handler UIHandler = new Handler(Looper.getMainLooper());
    private final MoviePersistenceManager persistenceManager;
    private final MovieFacade movieFacade = new MovieFacade();
    private final MovieListAdapter movieListAdapter;
    private static final int ITEMS_PER_PAGE = 30;
    private volatile boolean running = true;
    private final String deviceId;
    private final Context context;
    private final Client client;

    MovieLoader(MovieListAdapter movieListAdapter, Client myClient, MoviePersistenceManager persistenceManager, Context context) {
        this.deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        this.movieListAdapter = movieListAdapter;
        this.persistenceManager = persistenceManager;
        this.client = myClient;
        this.context = context;
    }

    public void run() {
        logger.log(Level.INFO, "Dynamically loading titles");

        String token = FirebaseInstanceId.getInstance().getToken();

        if (client.getAccessToken() == null) {
            UIHandler.post(() -> Toast.makeText(context, "Login failed - Check credentials", Toast.LENGTH_LONG).show());
            return;
        }

        movieListAdapter.clearLists();
        int page = 0;
        do {
            MovieRequest movieRequest = MovieRequest.Builder.newInstance()
                    .setPath(client.getCurrentPath().toString())
                    .setResultsPerPage(ITEMS_PER_PAGE)
                    .setDeviceId(deviceId)
                    .setPushToken(token)
                    .setPage(page)
                    .build();

            List<Movie> infoList = movieFacade.getMovieInfo(client, movieRequest);

            if (!running) break;

            movieListAdapter.updateList(infoList);
            UIHandler.post(movieListAdapter::notifyDataSetChanged);
            if (!movieListAdapter.getChars().equals("")) {
                UIHandler.post(() -> movieListAdapter.getFilter().filter(movieListAdapter.getChars()));
            }

            page++;
        } while (page <= (client.getMovieCount() / ITEMS_PER_PAGE));

        if (running) {
            persistenceManager.addAll(client.getCurrentPath().toString(), new ArrayList<>(movieListAdapter.getOriginalMovieList()));
        }

        running = false;
    }

    public boolean isRunning(){
        return running;
    }

    public void terminate(){
        running = false;
    }
}