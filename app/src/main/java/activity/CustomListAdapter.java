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

import rahmnathan.localmovies.R;

public class CustomListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final Map<String, Bitmap> data;
    private final List<String> titles;

    public CustomListAdapter(Activity context, Map<String, Bitmap> data, List<String> titles) {
        super(context, R.layout.my_adapter, titles);
        this.context=context;
        this.data = data;
        this.titles = titles;
    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.my_adapter, null,true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.textView);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView);

        String currentTitle = titles.get(position);

        txtTitle.setText(currentTitle);

        if(data.containsKey(currentTitle)) {
            imageView.setImageBitmap(data.get(currentTitle));
        } else{
            imageView.setImageResource(R.drawable.movie_icon);
        }

        return rowView;
    }
}