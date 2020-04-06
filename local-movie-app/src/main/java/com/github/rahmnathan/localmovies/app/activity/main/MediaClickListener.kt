package com.github.rahmnathan.localmovies.app.activity.main

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import com.github.rahmnathan.localmovies.app.activity.player.PlayerActivity
import com.github.rahmnathan.localmovies.app.activity.main.view.MediaListAdapter
import com.github.rahmnathan.localmovies.app.media.provider.MediaRepository
import com.github.rahmnathan.localmovies.app.Client
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.cast.config.ExpandedControlActivity
import com.github.rahmnathan.localmovies.app.cast.control.GoogleCastUtils
import com.github.rahmnathan.localmovies.app.persistence.MediaHistory
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import java.io.File
import java.util.stream.Collectors

class MediaClickListener(
        private val mediaRepository: MediaRepository,
        private val listAdapter: MediaListAdapter,
        private val castContext: CastContext,
        private val history: MediaHistory,
        private val context: Context,
        private val client: Client,
        private val castUtils: GoogleCastUtils) : OnItemClickListener {

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        val posterPath: String
        val titles: List<Media>
        val media = listAdapter.getMovie(position)
        history.addHistoryItem(media)

        if (client.isViewingVideos) {
            // If we're viewing movies or episodes we start the video
            if (client.isViewingEpisodes) {
                // If we're playing episodes, we queue up the rest of the season
                posterPath = client.currentPath.toString()
                titles = listAdapter.getOriginalMediaList().stream()
                        .filter { movieInfo: Media -> Integer.valueOf(movieInfo.number!!) > Integer.valueOf(media.number!!) || movieInfo.title == media.title }
                        .collect(Collectors.toList())
            } else {
                posterPath = client.currentPath.toString() + File.separator + media.filename
                titles = listOf(media)
            }
            val queueItems = castUtils.assembleMediaQueue(titles, posterPath)
            queueVideos(queueItems)
        } else {
            client.appendToCurrentPath(media.filename)
            mediaRepository.getVideos()
        }
    }

    private fun queueVideos(queueItems: List<MediaQueueItem>) {
        val session = castContext.sessionManager.currentCastSession
        if (session != null && session.isConnected) {
            val remoteMediaClient = session.remoteMediaClient
            remoteMediaClient.queueLoad(queueItems.toTypedArray(), 0, 0, null)
            context.startActivity(Intent(context, ExpandedControlActivity::class.java))
        } else {
            val intent = Intent(context, PlayerActivity::class.java)
            val url = queueItems[0].media.contentId
            intent.putExtra("url", url)
            context.startActivity(intent)
        }
    }
}