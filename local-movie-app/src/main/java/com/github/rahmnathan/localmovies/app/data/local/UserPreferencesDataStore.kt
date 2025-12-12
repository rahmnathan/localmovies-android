package com.github.rahmnathan.localmovies.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserCredentials(
    val username: String = "",
    val password: String = "",
    val serverUrl: String = "https://movies.nathanrahm.com",
    val authServerUrl: String = "https://login.nathanrahm.com"
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_SERVER_URL = stringPreferencesKey("auth_server_url")
    }

    val userCredentialsFlow: Flow<UserCredentials> = context.dataStore.data.map { prefs ->
        UserCredentials(
            username = prefs[PreferencesKeys.USERNAME] ?: "",
            password = prefs[PreferencesKeys.PASSWORD] ?: "",
            serverUrl = prefs[PreferencesKeys.SERVER_URL] ?: "https://movies.nathanrahm.com",
            authServerUrl = prefs[PreferencesKeys.AUTH_SERVER_URL] ?: "https://login.nathanrahm.com"
        )
    }

    suspend fun saveCredentials(credentials: UserCredentials) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USERNAME] = credentials.username
            prefs[PreferencesKeys.PASSWORD] = credentials.password
            prefs[PreferencesKeys.SERVER_URL] = credentials.serverUrl
            prefs[PreferencesKeys.AUTH_SERVER_URL] = credentials.authServerUrl
        }
    }
}
