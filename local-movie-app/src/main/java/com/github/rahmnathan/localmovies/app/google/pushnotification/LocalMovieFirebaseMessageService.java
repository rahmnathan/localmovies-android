package com.github.rahmnathan.localmovies.app.google.pushnotification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.github.rahmnathan.localmovies.app.activity.MainActivity;
import com.github.rahmnathan.localmovies.client.Client;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import rahmnathan.localmovies.R;

public class LocalMovieFirebaseMessageService extends FirebaseMessagingService {
    private static final Logger logger = Logger.getLogger(LocalMovieFirebaseMessageService.class.getName());

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        buildChannel();
        Map<String, String> data = remoteMessage.getData();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.movie_icon)
                        .setContentTitle(data.get("title"))
                        .setContentText(data.get("body"))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setChannelId("LocalMovies")
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        Optional<byte[]> optionalPoster = getMoviePoster(data.get("path"));
        optionalPoster.ifPresent(poster -> {
            Bitmap bitmap = BitmapFactory.decodeByteArray(poster, 0, poster.length);
            mBuilder.setLargeIcon(bitmap);
            mBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigLargeIcon(bitmap).bigPicture(bitmap));
        });

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(this);
        mNotifyMgr.notify(new Random().nextInt(), mBuilder.build());
    }

    private void buildChannel(){
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("LocalMovies", "LocalMovies", importance);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

    }

    private Optional<byte[]>    getMoviePoster(String path) {
        HttpURLConnection urlConnection = null;
        String url = MainActivity.client.getComputerUrl() + "/localmovies/v2/movie/poster?path=" + path;

        try {
            urlConnection = (HttpURLConnection) (new URL(url)).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Authorization", "bearer " + MainActivity.client.getAccessToken());
            urlConnection.setConnectTimeout(10000);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed connecting to movie info service", e);
        }

        if (urlConnection != null) {
            try {
                return Optional.ofNullable(readBytes(urlConnection.getInputStream()));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failure loading image", e);
            }
        }
        return Optional.empty();
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        return byteBuffer.toByteArray();
    }
}
