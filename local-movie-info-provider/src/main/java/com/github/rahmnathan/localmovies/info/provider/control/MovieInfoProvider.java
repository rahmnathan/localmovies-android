package com.github.rahmnathan.localmovies.info.provider.control;

import com.github.rahmnathan.localmovies.client.Client;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MovieInfoProvider {
    private final JSONtoMovieInfoMapper movieInfoMapper = new JSONtoMovieInfoMapper();
    private final Logger logger = Logger.getLogger(MovieInfoProvider.class.getName());

    public List<MovieInfo> getMovieInfo(Client client, int page, int resultsPerPage) {
        JSONArray movieInfoJson = getMovieInfoJson(client, page, resultsPerPage);
        if(movieInfoJson == null)
            return new ArrayList<>();

        return movieInfoMapper.jsonArrayToMovieInfoList(movieInfoJson);
    }

    private JSONArray getMovieInfoJson(Client client, int page, int resultsPerPage){
        try {
            String restRequest = client.getComputerUrl() + "/movie-api/v1/titlerequest?access_token="
                    + client.getAccessToken() + "&page=" + page + "&resultsPerPage=" + resultsPerPage
                    + "&path=" + URLEncoder.encode(client.getCurrentPath().toString(), StandardCharsets.UTF_8.name());
            HttpURLConnection connection = (HttpURLConnection) (new URL(restRequest)).openConnection();
            if (page == 0)
                client.setMovieCount(Integer.valueOf(connection.getHeaderField("Count")));
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            br.lines().forEachOrdered(result::append);
            br.close();
            connection.disconnect();
            return new JSONArray(result.toString());
        }catch (Exception e) {
            logger.info(e.toString());
        }
        return null;
    }
}
