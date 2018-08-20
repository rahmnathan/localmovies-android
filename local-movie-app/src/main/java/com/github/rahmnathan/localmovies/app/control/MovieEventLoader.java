package com.github.rahmnathan.localmovies.app.control;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.github.rahmnathan.localmovies.app.activity.SetupActivity;
import com.github.rahmnathan.localmovies.app.adapter.list.MovieListAdapter;
import com.github.rahmnathan.localmovies.app.data.Client;
import com.github.rahmnathan.localmovies.app.adapter.external.localmovie.MovieFacade;
import com.github.rahmnathan.localmovies.app.data.Movie;
import com.github.rahmnathan.localmovies.app.data.MovieEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MovieEventLoader implements Runnable {
    private final Logger logger = Logger.getLogger(MovieEventLoader.class.getName());
    private final Handler UIHandler = new Handler(Looper.getMainLooper());
    private final MoviePersistenceManager persistenceManager;
    private final MovieFacade movieFacade = new MovieFacade();
    private final MovieListAdapter movieListAdapter;
    private final Context context;
    private final Client client;

    public MovieEventLoader(MovieListAdapter movieListAdapter, Client myClient, MoviePersistenceManager persistenceManager, Context context) {
        this.movieListAdapter = movieListAdapter;
        this.persistenceManager = persistenceManager;
        this.client = myClient;
        this.context = context;
    }


    @Override
    public void run() {
        logger.log(Level.INFO, "Dynamically loading titles");

        if (client.getAccessToken() == null) {
            UIHandler.post(() -> Toast.makeText(context, "Login failed - Check credentials", Toast.LENGTH_LONG).show());
            return;
        }

        List<MovieEvent> events = movieFacade.getMovieEvents(client);
        events.forEach(event -> {
            if(event.getEvent().equalsIgnoreCase("CREATE")){
                Movie movie = event.getMovie();
                persistenceManager.addOne(getParentPath(event.getRelativePath()), movie);
                movieListAdapter.clearLists();
                movieListAdapter.updateList(persistenceManager.getMoviesAtPath(client.getCurrentPath().toString()).orElse(new ArrayList<>()));
                UIHandler.post(movieListAdapter::notifyDataSetChanged);
            } else {
                persistenceManager.deleteMovie(event.getRelativePath());
                movieListAdapter.clearLists();
                movieListAdapter.updateList(persistenceManager.getMoviesAtPath(client.getCurrentPath().toString()).orElse(new ArrayList<>()));
                UIHandler.post(movieListAdapter::notifyDataSetChanged);
            }
        });

        if (!movieListAdapter.getChars().equals("")) {
            UIHandler.post(() -> movieListAdapter.getFilter().filter(movieListAdapter.getChars()));
        }

        client.setLastUpdate(System.currentTimeMillis());
        SetupActivity.saveData(client, context);
    }

    private static String getParentPath(String path){
        String[] dirs = path.split(File.separator);
        return Arrays.stream(dirs)
                .limit(dirs.length - 1)
                .collect(Collectors.joining(File.separator));
    }
}
