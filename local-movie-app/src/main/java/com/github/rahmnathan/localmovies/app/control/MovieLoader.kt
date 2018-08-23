package com.github.rahmnathan.localmovies.app.control

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.Secure.getString
import android.widget.Toast

import com.github.rahmnathan.localmovies.app.adapter.list.MovieListAdapter
import com.github.rahmnathan.localmovies.app.data.Client
import com.github.rahmnathan.localmovies.app.adapter.external.localmovie.MovieFacade
import com.github.rahmnathan.localmovies.app.data.Movie
import com.github.rahmnathan.localmovies.app.data.MovieRequest
import com.google.firebase.iid.FirebaseInstanceId

import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger

class MovieLoader internal constructor(private val movieListAdapter: MovieListAdapter, private val client: Client,
                                       private val persistenceManager: MoviePersistenceManager, private val context: Context) : Runnable {
    private val logger = Logger.getLogger(MovieLoader::class.java.name)
    private val UIHandler = Handler(Looper.getMainLooper())
    private val movieFacade = MovieFacade()
    @Volatile
    var isRunning = true
        private set
    private val deviceId: String = getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    override fun run() {
        logger.log(Level.INFO, "Dynamically loading titles")

        val token = FirebaseInstanceId.getInstance().token

        if (client.accessToken == null) {
            UIHandler.post { Toast.makeText(context, "Login failed - Check credentials", Toast.LENGTH_LONG).show() }
            return
        }

        movieListAdapter.clearLists()
        var page = 0
        do {
            val movieRequest = MovieRequest(
                    page = page,
                    resultsPerPage = ITEMS_PER_PAGE,
                    deviceId = deviceId,
                    path = client.currentPath.toString(),
                    pushToken = token.orEmpty()
            )

            val infoList = movieFacade.getMovieInfo(client, movieRequest)

            if (!isRunning) break

            movieListAdapter.updateList(infoList)
            UIHandler.post { movieListAdapter.notifyDataSetChanged() }
            if (movieListAdapter.chars != "") {
                UIHandler.post { movieListAdapter.filter.filter(movieListAdapter.chars) }
            }

            page++
        } while (page <= client.movieCount / ITEMS_PER_PAGE)

        if (isRunning) {
            persistenceManager.addAll(client.currentPath.toString(), ArrayList(movieListAdapter.originalMovieList))
        }

        isRunning = false
    }

    fun terminate() {
        isRunning = false
    }

    companion object {
        private val ITEMS_PER_PAGE = 30
    }
}