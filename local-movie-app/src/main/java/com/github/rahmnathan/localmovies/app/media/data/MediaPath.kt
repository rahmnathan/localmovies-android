package com.github.rahmnathan.localmovies.app.media.data

import java.util.*

class MediaPath : ArrayDeque<String?>() {
    override fun toString(): String {
        return java.lang.String.join("/", this)
    }
}