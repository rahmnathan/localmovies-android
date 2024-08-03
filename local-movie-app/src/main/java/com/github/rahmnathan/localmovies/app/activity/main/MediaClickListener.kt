package com.github.rahmnathan.localmovies.app.activity.main

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import com.github.rahmnathan.localmovies.app.activity.player.PlayerActivity
import com.github.rahmnathan.localmovies.app.activity.main.view.MediaListAdapter
import com.github.rahmnathan.localmovies.app.media.provider.boundary.MediaRepository
import com.github.rahmnathan.localmovies.app.Client
import com.github.rahmnathan.localmovies.app.activity.player.MediaProgressListener
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.cast.config.ExpandedControlActivity
import com.github.rahmnathan.localmovies.app.cast.control.GoogleCastUtils
import com.github.rahmnathan.localmovies.app.media.data.MediaEndpoint
import com.github.rahmnathan.localmovies.app.media.provider.control.MediaFacade
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import org.json.JSONObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.stream.Collectors

class MediaClickListener(
        private val mediaRepository: MediaRepository,
        private val listAdapter: MediaListAdapter,
        private val castContext: CastContext,
        private val context: Context,
        private val client: Client,
        private val castUtils: GoogleCastUtils,
        private val mediaFacade: MediaFacade,
        private val executorService: ExecutorService) : OnItemClickListener {

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        val titles: List<Media>
        val media = listAdapter.getMovie(position)
        client.endpoint = MediaEndpoint.MEDIA;

        if (client.isViewingVideos) {
            // If we're viewing movies or episodes we start the video
            titles = if (client.isViewingEpisodes) {
                // If we're playing episodes, we queue up the rest of the season
                listAdapter.getOriginalMediaList().stream()
                    .filter { movieInfo: Media -> Integer.valueOf(movieInfo.number!!) > Integer.valueOf(media.number!!) || movieInfo.title == media.title }
                    .collect(Collectors.toList())
            } else {
                listOf(media)
            }
            val queueItems = castUtils.assembleMediaQueue(titles)
            queueVideos(queueItems)
        } else {
            client.appendToCurrentPath(media.filename)
            CompletableFuture.runAsync({mediaRepository.getVideos()}, executorService)
        }
    }

    private fun queueVideos(queueItems: List<MediaQueueItem>) {
        val session = castContext.sessionManager.currentCastSession
        if (session != null && session.isConnected) {
            val remoteMediaClient = session.remoteMediaClient
            remoteMediaClient?.queueLoad(queueItems.toTypedArray(), 0, 0, JSONObject())

            val progressListener = MediaProgressListener(mediaFacade, client, remoteMediaClient)
            remoteMediaClient?.addProgressListener(progressListener, 5000)

            context.startActivity(Intent(context, ExpandedControlActivity::class.java))
        } else {
            val intent = Intent(context, PlayerActivity::class.java)
            val url = queueItems[0].media?.contentId
            intent.putExtra("media-id", queueItems[0].media?.metadata?.getString("media-id"))
            intent.putExtra("url", url)
            context.startActivity(intent)
        }
    }
}