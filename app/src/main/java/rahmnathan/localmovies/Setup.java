package rahmnathan.localmovies;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by nathan on 3/4/16.
 */

public class Setup extends Activity {

    // These are our textfields

    private EditText chrome;
    private EditText phone;
    private EditText name;
    private EditText server;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_main);

        chrome = (EditText) findViewById(R.id.chrome);
        phone = (EditText) findViewById(R.id.phone);
        name = (EditText) findViewById(R.id.phoneName);
        server = (EditText) findViewById(R.id.ServerIP);

        // Populating our textfields with our saved data if it exists

        try {
            chrome.setText(MainActivity.myPhone.getCastIP());
            phone.setText(MainActivity.myPhone.getPhoneIP());
            name.setText(MainActivity.myPhone.getPhoneName());
            server.setText(MainActivity.myPhone.getComputerIP());
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        Button set = (Button) findViewById(R.id.set);
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData(chrome.getText().toString(), phone.getText().toString(),
                        name.getText().toString(), server.getText().toString());
            }
        });
    }

    private void saveData(String chrome, String phone, String name, String server){

        try {

            ActivityCompat.requestPermissions(
                    Setup.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );

            // Navigating to setup file and writing data to file with unique string between variables

            File setupFile = new File(Environment.getExternalStorageDirectory(), "setup.txt");
            if (!setupFile.exists()){
                setupFile.createNewFile();
            }
            
            FileWriter writer = new FileWriter(setupFile);
            writer.append(chrome + "splithere159" + phone +
                    "splithere159" + name + "splithere159" + server);
            writer.flush();
            writer.close();

            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

