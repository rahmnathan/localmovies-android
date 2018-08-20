package com.github.rahmnathan.localmovies.app.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.rahmnathan.localmovies.app.control.MovieDAO;
import com.github.rahmnathan.localmovies.app.control.MovieDatabase;
import com.github.rahmnathan.localmovies.app.data.Client;

import rahmnathan.localmovies.R;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetupActivity extends Activity {
    private static final Logger logger = Logger.getLogger(SetupActivity.class.getName());
    private static final String SETUP_FILE = "setup";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_main);
        ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
        );

        EditText userName = findViewById(R.id.userName);
        userName.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DARKEN);
        EditText password = findViewById(R.id.password);
        password.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DARKEN);
        EditText url = findViewById(R.id.url);
        url.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DARKEN);

        // Populating our text fields with our saved data if it exists
        Client client = getPhoneInfo();
        userName.setText(client.getUserName());
        password.setText(client.getPassword());
        url.setText(client.getComputerUrl());

        Button set = findViewById(R.id.set);
        set.setOnClickListener(view -> {
            Client client1 = new Client(url.getText().toString(), userName.getText().toString(), password.getText().toString());
            saveData(client1, this);

            startActivity(new Intent(SetupActivity.this, MainActivity.class));
        });

        Button clearMovies = findViewById(R.id.clearMovies);
        clearMovies.setOnClickListener(view -> CompletableFuture.runAsync(() -> {
            MovieDAO movieDAO = MovieDatabase.getDatabase(this).movieDAO();
            movieDAO.deleteAll();

            startActivity(new Intent(SetupActivity.this, MainActivity.class));
        }));
    }

    public static void saveData(Client client, Context context) {
        try (ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(SETUP_FILE, MODE_PRIVATE))) {
            os.writeObject(client);
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save client data", e);
        }
    }

    private Client getPhoneInfo() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(openFileInput(SETUP_FILE))) {
            return (Client) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Failed to get client data", e);
            return new Client();
        }
    }
}