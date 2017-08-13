package com.github.rahmnathan.localmovies;

import com.github.rahmnathan.localmovies.client.Client;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class KeycloakAuthenticator implements Runnable {
    private final Logger logger = Logger.getLogger(KeycloakAuthenticator.class.getName());
    private final Client client;

    public KeycloakAuthenticator(Client client) {
        this.client = client;
    }

    public void run() {
        updateAccessToken();
    }

    private void updateAccessToken() {
        logger.info("Logging in with Keycloak");
        String urlString = client.getComputerUrl() + "/auth/realms/LocalMovies/protocol/openid-connect/token";

        byte[] loginInfo = buildLoginInfo(client);
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setFixedLengthStreamingMode(loginInfo.length);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            urlConnection.setConnectTimeout(5000);
            urlConnection.connect();
        } catch (IOException e) {
            logger.severe(e.toString());
        }

        if (urlConnection != null) {
            try (DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream())) {
                wr.write(loginInfo);
            } catch (IOException e) {
                logger.severe(e.toString());
            }

            StringBuilder result = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                br.lines().forEachOrdered(result::append);
            } catch (IOException e) {
                logger.severe(e.toString());
            } finally {
                urlConnection.disconnect();
            }

            JSONObject response = new JSONObject(result.toString());
            if (response.has("access_token")) {
                client.setAccessToken(response.getString("access_token"));
            }
        }
    }

    private byte[] buildLoginInfo(Client client) {
        Map<String, String> args = new HashMap<>();
        args.put("grant_type", "password");
        args.put("client_id", "movielogin");
        args.put("username", client.getUserName());
        args.put("password", client.getPassword());
        StringBuilder sb = new StringBuilder();
        args.entrySet().forEach(entry -> {
            try {
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8")).append("&");
            } catch (UnsupportedEncodingException e) {
                logger.severe(e.toString());
            }
        });

        return sb.toString().substring(0, sb.length() - 1).getBytes();
    }
}