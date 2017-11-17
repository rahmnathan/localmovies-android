package com.github.rahmnathan.localmovies.app.adapter;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.rahmnathan.localmovies.app.enums.MovieGenre;
import com.github.rahmnathan.localmovies.app.enums.MovieOrder;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import rahmnathan.localmovies.R;

public class MovieListAdapter extends ArrayAdapter<MovieInfo> implements Filterable {

    private final Activity context;
    private List<MovieInfo> movies;
    private final List<MovieInfo> originalMovieList;
    private AdapterFilter adapterFilter;
    private CharSequence chars = "";

    public MovieListAdapter(Activity context, List<MovieInfo> movieInfoList) {
        super(context, R.layout.my_adapter, movieInfoList);
        this.context = context;
        this.movies = movieInfoList;
        originalMovieList = new ArrayList<>();
    }

    @NonNull
    public View getView(int position, View rowView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        if (rowView == null)
            rowView = inflater.inflate(R.layout.my_adapter, parent, false);

        TextView title = rowView.findViewById(R.id.textView);
        ImageView imageView = rowView.findViewById(R.id.imageView);
        TextView year = rowView.findViewById(R.id.year);
        TextView ratings = rowView.findViewById(R.id.rating);

        if (movies.size() <= position)
            return new View(context);
        MovieInfo movie = movies.get(position);

        if (title == null)
            return new View(context);
        title.setText(movie.getTitle());
        title.setTextSize(17);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.WHITE);

        String base64Image = movie.getImage();
        if (base64Image != null && !base64Image.equals("") && !base64Image.equals("null")) {
            byte[] image = Base64.decode(movie.getImage(), Base64.DEFAULT);
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
        } else {
            imageView.setImageResource(R.mipmap.no_poster);
        }

        year.setTextColor(Color.WHITE);
        year.setGravity(Gravity.CENTER);
        year.setTextSize(12);
        ratings.setGravity(Gravity.CENTER);
        ratings.setTextColor(Color.WHITE);
        ratings.setTextSize(12);
        year.setText(String.format("Release Year: %s", movie.getReleaseYear()));
        ratings.setText(String.format("IMDB: %s Meta: %s", movie.getIMDBRating(), movie.getMetaRating()));

        return rowView;
    }

    @Override
    @NonNull
    public Filter getFilter() {
        if (adapterFilter == null) {
            adapterFilter = new AdapterFilter();
            return adapterFilter;
        } else {
            return adapterFilter;
        }
    }

    public void filterGenre(MovieGenre genre){
        List<MovieInfo> filteredList = originalMovieList.stream()
                .sorted(Comparator.comparing(MovieInfo::getTitle))
                .filter(movieInfo -> movieInfo.getGenre().toLowerCase().contains(genre.getFormattedName()))
                .collect(Collectors.toList());

        display(filteredList);
    }

    public void clearLists() {
        movies.clear();
        originalMovieList.clear();
    }

    public void updateList(List<MovieInfo> movieInfoList) {
        this.movies.addAll(movieInfoList);
        this.originalMovieList.addAll(movieInfoList);
    }

    public CharSequence getChars() {
        return chars;
    }

    public List<MovieInfo> getOriginalMovieList() {
        return originalMovieList;
    }

    public MovieInfo getMovie(int position) {
        return movies.get(position);
    }

    @Override
    public int getCount() {
        return movies.size();
    }

    private class AdapterFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            chars = charSequence;
            FilterResults filterResults = new FilterResults();
            if (movies != null) {
                movies.clear();
                if (charSequence == null || charSequence.length() == 0) {
                    movies.addAll(originalMovieList);
                } else {
                    movies.addAll(originalMovieList.stream()
                            .filter(movie -> movie.getTitle().toLowerCase().contains(charSequence.toString().toLowerCase()))
                            .collect(Collectors.toList()));
                }
                filterResults.values = movies;
                filterResults.count = movies.size();
            }
            return filterResults;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            movies = (List<MovieInfo>) filterResults.values;
            notifyDataSetChanged();
        }
    }

    public void display(List<MovieInfo> movieInfoList){
        movies.clear();
        movies.addAll(movieInfoList);
        notifyDataSetChanged();
    }

    public void sort(MovieOrder order) {
        List<MovieInfo> tempList = new ArrayList<>();
        switch (order) {
            case DATE_ADDED:
                tempList = movies.stream()
                        .sorted((movie1, movie2) -> movie2.getCreated().compareTo(movie1.getCreated()))
                        .collect(Collectors.toList());
                break;
            case MOST_VIEWS:
                tempList = movies.stream()
                        .sorted((movie1, movie2) -> Integer.valueOf(movie2.getViews()).compareTo(movie1.getViews()))
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
        notifyDataSetChanged();
    }
}