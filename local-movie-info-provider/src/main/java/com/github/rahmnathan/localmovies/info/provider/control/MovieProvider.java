package com.github.rahmnathan.localmovies.info.provider.control;

import com.github.rahmnathan.localmovies.client.Client;
import com.github.rahmnathan.localmovies.info.provider.data.Movie;
import com.github.rahmnathan.localmovies.info.provider.data.MovieEvent;
import com.github.rahmnathan.localmovies.info.provider.data.MovieRequest;
import com.google.gson.Gson;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovieProvider {
    private final Logger logger = Logger.getLogger(MovieProvider.class.getName());
    private final Gson gson = new Gson();

    public List<Movie> getMovieInfo(Client client, MovieRequest movieRequest) {
        Optional<JSONArray> movieInfoJson = getMovieInfoJson(client, movieRequest);
        return movieInfoJson.map(JSONtoMovieMapper.INSTANCE::jsonArrayToMovieInfoList).orElseGet(ArrayList::new);
    }

    public List<MovieEvent> getMovieEvents(Client client) {
        Optional<JSONArray> movieInfoJson = getMovieEventJson(client);
        return movieInfoJson.map(JSONtoMovieMapper.INSTANCE::jsonArrayToMovieEventList).orElseGet(ArrayList::new);
    }

    private Optional<JSONArray> getMovieInfoJson(Client client, MovieRequest movieRequest) {
        HttpURLConnection urlConnection = null;
        String url = client.getComputerUrl() + "/localmovies/v2/movies";

        try {
            urlConnection = (HttpURLConnection) (new URL(url)).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Authorization", "bearer " + client.getAccessToken());
            urlConnection.setConnectTimeout(10000);
        } catch (IOException e){
            logger.log(Level.SEVERE, "Failed connecting to movie info service", e);
        }

        if(urlConnection != null) {
            String movieRequestBody = gson.toJson(movieRequest);
            try (OutputStreamWriter outputStream = new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8")) {
                outputStream.write(movieRequestBody);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed writing to movie info service", e);
            }

            if (movieRequest.getPage() == 0) {
                logger.fine("Reading page count");
                client.setMovieCount(Integer.valueOf(urlConnection.getHeaderField("Count")));
            }

            StringBuilder result = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                br.lines().forEachOrdered(result::append);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed reading from movie info service", e);
            } finally {
                urlConnection.disconnect();
            }

            return Optional.of(new JSONArray(result.toString()));
        }
        return Optional.empty();
    }

    private Optional<JSONArray> getMovieEventJson(Client client) {
        HttpURLConnection urlConnection = null;
        if(client.getLastUpdate() == null){
            client.setLastUpdate(System.currentTimeMillis());
        }
        String url = client.getComputerUrl() + "/localmovies/v2/movie/events?timestamp=" + client.getLastUpdate();

        try {
            urlConnection = (HttpURLConnection) (new URL(url)).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Authorization", "bearer " + client.getAccessToken());
            urlConnection.setConnectTimeout(10000);
        } catch (IOException e){
            logger.log(Level.SEVERE, "Failed connecting to movie info service", e);
        }

        if(urlConnection != null) {
            StringBuilder result = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                br.lines().forEachOrdered(result::append);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed reading from movie info service", e);
            } finally {
                urlConnection.disconnect();
            }

            return Optional.of(new JSONArray(result.toString()));
        }
        return Optional.empty();
    }
}