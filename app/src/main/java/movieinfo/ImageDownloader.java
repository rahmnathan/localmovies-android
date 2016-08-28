package movieinfo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageDownloader {

    MovieData movieData = new MovieData();
    JSONObject jsonObject;
    String originalTitle;

    public MovieData getMovieData(String title){
        movieData.setTitle(title);

        getData(title);

        movieData.setImage(getImage());
        try {
            movieData.setActors(jsonObject.getString("Actors"));
        }catch(JSONException e){
            movieData.setActors("No Data");
        }
        try {
            movieData.setIMDBRating(jsonObject.getString("imdbRating"));
        }catch(JSONException e){
            movieData.setActors("No Data");
        }
        try {
            movieData.setMetaRating(jsonObject.getString("Metascore"));
        }catch(JSONException e){
            movieData.setActors("No Data");
        }
        try {
            movieData.setRating(jsonObject.getString("Rated"));
        }catch(JSONException e){
            movieData.setActors("No Data");
        }
        try {
            movieData.setReleaseYear(jsonObject.getString("Year"));
        }catch(JSONException e){
            movieData.setActors("No Data");
        }

        return movieData;

    }

    public void getData(String title) {

        String uri = "http://www.omdbapi.com/?t=";

        originalTitle = title;

        title = title.substring(0, title.length() - 4);

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

            //System.out.println(imageURL.toString());

            Bitmap bitmap = BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());

            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
