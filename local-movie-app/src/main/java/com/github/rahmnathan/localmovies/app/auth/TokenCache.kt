package com.github.rahmnathan.localmovies.app.auth

import android.util.Log
import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import com.github.rahmnathan.localmovies.app.di.DynamicOAuth2Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Proactively caches OAuth2 access tokens to avoid blocking calls in synchronous contexts
 * (like OkHttp interceptors). The token is refreshed in the background when credentials
 * change or when explicitly requested.
 */
@Singleton
class TokenCache @Inject constructor(
    private val dynamicOAuth2Service: DynamicOAuth2Service,
    private val preferencesDataStore: UserPreferencesDataStore
) {
    companion object {
        private const val TAG = "TokenCache"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _cachedToken = MutableStateFlow<String?>(null)
    val cachedToken: StateFlow<String?> = _cachedToken.asStateFlow()

    @Volatile
    private var lastCredentialsHash: Int = 0

    init {
        // Observe credential changes and refresh token proactively
        scope.launch {
            preferencesDataStore.userCredentialsFlow.collect { credentials ->
                val credentialsHash = credentials.hashCode()
                if (credentialsHash != lastCredentialsHash && credentials.username.isNotBlank()) {
                    Log.d(TAG, "Credentials changed, refreshing token proactively")
                    lastCredentialsHash = credentialsHash
                    refreshToken()
                }
            }
        }
    }

    /**
     * Returns the cached access token synchronously.
     * This is safe to call from OkHttp interceptors.
     * Returns null if no token is cached yet.
     */
    fun getAccessToken(): String? {
        return _cachedToken.value
    }

    /**
     * Refreshes the token in the background.
     * Call this when you suspect the token may be stale.
     */
    fun refreshTokenAsync() {
        scope.launch {
            refreshToken()
        }
    }

    /**
     * Refreshes the token and updates the cache.
     * This is a suspend function that can be called from coroutines.
     */
    suspend fun refreshToken() {
        try {
            val token = dynamicOAuth2Service.getServiceSuspend().accessToken.serialize()
            _cachedToken.value = token
            Log.d(TAG, "Token refreshed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh token", e)
            // Keep the old token if refresh fails - it might still be valid
        }
    }

    /**
     * Clears the cached token. Call this on logout.
     */
    fun clearToken() {
        _cachedToken.value = null
        lastCredentialsHash = 0
        Log.d(TAG, "Token cache cleared")
    }
}
