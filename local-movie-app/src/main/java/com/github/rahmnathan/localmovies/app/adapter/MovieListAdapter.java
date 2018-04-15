package com.github.rahmnathan.localmovies.app.adapter;

import android.app.Activity;
import android.support.annotation.NonNull;
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
import com.github.rahmnathan.localmovies.info.provider.data.Movie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import rahmnathan.localmovies.R;

public class MovieListAdapter extends ArrayAdapter<Movie> implements Filterable {

    private final ListAdapterUtils adapterUtils = new ListAdapterUtils();
    private final List<Movie> originalMovieList = new ArrayList<>();
    private final AdapterFilter adapterFilter = new AdapterFilter();
    private final Activity context;
    private List<Movie> movies;
    private CharSequence chars = "";

    public MovieListAdapter(Activity context, List<Movie> movieList) {
        super(context, R.layout.my_adapter, movieList);
        this.movies = movieList;
        this.context = context;
    }

    @NonNull
    public View getView(int position, View rowView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        if (rowView == null)
            rowView = inflater.inflate(R.layout.my_adapter, parent, false);
        if (position >= movies.size())
            return new View(context);

        TextView titleView = rowView.findViewById(R.id.textView);
        ImageView imageView = rowView.findViewById(R.id.imageView);
        TextView yearView = rowView.findViewById(R.id.year);
        TextView ratingView = rowView.findViewById(R.id.rating);

        Movie movie = movies.get(position);
        adapterUtils.mapTitle(movie.getTitle(), titleView, 17);
        adapterUtils.mapImage(movie.getImage(), imageView);
        adapterUtils.mapYear(movie.getReleaseYear(), yearView, 12);
        adapterUtils.mapRatings(movie.getImdbRating(), movie.getMetaRating(), ratingView);

        return rowView;
    }

    @Override
    @NonNull
    public Filter getFilter() {
        return adapterFilter;
    }

    public void filterGenre(MovieGenre genre){
        List<Movie> filteredList = originalMovieList.stream()
                .sorted(Comparator.comparing(Movie::getTitle))
                .filter(movieInfo -> movieInfo.getGenre().toLowerCase().contains(genre.getFormattedName()))
                .collect(Collectors.toList());

        display(filteredList);
    }

    public void clearLists() {
        movies.clear();
        originalMovieList.clear();
    }

    public void updateList(List<Movie> movieList) {
        this.movies.addAll(movieList);
        this.originalMovieList.addAll(movieList);
    }

    public CharSequence getChars() {
        return chars;
    }

    public List<Movie> getOriginalMovieList() {
        return originalMovieList;
    }

    public Movie getMovie(int position) {
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
            movies = (List<Movie>) filterResults.values;
            notifyDataSetChanged();
        }
    }

    public void display(List<Movie> newMovies){
        adapterUtils.display(movies, newMovies);
        notifyDataSetChanged();
    }

    public void sort(MovieOrder order) {
        adapterUtils.sort(movies, order);
        notifyDataSetChanged();
    }
}