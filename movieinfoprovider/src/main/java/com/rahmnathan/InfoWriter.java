package com.rahmnathan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

class InfoWriter {

    public void writeInfo(List<MovieInfo> movieInfo, String currentPath, String dataDirectory){

        String[] viewGetter = currentPath.split("/");

        String view = viewGetter[viewGetter.length - 1] + ".txt";

        File setupFolder = new File(dataDirectory +"/LocalMovies/");

        setupFolder.mkdir();

        try{
        File file = new File(setupFolder, view);

            if (!file.exists()) {
                file.createNewFile();
            }

            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));

            outputStream.writeObject(movieInfo);

            outputStream.close();
        } catch (IOException e){
            e.printStackTrace();
        }

    }
}
