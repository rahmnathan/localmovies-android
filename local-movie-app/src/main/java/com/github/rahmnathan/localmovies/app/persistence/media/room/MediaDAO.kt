package com.github.rahmnathan.localmovies.app.persistence.media.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MediaDAO {
    @get:Query("SELECT * FROM MediaEntity")
    val all: List<MediaEntity?>?

    @Query("select * from MediaEntity where directoryPath = :path and filename = :name limit 1")
    fun getByPathAndFilename(path: String?, name: String?): MediaEntity?

    @Insert
    fun insertAll(mediaEntities: List<MediaEntity?>?)

    @Insert
    fun insert(mediaEntity: MediaEntity?)

    @Query("delete from MediaEntity")
    fun deleteAll()

    @Delete
    fun delete(media: MediaEntity?)
}