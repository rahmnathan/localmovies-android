package com.github.rahmnathan.localmovies.app.persistence;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {com.github.rahmnathan.localmovies.app.persistence.MovieEntity.class}, version = 1)
public abstract class MovieDatabase extends RoomDatabase {
    public abstract com.github.rahmnathan.localmovies.app.persistence.MovieDAO movieDAO();

    private static MovieDatabase INSTANCE;

    public static MovieDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MovieDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context, MovieDatabase.class, "movie_database").build();
                }
            }
        }
        return INSTANCE;
    }
}
