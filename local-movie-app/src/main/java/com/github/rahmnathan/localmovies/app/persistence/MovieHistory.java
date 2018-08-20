package com.github.rahmnathan.localmovies.app.persistence;

import android.content.Context;

import com.github.rahmnathan.localmovies.app.data.Movie;
import com.google.common.collect.EvictingQueue;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovieHistory {
    private final Logger logger = Logger.getLogger(MovieHistory.class.getName());
    private static final String HISTORY_FILE = "history";
    private final Queue<Movie> movieQueue;
    private final Context context;

    public MovieHistory(Context context) {
        this.context = context;
        movieQueue = getMovieHistory();
    }

    public List<Movie> getHistoryList(){
        if(movieQueue != null) {
            List<Movie> tempList = new ArrayList<>(movieQueue);
            Collections.reverse(tempList);
            return tempList;
        } else {
            return Collections.emptyList();
        }
    }

    public void addHistoryItem(Movie movie){
        movieQueue.add(movie);
        saveHistory();
    }

    private void saveHistory() {
        try (ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(HISTORY_FILE, Context.MODE_PRIVATE))) {
            os.writeObject(movieQueue);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failure saving history", e);
        }
    }

    private Queue<Movie> getMovieHistory() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(context.openFileInput(HISTORY_FILE))) {
            return (Queue<Movie>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Failed to get movie history", e);
            return EvictingQueue.create(20);
        }
    }
}
