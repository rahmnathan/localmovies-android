package appmain;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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
import com.phoneinfo.Phone;
import com.restclient.RestClient;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.rahmnathan.MovieInfo;

import java.util.ArrayList;
import java.util.List;

import rahmnathan.localmovies.R;
import appsetup.Setup;
import appmain.ThreadManager.SERVER_CALL;

public class MainActivity extends AppCompatActivity {

    public static MovieListAdapter movieListAdapter;
    public static Phone myPhone;
    private static final RestClient REST_CLIENT = new RestClient();
    public static final List<MovieInfo> MOVIE_INFO_LIST = new ArrayList<>();
    public static ProgressBar progressBar;
    CastContext castContext;


    public static LoadingCache<String, List<MovieInfo>> movieInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        castContext = CastContext.getSharedInstance(this);

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        movieInfo =
                CacheBuilder.newBuilder()
                        .maximumSize(250)
                        .build(
                                new CacheLoader<String, List<MovieInfo>>() {
                                    @Override
                                    public List<MovieInfo> load(String currentPath) {
                                        return REST_CLIENT.getMovieInfo(myPhone);
                                    }
                                });

        // Getting phone info and Triggering initial getMovieInfo of titles from server

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        try {
            myPhone = new Setup().getPhoneInfo(myPhone, this);
            new ThreadManager(SERVER_CALL.GET_TITLES, "Movies").start();
        } catch(NullPointerException e){
            startActivity(new Intent(MainActivity.this, Setup.class));
        }

        Button controls = (Button) findViewById(R.id.controls);
        controls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ExpandedControlActivity.class));
            }
        });

        Button series = (Button) findViewById(R.id.series);
        series.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myPhone.setCurrentPath(myPhone.getMainPath());

                // Requesting series list and updating listview

                new ThreadManager(SERVER_CALL.GET_TITLES, "Series").start();
            }
        });

        Button movies = (Button) findViewById(R.id.movies);
        movies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myPhone.setCurrentPath(myPhone.getMainPath());

                // Requesting movie list and updating listview

                new ThreadManager(SERVER_CALL.GET_TITLES, "Movies").start();
            }
        });

        movieListAdapter = new MovieListAdapter(this, MOVIE_INFO_LIST);

        final GridView movieList = (GridView) findViewById(R.id.gridView);
        movieList.setAdapter(movieListAdapter);

        EditText searchText = (EditText) findViewById(R.id.searchText);
        searchText.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // Filtering list with user input

                movieListAdapter.getFilter().filter(cs);
            }
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
            @Override
            public void afterTextChanged(Editable arg0) {}
        });

        movieList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String title = movieListAdapter.movies.get(position).toString();

                if (myPhone.getCurrentPath().toLowerCase().contains("season") ||
                        myPhone.getCurrentPath().toLowerCase().contains("movies")) {
                    REST_CLIENT.refresh(myPhone);
                    /*
                     If we're viewing movies or episodes we
                     play the movie and start our Remote activity
                   */
                    myPhone.setVideoPath(myPhone.getCurrentPath() + title);
                    MediaMetadata metaData = new MediaMetadata();
                    if(myPhone.getCurrentPath().toLowerCase().contains("season")) {
                        metaData.addImage(new WebImage(Uri.parse("https://" + myPhone.getComputerIP()
                                + ":8443/poster?access_token=" + myPhone.getAccessToken() + "&path="
                                + myPhone.getCurrentPath() + "&title=" + title)));
                    } else {
                        metaData.addImage(new WebImage(Uri.parse("https://" + myPhone.getComputerIP()
                                + ":8443/poster?access_token=" + myPhone.getAccessToken() + "&path="
                                + myPhone.getVideoPath() + "&title=" + title)));
                    }

                    metaData.putString(MediaMetadata.KEY_TITLE, title.substring(0, title.length()-4));
                    String url = "https://" + myPhone.getComputerIP() + ":8443/video.mp4?access_token="
                            + myPhone.getAccessToken() + "&path=" + myPhone.getVideoPath();
                    MediaInfo mediaInfo = new MediaInfo.Builder(url)
                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                            .setContentType("videos/mp4")
                            .setMetadata(metaData)
                            .build();

                    try {
                        CastSession session = castContext.getSessionManager().getCurrentCastSession();
                        RemoteMediaClient remoteMediaClient = session.getRemoteMediaClient();
                        remoteMediaClient.load(mediaInfo, true, 0);
                        Toast.makeText(MainActivity.this, "Casting" , Toast.LENGTH_LONG).show();
                    } catch (Exception e){
                        Intent intent = new Intent(MainActivity.this, VideoPlayer.class);
                        intent.putExtra("url", url);
                        startActivity(intent);
                    }
                } else {
                    new ThreadManager(SERVER_CALL.GET_TITLES, title).start();
                }
            }
        });
    }

    @Override
    public void onBackPressed(){
        String currentPath = myPhone.getCurrentPath();
        if(currentPath.endsWith("Series/") | currentPath.endsWith("Movies/")){
            System.exit(8);
        } else{
            String newPath = "";
            String[] pathSplit = currentPath.split("/");
            String title = pathSplit[pathSplit.length - 2];
            for(int x = 0; x<pathSplit.length - 2; x++){
                newPath = newPath + pathSplit[x] + "/";
            }
            myPhone.setCurrentPath(newPath);
            new ThreadManager(SERVER_CALL.GET_TITLES, title).start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),
                menu,
                R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, Setup.class));
                break;
        }
        return true;
    }
}