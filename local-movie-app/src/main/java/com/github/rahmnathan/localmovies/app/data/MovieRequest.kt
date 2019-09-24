package com.github.rahmnathan.localmovies.app.data

class MovieRequest(val page: Int, val resultsPerPage: Int, val path: String, val order:String) {

    companion object {
        private val client = "ANDROID"
    }
}
