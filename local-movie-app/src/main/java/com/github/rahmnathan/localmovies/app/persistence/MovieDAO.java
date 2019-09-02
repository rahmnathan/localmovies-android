package com.github.rahmnathan.localmovies.app.persistence;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MovieDAO {
    @Query("SELECT * FROM MovieEntity")
    List<com.github.rahmnathan.localmovies.app.persistence.MovieEntity> getAll();

    @Query("select * from MovieEntity where directoryPath = :path and filename = :name limit 1")
    com.github.rahmnathan.localmovies.app.persistence.MovieEntity getByPathAndFilename(String path, String name);

    @Insert
    void insertAll(List<MovieEntity> movieEntities);

    @Insert
    void insert(MovieEntity movieEntity);

    @Query("delete from MovieEntity")
    void deleteAll();

    @Delete
    void delete(com.github.rahmnathan.localmovies.app.persistence.MovieEntity movie);
}
