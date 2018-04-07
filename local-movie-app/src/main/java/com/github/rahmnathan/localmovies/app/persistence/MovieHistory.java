package com.github.rahmnathan.localmovies.app.persistence;

import android.content.Context;

import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;
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
    private final Queue<MovieInfo> movieInfoQueue;
    private final Context context;

    public MovieHistory(Context context) {
        this.context = context;
        movieInfoQueue = getMovieHistory();
    }

    public List<MovieInfo> getHistoryList(){
        if(movieInfoQueue != null) {
            List<MovieInfo> tempList = new ArrayList<>(movieInfoQueue);
            Collections.reverse(tempList);
            return tempList;
        } else {
            return Collections.emptyList();
        }
    }

    public void addHistoryItem(MovieInfo movieInfo){
        movieInfoQueue.add(movieInfo);
        saveHistory();
    }

    private void saveHistory() {
        try (ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(HISTORY_FILE, Context.MODE_PRIVATE))) {
            os.writeObject(movieInfoQueue);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failure saving history", e);
        }
    }

    private Queue<MovieInfo> getMovieHistory() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(context.openFileInput(HISTORY_FILE))) {
            return (Queue<MovieInfo>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Failed to get movie history", e);
            return EvictingQueue.create(20);
        }
    }
}
