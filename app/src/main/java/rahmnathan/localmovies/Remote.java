package rahmnathan.localmovies;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

enum controls {
    VOLUME_UP, VOLUME_DOWN, SEEK_FORWARD, SEEK_BACK, PLAY_PAUSE, STOP
}

public class Remote extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Setting up all of our buttons

        Button play = (Button) findViewById(R.id.play);
        Button seekForward = (Button) findViewById(R.id.seekForward);
        Button seekBack = (Button) findViewById(R.id.seekBack);
        Button volumeUp = (Button) findViewById(R.id.volumeUp);
        Button volumeDown = (Button) findViewById(R.id.volumeDown);
        Button stop = (Button) findViewById(R.id.stop);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    sendControl(controls.PLAY_PAUSE.name());
                    System.out.println(controls.PLAY_PAUSE.toString());
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

        try {

            Socket socket = new Socket(MainActivity.myPhone.getComputerIP(), portNum);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

            bw.write(command + "splithere159" + MainActivity.myPhone.getPhoneName());

            bw.flush();
            socket.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}