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
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import rahmnathan.localmovies.R;

public class MovieListAdapter extends ArrayAdapter<MovieInfo> implements Filterable {

    private final ListAdapterUtils adapterUtils = new ListAdapterUtils();
    private final List<MovieInfo> originalMovieList = new ArrayList<>();
    private final AdapterFilter adapterFilter = new AdapterFilter();
    private final Activity context;
    private List<MovieInfo> movies;
    private CharSequence chars = "";

    public MovieListAdapter(Activity context, List<MovieInfo> movieInfoList) {
        super(context, R.layout.my_adapter, movieInfoList);
        this.movies = movieInfoList;
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

        MovieInfo movie = movies.get(position);
        adapterUtils.mapTitle(movie.getTitle(), titleView);
        adapterUtils.mapImage(movie.getImage(), imageView);
        adapterUtils.mapYear(movie.getReleaseYear(), yearView);
        adapterUtils.mapRatings(movie.getIMDBRating(), movie.getMetaRating(), ratingView);

        return rowView;
    }

    @Override
    @NonNull
    public Filter getFilter() {
        return adapterFilter;
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

    public void display(List<MovieInfo> newMovies){
        adapterUtils.display(movies, newMovies);
        notifyDataSetChanged();
    }

    public void sort(MovieOrder order) {
        adapterUtils.sort(movies, order);
        notifyDataSetChanged();
    }
}