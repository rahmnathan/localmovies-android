package com.restclient;

import com.rahmnathan.MovieInfo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

class JSONtoMovieInfoMapper {

    List<MovieInfo> jsonArrayToMovieInfoList(JSONArray jsonList) {
        List<MovieInfo> movieInfoList = new ArrayList<>();

        jsonList.forEach(json -> {
            JSONObject jsonObject = (JSONObject) json;

            MovieInfo.Builder builder = MovieInfo.Builder.newInstance()
                    .setReleaseYear(jsonObject.getString("releaseYear"))
                    .setMetaRating(jsonObject.getString("metaRating"))
                    .setIMDBRating(jsonObject.getString("imdbrating"))
                    .setTitle(jsonObject.getString("title"));
            
            try {
                builder.setImage(jsonObject.getString("image"));
            } catch (Exception e) {
                builder.setImage(null);
            }

            movieInfoList.add(builder.build());
        });

        return movieInfoList;
    }
}
