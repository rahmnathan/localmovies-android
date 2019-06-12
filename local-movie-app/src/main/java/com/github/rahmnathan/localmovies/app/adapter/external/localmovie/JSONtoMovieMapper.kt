package com.github.rahmnathan.localmovies.app.adapter.external.localmovie

import com.github.rahmnathan.localmovies.app.data.Media
import java.util.ArrayList

import org.json.JSONArray
import org.json.JSONObject

internal object JSONtoMovieMapper {

    fun jsonArrayToMovieInfoList(jsonList: JSONArray): List<Media> {
        val movieList = ArrayList<Media>()
        for (i in 0 until jsonList.length()) {
            val mediaFile = jsonList.getJSONObject(i)
            movieList.add(mediaFileToMovie(mediaFile))
        }
        return movieList
    }

    fun mediaFileToMovie(mediaFile: JSONObject): Media {
        val movieInfo = mediaFile.getJSONObject("media")

        println("")

        return Media(
                releaseYear = movieInfo.getString("releaseYear"),
                metaRating = movieInfo.getString("metaRating"),
                imdbRating = movieInfo.getString("imdbRating"),
                created = mediaFile.getLong("created"),
                filename = mediaFile.getString("fileName"),
                title = movieInfo.getString("title"),
                genre = movieInfo.getString("genre"),
                image = movieInfo.getString("image"),
                views = mediaFile.getInt("views"),
                actors = movieInfo.getString("actors"),
                plot =  movieInfo.getString("plot"),
                path = mediaFile.getString("path"),
                type = movieInfo.getString("mediaType"),
                number = movieInfo.getString("number")
        )
    }

    fun jsonArrayToMovieEventList(jsonList: JSONArray): List<com.github.rahmnathan.localmovies.app.data.MovieEvent> {
        val movieList = ArrayList<com.github.rahmnathan.localmovies.app.data.MovieEvent>()
        for (i in 0 until jsonList.length()) {
            val mediaFileEvent = jsonList.getJSONObject(i)
            if(!mediaFileEvent.isNull("mediaFile")) {
                val mediaFile = mediaFileEvent.getJSONObject("mediaFile")
                val movie = mediaFileToMovie(mediaFile)
                movieList.add(com.github.rahmnathan.localmovies.app.data.MovieEvent(mediaFileEvent.getString("event"), mediaFileEvent.getString("relativePath"), movie))
            } else {
                movieList.add(com.github.rahmnathan.localmovies.app.data.MovieEvent(mediaFileEvent.getString("event"), mediaFileEvent.getString("relativePath"), null))
            }
        }
        return movieList
    }
}
