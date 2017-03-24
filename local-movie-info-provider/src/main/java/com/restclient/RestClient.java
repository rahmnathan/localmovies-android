package com.restclient;

import com.localmovies.AuthenticationProvider;
import com.localmovies.KeycloakAuthenticator;
import com.localmovies.Response;
import com.phoneinfo.Phone;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RestClient {
    private final JSONtoMovieInfoMapper movieInfoMapper = new JSONtoMovieInfoMapper();
    private final Logger logger = Logger.getLogger("RestClient");
    private final AuthenticationProvider authenticationProvider = new KeycloakAuthenticator();

    public List<MovieInfo> getMovieInfo(Phone myPhone, int page, int resultsPerPage) {
        if(myPhone.getAccessToken() == null){
            logger.log(Level.INFO, "Refreshing token");
            Response response = authenticationProvider.updateAuthenticationToken(myPhone);
            switch (response){
                case SUCCESS:
                    break;
                case AUTH_FAIL:
                    List<MovieInfo> infoList = new ArrayList<>();
                    infoList.add(MovieInfo.Builder.newInstance()
                            .setTitle("Authorization Failure").build());
                    return infoList;
                case CONNECTION_FAIL:
                    List<MovieInfo> infoList1 = new ArrayList<>();
                    infoList1.add(MovieInfo.Builder.newInstance()
                            .setTitle("Unable to connect to Auth server").build());
                    return infoList1;
            }
        }

        return requestMovieInfoList(myPhone, page, resultsPerPage);
    }

    private List<MovieInfo> requestMovieInfoList(Phone myPhone, int page, int resultsPerPage){
        logger.log(Level.INFO, "Requesting movies");
        String restRequest = "https://" + myPhone.getComputerIP() + ":8443/titlerequest?access_token="
                + myPhone.getAccessToken() + "&page=" + page + "&resultsPerPage=" + resultsPerPage
                + "&path=" + myPhone.getCurrentPath().toString().replace(" ", "%20");
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(restRequest)).openConnection();
            if(page == 0)
                myPhone.setMovieCount(Integer.valueOf(connection.getHeaderField("Count")));
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            br.lines().forEachOrdered(result::append);
            br.close();
            connection.disconnect();

            return movieInfoMapper.jsonArrayToMovieInfoList(new JSONArray(result.toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
