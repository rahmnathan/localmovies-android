package com.github.rahmnathan.localmovies.app.auth

import com.github.rahmnathan.oauth2.adapter.domain.OAuth2Service
import com.github.rahmnathan.oauth2.adapter.domain.client.OAuth2Client
import com.github.rahmnathan.oauth2.adapter.domain.client.OAuth2ClientConfig
import com.github.rahmnathan.oauth2.adapter.domain.credential.Duration
import com.github.rahmnathan.oauth2.adapter.domain.credential.OAuth2CredentialPassword
import com.github.rahmnathan.oauth2.adapter.keycloak.resilience4j.KeycloakClientResilience4j
import java.time.temporal.ChronoUnit

internal object OAuth2ServiceProvider {

    @JvmStatic
    fun getOAuth2Service(username: String, password: String): OAuth2Service {
        val clientConfig = OAuth2ClientConfig.builder()
                .initialRetryDelay(500)
                .retryCount(3)
                .url("https://login.nathanrahm.com")
                .timoutMs(3000)
                .build()

        val keycloakClient: OAuth2Client = KeycloakClientResilience4j(clientConfig)

        val passwordConfig = OAuth2CredentialPassword.builder()
                .password(password)
                .clientId("localmovies")
                .username(username)
                .realm("NathanRahm")
                .tokenRefreshThreshold(Duration(ChronoUnit.SECONDS, 30))
                .build()

        return OAuth2Service(passwordConfig, keycloakClient)
    }
}