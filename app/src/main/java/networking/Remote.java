package networking;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import activity.MainActivity;
import rahmnathan.localmovies.R;

public class Remote extends Activity {

    private enum controls {
        VOLUME_UP, VOLUME_DOWN, SEEK_FORWARD, SEEK_BACK, PLAY_PAUSE, STOP
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Setting up all of our buttons

        final Button play = (Button) findViewById(R.id.play);
        final Button seekForward = (Button) findViewById(R.id.seekForward);
        final Button seekBack = (Button) findViewById(R.id.seekBack);
        final Button volumeUp = (Button) findViewById(R.id.volumeUp);
        final Button volumeDown = (Button) findViewById(R.id.volumeDown);
        final Button stop = (Button) findViewById(R.id.stop);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    sendControl(controls.PLAY_PAUSE.name());
            }
        });

        seekBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendControl(controls.SEEK_BACK.name());

            }
        });

        seekForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendControl(controls.SEEK_FORWARD.name());
            }
        });

        volumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendControl(controls.VOLUME_UP.name());
            }
        });

        volumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendControl(controls.VOLUME_DOWN.name());
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View View) {
                sendControl(controls.STOP.name());
            }
        });
    }

    private void sendControl(String command) {

        int portNum = 3995;
        String[] commandArray = {command, MainActivity.myPhone.getPhoneName()};

        try {
            Socket socket = new Socket(MainActivity.myPhone.getComputerIP(), portNum);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

            objectOutputStream.writeObject(commandArray);

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}