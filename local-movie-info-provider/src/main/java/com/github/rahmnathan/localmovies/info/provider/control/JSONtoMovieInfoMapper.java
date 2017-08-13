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
            JSONObject json = jsonList.getJSONObject(i);
            MovieInfo.Builder builder = MovieInfo.Builder.newInstance()
                    .setReleaseYear(json.getString("releaseYear"))
                    .setMetaRating(json.getString("metaRating"))
                    .setIMDBRating(json.getString("imdbrating"))
                    .setTitle(json.getString("title"))
                    .setCreated(json.getLong("dateCreated"))
                    .setViews(json.getInt("views"));

            if(json.has("image"))
                builder.setImage(json.getString("image"));

            movieInfoList.add(builder.build());
        }
        return movieInfoList;
    }
}
