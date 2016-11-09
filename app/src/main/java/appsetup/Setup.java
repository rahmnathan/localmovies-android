package appsetup;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.phoneinfo.Phone;

import appmain.ThreadManager;
import appmain.MainActivity;
import rahmnathan.localmovies.R;
import appmain.ThreadManager.SERVER_CALL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Setup extends Activity {
    private EditText server;
    private EditText path;

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

        server = (EditText) findViewById(R.id.ServerIP);
        server.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DARKEN);
        path = (EditText) findViewById(R.id.inputPath);
        path.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DARKEN);

        // Populating our textfields with our saved data if it exists

        try {
            server.setText(MainActivity.myPhone.getComputerIP());
            path.setText(MainActivity.myPhone.getMainPath());
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        Button set = (Button) findViewById(R.id.set);
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData(server.getText().toString(), path.getText().toString());

                startActivity(new Intent(Setup.this, MainActivity.class));

            }
        });

        Button refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                new ThreadManager(SERVER_CALL.REFRESH, "Refresh").start();
                Intent intent = new Intent(Setup.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private void saveData(String server, String path){
        try {
            // Navigating to setup file and writing data to it

            File setupFile = new File(this.getFilesDir(), "setup.txt");
            if (!setupFile.exists()){
                setupFile.createNewFile();
            }

            Phone myPhone = new Phone(path);
            myPhone.setComputerIP(server);

            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(setupFile));
            os.writeObject(myPhone);
            os.close();

            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Phone getPhoneInfo(Phone myPhone, Context context) {

        File setupFile = new File(context.getFilesDir(), "setup.txt");

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(setupFile));
            myPhone = (Phone) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        myPhone.setCurrentPath(myPhone.getMainPath());

        return myPhone;
    }
}

