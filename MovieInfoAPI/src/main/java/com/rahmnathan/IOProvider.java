package com.rahmnathan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class IOProvider {

    public void writeInfoToFile(List<MovieInfo> movieInfo, String currentPath, String dataDirectory){

        String view = currentPath.replace("/", "") + ".txt";

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

    public List<MovieInfo> getInfoFromFile(String currentPath, String dataDirectory) throws Exception {

        String view = currentPath.replace("/", "") + ".txt";

        File setupFolder = new File(dataDirectory + "/LocalMovies/");
        setupFolder.mkdir();

        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(new File(setupFolder, view)));

        List<MovieInfo> movieData = (List<MovieInfo>) inputStream.readObject();

        inputStream.close();
        return movieData;
    }
}
