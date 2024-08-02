package com.github.rahmnathan.localmovies.app.media.data

enum class MediaEndpoint(val endpoint: String) {
    MEDIA("/localmovie/mobile/v1/media"),
    HISTORY("/localmovie/mobile/v1/media/history")
}