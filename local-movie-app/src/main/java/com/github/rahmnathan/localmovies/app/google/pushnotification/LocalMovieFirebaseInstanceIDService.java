package com.github.rahmnathan.localmovies.app.google.pushnotification;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.util.logging.Logger;

public class LocalMovieFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private static final Logger logger = Logger.getLogger(LocalMovieFirebaseInstanceIDService.class.getName());

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        logger.info("Refreshed token: " + refreshedToken);
    }
}
