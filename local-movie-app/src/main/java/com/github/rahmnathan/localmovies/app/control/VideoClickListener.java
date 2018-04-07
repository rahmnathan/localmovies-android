package com.github.rahmnathan.localmovies.app.control;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.rahmnathan.localmovies.KeycloakAuthenticator;
import com.github.rahmnathan.localmovies.app.activity.PlayerActivity;
import com.github.rahmnathan.localmovies.app.adapter.MovieListAdapter;
import com.github.rahmnathan.localmovies.app.google.cast.control.GoogleCastUtils;
import com.github.rahmnathan.localmovies.app.persistence.MovieHistory;
import com.github.rahmnathan.localmovies.client.Client;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class VideoClickListener implements AdapterView.OnItemClickListener {
    private static final Logger logger = Logger.getLogger(VideoClickListener.class.getName());
    private ConcurrentMap<String, List<MovieInfo>> movieCache;
    private MovieListAdapter listAdapter;
    private ProgressBar progressBar;
    private CastContext castContext;
    private MovieHistory history;
    private Context context;
    private Client client;

    private VideoClickListener(){
        // Use the builder
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String posterPath;
        List<MovieInfo> titles;
        MovieInfo movie = listAdapter.getMovie(position);
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
                posterPath = client.getCurrentPath() + movie.getFilename();
                titles = Collections.singletonList(movie);
            }

            MediaQueueItem[] queueItems = GoogleCastUtils.assembleMediaQueue(titles, posterPath, client);
            queueVideos(queueItems);
        } else {
            client.appendToCurrentPath(movie.getFilename());
            getVideos(movieCache, client, listAdapter, context, progressBar);
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

    public static void getVideos(ConcurrentMap<String, List<MovieInfo>> movieInfoCache, Client myClient, MovieListAdapter movieListAdapter, Context context, ProgressBar progressBar) {
        if (movieInfoCache.containsKey(myClient.getCurrentPath().toString())) {
            movieListAdapter.clearLists();
            movieListAdapter.updateList(movieInfoCache.get(myClient.getCurrentPath().toString()));
            movieListAdapter.notifyDataSetChanged();
        } else {
            CompletableFuture.runAsync(new MovieInfoLoader(progressBar, movieListAdapter, myClient, movieInfoCache, context));
        }
    }

    private Integer getEpisodeNumber(String title) {
        return Integer.valueOf(title.split(" ")[1]);
    }

    public static class Builder {
        private VideoClickListener clickListener = new VideoClickListener();

        public static Builder newInstance(){
            return new Builder();
        }

        public Builder setMovieInfoCache(ConcurrentMap<String, List<MovieInfo>> movieInfoCache) {
            clickListener.movieCache = movieInfoCache;
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

        public VideoClickListener build(){
            VideoClickListener result = clickListener;
            clickListener = new VideoClickListener();

            return result;
        }
    }
}
