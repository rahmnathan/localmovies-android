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
import java.util.Map;

import movieinfo.ImageDownloader;
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
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.my_adapter, null,true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.textView);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView);

        MovieData movie = movies.get(position);

        String currentTitle = movie.getTitle();

        txtTitle.setText(currentTitle);

        if(!(movie.getImage() == null)) {
            imageView.setImageBitmap(movie.getImage());
        } else{
            imageView.setImageResource(R.drawable.movie_icon);
        }

        return rowView;
    }
}