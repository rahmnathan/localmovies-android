package com.github.rahmnathan.localmovies.app.adapter.list;

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

import com.github.rahmnathan.localmovies.app.data.Media;
import com.github.rahmnathan.localmovies.app.data.MovieGenre;
import com.github.rahmnathan.localmovies.app.data.MovieOrder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import rahmnathan.localmovies.R;

import static com.github.rahmnathan.localmovies.app.adapter.list.ListAdapterUtils.mapImageToView;
import static com.github.rahmnathan.localmovies.app.adapter.list.ListAdapterUtils.mapRatingsToView;
import static com.github.rahmnathan.localmovies.app.adapter.list.ListAdapterUtils.mapTitleToView;
import static com.github.rahmnathan.localmovies.app.adapter.list.ListAdapterUtils.mapYearToView;

public class MovieListAdapter extends ArrayAdapter<Media> implements Filterable {

    private final List<Media> originalMediaList = new ArrayList<>();
    private final AdapterFilter adapterFilter = new AdapterFilter();
    private final Activity context;
    private List<Media> media;
    private CharSequence chars = "";

    public MovieListAdapter(Activity context, List<Media> mediaList) {
        super(context, R.layout.my_adapter, mediaList);
        this.media = mediaList;
        this.context = context;
    }

    @NonNull
    public View getView(int position, View rowView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        if (rowView == null)
            rowView = inflater.inflate(R.layout.my_adapter, parent, false);
        if (position >= this.media.size())
            return new View(context);

        TextView titleView = rowView.findViewById(R.id.textView);
        ImageView imageView = rowView.findViewById(R.id.imageView);
        TextView yearView = rowView.findViewById(R.id.year);
        TextView ratingView = rowView.findViewById(R.id.rating);

        Media media = this.media.get(position);

        String title = media.getTitle();
        if(media.getType().equalsIgnoreCase("episode")){
            title = "#" + media.getNumber() + " - " + title;
        }

        mapTitleToView(title, titleView, 16);
        mapImageToView(media.getImage(), imageView);
        mapYearToView(media.getReleaseYear(), yearView, 12);
        mapRatingsToView(media.getImdbRating(), media.getMetaRating(), ratingView);

        return rowView;
    }

    @Override
    @NonNull
    public Filter getFilter() {
        return adapterFilter;
    }

    public void filterGenre(MovieGenre genre){
        List<Media> filteredList = originalMediaList.stream()
                .sorted(Comparator.comparing(Media::getTitle))
                .filter(movieInfo -> movieInfo.getGenre().toLowerCase().contains(genre.getFormattedName()))
                .collect(Collectors.toList());

        display(filteredList);
    }

    public void clearLists() {
        media.clear();
        originalMediaList.clear();
    }

    public void updateList(List<Media> mediaList) {
        this.media.addAll(mediaList);
        this.originalMediaList.addAll(mediaList);
    }

    public CharSequence getChars() {
        return chars;
    }

    public List<Media> getOriginalMediaList() {
        return originalMediaList;
    }

    public Media getMovie(int position) {
        return media.get(position);
    }

    @Override
    public int getCount() {
        return media.size();
    }

    private class AdapterFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            chars = charSequence;
            FilterResults filterResults = new FilterResults();
            if (media != null) {
                media.clear();
                if (charSequence == null || charSequence.length() == 0) {
                    media.addAll(originalMediaList);
                } else {
                    media.addAll(originalMediaList.stream()
                            .filter(movie -> movie.getTitle().toLowerCase().contains(charSequence.toString().toLowerCase()))
                            .collect(Collectors.toList()));
                }
                filterResults.values = media;
                filterResults.count = media.size();
            }
            return filterResults;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            media = (List<Media>) filterResults.values;
            notifyDataSetChanged();
        }
    }

    public void display(List<Media> newMedia){
        media.clear();
        media.addAll(newMedia);
        notifyDataSetChanged();
    }

    public void sort(MovieOrder order) {
        ListAdapterUtils.sort(media, order);
        notifyDataSetChanged();
    }
}