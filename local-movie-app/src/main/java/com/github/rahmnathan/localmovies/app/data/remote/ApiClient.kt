package com.github.rahmnathan.localmovies.app.data.remote

import com.github.rahmnathan.localmovies.app.di.DynamicOAuth2Service
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.gson.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    private val dynamicOAuth2Service: DynamicOAuth2Service
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
                    // Get fresh OAuth2Service with current credentials (suspend-safe)
                    BearerTokens(
                        accessToken = dynamicOAuth2Service.getServiceSuspend().accessToken.serialize(),
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
