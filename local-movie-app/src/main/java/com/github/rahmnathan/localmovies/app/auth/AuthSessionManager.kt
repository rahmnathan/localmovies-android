package com.github.rahmnathan.localmovies.app.auth

import android.util.Log
import com.github.rahmnathan.localmovies.app.data.local.UserCredentials
import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

class AuthSessionException(
    val errorCode: String,
    message: String
) : IllegalStateException(message)

@Singleton
class AuthSessionManager @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore
) {
    companion object {
        private const val TAG = "AuthSessionManager"
        private const val REALM = "NathanRahm"
        private const val CLIENT_ID = "localmovies"
        private const val TOKEN_SCOPE = "openid offline_access"
    }

    suspend fun login(
        username: String,
        password: String,
        serverUrl: String,
        authServerUrl: String
    ): UserCredentials = withContext(Dispatchers.IO) {
        val response = requestToken(
            authServerUrl = authServerUrl,
            formBody = listOf(
                "client_id" to CLIENT_ID,
                "grant_type" to "password",
                "scope" to TOKEN_SCOPE,
                "username" to username,
                "password" to password
            )
        )

        val session = response.toSession(
            username = username,
            serverUrl = serverUrl,
            authServerUrl = authServerUrl
        )
        preferencesDataStore.saveCredentials(session)
        session
    }

    suspend fun refreshSession(credentials: UserCredentials): UserCredentials = withContext(Dispatchers.IO) {
        val response = requestToken(
            authServerUrl = credentials.authServerUrl,
            formBody = listOf(
                "client_id" to CLIENT_ID,
                "grant_type" to "refresh_token",
                "refresh_token" to credentials.offlineToken
            )
        )

        val updatedSession = response.toSession(
            username = credentials.username,
            serverUrl = credentials.serverUrl,
            authServerUrl = credentials.authServerUrl,
            fallbackOfflineToken = credentials.offlineToken,
            fallbackOfflineTokenExpiry = credentials.offlineTokenExpiresAtEpochMillis
        )
        preferencesDataStore.saveCredentials(updatedSession)
        updatedSession
    }

    suspend fun migrateLegacyCredentials(credentials: UserCredentials): UserCredentials = withContext(Dispatchers.IO) {
        if (!credentials.hasLegacyPassword()) {
            throw IllegalStateException("No legacy password is available for migration")
        }

        Log.d(TAG, "Migrating legacy password-based session for ${credentials.username}")
        login(
            username = credentials.username,
            password = credentials.legacyPassword,
            serverUrl = credentials.serverUrl,
            authServerUrl = credentials.authServerUrl
        )
    }

    private fun requestToken(authServerUrl: String, formBody: List<Pair<String, String>>): TokenResponse {
        val body = formBody.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val tokenUrl = "${authServerUrl.trimEnd('/')}/realms/$REALM/protocol/openid-connect/token"
        val connection = URL(tokenUrl).openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.doInput = true
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(body.toByteArray(StandardCharsets.UTF_8).size)
            connection.connect()

            DataOutputStream(connection.outputStream).use { output ->
                output.write(body.toByteArray(StandardCharsets.UTF_8))
            }

            val responseBody = readResponseBody(connection)
            if (connection.responseCode !in 200..299) {
                throw parseAuthException(responseBody, connection.responseCode)
            }

            parseTokenResponse(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponseBody(connection: HttpURLConnection): String {
        val stream = try {
            connection.inputStream
        } catch (_: Exception) {
            connection.errorStream
        } ?: return ""

        return stream.bufferedText()
    }

    private fun parseTokenResponse(responseBody: String): TokenResponse {
        val json = JsonParser.parseString(responseBody).asJsonObject
        return TokenResponse(
            accessToken = json.getRequiredString("access_token"),
            offlineToken = json.getOptionalString("refresh_token"),
            expiresInSeconds = json.getOptionalLong("expires_in"),
            offlineTokenExpiresInSeconds = json.getOptionalLong("refresh_expires_in")
        )
    }

    private fun parseAuthException(responseBody: String, responseCode: Int): AuthSessionException {
        return runCatching {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val error = json.getOptionalString("error")
            val description = json.getOptionalString("error_description")
            val message = listOfNotNull(error, description)
                .filter { it.isNotBlank() }
                .joinToString(": ")
                .ifBlank { "Authentication failed ($responseCode)" }
            AuthSessionException(
                errorCode = error.ifBlank { "unknown_error" },
                message = message
            )
        }.getOrElse {
            AuthSessionException(
                errorCode = "unknown_error",
                message = "Authentication failed ($responseCode)"
            )
        }
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }

    private fun InputStream.bufferedText(): String {
        return BufferedReader(InputStreamReader(this)).use { reader ->
            buildString {
                reader.lineSequence().forEach(::append)
            }
        }
    }

    private fun JsonObject.getRequiredString(key: String): String {
        return getOptionalString(key).takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Authentication response did not include $key")
    }

    private fun JsonObject.getOptionalString(key: String): String {
        val element = get(key) ?: return ""
        if (element.isJsonNull) return ""
        return element.asString.orEmpty()
    }

    private fun JsonObject.getOptionalLong(key: String): Long {
        val element = get(key) ?: return 0L
        if (element.isJsonNull) return 0L
        return element.asLong
    }

    private data class TokenResponse(
        val accessToken: String,
        val offlineToken: String,
        val expiresInSeconds: Long,
        val offlineTokenExpiresInSeconds: Long
    ) {
        fun toSession(
            username: String,
            serverUrl: String,
            authServerUrl: String,
            fallbackOfflineToken: String = "",
            fallbackOfflineTokenExpiry: Long = 0
        ): UserCredentials {
            val now = System.currentTimeMillis()
            val resolvedOfflineToken = offlineToken.ifBlank { fallbackOfflineToken }
            val resolvedOfflineTokenExpiry = when {
                offlineTokenExpiresInSeconds > 0 -> now + offlineTokenExpiresInSeconds * 1_000
                fallbackOfflineTokenExpiry > 0 -> fallbackOfflineTokenExpiry
                else -> 0L
            }

            return UserCredentials(
                username = username,
                serverUrl = serverUrl,
                authServerUrl = authServerUrl,
                accessToken = accessToken,
                offlineToken = resolvedOfflineToken,
                accessTokenExpiresAtEpochMillis = now + expiresInSeconds * 1_000,
                offlineTokenExpiresAtEpochMillis = resolvedOfflineTokenExpiry
            )
        }
    }
}
