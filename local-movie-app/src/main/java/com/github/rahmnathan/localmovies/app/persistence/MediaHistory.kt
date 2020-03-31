package com.github.rahmnathan.localmovies.app.persistence

import android.content.Context
import com.github.rahmnathan.localmovies.app.data.Media
import com.google.common.collect.EvictingQueue
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class MediaHistory(private val context: Context) {
    private val logger = Logger.getLogger(MediaHistory::class.java.name)
    private var mediaQueue: Queue<Media?>

    val historyList: List<Media>
        get() = ArrayList(mediaQueue).filterNotNull().sortedByDescending { media -> media.created  }

    fun addHistoryItem(media: Media?) {
        mediaQueue.add(media)
        saveHistory()
    }

    private fun saveHistory() {
        try {
            ObjectOutputStream(context.openFileOutput(HISTORY_FILE, Context.MODE_PRIVATE)).use { os -> os.writeObject(mediaQueue) }
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "Failure saving history", e)
        }
    }

    private val movieHistory: Queue<Media?>
        get() {
            try {
                ObjectInputStream(context.openFileInput(HISTORY_FILE)).use { objectInputStream -> return objectInputStream.readObject() as Queue<Media?> }
            } catch (e: IOException) {
                logger.log(Level.SEVERE, "Failed to get media history", e)
                return EvictingQueue.create(20)
            } catch (e: ClassNotFoundException) {
                logger.log(Level.SEVERE, "Failed to get media history", e)
                return EvictingQueue.create(20)
            }
        }

    companion object {
        private const val HISTORY_FILE = "history"
    }

    init {
        mediaQueue = movieHistory
    }
}