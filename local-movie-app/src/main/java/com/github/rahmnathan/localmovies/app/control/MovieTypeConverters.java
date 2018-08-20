package com.github.rahmnathan.localmovies.app.control;

import android.arch.persistence.room.TypeConverter;

import com.github.rahmnathan.localmovies.app.data.Movie;
import com.google.gson.Gson;

public class MovieTypeConverters {
    private static final Gson gson = new Gson();

    @TypeConverter
    public Movie toMovieList(String json){
        if(json == null) return null;
        return gson.fromJson(json, Movie.class);
    }

    @TypeConverter
    public String fromMovieList(Movie movie){
        if(movie == null) return null;
        return gson.toJson(movie);
    }
}
