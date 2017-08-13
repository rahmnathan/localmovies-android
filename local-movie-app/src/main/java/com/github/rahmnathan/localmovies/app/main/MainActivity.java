package com.github.rahmnathan.localmovies.app.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.rahmnathan.localmovies.app.google.cast.config.ExpandedControlActivity;
import com.github.rahmnathan.localmovies.app.google.cast.control.GoogleCastUtils;
import com.github.rahmnathan.localmovies.app.history.MovieHistory;
import com.github.rahmnathan.localmovies.app.video.player.VideoPlayer;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import rahmnathan.localmovies.R;
import com.github.rahmnathan.localmovies.app.setup.Setup;

public class MainActivity extends AppCompatActivity {
    private MovieListAdapter movieListAdapter;
    private Client myClient;
    private ProgressBar progressBar;
    private CastContext castContext;
    private MovieHistory movieHistory;
    private final Logger logger = Logger.getLogger(MainActivity.class.getName());
    private final ConcurrentMap<String, List<MovieInfo>> movieInfoCache = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        castContext = CastContext.getSharedInstance(this);
        movieHistory = new MovieHistory(this);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        movieListAdapter = new MovieListAdapter(this, new ArrayList<>());
        GridView movieGridView = (GridView) findViewById(R.id.gridView);
        movieGridView.setAdapter(movieListAdapter);

        // Getting phone info and Triggering initial request of titles from server

        try {
            myClient = getPhoneInfo();
            myClient.appendToCurrentPath("Movies");
            Toast.makeText(this, "Logging in", Toast.LENGTH_SHORT).show();
            executorService.submit(new KeycloakAuthenticator(myClient));
            getVideos();
        } catch (Exception e) {
            startActivity(new Intent(MainActivity.this, Setup.class));
        }

        EditText searchText = (EditText) findViewById(R.id.searchText);
        searchText.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                movieListAdapter.getFilter().filter(cs);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });

        Button controls = (Button) findViewById(R.id.controls);
        controls.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, ExpandedControlActivity.class)));

        Button series = (Button) findViewById(R.id.series);
        series.setOnClickListener(view -> {
            myClient.resetCurrentPath();
            searchText.setText("");
            myClient.appendToCurrentPath("Series");
            getVideos();
        });
        Button movies = (Button) findViewById(R.id.movies);
        movies.setOnClickListener(view -> {
            myClient.resetCurrentPath();
            searchText.setText("");
            myClient.appendToCurrentPath("Movies");
            getVideos();
        });

        movieGridView.setOnItemClickListener((parent, view, position, id) -> {
            String title = movieListAdapter.getTitle(position);
            String posterPath;
            List<String> titles = new ArrayList<>();
            if (myClient.isViewingVideos()) {
                movieHistory.addHistoryItem(movieListAdapter.getItem(position));
                // If we're viewing movies or episodes we refresh our token and start the video
                executorService.submit(new KeycloakAuthenticator(myClient));
                if (myClient.isViewingEpisodes()) {
                    // If we're playing episodes, we queue up the rest of the season
                    posterPath = myClient.getCurrentPath().toString();
                    movieListAdapter.getOriginalMovieList().forEach(movieInfo -> {
                        if (getEpisodeNumber(movieInfo.getTitle()).compareTo(getEpisodeNumber(title)) > 0
                                || movieInfo.getTitle().equals(title)) {
                            titles.add(movieInfo.getTitle());
                        }
                    });
                } else {
                    posterPath = myClient.getCurrentPath() + title;
                    titles.add(title);
                }

                MediaQueueItem[] queueItems = GoogleCastUtils.assembleMediaQueue(titles, posterPath, myClient);

                try {
                    CastSession session = castContext.getSessionManager().getCurrentCastSession();
                    RemoteMediaClient remoteMediaClient = session.getRemoteMediaClient();
                    remoteMediaClient.queueLoad(queueItems, 0, 0, null);
                    Toast.makeText(MainActivity.this, "Casting", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    logger.severe(e.toString());
                    Intent intent = new Intent(MainActivity.this, VideoPlayer.class);
                    String url = queueItems[0].getMedia().getContentId();
                    intent.putExtra("url", url);
                    startActivity(intent);
                }
            } else {
                myClient.appendToCurrentPath(title);
                getVideos();
            }
        });
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

    private void getVideos() {
        if (movieInfoCache.containsKey(myClient.getCurrentPath().toString())) {
            movieListAdapter.clearLists();
            movieListAdapter.updateList(movieInfoCache.get(myClient.getCurrentPath().toString()));
            movieListAdapter.notifyDataSetChanged();
        } else {
            executorService.submit(new MovieInfoLoader(progressBar, movieListAdapter, myClient, movieInfoCache, this));
        }
    }

    private Integer getEpisodeNumber(String title) {
        return Integer.valueOf(title.split(" ")[1].split("\\.")[0]);
    }

    @Override
    public void onBackPressed() {
        String currentDirectory = myClient.getCurrentPath().peekLast();
        if (currentDirectory.toLowerCase().equals("series") | currentDirectory.toLowerCase().equals("movies"))
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
                startActivity(new Intent(MainActivity.this, Setup.class));
                break;
            case R.id.action_sortByAdded:
                movieListAdapter.sort(MovieOrder.DATE_ADDED);
                break;
            case R.id.action_sortByViews:
                movieListAdapter.sort(MovieOrder.MOST_VIEWS);
                break;
            case R.id.action_sortByYear:
                movieListAdapter.sort(MovieOrder.RELEASE_YEAR);
                break;
            case R.id.action_sortByRating:
                movieListAdapter.sort(MovieOrder.RATING);
                break;
            case R.id.action_sortByTitle:
                movieListAdapter.sort(MovieOrder.TITLE);
                break;
            case R.id.action_history:
                movieListAdapter.display(movieHistory.getHistoryList());
                break;
        }
        return true;
    }
}