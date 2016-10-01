package com.movieinfoprovider;

import com.google.common.io.ByteStreams;
import com.rahmnathan.*;
import com.rahmnathan.MovieInfoProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class OMDBMovieInfoProvider implements MovieInfoProvider {

    private IOProvider ioProvider = new IOProvider();

    @Override
    public List<MovieInfo> getMovieInfo(List<String> titleList, String currentPath, String dataDirectory){

        try{
            return ioProvider.getInfoFromFile(currentPath, dataDirectory);
        } catch (Exception e){
            List<MovieInfo> movieList = getInfoFromOMDB(titleList, currentPath);
            ioProvider.writeInfoToFile(movieList, currentPath, dataDirectory);

            return movieList;
        }
    }

    private List<MovieInfo> getInfoFromOMDB(List<String> titleList, String currentPath){

        List<MovieInfo> movieDataList = new ArrayList<>();

        for(String x : titleList) {

            JSONObject jsonObject = getData(x, currentPath);

            MovieInfo.Builder movieInfoBuilder = MovieInfo.Builder.newInstace();
            movieInfoBuilder.setTitle(x);
            movieInfoBuilder.setImage(getImage(jsonObject));

            try {
                movieInfoBuilder.setIMDBRating(jsonObject.getString("imdbRating"));
            } catch(Exception e){
                movieInfoBuilder.setIMDBRating("N/A");
            }
            try {
                movieInfoBuilder.setMetaRating(jsonObject.getString("Metascore"));
            } catch (Exception e){
                movieInfoBuilder.setMetaRating("N/A");
            }
            try {
                movieInfoBuilder.setReleaseYear(jsonObject.getString("Year"));
            } catch (Exception e){
                movieInfoBuilder.setReleaseYear("N/A");
            }

            movieDataList.add(movieInfoBuilder.build());
        }
        return movieDataList;
    }

    private JSONObject getData(String title, String currentPath) {

        String uri = "http://www.omdbapi.com/?t=";
        String currentPathLowerCase = currentPath.toLowerCase();

        if(currentPathLowerCase.contains("season") || currentPathLowerCase.contains("movies")) {
            title = title.substring(0, title.length() - 4);
        }

        try {
            URL url = new URL(uri + title.replace(" ", "%20"));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            String end = "";

            String string = br.readLine();

            while (!(string == null)) {
                end = end + string;
                string = br.readLine();
            }
            br.close();
            urlConnection.disconnect();

            return new JSONObject(end);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] getImage(JSONObject jsonObject) {
        try {
            URL imageURL = new URL(jsonObject.get("Poster").toString());
            InputStream is = imageURL.openConnection().getInputStream();
            return ByteStreams.toByteArray(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
