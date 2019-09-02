package com.github.rahmnathan.localmovies.app.control;

import androidx.room.TypeConverter;

import com.github.rahmnathan.localmovies.app.data.Media;
import com.google.gson.Gson;

public class MovieTypeConverters {
    private static final Gson gson = new Gson();

    @TypeConverter
    public Media toMovieList(String json){
        if(json == null) return null;
        return gson.fromJson(json, Media.class);
    }

    @TypeConverter
    public String fromMovieList(Media media){
        if(media == null) return null;
        return gson.toJson(media);
    }
}
