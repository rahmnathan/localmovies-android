package com.github.rahmnathan.localmovies.info.provider.data

import java.io.Serializable
import java.util.Comparator

class Movie(val title: String, val imdbRating: String, val metaRating: String, val image: String, val releaseYear: String,
                                val created: Long?, val views: Int, val genre: String, val filename: String, val actors: String, val plot: String) : Serializable, Comparator<Movie> {

    override fun toString(): String {
        return title
    }

    override fun compare(info1: Movie, info2: Movie): Int {
        return info1.title.compareTo(info2.title)
    }
}