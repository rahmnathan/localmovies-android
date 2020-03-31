package com.github.rahmnathan.localmovies.app.control

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import com.github.rahmnathan.localmovies.app.adapter.external.localmovie.MediaFacade
import com.github.rahmnathan.localmovies.app.adapter.list.MediaListAdapter
import com.github.rahmnathan.localmovies.app.data.Client
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
        private val context: Context,
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
        if (optionalMovies.isPresent) {
            mediaListAdapter.clearLists()
            mediaListAdapter.updateList(optionalMovies.get())
            UIHandler.post { mediaListAdapter.notifyDataSetChanged() }
            UIHandler.post { progressBar.visibility = View.INVISIBLE }
        } else {
            UIHandler.post { progressBar.visibility = View.VISIBLE }
            mediaLoaderRunnable = MediaLoaderRunnable(mediaListAdapter, client, context, mediaFacade)

            CompletableFuture.runAsync(mediaLoaderRunnable, executorService)
                    .thenRun { UIHandler.post { progressBar.visibility = View.GONE } }
                    .thenRun { persistenceService.addAll(client.currentPath.toString(), ArrayList(mediaListAdapter.getOriginalMediaList()))}
                    .thenRun { FirebaseMessaging.getInstance().subscribeToTopic("movies")}
        }
    }
}