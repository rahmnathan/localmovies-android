package com.github.rahmnathan.localmovies.app.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.github.rahmnathan.localmovies.app.activity.SetupActivity
import com.github.rahmnathan.localmovies.app.data.Client
import com.github.rahmnathan.localmovies.app.persistence.MovieDatabase
import rahmnathan.localmovies.R
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.CompletableFuture
import java.util.logging.Level
import java.util.logging.Logger

class SetupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setup_main)
        ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
        )

        val userName = findViewById<EditText>(R.id.userName)
        userName.background.setColorFilter(Color.RED, PorterDuff.Mode.DARKEN)
        val password = findViewById<EditText>(R.id.password)
        password.background.setColorFilter(Color.RED, PorterDuff.Mode.DARKEN)

        // Populating our text fields with our saved data if it exists
        val client = phoneInfo
        userName.setText(client.userName)
        password.setText(client.password)

        val set = findViewById<Button>(R.id.set)
        set.setOnClickListener { view: View? ->
            val client1 = Client(userName.text.toString(), password.text.toString())
            saveData(client1, this)
            startActivity(Intent(this@SetupActivity, MainActivity::class.java))
        }

        val clearMovies = findViewById<Button>(R.id.clearMovies)
        clearMovies.setOnClickListener { view: View? ->
            CompletableFuture.runAsync {
                val movieDAO = MovieDatabase.getDatabase(this)!!.movieDAO()
                movieDAO!!.deleteAll()
                startActivity(Intent(this@SetupActivity, MainActivity::class.java))
            }
        }
    }

    private val phoneInfo: Client
        get() {
            try {
                ObjectInputStream(openFileInput(SETUP_FILE)).use { objectInputStream -> return objectInputStream.readObject() as Client }
            } catch (e: IOException) {
                logger.log(Level.SEVERE, "Failed to get client data", e)
                return Client()
            } catch (e: ClassNotFoundException) {
                logger.log(Level.SEVERE, "Failed to get client data", e)
                return Client()
            }
        }

    companion object {
        private val logger = Logger.getLogger(SetupActivity::class.java.name)
        private const val SETUP_FILE = "setup"
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        @JvmStatic
        fun saveData(client: Client?, context: Context) {
            try {
                ObjectOutputStream(context.openFileOutput(SETUP_FILE, Context.MODE_PRIVATE)).use { os ->
                    os.writeObject(client)
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                logger.log(Level.SEVERE, "Failed to save client data", e)
            }
        }
    }
}