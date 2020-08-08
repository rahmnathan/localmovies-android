package com.github.rahmnathan.localmovies.app.media.provider.boundary

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import com.github.rahmnathan.localmovies.app.activity.main.view.MediaListAdapter
import com.github.rahmnathan.localmovies.app.Client
import com.github.rahmnathan.localmovies.app.media.provider.control.MediaFacade
import com.github.rahmnathan.localmovies.app.media.provider.control.MediaLoaderRunnable
import com.github.rahmnathan.localmovies.app.persistence.media.MediaPersistenceService
import com.google.firebase.messaging.FirebaseMessaging
import java.util.ArrayList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.logging.Logger

class MediaRepository(
        private val persistenceService: MediaPersistenceService,
        private val mediaListAdapter: MediaListAdapter,
        private val mediaFacade: MediaFacade,
        private val progressBar: ProgressBar,
        private val client: Client,
        private val executorService: ExecutorService){

    private val logger = Logger.getLogger(MediaRepository::class.java.name)
    private val UIHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var mediaLoaderRunnable: MediaLoaderRunnable? = null

    fun getVideos() {
        if (mediaLoaderRunnable != null && mediaLoaderRunnable!!.isRunning) {
            mediaLoaderRunnable!!.terminate()
        }

        val optionalMovies = persistenceService.getMoviesAtPath(client.currentPath.toString())
        if (optionalMovies.isNotEmpty()) {
            mediaListAdapter.clearLists()
            mediaListAdapter.updateList(optionalMovies)
            UIHandler.post { mediaListAdapter.notifyDataSetChanged() }
            UIHandler.post { progressBar.visibility = View.INVISIBLE }
        } else {
            UIHandler.post { progressBar.visibility = View.VISIBLE }
            mediaLoaderRunnable = MediaLoaderRunnable(mediaListAdapter, client, mediaFacade)

            CompletableFuture.runAsync(mediaLoaderRunnable, executorService)
                    .thenRun { UIHandler.post { progressBar.visibility = View.GONE } }
                    .thenRun { persistenceService.addAll(client.currentPath.toString(), ArrayList(mediaListAdapter.getOriginalMediaList()))}
                    .thenRun { FirebaseMessaging.getInstance().subscribeToTopic("movies")}
        }
    }
}