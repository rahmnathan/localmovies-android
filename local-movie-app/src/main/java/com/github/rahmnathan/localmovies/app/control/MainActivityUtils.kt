package com.github.rahmnathan.localmovies.app.control

import android.view.MenuItem
import android.widget.GridView
import com.github.rahmnathan.localmovies.app.adapter.list.MediaListAdapter
import com.github.rahmnathan.localmovies.app.data.Client
import com.github.rahmnathan.localmovies.app.data.MovieGenre
import com.github.rahmnathan.localmovies.app.data.MovieOrder
import rahmnathan.localmovies.R
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream

object MainActivityUtils {
    @JvmStatic
    fun sortVideoList(item: MenuItem, listAdapter: MediaListAdapter, gridView: GridView) {
        when (item.itemId) {
            R.id.order_date_added -> sort(MovieOrder.DATE_ADDED, listAdapter, gridView)
            R.id.order_year -> sort(MovieOrder.RELEASE_YEAR, listAdapter, gridView)
            R.id.order_rating -> sort(MovieOrder.RATING, listAdapter, gridView)
            R.id.order_title -> sort(MovieOrder.TITLE, listAdapter, gridView)
            R.id.genre_comedy -> filterGenre(MovieGenre.COMEDY, listAdapter, gridView)
            R.id.action_action -> filterGenre(MovieGenre.ACTION, listAdapter, gridView)
            R.id.genre_sciFi -> filterGenre(MovieGenre.SCIFI, listAdapter, gridView)
            R.id.genre_horror -> filterGenre(MovieGenre.HORROR, listAdapter, gridView)
            R.id.genre_thriller -> filterGenre(MovieGenre.THRILLER, listAdapter, gridView)
            R.id.genre_fantasy -> filterGenre(MovieGenre.FANTASY, listAdapter, gridView)
        }
    }

    private fun sort(order: MovieOrder, listAdapter: MediaListAdapter, gridView: GridView) {
        listAdapter.sort(order)
        gridView.smoothScrollToPosition(0)
    }

    private fun filterGenre(genre: MovieGenre, listAdapter: MediaListAdapter, gridView: GridView) {
        listAdapter.filterGenre(genre)
        gridView.smoothScrollToPosition(0)
    }
}