package com.github.rahmnathan.localmovies.info.provider.control

import com.github.rahmnathan.localmovies.info.provider.data.Movie

import java.util.ArrayList

import org.json.JSONArray

internal object JSONtoMovieMapper {

    fun jsonArrayToMovieInfoList(jsonList: JSONArray): List<Movie> {
        val movieList = ArrayList<Movie>()
        for (i in 0 until jsonList.length()) {
            val mediaFile = jsonList.getJSONObject(i)
            val movieInfo = mediaFile.getJSONObject("movie")

            val movie = Movie(
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
                    plot =  movieInfo.getString("plot")
            )
            movieList.add(movie)
        }
        return movieList
    }
}
