package com.github.rahmnathan.localmovies.app.control;

import androidx.appcompat.widget.SearchView;

import com.github.rahmnathan.localmovies.app.adapter.list.MovieListAdapter;

public class MovieSearchTextWatcher implements SearchView.OnQueryTextListener {
    private final MovieListAdapter listAdapter;

    public MovieSearchTextWatcher(MovieListAdapter listAdapter) {
        this.listAdapter = listAdapter;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        listAdapter.getFilter().filter(s);
        return false;
    }
}
