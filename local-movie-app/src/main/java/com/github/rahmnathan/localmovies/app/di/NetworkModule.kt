package com.github.rahmnathan.localmovies.app.di

import com.github.rahmnathan.localmovies.app.auth.OAuth2ServiceProvider
import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import com.github.rahmnathan.oauth2.adapter.domain.OAuth2Service
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
        return dynamicService.getService()
    }
}

/**
 * Wrapper that provides OAuth2Service with current credentials from DataStore.
 * This ensures that after login, fresh credentials are used instead of stale cached ones.
 */
@Singleton
class DynamicOAuth2Service(
    private val preferencesDataStore: UserPreferencesDataStore
) {
    @Volatile
    private var cachedService: OAuth2Service? = null
    @Volatile
    private var cachedCredentialsHash: Int = 0

    fun getService(): OAuth2Service {
        val credentials = runBlocking {
            try {
                preferencesDataStore.userCredentialsFlow.first()
            } catch (e: Exception) {
                android.util.Log.e("DynamicOAuth2Service", "Error reading credentials", e)
                com.github.rahmnathan.localmovies.app.data.local.UserCredentials()
            }
        }

        // Create hash of credentials to detect changes
        val credentialsHash = credentials.hashCode()

        // If credentials changed, recreate the service
        if (cachedService == null || cachedCredentialsHash != credentialsHash) {
            android.util.Log.d("DynamicOAuth2Service", "Creating new OAuth2Service with credentials: username=${credentials.username}, authUrl=${credentials.authServerUrl}")
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
