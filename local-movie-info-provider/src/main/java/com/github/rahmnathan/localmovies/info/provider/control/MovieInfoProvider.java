package com.github.rahmnathan.localmovies.info.provider.control;

import com.github.rahmnathan.localmovies.client.Client;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfoRequest;
import com.google.gson.Gson;

import org.json.JSONArray;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MovieInfoProvider {
    private final Logger logger = Logger.getLogger(MovieInfoProvider.class.getName());
    private final Gson gson = new Gson();

    public List<MovieInfo> getMovieInfo(Client client, MovieInfoRequest movieInfoRequest) {
        JSONArray movieInfoJson = getMovieInfoJson(client, movieInfoRequest);
        if (movieInfoJson == null)
            return new ArrayList<>();

        return JSONtoMovieInfoMapper.jsonArrayToMovieInfoList(movieInfoJson);
    }

    private JSONArray getMovieInfoJson(Client client, MovieInfoRequest movieInfoRequest) {
        HttpURLConnection urlConnection = null;
        try {
            String url = client.getComputerUrl() + "/movie-api/titlerequest";
            urlConnection = (HttpURLConnection) (new URL(url)).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Authorization", "bearer " + client.getAccessToken());
            urlConnection.setConnectTimeout(10000);

            String movieRequestBody = gson.toJson(movieInfoRequest);
            try(OutputStreamWriter outputStream = new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8")){
                outputStream.write(movieRequestBody);
            } catch (IOException e){
                logger.severe(e.toString());
            }

            logger.fine("Response Code: " + urlConnection.getResponseCode());

            if (movieInfoRequest.getPage() == 0) {
                logger.fine("Reading page count");
                client.setMovieCount(Integer.valueOf(urlConnection.getHeaderField("Count")));
            }

            StringBuilder result = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                br.lines().forEachOrdered(result::append);
                return new JSONArray(result.toString());
            } catch (IOException e) {
                logger.severe(e.toString());
            } finally {
                urlConnection.disconnect();
            }
        } catch (IOException e) {
            logger.severe(e.toString());
        } finally {
            if(urlConnection != null){
                urlConnection.disconnect();
            }
        }
        return null;
    }
}