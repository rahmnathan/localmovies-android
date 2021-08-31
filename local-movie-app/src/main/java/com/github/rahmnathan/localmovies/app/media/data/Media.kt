package com.github.rahmnathan.localmovies.app.media.data

import java.io.Serializable
import java.util.Comparator

class Media(val title: String, val imdbRating: String, val metaRating: String, val image: String, val releaseYear: String,
            val created: Long?, val genre: String, val filename: String, val actors: String, val plot: String,
            val path: String, val number: String?, val type: String, val mediaFileId: String) : Serializable, Comparator<Media> {

    override fun toString(): String {
        return title
    }

    override fun compare(info1: Media, info2: Media): Int {
        return info1.title.compareTo(info2.title)
    }
}