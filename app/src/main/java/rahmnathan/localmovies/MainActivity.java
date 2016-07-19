package rahmnathan.localmovies;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.util.ArrayList;
import java.util.List;

import Phone.Phone;

public class MainActivity extends AppCompatActivity {

    static ArrayAdapter ad;
    static Phone myPhone;

    public static Handler UIHandler;

    static List<String> titles = new ArrayList<>();

    private static final String mainPath = "/media/pi/MyPassport/Media/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        titles.add("Loading. . .");

        // Triggering initial send of titles from server

        new TriggerServer(mainPath, MainActivity.this).start();

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

                myPhone.setPath(mainPath + "Series/");

                // Receiving series list and updating listview

                new Server().send(myPhone);
            }
        });

        Button movies = (Button) findViewById(R.id.movies);
        movies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                myPhone.setPath(mainPath + "Movies/");

                // Receiving movie list and updating listview

                new Server().send(myPhone);
            }
        });

        // Creating ListAdapter and listView to display titles

        ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);
        final ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(ad);

        // Setting up our search box with a text change listener

        EditText searchText = (EditText) findViewById(R.id.searchText);
        searchText.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {

                // Filtering list with user input

                ad.getFilter().filter(cs);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // TODO Auto-generated method stub
            }
            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });

        // Setting click listener for list items

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (myPhone.getPath().toLowerCase().contains("season") ||
                        myPhone.getPath().toLowerCase().contains("movies")) {
                    /*
                     If we're viewing movies or episodes we
                     play the movie and start our Remote activity
                   */
                    myPhone.setPath(myPhone.getPath() + listView.getItemAtPosition(position));
                    myPhone.setCasting(true);
                    new Server().send(myPhone);
                    Toast.makeText(MainActivity.this, "Casting", Toast.LENGTH_SHORT).show();
                    myPhone.setCasting(false);
                    startActivity(new Intent(MainActivity.this, Remote.class));
                } else {
                    myPhone.setPath(myPhone.getPath() + listView.getItemAtPosition(position) + "/");
                    new Server().send(myPhone);
                }
            }
        }
        );
    }

    static {
        UIHandler = new Handler(Looper.getMainLooper());
    }

    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }

}