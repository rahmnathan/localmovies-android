package movieinfo;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;


public class InfoWriter {

    public void writeInfo(List<MovieData> movieInfo, String currentPath){

        String[] viewGetter = currentPath.split("/");

        String view = viewGetter[viewGetter.length - 1] + ".txt";

        File setupFolder = new File(Environment.getExternalStorageDirectory().toString() + "/LocalMovies/");

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
