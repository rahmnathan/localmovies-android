package appremote;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import appmain.MainActivity;
import appmain.ThreadManager;
import rahmnathan.localmovies.R;

public class Remote extends Activity {

    public static volatile boolean repeat = false;
    public static RemoteMediaClient mediaClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mediaClient = MainActivity.remoteMediaClient;

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
                if(mediaClient.isPlaying())
                    mediaClient.pause();
                else
                    mediaClient.play();
            }
        });

        seekBack.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // TODO - Implement seeking
                return true;
            }
        });

        seekForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                // TODO - Implement seeking
            }
        });

        volumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO - Implement volume controls
            }
        });

        volumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO - Implement volume controls
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View View) {
                mediaClient.stop();
            }
        });

    }
}