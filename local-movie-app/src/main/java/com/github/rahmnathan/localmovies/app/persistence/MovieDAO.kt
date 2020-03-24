package com.github.rahmnathan.localmovies.app.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MovieDAO {
    @get:Query("SELECT * FROM MovieEntity")
    val all: List<MovieEntity?>?

    @Query("select * from MovieEntity where directoryPath = :path and filename = :name limit 1")
    fun getByPathAndFilename(path: String?, name: String?): MovieEntity?

    @Insert
    fun insertAll(movieEntities: List<MovieEntity?>?)

    @Insert
    fun insert(movieEntity: MovieEntity?)

    @Query("delete from MovieEntity")
    fun deleteAll()

    @Delete
    fun delete(movie: MovieEntity?)
}