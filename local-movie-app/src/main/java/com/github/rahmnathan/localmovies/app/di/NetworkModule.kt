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
    fun provideOAuth2Service(
        preferencesDataStore: UserPreferencesDataStore
    ): OAuth2Service {
        val credentials = runBlocking {
            preferencesDataStore.userCredentialsFlow.first()
        }

        return OAuth2ServiceProvider.getOAuth2Service(
            credentials.username,
            credentials.password,
            credentials.authServerUrl
        )
    }
}
