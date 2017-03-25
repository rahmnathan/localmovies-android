package com.localmovies.provider.control;

import com.localmovies.client.Client;
import com.localmovies.provider.data.MovieInfo;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovieInfoProvider {
    private final JSONtoMovieInfoMapper movieInfoMapper = new JSONtoMovieInfoMapper();
    private final Logger logger = Logger.getLogger("MovieInfoProvider");

    public List<MovieInfo> getMovieInfo(Client client, int page, int resultsPerPage) {
        logger.log(Level.INFO, "Requesting movies");
        JSONArray movieInfoJson = getMovieInfoJson(client, page, resultsPerPage);
        if(movieInfoJson == null)
            return new ArrayList<>();

        return movieInfoMapper.jsonArrayToMovieInfoList(movieInfoJson);
    }

    private JSONArray getMovieInfoJson(Client client, int page, int resultsPerPage){
        String restRequest = "https://" + client.getComputerIP() + ":8443/titlerequest?access_token="
                + client.getAccessToken() + "&page=" + page + "&resultsPerPage=" + resultsPerPage
                + "&path=" + client.getCurrentPath().toString().replace(" ", "%20");
        try {
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
            e.printStackTrace();
        }
        return null;
    }
}
