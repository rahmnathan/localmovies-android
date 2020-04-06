package com.github.rahmnathan.localmovies.app.media.provider.event

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.github.rahmnathan.localmovies.app.activity.setup.SetupActivity.Companion.saveData
import com.github.rahmnathan.localmovies.app.media.provider.MediaFacade
import com.github.rahmnathan.localmovies.app.activity.main.view.MediaListAdapter
import com.github.rahmnathan.localmovies.app.media.MediaPathUtils.getParentPath
import com.github.rahmnathan.localmovies.app.Client
import com.github.rahmnathan.localmovies.app.media.data.MediaEvent
import com.github.rahmnathan.localmovies.app.persistence.media.MediaPersistenceService
import java.util.*
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger

class MediaEventLoader(private val mediaListAdapter: MediaListAdapter,
                       private val client: Client,
                       private val mediaFacade: MediaFacade,
                       private val persistenceService: MediaPersistenceService,
                       private val context: Context) : Runnable {
    private val logger = Logger.getLogger(MediaEventLoader::class.java.name)
    private val UIHandler = Handler(Looper.getMainLooper())

    override fun run() {
        logger.log(Level.INFO, "Dynamically loading events.")
        val count = mediaFacade.getMovieEventCount()
        if (!count.isPresent) {
            logger.severe("Error retrieving media event count.")
            return
        }

        for (page in 0..count.get() / ITEMS_PER_PAGE) {
            val events = mediaFacade.getMovieEvents(page.toInt(), ITEMS_PER_PAGE)
            events.forEach(Consumer { event: MediaEvent ->
                logger.info("Found media event: $event")
                if (event.event.equals("CREATE", ignoreCase = true)) {
                    val media = event.media
                    persistenceService.deleteMovie(event.relativePath)
                    persistenceService.addOne(getParentPath(event.relativePath), media!!)
                    mediaListAdapter.clearLists()
                    mediaListAdapter.updateList(persistenceService.getMoviesAtPath(client.currentPath.toString()).orElse(ArrayList()))
                    UIHandler.post { mediaListAdapter.notifyDataSetChanged() }
                } else {
                    persistenceService.deleteMovie(event.relativePath)
                    mediaListAdapter.clearLists()
                    mediaListAdapter.updateList(persistenceService.getMoviesAtPath(client.currentPath.toString()).orElse(ArrayList()))
                    UIHandler.post { mediaListAdapter.notifyDataSetChanged() }
                }
            })
        }

        if (mediaListAdapter.chars != "") {
            UIHandler.post { mediaListAdapter.filter.filter(mediaListAdapter.chars) }
        }

        client.lastUpdate = System.currentTimeMillis()
        saveData(client, context)
    }

    companion object {
        private const val ITEMS_PER_PAGE = 30
    }

}