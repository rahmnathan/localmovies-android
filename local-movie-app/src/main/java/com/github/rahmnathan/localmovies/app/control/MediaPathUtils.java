package com.github.rahmnathan.localmovies.app.control;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

class MediaPathUtils {

    static String getParentPath(String path){
        String[] dirs = path.split(File.separator);
        return Arrays.stream(dirs)
                .limit(dirs.length - 1)
                .collect(Collectors.joining(File.separator));
    }

    static String getFilename(String path){
        String[] directoryList = path.split(File.separator);
        return directoryList[directoryList.length - 1];
    }
}
