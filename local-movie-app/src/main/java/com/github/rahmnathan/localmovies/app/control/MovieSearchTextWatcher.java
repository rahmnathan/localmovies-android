package com.github.rahmnathan.localmovies.app.control;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.GridView;

import com.github.rahmnathan.localmovies.app.adapter.list.MovieListAdapter;

public class MovieSearchTextWatcher implements TextWatcher {
    private final MovieListAdapter listAdapter;

    public MovieSearchTextWatcher(MovieListAdapter listAdapter) {
        this.listAdapter = listAdapter;
    }

    public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
        listAdapter.getFilter().filter(cs);
    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    @Override
    public void afterTextChanged(Editable arg0) {
    }
}
