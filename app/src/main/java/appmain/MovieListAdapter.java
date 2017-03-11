package appmain;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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

import com.rahmnathan.MovieInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import rahmnathan.localmovies.R;

class MovieListAdapter extends ArrayAdapter<MovieInfo> implements Filterable {

    private final Activity context;
    List<MovieInfo> movies;
    private final List<MovieInfo> originalMovieList;
    private AdapterFilter adapterFilter;

    MovieListAdapter(Activity context, List<MovieInfo> movies) {
        super(context, R.layout.my_adapter, movies);
        this.context = context;
        this.movies = movies;
        this.originalMovieList = movies;
    }

    public View getView(int position, View rowView, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        if (rowView == null) {
            rowView = inflater.inflate(R.layout.my_adapter, parent, false);
        }

        TextView txtTitle = (TextView) rowView.findViewById(R.id.textView);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView);
        TextView year = (TextView) rowView.findViewById(R.id.year);
        TextView ratings = (TextView) rowView.findViewById(R.id.rating);

        MovieInfo movie = movies.get(position);
        String currentTitle = movie.getTitle();

        if (currentTitle.contains(".")) {
            currentTitle = currentTitle.substring(0, currentTitle.length() - 4);
        }
        txtTitle.setText(currentTitle);
        txtTitle.setGravity(Gravity.CENTER);
        txtTitle.setTextColor(Color.WHITE);

        String base64Image = movie.getImage();
        if (base64Image != null && !base64Image.equals("") && !base64Image.equals("null")) {
            byte[] image = Base64.decode(movie.getImage(), Base64.DEFAULT);
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
        } else {
            imageView.setImageResource(R.mipmap.no_poster);
        }

        year.setTextColor(Color.WHITE);
        year.setGravity(Gravity.CENTER);
        ratings.setGravity(Gravity.CENTER);
        ratings.setTextColor(Color.WHITE);
        year.setText(String.format("Release Year: %s", movie.getReleaseYear()));
        ratings.setText(String.format("IMDB: %s Meta: %s", movie.getIMDBRating(), movie.getMetaRating()));

        return rowView;
    }

    @Override
    public Filter getFilter() {
        if (adapterFilter == null) {
            adapterFilter = new AdapterFilter();
            return adapterFilter;
        } else {
            return adapterFilter;
        }
    }

    @Override
    public int getCount() {
        return movies.size();
    }

    private class AdapterFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults filterResults = new FilterResults();
            if (charSequence == null || charSequence.length() == 0) {
                movies = originalMovieList;
                filterResults.values = movies;
                filterResults.count = movies.size();
            } else {
                movies = originalMovieList.parallelStream()
                        .filter((movie)-> movie.getTitle().toLowerCase().contains(charSequence.toString().toLowerCase()))
                        .collect(Collectors.toList());

                filterResults.values = movies;
                filterResults.count = movies.size();
            }
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            movies = (List<MovieInfo>) filterResults.values;
            notifyDataSetChanged();
        }
    }

}