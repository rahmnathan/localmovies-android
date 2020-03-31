package com.github.rahmnathan.localmovies.app.control

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ProgressBar
import com.github.rahmnathan.localmovies.app.activity.PlayerActivity
import com.github.rahmnathan.localmovies.app.adapter.external.keycloak.KeycloakAuthenticator
import com.github.rahmnathan.localmovies.app.adapter.external.localmovie.MediaFacade
import com.github.rahmnathan.localmovies.app.adapter.list.MediaListAdapter
import com.github.rahmnathan.localmovies.app.data.Client
import com.github.rahmnathan.localmovies.app.data.Media
import com.github.rahmnathan.localmovies.app.google.cast.config.ExpandedControlActivity
import com.github.rahmnathan.localmovies.app.google.cast.control.GoogleCastUtils
import com.github.rahmnathan.localmovies.app.persistence.MediaHistory
import com.github.rahmnathan.localmovies.app.persistence.media.MediaPersistenceService
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import com.google.firebase.messaging.FirebaseMessaging
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.logging.Logger
import java.util.stream.Collectors

class MediaClickListener(
        private val mediaRepository: MediaRepository,
        val listAdapter: MediaListAdapter,
        val castContext: CastContext,
        val history: MediaHistory,
        val context: Context,
        val client: Client) : OnItemClickListener {

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        val posterPath: String
        val titles: List<Media>
        val media = listAdapter.getMovie(position)
        history.addHistoryItem(media)

        if (client.isViewingVideos) {
            // If we're viewing movies or episodes we refresh our token and start the video
            CompletableFuture.runAsync(KeycloakAuthenticator(client))
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
            val queueItems = GoogleCastUtils.assembleMediaQueue(titles, posterPath, client)
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