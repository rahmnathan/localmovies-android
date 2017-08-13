package com.github.rahmnathan.localmovies.app.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
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
import com.github.rahmnathan.localmovies.app.google.cast.control.CastControl;
import com.github.rahmnathan.localmovies.app.video.player.VideoPlayer;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;
import com.github.rahmnathan.localmovies.KeycloakAuthenticator;
import com.github.rahmnathan.localmovies.client.Client;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;

import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final Logger logger = Logger.getLogger(MainActivity.class.getName());
    private final ConcurrentMap<String, List<MovieInfo>> movieInfoCache = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        castContext = CastContext.getSharedInstance(this);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        movieListAdapter = new MovieListAdapter(this, new ArrayList<>());
        GridView movieGridView = (GridView) findViewById(R.id.gridView);
        movieGridView.setAdapter(movieListAdapter);

        // Getting phone info and Triggering initial request of titles from server

        try {
            myClient = getPhoneInfo();
            myClient.appendToCurrentPath("Movies");
            updateAccessToken(false);
            requestTitles();
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
            requestTitles();
        });
        Button movies = (Button) findViewById(R.id.movies);
        movies.setOnClickListener(view -> {
            myClient.resetCurrentPath();
            searchText.setText("");
            myClient.appendToCurrentPath("Movies");
            requestTitles();
        });

        movieGridView.setOnItemClickListener((parent, view, position, id) -> {
            String title = movieListAdapter.getTitle(position);
            String posterPath;
            List<String> titles = new ArrayList<>();

            if (myClient.isViewingVideos()) {
                //If we're viewing movies or episodes we refresh our key and start the movie
                updateAccessToken(true);
                myClient.setVideoPath(myClient.getCurrentPath() + title);
                if (myClient.isViewingEpisodes()) {
                    posterPath = myClient.getCurrentPath().toString();
                    movieListAdapter.getOriginalMovieList().forEach(movieInfo -> {
                        if(movieInfo.getTitle().equals(title) ||
                                Integer.valueOf(movieInfo.getTitle().split(" ")[1].split("\\.")[0])
                                        .compareTo(Integer.valueOf(title.split(" ")[1].split("\\.")[0])) > 0){
                            titles.add(movieInfo.getTitle());
                        }
                    });
                }
                else {
                    posterPath = myClient.getVideoPath();
                    titles.add(title);
                }

                MediaQueueItem[] queueItems = CastControl.assembleMediaQueue(titles, posterPath, myClient);

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
                requestTitles();
            }
        });
    }

    private Client getPhoneInfo() throws Exception {
        ObjectInputStream objectInputStream = new ObjectInputStream(openFileInput("setup.txt"));
        Client client = (Client) objectInputStream.readObject();
        objectInputStream.close();
        client.resetCurrentPath();
        return client;
    }

    private void requestTitles(){
        if(movieInfoCache.containsKey(myClient.getCurrentPath().toString())){
            movieListAdapter.clearLists();
            movieListAdapter.updateList(movieInfoCache.get(myClient.getCurrentPath().toString()));
            movieListAdapter.notifyDataSetChanged();
        }else {
            executorService.submit(new MovieInfoLoader(progressBar, movieListAdapter, myClient, movieInfoCache, this));
        }
    }

    private void updateAccessToken(boolean quiet){
        if(!quiet)
            Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        executorService.submit(new KeycloakAuthenticator(myClient));
    }

    @Override
    public void onBackPressed() {
        String currentDirectory = myClient.getCurrentPath().peekLast();
        if (currentDirectory.toLowerCase().equals("series") | currentDirectory.toLowerCase().equals("movies"))
            System.exit(8);

        myClient.popOneDirectory();
        requestTitles();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
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
        }
        return true;
    }
}