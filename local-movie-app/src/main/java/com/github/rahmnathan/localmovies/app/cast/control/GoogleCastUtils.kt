package com.github.rahmnathan.localmovies.app.cast.control

import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.media.provider.control.MediaFacade
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.common.images.WebImage
import com.google.common.net.MediaType
import java.util.stream.Collectors
import javax.inject.Inject
import androidx.core.net.toUri
import com.github.rahmnathan.localmovies.app.Client

class GoogleCastUtils @Inject constructor(private val mediaFacade: MediaFacade,
                                          private val client: Client) {

    fun assembleMediaQueue(media: List<Media>): List<MediaQueueItem> {
        return media.stream()
                .map { title: Media -> buildMediaQueueItem(title) }
                .collect(Collectors.toList())
    }

    private fun buildMediaQueueItem(media: Media): MediaQueueItem {
        val signedUrls = mediaFacade.getSignedUrls(media.mediaFileId)
        val metaData = MediaMetadata()
        metaData.putString(MediaMetadata.KEY_TITLE, media.title)
        metaData.putString("media-id", media.mediaFileId)
        metaData.putString("update-position-url", signedUrls.updatePosition)
        metaData.addImage(WebImage((client.serverUrl + signedUrls.poster).toUri()))
        val mediaInfo = MediaInfo.Builder(client.serverUrl + signedUrls.stream)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(MediaType.ANY_VIDEO_TYPE.toString())
                .setMetadata(metaData)
                .build()
        return MediaQueueItem.Builder(mediaInfo)
                .setAutoplay(true)
                .setPreloadTime(30.0)
                .build()
    }
}