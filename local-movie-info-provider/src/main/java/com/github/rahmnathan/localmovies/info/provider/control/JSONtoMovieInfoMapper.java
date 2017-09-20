package com.github.rahmnathan.localmovies.info.provider.control;

import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

class JSONtoMovieInfoMapper {

    static List<MovieInfo> jsonArrayToMovieInfoList(JSONArray jsonList) {
        List<MovieInfo> movieInfoList = new ArrayList<>();
        for (int i = 0; i < jsonList.length(); i++) {
            JSONObject mediaFile = jsonList.getJSONObject(i);
            MovieInfo.Builder builder = MovieInfo.Builder.newInstance()
                    .setCreated(mediaFile.getLong("dateCreated"))
                    .setViews(mediaFile.getInt("views"));

            JSONObject movieInfo = mediaFile.getJSONObject("movieInfo");

            builder.setReleaseYear(movieInfo.getString("releaseYear"))
                    .setMetaRating(movieInfo.getString("metaRating"))
                    .setIMDBRating(movieInfo.getString("imdbrating"))
                    .setTitle(movieInfo.getString("title"))
                    .setGenre(movieInfo.getString("genre"))
                    .setImage(movieInfo.getString("image"));

            movieInfoList.add(builder.build());
        }
        return movieInfoList;
    }
}
