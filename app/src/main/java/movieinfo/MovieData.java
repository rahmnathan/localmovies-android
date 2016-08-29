package movieinfo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class MovieData implements Serializable {

    private static final long serialVersionUID = 1L;
    private String title;
    private String IMDBRating;
    private String MetaRating;
    private byte[] image;
    private String releaseYear;
    private String rating;
    private String actors;

    public void setActors(String actors) {
        this.actors = actors;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public void setReleaseYear(String releaseYear){
        this.releaseYear = releaseYear;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setIMDBRating(String IMDBRating) {
        this.IMDBRating = IMDBRating;
    }

    public void setImage(Bitmap image){
        if(image != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 0, outputStream);

            this.image = outputStream.toByteArray();
        } else{
            this.image = null;
        }
    }

    public void setMetaRating(String metaRating) {
        MetaRating = metaRating;
    }

    public String getActors() {
        return actors;
    }

    public String getRating() {
        return rating;
    }

    public String getReleaseYear(){
        return releaseYear;
    }

    public String getTitle(){
        return title;
    }

    public String getIMDBRating(){
        return IMDBRating;
    }

    public String getMetaRating(){
        return MetaRating;
    }

    public Bitmap getImage() {

        if(image != null) {
            return BitmapFactory.decodeByteArray(image, 0, image.length);
        } else {
            return null;
        }
    }
}