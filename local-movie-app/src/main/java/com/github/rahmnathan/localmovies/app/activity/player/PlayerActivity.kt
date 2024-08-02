package com.github.rahmnathan.localmovies.app.activity.player

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import com.github.rahmnathan.localmovies.app.Client
import com.github.rahmnathan.localmovies.app.LocalMoviesApplication
import com.github.rahmnathan.localmovies.app.media.provider.control.MediaFacade
import rahmnathan.localmovies.R
import java.util.UUID
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

class PlayerActivity : Activity() {


    @Inject
    lateinit var mediaFacade: MediaFacade
    @Inject @Volatile lateinit var client: Client

    public override fun onCreate(savedInstanceState: Bundle?) {
        (application as LocalMoviesApplication).appComponent.inject(this)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.video_player)

        val videoView = findViewById<VideoView>(R.id.VideoView)
        val mediaController = MediaController(this)

        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        val b = intent.extras
        videoView.setVideoURI(Uri.parse(b!!.getString("url")))
        videoView.start()

        val uid = UUID.randomUUID().toString()

        CompletableFuture.runAsync {
            while (true) {
                Thread.sleep(5000)
                mediaFacade.saveProgress(
                    client,
                    b.getString("media-id"),
                    videoView.currentPosition.toString(),
                    uid
                )
            }
        }
    }
}