package com.github.rahmnathan.localmovies.app.control;

import android.arch.persistence.room.TypeConverter;

import com.github.rahmnathan.localmovies.info.provider.data.Movie;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class MovieTypeConverters {
    private static final Gson gson = new Gson();

    @TypeConverter
    public List<Movie> toMovieList(String json){
        if(json == null) return null;

        Type type = new TypeToken<List<Movie>>(){}.getType();

        return gson.fromJson(json, type);
    }

    @TypeConverter
    public String fromMovieList(List<Movie> movies){
        if(movies == null) return null;

        Type type = new TypeToken<List<Movie>>(){}.getType();

        return gson.toJson(movies, type);
    }
}
