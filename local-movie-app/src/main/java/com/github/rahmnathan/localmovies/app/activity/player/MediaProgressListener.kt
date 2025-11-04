package com.github.rahmnathan.localmovies.app.activity.player

import com.github.rahmnathan.localmovies.app.media.provider.control.MediaFacade
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.cast.framework.media.RemoteMediaClient.ProgressListener
import java.util.UUID

class MediaProgressListener(
    private val mediaFacade: MediaFacade,
    private val remoteMediaClient: RemoteMediaClient?) : ProgressListener {

    val uid = UUID.randomUUID().toString();

    override fun onProgressUpdated(p0: Long, p1: Long) {
        mediaFacade.saveProgress(remoteMediaClient?.mediaStatus?.mediaInfo?.metadata?.getString("update-position-url"), p0.toString(), uid)
    }
}