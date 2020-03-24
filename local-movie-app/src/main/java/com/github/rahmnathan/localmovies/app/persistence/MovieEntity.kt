package com.github.rahmnathan.localmovies.app.persistence

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.rahmnathan.localmovies.app.data.Media

@Entity
class MovieEntity(var directoryPath: String, @field:Embedded var media: Media) {
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null

}