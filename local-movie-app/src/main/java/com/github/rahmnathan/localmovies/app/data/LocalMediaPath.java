package com.github.rahmnathan.localmovies.app.data;

import java.util.ArrayDeque;
import java.util.stream.Collectors;

public class LocalMediaPath extends ArrayDeque<String> {

    @Override
    public String toString(){
        return String.join("/", this);
    }
}
