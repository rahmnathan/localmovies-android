package com.github.rahmnathan.localmovies.app.adapter.external.keycloak

import com.github.rahmnathan.localmovies.app.data.Client
import com.github.rahmnathan.oauth2.adapter.domain.OAuth2Service
import com.github.rahmnathan.oauth2.adapter.domain.client.OAuth2Client
import com.github.rahmnathan.oauth2.adapter.domain.client.OAuth2ClientConfig
import com.github.rahmnathan.oauth2.adapter.domain.credential.Duration
import com.github.rahmnathan.oauth2.adapter.domain.credential.OAuth2CredentialPassword
import com.github.rahmnathan.oauth2.adapter.keycloak.resilience4j.KeycloakClientResilience4j
import java.time.temporal.ChronoUnit
import java.util.logging.Logger
import javax.inject.Inject

class KeycloakAuthenticator(private val client: Client) : Runnable {
    private val logger = Logger.getLogger(KeycloakAuthenticator::class.java.name)
    private val oAuth2Service: OAuth2Service

    override fun run() {
        updateAccessToken()
    }

    private fun updateAccessToken() {
        try {
            client.accessToken = oAuth2Service.accessToken.serialize()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    init {
        val clientConfig = OAuth2ClientConfig.builder()
                .initialRetryDelay(500)
                .retryCount(3)
                .url("https://login.nathanrahm.com/auth")
                .timoutMs(3000)
                .build()

        val keycloakClient: OAuth2Client = KeycloakClientResilience4j(clientConfig)

        val passwordConfig = OAuth2CredentialPassword.builder()
                .password(client.password!!)
                .clientId("localmovies")
                .username(client.userName!!)
                .realm("LocalMovies")
                .tokenRefreshThreshold(Duration(ChronoUnit.SECONDS, 30))
                .build()

        oAuth2Service = OAuth2Service(passwordConfig, keycloakClient)
    }
}