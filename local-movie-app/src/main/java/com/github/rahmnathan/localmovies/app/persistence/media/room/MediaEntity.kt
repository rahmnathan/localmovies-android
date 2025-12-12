package com.github.rahmnathan.localmovies.app.persistence.media.room

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.rahmnathan.localmovies.app.media.data.Media

@Entity
class MediaEntity(var directoryPath: String?, @field:Embedded var media: Media) {
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null

}