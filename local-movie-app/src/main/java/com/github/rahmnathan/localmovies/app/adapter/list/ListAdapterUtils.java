package com.github.rahmnathan.localmovies.app.adapter.list;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.rahmnathan.localmovies.app.data.Media;
import com.github.rahmnathan.localmovies.app.data.MovieOrder;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import rahmnathan.localmovies.R;

public class ListAdapterUtils {

    static void sort(List<Media> media, MovieOrder order) {
        switch (order) {
            case DATE_ADDED:
                media.sort((movie1, movie2) -> Objects.requireNonNull(movie2.getCreated()).compareTo(movie1.getCreated()));
                break;
            case MOST_VIEWS:
                media.sort((movie1, movie2) -> Integer.compare(movie2.getViews(), movie1.getViews()));
                break;
            case RATING:
                media.sort((movie1, movie2) -> movie2.getImdbRating().compareTo(movie1.getImdbRating()));
                break;
            case RELEASE_YEAR:
                media.sort((movie1, movie2) -> movie2.getReleaseYear().compareTo(movie1.getReleaseYear()));
                break;
            case TITLE:
                media.sort(Comparator.comparing(Media::getTitle));
                break;
        }
    }

    public static void mapTitleToView(String title, TextView titleView, int fontSize){
        titleView.setText(title);
        titleView.setTextSize(fontSize);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextColor(Color.WHITE);
    }

    static void mapRatingsToView(String imdbRating, String metaRating, TextView ratings){
        ratings.setGravity(Gravity.CENTER);
        ratings.setTextColor(Color.WHITE);
        ratings.setTextSize(12);
        ratings.setText(String.format("IMDB: %s Meta: %s", imdbRating, metaRating));
    }

    public static void mapImageToView(String base64Image, ImageView imageView){
        if (base64Image != null && !base64Image.equals("") && !base64Image.equals("null")) {
            byte[] image = Base64.decode(base64Image, Base64.DEFAULT);
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
        } else {
            imageView.setImageResource(R.mipmap.no_poster);
        }
    }

    public static void mapYearToView(String releaseYear, TextView year, int fontSize){
        year.setText(String.format("Release Year: %s", releaseYear));
        year.setTextColor(Color.WHITE);
        year.setGravity(Gravity.CENTER);
        year.setTextSize(fontSize);
    }
}
