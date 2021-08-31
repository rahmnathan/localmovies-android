package com.github.rahmnathan.localmovies.app.media.provider.control

import com.github.rahmnathan.localmovies.app.media.data.Media
import java.util.ArrayList

import org.json.JSONArray
import org.json.JSONObject

internal object JSONtoMediaMapper {

    fun jsonArrayToMovieInfoList(jsonList: JSONArray): List<Media> {
        val movieList = ArrayList<Media>()
        for (i in 0 until jsonList.length()) {
            val mediaFile = jsonList.getJSONObject(i)
            movieList.add(mediaFileToMovie(mediaFile))
        }
        return movieList
    }

    private fun mediaFileToMovie(mediaFile: JSONObject): Media {
        val media = mediaFile.getJSONObject("media")

        return Media(
                mediaFileId = mediaFile.getString("mediaFileId"),
                releaseYear = media.getString("releaseYear"),
                metaRating = media.getString("metaRating"),
                imdbRating = media.getString("imdbRating"),
                created = mediaFile.getLong("created"),
                filename = mediaFile.getString("fileName"),
                title = media.getString("title"),
                genre = media.getString("genre"),
                image = media.getString("image"),
                actors = media.getString("actors"),
                plot =  media.getString("plot"),
                path = mediaFile.getString("path"),
                type = media.getString("mediaType"),
                number = media.getString("number")
        )
    }

    fun jsonArrayToMovieEventList(jsonList: JSONArray): List<com.github.rahmnathan.localmovies.app.media.data.MediaEvent> {
        val movieList = ArrayList<com.github.rahmnathan.localmovies.app.media.data.MediaEvent>()
        for (i in 0 until jsonList.length()) {
            val mediaFileEvent = jsonList.getJSONObject(i)
            if(!mediaFileEvent.isNull("mediaFile")) {
                val mediaFile = mediaFileEvent.getJSONObject("mediaFile")
                val movie = mediaFileToMovie(mediaFile)
                movieList.add(com.github.rahmnathan.localmovies.app.media.data.MediaEvent(mediaFileEvent.getString("event"), mediaFileEvent.getString("relativePath"), movie))
            } else {
                movieList.add(com.github.rahmnathan.localmovies.app.media.data.MediaEvent(mediaFileEvent.getString("event"), mediaFileEvent.getString("relativePath"), null))
            }
        }
        return movieList
    }
}
