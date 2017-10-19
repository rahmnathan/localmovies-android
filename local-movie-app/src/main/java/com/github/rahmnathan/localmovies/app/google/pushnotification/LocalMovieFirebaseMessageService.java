package com.github.rahmnathan.localmovies.app.google.pushnotification;

import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.logging.Logger;

import rahmnathan.localmovies.R;

public class LocalMovieFirebaseMessageService extends FirebaseMessagingService {
    private final Logger logger = Logger.getLogger(LocalMovieFirebaseMessageService.class.getName());

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.movie_icon)
                        .setContentTitle(data.get("Title"))
                        .setContentText(data.get("Body"))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mNotifyMgr.notify(1, mBuilder.build());
    }
}
