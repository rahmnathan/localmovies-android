package com.github.rahmnathan.localmovies.app.activity.main.view

import androidx.appcompat.widget.SearchView
import com.github.rahmnathan.localmovies.app.activity.main.view.MediaListAdapter

class MediaSearchTextWatcher(private val listAdapter: MediaListAdapter) : SearchView.OnQueryTextListener {
    override fun onQueryTextSubmit(s: String): Boolean {
        return false
    }

    override fun onQueryTextChange(s: String): Boolean {
        listAdapter.filter.filter(s)
        return false
    }

}