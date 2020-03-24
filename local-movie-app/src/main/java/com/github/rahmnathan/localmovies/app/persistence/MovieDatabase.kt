package com.github.rahmnathan.localmovies.app.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MovieEntity::class], version = 1)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun movieDAO(): MovieDAO?

    companion object {
        private var INSTANCE: MovieDatabase? = null
        fun getDatabase(context: Context?): MovieDatabase? {
            if (INSTANCE == null) {
                synchronized(MovieDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(context!!, MovieDatabase::class.java, "movie_database").build()
                    }
                }
            }
            return INSTANCE
        }
    }
}