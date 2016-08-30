package activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import movieinfo.MovieData;
import rahmnathan.localmovies.R;

public class CustomListAdapter extends ArrayAdapter<MovieData> implements Filterable {

    private final Activity context;
    private List<MovieData> movies;
    private List<MovieData> originalMovieList;
    public AdapterFilter adapterFilter;

    public CustomListAdapter(Activity context, List<MovieData> movies) {
        super(context, R.layout.my_adapter, movies);
        this.context=context;
        this.movies = movies;
        this.originalMovieList = movies;
    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.my_adapter, null, true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.textView);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView);
        TextView year = (TextView) rowView.findViewById(R.id.year);
        TextView ratings = (TextView) rowView.findViewById(R.id.rating);

        MovieData movie = movies.get(position);

        String currentTitle = movie.getTitle();

        String currentPath = MainActivity.myPhone.getPath().toLowerCase();

        int mainPathLength = MainActivity.myPhone.getMainPath().split("/").length;
        int currentPathLength = MainActivity.myPhone.getPath().split("/").length;
        int level = currentPathLength - mainPathLength;

        if (currentPath.contains("movies") || currentPath.contains("season")) {
            currentTitle = currentTitle.substring(0, currentTitle.length() - 4);
        }
        txtTitle.setText(currentTitle);

        Bitmap bitmap = movie.getImage();

        if (bitmap != null && level == 1) {

            year.setText("Release Year: " + movie.getReleaseYear());
            ratings.setText("IMDB: " + movie.getIMDBRating() + " Meta: " + movie.getMetaRating() + "  ");
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.movie_icon);
            ratings.setText("N/A");
            year.setText("N/A");
        }

        return rowView;
    }

    @Override
    public Filter getFilter(){
        if(adapterFilter == null){
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

    public class AdapterFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence){
            FilterResults filterResults = new FilterResults();
            if(charSequence == null || charSequence.length() == 0){
                filterResults.values = originalMovieList;
                filterResults.count = originalMovieList.size();
            } else{
                List<MovieData> movieDataList = new ArrayList<>();

                for(MovieData movie : originalMovieList){
                    if(movie.getTitle().toLowerCase().contains(charSequence.toString().toLowerCase())){
                        movieDataList.add(movie);
                    }
                }
                filterResults.values = movieDataList;
                filterResults.count = movieDataList.size();
            }
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults){
                movies = (List<MovieData>) filterResults.values;
                notifyDataSetChanged();
        }
    }

}