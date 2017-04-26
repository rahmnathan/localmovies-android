package com.localmovies.client;

import java.util.ArrayDeque;
import java.util.Arrays;

public class LocalMediaPath extends ArrayDeque<String> {

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        this.forEach(directory -> sb.append(directory).append("/"));

        return sb.toString();
    }
}
