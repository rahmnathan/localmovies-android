package com.localmovies.client;

import java.util.ArrayDeque;
import java.util.Arrays;

public class LocalMediaPath extends ArrayDeque<String> {

    public LocalMediaPath(String path){
        Arrays.stream(path.split("/")).forEachOrdered(this::add);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        this.forEach(directory -> sb.append(directory).append("/"));

        return sb.toString();
    }
}
