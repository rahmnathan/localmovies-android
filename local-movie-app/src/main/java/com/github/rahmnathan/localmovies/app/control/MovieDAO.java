package com.github.rahmnathan.localmovies.app.control;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface MovieDAO {
    @Query("SELECT * FROM MovieEntity")
    List<MovieEntity> getAll();

    @Insert
    void insert(MovieEntity movieEntity);

    @Insert
    void insertAll(List<MovieEntity> movieEntities);

    @Delete
    void delete(MovieEntity movie);
}
