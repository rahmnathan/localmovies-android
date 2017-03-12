package appsetup;

import android.Manifest;
import android.app.Activity;
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

import appmain.MainActivity;
import rahmnathan.localmovies.R;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Setup extends Activity {
    private Phone phone;
    private EditText userName;
    private EditText password;

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

        // Populating our textfields with our saved data if it exists
        phone = getPhoneInfo();
        if(phone == null)
            phone = new Phone();
        try {
            userName.setText(phone.getUserName());
            password.setText(phone.getPassword());
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        Button set = (Button) findViewById(R.id.set);
        set.setOnClickListener((view)-> {
                saveData(userName.getText().toString(), password.getText().toString());

                startActivity(new Intent(Setup.this, MainActivity.class));

            });
    }

    private void saveData(String userName, String password){
        try {
            File setupFile = new File(this.getFilesDir(), "setup.txt");
            if (!setupFile.exists()){
                setupFile.createNewFile();
            }

            phone.setUserName(userName);
            phone.setPassword(password);
            ObjectOutputStream os = new ObjectOutputStream(openFileOutput("setup.txt", MODE_PRIVATE));
            os.writeObject(phone);
            os.close();

            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Phone getPhoneInfo() {
        Phone phone = new Phone();
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(openFileInput("setup.txt"));
            phone = (Phone) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        phone.setCurrentPath(phone.getMainPath());
        return phone;
    }
}

