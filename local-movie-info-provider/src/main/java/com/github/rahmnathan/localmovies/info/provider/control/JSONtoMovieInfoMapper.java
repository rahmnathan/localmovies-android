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
            JSONObject object = jsonList.getJSONObject(i);
            MovieInfo.Builder builder = MovieInfo.Builder.newInstance()
                    .setReleaseYear(object.getString("releaseYear"))
                    .setMetaRating(object.getString("metaRating"))
                    .setIMDBRating(object.getString("imdbrating"))
                    .setTitle(object.getString("title"))
                    .setCreated(object.getLong("dateCreated"))
                    .setViews(object.getInt("views"));

            if(object.has("image"))
                builder.setImage(object.getString("image"));

            movieInfoList.add(builder.build());
        }
        return movieInfoList;
    }
}
