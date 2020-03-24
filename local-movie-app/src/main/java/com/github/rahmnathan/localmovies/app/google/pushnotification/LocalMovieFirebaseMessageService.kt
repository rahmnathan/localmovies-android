package com.github.rahmnathan.localmovies.app.google.pushnotification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.rahmnathan.localmovies.app.activity.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import rahmnathan.localmovies.R
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class LocalMovieFirebaseMessageService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
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

        val optionalPoster = getMoviePoster(data["path"])
        optionalPoster.ifPresent { poster: ByteArray ->
            val bitmap = BitmapFactory.decodeByteArray(poster, 0, poster.size)
            mBuilder.setLargeIcon(bitmap)
            mBuilder.setStyle(NotificationCompat.BigPictureStyle().bigLargeIcon(bitmap).bigPicture(bitmap))
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        mBuilder.setContentIntent(pendingIntent)

        val mNotifyMgr = NotificationManagerCompat.from(this)
        mNotifyMgr.notify(Random().nextInt(), mBuilder.build())
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

    private fun getMoviePoster(path: String?): Optional<ByteArray> {
        var urlConnection: HttpURLConnection? = null
        val url = MainActivity.client.computerUrl + "/localmovies/v2/media/poster?path=" + path

        try {
            urlConnection = URL(url).openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.setRequestProperty("Authorization", "bearer " + MainActivity.client.accessToken)
            urlConnection.connectTimeout = 10000
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "Failed connecting to media info service", e)
        }

        if (urlConnection != null) {
            try {
                return Optional.ofNullable(readBytes(urlConnection.inputStream))
            } catch (e: IOException) {
                logger.log(Level.SEVERE, "Failure loading image", e)
            }
        }
        return Optional.empty()
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

    companion object {
        private val logger = Logger.getLogger(LocalMovieFirebaseMessageService::class.java.name)
    }
}