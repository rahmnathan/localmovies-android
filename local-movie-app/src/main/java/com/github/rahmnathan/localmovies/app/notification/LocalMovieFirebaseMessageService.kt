package com.github.rahmnathan.localmovies.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.rahmnathan.localmovies.app.activity.main.MainActivity
import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import com.github.rahmnathan.localmovies.app.data.remote.MediaApi
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import rahmnathan.localmovies.R
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject

@AndroidEntryPoint
class LocalMovieFirebaseMessageService : FirebaseMessagingService() {

    @Inject lateinit var mediaApi: MediaApi
    @Inject lateinit var preferencesDataStore: UserPreferencesDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Check permission first (fixes the missing permission handling issue)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            logger.warning("POST_NOTIFICATIONS permission not granted")
            return
        }

        buildChannel()

        val data = remoteMessage.data
        val mBuilder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.movie_icon)
                .setContentTitle(data["title"])
                .setContentText(data["body"])
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setChannelId("LocalMovies")
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Use coroutines for poster fetching
        serviceScope.launch {
            try {
                val poster = getMoviePoster(data["path"])
                if (poster != null) {
                    val bitmap = BitmapFactory.decodeByteArray(poster, 0, poster.size)
                    mBuilder.setLargeIcon(bitmap)
                    mBuilder.setStyle(NotificationCompat.BigPictureStyle().bigLargeIcon(bitmap).bigPicture(bitmap))
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Failed to fetch poster", e)
            }

            val pendingIntent = PendingIntent.getActivity(
                this@LocalMovieFirebaseMessageService,
                0,
                Intent(this@LocalMovieFirebaseMessageService, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            mBuilder.setContentIntent(pendingIntent)

            val mNotifyMgr = NotificationManagerCompat.from(this@LocalMovieFirebaseMessageService)
            mNotifyMgr.notify(Random().nextInt(), mBuilder.build())
        }
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        logger.info("Firebase token: $s")
    }

    private fun buildChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("LocalMovies", "LocalMovies", importance)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }

    private suspend fun getMoviePoster(path: String?): ByteArray? = withContext(Dispatchers.IO) {
        var urlConnection: HttpURLConnection? = null
        val credentials = preferencesDataStore.userCredentialsFlow.first()
        val url = credentials.serverUrl + "/localmovies/v1/media/poster?path=" + path

        try {
            urlConnection = URL(url).openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.connectTimeout = 10000
            readBytes(urlConnection.inputStream)
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "Failed loading poster", e)
            null
        } finally {
            urlConnection?.disconnect()
        }
    }

    @Throws(IOException::class)
    private fun readBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private val logger = Logger.getLogger(LocalMovieFirebaseMessageService::class.java.name)
    }
}