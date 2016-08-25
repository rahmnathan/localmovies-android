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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;

import networking.Phone;
import networking.ServerDiscoverer;
import rahmnathan.localmovies.R;
import remote.Remote;
import setup.Setup;

public class MainActivity extends AppCompatActivity {

    public static ArrayAdapter ad;
    public static Phone myPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // Getting phone info and Triggering initial requestTitles of titles from server

        try {
            myPhone = new Setup().getPhoneInfo();
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

        ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        final ListView movieList = (ListView) findViewById(R.id.listView);
        movieList.setAdapter(ad);

        // Setting up our search box with a text change listener

        EditText searchText = (EditText) findViewById(R.id.searchText);
        searchText.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {

                // Filtering list with user input

                ad.getFilter().filter(cs);
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
                if (myPhone.getPath().toLowerCase().contains("season") ||
                        myPhone.getPath().toLowerCase().contains("movies")) {
                    /*
                     If we're viewing movies or episodes we
                     play the movie and start our Remote activity
                   */
                    myPhone.setPath(myPhone.getPath() + movieList.getItemAtPosition(position));
                    new ThreadManager("PlayMovie").start();
                    Toast.makeText(MainActivity.this, "Casting", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, Remote.class));
                } else {
                    myPhone.setPath(myPhone.getPath() + movieList.getItemAtPosition(position) + File.separator);

                    new ThreadManager("GetTitles").start();
                }
            }
        });
    }
}