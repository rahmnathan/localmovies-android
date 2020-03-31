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
import com.google.android.gms.cast.framework.CastContext
import dagger.Module
import dagger.Provides
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
class AppModule(private val app: Application) {

    @Provides
    @Singleton
    fun provideContext(): Context = app

    @Provides
    fun provideExecutorService(): ExecutorService = Executors.newSingleThreadExecutor()

    @Provides
    fun provideCastContext(context: Context): CastContext = CastContext.getSharedInstance(context)

    @Provides
    @Singleton
    fun provideClient(context: Context): Client = MainActivityUtils.getPhoneInfo(context.openFileInput("setup"))

    @Provides
    fun provideMediaFacade(client: Client): MediaFacade = MediaFacade(client)

    @Provides
    @Singleton
    fun provideMediaDAO(context: Context): MediaDAO = MediaDatabase.getDatabase(context).movieDAO()!!

    @Provides
    @Singleton
    fun provideMediaPersistenceService(mediaDAO: MediaDAO, executorService: ExecutorService): MediaPersistenceService {
        return MediaPersistenceServiceRoom(mediaDAO, executorService)
    }
}