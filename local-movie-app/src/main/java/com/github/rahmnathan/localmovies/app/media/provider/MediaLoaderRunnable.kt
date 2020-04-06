package com.github.rahmnathan.localmovies.app.media.provider

import android.os.Handler
import android.os.Looper

import com.github.rahmnathan.localmovies.app.activity.main.view.MediaListAdapter
import com.github.rahmnathan.localmovies.app.Client
import com.github.rahmnathan.localmovies.app.media.data.MediaRequest

import java.util.logging.Level
import java.util.logging.Logger

class MediaLoaderRunnable internal constructor(private val mediaListAdapter: MediaListAdapter,
                                               private val client: Client,
                                               private val mediaFacade: MediaFacade) : Runnable {
    private val logger = Logger.getLogger(MediaLoaderRunnable::class.java.name)
    private val UIHandler = Handler(Looper.getMainLooper())
    @Volatile var isRunning = true

    override fun run() {
        logger.log(Level.INFO, "Dynamically loading titles")

        mediaListAdapter.clearLists()
        var page = 0
        do {
            val movieRequest = MediaRequest(
                    page = page,
                    resultsPerPage = ITEMS_PER_PAGE,
                    path = client.currentPath.toString(),
                    order = "TITLE"
            )

            val infoList = mediaFacade.getMovieInfo(movieRequest)

            if (!isRunning) break

            mediaListAdapter.updateList(infoList)
            UIHandler.post { mediaListAdapter.notifyDataSetChanged() }
            if (mediaListAdapter.chars != "") {
                UIHandler.post { mediaListAdapter.filter.filter(mediaListAdapter.chars) }
            }

            page++
        } while (page <= client.movieCount!! / ITEMS_PER_PAGE)

        isRunning = false
    }

    fun terminate() {
        isRunning = false
    }

    companion object {
        private const val ITEMS_PER_PAGE = 30
    }
}