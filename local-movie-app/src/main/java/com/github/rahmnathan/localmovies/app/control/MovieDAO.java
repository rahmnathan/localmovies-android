package com.github.rahmnathan.localmovies.app.control;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface MovieDAO {
    @Query("SELECT * FROM movie_list")
    List<MovieListEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MovieListEntity movie);

    @Delete
    void delete(MovieListEntity movie);
}
