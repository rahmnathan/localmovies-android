package com.github.rahmnathan.localmovies.info.provider.control

import com.github.rahmnathan.localmovies.info.provider.data.Movie
import com.github.rahmnathan.localmovies.info.provider.data.MovieEvent

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
                imdbRating = movieInfo.getString("imdbrating"),
                created = mediaFile.getLong("dateCreated"),
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

    fun jsonArrayToMovieEventList(jsonList: JSONArray): List<MovieEvent> {
        val movieList = ArrayList<MovieEvent>()
        for (i in 0 until jsonList.length()) {
            val mediaFileEvent = jsonList.getJSONObject(i)
            val mediaFile = mediaFileEvent.getJSONObject("mediaFile")

            val movie = mediaFileToMovie(mediaFile)
            movieList.add(MovieEvent(mediaFileEvent.getString("event"), movie, mediaFileEvent.getString("relativePath")))
        }
        return movieList
    }
}
