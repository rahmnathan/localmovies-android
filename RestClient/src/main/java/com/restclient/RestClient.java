package com.restclient;

import com.rahmnathan.MovieInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class RestClient {

    private final JSONtoMovieInfoMapper movieInfoMapper = new JSONtoMovieInfoMapper();

    public List<MovieInfo> requestTitles(com.phoneinfo.Phone myPhone) {
        if(myPhone.getAccessToken() == null)
            refreshKey(myPhone);

        String restRequest = "http://" + myPhone.getComputerIP() + ":3990/titlerequest?access_token="
                + myPhone.getAccessToken() + "&path=" + myPhone.getCurrentPath().replace(" ", "%20");
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

    public void refreshKey(com.phoneinfo.Phone myPhone){
        String urlString = "http://" + myPhone.getComputerIP() + ":8082/auth/realms/Demo/protocol/openid-connect/token";

        System.out.println("refresh key");
        System.out.println(urlString);
        Map<String, String> args = new HashMap<>();
        args.put("grant_type", "password");
        args.put("client_id", "movielogin");
        args.put("username", myPhone.getUserName());
        args.put("password", myPhone.getPassword());
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> entry: args.entrySet()){
            try {
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8") + "="
                        + URLEncoder.encode(entry.getValue(), "UTF-8"));
                sb.append("&");
            } catch (UnsupportedEncodingException e){
                e.printStackTrace();
            }
        }
        byte[] postData = sb.toString().substring(0, sb.length()-1).getBytes();
        int postDataLength = postData.length;
        try{
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setFixedLengthStreamingMode(postDataLength);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.connect();
            try{
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.write( postData );
                wr.close();
            } catch (Exception e){
                e.printStackTrace();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = br.readLine();
            String result = "";
            while(response != null){
                result += response;
                response = br.readLine();
            }
            br.close();
            connection.disconnect();
            myPhone.setAccessToken(new JSONObject(result).getString("access_token"));
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
