package com.restclient;

import com.rahmnathan.MovieInfo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

class JSONtoMovieInfoMapper {

    List<MovieInfo> jsonArrayToMovieInfoList(JSONArray jsonList) {
        List<MovieInfo> movieInfoList = new ArrayList<>();

        for(int i = 0; i<jsonList.length(); i++) {

            MovieInfo.Builder builder = MovieInfo.Builder.newInstace();
            JSONObject object = jsonList.getJSONObject(i);

            builder.setReleaseYear(object.getString("releaseYear"));
            builder.setMetaRating(object.getString("metaRating"));
            builder.setIMDBRating(object.getString("imdbrating"));

            try {
                builder.setImage(object.getString("image"));
            } catch (Exception e){
                e.printStackTrace();
                builder.setImage(null);
            }

            builder.setTitle(object.getString("title"));

            movieInfoList.add(builder.build());
        }
        return movieInfoList;
    }
}
