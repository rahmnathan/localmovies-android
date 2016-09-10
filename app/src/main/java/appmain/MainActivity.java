package appmain;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.phoneinfo.Phone;
import com.restclient.RestClient;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.rahmnathan.MovieInfo;
import com.movieinfoprovider.OMDBMovieInfoProvider;

import java.util.ArrayList;
import java.util.List;

import appsetup.ServerDiscoverer;
import rahmnathan.localmovies.R;
import appremote.Remote;
import appsetup.Setup;

public class MainActivity extends AppCompatActivity {

    public static MovieListAdapter myAdapter;
    public static Phone myPhone;
    private static final RestClient REST_CLIENT = new RestClient();
    public static final List<MovieInfo> movieList = new ArrayList<>();
    private static final OMDBMovieInfoProvider MOVIE_INFO_PROVIDER = new OMDBMovieInfoProvider();
    public static ProgressBar progressBar;

    public static LoadingCache<String, List<MovieInfo>> movieInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        movieInfo =
                CacheBuilder.newBuilder()
                        .maximumSize(100)
                        .build(
                                new CacheLoader<String, List<MovieInfo>>() {
                                    @Override
                                    public List<MovieInfo> load(String currentPath) {
                                        return MOVIE_INFO_PROVIDER.getMovieInfo(REST_CLIENT.requestTitles(myPhone),
                                                currentPath, MainActivity.this.getFilesDir().toString());
                                    }
                                });

        // Getting phone info and Triggering initial requestTitles of titles from server

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        try {
            myPhone = new Setup().getPhoneInfo(myPhone, this);
            new ServerDiscoverer(myPhone, this).start();

        } catch(NullPointerException e){
            startActivity(new Intent(MainActivity.this, Setup.class));
        }

        // Creating buttons for controls, setup, series, and movies

        Button controls = (Button) findViewById(R.id.controls);
        controls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Remote.class));
            }
        });

        Button setup = (Button) findViewById(R.id.setup);
        setup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Setup.class));
            }
        });

        Button series = (Button) findViewById(R.id.series);
        series.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myPhone.setPath(myPhone.getMainPath());

                // Requesting series list and updating listview

                new ThreadManager("GetTitles", "Series", MainActivity.this).start();
            }
        });

        Button movies = (Button) findViewById(R.id.movies);
        movies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myPhone.setPath(myPhone.getMainPath());

                // Requesting movie list and updating listview

                new ThreadManager("GetTitles", "Movies", MainActivity.this).start();
            }
        });

        // Creating ListAdapter and listView to display titles

        myAdapter = new MovieListAdapter(this, movieList);

        final ListView movieList = (ListView) findViewById(R.id.listView);
        movieList.setAdapter(myAdapter);

        // Setting up our search box with a text change listener

        EditText searchText = (EditText) findViewById(R.id.searchText);
        searchText.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // Filtering list with user input

                myAdapter.getFilter().filter(cs);
            }
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
            @Override
            public void afterTextChanged(Editable arg0) {}
        });

        movieList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String title = MovieListAdapter.movies.get(position).toString();

                if (myPhone.getPath().toLowerCase().contains("season") ||
                        myPhone.getPath().toLowerCase().contains("movies")) {
                    /*
                     If we're viewing movies or episodes we
                     play the movie and start our Remote activity
                   */
                    new ThreadManager("PlayMovie", title, MainActivity.this).start();
                    Toast.makeText(MainActivity.this, "Casting", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, Remote.class));
                } else {

                    new ThreadManager("GetTitles", title, MainActivity.this).start();
                }
            }
        });
    }

    @Override
    public void onBackPressed(){
        String currentPath = myPhone.getPath();
        if(currentPath.endsWith("Series/") | currentPath.endsWith("Movies/")){
            System.exit(8);
        } else{
            String newPath = "";
            String[] pathSplit = currentPath.split("/");
            String title = pathSplit[pathSplit.length - 2];
            for(int x = 0; x<pathSplit.length - 2; x++){
                newPath = newPath + pathSplit[x] + "/";
            }
            myPhone.setPath(newPath);
            new ThreadManager("GetTitles", title, this).start();
        }
    }

}