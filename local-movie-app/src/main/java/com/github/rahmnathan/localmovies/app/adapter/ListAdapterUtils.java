package com.github.rahmnathan.localmovies.app.adapter;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.rahmnathan.localmovies.app.enums.MovieOrder;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import rahmnathan.localmovies.R;

class ListAdapterUtils {

    void sort(List<MovieInfo> movies, MovieOrder order) {
        List<MovieInfo> tempList = new ArrayList<>();
        switch (order) {
            case DATE_ADDED:
                tempList = movies.stream()
                        .sorted((movie1, movie2) -> movie2.getCreated().compareTo(movie1.getCreated()))
                        .collect(Collectors.toList());
                break;
            case MOST_VIEWS:
                tempList = movies.stream()
                        .sorted((movie1, movie2) -> Integer.compare(movie2.getViews(), movie1.getViews()))
                        .collect(Collectors.toList());
                break;
            case RATING:
                tempList = movies.stream()
                        .sorted((movie1, movie2) -> String.valueOf(movie2.getIMDBRating()).compareTo(String.valueOf(movie1.getIMDBRating())))
                        .collect(Collectors.toList());
                break;
            case RELEASE_YEAR:
                tempList = movies.stream()
                        .sorted((movie1, movie2) -> movie2.getReleaseYear().compareTo(movie1.getReleaseYear()))
                        .collect(Collectors.toList());
                break;
            case TITLE:
                tempList = movies.stream()
                        .sorted(Comparator.comparing(MovieInfo::getTitle))
                        .collect(Collectors.toList());
                break;
        }
        movies.clear();
        movies.addAll(tempList);
    }

    void mapTitle(String title, TextView titleView){
        titleView.setText(title);
        titleView.setTextSize(17);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextColor(Color.WHITE);
    }

    void mapRatings(String imdbRating, String metaRating, TextView ratings){
        ratings.setGravity(Gravity.CENTER);
        ratings.setTextColor(Color.WHITE);
        ratings.setTextSize(12);
        ratings.setText(String.format("IMDB: %s Meta: %s", imdbRating, metaRating));
    }

    void mapImage(String base64Image, ImageView imageView){
        if (base64Image != null && !base64Image.equals("") && !base64Image.equals("null")) {
            byte[] image = Base64.decode(base64Image, Base64.DEFAULT);
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
        } else {
            imageView.setImageResource(R.mipmap.no_poster);
        }
    }

    void mapYear(String releaseYear, TextView year){
        year.setText(String.format("Release Year: %s", releaseYear));
        year.setTextColor(Color.WHITE);
        year.setGravity(Gravity.CENTER);
        year.setTextSize(12);
    }

    void display(List<MovieInfo> movies, List<MovieInfo> newMovies){
        movies.clear();
        movies.addAll(newMovies);
    }
}
