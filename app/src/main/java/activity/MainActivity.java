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

import networking.ClientOutput;
import networking.Phone;
import remote.Remote;
import networking.TriggerServer;
import setup.Setup;
import rahmnathan.localmovies.R;

public class MainActivity extends AppCompatActivity {

    public static ArrayAdapter ad;
    public static Phone myPhone;
    private final ClientOutput clientOutput = new ClientOutput();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // Getting phone info and Triggering initial send of titles from server

        try {
            myPhone = new Setup().getPhoneInfo();
        } catch(NullPointerException e){
            startActivity(new Intent(this, Setup.class));
        }

        new TriggerServer(myPhone, this).start();

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

                myPhone.setPath(myPhone.getMainPath() + "Series/");

                // Receiving series list and updating listview

                clientOutput.send(myPhone);
            }
        });

        Button movies = (Button) findViewById(R.id.movies);
        movies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                myPhone.setPath(myPhone.getMainPath() + "Movies/");

                // Receiving movie list and updating listview

                clientOutput.send(myPhone);
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

        // Setting click listener for list items

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
                    myPhone.setCasting(true);
                    clientOutput.send(myPhone);
                    Toast.makeText(MainActivity.this, "Casting", Toast.LENGTH_SHORT).show();
                    myPhone.setCasting(false);
                    startActivity(new Intent(MainActivity.this, Remote.class));
                } else {
                    myPhone.setPath(myPhone.getPath() + movieList.getItemAtPosition(position) + "/");
                    clientOutput.send(myPhone);
                }
            }
        });
    }
}