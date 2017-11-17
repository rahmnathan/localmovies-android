package com.github.rahmnathan.localmovies.app.google.pushnotification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.github.rahmnathan.localmovies.app.activity.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import rahmnathan.localmovies.R;

public class LocalMovieFirebaseMessageService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.movie_icon)
                        .setContentTitle(data.get("Title"))
                        .setContentText(data.get("Body"))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pendingIntent);

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mNotifyMgr.notify(1, mBuilder.build());
    }
}
