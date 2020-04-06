package com.github.rahmnathan.localmovies.app.persistence.media.room

import androidx.room.TypeConverter
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.google.gson.Gson

class MediaTypeConverters {
    @TypeConverter
    fun toMovieList(json: String?): Media? {
        return if (json == null) null else gson.fromJson(json, Media::class.java)
    }

    @TypeConverter
    fun fromMovieList(media: Media?): String? {
        return if (media == null) null else gson.toJson(media)
    }

    companion object {
        private val gson = Gson()
    }
}