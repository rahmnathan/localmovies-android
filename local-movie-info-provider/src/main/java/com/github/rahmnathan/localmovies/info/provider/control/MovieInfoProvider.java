package com.github.rahmnathan.localmovies.info.provider.control;

import com.github.rahmnathan.localmovies.client.Client;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public List<MovieInfo> getMovieInfo(Client client, int page, int resultsPerPage, String pushToken) {
        JSONArray movieInfoJson = getMovieInfoJson(client, page, resultsPerPage, pushToken);
        if (movieInfoJson == null)
            return new ArrayList<>();

        return JSONtoMovieInfoMapper.jsonArrayToMovieInfoList(movieInfoJson);
    }

    private JSONArray getMovieInfoJson(Client client, int page, int resultsPerPage, String pushToken) {
        HttpURLConnection urlConnection = null;
        try {
            String restRequest = client.getComputerUrl() + "/movie-api/v1/titlerequest?access_token="
                    + client.getAccessToken() + "&page=" + page + "&resultsPerPage=" + resultsPerPage
                    + "&path=" + URLEncoder.encode(client.getCurrentPath().toString(), StandardCharsets.UTF_8.name())
                    + "&pushToken=" + pushToken;
            urlConnection = (HttpURLConnection) (new URL(restRequest)).openConnection();
        } catch (IOException e) {
            logger.severe(e.toString());
        }

        if (urlConnection != null) {
            if (page == 0)
                client.setMovieCount(Integer.valueOf(urlConnection.getHeaderField("Count")));

            StringBuilder result = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                br.lines().forEachOrdered(result::append);
                return new JSONArray(result.toString());
            } catch (IOException e) {
                logger.severe(e.toString());
            } finally {
                urlConnection.disconnect();
            }
        }
        return null;
    }
}