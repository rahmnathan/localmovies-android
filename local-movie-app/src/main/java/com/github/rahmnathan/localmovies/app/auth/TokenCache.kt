package com.github.rahmnathan.localmovies.app.auth

import android.util.Log
import com.github.rahmnathan.localmovies.app.data.local.UserCredentials
import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the current access token available for synchronous callers while refreshing
 * or migrating the persisted session in the background.
 */
@Singleton
class TokenCache @Inject constructor(
    private val authSessionManager: AuthSessionManager,
    private val preferencesDataStore: UserPreferencesDataStore
) {
    companion object {
        private const val TAG = "TokenCache"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()

    private val _cachedToken = MutableStateFlow<String?>(null)
    val cachedToken: StateFlow<String?> = _cachedToken.asStateFlow()

    @Volatile
    private var currentCredentials: UserCredentials = UserCredentials()

    init {
        scope.launch {
            preferencesDataStore.userCredentialsFlow.collect { credentials ->
                currentCredentials = credentials
                _cachedToken.value = credentials.accessToken.ifBlank { null }

                when {
                    credentials.hasSession() && !credentials.isAccessTokenValid() -> refreshTokenAsync()
                    credentials.hasLegacyPassword() && credentials.accessToken.isBlank() -> refreshTokenAsync()
                    credentials.username.isBlank() -> clearToken()
                }
            }
        }
    }

    fun getAccessToken(): String? {
        if (currentCredentials.hasSession() && !currentCredentials.isAccessTokenValid()) {
            refreshTokenAsync()
        }
        return _cachedToken.value
    }

    fun refreshTokenAsync() {
        scope.launch {
            refreshToken()
        }
    }

    suspend fun refreshToken() {
        refreshMutex.withLock {
            val credentials = preferencesDataStore.userCredentialsFlow.first()

            try {
                val updatedCredentials = when {
                    credentials.hasSession() -> authSessionManager.refreshSession(credentials)
                    credentials.hasLegacyPassword() -> authSessionManager.migrateLegacyCredentials(credentials)
                    else -> {
                        _cachedToken.value = null
                        return
                    }
                }

                currentCredentials = updatedCredentials
                _cachedToken.value = updatedCredentials.accessToken
                Log.d(TAG, "Token refreshed successfully")
            } catch (e: Exception) {
                if (e is AuthSessionException && e.errorCode == "invalid_grant") {
                    Log.w(TAG, "Stored session is no longer valid, clearing credentials", e)
                    preferencesDataStore.clearCredentials()
                    clearToken()
                    return
                }

                if (!credentials.isAccessTokenValid(refreshThresholdMillis = 0)) {
                    _cachedToken.value = null
                }
                Log.e(TAG, "Failed to refresh token", e)
            }
        }
    }

    fun clearToken() {
        currentCredentials = UserCredentials()
        _cachedToken.value = null
        Log.d(TAG, "Token cache cleared")
    }
}
