package com.github.rahmnathan.localmovies.app.data;

import java.util.ArrayDeque;

public class LocalMediaPath extends ArrayDeque<String> {

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        this.forEach(directory -> sb.append(directory).append("/"));

        return sb.toString().substring(0, sb.length()-1);
    }
}
