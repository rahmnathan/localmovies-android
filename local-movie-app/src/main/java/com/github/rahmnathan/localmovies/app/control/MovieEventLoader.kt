package com.github.rahmnathan.localmovies.app.control

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.github.rahmnathan.localmovies.app.activity.SetupActivity.Companion.saveData
import com.github.rahmnathan.localmovies.app.adapter.external.localmovie.MovieFacade.getMovieEventCount
import com.github.rahmnathan.localmovies.app.adapter.external.localmovie.MovieFacade.getMovieEvents
import com.github.rahmnathan.localmovies.app.adapter.list.MovieListAdapter
import com.github.rahmnathan.localmovies.app.control.MediaPathUtils.getParentPath
import com.github.rahmnathan.localmovies.app.data.Client
import com.github.rahmnathan.localmovies.app.data.MovieEvent
import java.util.*
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger

class MovieEventLoader(private val movieListAdapter: MovieListAdapter, private val client: Client, private val persistenceManager: MoviePersistenceManager, private val context: Context) : Runnable {
    private val logger = Logger.getLogger(MovieEventLoader::class.java.name)
    private val UIHandler = Handler(Looper.getMainLooper())

    override fun run() {
        logger.log(Level.INFO, "Dynamically loading events.")
        if (client.accessToken == null) {
            UIHandler.post { Toast.makeText(context, "Login failed - Check credentials", Toast.LENGTH_LONG).show() }
            return
        }
        val count = getMovieEventCount(client)
        if (!count.isPresent) {
            logger.severe("Error retrieving media event count.")
            return
        }

        for (page in 0..count.get() / ITEMS_PER_PAGE) {
            val events = getMovieEvents(client, page.toInt(), ITEMS_PER_PAGE)
            events.forEach(Consumer { event: MovieEvent ->
                logger.info("Found media event: $event")
                if (event.event.equals("CREATE", ignoreCase = true)) {
                    val media = event.media
                    persistenceManager.deleteMovie(event.relativePath)
                    persistenceManager.addOne(getParentPath(event.relativePath), media!!)
                    movieListAdapter.clearLists()
                    movieListAdapter.updateList(persistenceManager.getMoviesAtPath(client.currentPath.toString()).orElse(ArrayList()))
                    UIHandler.post { movieListAdapter.notifyDataSetChanged() }
                } else {
                    persistenceManager.deleteMovie(event.relativePath)
                    movieListAdapter.clearLists()
                    movieListAdapter.updateList(persistenceManager.getMoviesAtPath(client.currentPath.toString()).orElse(ArrayList()))
                    UIHandler.post { movieListAdapter.notifyDataSetChanged() }
                }
            })
        }

        if (movieListAdapter.chars != "") {
            UIHandler.post { movieListAdapter.filter.filter(movieListAdapter.chars) }
        }

        client.lastUpdate = System.currentTimeMillis()
        saveData(client, context)
    }

    companion object {
        private const val ITEMS_PER_PAGE = 30
    }

}