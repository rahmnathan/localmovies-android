package com.github.rahmnathan.localmovies.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.rahmnathan.localmovies.app.adapter.list.MovieListAdapter;
import com.github.rahmnathan.localmovies.app.control.MovieClickListener;
import com.github.rahmnathan.localmovies.app.control.MovieEventLoader;
import com.github.rahmnathan.localmovies.app.control.MoviePersistenceManager;
import com.github.rahmnathan.localmovies.app.control.MovieSearchTextWatcher;
import com.github.rahmnathan.localmovies.app.data.Media;
import com.github.rahmnathan.localmovies.app.google.cast.config.ExpandedControlActivity;
import com.github.rahmnathan.localmovies.app.persistence.MovieHistory;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.github.rahmnathan.localmovies.app.adapter.external.keycloak.KeycloakAuthenticator;
import com.github.rahmnathan.localmovies.app.data.Client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rahmnathan.localmovies.R;

import static com.github.rahmnathan.localmovies.app.activity.DescriptionPopUpActivity.MOVIE;
import static com.github.rahmnathan.localmovies.app.control.MainActivityUtils.getPhoneInfo;
import static com.github.rahmnathan.localmovies.app.control.MainActivityUtils.sortVideoList;
import static com.github.rahmnathan.localmovies.app.control.MovieClickListener.getVideos;

public class MainActivity extends AppCompatActivity {
    private final ConcurrentMap<String, List<Media>> movieCache = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile MoviePersistenceManager persistenceManager;
    private static final String MOVIES = "Movies";
    private static final String SERIES = "Series";
    private MovieListAdapter listAdapter;
    private MovieHistory history;
    private ProgressBar progressBar;
    private GridView gridView;
    public static Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        history = new MovieHistory(this);
        progressBar = findViewById(R.id.progressBar);
        listAdapter = new MovieListAdapter(this, new ArrayList<>());
        gridView = findViewById(R.id.gridView);
        gridView.setAdapter(listAdapter);

        persistenceManager = new MoviePersistenceManager(movieCache, this, executorService);

        // Getting phone info and Triggering initial request of titles from server

        try {
            client = getPhoneInfo(openFileInput("setup"));
            client.appendToCurrentPath(MOVIES);
            Toast.makeText(this, "Logging in", Toast.LENGTH_SHORT).show();
            CompletableFuture.runAsync(new KeycloakAuthenticator(client), executorService)
                    .thenRun(this::loadVideos)
                    .thenRun(new MovieEventLoader(listAdapter, client, persistenceManager, this));
        } catch (Exception e) {
            startActivity(new Intent(MainActivity.this, SetupActivity.class));
        }

        EditText searchText = findViewById(R.id.searchText);
        searchText.addTextChangedListener(new MovieSearchTextWatcher(listAdapter));

        Button controls = findViewById(R.id.controls);
        controls.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, ExpandedControlActivity.class)));

        Button series = findViewById(R.id.series);
        series.setOnClickListener(view -> getRootVideos(SERIES, searchText));

        Button movies = findViewById(R.id.media);
        movies.setOnClickListener(view -> getRootVideos(MOVIES, searchText));

        MovieClickListener clickListener = MovieClickListener.Builder.newInstance()
                .setCastContext(CastContext.getSharedInstance(this))
                .setContext(this)
                .setProgressBar(progressBar)
                .setClient(client)
                .setMovieListAdapter(listAdapter)
                .setMovieInfoCache(persistenceManager)
                .setMovieHistory(history)
                .build();

        gridView.setOnItemClickListener(clickListener);
        gridView.setOnItemLongClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, DescriptionPopUpActivity.class);
            intent.putExtra(MOVIE, listAdapter.getMovie(position));
            startActivity(intent);
            return true;
        });
    }

    private void getRootVideos(String path, EditText searchText){
        client.resetCurrentPath();
        searchText.setText("");
        client.appendToCurrentPath(path);
        loadVideos();
    }

    private void loadVideos(){
        getVideos(persistenceManager, client, listAdapter, this, progressBar);
    }

    @Override
    public void onBackPressed() {
        String currentDirectory = client.getCurrentPath().peekLast();
        if (currentDirectory.equalsIgnoreCase(SERIES) || currentDirectory.equalsIgnoreCase(MOVIES))
            System.exit(8);

        client.popOneDirectory();
        loadVideos();
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
        sortVideoList(item, listAdapter, gridView, this, client, history);
        return true;
    }
}