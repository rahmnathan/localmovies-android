package com.github.rahmnathan.localmovies.app.control;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.GridView;

import com.github.rahmnathan.localmovies.app.adapter.MovieListAdapter;

public class VideoSearchTextWatcher implements TextWatcher {
    private final MovieListAdapter listAdapter;
    private final GridView gridView;

    public VideoSearchTextWatcher(MovieListAdapter listAdapter, GridView gridView) {
        this.listAdapter = listAdapter;
        this.gridView = gridView;
    }

    public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
        listAdapter.getFilter().filter(cs);
        gridView.smoothScrollToPosition(0);
    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    @Override
    public void afterTextChanged(Editable arg0) {
    }
}
