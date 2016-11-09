package com.restclient;

import com.rahmnathan.MovieInfo;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class RestClient {

    private final JSONtoMovieInfoMapper movieInfoMapper = new JSONtoMovieInfoMapper();

    public List<MovieInfo> requestTitles(com.phoneinfo.Phone myPhone) {

        String restRequest = "http://" + myPhone.getComputerIP() + ":3990/titlerequest?path=" +
                myPhone.getCurrentPath().replace(" ", "%20");

        try {
            URL url = new URL(restRequest);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = br.readLine();
            String result = "";
            while(response != null){
                result += response;
                response = br.readLine();
            }
            br.close();
            connection.disconnect();

            JSONArray array = new JSONArray(result);

            return movieInfoMapper.jsonArrayToMovieInfoList(array);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void refresh(com.phoneinfo.Phone myPhone){
        String restRequest = "http://" + myPhone.getComputerIP() + ":3990/refresh";

        try{
            URL url = new URL(restRequest);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.getResponseCode();
            connection.disconnect();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
