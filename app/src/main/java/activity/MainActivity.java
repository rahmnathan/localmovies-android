package activity;

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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import movieinfo.MovieInfoRetriever;
import movieinfo.MovieData;
import networking.Phone;
import networking.ServerDiscoverer;
import networking.ServerRequest;
import rahmnathan.localmovies.R;
import remote.Remote;
import setup.Setup;

public class MainActivity extends AppCompatActivity {

    public static CustomListAdapter myAdapter;
    public static Phone myPhone;
    private static final ServerRequest serverRequest = new ServerRequest();
    public static List<MovieData> movieList = new ArrayList<>();
    public static MovieInfoRetriever movieInfoRetriever = new MovieInfoRetriever();

    public static final LoadingCache<String, List<String>> titles =
            CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .build(
                            new CacheLoader<String, List<String>>() {
                                @Override
                                public List<String> load(String currentPath) {
                                    return serverRequest.requestTitles(myPhone);
                                }
                            });

    public static final LoadingCache<String, List<MovieData>> movieInfo =
            CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .build(
                            new CacheLoader<String, List<MovieData>>() {
                                @Override
                                public List<MovieData> load(String currentPath) {
                                    try {
                                        return movieInfoRetriever.getMovieData(titles.get(currentPath), currentPath);
                                    } catch (ExecutionException e){
                                        e.printStackTrace();
                                    }
                                    return null;
                                }
                            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // Getting phone info and Triggering initial requestTitles of titles from server

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        try {
            myPhone = new Setup().getPhoneInfo();
            new ServerDiscoverer(myPhone, this, progressBar).start();

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

                myPhone.setPath(myPhone.getMainPath() + "Series" + File.separator);

                // Requesting series list and updating listview

                new ThreadManager("GetTitles").start();
            }
        });

        Button movies = (Button) findViewById(R.id.movies);
        movies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                myPhone.setPath(myPhone.getMainPath() + "Movies" + File.separator);

                // Requesting movie list and updating listview

                new ThreadManager("GetTitles").start();
            }
        });

        // Creating ListAdapter and listView to display titles

        myAdapter = new CustomListAdapter(this, movieList);


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
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }
            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });

        movieList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                MovieData movieData = (MovieData) movieList.getItemAtPosition(position);

                if (myPhone.getPath().toLowerCase().contains("season") ||
                        myPhone.getPath().toLowerCase().contains("movies")) {
                    /*
                     If we're viewing movies or episodes we
                     play the movie and start our Remote activity
                   */
                    myPhone.setPath(myPhone.getPath() + movieData.getTitle());
                            new ThreadManager("PlayMovie").start();
                    Toast.makeText(MainActivity.this, "Casting", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, Remote.class));
                } else {
                    myPhone.setPath(myPhone.getPath() + movieData.getTitle() + File.separator);
                    System.out.println(movieList.getItemAtPosition(position).toString());

                    new ThreadManager("GetTitles").start();
                }
            }
        });
    }

    @Override
    public void onBackPressed(){
        String currentPath = myPhone.getPath();
        if(currentPath.endsWith("Series/") | currentPath.endsWith("Movies/")){
            System.exit(0);
        } else{
            String newPath = "/";
            String[] pathSplit = currentPath.split("/");
            for(int x = 0; x<pathSplit.length - 1; x++){
                newPath = newPath + pathSplit[x] + "/";
            }
            myPhone.setPath(newPath);
            new ThreadManager("GetTitles").start();
        }
    }

}