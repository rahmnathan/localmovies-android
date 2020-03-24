package com.github.rahmnathan.localmovies.app.control

import androidx.appcompat.widget.SearchView
import com.github.rahmnathan.localmovies.app.adapter.list.MovieListAdapter

class MovieSearchTextWatcher(private val listAdapter: MovieListAdapter) : SearchView.OnQueryTextListener {
    override fun onQueryTextSubmit(s: String): Boolean {
        return false
    }

    override fun onQueryTextChange(s: String): Boolean {
        listAdapter.filter.filter(s)
        return false
    }

}