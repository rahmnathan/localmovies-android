package com.localmovies;

import com.localmovies.client.Client;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class KeycloakAuthenticator implements AuthenticationProvider {
    public Response updateAuthenticationToken(Client client){
        String urlString = "https://" + client.getComputerIP() + ":8445/auth/realms/Demo/protocol/openid-connect/token";

        Map<String, String> args = new HashMap<>();
        args.put("grant_type", "password");
        args.put("client_id", "movielogin");
        args.put("username", client.getUserName());
        args.put("password", client.getPassword());
        StringBuilder sb = new StringBuilder();
        args.entrySet().forEach((entry) -> {
            try {
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8")).append("&");
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
            client.setAccessToken(new JSONObject(result.toString()).getString("access_token"));
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
