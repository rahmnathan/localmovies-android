package com.github.rahmnathan.localmovies.app.adapter;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.rahmnathan.localmovies.app.enums.MovieOrder;
import com.github.rahmnathan.localmovies.info.provider.data.Movie;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import rahmnathan.localmovies.R;

public class ListAdapterUtils {

    void sort(List<Movie> movies, MovieOrder order) {
        switch (order) {
            case DATE_ADDED:
                movies.sort((movie1, movie2) -> Objects.requireNonNull(movie2.getCreated()).compareTo(movie1.getCreated()));
                break;
            case MOST_VIEWS:
                movies.sort((movie1, movie2) -> Integer.compare(movie2.getViews(), movie1.getViews()));
                break;
            case RATING:
                movies.sort((movie1, movie2) -> String.valueOf(movie2.getImdbRating()).compareTo(String.valueOf(movie1.getImdbRating())));
                break;
            case RELEASE_YEAR:
                movies.sort((movie1, movie2) -> movie2.getReleaseYear().compareTo(movie1.getReleaseYear()));
                break;
            case TITLE:
                movies.sort(Comparator.comparing(Movie::getTitle));
                break;
        }
    }

    public void mapTitle(String title, TextView titleView, int fontSize){
        titleView.setText(title);
        titleView.setTextSize(fontSize);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextColor(Color.WHITE);
    }

    void mapRatings(String imdbRating, String metaRating, TextView ratings){
        ratings.setGravity(Gravity.CENTER);
        ratings.setTextColor(Color.WHITE);
        ratings.setTextSize(12);
        ratings.setText(String.format("IMDB: %s Meta: %s", imdbRating, metaRating));
    }

    public void mapImage(String base64Image, ImageView imageView){
        if (base64Image != null && !base64Image.equals("") && !base64Image.equals("null")) {
            byte[] image = Base64.decode(base64Image, Base64.DEFAULT);
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
        } else {
            imageView.setImageResource(R.mipmap.no_poster);
        }
    }

    public void mapYear(String releaseYear, TextView year, int fontSize){
        year.setText(String.format("Release Year: %s", releaseYear));
        year.setTextColor(Color.WHITE);
        year.setGravity(Gravity.CENTER);
        year.setTextSize(fontSize);
    }

    void display(List<Movie> movies, List<Movie> newMovies){
        movies.clear();
        movies.addAll(newMovies);
    }
}
