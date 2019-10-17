package com.github.rahmnathan.localmovies.app.adapter.external.keycloak;

import com.github.rahmnathan.localmovies.app.data.Client;
import com.github.rahmnathan.oauth2.adapter.domain.OAuth2Service;
import com.github.rahmnathan.oauth2.adapter.domain.client.OAuth2Client;
import com.github.rahmnathan.oauth2.adapter.domain.client.OAuth2ClientConfig;
import com.github.rahmnathan.oauth2.adapter.domain.credential.Duration;
import com.github.rahmnathan.oauth2.adapter.domain.credential.OAuth2CredentialPassword;
import com.github.rahmnathan.oauth2.adapter.keycloak.resilience4j.KeycloakClientResilience4j;

import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

public class KeycloakAuthenticator implements Runnable {
    private final Logger logger = Logger.getLogger(KeycloakAuthenticator.class.getName());
    private final OAuth2Service oAuth2Service;
    private final Client client;

    public KeycloakAuthenticator(Client client) {
        this.client = client;

        OAuth2ClientConfig clientConfig = OAuth2ClientConfig.builder()
                .initialRetryDelay(500)
                .retryCount(3)
                .url("https://login.nathanrahm.com/auth")
                .timoutMs(3000)
                .build();

        OAuth2Client keycloakClient = new KeycloakClientResilience4j(clientConfig);

        OAuth2CredentialPassword passwordConfig = OAuth2CredentialPassword.builder()
                .password(client.getPassword())
                .clientId("localmovies")
                .username(client.getUserName())
                .realm("LocalMovies")
                .tokenRefreshThreshold(new Duration(ChronoUnit.SECONDS, 30))
                .build();

        this.oAuth2Service = new OAuth2Service(passwordConfig, keycloakClient);
    }

    public void run() {
        updateAccessToken();
    }

    private void updateAccessToken() {
        try{
            client.setAccessToken(oAuth2Service.getAccessToken().serialize());
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}