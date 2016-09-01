package com.rahmnathan;

import com.google.common.io.ByteStreams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MovieInfoProvider {

    private JSONObject jsonObject;
    private IOProvider ioProvider = new IOProvider();

    public List<MovieInfo> getMovieData(List<String> titleList, String currentPath, String dataDirectory){

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
            MovieInfo movieData = new MovieInfo();
            movieData.setTitle(x);

            getData(x, currentPath);

            movieData.setImage(getImage());

            try {
                movieData.setIMDBRating(jsonObject.getString("imdbRating"));
            } catch (JSONException e) {
                movieData.setIMDBRating("N/A");
            }
            try {
                movieData.setMetaRating(jsonObject.getString("Metascore"));
            } catch (JSONException e) {
                movieData.setMetaRating("N/A");
            }
            try {
                movieData.setReleaseYear(jsonObject.getString("Year"));
            } catch (JSONException e) {
                movieData.setReleaseYear("N/A");
            }

            movieDataList.add(movieData);
        }
        return movieDataList;
    }

    private void getData(String title, String currentPath) {

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

            jsonObject = new JSONObject(end);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] getImage() {
        try {
            URL imageURL = new URL(jsonObject.get("Poster").toString());
            InputStream is = imageURL.openConnection().getInputStream();
            return ByteStreams.toByteArray(is);
        } catch (Exception e) {}
        return null;
    }
}
