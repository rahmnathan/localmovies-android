package appmain;

import android.content.Intent;
import android.net.Uri;
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

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;
import com.localmovies.KeycloakAuthenticator;
import com.localmovies.client.Client;
import com.localmovies.provider.data.MovieInfo;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rahmnathan.localmovies.R;
import appsetup.Setup;

public class MainActivity extends AppCompatActivity {
    private MovieListAdapter movieListAdapter;
    private Client myClient;
    private ProgressBar progressBar;
    private CastContext castContext;
    private final ConcurrentMap<String, List<MovieInfo>> movieInfoCache = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        castContext = CastContext.getSharedInstance(this);

        // Getting phone info and Triggering initial request of titles from server

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        movieListAdapter = new MovieListAdapter(this, new ArrayList<>());
        GridView movieListView = (GridView) findViewById(R.id.gridView);
        movieListView.setAdapter(movieListAdapter);

        try {
            myClient = getPhoneInfo();
            myClient.appendToCurrentPath("Movies");
            updateAccessToken();
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

        movieListView.setOnItemClickListener((parent, view, position, id) -> {
            String title = movieListAdapter.getTitle(position);

            if (myClient.isViewingVideos()) {
                    /*
                     If we're viewing movies or episodes we refresh our key and start the movie
                   */
                updateAccessToken();
                myClient.setVideoPath(myClient.getCurrentPath() + title);
                String posterPath;
                if (myClient.isViewingEpisodes())
                    posterPath = myClient.getCurrentPath().toString();
                else
                    posterPath = myClient.getVideoPath();

                MediaMetadata metaData = new MediaMetadata();
                metaData.addImage(new WebImage(Uri.parse("https://" + myClient.getComputerIP()
                        + ":8443/poster?access_token=" + myClient.getAccessToken() + "&path="
                        + posterPath + "&title=" + title)));

                metaData.putString(MediaMetadata.KEY_TITLE, title.substring(0, title.length() - 4));
                String url = "https://" + myClient.getComputerIP() + ":8443/video.mp4?access_token="
                        + myClient.getAccessToken() + "&path=" + myClient.getVideoPath();
                MediaInfo mediaInfo = new MediaInfo.Builder(url)
                        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                        .setContentType("videos/mp4")
                        .setMetadata(metaData)
                        .build();

                try {
                    CastSession session = castContext.getSessionManager().getCurrentCastSession();
                    RemoteMediaClient remoteMediaClient = session.getRemoteMediaClient();
                    remoteMediaClient.load(mediaInfo);
                    Toast.makeText(MainActivity.this, "Casting", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Intent intent = new Intent(MainActivity.this, VideoPlayer.class);
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

    private void updateAccessToken(){
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