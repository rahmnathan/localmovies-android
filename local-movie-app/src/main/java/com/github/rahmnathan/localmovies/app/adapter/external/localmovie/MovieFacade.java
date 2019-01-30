package com.github.rahmnathan.localmovies.app.adapter.external.localmovie;

import com.github.rahmnathan.localmovies.app.data.Client;
import com.github.rahmnathan.localmovies.app.data.Movie;
import com.github.rahmnathan.localmovies.app.data.MovieEvent;
import com.github.rahmnathan.localmovies.app.data.MovieRequest;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovieFacade {
    private static final Logger logger = Logger.getLogger(MovieFacade.class.getName());
    private static final String X_CORRELATION_ID = "x-correlation-id";
    private static final Gson GSON = new Gson();

    public static List<Movie> getMovieInfo(Client client, MovieRequest movieRequest) {
        String xCorrelationId = UUID.randomUUID().toString();
        logger.info("Requesting movies with x-correlation-id: " + xCorrelationId);
        Optional<JSONArray> movieInfoJson = getMovieInfoJson(client, movieRequest, xCorrelationId);
        return movieInfoJson.map(JSONtoMovieMapper.INSTANCE::jsonArrayToMovieInfoList).orElseGet(ArrayList::new);
    }

    public static List<MovieEvent> getMovieEvents(Client client) {
        String xCorrelationId = UUID.randomUUID().toString();
        logger.info("Requesting movie events with x-correlation-id: " + xCorrelationId);
        Optional<JSONArray> movieInfoJson = getMovieEventJson(client, xCorrelationId);
        return movieInfoJson.map(JSONtoMovieMapper.INSTANCE::jsonArrayToMovieEventList).orElseGet(ArrayList::new);
    }

    private static Optional<JSONArray> getMovieInfoJson(Client client, MovieRequest movieRequest, String xCorrelationId) {
        HttpURLConnection urlConnection = null;
        String url = client.getComputerUrl() + "/localmovie/v2/media";

        try {
            urlConnection = (HttpURLConnection) (new URL(url)).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty(X_CORRELATION_ID, xCorrelationId);
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Authorization", "bearer " + client.getAccessToken());
            urlConnection.setConnectTimeout(10000);
        } catch (IOException e){
            logger.log(Level.SEVERE, "Failed connecting to movie info service", e);
        }

        if(urlConnection != null) {
            String movieRequestBody = GSON.toJson(movieRequest);
            try (OutputStreamWriter outputStream = new OutputStreamWriter(urlConnection.getOutputStream(), StandardCharsets.UTF_8)) {
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

            try {
                return Optional.of(new JSONArray(result.toString()));
            } catch (JSONException e){
                logger.log(Level.SEVERE,"Failure unmarhalling json.", e);
            }
        }
        return Optional.empty();
    }

    private static Optional<JSONArray> getMovieEventJson(Client client, String xCorrelationId) {
        HttpURLConnection urlConnection = null;
        if(client.getLastUpdate() == null){
            client.setLastUpdate(System.currentTimeMillis());
        }
        String url = client.getComputerUrl() + "/localmovie/v2/media/events?timestamp=" + client.getLastUpdate();

        try {
            urlConnection = (HttpURLConnection) (new URL(url)).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty(X_CORRELATION_ID, xCorrelationId);
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

            try {
                return Optional.of(new JSONArray(result.toString()));
            } catch (JSONException e){
                logger.log(Level.SEVERE, "Failure unmarshalling json.", e);
            }
        }
        return Optional.empty();
    }
}