package com.github.rahmnathan.localmovies.app.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;

import android.view.Gravity;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        getMenuInflater().inflate(R.menu.cast, toolbar.getMenu());
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), toolbar.getMenu(), R.id.media_route_menu_item);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(toolbar.getMenu().findItem(R.id.action_search));
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new MovieSearchTextWatcher(listAdapter));

        PrimaryDrawerItem homeItem = new PrimaryDrawerItem()
                .withIdentifier(1)
                .withName("Home")
                .withTextColor(Color.WHITE)
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    getRootVideos(MOVIES, searchView);
                    return false;
                });

        PrimaryDrawerItem historyItem = new PrimaryDrawerItem()
                .withIdentifier(2)
                .withName("History")
                .withTextColor(Color.WHITE)
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    client.resetCurrentPath();
                    client.appendToCurrentPath(MOVIES);
                    listAdapter.display(history.getHistoryList());
                    return false;
                });

        PrimaryDrawerItem settingsItem = new PrimaryDrawerItem()
                .withIdentifier(3)
                .withName("My Account")
                .withTextColor(Color.WHITE)
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    MainActivity.this.startActivity(new Intent(MainActivity.this, SetupActivity.class));
                    return false;
                });

        Drawer result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .addDrawerItems(homeItem, historyItem, settingsItem)
                .withSliderBackgroundColor(Color.BLACK)
                .build();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.action_movies);
        bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);

        PopupMenu popup = new PopupMenu(this, bottomNavigationView, Gravity.END);
        popup.setOnMenuItemClickListener(item -> {
            sortVideoList(item, listAdapter, gridView);
            return true;
        });

        popup.getMenuInflater().inflate(R.menu.settings, popup.getMenu());

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_controls:
                    startActivity(new Intent(MainActivity.this, ExpandedControlActivity.class));
                    break;
                case R.id.action_movies:
                    getRootVideos(MOVIES, searchView);
                    break;
                case R.id.action_series:
                    getRootVideos(SERIES, searchView);
                    break;
                case R.id.action_more:
                    popup.show();
                    break;
            }
            return true;
        });

        persistenceManager = new MoviePersistenceManager(movieCache, this, executorService);

        try {
            client = getPhoneInfo(openFileInput("setup"));
            client.appendToCurrentPath(MOVIES);
            CompletableFuture.runAsync(new KeycloakAuthenticator(client), executorService)
                    .thenRun(this::loadVideos)
                    .thenRun(new MovieEventLoader(listAdapter, client, persistenceManager, this));
        } catch (Exception e) {
            startActivity(new Intent(MainActivity.this, SetupActivity.class));
        }

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

    private void getRootVideos(String path, SearchView searchText){
        client.resetCurrentPath();
        searchText.setQuery("", false);
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
}