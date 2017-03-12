package appmain;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import rahmnathan.localmovies.R;

public class VideoPlayer extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player);

        VideoView videoView = (VideoView)findViewById(R.id.VideoView);
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        Bundle b = getIntent().getExtras();
        videoView.setVideoURI(Uri.parse(b.getString("url")));

        videoView.start();
    }
}