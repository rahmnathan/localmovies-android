package com.restclient;

import com.phoneinfo.Phone;
import com.rahmnathan.MovieInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

public class RestClient {
    private final JSONtoMovieInfoMapper movieInfoMapper = new JSONtoMovieInfoMapper();

    private enum Response {
        AUTH_FAIL, CONNECTION_FAIL, SUCCESS, UNKNOWN_FAIL
    }

    public List<MovieInfo> getMovieInfo(Phone myPhone, int page, int resultsPerPage) {
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
                case UNKNOWN_FAIL:
                    List<MovieInfo> infoList2 = new ArrayList<>();
                    infoList2.add(MovieInfo.Builder.newInstance()
                            .setTitle("Unknown Auth failure").build());
                    return infoList2;
            }
        }

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

    public Response refreshKey(Phone myPhone){
        String urlString = "https://" + myPhone.getComputerIP() + ":8445/auth/realms/Demo/protocol/openid-connect/token";

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
            connection.setConnectTimeout(5000);
            connection.connect();
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write( postData );
            wr.close();
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
            return Response.SUCCESS;
        } catch (SocketTimeoutException e){
            e.printStackTrace();
            return Response.CONNECTION_FAIL;
        } catch (FileNotFoundException e){
            e.printStackTrace();
            return Response.AUTH_FAIL;
        } catch (IOException e) {
            e.printStackTrace();
            return Response.UNKNOWN_FAIL;
        }
    }
}
