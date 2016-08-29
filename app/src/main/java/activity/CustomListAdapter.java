package activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import movieinfo.MovieData;
import rahmnathan.localmovies.R;

public class CustomListAdapter extends ArrayAdapter<MovieData> {

    private final Activity context;
    private final List<MovieData> movies;

    public CustomListAdapter(Activity context, List<MovieData> movies) {
        super(context, R.layout.my_adapter, movies);
        this.context=context;
        this.movies = movies;

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

        year.setText("Release Year: " + movie.getReleaseYear());

        ratings.setText("IMDB: " + movie.getIMDBRating() + " Meta: " + movie.getMetaRating() + "  ");

        Bitmap bitmap = movie.getImage();

        if (bitmap != null && level == 1) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.movie_icon);
        }

        return rowView;
    }
}