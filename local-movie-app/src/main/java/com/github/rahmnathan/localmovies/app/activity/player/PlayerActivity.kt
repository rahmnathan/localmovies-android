package com.github.rahmnathan.localmovies.app.activity.player

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import rahmnathan.localmovies.R

class PlayerActivity : Activity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_player)

        val videoView = findViewById<VideoView>(R.id.VideoView)
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        val b = intent.extras
        videoView.setVideoURI(Uri.parse(b!!.getString("url")))
        videoView.start()
    }
}