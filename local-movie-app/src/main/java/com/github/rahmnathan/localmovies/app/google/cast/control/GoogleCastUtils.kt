package com.github.rahmnathan.localmovies.app.google.cast.control

import android.net.Uri
import com.github.rahmnathan.localmovies.app.data.Client
import com.github.rahmnathan.localmovies.app.data.Media
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

object GoogleCastUtils {
    private val logger = Logger.getLogger(GoogleCastUtils::class.java.name)
    fun assembleMediaQueue(media: List<Media>, posterPath: String, myClient: Client): List<MediaQueueItem> {
        return media.stream()
                .map { title: Media -> buildMediaQueueItem(title, posterPath, myClient) }
                .collect(Collectors.toList())
    }

    private fun buildMediaQueueItem(media: Media, posterPath: String, myClient: Client): MediaQueueItem {
        val image = WebImage(Uri.parse(myClient.computerUrl
                + "/localmovie/v2/media/poster?access_token=" + myClient.accessToken
                + "&path=" + posterPath))
        val movieUrl = (myClient.computerUrl
                + "/localmovie/v2/media/stream.mp4?access_token=" + myClient.accessToken
                + "&path=" + encodeParameter(myClient.currentPath.toString() + File.separator + media.filename))
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