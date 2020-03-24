package com.github.rahmnathan.localmovies.app.data

import java.util.*

class LocalMediaPath : ArrayDeque<String?>() {
    override fun toString(): String {
        return java.lang.String.join("/", this)
    }
}