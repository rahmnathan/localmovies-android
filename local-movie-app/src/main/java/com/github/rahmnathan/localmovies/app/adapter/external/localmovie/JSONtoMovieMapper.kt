package com.github.rahmnathan.localmovies.app.adapter.external.localmovie

import com.github.rahmnathan.localmovies.app.data.Movie
import java.util.ArrayList

import org.json.JSONArray
import org.json.JSONObject

internal object JSONtoMovieMapper {

    fun jsonArrayToMovieInfoList(jsonList: JSONArray): List<Movie> {
        val movieList = ArrayList<Movie>()
        for (i in 0 until jsonList.length()) {
            val mediaFile = jsonList.getJSONObject(i)
            movieList.add(mediaFileToMovie(mediaFile))
        }
        return movieList
    }

    fun mediaFileToMovie(mediaFile: JSONObject): Movie {
        val movieInfo = mediaFile.getJSONObject("movie")

        return Movie(
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
                path = mediaFile.getString("path")
        )
    }

    fun jsonArrayToMovieEventList(jsonList: JSONArray): List<com.github.rahmnathan.localmovies.app.data.MovieEvent> {
        val movieList = ArrayList<com.github.rahmnathan.localmovies.app.data.MovieEvent>()
        for (i in 0 until jsonList.length()) {
            val mediaFileEvent = jsonList.getJSONObject(i)
            if(!mediaFileEvent.isNull("mediaFile")) {
                val mediaFile = mediaFileEvent.getJSONObject("mediaFile")
                val movie = mediaFileToMovie(mediaFile)
                movieList.add(com.github.rahmnathan.localmovies.app.data.MovieEvent(mediaFileEvent.getString("event"), movie, mediaFileEvent.getString("relativePath")))
            } else {
                movieList.add(com.github.rahmnathan.localmovies.app.data.MovieEvent(mediaFileEvent.getString("event"), mediaFileEvent.getString("relativePath")))
            }
        }
        return movieList
    }
}
