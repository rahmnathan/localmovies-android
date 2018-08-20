package com.github.rahmnathan.localmovies.app.persistence;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface MovieDAO {
    @Query("SELECT * FROM MovieEntity")
    List<com.github.rahmnathan.localmovies.app.persistence.MovieEntity> getAll();

    @Query("delete from MovieEntity")
    void deleteAll();

    @Insert
    void insertAll(List<com.github.rahmnathan.localmovies.app.persistence.MovieEntity> movieEntities);

    @Query("select * from MovieEntity where directoryPath = :path and filename = :name limit 1")
    com.github.rahmnathan.localmovies.app.persistence.MovieEntity getByPathAndFilename(String path, String name);

    @Delete
    void delete(com.github.rahmnathan.localmovies.app.persistence.MovieEntity movie);
}
