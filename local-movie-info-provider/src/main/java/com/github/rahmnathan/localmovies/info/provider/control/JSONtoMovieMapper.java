package com.github.rahmnathan.localmovies.info.provider.control;

import com.github.rahmnathan.localmovies.info.provider.data.Movie;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

class JSONtoMovieMapper {

    static List<Movie> jsonArrayToMovieInfoList(JSONArray jsonList) {
        List<Movie> movieList = new ArrayList<>();
        for (int i = 0; i < jsonList.length(); i++) {
            JSONObject mediaFile = jsonList.getJSONObject(i);
            Movie.Builder builder = Movie.Builder.newInstance()
                    .setCreated(mediaFile.getLong("dateCreated"))
                    .setFileName(mediaFile.getString("fileName"))
                    .setViews(mediaFile.getInt("views"));

            JSONObject movieInfo = mediaFile.getJSONObject("movieInfo");

            builder.setReleaseYear(movieInfo.getString("releaseYear"))
                    .setMetaRating(movieInfo.getString("metaRating"))
                    .setIMDBRating(movieInfo.getString("imdbrating"))
                    .setTitle(movieInfo.getString("title"))
                    .setGenre(movieInfo.getString("genre"))
                    .setImage(movieInfo.getString("image"));

            movieList.add(builder.build());
        }
        return movieList;
    }
}
