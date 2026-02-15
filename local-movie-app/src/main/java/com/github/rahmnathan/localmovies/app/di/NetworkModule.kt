package com.github.rahmnathan.localmovies.app.di

import android.util.Log
import com.github.rahmnathan.localmovies.app.auth.OAuth2ServiceProvider
import com.github.rahmnathan.localmovies.app.data.local.UserCredentials
import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import com.github.rahmnathan.oauth2.adapter.domain.OAuth2Service
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideDynamicOAuth2Service(
        preferencesDataStore: UserPreferencesDataStore
    ): DynamicOAuth2Service {
        return DynamicOAuth2Service(preferencesDataStore)
    }

    @Provides
    fun provideOAuth2Service(
        dynamicService: DynamicOAuth2Service
    ): OAuth2Service {
        // For direct injection, return the cached service or a default
        return dynamicService.getCachedService()
    }
}

/**
 * Wrapper that provides OAuth2Service with current credentials from DataStore.
 * This ensures that after login, fresh credentials are used instead of stale cached ones.
 *
 * Prefer using [getServiceSuspend] in coroutine contexts to avoid blocking.
 */
@Singleton
class DynamicOAuth2Service(
    private val preferencesDataStore: UserPreferencesDataStore
) {
    companion object {
        private const val TAG = "DynamicOAuth2Service"
    }

    private val mutex = Mutex()

    @Volatile
    private var cachedService: OAuth2Service? = null
    @Volatile
    private var cachedCredentialsHash: Int = 0

    /**
     * Returns the OAuth2Service, fetching credentials asynchronously.
     * This is the preferred method to use in coroutine contexts.
     */
    suspend fun getServiceSuspend(): OAuth2Service = mutex.withLock {
        val credentials = try {
            preferencesDataStore.userCredentialsFlow.first()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading credentials", e)
            UserCredentials()
        }

        return getOrCreateService(credentials)
    }

    /**
     * Returns the cached OAuth2Service without blocking.
     * Returns a service with empty credentials if none is cached.
     * Use [getServiceSuspend] when possible for fresh credentials.
     */
    fun getCachedService(): OAuth2Service {
        return cachedService ?: OAuth2ServiceProvider.getOAuth2Service("", "", "")
    }

    private fun getOrCreateService(credentials: UserCredentials): OAuth2Service {
        val credentialsHash = credentials.hashCode()

        if (cachedService == null || cachedCredentialsHash != credentialsHash) {
            Log.d(TAG, "Creating new OAuth2Service with credentials: username=${credentials.username}, authUrl=${credentials.authServerUrl}")
            cachedService = OAuth2ServiceProvider.getOAuth2Service(
                credentials.username,
                credentials.password,
                credentials.authServerUrl
            )
            cachedCredentialsHash = credentialsHash
        }

        return cachedService!!
    }
}
