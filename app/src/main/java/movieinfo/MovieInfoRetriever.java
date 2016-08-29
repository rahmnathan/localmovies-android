package movieinfo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.os.EnvironmentCompat;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import activity.MainActivity;

public class MovieInfoRetriever {

    JSONObject jsonObject;
    String originalTitle;

    public List<MovieData> getMovieData(List<String> titleList, String currentPath){

        try{
            return getInfoFromFile(currentPath);
        } catch (Exception e){

            System.out.println("searching OMDB");

            List<MovieData> movieList = getInfoFromOMDB(titleList);

            new InfoWriter().writeInfo(movieList, currentPath);

            return movieList;
        }
    }

    public List<MovieData> getInfoFromFile(String currentPath) throws Exception {

        String[] viewGetter = currentPath.split("/");

        String view = viewGetter[viewGetter.length - 1] + ".txt";

        File setupFolder = new File(Environment.getExternalStorageDirectory().toString() + "/LocalMovies/");

        setupFolder.mkdir();



        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(new File(setupFolder, view)));

        List<MovieData> movieData = (List<MovieData>) inputStream.readObject();

        inputStream.close();

        return movieData;
    }

    public List<MovieData> getInfoFromOMDB(List<String> titleList){

        List<MovieData> movieDataList = new ArrayList<>();

        for(String x : titleList) {

            MovieData movieData = new MovieData();
            movieData.setTitle(x);

            getData(x);

            movieData.setImage(getImage());

            try {
                movieData.setActors(jsonObject.getString("Actors"));
            } catch (JSONException e) {
                movieData.setActors("N/A");
            }
            try {
                movieData.setIMDBRating(jsonObject.getString("imdbRating"));
            } catch (JSONException e) {
                movieData.setIMDBRating("N/A");
            }
            try {
                movieData.setMetaRating(jsonObject.getString("Metascore"));
            } catch (JSONException e) {
                movieData.setMetaRating("N/A");
            }
            try {
                movieData.setRating(jsonObject.getString("Rated"));
            } catch (JSONException e) {
                movieData.setRating("N/A");
            }
            try {
                movieData.setReleaseYear(jsonObject.getString("Year"));
            } catch (JSONException e) {
                movieData.setReleaseYear("N/A");
            }

            movieDataList.add(movieData);
        }
        return movieDataList;
    }

    public void getData(String title) {

        String uri = "http://www.omdbapi.com/?t=";
        originalTitle = title;
        String currentPath = MainActivity.myPhone.getPath().toLowerCase();

        if(currentPath.contains("season") || currentPath.contains("movies")) {
            title = title.substring(0, title.length() - 4);
        }

        try {
            URL url = new URL(uri + title.replace(" ", "%20"));

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            String end = "";

            String string = br.readLine();

            while (!(string == null)) {
                end = end + string;
                string = br.readLine();
            }
            br.close();
            urlConnection.disconnect();

            jsonObject = new JSONObject(end);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Bitmap getImage() {
        try {
            URL imageURL = new URL(jsonObject.get("Poster").toString());
            Bitmap bitmap = BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());
            return bitmap;

        } catch (Exception e) {}
        return null;
    }
}
