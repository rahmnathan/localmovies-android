package com.github.rahmnathan.localmovies.app.data.remote

import com.github.rahmnathan.oauth2.adapter.domain.OAuth2Service
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    private val oAuth2Service: OAuth2Service
) {
    val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            gson()
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(
                        accessToken = oAuth2Service.accessToken.serialize(),
                        refreshToken = ""
                    )
                }
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 10000
        }

        defaultRequest {
            headers.append("x-correlation-id", java.util.UUID.randomUUID().toString())
        }
    }
}
