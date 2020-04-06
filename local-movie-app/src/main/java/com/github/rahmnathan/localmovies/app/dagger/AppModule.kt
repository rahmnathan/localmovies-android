package com.github.rahmnathan.localmovies.app.dagger

import android.app.Application
import android.content.Context
import com.github.rahmnathan.localmovies.app.adapter.external.localmovie.MediaFacade
import com.github.rahmnathan.localmovies.app.control.OAuth2ServiceProvider.getOAuth2Service
import com.github.rahmnathan.localmovies.app.data.Client
import com.github.rahmnathan.localmovies.app.persistence.media.MediaPersistenceService
import com.github.rahmnathan.localmovies.app.persistence.media.room.MediaDAO
import com.github.rahmnathan.localmovies.app.persistence.media.room.MediaDatabase
import com.github.rahmnathan.localmovies.app.persistence.media.room.MediaPersistenceServiceRoom
import com.github.rahmnathan.oauth2.adapter.domain.OAuth2Service
import com.google.android.gms.cast.framework.CastContext
import dagger.Module
import dagger.Provides
import java.io.ObjectInputStream
import java.lang.Exception
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
        return getOAuth2Service(client.userName.toString(), client.password.toString())
    }
}