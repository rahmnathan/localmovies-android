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

    @Query("select * from MediaEntity where directoryPath = :path")
    fun getByPath(path: String?): List<MediaEntity?>?

    @Insert
    fun insertAll(mediaEntities: List<MediaEntity>)

    @Insert
    fun insert(mediaEntity: MediaEntity)

    @Query("delete from MediaEntity")
    fun deleteAll(): Int

    @Delete
    fun delete(media: MediaEntity)

    // Suspend functions for coroutines
    @Query("SELECT * FROM MediaEntity")
    suspend fun getAllSuspend(): List<MediaEntity>

    @Query("select * from MediaEntity where directoryPath = :path")
    suspend fun getByPathSuspend(path: String): List<MediaEntity>

    @Insert
    suspend fun insertAllSuspend(mediaEntities: List<MediaEntity>)

    @Insert
    suspend fun insertSuspend(mediaEntity: MediaEntity)

    @Query("delete from MediaEntity")
    suspend fun deleteAllSuspend(): Int

    @Delete
    suspend fun deleteSuspend(media: MediaEntity)
}