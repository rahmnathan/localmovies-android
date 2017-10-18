package com.github.rahmnathan.localmovies.app.google.pushnotification;

import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.logging.Logger;

import rahmnathan.localmovies.R;

public class LocalMovieFirebaseMessageService extends FirebaseMessagingService {
    private final Logger logger = Logger.getLogger(LocalMovieFirebaseMessageService.class.getName());

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        if(notification == null){
            logger.info("Received null push notification");
            return;
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.movie_icon)
                        .setContentTitle(notification.getTitle())
                        .setContentText(notification.getBody())
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mNotifyMgr.notify(1, mBuilder.build());
    }
}
