package com.github.rahmnathan.localmovies.app.control;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.rahmnathan.localmovies.app.adapter.external.keycloak.KeycloakAuthenticator;
import com.github.rahmnathan.localmovies.app.activity.PlayerActivity;
import com.github.rahmnathan.localmovies.app.adapter.list.MovieListAdapter;
import com.github.rahmnathan.localmovies.app.data.Movie;
import com.github.rahmnathan.localmovies.app.google.cast.control.GoogleCastUtils;
import com.github.rahmnathan.localmovies.app.persistence.MovieHistory;
import com.github.rahmnathan.localmovies.app.data.Client;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MovieClickListener implements AdapterView.OnItemClickListener {
    private static final Logger logger = Logger.getLogger(MovieClickListener.class.getName());
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final Handler UIHandler = new Handler(Looper.getMainLooper());
    private MoviePersistenceManager persistenceManager;
    private static volatile MovieLoader movieLoader;
    private MovieListAdapter listAdapter;
    private ProgressBar progressBar;
    private CastContext castContext;
    private MovieHistory history;
    private Context context;
    private Client client;

    private MovieClickListener(){
        // Use the builder
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String posterPath;
        List<Movie> titles;
        Movie movie = listAdapter.getMovie(position);
        if (client.isViewingVideos()) {
            history.addHistoryItem(listAdapter.getItem(position));
            // If we're viewing movies or episodes we refresh our token and start the video
            CompletableFuture.runAsync(new KeycloakAuthenticator(client));
            if (client.isViewingEpisodes()) {
                // If we're playing episodes, we queue up the rest of the season
                posterPath = client.getCurrentPath().toString();
                titles = listAdapter.getOriginalMovieList().stream()
                        .filter(movieInfo -> getEpisodeNumber(movieInfo.getTitle()).compareTo(getEpisodeNumber(movie.getTitle())) > 0 || movieInfo.getTitle().equals(movie.getTitle()))
                        .collect(Collectors.toList());
            } else {
                posterPath = client.getCurrentPath() + File.separator + movie.getFilename();
                titles = Collections.singletonList(movie);
            }

            MediaQueueItem[] queueItems = GoogleCastUtils.assembleMediaQueue(titles, posterPath, client);
            queueVideos(queueItems);
        } else {
            client.appendToCurrentPath(movie.getFilename());
            getVideos(persistenceManager, client, listAdapter, context, progressBar);
        }
    }

    private void queueVideos(MediaQueueItem[] queueItems){
        try {
            CastSession session = castContext.getSessionManager().getCurrentCastSession();
            RemoteMediaClient remoteMediaClient = session.getRemoteMediaClient();
            remoteMediaClient.queueLoad(queueItems, 0, 0, null);
            Toast.makeText(context, "Casting", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            logger.severe(e.toString());
            Intent intent = new Intent(context, PlayerActivity.class);
            String url = queueItems[0].getMedia().getContentId();
            intent.putExtra("url", url);
            context.startActivity(intent);
        }
    }

    public static void getVideos(MoviePersistenceManager persistenceManager, Client myClient, MovieListAdapter movieListAdapter, Context context, ProgressBar progressBar) {
        if(movieLoader != null && movieLoader.isRunning()){
            movieLoader.terminate();
        }

        Optional<List<Movie>> optionalMovies = persistenceManager.getMoviesAtPath(myClient.getCurrentPath().toString());
        if (optionalMovies.isPresent()) {
            movieListAdapter.clearLists();
            movieListAdapter.updateList(optionalMovies.get());
            UIHandler.post(movieListAdapter::notifyDataSetChanged);
            UIHandler.post(() -> progressBar.setVisibility(View.INVISIBLE));
        } else {
            UIHandler.post(() -> progressBar.setVisibility(View.VISIBLE));
            movieLoader = new MovieLoader(movieListAdapter, myClient, persistenceManager, context);
            CompletableFuture.runAsync(movieLoader, executorService)
                    .thenRun(() -> UIHandler.post(() -> progressBar.setVisibility(View.GONE)))
                    .thenRun(() -> FirebaseMessaging.getInstance().subscribeToTopic("movies"));
        }
    }

    private Integer getEpisodeNumber(String title) {
        return Integer.valueOf(title.split(" ")[1]);
    }

    public static class Builder {
        private MovieClickListener clickListener = new MovieClickListener();

        public static Builder newInstance(){
            return new Builder();
        }

        public Builder setMovieInfoCache(MoviePersistenceManager persistenceManager) {
            clickListener.persistenceManager = persistenceManager;
            return this;
        }

        public Builder setMovieListAdapter(MovieListAdapter movieListAdapter) {
            clickListener.listAdapter = movieListAdapter;
            return this;
        }

        public Builder setMovieHistory(MovieHistory movieHistory) {
            clickListener.history = movieHistory;
            return this;
        }

        public Builder setProgressBar(ProgressBar progressBar) {
            clickListener.progressBar = progressBar;
            return this;
        }

        public Builder setCastContext(CastContext castContext) {
            clickListener.castContext = castContext;
            return this;
        }

        public Builder setContext(Context context) {
            clickListener.context = context;
            return this;
        }

        public Builder setClient(Client myClient) {
            clickListener.client = myClient;
            return this;
        }

        public MovieClickListener build(){
            MovieClickListener result = clickListener;
            clickListener = new MovieClickListener();

            return result;
        }
    }
}
