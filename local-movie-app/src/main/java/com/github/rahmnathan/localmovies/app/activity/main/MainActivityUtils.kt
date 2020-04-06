package com.github.rahmnathan.localmovies.app.activity.main

import android.view.MenuItem
import android.widget.GridView
import com.github.rahmnathan.localmovies.app.activity.main.view.MediaListAdapter
import com.github.rahmnathan.localmovies.app.media.data.MediaGenre
import com.github.rahmnathan.localmovies.app.media.data.MediaOrder
import rahmnathan.localmovies.R

object MainActivityUtils {
    @JvmStatic
    fun sortVideoList(item: MenuItem, listAdapter: MediaListAdapter, gridView: GridView) {
        when (item.itemId) {
            R.id.order_date_added -> sort(MediaOrder.DATE_ADDED, listAdapter, gridView)
            R.id.order_year -> sort(MediaOrder.RELEASE_YEAR, listAdapter, gridView)
            R.id.order_rating -> sort(MediaOrder.RATING, listAdapter, gridView)
            R.id.order_title -> sort(MediaOrder.TITLE, listAdapter, gridView)
            R.id.genre_comedy -> filterGenre(MediaGenre.COMEDY, listAdapter, gridView)
            R.id.action_action -> filterGenre(MediaGenre.ACTION, listAdapter, gridView)
            R.id.genre_sciFi -> filterGenre(MediaGenre.SCIFI, listAdapter, gridView)
            R.id.genre_horror -> filterGenre(MediaGenre.HORROR, listAdapter, gridView)
            R.id.genre_thriller -> filterGenre(MediaGenre.THRILLER, listAdapter, gridView)
            R.id.genre_fantasy -> filterGenre(MediaGenre.FANTASY, listAdapter, gridView)
        }
    }

    private fun sort(order: MediaOrder, listAdapter: MediaListAdapter, gridView: GridView) {
        listAdapter.sort(order)
        gridView.smoothScrollToPosition(0)
    }

    private fun filterGenre(genre: MediaGenre, listAdapter: MediaListAdapter, gridView: GridView) {
        listAdapter.filterGenre(genre)
        gridView.smoothScrollToPosition(0)
    }
}