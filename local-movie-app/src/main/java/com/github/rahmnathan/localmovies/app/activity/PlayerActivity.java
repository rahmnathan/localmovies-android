package com.github.rahmnathan.localmovies.app.activity;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import rahmnathan.localmovies.R;

public class PlayerActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player);

        VideoView videoView = findViewById(R.id.VideoView);
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        Bundle b = getIntent().getExtras();
        videoView.setVideoURI(Uri.parse(b.getString("url")));

        videoView.start();
    }
}