package com.github.rahmnathan.localmovies.app.activity.setup

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.github.rahmnathan.localmovies.app.activity.main.MainActivity
import com.github.rahmnathan.localmovies.app.auth.OAuth2ServiceProvider
import com.github.rahmnathan.localmovies.app.Client
import java.lang.Exception
import java.util.logging.Logger

class LoginHandler internal constructor(private val client: Client,
                                        private val activity: Activity) : Runnable {
    private val logger = Logger.getLogger(LoginHandler::class.java.name)
    private val UIHandler = Handler(Looper.getMainLooper())

    override fun run() {
        try {
            val oAuth2Service = OAuth2ServiceProvider.getOAuth2Service(client.userName.toString(), client.password.toString())
            oAuth2Service.accessToken
            SetupActivity.saveData(client, activity)
            activity.startActivity(Intent(activity, MainActivity::class.java))
        } catch (ex: Exception) {
            logger.severe("Failure logging in with provided credentials. $ex")
            UIHandler.post {Toast.makeText(activity, "Invalid username or password.", Toast.LENGTH_LONG).show()}
        }
    }
}
