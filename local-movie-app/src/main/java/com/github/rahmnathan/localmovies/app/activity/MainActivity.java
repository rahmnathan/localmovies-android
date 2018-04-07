package com.github.rahmnathan.localmovies.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.rahmnathan.localmovies.app.adapter.MovieListAdapter;
import com.github.rahmnathan.localmovies.app.control.MovieInfoLoader;
import com.github.rahmnathan.localmovies.app.google.cast.config.ExpandedControlActivity;
import com.github.rahmnathan.localmovies.app.google.cast.control.GoogleCastUtils;
import com.github.rahmnathan.localmovies.app.persistence.MovieHistory;
import com.github.rahmnathan.localmovies.app.enums.MovieGenre;
import com.github.rahmnathan.localmovies.app.enums.MovieOrder;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.github.rahmnathan.localmovies.KeycloakAuthenticator;
import com.github.rahmnathan.localmovies.client.Client;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import rahmnathan.localmovies.R;

public class MainActivity extends AppCompatActivity {
    private final ConcurrentMap<String, List<MovieInfo>> movieInfoCache = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(MainActivity.class.getName());
    private MovieListAdapter movieListAdapter;
    private MovieHistory movieHistory;
    private ProgressBar progressBar;
    private CastContext castContext;
    private GridView gridView;
    private Client myClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        castContext = CastContext.getSharedInstance(this);
        movieHistory = new MovieHistory(this);
        progressBar = findViewById(R.id.progressBar);
        movieListAdapter = new MovieListAdapter(this, new ArrayList<>());
        gridView = findViewById(R.id.gridView);
        gridView.setAdapter(movieListAdapter);

        // Getting phone info and Triggering initial request of titles from server

        try {
            myClient = getPhoneInfo();
            myClient.appendToCurrentPath("Movies");
            Toast.makeText(this, "Logging in", Toast.LENGTH_SHORT).show();
            CompletableFuture<Void> future = CompletableFuture.runAsync(new KeycloakAuthenticator(myClient));
            future.thenRun(this::getVideos);
        } catch (Exception e) {
            startActivity(new Intent(MainActivity.this, SetupActivity.class));
        }

        EditText searchText = findViewById(R.id.searchText);
        searchText.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                movieListAdapter.getFilter().filter(cs);
                gridView.smoothScrollToPosition(0);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });

        Button controls = findViewById(R.id.controls);
        controls.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, ExpandedControlActivity.class)));

        Button series = findViewById(R.id.series);
        series.setOnClickListener(view -> getRootVideos("Series", searchText));

        Button movies = findViewById(R.id.movies);
        movies.setOnClickListener(view -> getRootVideos("Movies", searchText));

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            String posterPath;
            List<MovieInfo> titles;
            MovieInfo movie = movieListAdapter.getMovie(position);
            if (myClient.isViewingVideos()) {
                movieHistory.addHistoryItem(movieListAdapter.getItem(position));
                // If we're viewing movies or episodes we refresh our token and start the video
                CompletableFuture.runAsync(new KeycloakAuthenticator(myClient));
                if (myClient.isViewingEpisodes()) {
                    // If we're playing episodes, we queue up the rest of the season
                    posterPath = myClient.getCurrentPath().toString();
                    titles = movieListAdapter.getOriginalMovieList().stream()
                            .filter(movieInfo -> getEpisodeNumber(movieInfo.getTitle()).compareTo(getEpisodeNumber(movie.getTitle())) > 0 || movieInfo.getTitle().equals(movie.getTitle()))
                            .collect(Collectors.toList());
                } else {
                    posterPath = myClient.getCurrentPath() + movie.getFilename();
                    titles = Collections.singletonList(movie);
                }

                MediaQueueItem[] queueItems = GoogleCastUtils.assembleMediaQueue(titles, posterPath, myClient);
                queueVideos(queueItems);
            } else {
                myClient.appendToCurrentPath(movie.getFilename());
                getVideos();
            }
        });
    }

    private void queueVideos(MediaQueueItem[] queueItems){
        try {
            CastSession session = castContext.getSessionManager().getCurrentCastSession();
            RemoteMediaClient remoteMediaClient = session.getRemoteMediaClient();
            remoteMediaClient.queueLoad(queueItems, 0, 0, null);
            Toast.makeText(MainActivity.this, "Casting", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            logger.severe(e.toString());
            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
            String url = queueItems[0].getMedia().getContentId();
            intent.putExtra("url", url);
            startActivity(intent);
        }
    }

    private Client getPhoneInfo() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(openFileInput("setup"))) {
            Client client = (Client) objectInputStream.readObject();
            client.resetCurrentPath();
            return client;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException();
        }
    }

    private void getRootVideos(String path, EditText searchText){
        myClient.resetCurrentPath();
        searchText.setText("");
        myClient.appendToCurrentPath(path);
        getVideos();
    }

    private void getVideos() {
        if (movieInfoCache.containsKey(myClient.getCurrentPath().toString())) {
            movieListAdapter.clearLists();
            movieListAdapter.updateList(movieInfoCache.get(myClient.getCurrentPath().toString()));
            movieListAdapter.notifyDataSetChanged();
        } else {
            CompletableFuture.runAsync(new MovieInfoLoader(progressBar, movieListAdapter, myClient, movieInfoCache, this));
        }
    }

    private Integer getEpisodeNumber(String title) {
        return Integer.valueOf(title.split(" ")[1]);
    }

    @Override
    public void onBackPressed() {
        String currentDirectory = myClient.getCurrentPath().peekLast();
        if (currentDirectory.toLowerCase().equals("series") || currentDirectory.toLowerCase().equals("movies"))
            System.exit(8);

        myClient.popOneDirectory();
        getVideos();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SetupActivity.class));
                break;
            case R.id.order_date_added:
                sort(MovieOrder.DATE_ADDED);
                break;
            case R.id.order_views:
                sort(MovieOrder.MOST_VIEWS);
                break;
            case R.id.order_year:
                sort(MovieOrder.RELEASE_YEAR);
                break;
            case R.id.order_rating:
                sort(MovieOrder.RATING);
                break;
            case R.id.order_title:
                sort(MovieOrder.TITLE);
                break;
            case R.id.genre_comedy:
                filterGenre(MovieGenre.COMEDY);
                break;
            case R.id.action_action:
                filterGenre(MovieGenre.ACTION);
                break;
            case R.id.genre_sciFi:
                filterGenre(MovieGenre.SCIFI);
                break;
            case R.id.genre_horror:
                filterGenre(MovieGenre.HORROR);
                break;
            case R.id.genre_thriller:
                filterGenre(MovieGenre.THRILLER);
                break;
            case R.id.genre_fantasy:
                filterGenre(MovieGenre.FANTASY);
                break;
            case R.id.action_history:
                myClient.resetCurrentPath();
                myClient.appendToCurrentPath("Movies");
                movieListAdapter.display(movieHistory.getHistoryList());
                break;
        }
        return true;
    }

    private void sort(MovieOrder order){
        movieListAdapter.sort(order);
        gridView.smoothScrollToPosition(0);
    }

    private void filterGenre(MovieGenre genre){
        movieListAdapter.filterGenre(genre);
        gridView.smoothScrollToPosition(0);
    }
}