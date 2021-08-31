package com.github.rahmnathan.localmovies.app.cast.control

import android.net.Uri
import com.github.rahmnathan.localmovies.app.Client
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.oauth2.adapter.domain.OAuth2Service
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.common.images.WebImage
import com.google.common.net.MediaType
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.inject.Inject

class GoogleCastUtils @Inject constructor(
        private val oAuth2Service: OAuth2Service,
        private val client: Client
) {
    private val logger = Logger.getLogger(GoogleCastUtils::class.java.name)

    fun assembleMediaQueue(media: List<Media>, posterPath: String): List<MediaQueueItem> {
        return media.stream()
                .map { title: Media -> buildMediaQueueItem(title, posterPath) }
                .collect(Collectors.toList())
    }

    private fun buildMediaQueueItem(media: Media, posterPath: String): MediaQueueItem {
        val image = WebImage(Uri.parse(client.serverUrl
                + "/localmovie/v1/media/poster?access_token=" + oAuth2Service.accessToken.serialize()
                + "&path=" + posterPath))
        val movieUrl = (client.serverUrl
                + "/localmovie/v1/media/stream.mp4?access_token=" + oAuth2Service.accessToken.serialize()
                + "&path=" + encodeParameter(client.currentPath.toString() + File.separator + media.filename))
        val metaData = MediaMetadata()
        metaData.putString(MediaMetadata.KEY_TITLE, media.title)
        metaData.addImage(image)
        val mediaInfo = MediaInfo.Builder(movieUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(MediaType.ANY_VIDEO_TYPE.toString())
                .setMetadata(metaData)
                .build()
        return MediaQueueItem.Builder(mediaInfo)
                .setAutoplay(true)
                .setPreloadTime(30.0)
                .build()
    }

    private fun encodeParameter(parameter: String): String {
        return try {
            URLEncoder.encode(parameter, StandardCharsets.UTF_8.name())
        } catch (e: UnsupportedEncodingException) {
            logger.log(Level.SEVERE, "Failed to encode parameter: $parameter", e)
            ""
        }
    }
}