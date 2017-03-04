package appmain;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;

import com.phoneinfo.Phone;
import com.rahmnathan.MovieInfo;
import com.restclient.RestClient;

import java.util.Collections;
import java.util.List;

public class ThreadManager extends Thread {
    private final String title;
    private final Phone phone;
    private ProgressBar progressBar;
    private MovieListAdapter movieListAdapter;

    private final Handler UIHandler = new Handler(Looper.getMainLooper());

    public ThreadManager(String title, ProgressBar progressBar, MovieListAdapter movieListAdapter,
                         Phone myPhone){
        this.title = title;
        this.phone = myPhone;
        this.movieListAdapter = movieListAdapter;
        this.progressBar = progressBar;
    }

    public void run() {
        phone.setCurrentPath(phone.getCurrentPath() + title + "/");
        updateListView();
    }

    private void updateListView() {

        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        MainActivity.MOVIE_INFO_LIST.clear();
        try {
            List<MovieInfo> infoList = MainActivity.movieInfo.get(phone.getCurrentPath());
            Collections.sort(infoList, MovieInfo.Builder.newInstance().build());
            MainActivity.MOVIE_INFO_LIST.addAll(infoList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                movieListAdapter.notifyDataSetChanged();
            }
        });
    }
}
