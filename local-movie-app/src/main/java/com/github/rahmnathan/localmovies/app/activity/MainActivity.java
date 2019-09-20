package com.github.rahmnathan.localmovies.app.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;

import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.PopupMenu;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

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

        EditText searchText = findViewById(R.id.searchText);
        PrimaryDrawerItem homeItem = new PrimaryDrawerItem().withIdentifier(1).withName("Home")
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    getRootVideos(MOVIES, searchText);
                    return true;
                });

        PrimaryDrawerItem historyItem = new PrimaryDrawerItem().withIdentifier(2).withName("History")
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    client.resetCurrentPath();
                    client.appendToCurrentPath(MOVIES);
                    listAdapter.display(history.getHistoryList());
                    return false;
                });

        PrimaryDrawerItem settingsItem = new PrimaryDrawerItem().withIdentifier(3).withName("My Account")
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    MainActivity.this.startActivity(new Intent(MainActivity.this, SetupActivity.class));
                    return true;
                });

        Drawer result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(findViewById(R.id.toolbar))
                .addDrawerItems(homeItem, historyItem, settingsItem)
                .withSliderBackgroundColor(99999999)
                .build();

        searchText.addTextChangedListener(new MovieSearchTextWatcher(listAdapter));

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
                    getRootVideos(MOVIES, searchText);
                    break;
                case R.id.action_series:
                    getRootVideos(SERIES, searchText);
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
}