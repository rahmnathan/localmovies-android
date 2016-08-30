package remote;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import rahmnathan.localmovies.R;

public class Remote extends Activity {

    private enum controls {
        VOLUME_UP, VOLUME_DOWN, SEEK_FORWARD, SEEK_BACK, PLAY_PAUSE, STOP
    }

    public static volatile boolean repeat = false;

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
                new ViewPressRepeater(controls.PLAY_PAUSE.name()).start();
            }
        });

        seekBack.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        repeat = true;
                        new ViewPressRepeater(controls.SEEK_BACK.name()).start();
                        break;
                    case MotionEvent.ACTION_UP:
                        repeat = false;
                        break;
                }
                return true;
            }
        });

        seekForward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        repeat = true;
                        new ViewPressRepeater(controls.SEEK_FORWARD.name()).start();
                        break;
                    case MotionEvent.ACTION_UP:
                        repeat = false;
                        break;
                }
                return true;
            }
        });

        volumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ViewPressRepeater(controls.VOLUME_UP.name()).start();
            }
        });

        volumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ViewPressRepeater(controls.VOLUME_DOWN.name()).start();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View View) {
                new ViewPressRepeater(controls.STOP.name()).start();
            }
        });

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            event.startTracking();
            new ViewPressRepeater(controls.VOLUME_DOWN.name()).start();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            new ViewPressRepeater(controls.VOLUME_UP.name()).start();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}