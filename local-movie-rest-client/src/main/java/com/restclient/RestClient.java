package com.restclient;

import com.phoneinfo.Phone;
import com.rahmnathan.MovieInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RestClient {
    private final JSONtoMovieInfoMapper movieInfoMapper = new JSONtoMovieInfoMapper();
    private final Logger logger = Logger.getLogger("RestClient");

    private enum Response {
        AUTH_FAIL, CONNECTION_FAIL, SUCCESS
    }

    public List<MovieInfo> getMovieInfo(Phone myPhone, int page, int resultsPerPage) {
        logger.log(Level.INFO, "Refreshing token");
        if(myPhone.getAccessToken() == null){
            Response response = refreshKey(myPhone);
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

        logger.log(Level.INFO, "Requesting movies");
        return requestMovieInfoList(myPhone, page, resultsPerPage);
    }

    private List<MovieInfo> requestMovieInfoList(Phone myPhone, int page, int resultsPerPage){
        String restRequest = "https://" + myPhone.getComputerIP() + ":8443/titlerequest?access_token="
                + myPhone.getAccessToken() + "&page=" + page + "&resultsPerPage=" + resultsPerPage
                + "&path=" + myPhone.getCurrentPath().replace(" ", "%20");
        try {
            URL url = new URL(restRequest);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if(page == 0)
                myPhone.setMovieCount(Integer.valueOf(connection.getHeaderField("Count")));
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            br.lines().forEachOrdered(result::append);
            br.close();
            connection.disconnect();

            logger.log(Level.INFO, "Got titles");
            return movieInfoMapper.jsonArrayToMovieInfoList(new JSONArray(result.toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Response refreshKey(Phone myPhone){
        String urlString = "https://" + myPhone.getComputerIP() + ":8445/auth/realms/Demo/protocol/openid-connect/token";

        Map<String, String> args = new HashMap<>();
        args.put("grant_type", "password");
        args.put("client_id", "movielogin");
        args.put("username", myPhone.getUserName());
        args.put("password", myPhone.getPassword());
        StringBuilder sb = new StringBuilder();
        args.entrySet().forEach((entry) -> {
                    try {
                        sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                        sb.append("=");
                        sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                        sb.append("&");
                    } catch (UnsupportedEncodingException e){
                        e.printStackTrace();
                    }
                });
        byte[] postData = sb.toString().substring(0, sb.length()-1).getBytes();
        int postDataLength = postData.length;
        try{
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setFixedLengthStreamingMode(postDataLength);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setConnectTimeout(5000);
            connection.connect();
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(postData);
            wr.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            br.lines().forEachOrdered(result::append);
            br.close();
            connection.disconnect();
            myPhone.setAccessToken(new JSONObject(result.toString()).getString("access_token"));
            return Response.SUCCESS;
        } catch (SocketTimeoutException e){
            e.printStackTrace();
            return Response.CONNECTION_FAIL;
        } catch (Exception e){
            e.printStackTrace();
            return Response.AUTH_FAIL;
        }
    }
}
