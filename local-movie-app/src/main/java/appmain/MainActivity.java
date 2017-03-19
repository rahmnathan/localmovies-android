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
import com.phoneinfo.LocalMediaPath;
import com.phoneinfo.Phone;
import com.rahmnathan.MovieInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rahmnathan.localmovies.R;
import appsetup.Setup;

public class MainActivity extends AppCompatActivity {
    private MovieListAdapter movieListAdapter;
    private Phone myPhone;
    private List<MovieInfo> movieInfoList;
    private ProgressBar progressBar;
    private CastContext castContext;
    private ConcurrentMap<String, List<MovieInfo>> movieInfoCache = new ConcurrentHashMap<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        castContext = CastContext.getSharedInstance(this);
        movieInfoList = new ArrayList<>();

        // Getting phone info and Triggering initial request of titles from server

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        movieListAdapter = new MovieListAdapter(this, movieInfoList);
        GridView movieListView = (GridView) findViewById(R.id.gridView);
        movieListView.setAdapter(movieListAdapter);

        try {
            myPhone = getPhoneInfo();
            myPhone.appendToCurrentPath("Movies");
            requestTitles();
        } catch (Exception e) {
            startActivity(new Intent(MainActivity.this, Setup.class));
        }

        Button controls = (Button) findViewById(R.id.controls);
        controls.setOnClickListener((view) -> startActivity(new Intent(MainActivity.this, ExpandedControlActivity.class)));

        Button series = (Button) findViewById(R.id.series);
        series.setOnClickListener((view) -> {
            myPhone.resetCurrentPath();
            myPhone.appendToCurrentPath("Series");
            requestTitles();
        });
        Button movies = (Button) findViewById(R.id.movies);
        movies.setOnClickListener((view) -> {
            myPhone.resetCurrentPath();
            myPhone.appendToCurrentPath("Movies");
            requestTitles();
        });

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

        movieListView.setOnItemClickListener((parent, view, position, id) -> {
            String title = movieListAdapter.getTitle(position);

            if (myPhone.isViewingVideos()) {
                    /*
                     If we're viewing movies or episodes we
                     refresh our key and start the movie
                   */
                requestToken();
                myPhone.setVideoPath(myPhone.getCurrentPath() + title);
                String videoPath;
                if (myPhone.isViewingEpisodes())
                    videoPath = myPhone.getCurrentPath().toString();
                else
                    videoPath = myPhone.getVideoPath();

                MediaMetadata metaData = new MediaMetadata();
                metaData.addImage(new WebImage(Uri.parse("https://" + myPhone.getComputerIP()
                        + ":8443/poster?access_token=" + myPhone.getAccessToken() + "&path="
                        + videoPath + "&title=" + title)));

                metaData.putString(MediaMetadata.KEY_TITLE, title.substring(0, title.length() - 4));
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
                    remoteMediaClient.load(mediaInfo);
                    Toast.makeText(MainActivity.this, "Casting", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Intent intent = new Intent(MainActivity.this, VideoPlayer.class);
                    intent.putExtra("url", url);
                    startActivity(intent);
                }
            } else {
                myPhone.appendToCurrentPath(title);
                requestTitles();
            }
        });
    }

    private Phone getPhoneInfo() throws Exception {
        ObjectInputStream objectInputStream = new ObjectInputStream(openFileInput("setup.txt"));
        Phone phone = (Phone) objectInputStream.readObject();
        objectInputStream.close();
        phone.resetCurrentPath();
        return phone;
    }

    private void requestTitles(){
        if(movieInfoCache.containsKey(myPhone.getCurrentPath().toString())){
            movieInfoList.clear();
            movieInfoList.addAll(movieInfoCache.get(myPhone.getCurrentPath().toString()));
            movieListAdapter.notifyDataSetChanged();
        }else {
            executorService.submit(new HttpRequestRunnable(progressBar, movieListAdapter, myPhone, movieInfoList,
                    HttpRequestRunnable.Task.TITLE_REQUEST, movieInfoCache));
        }
    }

    private void requestToken(){
        executorService.submit(new HttpRequestRunnable(progressBar, movieListAdapter, myPhone, movieInfoList,
                HttpRequestRunnable.Task.TOKEN_REFRESH, movieInfoCache));
    }

    @Override
    public void onBackPressed() {
        String currentDirectory = myPhone.getCurrentPath().peekLast();
        if (currentDirectory.equals("Series") | currentDirectory.equals("Movies"))
            System.exit(8);

        myPhone.popOneDirectory();
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
        }
        return true;
    }
}