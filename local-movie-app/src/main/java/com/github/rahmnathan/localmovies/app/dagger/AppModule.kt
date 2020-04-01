package com.github.rahmnathan.localmovies.app.dagger

import android.app.Application
import android.content.Context
import com.github.rahmnathan.localmovies.app.adapter.external.localmovie.MediaFacade
import com.github.rahmnathan.localmovies.app.control.MainActivityUtils
import com.github.rahmnathan.localmovies.app.control.MediaRepository
import com.github.rahmnathan.localmovies.app.data.Client
import com.github.rahmnathan.localmovies.app.persistence.media.MediaPersistenceService
import com.github.rahmnathan.localmovies.app.persistence.media.room.MediaDAO
import com.github.rahmnathan.localmovies.app.persistence.media.room.MediaDatabase
import com.github.rahmnathan.localmovies.app.persistence.media.room.MediaPersistenceServiceRoom
import com.github.rahmnathan.oauth2.adapter.domain.OAuth2Service
import com.github.rahmnathan.oauth2.adapter.domain.client.OAuth2Client
import com.github.rahmnathan.oauth2.adapter.domain.client.OAuth2ClientConfig
import com.github.rahmnathan.oauth2.adapter.domain.credential.Duration
import com.github.rahmnathan.oauth2.adapter.domain.credential.OAuth2CredentialPassword
import com.github.rahmnathan.oauth2.adapter.keycloak.resilience4j.KeycloakClientResilience4j
import com.google.android.gms.cast.framework.CastContext
import dagger.Module
import dagger.Provides
import java.io.ObjectInputStream
import java.lang.Exception
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Logger
import javax.inject.Singleton

@Module
class AppModule(private val app: Application) {
    private val logger = Logger.getLogger(AppModule::class.java.name)

    @Provides
    @Singleton
    fun provideContext(): Context = app

    @Provides
    @Singleton
    fun provideExecutorService(): ExecutorService = Executors.newSingleThreadExecutor()

    @Provides
    @Singleton
    fun provideCastContext(context: Context): CastContext = CastContext.getSharedInstance(context)

    @Provides
    @Singleton
    fun provideClient(context: Context): Client {
        return try {
            ObjectInputStream(context.openFileInput("setup")).use { objectInputStream ->
                val client = objectInputStream.readObject() as Client
                client.resetCurrentPath()
                return client
            }
        } catch (e: Exception) {
            logger.severe("Failure loading stored client data. $e")
            Client()
        }
    }

    @Provides
    @Singleton
    fun provideMediaFacade(client: Client, oAuth2Service: OAuth2Service): MediaFacade = MediaFacade(client,oAuth2Service)

    @Provides
    @Singleton
    fun provideMediaDAO(context: Context): MediaDAO = MediaDatabase.getDatabase(context).movieDAO()!!

    @Provides
    @Singleton
    fun provideMediaPersistenceService(mediaDAO: MediaDAO, executorService: ExecutorService): MediaPersistenceService {
        return MediaPersistenceServiceRoom(mediaDAO, executorService)
    }

    @Provides
    @Singleton
    fun provideOAuth2Service(client: Client): OAuth2Service {
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

        return OAuth2Service(passwordConfig, keycloakClient)
    }
}