package com.github.rahmnathan.localmovies.app.setup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.rahmnathan.localmovies.client.Client;

import com.github.rahmnathan.localmovies.app.main.MainActivity;
import rahmnathan.localmovies.R;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Setup extends Activity {
    private Client client;
    private EditText userName;
    private EditText password;
    private EditText url;

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

        userName = (EditText) findViewById(R.id.userName);
        userName.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DARKEN);
        password = (EditText) findViewById(R.id.password);
        password.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DARKEN);
        url = (EditText) findViewById(R.id.url);
        url.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DARKEN);

        // Populating our textfields with our saved data if it exists
        client = getPhoneInfo();
        if(client == null)
            client = new Client();
        try {
            userName.setText(client.getUserName());
            password.setText(client.getPassword());
            url.setText(client.getComputerUrl());
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        Button set = (Button) findViewById(R.id.set);
        set.setOnClickListener(view -> {
                saveData(userName.getText().toString(), password.getText().toString(), url.getText().toString());

                startActivity(new Intent(Setup.this, MainActivity.class));

            });
    }

    private void saveData(String userName, String password, String computerUrl){
        try {
            File setupFile = new File(this.getFilesDir(), "setup.txt");
            if (!setupFile.exists())
                setupFile.createNewFile();

            client.setUserName(userName);
            client.setPassword(password);
            client.setComputerUrl(computerUrl);
            ObjectOutputStream os = new ObjectOutputStream(openFileOutput("setup.txt", MODE_PRIVATE));
            os.writeObject(client);
            os.close();

            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Client getPhoneInfo() {
        Client client = new Client();
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(openFileInput("setup.txt"));
            client = (Client) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        client.resetCurrentPath();
        return client;
    }
}