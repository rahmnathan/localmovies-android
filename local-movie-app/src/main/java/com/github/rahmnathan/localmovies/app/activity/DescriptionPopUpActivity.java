package com.github.rahmnathan.localmovies.app.activity;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.rahmnathan.localmovies.app.data.Movie;

import rahmnathan.localmovies.R;

import static android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND;
import static com.github.rahmnathan.localmovies.app.adapter.list.ListAdapterUtils.mapImageToView;
import static com.github.rahmnathan.localmovies.app.adapter.list.ListAdapterUtils.mapTitleToView;
import static com.github.rahmnathan.localmovies.app.adapter.list.ListAdapterUtils.mapYearToView;

public class DescriptionPopUpActivity extends Activity {
    public static final String MOVIE = "movie";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.description_popup);
        getWindow().addFlags(FLAG_DIM_BEHIND);
        getWindow().setDimAmount(0.8f);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        getWindow().setLayout(Double.valueOf(width * .75).intValue(), Double.valueOf(height * .75).intValue());

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            Object object = bundle.get(MOVIE);
            if (object != null) {
                Movie movie = (Movie) object;
                TextView titleView = findViewById(R.id.detailedTitle);
                ImageView imageView = findViewById(R.id.detailedPoster);
                TextView yearView = findViewById(R.id.detailedYear);
                TextView metaRatingView = findViewById(R.id.detailedMetaRating);
                TextView imdbRatingView = findViewById(R.id.detailedIMDBRating);
                TextView plotView = findViewById(R.id.detailedPlot);
                TextView actorView = findViewById(R.id.detailedActors);

                mapImageToView(movie.getImage(), imageView);
                mapTitleToView(movie.getTitle(), titleView, 22);
                mapYearToView(movie.getReleaseYear(), yearView, 16);

                mapTextToView(metaRatingView, String.format("Metacritic Rating: %s", movie.getMetaRating()), 16);
                mapTextToView(imdbRatingView, String.format("IMDB Rating: %s", movie.getImdbRating()), 16);
                mapTextToView(plotView, movie.getPlot(), 14);
                mapTextToView(actorView, String.format("Starring: %s", movie.getActors()), 14);
            }
        }
    }

    private void mapTextToView(TextView textView, String value, int fontSize){
        textView.setTextColor(Color.WHITE);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(fontSize);
        textView.setText(value);
    }
}
