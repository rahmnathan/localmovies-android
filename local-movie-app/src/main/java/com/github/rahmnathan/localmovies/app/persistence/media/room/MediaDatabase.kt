package com.github.rahmnathan.localmovies.app.persistence.media.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MediaEntity::class], version = 2, exportSchema = false)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun movieDAO(): MediaDAO?

    companion object {
        @Volatile
        private var INSTANCE: MediaDatabase? = null
        fun getDatabase(context: Context): MediaDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediaDatabase::class.java,
                    "word_database"
                )
                    .fallbackToDestructiveMigration(true) // Recreate DB when schema changes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}