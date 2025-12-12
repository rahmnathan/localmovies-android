package com.github.rahmnathan.localmovies.app.di

import android.content.Context
import com.github.rahmnathan.localmovies.app.persistence.media.MediaPersistenceService
import com.github.rahmnathan.localmovies.app.persistence.media.room.MediaDAO
import com.github.rahmnathan.localmovies.app.persistence.media.room.MediaDatabase
import com.github.rahmnathan.localmovies.app.persistence.media.room.MediaPersistenceServiceRoom
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMediaDatabase(@ApplicationContext context: Context): MediaDatabase {
        return MediaDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideMediaDao(database: MediaDatabase): MediaDAO {
        return database.movieDAO()!!
    }

    @Provides
    @Singleton
    fun provideMediaPersistenceService(mediaDAO: MediaDAO): MediaPersistenceService {
        return MediaPersistenceServiceRoom(mediaDAO)
    }
}
